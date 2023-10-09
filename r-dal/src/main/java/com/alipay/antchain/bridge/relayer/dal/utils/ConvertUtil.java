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

package com.alipay.antchain.bridge.relayer.dal.utils;

import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.core.am.*;
import com.alipay.antchain.bridge.commons.core.base.*;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageV1;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageV2;
import com.alipay.antchain.bridge.relayer.commons.model.*;
import com.alipay.antchain.bridge.relayer.dal.entities.*;

public class ConvertUtil {

    public static List<AnchorProcessEntity> convertFromAnchorProcessHeights(AnchorProcessHeights heights) {
        return heights.getProcessHeights().entrySet().stream()
                .map(
                        entry -> new AnchorProcessEntity(
                                heights.getProduct(),
                                heights.getBlockchainId(),
                                entry.getKey(),
                                entry.getValue()
                        )
                ).collect(Collectors.toList());
    }

    public static BlockchainEntity convertFromBlockchainMeta(BlockchainMeta blockchainMeta) {
        return BlockchainEntity.builder()
                .blockchainId(blockchainMeta.getBlockchainId())
                .product(blockchainMeta.getProduct())
                .properties(blockchainMeta.getProperties().encode())
                .desc(blockchainMeta.getDesc())
                .alias(blockchainMeta.getAlias())
                .build();
    }

    public static BlockchainMeta convertFromBlockchainEntity(BlockchainEntity blockchainEntity) {
        return new BlockchainMeta(
                blockchainEntity.getProduct(),
                blockchainEntity.getBlockchainId(),
                blockchainEntity.getAlias(),
                blockchainEntity.getDesc(),
                blockchainEntity.getProperties()
        );
    }

    public static PluginServerObjectsEntity convertFromPluginServerDO(PluginServerDO pluginServerDO) {
        return PluginServerObjectsEntity.builder()
                .psId(pluginServerDO.getPsId())
                .address(pluginServerDO.getAddress())
                .domains(StrUtil.join(",", pluginServerDO.getDomainsServing()))
                .products(StrUtil.join(",", pluginServerDO.getProductsSupported()))
                .properties(pluginServerDO.getProperties().encode())
                .state(pluginServerDO.getState())
                .build();
    }

    public static PluginServerDO convertFromPluginServerObjectsEntity(PluginServerObjectsEntity pluginServerObject) {
        PluginServerDO pluginServerDO = new PluginServerDO();
        pluginServerDO.setId(pluginServerObject.getId().intValue());
        pluginServerDO.setPsId(pluginServerObject.getPsId());
        pluginServerDO.setAddress(pluginServerObject.getAddress());
        pluginServerDO.setState(pluginServerObject.getState());
        pluginServerDO.setProductsSupported(StrUtil.split(pluginServerObject.getProducts(), ","));
        pluginServerDO.setDomainsServing(StrUtil.split(pluginServerObject.getDomains(), ","));
        pluginServerDO.setProperties(
                PluginServerDO.PluginServerProperties.decode(pluginServerObject.getProperties())
        );
        pluginServerDO.setGmtCreate(pluginServerObject.getGmtCreate());
        pluginServerDO.setGmtModified(pluginServerObject.getGmtModified());

        return pluginServerDO;
    }

    public static AuthMsgWrapper convertFromAuthMsgPoolEntity(AuthMsgPoolEntity authMsgPoolEntity) {
        IAuthMessage authMessage;
        if (authMsgPoolEntity.getVersion() == AuthMessageV1.MY_VERSION) {
            AuthMessageV1 authMessageV1 = (AuthMessageV1) AuthMessageFactory.createAuthMessage(authMsgPoolEntity.getVersion());
            authMessageV1.setIdentity(CrossChainIdentity.fromHexStr(authMsgPoolEntity.getMsgSender()));
            authMessageV1.setUpperProtocol(authMsgPoolEntity.getProtocolType().ordinal());
            authMessageV1.setPayload(authMsgPoolEntity.getPayload());
            authMessage = authMessageV1;
        } else if (authMsgPoolEntity.getVersion() == AuthMessageV2.MY_VERSION) {
            AuthMessageV2 authMessageV2 = (AuthMessageV2) AuthMessageFactory.createAuthMessage(authMsgPoolEntity.getVersion());
            ;
            authMessageV2.setIdentity(CrossChainIdentity.fromHexStr(authMsgPoolEntity.getMsgSender()));
            authMessageV2.setUpperProtocol(authMsgPoolEntity.getProtocolType().ordinal());
            authMessageV2.setPayload(authMsgPoolEntity.getPayload());
            authMessageV2.setTrustLevel(
                    AuthMessageTrustLevelEnum.parseFromValue(
                            authMsgPoolEntity.getTrustLevel().getCode()
                    )
            );
            authMessage = authMessageV2;
        } else {
            throw new RuntimeException(
                    String.format(
                            "wrong version %d of am (id: %d) when convert from entity",
                            authMsgPoolEntity.getVersion(), authMsgPoolEntity.getId()
                    )
            );
        }
        return new AuthMsgWrapper(
                authMsgPoolEntity.getId(),
                authMsgPoolEntity.getProduct(),
                authMsgPoolEntity.getBlockchainId(),
                authMsgPoolEntity.getDomain(),
                authMsgPoolEntity.getUcpId(),
                authMsgPoolEntity.getAmClientContractAddress(),
                authMsgPoolEntity.getProcessState(),
                authMessage
        );
    }

