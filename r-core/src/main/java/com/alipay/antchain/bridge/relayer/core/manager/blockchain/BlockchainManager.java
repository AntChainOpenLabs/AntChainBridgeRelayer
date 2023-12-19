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
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.OnChainServiceStatusEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.UpperProtocolTypeBeyondAMEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AnchorProcessHeights;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.commons.model.DomainCertWrapper;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlockchainClient;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.BlockchainAnchorProcess;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.BlockchainClientPool;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.HeteroBlockchainClient;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class BlockchainManager implements IBlockchainManager {

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
            if (ObjectUtil.isNull(blockchainProperties)) {
                throw new RuntimeException(
                        StrUtil.format("null blockchain properties from client config : {}", JSON.toJSONString(clientConfig))
                );
            }
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

            log.info("[BlockchainManager] add blockchain {} success", blockchainMeta.getMetaKey());

        } catch (AntChainBridgeRelayerException e) {
            blockchainClientPool.deleteClient(blockchainMeta.getProduct(), blockchainMeta.getBlockchainId());
            throw e;
        } catch (Exception e) {
            blockchainClientPool.deleteClient(blockchainMeta.getProduct(), blockchainMeta.getBlockchainId());
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                    e,
                    "failed to add new blockchain {} with plugin server {}",
                    blockchainMeta.getMetaKey(), blockchainMeta.getProperties().getPluginServerId()
            );
        }
    }

    @Override
    public void updateBlockchain(String product, String blockchainId, String pluginServerId, String alias, String desc, Map<String, String> clientConfig) {
        try {
            BlockchainMeta blockchainMeta = getBlockchainMeta(product, blockchainId);
            if (ObjectUtil.isNull(blockchainMeta)) {
                throw new RuntimeException(StrUtil.format("none blockchain found for {}-{}", product, blockchainId));
            }

            if (blockchainMeta.isRunning()) {
                AbstractBlockchainClient client = blockchainClientPool.createClient(blockchainMeta);
                if (ObjectUtil.isNull(client)) {
                    throw new AntChainBridgeRelayerException(
                            RelayerErrorCodeEnum.CORE_BLOCKCHAIN_CLIENT_INIT_ERROR,
                            "null blockchain client for {}-{}", product, blockchainId
                    );
                }
            }

            BlockchainMeta.BlockchainProperties blockchainProperties = BlockchainMeta.BlockchainProperties.decode(
                    JSON.toJSONBytes(clientConfig)
            );
            if (ObjectUtil.isNull(blockchainProperties)) {
                throw new RuntimeException(StrUtil.format("none blockchain properties built for {}-{}", product, blockchainId));
            }

            blockchainMeta.setAlias(alias);
            blockchainMeta.setDesc(desc);
            blockchainMeta.updateProperties(blockchainProperties);
            if (updateBlockchainMeta(new BlockchainMeta(product, blockchainId, alias, desc, blockchainProperties))) {
                throw new RuntimeException(
                        StrUtil.format(
                                "failed to update meta for blockchain {} - {} into DB",
                                product, blockchainId
                        )
                );
            }


        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                    e,
                    "failed to add new blockchain {} - {} with plugin server {}",
                    product, blockchainId
            );
        }
    }

    @Override
    public void updateBlockchainProperty(String product, String blockchainId, String confKey, String confValue) {
        log.info("update property (key: {}, val: {}) for blockchain {} - {}", confKey, confValue, product, blockchainId);

        try {
            BlockchainMeta blockchainMeta = getBlockchainMeta(product, blockchainId);
            if (ObjectUtil.isNull(blockchainMeta)) {
                throw new AntChainBridgeRelayerException(
                        RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                        "none blockchain found for {}-{}", product, blockchainId
                );
            }

            blockchainMeta.updateProperty(confKey, confValue);
            if (!updateBlockchainMeta(blockchainMeta)) {
                throw new AntChainBridgeRelayerException(
                        RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                        "failed to update property (key: {}, val: {}) for blockchain {} - {} into DB",
                        confKey, confValue, product, blockchainId
                );
            }
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.UNKNOWN_INTERNAL_ERROR,
                    e,
                    "failed to update property (key: {}, val: {}) for blockchain {} - {} with unknown exception",
                    confKey, confValue, product, blockchainId
            );
        }
        log.info("successful to update property (key: {}, val: {}) for blockchain {} - {}", confKey, confValue, product, blockchainId);
    }

    @Override
    public boolean hasBlockchain(String domain) {
        return blockchainRepository.hasBlockchain(domain);
    }

    @Override
    public DomainCertWrapper getDomainCert(String domain) {
        return blockchainRepository.getDomainCert(domain);
    }

    @Override
    public void deployAMClientContract(String product, String blockchainId) {
        try {
            BlockchainMeta blockchainMeta = this.getBlockchainMeta(product, blockchainId);
            if (ObjectUtil.isNull(blockchainMeta)) {
                throw new RuntimeException(StrUtil.format("none blockchain found for {}-{}", product, blockchainId));
            }
            deployHeteroBlockchainAMContract(blockchainMeta);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                    e,
                    "failed to deploy am contract for blockchain {} - {}",
                    product, blockchainId
            );
        }
    }

    @Override
    @Transactional(rollbackFor = AntChainBridgeRelayerException.class)
    public void deployBBCContractAsync(String product, String blockchainId) {
        try {
            BlockchainMeta blockchainMeta = getBlockchainMeta(product, blockchainId);
            if (ObjectUtil.isNull(blockchainMeta)) {
                throw new RuntimeException(StrUtil.format("blockchain not found : {}-{}", product, blockchainId));
            }

            if (ObjectUtil.isNull(blockchainMeta.getProperties().getAmServiceStatus())) {
                blockchainMeta.getProperties().setAmServiceStatus(OnChainServiceStatusEnum.INIT);
                updateBlockchainMeta(blockchainMeta);
            }
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                    e,
                    "failed to mark blockchain {} - {} to deploy BBC contracts",
                    product, blockchainId
            );
        }
    }

    @Override
    public void startBlockchainAnchor(String product, String blockchainId) {
        log.info("start blockchain anchor {} - {}", product, blockchainId);

        try {
            BlockchainMeta blockchainMeta = this.getBlockchainMeta(product, blockchainId);
            if (ObjectUtil.isNull(blockchainMeta)) {
                throw new RuntimeException(StrUtil.format("none blockchain found for {}-{}", product, blockchainId));
            }

            // 已启动的不重复启动
            if (blockchainMeta.isRunning()) {
                return;
            }

            blockchainMeta.getProperties().setAnchorRuntimeStatus(BlockchainStateEnum.RUNNING);
            if (!updateBlockchainMeta(blockchainMeta)) {
                throw new RuntimeException(
                        StrUtil.format(
                                "failed to update meta for blockchain {} - {} into DB",
                                product, blockchainId
                        )
                );
            }
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                    e,
                    "failed to start blockchain {} - {}",
                    product, blockchainId
            );
        }
    }

    @Override
    public void stopBlockchainAnchor(String product, String blockchainId) {
        log.info("stop blockchain anchor {} - {}", product, blockchainId);

        try {
            BlockchainMeta blockchainMeta = this.getBlockchainMeta(product, blockchainId);
            if (ObjectUtil.isNull(blockchainMeta)) {
                throw new RuntimeException(StrUtil.format("none blockchain found for {}-{}", product, blockchainId));
            }

            // 已启动的不重复启动
            if (BlockchainStateEnum.STOPPED == blockchainMeta.getProperties().getAnchorRuntimeStatus()) {
                return;
            }

            blockchainMeta.getProperties().setAnchorRuntimeStatus(BlockchainStateEnum.STOPPED);
            if (!updateBlockchainMeta(blockchainMeta)) {
                throw new RuntimeException(
                        StrUtil.format(
                                "failed to update meta for blockchain {} - {} into DB",
                                product, blockchainId
                        )
                );
            }
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                    e,
                    "failed to stop blockchain {} - {}",
                    product, blockchainId
            );
        }
    }

    @Override
    public BlockchainMeta getBlockchainMeta(String product, String blockchainId) {
        return blockchainRepository.getBlockchainMeta(product, blockchainId);
    }

    @Override
    public BlockchainMeta getBlockchainMetaByDomain(String domain) {
        return blockchainRepository.getBlockchainMetaByDomain(domain);
    }

    @Override
    public String getBlockchainDomain(String product, String blockchainId) {
        return blockchainRepository.getBlockchainDomain(product, blockchainId);
    }

    @Override
    public boolean updateBlockchainMeta(BlockchainMeta blockchainMeta) {
        return blockchainRepository.updateBlockchainMeta(blockchainMeta);
    }

    @Override
    public List<BlockchainMeta> getAllBlockchainMeta() {
        return blockchainRepository.getAllBlockchainMeta();
    }

    @Override
    public BlockchainAnchorProcess getBlockchainAnchorProcess(String product, String blockchainId) {
        AnchorProcessHeights heights = blockchainRepository.getAnchorProcessHeights(product, blockchainId);
        if (ObjectUtil.isNull(heights)) {
            log.error("null heights for blockchain {}-{}", product, blockchainId);
            return null;
        }
        return BlockchainAnchorProcess.convertFrom(heights);
    }

    @Override
    public List<BlockchainMeta> getAllServingBlockchains() {
        try {
            return blockchainRepository.getBlockchainMetaByState(BlockchainStateEnum.RUNNING);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                    e,
                    "failed to query all serving blockchains"
            );
        }
    }

    @Override
    public List<BlockchainMeta> getAllStoppedBlockchains() {
        try {
            return blockchainRepository.getBlockchainMetaByState(BlockchainStateEnum.STOPPED);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                    e,
                    "failed to query all stopped blockchains"
            );
        }
    }

    @Override
    public boolean checkIfDomainPrepared(String domain) {
        try {
            BlockchainMeta blockchainMeta = blockchainRepository.getBlockchainMetaByDomain(domain);
            if (ObjectUtil.isNull(blockchainMeta)) {
                return false;
            }
        } catch (Exception e) {
            log.error("failed to query blockchain by domain {}", domain, e);
            return false;
        }
        return true;
    }

    @Override
    public boolean checkIfDomainRunning(String domain) {
        try {
            BlockchainMeta blockchainMeta = blockchainRepository.getBlockchainMetaByDomain(domain);
            if (ObjectUtil.isNull(blockchainMeta)) {
                return false;
            }
            return blockchainMeta.isRunning();
        } catch (Exception e) {
            log.error("failed to query blockchain by domain {}", domain, e);
            return false;
        }
    }

    @Override
    public boolean checkIfDomainAMDeployed(String domain) {
        try {
            BlockchainMeta blockchainMeta = blockchainRepository.getBlockchainMetaByDomain(domain);
            if (ObjectUtil.isNull(blockchainMeta)) {
                return false;
            }
            return OnChainServiceStatusEnum.DEPLOY_FINISHED == blockchainMeta.getProperties().getAmServiceStatus();
        } catch (Exception e) {
            log.error("failed to query blockchain by domain {}", domain, e);
            return false;
        }
    }

    @Override
    public List<String> getBlockchainsByPluginServerId(String pluginServerId) {
        try {
            return blockchainRepository.getBlockchainMetaByPluginServerId(pluginServerId).stream()
                    .map(BlockchainMeta::getBlockchainId)
                    .collect(Collectors.toList());
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                    e,
                    "failed to query blockchains by plugin server id {}"
            );
        }
    }

    @Override
    public void updateSDPMsgSeq(String receiverProduct, String receiverBlockchainId, String senderDomain, String from, String to, long newSeq) {
        throw new AntChainBridgeRelayerException(
                RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                "update SDP msg seq not supported for now"
        );
    }

    @Override
    public long querySDPMsgSeq(String receiverProduct, String receiverBlockchainId, String senderDomain, String from, String to) {
        try {
            AbstractBlockchainClient blockchainClient = blockchainClientPool.getClient(receiverProduct, receiverBlockchainId);
            if (ObjectUtil.isNull(blockchainClient)) {
                throw new AntChainBridgeRelayerException(
                        RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                        "failed to get blockchain client for blockchain ( product: {}, bcId: {})",
                        receiverProduct, receiverBlockchainId
                );
            }

            return blockchainClient.getSDPMsgClientContract().querySDPMsgSeqOnChain(
                    senderDomain,
                    from,
                    blockchainClient.getDomain(),
                    to
            );
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                    e,
                    "failed to query blockchains by plugin server id {}"
            );
        }
    }

    private boolean isBlockchainExists(String product, String blockchainId) {
        return blockchainRepository.hasBlockchain(product, blockchainId);
    }

    private void deployHeteroBlockchainAMContract(BlockchainMeta blockchainMeta) {
        AbstractBlockchainClient blockchainClient = blockchainClientPool.getClient(
                blockchainMeta.getProduct(),
                blockchainMeta.getBlockchainId()
        );

        if (ObjectUtil.isNull(blockchainClient)) {
            blockchainClient = blockchainClientPool.createClient(blockchainMeta);
        }

        if (!(blockchainClient instanceof HeteroBlockchainClient)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BLOCKCHAIN_ERROR,
                    "wrong type of client for blockchain ( product: {}, bcId: {}, pluginServer: {})",
                    blockchainMeta.getProduct(), blockchainMeta.getBlockchainId(), blockchainMeta.getPluginServerId()
            );
        }

        AbstractBBCContext bbcContext = blockchainClient.queryBBCContext();

        if (
                ObjectUtil.isNull(bbcContext.getAuthMessageContract())
                        || ContractStatusEnum.INIT == bbcContext.getAuthMessageContract().getStatus()
        ) {
            blockchainClient.getAMClientContract().deployContract();
        }

        if (
                ObjectUtil.isNull(bbcContext.getSdpContract())
                        || ContractStatusEnum.INIT == bbcContext.getSdpContract().getStatus()
        ) {
            blockchainClient.getSDPMsgClientContract().deployContract();
        }

        bbcContext = blockchainClient.queryBBCContext();
        blockchainClient.getAMClientContract()
                .setProtocol(
                        bbcContext.getSdpContract().getContractAddress(),
                        Integer.toString(UpperProtocolTypeBeyondAMEnum.SDP.getCode())
                );

        blockchainClient.getSDPMsgClientContract()
                .setAmContract(bbcContext.getAuthMessageContract().getContractAddress());

        bbcContext = blockchainClient.queryBBCContext();

        blockchainMeta.getProperties().setAmClientContractAddress(
                bbcContext.getAuthMessageContract().getContractAddress()
        );
        blockchainMeta.getProperties().setSdpMsgContractAddress(
                bbcContext.getSdpContract().getContractAddress()
        );
        blockchainMeta.getProperties().setBbcContext(
                (DefaultBBCContext) bbcContext
        );

        blockchainClient.setBlockchainMeta(blockchainMeta);
        updateBlockchainMeta(blockchainMeta);
    }
}
