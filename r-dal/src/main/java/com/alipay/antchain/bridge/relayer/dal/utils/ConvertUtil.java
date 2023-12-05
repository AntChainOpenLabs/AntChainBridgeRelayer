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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.bcdns.service.BCDNSTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.DomainNameCredentialSubject;
import com.alipay.antchain.bridge.commons.core.am.*;
import com.alipay.antchain.bridge.commons.core.base.*;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageV1;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageV2;
import com.alipay.antchain.bridge.relayer.commons.constant.CrossChainChannelDO;
import com.alipay.antchain.bridge.relayer.commons.model.*;
import com.alipay.antchain.bridge.relayer.dal.entities.*;
import lombok.NonNull;

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
                .description(blockchainMeta.getDesc())
                .alias(blockchainMeta.getAlias())
                .build();
    }

    public static BlockchainMeta convertFromBlockchainEntity(BlockchainEntity blockchainEntity) {
        return new BlockchainMeta(
                blockchainEntity.getProduct(),
                blockchainEntity.getBlockchainId(),
                blockchainEntity.getAlias(),
                blockchainEntity.getDescription(),
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
                authMsgPoolEntity.getFailCount(),
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
        entity.setFailCount(authMsgWrapper.getFailCount());
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
        context.setFromNetwork(ucpPoolEntity.getFromNetwork());
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
        wrapper.setTxSuccess(sdpMsgPoolEntity.getTxSuccess());
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
            message.setAtomic(sdpMsgPoolEntity.getAtomic());
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
        RelayerNodeInfo nodeInfo = new RelayerNodeInfo(
                entity.getNodeId(),
                CrossChainCertificateFactory.createCrossChainCertificate(
                        Base64.decode(entity.getNodeCrossChainCert())
                ),
                entity.getNodeSigAlgo(),
                stringToList(entity.getEndpoints()),
                stringToList(entity.getDomains())
        );
        if (ObjectUtil.isNotEmpty(entity.getBlockchainContent())) {
            nodeInfo.setRelayerBlockchainContent(
                    RelayerBlockchainContent.decodeFromJson(entity.getBlockchainContent())
            );
        }
        nodeInfo.setProperties(
                RelayerNodeInfo.RelayerNodeProperties.decodeFromJson(
                        new String(entity.getProperties())
                )
        );
        return nodeInfo;
    }

    public static RelayerNodeEntity convertFromRelayerNodeInfo(RelayerNodeInfo nodeInfo) throws IOException {
        RelayerNodeEntity entity = new RelayerNodeEntity();
        entity.setNodeId(nodeInfo.getNodeId());
        entity.setRelayerCertId(nodeInfo.getRelayerCertId());
        entity.setDomains(
                listToString(nodeInfo.getDomains())
        );
        entity.setNodeCrossChainCert(Base64.encode(nodeInfo.getRelayerCrossChainCertificate().encode()));
        entity.setEndpoints(
                listToString(nodeInfo.getEndpoints())
        );

        entity.setBlockchainContent(
                ObjectUtil.isNull(nodeInfo.getRelayerBlockchainContent()) ?
                        StrUtil.EMPTY : nodeInfo.getRelayerBlockchainContent().encodeToJson()
        );
        entity.setProperties(nodeInfo.marshalProperties().getBytes());

        return entity;
    }

    public static String listToString(List<String> list) {
        return list.stream().reduce((s1, s2) -> StrUtil.join("^", s1, s2)).orElse("");
    }

    public static List<String> stringToList(String str) {
        return StrUtil.split(str, "^");
    }

    public static RelayerHealthInfo convertFromDTActiveNodeEntity(int port, long activateLength, DTActiveNodeEntity entity) {
        return new RelayerHealthInfo(
                entity.getNodeIp(),
                port,
                entity.getLastActiveTime().getTime(),
                activateLength
        );
    }

    public static BlockchainDistributedTask convertFromBlockchainDTTaskEntity(BlockchainDTTaskEntity entity) {
        BlockchainDistributedTask blockchainDistributedTask = new BlockchainDistributedTask();
        blockchainDistributedTask.setNodeId(entity.getNodeId());
        blockchainDistributedTask.setTaskType(entity.getTaskType());
        blockchainDistributedTask.setBlockchainId(entity.getBlockchainId());
        blockchainDistributedTask.setBlockchainProduct(entity.getProduct());
        blockchainDistributedTask.setStartTime(entity.getTimeSlice().getTime());
        blockchainDistributedTask.setExt(entity.getExt());
        return blockchainDistributedTask;
    }

    public static BlockchainDTTaskEntity convertFromBlockchainDistributedTask(BlockchainDistributedTask task) {
        BlockchainDTTaskEntity entity = new BlockchainDTTaskEntity();
        entity.setTaskType(task.getTaskType());
        entity.setBlockchainId(task.getBlockchainId());
        entity.setProduct(task.getBlockchainProduct());
        entity.setNodeId(task.getNodeId());
        entity.setTimeSlice(new Date(task.getStartTime()));
        entity.setExt(task.getExt());
        return entity;
    }

    public static BizDistributedTask convertFromBizDTTaskEntity(BizDTTaskEntity entity) {
        BizDistributedTask bizDistributedTask = new BizDistributedTask();
        bizDistributedTask.setNodeId(entity.getNodeId());
        bizDistributedTask.setTaskType(entity.getTaskType());
        bizDistributedTask.setUniqueKey(entity.getUniqueKey());
        bizDistributedTask.setStartTime(entity.getTimeSlice().getTime());
        bizDistributedTask.setExt(entity.getExt());
        return bizDistributedTask;
    }

    public static BizDTTaskEntity convertFromBizDistributedTask(BizDistributedTask task) {
        BizDTTaskEntity entity = new BizDTTaskEntity();
        entity.setTaskType(task.getTaskType());
        entity.setUniqueKey(task.getUniqueKey());
        entity.setNodeId(task.getNodeId());
        entity.setTimeSlice(new Date(task.getStartTime()));
        entity.setExt(task.getExt());
        return entity;
    }

    public static ActiveNode convertFromDTActiveNodeEntityActiveNode(DTActiveNodeEntity entity) {
        ActiveNode node = new ActiveNode();
        node.setNodeIp(entity.getNodeIp());
        node.setNodeId(entity.getNodeId());
        node.setLastActiveTime(entity.getLastActiveTime().getTime());
        return node;
    }

    public static CrossChainMsgACLEntity convertFromCrossChainMsgACLItem(CrossChainMsgACLItem item) {
        CrossChainMsgACLEntity entity = new CrossChainMsgACLEntity();
        entity.setBizId(item.getBizId());

        entity.setOwnerDomain(item.getOwnerDomain());
        entity.setOwnerId(item.getOwnerIdentity());
        entity.setOwnerIdHex(item.getOwnerIdentityHex().toLowerCase());

        entity.setGrantDomain(item.getGrantDomain());
        entity.setGrantId(item.getGrantIdentity());
        entity.setGrantIdHex(item.getGrantIdentityHex().toLowerCase());

        entity.setIsDeleted(item.getIsDeleted());

        return entity;
    }

    public static CrossChainMsgACLItem convertFromCrossChainMsgACLItem(CrossChainMsgACLEntity entity) {
        CrossChainMsgACLItem item = new CrossChainMsgACLItem();

        item.setBizId(entity.getBizId());

        item.setOwnerDomain(entity.getOwnerDomain());
        item.setOwnerIdentity(entity.getOwnerId());
        item.setOwnerIdentityHex(entity.getOwnerIdHex());

        item.setGrantDomain(entity.getGrantDomain());
        item.setGrantIdentity(entity.getGrantId());
        item.setGrantIdentityHex(entity.getGrantIdHex());

        item.setIsDeleted(entity.getIsDeleted());

        return item;
    }

    public static DomainCertWrapper convertFromDomainCertEntity(DomainCertEntity entity) {
        AbstractCrossChainCertificate crossChainCertificate = CrossChainCertificateFactory.createCrossChainCertificate(
                entity.getDomainCert()
        );
        Assert.equals(
                CrossChainCertificateTypeEnum.DOMAIN_NAME_CERTIFICATE,
                crossChainCertificate.getType()
        );
        DomainNameCredentialSubject domainNameCredentialSubject = DomainNameCredentialSubject.decode(
                crossChainCertificate.getCredentialSubject()
        );

        return new DomainCertWrapper(
                crossChainCertificate,
                domainNameCredentialSubject,
                entity.getProduct(),
                entity.getBlockchainId(),
                entity.getDomain(),
                entity.getDomainSpace()
        );
    }

    public static DomainCertEntity convertFromDomainCertWrapper(DomainCertWrapper wrapper) {
        DomainCertEntity entity = new DomainCertEntity();
        entity.setDomainCert(wrapper.getCrossChainCertificate().encode());
        entity.setDomain(wrapper.getDomain());
        entity.setProduct(wrapper.getBlockchainProduct());
        entity.setBlockchainId(wrapper.getBlockchainId());
        entity.setSubjectOid(
                wrapper.getDomainNameCredentialSubject().getApplicant().encode()
        );
        entity.setIssuerOid(
                wrapper.getCrossChainCertificate().getIssuer().encode()
        );
        entity.setDomainSpace(wrapper.getDomainSpace());
        return entity;
    }

    public static DomainSpaceCertEntity convertFromDomainSpaceCertWrapper(DomainSpaceCertWrapper wrapper) {
        DomainSpaceCertEntity entity = new DomainSpaceCertEntity();
        entity.setDomainSpace(wrapper.getDomainSpace());
        entity.setParentSpace(wrapper.getParentDomainSpace());
        entity.setOwnerOidHex(HexUtil.encodeHexStr(wrapper.getOwnerOid().encode()));
        entity.setDesc(wrapper.getDesc());
        entity.setDomainSpaceCert(wrapper.getDomainSpaceCert().encode());
        return entity;
    }

    public static DomainSpaceCertWrapper convertFromDomainSpaceCertEntity(DomainSpaceCertEntity entity) {
        AbstractCrossChainCertificate certificate = CrossChainCertificateFactory.createCrossChainCertificate(entity.getDomainSpaceCert());
        DomainSpaceCertWrapper wrapper = new DomainSpaceCertWrapper(certificate);
        wrapper.setDesc(entity.getDesc());
        return wrapper;
    }

    public static BCDNSServiceDO convertFromBCDNSServiceEntity(@NonNull BCDNSServiceEntity entity, @NonNull DomainSpaceCertWrapper domainSpaceCertWrapper) {
        return new BCDNSServiceDO(
                entity.getDomainSpace(),
                ObjectIdentity.decode(HexUtil.decodeHex(entity.getOwnerOid())),
                domainSpaceCertWrapper,
                BCDNSTypeEnum.parseFromValue(entity.getType()),
                entity.getState(),
                entity.getProperties()
        );
    }

    public static BCDNSServiceEntity convertFromBCDNSServiceDO(BCDNSServiceDO bcdnsServiceDO) {
        BCDNSServiceEntity entity = new BCDNSServiceEntity();
        entity.setDomainSpace(bcdnsServiceDO.getDomainSpace());
        entity.setOwnerOid(HexUtil.encodeHexStr(bcdnsServiceDO.getOwnerOid().encode()));
        entity.setType(bcdnsServiceDO.getType().getCode());
        entity.setState(bcdnsServiceDO.getState());
        entity.setProperties(bcdnsServiceDO.getProperties());
        return entity;
    }

    public static DomainCertApplicationDO convertFromDomainCertApplicationEntity(DomainCertApplicationEntity entity) {
        return BeanUtil.copyProperties(entity, DomainCertApplicationDO.class);
    }

    public static DomainCertApplicationEntity convertFromDomainCertApplicationDO(DomainCertApplicationDO domainCertApplicationDO) {
        return BeanUtil.copyProperties(domainCertApplicationDO, DomainCertApplicationEntity.class);
    }

    public static MarkDTTaskEntity convertFromMarkDTTask(MarkDTTask markDTTask) {
        MarkDTTaskEntity entity = new MarkDTTaskEntity();
        entity.setNodeId(StrUtil.emptyToDefault(markDTTask.getNodeId(), null));
        entity.setUniqueKey(StrUtil.emptyToDefault(markDTTask.getUniqueKey(), null));
        entity.setTaskType(markDTTask.getTaskType());
        entity.setEndTime(ObjectUtil.isNotNull(markDTTask.getEndTime()) ? new Date(markDTTask.getEndTime()) : null);
        entity.setState(markDTTask.getState());
        return entity;
    }

    public static MarkDTTask convertFromMarkDTTaskEntity(MarkDTTaskEntity entity) {
        MarkDTTask task = BeanUtil.copyProperties(entity, MarkDTTask.class, MarkDTTaskEntity.Fields.endTime);
        task.setEndTime(entity.getEndTime().getTime());
        return task;
    }

    public static CrossChainChannelDO convertFromCrossChainChannelEntity(CrossChainChannelEntity entity) {
        return BeanUtil.copyProperties(entity, CrossChainChannelDO.class);
    }

    public static CrossChainChannelEntity convertFromCrossChainChannelDO(CrossChainChannelDO crossChainChannelDO) {
        return BeanUtil.copyProperties(crossChainChannelDO, CrossChainChannelEntity.class);
    }
}