    public static AuthMsgPoolEntity convertFromAuthMsgWrapper(AuthMsgWrapper authMsgWrapper) {
        AuthMsgPoolEntity entity = new AuthMsgPoolEntity();
        entity.setId(authMsgWrapper.getAuthMsgId());
        entity.setUcpId(authMsgWrapper.getUcpId());
        entity.setProduct(authMsgWrapper.getProduct());
        entity.setBlockchainId(authMsgWrapper.getBlockchainId());
        entity.setDomain(authMsgWrapper.getDomain());
        entity.setAmClientContractAddress(authMsgWrapper.getAmClientContractAddress());
        entity.setVersion(authMsgWrapper.getVersion());
        entity.setMsgSender(authMsgWrapper.getMsgSender());
        entity.setProtocolType(authMsgWrapper.getProtocolType());
        entity.setTrustLevel(authMsgWrapper.getTrustLevel());
        entity.setPayload(authMsgWrapper.getPayload());
        entity.setProcessState(authMsgWrapper.getProcessState());
        entity.setExt(authMsgWrapper.getRawLedgerInfo());

        return entity;
    }

    public static UniformCrosschainPacketContext convertFromUCPPoolEntity(UCPPoolEntity ucpPoolEntity) {
        UniformCrosschainPacket packet = new UniformCrosschainPacket();
        packet.setPtcId(ObjectIdentity.decode(ucpPoolEntity.getPtcOid()));
        packet.setSrcDomain(new CrossChainDomain(ucpPoolEntity.getSrcDomain()));
        packet.setVersion(ucpPoolEntity.getVersion());
        packet.setTpProof(ucpPoolEntity.getTpProof());
        packet.setSrcMessage(JSON.parseObject(ucpPoolEntity.getRawMessage(), CrossChainMessage.class));

        UniformCrosschainPacketContext context = new UniformCrosschainPacketContext();
        context.setUcp(packet);
        context.setId(ucpPoolEntity.getId());
        context.setUcpId(ucpPoolEntity.getUcpId());
        context.setProduct(ucpPoolEntity.getProduct());
        context.setBlockchainId(ucpPoolEntity.getBlockchainId());
        context.setUdagPath(ucpPoolEntity.getUdagPath());
        context.setProcessState(ucpPoolEntity.getProcessState());
        context.setFromNetwork(ucpPoolEntity.isFromNetwork());
        context.setRelayerId(ucpPoolEntity.getRelayerId());

        return context;
    }

    public static SDPMsgWrapper convertFromSDPMsgPoolEntity(SDPMsgPoolEntity sdpMsgPoolEntity) {
        SDPMsgWrapper wrapper = new SDPMsgWrapper();
        wrapper.setId(sdpMsgPoolEntity.getId());
        wrapper.setReceiverBlockchainProduct(sdpMsgPoolEntity.getReceiverBlockchainProduct());
        wrapper.setReceiverBlockchainId(sdpMsgPoolEntity.getReceiverBlockchainId());
        wrapper.setReceiverAMClientContract(sdpMsgPoolEntity.getReceiverAMClientContract());
        wrapper.setProcessState(sdpMsgPoolEntity.getProcessState());
        wrapper.setTxHash(sdpMsgPoolEntity.getTxHash());
        wrapper.setTxSuccess(sdpMsgPoolEntity.isTxSuccess());
        wrapper.setTxFailReason(sdpMsgPoolEntity.getTxFailReason());

        if (sdpMsgPoolEntity.getVersion() == SDPMessageV1.MY_VERSION) {
            SDPMessageV1 message = new SDPMessageV1();
            message.setSequence(sdpMsgPoolEntity.getMsgSequence().intValue());
            message.setTargetDomain(new CrossChainDomain(sdpMsgPoolEntity.getReceiverDomainName()));
            message.setTargetIdentity(new CrossChainIdentity(HexUtil.decodeHex(sdpMsgPoolEntity.getReceiverId())));
            wrapper.setSdpMessage(message);
        } else if (sdpMsgPoolEntity.getVersion() == SDPMessageV2.MY_VERSION) {
            SDPMessageV2 message = new SDPMessageV2();
            message.setSequence(sdpMsgPoolEntity.getMsgSequence().intValue());
            message.setTargetDomain(new CrossChainDomain(sdpMsgPoolEntity.getReceiverDomainName()));
            message.setTargetIdentity(new CrossChainIdentity(HexUtil.decodeHex(sdpMsgPoolEntity.getReceiverId())));
            message.setAtomic(sdpMsgPoolEntity.isAtomic());
            wrapper.setSdpMessage(message);
        } else {
            throw new RuntimeException("Invalid version of sdp message: " + sdpMsgPoolEntity.getVersion());
        }

        AuthMsgWrapper authMsgWrapper = new AuthMsgWrapper();
        authMsgWrapper.setAuthMsgId(sdpMsgPoolEntity.getAuthMsgId());
        authMsgWrapper.setProduct(sdpMsgPoolEntity.getSenderBlockchainProduct());
        authMsgWrapper.setBlockchainId(sdpMsgPoolEntity.getSenderBlockchainId());
        authMsgWrapper.setDomain(sdpMsgPoolEntity.getSenderDomainName());
        authMsgWrapper.setMsgSender(sdpMsgPoolEntity.getSenderId());
        authMsgWrapper.setAmClientContractAddress(sdpMsgPoolEntity.getSenderAMClientContract());

        wrapper.setAuthMsgWrapper(authMsgWrapper);

        return wrapper;
    }

