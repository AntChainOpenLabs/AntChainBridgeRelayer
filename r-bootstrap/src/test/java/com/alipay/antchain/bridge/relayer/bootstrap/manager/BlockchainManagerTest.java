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

package com.alipay.antchain.bridge.relayer.bootstrap.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.relayer.bootstrap.TestBase;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.Constants;
import com.alipay.antchain.bridge.relayer.commons.constant.OnChainServiceStatusEnum;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.commons.model.DomainCertWrapper;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.core.types.pluginserver.IBBCServiceClient;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class BlockchainManagerTest extends TestBase {

    public static String psId = "myps";

    @Resource
    private IBlockchainManager blockchainManager;

    @Resource
    private IBCDNSManager bcdnsManager;

    @Resource
    private IBlockchainRepository blockchainRepository;

    @Mock
    public IBBCServiceClient ibbcServiceClient;

    @Test
    public void testAddBlockchain() {
        initAntChainDotCom();

        Assert.assertTrue(blockchainManager.hasBlockchain(antChainDotComDomain));

        BlockchainMeta blockchainMeta = blockchainManager.getBlockchainMetaByDomain(antChainDotComDomain);
        Assert.assertEquals(antChainDotComProduct, blockchainMeta.getProduct());
        Assert.assertEquals(antChainDotComBlockchainId, blockchainMeta.getBlockchainId());
        Assert.assertEquals(psId, blockchainMeta.getPluginServerId());

        Assert.assertNotNull(blockchainManager.getBlockchainMeta(antChainDotComProduct, antChainDotComBlockchainId));
    }

    @Test
    public void testUpdateBlockchainProperty() {
        initAntChainDotCom();

        blockchainManager.updateBlockchainProperty(
                antChainDotComProduct,
                antChainDotComBlockchainId,
                Constants.AM_SERVICE_STATUS,
                OnChainServiceStatusEnum.INIT.name()
        );
        BlockchainMeta blockchainMeta = blockchainManager.getBlockchainMeta(antChainDotComProduct, antChainDotComBlockchainId);
        Assert.assertNotNull(blockchainMeta);
        Assert.assertEquals(OnChainServiceStatusEnum.INIT, blockchainMeta.getProperties().getAmServiceStatus());
    }

    @Test
    public void testDeployAMClientContract() {
        initAntChainDotCom();
        blockchainManager.deployAMClientContract(antChainDotComProduct, antChainDotComBlockchainId);
        BlockchainMeta blockchainMeta = blockchainManager.getBlockchainMeta(antChainDotComProduct, antChainDotComBlockchainId);
        Assert.assertEquals(
                blockchainProperties1.getBbcContext().getAuthMessageContract().getContractAddress(),
                blockchainMeta.getProperties().getAmClientContractAddress()
        );
    }

    @Test
    public void testStartBlockchainAnchor() {
        initAntChainDotCom();
        blockchainManager.startBlockchainAnchor(antChainDotComProduct, antChainDotComBlockchainId);
        BlockchainMeta blockchainMeta = blockchainManager.getBlockchainMeta(antChainDotComProduct, antChainDotComBlockchainId);
        Assert.assertEquals(
                BlockchainStateEnum.RUNNING,
                blockchainMeta.getProperties().getAnchorRuntimeStatus()
        );
    }

    @Test
    public void testStopBlockchainAnchor() {
        initAntChainDotCom();
        blockchainManager.stopBlockchainAnchor(antChainDotComProduct, antChainDotComBlockchainId);
        BlockchainMeta blockchainMeta = blockchainManager.getBlockchainMeta(antChainDotComProduct, antChainDotComBlockchainId);
        Assert.assertEquals(
                BlockchainStateEnum.STOPPED,
                blockchainMeta.getProperties().getAnchorRuntimeStatus()
        );
    }

    @Test
    public void testGetAllServingBlockchains() {
        initAntChainDotCom();
        initCatChainDotCom();
        blockchainManager.startBlockchainAnchor(antChainDotComProduct, antChainDotComBlockchainId);
        blockchainManager.startBlockchainAnchor(catChainDotComProduct, catChainDotComBlockchainId);

        List<BlockchainMeta> blockchainMetas = blockchainManager.getAllServingBlockchains();
        Assert.assertEquals(2, blockchainMetas.size());

        blockchainManager.stopBlockchainAnchor(antChainDotComProduct, antChainDotComBlockchainId);

        blockchainMetas = blockchainManager.getAllServingBlockchains();
        Assert.assertEquals(1, blockchainMetas.size());
    }

    @Test
    public void testGetAllStoppedBlockchains() {
        initAntChainDotCom();
        initCatChainDotCom();
        blockchainManager.startBlockchainAnchor(antChainDotComProduct, antChainDotComBlockchainId);
        blockchainManager.startBlockchainAnchor(catChainDotComProduct, catChainDotComBlockchainId);

        List<BlockchainMeta> blockchainMetas = blockchainManager.getAllStoppedBlockchains();
        Assert.assertEquals(0, blockchainMetas.size());

        blockchainManager.stopBlockchainAnchor(antChainDotComProduct, antChainDotComBlockchainId);

        blockchainMetas = blockchainManager.getAllStoppedBlockchains();
        Assert.assertEquals(1, blockchainMetas.size());
    }

    private void initDomain() {
        blockchainRepository.saveDomainCert(new DomainCertWrapper(antchainDotCommCert));
    }

    private void initAntChainDotCom() {
        initDomain();

        Mockito.when(
                bbcPluginManager.createBBCClient(
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyString()
                )
        ).thenReturn(ibbcServiceClient);
        Mockito.doNothing().when(ibbcServiceClient).startup(Mockito.any());
        Mockito.doNothing().when(ibbcServiceClient).setupAuthMessageContract();
        Mockito.doNothing().when(ibbcServiceClient).setupSDPMessageContract();
        Mockito.doNothing().when(ibbcServiceClient).setProtocol(Mockito.anyString(), Mockito.anyString());
        Mockito.doNothing().when(ibbcServiceClient).setAmContract(Mockito.anyString());
        Mockito.when(ibbcServiceClient.getContext()).thenReturn(blockchainProperties1.getBbcContext());

        Mockito.when(
                blockchainMetaCache.containsKey(Mockito.anyString())
        ).thenReturn(false);

        Map<String, String> clientConfig = new HashMap<>();
        clientConfig.put(Constants.HETEROGENEOUS_BBC_CONTEXT, JSON.toJSONString(blockchainProperties1.getBbcContext()));
        bcdnsManager.bindDomainCertWithBlockchain(antChainDotComDomain, antChainDotComProduct, antChainDotComBlockchainId);
        blockchainManager.addBlockchain(
                antChainDotComProduct,
                antChainDotComBlockchainId,
                psId,
                "", "",
                clientConfig
        );
    }

    private void initCatChainDotCom() {
        blockchainRepository.saveDomainCert(new DomainCertWrapper(catchainDotCommCert));

        Map<String, String> clientConfig = new HashMap<>();
        clientConfig.put(Constants.HETEROGENEOUS_BBC_CONTEXT, JSON.toJSONString(blockchainProperties2.getBbcContext()));
        bcdnsManager.bindDomainCertWithBlockchain(catChainDotComDomain, catChainDotComProduct, catChainDotComBlockchainId);
        blockchainManager.addBlockchain(
                catChainDotComProduct,
                catChainDotComBlockchainId,
                psId,
                "", "",
                clientConfig
        );
    }
}
