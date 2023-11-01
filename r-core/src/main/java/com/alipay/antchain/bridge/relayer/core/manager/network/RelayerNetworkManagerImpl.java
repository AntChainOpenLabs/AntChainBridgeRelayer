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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.RelayerCredentialSubject;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.RelayerNodeSyncStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.*;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
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

    //TODO: for next stage development
    private final AbstractCrossChainCertificate crossChainCertificate;

    private final RelayerCredentialSubject relayerCredentialSubject;

    private final IBCDNSManager bcdnsManager;

    public RelayerNetworkManagerImpl(
            String localNodeSigAlgo,
            String localNodeServerMode,
            AbstractCrossChainCertificate crossChainCertificate,
            RelayerCredentialSubject relayerCredentialSubject,
            IRelayerNetworkRepository relayerNetworkRepository,
            IBlockchainRepository blockchainRepository,
            ISystemConfigRepository systemConfigRepository,
            IBCDNSManager bcdnsManager
    ) {
        this.crossChainCertificate = crossChainCertificate;
        this.relayerCredentialSubject = relayerCredentialSubject;
        this.localNodeId = DigestUtil.sha256Hex(relayerCredentialSubject.getApplicant().getRawId());
        this.localNodeServerMode = localNodeServerMode;
        this.relayerNetworkRepository = relayerNetworkRepository;
        this.blockchainRepository = blockchainRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.localNodeSigAlgo = localNodeSigAlgo;
        this.bcdnsManager = bcdnsManager;
    }

    @Override
    public RelayerNodeInfo getRelayerNodeInfo() {
        try {
            return new RelayerNodeInfo(
                    localNodeId,
                    crossChainCertificate,
                    relayerCredentialSubject,
                    localNodeSigAlgo,
                    systemConfigRepository.getLocalEndpoints(),
                    blockchainRepository.getBlockchainDomainsByState(BlockchainStateEnum.RUNNING)
            );
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
                domainSpaceChain,
                domainCertWrapper,
                null,
                null,
                blockchainMeta.getProperties().getAmClientContractAddress(),
                MapUtil.newHashMap()
        );
    }

    @Override
    public boolean addRelayerNode(RelayerNodeInfo nodeInfo) {
        return false;
    }

    @Override
    public boolean addRelayerNodeWithoutDomainInfo(RelayerNodeInfo nodeInfo) {
        return false;
    }

    @Override
    public boolean addRelayerNodeProperty(String nodeId, String key, String value) {
        return false;
    }

    @Override
    public RelayerNodeInfo getRelayerNode(String nodeId) {
        return null;
    }

    @Override
    public boolean syncRelayerNode(String networkId, String nodeId) {
        return false;
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
}