    public static SDPMsgPoolEntity convertFromSDPMsgWrapper(SDPMsgWrapper wrapper) {
        SDPMsgPoolEntity entity = new SDPMsgPoolEntity();
        entity.setId(wrapper.getId());
        entity.setVersion(wrapper.getVersion());
        entity.setAtomic(wrapper.getAtomic());

        entity.setSenderBlockchainProduct(wrapper.getSenderBlockchainProduct());
        entity.setSenderBlockchainId(wrapper.getSenderBlockchainId());
        entity.setSenderDomainName(wrapper.getSenderBlockchainDomain());
        entity.setSenderId(wrapper.getMsgSender());
        entity.setSenderAMClientContract(wrapper.getSenderAMClientContract());

        entity.setReceiverBlockchainProduct(wrapper.getReceiverBlockchainProduct());
        entity.setReceiverBlockchainId(wrapper.getReceiverBlockchainId());
        entity.setReceiverDomainName(wrapper.getReceiverBlockchainDomain());
        entity.setReceiverId(wrapper.getMsgReceiver());
        entity.setReceiverAMClientContract(wrapper.getReceiverAMClientContract());

        entity.setMsgSequence((long) wrapper.getMsgSequence());
        entity.setProcessState(wrapper.getProcessState());
        entity.setTxHash(wrapper.getTxHash());
        entity.setTxSuccess(wrapper.isTxSuccess());
        entity.setTxFailReason(wrapper.getTxFailReason());

        return entity;
    }

    public static RelayerNetworkEntity convertFromRelayerNetworkItem(String networkId, String domain, RelayerNetwork.Item relayerNetworkItem) {
        RelayerNetworkEntity entity = new RelayerNetworkEntity();
        entity.setNetworkId(networkId);
        entity.setDomain(domain);
        entity.setNodeId(relayerNetworkItem.getNodeId());
        entity.setSyncState(relayerNetworkItem.getSyncState());

        return entity;
    }

    public static RelayerNetwork.Item convertFromRelayerNetworkEntity(RelayerNetworkEntity entity) {
        return new RelayerNetwork.Item(entity.getNodeId(), entity.getSyncState());
    }

    public static RelayerNodeInfo convertFromRelayerNodeEntity(RelayerNodeEntity entity) {
        RelayerNodeInfo nodeInfo = new RelayerNodeInfo();

        nodeInfo.setNodeId(entity.getNodeId());
        nodeInfo.setNodePublicKey(entity.getNodePublicKey());
        nodeInfo.setDomains(StrUtil.split(entity.getDomains(), "^"));
        nodeInfo.setEndpoints(StrUtil.split(entity.getEndpoints(), "^"));
        nodeInfo.setProperties(RelayerNodeInfo.RelayerNodeProperties.decodeFromJson(new String(entity.getProperties())));

        return nodeInfo;
    }

    public static RelayerNodeEntity convertFromRelayerNodeInfo(RelayerNodeInfo nodeInfo) {
        RelayerNodeEntity entity = new RelayerNodeEntity();
        entity.setNodeId(nodeInfo.getNodeId());
        entity.setDomains(
                nodeInfo.getDomains().stream().reduce((s1, s2) -> StrUtil.join("^", s1, s2)).orElse("")
        );
        entity.setNodePublicKey(nodeInfo.getNodePublicKey());
        entity.setEndpoints(
                nodeInfo.getEndpoints().stream().reduce((s1, s2) -> StrUtil.join("^", s1, s2)).orElse("")
        );
        entity.setProperties(nodeInfo.marshalProperties().getBytes());

        return entity;
    }

    public static RelayerHealthInfo convertFromDTActiveNodeEntity(int port, DTActiveNodeEntity entity) {
        return new RelayerHealthInfo(
                entity.getNodeIp(),
                port,
                entity.getLastActiveTime().getTime()
        );
    }
}
