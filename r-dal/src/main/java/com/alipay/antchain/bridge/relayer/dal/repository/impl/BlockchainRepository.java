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

import cn.hutool.cache.Cache;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AnchorProcessHeights;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.commons.model.DomainCertWrapper;
import com.alipay.antchain.bridge.relayer.dal.entities.AnchorProcessEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.BaseEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.BlockchainEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.DomainCertEntity;
import com.alipay.antchain.bridge.relayer.dal.mapper.DomainCertMapper;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import com.alipay.antchain.bridge.relayer.dal.service.BlockchainService;
import com.alipay.antchain.bridge.relayer.dal.service.IAnchorProcessService;
import com.alipay.antchain.bridge.relayer.dal.utils.ConvertUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.ByteArrayCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BlockchainRepository implements IBlockchainRepository {

    @Resource
    private IAnchorProcessService anchorProcessService;

    @Resource
    private BlockchainService blockchainService;

    @Resource
    private DomainCertMapper domainCertMapper;

    @Resource
    private RedissonClient redisson;

    @Value("${anchor.process.cache.heights.ttl:240000}")
    private long ttlForHeightsCache;

    @Value("${anchor.process.cache.flush.period:0}")
    private long flushPeriodForHeightsCache;

    @Resource
    private Cache<String, DomainCertWrapper> domainCertWrapperCache;

    @Resource
    private Cache<String, BlockchainMeta> blockchainMetaCache;

    @Resource(name = "blockchainIdToDomainCache")
    private Cache<String, String> blockchainIdToDomainCache;

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
    public AnchorProcessHeights getAnchorProcessHeights(String product, String blockchainId) {
        try {
            AnchorProcessHeights heights = getAnchorProcessHeightsFromCache(product, blockchainId);
            if (ObjectUtil.isNull(heights)) {
                List<AnchorProcessEntity> entities = anchorProcessService.lambdaQuery()
                        .select(
                                ListUtil.toList(
                                        AnchorProcessEntity::getTask,
                                        AnchorProcessEntity::getBlockHeight,
                                        BaseEntity::getGmtModified
                                )
                        ).eq(AnchorProcessEntity::getProduct, product)
                        .eq(AnchorProcessEntity::getBlockchainId, blockchainId)
                        .list();
                if (ObjectUtil.isEmpty(entities)) {
                    return null;
                }
                heights = new AnchorProcessHeights(product, blockchainId);
                for (AnchorProcessEntity entity : entities) {
                    heights.getProcessHeights().put(entity.getTask(), entity.getBlockHeight());
                    heights.getModifiedTimeMap().put(entity.getTask(), entity.getGmtModified().getTime());
                }

                return heights;
            }

            for (Map.Entry<String, Long> entry : heights.getProcessHeights().entrySet()) {
                heights.getModifiedTimeMap().put(entry.getKey(), heights.getLastUpdateTime());
            }
            return heights;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_ANCHOR_HEIGHTS_ERROR,
                    e,
                    "failed to get heights for ( product: {}, blockchain id: {} )",
                    product, blockchainId
            );
        }
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
            blockchainMetaCache.put(blockchainMeta.getBlockchainId(), blockchainMeta);
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
        if (
                blockchainService.getBaseMapper().update(
                        BlockchainEntity.builder()
                                .alias(blockchainMeta.getAlias())
                                .desc(blockchainMeta.getDesc())
                                .properties(blockchainMeta.getProperties().encode())
                                .build(),
                        new LambdaUpdateWrapper<BlockchainEntity>()
                                .eq(BlockchainEntity::getBlockchainId, "test")
                ) == 1
        ) {
            blockchainMetaCache.put(blockchainMeta.getBlockchainId(), blockchainMeta);
            return true;
        }
        return false;
    }

    @Override
    public List<BlockchainMeta> getAllBlockchainMeta() {
        return blockchainService.list().stream()
                .map(ConvertUtil::convertFromBlockchainEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BlockchainMeta> getBlockchainMetaByState(BlockchainStateEnum state) {
        try {
            List<BlockchainEntity> blockchainEntities = blockchainService.lambdaQuery()
                    .like(BlockchainEntity::getProperties, state.getCode())
                    .list();
            if (ObjectUtil.isEmpty(blockchainEntities)) {
                return ListUtil.empty();
            }
            return blockchainEntities.stream()
                    .map(ConvertUtil::convertFromBlockchainEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e,
                    "failed to query blockchains from DB with state {}", state.getCode()
            );
        }
    }

    @Override
    public BlockchainMeta getBlockchainMetaByDomain(String domain) {
        try {
            if (blockchainMetaCache.containsKey(getDomainBlockchainMetaCacheKey(domain))) {
                return blockchainMetaCache.get(getDomainBlockchainMetaCacheKey(domain));
            }

            BlockchainEntity blockchainEntity = blockchainService.getBaseMapper().queryBlockchainByDomain(domain);
            if (ObjectUtil.isNull(blockchainEntity)) {
                return null;
            }
            BlockchainMeta blockchainMeta = ConvertUtil.convertFromBlockchainEntity(blockchainEntity);
            blockchainMetaCache.put(getDomainBlockchainMetaCacheKey(domain), blockchainMeta);
            if (StrUtil.isAllNotEmpty(domain, blockchainEntity.getBlockchainId())) {
                blockchainIdToDomainCache.put(blockchainEntity.getBlockchainId(), domain);
            }
            return blockchainMeta;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e,
                    "failed to query blockchains from DB with domain {}", domain
            );
        }
    }

    @Override
    public boolean hasBlockchain(String domain) {
        String blockchainId;
        if (domainCertWrapperCache.containsKey(domain)) {
            blockchainId = domainCertWrapperCache.get(domain).getBlockchainId();
        } else {
            DomainCertEntity domainCertEntity = domainCertMapper.selectOne(
                    new LambdaQueryWrapper<DomainCertEntity>()
                            .select(ListUtil.toList(DomainCertEntity::getBlockchainId))
                            .eq(DomainCertEntity::getDomain, domain)
            );
            if (ObjectUtil.isNull(domainCertEntity)) {
                return false;
            }
            blockchainId = domainCertEntity.getBlockchainId();
        }

        if (StrUtil.isAllNotEmpty(domain, blockchainId)) {
            blockchainIdToDomainCache.put(blockchainId, domain);
        }

        if (blockchainMetaCache.containsKey(getDomainBlockchainMetaCacheKey(domain))) {
            return true;
        }

        return blockchainService.getBaseMapper().exists(
                new LambdaQueryWrapper<BlockchainEntity>()
                        .eq(BlockchainEntity::getBlockchainId, blockchainId)
        );
    }

    @Override
    public List<BlockchainMeta> getBlockchainMetaByPluginServerId(String pluginServerId) {
        try {
            List<BlockchainEntity> blockchainEntities = blockchainService.lambdaQuery()
                    .like(BlockchainEntity::getProperties, pluginServerId)
                    .list();
            if (ObjectUtil.isEmpty(blockchainEntities)) {
                return ListUtil.empty();
            }
            return blockchainEntities.stream()
                    .map(ConvertUtil::convertFromBlockchainEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e,
                    "failed to query blockchains from DB with plugin server id {}", pluginServerId
            );
        }
    }

    @Override
    public BlockchainMeta getBlockchainMeta(String product, String blockchainId) {
        if (blockchainMetaCache.containsKey(blockchainId)) {
            return blockchainMetaCache.get(blockchainId);
        }

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
        BlockchainMeta blockchainMeta = ConvertUtil.convertFromBlockchainEntity(blockchainEntity);
        blockchainMetaCache.put(blockchainId, blockchainMeta);
        return blockchainMeta;
    }

    @Override
    public boolean hasBlockchain(String product, String blockchainId) {
        try {
            if (blockchainMetaCache.containsKey(blockchainId)) {
                return true;
            }

            return blockchainService.exists(
                    new LambdaQueryWrapper<BlockchainEntity>()
                            .eq(BlockchainEntity::getProduct, product)
                            .eq(BlockchainEntity::getBlockchainId, blockchainId)
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    String.format(
                            "failed to query blockchain existence from DB for ( product: %s, blockchain id: %s )",
                            product, blockchainId
                    ), e
            );
        }
    }

    @Override
    public String getBlockchainDomain(String product, String blockchainId) {
        try {
            if (blockchainIdToDomainCache.containsKey(blockchainId)) {
                return blockchainIdToDomainCache.get(blockchainId);
            }
            DomainCertEntity entity = domainCertMapper.selectOne(
                    new LambdaQueryWrapper<DomainCertEntity>()
                            .eq(DomainCertEntity::getProduct, product)
                            .eq(DomainCertEntity::getBlockchainId, blockchainId)
            );
            if (ObjectUtil.isNull(entity)) {
                return "";
            }

            blockchainIdToDomainCache.put(blockchainId, entity.getDomain());

            return entity.getDomain();
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    String.format(
                            "failed to query blockchain existence from DB for ( product: %s, blockchain id: %s )",
                            product, blockchainId
                    ), e
            );
        }
    }

    @Override
    @Transactional
    public List<String> getBlockchainDomainsByState(BlockchainStateEnum state) {
        try {
            List<BlockchainEntity> blockchainEntities = blockchainService.lambdaQuery()
                    .select(ListUtil.toList(BlockchainEntity::getBlockchainId))
                    .like(BlockchainEntity::getProperties, state.getCode())
                    .list();
            if (ObjectUtil.isEmpty(blockchainEntities)) {
                return ListUtil.empty();
            }

            List<DomainCertEntity> domainCertEntities = domainCertMapper.selectList(
                    new LambdaQueryWrapper<DomainCertEntity>()
                            .select(ListUtil.toList(DomainCertEntity::getDomain))
                            .in(
                                    DomainCertEntity::getBlockchainId,
                                    blockchainEntities.stream()
                                            .map(BlockchainEntity::getBlockchainId)
                                            .collect(Collectors.toList())
                            )
            );
            if (ObjectUtil.isEmpty(domainCertEntities)) {
                return ListUtil.empty();
            }

            return domainCertEntities.stream()
                    .map(DomainCertEntity::getDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e,
                    "failed to query blockchain domain list from DB for state {}",
                    state.getCode()
            );
        }
    }

    @Override
    public DomainCertWrapper getDomainCert(String domain) {
        try {
            if (domainCertWrapperCache.containsKey(domain)) {
                return domainCertWrapperCache.get(domain);
            }

            DomainCertEntity entity = domainCertMapper.selectOne(
                    new LambdaQueryWrapper<DomainCertEntity>()
                            .eq(DomainCertEntity::getDomain, domain)
            );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }

            DomainCertWrapper domainCertWrapper = ConvertUtil.convertFromDomainCertEntity(entity);
            domainCertWrapperCache.put(domain, domainCertWrapper);

            return domainCertWrapper;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BLOCKCHAIN_ERROR,
                    e,
                    "failed to query domain cert from DB for domain {}",
                    domain
            );
        }
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
        RBucket<byte[]> bucket = redisson.getBucket(AnchorProcessHeights.getKey(product, blockchainId), ByteArrayCodec.INSTANCE);
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
        redisson.getBucket(AnchorProcessHeights.getKey(heights.getProduct(), heights.getBlockchainId()), ByteArrayCodec.INSTANCE)
                .set(heights.encode(), Duration.of(ttlForHeightsCache, ChronoUnit.MILLIS));
    }

    private String getDomainBlockchainMetaCacheKey(String domain) {
        return "%domain%" + domain;
    }
}
