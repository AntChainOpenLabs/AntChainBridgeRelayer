/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.relayer.dal.repository.impl;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AnchorProcessHeights;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.dal.entities.AnchorProcessEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.BaseEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.BlockchainEntity;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import com.alipay.antchain.bridge.relayer.dal.service.BlockchainService;
import com.alipay.antchain.bridge.relayer.dal.service.IAnchorProcessService;
import com.alipay.antchain.bridge.relayer.dal.utils.ConvertUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.ByteArrayCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BlockchainRepository implements IBlockchainRepository {

    @Resource
    private IAnchorProcessService anchorProcessService;

    @Resource
    private BlockchainService blockchainService;

    @Resource
    private RedissonClient redissonClient;

    @Value("${anchor.process.cache.heights.ttl:240000}")
    private long ttlForHeightsCache;

    @Value("${anchor.process.cache.flush.period:0}")
    private long flushPeriodForHeightsCache;

    @Override
    public Long getAnchorProcessHeight(String product, String blockchainId, String heightType) {
        Long height = getHeightFromCache(product, blockchainId, heightType);
        if (ObjectUtil.isNull(height) || ObjectUtil.equals(0L, height)) {
            height = getAnchorProcessHeightFromDB(product, blockchainId, heightType);
            setHeightToCache(product, blockchainId, heightType, height);
        }
        return height;
    }

    @Override
    public void setAnchorProcessHeight(String product, String blockchainId, String heightType, Long height) {
        try {
            if (flushPeriodForHeightsCache == 0) {
                flushHeight(product, blockchainId, heightType, height);
                setHeightToCache(product, blockchainId, heightType, height);
            } else {
                AnchorProcessHeights heights = getAnchorProcessHeightsFromCache(product, blockchainId);
                if (ObjectUtil.isNull(heights)) {
                    heights = new AnchorProcessHeights(product, blockchainId);
                    heights.getProcessHeights().put(heightType, height);
                }

                long now = System.currentTimeMillis();
                if (now - heights.getLastUpdateTime() > flushPeriodForHeightsCache) {
                    flushAnchorProcessHeights(heights);
                    heights.setLastUpdateTime(now);
                }
                setAnchorProcessHeightsToCache(heights);
            }
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_ANCHOR_HEIGHTS_ERROR,
                    String.format("failed to update heights to DB for ( product: %s, blockchain id: %s )", product, blockchainId),
                    e
            );
        }
    }

    @Override
    public void saveBlockchainMeta(BlockchainMeta blockchainMeta) {
        try {
            blockchainService.getBaseMapper().insertBlockchain(
                    blockchainMeta.getProduct(),
                    blockchainMeta.getBlockchainId(),
                    blockchainMeta.getAlias(),
                    blockchainMeta.getDesc(),
                    blockchainMeta.getProperties().encode()
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    String.format(
                            "failed to insert blockchain meta to DB for ( product: %s, blockchain id: %s )",
                            blockchainMeta.getProduct(), blockchainMeta.getBlockchainId()
                    ), e
            );
        }
    }

    @Override
    public boolean updateBlockchainMeta(BlockchainMeta blockchainMeta) {
        return blockchainService.getBaseMapper().update(
                BlockchainEntity.builder()
                        .alias(blockchainMeta.getAlias())
                        .desc(blockchainMeta.getDesc())
                        .properties(blockchainMeta.getProperties().encode())
                        .build(),
                new LambdaUpdateWrapper<BlockchainEntity>()
                        .eq(BlockchainEntity::getBlockchainId, "test")
        ) == 1;
    }

    @Override
    public List<BlockchainMeta> getAllBlockchainMeta() {
        return blockchainService.list().stream()
                .map(ConvertUtil::convertFromBlockchainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public BlockchainMeta getBlockchainMeta(String product, String blockchainId) {
        BlockchainEntity blockchainEntity = blockchainService.lambdaQuery()
                .eq(BlockchainEntity::getProduct, product)
                .eq(BlockchainEntity::getBlockchainId, blockchainId)
                .select(
                        ListUtil.of(
                                BlockchainEntity::getProduct,
                                BlockchainEntity::getBlockchainId,
                                BlockchainEntity::getAlias,
                                BlockchainEntity::getDesc,
                                BlockchainEntity::getProperties
                        )
                ).one();
        if (ObjectUtil.isNull(blockchainEntity)) {
            return null;
        }
        return ConvertUtil.convertFromBlockchainEntity(blockchainEntity);
    }

    private void flushAnchorProcessHeights(AnchorProcessHeights heights) {
        for (Map.Entry<String, Long> entry : heights.getProcessHeights().entrySet()) {
            flushHeight(heights.getProduct(), heights.getBlockchainId(), entry.getKey(), entry.getValue());
        }
    }

    private void flushHeight(String product, String blockchainId, String heightType, Long height) {
        if (
                !anchorProcessService.lambdaUpdate()
                        .set(AnchorProcessEntity::getBlockHeight, height)
                        .set(BaseEntity::getGmtModified, new Date())
                        .eq(AnchorProcessEntity::getProduct, product)
                        .eq(AnchorProcessEntity::getBlockchainId, blockchainId)
                        .eq(AnchorProcessEntity::getTask, heightType)
                        .update()
        ) {
            throw new RuntimeException(String.format("update ( height type: %s, value: %d ) failed", heightType, height));
        }
    }

    private Long getAnchorProcessHeightFromDB(String product, String blockchainId, String heightType) {
        return anchorProcessService.lambdaQuery()
                .select(true, ListUtil.of(AnchorProcessEntity::getBlockHeight))
                .eq(AnchorProcessEntity::getProduct, product)
                .eq(AnchorProcessEntity::getBlockchainId, blockchainId)
                .eq(AnchorProcessEntity::getTask, heightType)
                .oneOpt()
                .map(AnchorProcessEntity::getBlockHeight).orElse(0L);
    }

    private Long getHeightFromCache(String product, String blockchainId, String heightKey) {
        AnchorProcessHeights heights = getAnchorProcessHeightsFromCache(product, blockchainId);
        if (ObjectUtil.isNull(heights)) {
            return null;
        }
        return heights.getProcessHeights().getOrDefault(heightKey, null);
    }

    private AnchorProcessHeights getAnchorProcessHeightsFromCache(String product, String blockchainId) {
        RBucket<byte[]> bucket = redissonClient.getBucket(AnchorProcessHeights.getKey(product, blockchainId), ByteArrayCodec.INSTANCE);
        byte[] rawHeights = bucket.get();
        if (ObjectUtil.isEmpty(rawHeights)) {
            return null;
        }
        return AnchorProcessHeights.decode(rawHeights);
    }

    private void setHeightToCache(String product, String blockchainId, String heightKey, Long height) {
        AnchorProcessHeights heights = getAnchorProcessHeightsFromCache(product, blockchainId);
        if (ObjectUtil.isNull(heights)) {
            heights = new AnchorProcessHeights(product, blockchainId);
            heights.getProcessHeights().put(heightKey, height);
        }
        setAnchorProcessHeightsToCache(heights);
    }

    private void setAnchorProcessHeightsToCache(AnchorProcessHeights heights) {
        redissonClient.getBucket(AnchorProcessHeights.getKey(heights.getProduct(), heights.getBlockchainId()), ByteArrayCodec.INSTANCE)
                .set(heights.encode(), Duration.of(ttlForHeightsCache, ChronoUnit.MILLIS));
    }
}
