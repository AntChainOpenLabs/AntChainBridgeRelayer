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

package com.alipay.antchain.bridge.relayer.server.types.network;

import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.core.types.network.request.RelayerRequestType;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Endpoint Server基类
 */
@Getter
@Setter
public abstract class BaseRelayerServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        BaseRelayerServer.class);

    private IRelayerNetworkManager relayerNetworkManager;

    private boolean isDiscoveryServer;

//    private Receiver receiver;

    public BaseRelayerServer() {

//        ServiceManagerModule serviceManagerModule = ServerContext.getInstance().getServerModule(
//                ServiceManagerModule.class);
//
//        this.oracleManager = serviceManagerModule.getOracleManager();
//        this.relayerNetworkManager = serviceManagerModule.getRelayerNetworkManager();
//
//        OracleServiceCoreServiceModule coreServiceModule = ServerContext.getInstance().getServerModule(
//                OracleServiceCoreServiceModule.class);
//        this.receiver = coreServiceModule.getInnerReceiver();
//
//        if (ServerContext.getInstance().getLocalConfig().getBoolean(LocalConfig.RELAYER_DISCOVERY_SERVER_SWITCH)) {
//            this.isDiscoveryServer = true;
//            LOGGER.info("[BaseRelayerServer] start service of relayer discovery server. ");
//        }
    }

    public void amRequest(String domainName, String authMsg, String udagProof, String ledgerInfo) {

//        this.receiver.receiveOffChainAMRequest(domainName, authMsg, udagProof, ledgerInfo);
    }

    public void doHandshake(RelayerNodeInfo nodeInfo, String networkId) {
//        DBTransactionManager.getInstance().execute(new TransactionCallbackWithoutResult() {
//            @Override
//            protected void doInTransactionWithoutResult(TransactionStatus status) {
//                if (!relayerNetworkManager.addRelayerNodeWithoutDomainInfo(nodeInfo)) {
//                    throw new RuntimeException(String.format("[doHandshake] failed to add relayer node %s",
//                            nodeInfo.getNodeId()));
//                }
//
//                if (!relayerNetworkManager.verifyWithTrustedOracle(
//                        OracleserviceRuntimeCache.getInstance().getTrustedOracleCache(), nodeInfo, networkId)) {
//                    throw new RuntimeException(String.format("[doHandshake] failed to verifyWithTrustedOracle for relayer node %s",
//                            nodeInfo.getNodeId()));
//                }
//
//                relayerNetworkManager.addRelayerNodeProperty(nodeInfo.getNodeId(), RelayerNodeInfo.LAST_TIME_HANDSHAKE,
//                        Long.toString(System.currentTimeMillis()));
//
//                // 现在在进行handshake之前，需要先配置好，两个relayer之间，哪些域名和合约可以互相调用，也许以后再增加ACL的策略。
//            }
//        });
    }

    /**
     * 获取对应domain的relayer信息。
     *
     * @param domain 域名
     * @return relayer信息
     */
    public RelayerNodeInfo getRelayerForDomain(String domain) {
        return this.relayerNetworkManager.getRelayerNodeInfoForDomain(domain);
    }

    /**
     * 向发现服务注册域名和relayer信息；
     *
     * @param nodeInfo
     * @param networkId
     */
    public void registerDomains(RelayerNodeInfo nodeInfo, String networkId) {
//        DBTransactionManager.getInstance().execute(new TransactionCallbackWithoutResult() {
//            @Override
//            protected void doInTransactionWithoutResult(TransactionStatus status) {
//                RelayerNodeInfo nodeInDB = relayerNetworkManager.getRelayerNode(nodeInfo.getNodeId());
//                if (nodeInDB != null) {
//                    nodeInDB.addDomains(nodeInfo.getDomains());
//                    nodeInDB.endpoints = nodeInfo.getEndpoints();
//                    if (!relayerNetworkManager.updateRelayerNode(nodeInDB)) {
//                        throw new RuntimeException(String.format("failed to addRelayerNode(nodeId: %s)", nodeInfo.getNodeId()));
//                    }
//                } else {
//                    nodeInDB = nodeInfo;
//                    if (!relayerNetworkManager.addRelayerNode(nodeInDB)) {
//                        throw new RuntimeException(String.format("failed to addRelayerNode(nodeId: %s)", nodeInfo.getNodeId()));
//                    }
//                }
//
//                for (int i = 0; i < nodeInfo.getDomains().size(); i++) {
//                    if (relayerNetworkManager.getRelayerNodeInfoForDomain(nodeInfo.getDomains().get(i)) != null) {
//                        throw new RuntimeException(String.format("domain %s already registered", nodeInfo.getDomains().get(i)));
//                    }
//
//                    if (!relayerNetworkManager.addRelayerNetworkItem(
//                            networkId, nodeInfo.getDomains().get(i), nodeInfo.getNodeId(), RelayerNodeSyncState.init)) {
//                        throw new RuntimeException(
//                                String.format("failed to addRelayerNetworkItem(networkId: %s, domain: %s, nodeId: %s)",
//                                        networkId, nodeInfo.getDomains().get(i), nodeInfo.getNodeId()));
//                    }
//                }
//            }
//        });
    }

    /**
     * 更新对应的域名。目前仅支持更新relayer的endpoints，property这些。
     * 该域名必须注册过；
     * nodeId不可以更改；
     * 暂时先不判断权限的问题；
     *
     * @param nodeInfo
     */
    public void updateDomains(RelayerNodeInfo nodeInfo) {
//        DBTransactionManager.getInstance().execute(new TransactionCallbackWithoutResult() {
//            @Override
//            protected void doInTransactionWithoutResult(TransactionStatus status) {
//                RelayerNodeInfo nodeInDB = relayerNetworkManager.getRelayerNode(nodeInfo.getNodeId());
//                if (nodeInDB == null) {
//                    throw new RuntimeException(String.format("this relayer %s is not registered", nodeInfo.getNodeId()));
//                }
//
//                if (!nodeInDB.getNodeId().equals(nodeInfo.getNodeId())) {
//                    throw new RuntimeException(String.format("wrong node id and original one is %s", nodeInDB.getNodeId()));
//                }
//
//                if (!nodeInDB.getDomains().containsAll(nodeInfo.getDomains())) {
//                    throw new RuntimeException(String.format(
//                            "some domains are not registered, please register them first. domains registered: %s",
//                            String.join(", ", nodeInDB.getDomains())));
//                }
//
//                nodeInDB.endpoints = nodeInfo.getEndpoints();
//
//                if (!relayerNetworkManager.updateRelayerNode(nodeInDB)) {
//                    throw new RuntimeException("failed to add relayer node. ");
//                }
//            }
//        });
    }

    public void deleteDomains(RelayerNodeInfo nodeInfo) {
//        DBTransactionManager.getInstance().execute(new TransactionCallbackWithoutResult() {
//            @Override
//            protected void doInTransactionWithoutResult(TransactionStatus status) {
//                RelayerNodeInfo nodeInDB = relayerNetworkManager.getRelayerNode(nodeInfo.getNodeId());
//                if (nodeInDB == null) {
//                    throw new RuntimeException(String.format("this relayer %s is not registered", nodeInfo.getNodeId()));
//                }
//
//                if (!nodeInDB.getNodeId().equals(nodeInfo.getNodeId())) {
//                    throw new RuntimeException(String.format("wrong node id and original one is %s", nodeInDB.getNodeId()));
//                }
//
//                if (!nodeInDB.getDomains().containsAll(nodeInfo.getDomains())) {
//                    throw new RuntimeException(String.format(
//                            "some domains are not registered, please register them first. domains registered: %s",
//                            String.join(", ", nodeInDB.getDomains())));
//                }
//
//                if (!nodeInDB.domains.removeAll(nodeInfo.getDomains())) {
//                    throw new RuntimeException("failed to delete domains");
//                }
//
//                if (!relayerNetworkManager.updateRelayerNode(nodeInDB)) {
//                    throw new RuntimeException("failed to add relayer node. ");
//                }
//
//                for (String domain : nodeInfo.getDomains()) {
//                    if (!relayerNetworkManager.deleteRelayerNetworkItem(domain, nodeInDB.getNodeId())) {
//                        throw new RuntimeException(
//                                String.format("failed to delete network item for domain %s and node %s",
//                                        domain, nodeInDB.getNodeId()));
//                    }
//                }
//            }
//        });
    }

    private boolean isAboutDomain(RelayerRequestType type) {
        switch (type) {
            case GET_RELAYER_FOR_DOMAIN:
            case REGISTER_DOMAIN:
            case UPDATE_DOMAIN:
            case DELETE_DOMAIN:
                break;
            default:
                return false;
        }
        return true;
    }
}
