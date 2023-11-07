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

package com.alipay.antchain.bridge.relayer.core.manager.network;

import java.security.PrivateKey;
import java.security.Signature;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.RelayerCredentialSubject;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.RelayerNodeSyncStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.*;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.types.network.RelayerClient;
import com.alipay.antchain.bridge.relayer.core.types.network.RelayerClientPool;
import com.alipay.antchain.bridge.relayer.core.types.network.request.RelayerRequest;
import com.alipay.antchain.bridge.relayer.core.types.network.response.RelayerResponse;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.IRelayerNetworkRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.ISystemConfigRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Getter
public class RelayerNetworkManagerImpl implements IRelayerNetworkManager {

    private final String localNodeId;

    private final String localNodeSigAlgo;

    private final String localNodeServerMode;

    private final IRelayerNetworkRepository relayerNetworkRepository;

    private final IBlockchainRepository blockchainRepository;

    private final ISystemConfigRepository systemConfigRepository;

    private final AbstractCrossChainCertificate localRelayerCertificate;

    private final RelayerCredentialSubject relayerCredentialSubject;

    private final PrivateKey localRelayerPrivateKey;

    private final IBCDNSManager bcdnsManager;

    private final RelayerClientPool relayerClientPool;

    private final Cache<String, RelayerNodeInfo> relayerNodeInfoCache = CacheUtil.newTimedCache(3_000);

    public RelayerNetworkManagerImpl(
            String localNodeSigAlgo,
            String localNodeServerMode,
            AbstractCrossChainCertificate relayerCertificate,
            RelayerCredentialSubject relayerCredentialSubject,
            PrivateKey localRelayerPrivateKey,
            IRelayerNetworkRepository relayerNetworkRepository,
            IBlockchainRepository blockchainRepository,
            ISystemConfigRepository systemConfigRepository,
            IBCDNSManager bcdnsManager,
            RelayerClientPool relayerClientPool
    ) {
        this.localRelayerCertificate = relayerCertificate;
        this.relayerCredentialSubject = relayerCredentialSubject;
        this.localRelayerPrivateKey = localRelayerPrivateKey;
        this.localNodeId = RelayerNodeInfo.calculateNodeId(relayerCertificate);
        this.localNodeServerMode = localNodeServerMode;
        this.relayerNetworkRepository = relayerNetworkRepository;
        this.blockchainRepository = blockchainRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.localNodeSigAlgo = localNodeSigAlgo;
        this.bcdnsManager = bcdnsManager;
        this.relayerClientPool = relayerClientPool;
    }

