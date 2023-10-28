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

package com.alipay.antchain.bridge.relayer.core.manager.blockchain;

import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlockchainClient;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.BlockchainAnchorProcess;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.BlockchainClientPool;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BlockchainManagerImpl implements IBlockchainManager {

    @Resource
    private IBlockchainRepository blockchainRepository;

    @Resource
    private BlockchainClientPool blockchainClientPool;

    @Override
    public void addBlockchain(
            String product,
            String blockchainId,
            String pluginServerId,
            String alias,
            String desc,
            Map<String, String> clientConfig
    ) {
        try {
            BlockchainMeta.BlockchainProperties blockchainProperties = BlockchainMeta.BlockchainProperties.decode(
                    JSON.toJSONBytes(clientConfig)
            );
            if (ObjectUtil.isNotNull(blockchainProperties.getAnchorRuntimeStatus())) {
                log.warn(
                        "add blockChain information (id : {}) contains anchor runtime status : {} and it will be removed",
                        blockchainId, blockchainProperties.getAnchorRuntimeStatus().getCode()
                );
            }
            blockchainProperties.setAnchorRuntimeStatus(BlockchainStateEnum.INIT);
            blockchainProperties.setPluginServerId(pluginServerId);

            addBlockchain(new BlockchainMeta(product, blockchainId, alias, desc, blockchainProperties));
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                    e,
                    "failed to add new blockchain {} - {} with plugin server {}",
                    product, blockchainId, pluginServerId
            );
        }
    }

    @Override
    public void addBlockchain(BlockchainMeta blockchainMeta) {
        log.info(
                "add blockchain {} - {} with plugin server {}",
                blockchainMeta.getProduct(), blockchainMeta.getBlockchainId(), blockchainMeta.getProperties().getPluginServerId()
        );

        try {
            if (isBlockchainExists(blockchainMeta.getProduct(), blockchainMeta.getBlockchainId())) {
                throw new AntChainBridgeRelayerException(
                        RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                        "blockchain {}-{} already exists",
                        blockchainMeta.getProduct(), blockchainMeta.getBlockchainId()
                );
            }

            // 检查客户端配置是否正确
            AbstractBlockchainClient client = blockchainClientPool.createClient(blockchainMeta);
            /* 记录当前区块链高度为初始锚定高度 */
            blockchainMeta.getProperties().setInitBlockHeight(
                    client.getLastBlockHeight()
            );

            blockchainRepository.saveBlockchainMeta(blockchainMeta);

            log.info("[BlockchainManagerImpl] add blockchain {} success", blockchainMeta.getMetaKey());

        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                    e,
                    "failed to add new blockchain {} with plugin server {}",
                    blockchainMeta.getMetaKey(), blockchainMeta.getProperties().getPluginServerId()
            );
        }
    }

    @Override
    public boolean updateBlockchain(String product, String blockchainId, String alias, String desc, Map<String, String> clientConfig) {
        return false;
    }

    @Override
    public boolean updateBlockchainProperty(String product, String blockchainId, String confKey, String confValue) {
        return false;
    }

    @Override
    public boolean deployOracleContract(String product, String blockchainId) {
        return false;
    }

    @Override
    public boolean updateBlockchain(BlockchainMeta blockchainMeta) {
        return false;
    }

    @Override
    public boolean upgradeOracleContract(String product, String blockchainId) {
        return false;
    }

    @Override
    public boolean upgradeP2PContract(String product, String blockchainId) {
        return false;
    }

    @Override
    public boolean upgradeP2PContract(String product, String blockchainId, String contractType, byte[] code) {
        return false;
    }

    @Override
    public boolean upgradeAMContract(String product, String blockchainId) {
        return false;
    }

    @Override
    public boolean upgradeAMContract(String product, String blockchainId, String contractType, byte[] code) {
        return false;
    }

    @Override
    public boolean upgradeFabricParserContract(String product, String blockchainId) {
        return false;
    }

    @Override
    public boolean upgradeMychainParserContract(String product, String blockchainId) {
        return false;
    }

    @Override
    public boolean setupUDNS(String product, String blockchainId) {
        return false;
    }

    @Override
    public boolean deployOracleAMContract(String product, String blockchainId) {
        return false;
    }

    @Override
    public boolean startBlockchainAnchor(String product, String blockchainId) {
        return false;
    }

    @Override
    public boolean stopBlockchainAnchor(String product, String blockchainId) {
        return false;
    }

    @Override
    public BlockchainMeta getBlockchainMeta(String product, String blockchainId) {
        return blockchainRepository.getBlockchainMeta(product, blockchainId);
    }

    @Override
    public boolean updateBlockchainMeta(BlockchainMeta blockchainMeta) {
        return false;
    }

    @Override
    public List<BlockchainMeta> getAllBlockchainMeta() {
        return null;
    }

    @Override
    public BlockchainAnchorProcess getBlockchainAnchorProcess(String product, String blockchainId) {
        return null;
    }

    @Override
    public List<BlockchainMeta> getAllServingBlockchains() {
        return null;
    }

    @Override
    public boolean checkIfDomainPrepared(String domain) {
        return false;
    }

    @Override
    public boolean checkIfDomainRunning(String domain) {
        return false;
    }

    @Override
    public boolean checkIfDomainAMDeployed(String domain) {
        return false;
    }

    @Override
    public List<String> getBlockchainsByPluginServerId(String pluginServerId) {
        return null;
    }

    @Override
    public void updateSDPMsgSeq(String receiverProduct, String receiverBlockchainId, String senderDomain, String from, String to, long newSeq) {

    }

    @Override
    public long querySDPMsgSeq(String receiverProduct, String receiverBlockchainId, String senderDomain, String from, String to) {
        return 0;
    }

    private boolean isBlockchainExists(String product, String blockchainId) {
        return blockchainRepository.hasBlockchain(product, blockchainId);
    }
}
