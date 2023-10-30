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

package com.alipay.antchain.bridge.relayer.bootstrap.repo;

import java.util.List;
import javax.annotation.Resource;

import com.alipay.antchain.bridge.relayer.bootstrap.TestBase;
import com.alipay.antchain.bridge.relayer.bootstrap.basic.BlockchainModelsTest;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.dal.entities.DomainCertEntity;
import com.alipay.antchain.bridge.relayer.dal.mapper.DomainCertMapper;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import org.junit.Assert;
import org.junit.Test;

public class BlockchainRepositoryTest extends TestBase {

    private static final BlockchainMeta.BlockchainProperties blockchainProperties
            = BlockchainMeta.BlockchainProperties.decode(BlockchainModelsTest.BLOCKCHAIN_META_EXAMPLE_OBJ.getBytes());

    private static final BlockchainMeta testchain1Meta = new BlockchainMeta("testchain", "testchain_1.id", "", "", blockchainProperties);

    @Resource
    private IBlockchainRepository blockchainRepository;

    @Resource
    private DomainCertMapper domainCertMapper;

    private boolean alreadySaveSomeBlockchains;

    private void saveSomeBlockchains() {
        if (alreadySaveSomeBlockchains) {
            return;
        }

        blockchainRepository.saveBlockchainMeta(testchain1Meta);
        DomainCertEntity entity = new DomainCertEntity();
        entity.setBlockchainId(testchain1Meta.getBlockchainId());
        entity.setProduct(testchain1Meta.getProduct());
        entity.setDomain("testchain1");
        domainCertMapper.insert(entity);

        alreadySaveSomeBlockchains = true;
    }

    @Test
    public void testSaveBlockchainMeta() {
        BlockchainMeta blockchainMeta = new BlockchainMeta("testchain", "testchain.id", "", "", blockchainProperties);
        blockchainRepository.saveBlockchainMeta(blockchainMeta);
        Assert.assertThrows(AntChainBridgeRelayerException.class, () -> blockchainRepository.saveBlockchainMeta(blockchainMeta));
    }

    @Test
    public void testGetAllBlockchainMetaByState() {
        saveSomeBlockchains();
        List<BlockchainMeta> result = blockchainRepository.getBlockchainMetaByState(BlockchainStateEnum.RUNNING);
        Assert.assertTrue(result.size() >= 1);
    }

    @Test
    public void testGetBlockchainMetaByDomain() {
        saveSomeBlockchains();
        BlockchainMeta blockchainMeta = blockchainRepository.getBlockchainMetaByDomain("testchain1");
        Assert.assertNotNull(blockchainMeta);
        Assert.assertEquals(testchain1Meta.getBlockchainId(), blockchainMeta.getBlockchainId());
    }
}