    @Override
    public RelayerNodeInfo getRelayerNodeInfo() {
        try {
            if (relayerNodeInfoCache.containsKey(localNodeId)) {
                return relayerNodeInfoCache.get(localNodeId);
            }
            RelayerNodeInfo localNodeInfo = new RelayerNodeInfo(
                    localNodeId,
                    localRelayerCertificate,
                    relayerCredentialSubject,
                    localNodeSigAlgo,
                    systemConfigRepository.getLocalEndpoints(),
                    blockchainRepository.getBlockchainDomainsByState(BlockchainStateEnum.RUNNING)
            );
            relayerNodeInfoCache.put(localNodeId, localNodeInfo);
            return localNodeInfo;
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    "failed to get local relayer node info",
                    e
            );
        }
    }

    @Override
    @Transactional
    public RelayerNodeInfo getRelayerNodeInfoWithContent() {
        RelayerNodeInfo nodeInfo = getRelayerNodeInfo();

        Map<String, RelayerBlockchainInfo> blockchainInfoMap = nodeInfo.getDomains().stream()
                .map(this::getRelayerBlockchainInfo)
                .collect(Collectors.toMap(
                        info -> info.getDomainCert().getDomain(),
                        info -> info
                ));
        Map<String, AbstractCrossChainCertificate> trustRootCertChain = blockchainInfoMap.values().stream()
                .map(info -> this.bcdnsManager.getTrustRootCertChain(info.getDomainCert().getDomainSpace()))
                .reduce(
                        (map1, map2) -> {
                            map1.putAll(map2);
                            return map1;
                        }
                ).orElse(MapUtil.newHashMap());

        RelayerBlockchainContent content = new RelayerBlockchainContent(
                blockchainInfoMap,
                trustRootCertChain
        );

        nodeInfo.setRelayerBlockchainContent(content);

        return nodeInfo;
    }

    @Override
    public RelayerBlockchainInfo getRelayerBlockchainInfo(String domain) {
        DomainCertWrapper domainCertWrapper = blockchainRepository.getDomainCert(domain);
        if (ObjectUtil.isNull(domainCertWrapper)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    "none domain cert found for {}", domain
            );
        }

        List<String> domainSpaceChain = bcdnsManager.getDomainSpaceChain(domainCertWrapper.getDomainSpace());
        if (ObjectUtil.isEmpty(domainSpaceChain)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    "none domain space chain found for {}", domainCertWrapper.getDomainSpace()
            );
        }

        BlockchainMeta blockchainMeta = blockchainRepository.getBlockchainMeta(
                domainCertWrapper.getBlockchainProduct(),
                domainCertWrapper.getBlockchainId()
        );
        if (ObjectUtil.isEmpty(blockchainMeta)) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    "none blockchain meta found for {}", domain
            );
        }

        return new RelayerBlockchainInfo(
                domainCertWrapper,
                domainSpaceChain,
                blockchainMeta.getProperties().getAmClientContractAddress()
        );
    }

    @Override
    public void addRelayerNode(RelayerNodeInfo nodeInfo) {
        log.info("add relayer node {} with endpoints {}", nodeInfo.getNodeId(), StrUtil.join(StrUtil.COMMA, nodeInfo.getEndpoints()));

        try {
            if (relayerNetworkRepository.hasRelayerNode(nodeInfo.getNodeId())) {
                log.warn("relayer node {} already exists", nodeInfo.getNodeId());
                return;
            }
            if (ObjectUtil.isEmpty(nodeInfo.getEndpoints())) {
                throw new AntChainBridgeRelayerException(
                        RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                        "relayer info not enough"
                );
            }

            // 如果公钥以及domain信息没设置，则远程请求补充
            if (ObjectUtil.isNull(nodeInfo.getRelayerCrossChainCertificate())) {
                RelayerClient relayerClient = relayerClientPool.getRelayerClient(nodeInfo);
                if (ObjectUtil.isNull(relayerClient)) {
                    throw new AntChainBridgeRelayerException(
                            RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                            "failed to create relayer client for relayer {} with endpoint {}",
                            nodeInfo.getNodeId(), StrUtil.join(StrUtil.COMMA, nodeInfo.getEndpoints())
                    );
                }

                RelayerNodeInfo relayerNodeInfo = relayerClient.getRelayerNodeInfo();
                if (ObjectUtil.isNull(relayerNodeInfo)) {
                    throw new RuntimeException("null relayer node info from remote relayer");
                }

                nodeInfo.setRelayerCrossChainCertificate(relayerNodeInfo.getRelayerCrossChainCertificate());
                nodeInfo.setRelayerCredentialSubject(relayerNodeInfo.getRelayerCredentialSubject());
                relayerNodeInfo.getDomains().forEach(
                        domain -> {
                            if (!nodeInfo.getDomains().contains(domain)) {
                                nodeInfo.addDomainIfNotExist(domain);
                            }
                        }
                );
            }

            relayerNetworkRepository.addRelayerNode(nodeInfo);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    e,
                    "failed to add relayer {} with endpoint {}",
                    nodeInfo.getNodeId(), StrUtil.join(StrUtil.COMMA, nodeInfo.getEndpoints())
            );
        }
    }

    @Override
    public void addRelayerNodeWithoutDomainInfo(RelayerNodeInfo nodeInfo) {
        try {
            if (relayerNetworkRepository.hasRelayerNode(nodeInfo.getNodeId())) {
                log.warn("relayer node {} already exist", nodeInfo.getNodeId());
                return;
            }
            List<String> domains = nodeInfo.getDomains();
            RelayerBlockchainContent relayerBlockchainContent = nodeInfo.getRelayerBlockchainContent();

            nodeInfo.setDomains(ListUtil.toList());
            ;
            nodeInfo.setRelayerBlockchainContent(null);

            relayerNetworkRepository.addRelayerNode(nodeInfo);

            nodeInfo.setDomains(domains);
            nodeInfo.setRelayerBlockchainContent(relayerBlockchainContent);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    e,
                    "failed to add relayer {} without domain stuff",
                    nodeInfo.getNodeId()
            );
        }
    }

    @Override
    public void addRelayerNodeProperty(String nodeId, String key, String value) {
        try {
            relayerNetworkRepository.updateRelayerNodeProperty(nodeId, key, value);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    e,
                    "failed to add relayer property {} - {}",
                    nodeId, key, value
            );
        }
    }

    @Override
    public RelayerNodeInfo getRelayerNode(String nodeId) {
        return relayerNetworkRepository.getRelayerNode(nodeId);
    }

    @Override
    @Transactional
    public void syncRelayerNode(String networkId, String nodeId) {
        log.info("begin sync relayer node {} in network {}", nodeId, networkId);

        try {
            RelayerNodeInfo relayerNode = relayerNetworkRepository.getRelayerNode(nodeId);
            if (null == relayerNode) {
                throw new RuntimeException(StrUtil.format("relayer {} not exist", nodeId));
            }

            log.info("relayer node {} has {} domain", nodeId, relayerNode.getDomains().size());

            RelayerBlockchainContent relayerBlockchainContent = relayerClientPool.getRelayerClient(relayerNode).getRelayerBlockchainContent();
            RelayerBlockchainContent.ValidationResult validationResult = relayerBlockchainContent.validate(
                    bcdnsManager.getTrustRootCertForRootDomain()
            );

            validationResult.getBlockchainInfoMapValidated().forEach(
                    (key, value) -> {
                        try {
                            processRelayerBlockchainInfo(
                                    networkId,
                                    key,
                                    relayerNode,
                                    value
                            );
                        } catch (Exception e) {
                            log.error("failed process blockchain info for {} from relayer {}", key, nodeId, e);
                        }
                        log.info("sync domain {} success from relayer {}", key, nodeId);
                    }
            );

            relayerNode.setRelayerBlockchainContent(relayerBlockchainContent);
            if (!relayerNetworkRepository.updateRelayerNode(relayerNode)) {
                throw new RuntimeException(
                        StrUtil.format("update relayer info fail {} ", relayerNode.getNodeId())
                );
            }

            bcdnsManager.saveDomainSpaceCerts(validationResult.getDomainSpaceValidated());
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    e,
                    "failed to sync relayer node {}",
                    nodeId
            );
        }
    }

    @Override
    public RelayerNetwork findNetworkByDomainName(String domainName) {
        return null;
    }

    @Override
    public boolean addRelayerNetwork(RelayerNetwork network) {
        return false;
    }

    @Override
    public boolean addRelayerNetworkItem(String networkId, String domain, String nodeId) {
        return false;
    }

    @Override
    public boolean addRelayerNetworkItem(String networkId, String domain, String nodeId, RelayerNodeSyncStateEnum syncState) {
        return false;
    }

    @Override
    public boolean deleteRelayerNetworkItem(String domain, String nodeId) {
        return false;
    }

    @Override
    public RelayerNetwork getRelayerNetwork(String networkId) {
        return null;
    }

    @Override
    public List<RelayerNetwork> getRelayerNetworks() {
        return null;
    }

    @Override
    public RelayerNodeInfo getRelayerNodeInfoForDomain(String domain) {
        return null;
    }

    @Override
    public RelayerNodeInfo getRemoteRelayerNodeInfo(String domain) {
        return null;
    }

    @Override
    public void registerDomainToDiscoveryServer(RelayerNodeInfo nodeInfo, String networkId) throws Exception {

    }

    @Override
    public void updateDomainToDiscoveryServer(RelayerNodeInfo nodeInfo) throws Exception {

    }

    @Override
    public void deleteDomainToDiscoveryServer(RelayerNodeInfo nodeInfo) throws Exception {

    }

    @Override
    public boolean tryHandshake(String domainName, RelayerNodeInfo remoteNodeInfo) {
        return false;
    }

    @Override
    public boolean updateRelayerNode(RelayerNodeInfo nodeInfo) {
        return false;
    }

    @Override
    public List<RelayerHealthInfo> healthCheckRelayers() {
        return null;
    }

    @Override
    public void signRelayerRequest(RelayerRequest relayerRequest) {
        try {
            relayerRequest.setNodeId(localNodeId);
            relayerRequest.setSenderRelayerCertificate(localRelayerCertificate);
            relayerRequest.setSigAlgo(localNodeSigAlgo);

            Signature signer = Signature.getInstance(localNodeSigAlgo);
            signer.initSign(localRelayerPrivateKey);
            signer.update(relayerRequest.rawEncode());
            relayerRequest.setSignature(signer.sign());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    "failed to sign for request type {}", relayerRequest.getRequestType().getCode()
            );
        }
    }

    @Override
    public void signRelayerResponse(RelayerResponse relayerResponse) {
        try {
            relayerResponse.setRemoteRelayerCertificate(localRelayerCertificate);
            relayerResponse.setSigAlgo(localNodeSigAlgo);

            Signature signer = Signature.getInstance(localNodeSigAlgo);
            signer.initSign(localRelayerPrivateKey);
            signer.update(relayerResponse.rawEncode());
            relayerResponse.setSignature(signer.sign());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    "failed to sign response"
            );
        }
    }

    @Override
    public boolean validateRelayerRequest(RelayerRequest relayerRequest) {
        if (!bcdnsManager.validateCrossChainCertificate(relayerRequest.getSenderRelayerCertificate())) {
            return false;
        }
        if (
                ObjectUtil.notEqual(
                        CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE,
                        relayerRequest.getSenderRelayerCertificate().getType()
                )
        ) {
            return false;
        }

        return relayerRequest.verify();
    }

    @Override
    public boolean validateRelayerResponse(RelayerResponse relayerResponse) {
        if (!bcdnsManager.validateCrossChainCertificate(relayerResponse.getRemoteRelayerCertificate())) {
            return false;
        }
        if (
                ObjectUtil.notEqual(
                        CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE,
                        relayerResponse.getRemoteRelayerCertificate().getType()
                )
        ) {
            return false;
        }

        return relayerResponse.verify();
    }

    private void processRelayerBlockchainInfo(String networkId, String domain, RelayerNodeInfo relayerNode,
                                              RelayerBlockchainInfo relayerBlockchainInfo) {
//        if (
//                !bcdnsManager.validateDomainCertificate(
//                        relayerBlockchainInfo.getDomainCert().getCrossChainCertificate(),
//                        relayerBlockchainInfo.getDomainSpaceChain()
//                )
//        ) {
//            throw new RuntimeException("Invalid domain certificate for domain " + domain);
//        }
        //TODO: validate ptc certificate
        //TODO: validate domain tpbta

        if (relayerNetworkRepository.hasNetworkItem(networkId, domain, relayerNode.getNodeId())) {
            relayerNetworkRepository.updateNetworkItem(networkId, domain, relayerNode.getNodeId(), RelayerNodeSyncStateEnum.SYNC);
        } else {
            addRelayerNetworkItem(networkId, domain, relayerNode.getNodeId(), RelayerNodeSyncStateEnum.SYNC);
        }
        relayerNode.addDomainIfNotExist(domain);
    }
}
