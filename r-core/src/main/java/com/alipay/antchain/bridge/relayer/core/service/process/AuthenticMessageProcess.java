package com.alipay.antchain.bridge.relayer.core.service.process;

import java.util.Base64;
import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.core.sdp.AbstractSDPMessage;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageFactory;
import com.alipay.antchain.bridge.relayer.commons.constant.*;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.*;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.core.manager.gov.IGovernManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.core.types.network.IRelayerClientPool;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.impl.BlockchainIdleDCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AM消息处理器
 * <p>
 * 根据不同的上层AM协议，处理流程不一样
 * <p>
 * 如果是MSG消息，则需要将取UDAG数据后，将消息拆分为一条发送消息落库
 */
@Component
@Slf4j
public class AuthenticMessageProcess {

    @Resource
    private ICrossChainMessageRepository crossChainMessageRepository;

    @Resource
    private IBlockchainManager blockchainManager;

    @Resource
    private IGovernManager governManager;

    @Resource
    private IRelayerNetworkManager relayerNetworkManager;

    @Resource
    private BlockchainIdleDCache blockchainIdleDCache;

    @Resource
    private IRelayerClientPool relayerClientPool;

    @Resource
    private IBCDNSManager bcdnsManager;

    @Value("${relayer.service.process.sdp.acl_on:true}")
    private boolean sdpACLOn;

    // TODO: 当支持TP-PROOF之后，应该从网络中获得到UCP，其携带着TP-PROOF
    public boolean doProcess(AuthMsgWrapper amMsgWrapper) {

        log.info(
                "process auth msg : (src_domain: {}, id: {}, if_remote: {})",
                amMsgWrapper.getDomain(),
                amMsgWrapper.getAuthMsgId(),
                amMsgWrapper.isNetworkAM()
        );

        if (
                amMsgWrapper.getProcessState() == AuthMsgProcessStateEnum.PROCESSED
                        || amMsgWrapper.getProcessState() == AuthMsgProcessStateEnum.REJECTED
        ) {
            log.error("auth msg repeat process : {}", amMsgWrapper.getAuthMsgId());
            return true;
        } else if (
                amMsgWrapper.getTrustLevel() == AuthMsgTrustLevelEnum.NEGATIVE_TRUST
                        && amMsgWrapper.getProcessState() != AuthMsgProcessStateEnum.PROVED
        ) {
            log.error("auth msg with NEGATIVE_TRUST its state error : {}-{}", amMsgWrapper.getAuthMsgId(), amMsgWrapper.getProcessState());
            return false;
        }

        try {
            if (!amMsgWrapper.isNetworkAM()) {
                processLocalAM(amMsgWrapper);
            } else {
                processRemoteAM(amMsgWrapper, null);
            }

            if (
                    amMsgWrapper.getProcessState() == AuthMsgProcessStateEnum.PROCESSED
                            || amMsgWrapper.getProcessState() == AuthMsgProcessStateEnum.PROCESSED_NO_PROOF
            ) {
                log.info(
                        "process high layer protocol {} of am message : (src_domain: {}, id: {}, if_remote: {})",
                        UpperProtocolTypeBeyondAMEnum.parseFromValue(amMsgWrapper.getAuthMessage().getUpperProtocol()).name(),
                        amMsgWrapper.getDomain(),
                        amMsgWrapper.getAuthMsgId(),
                        amMsgWrapper.isNetworkAM()
                );
                // 处理上层协议
                if (amMsgWrapper.getAuthMessage().getUpperProtocol() == UpperProtocolTypeBeyondAMEnum.SDP.getCode()) {
                    processSDPMsg(amMsgWrapper);
                } else {
                    throw new RuntimeException("unsupported am upper protocol type: " + amMsgWrapper.getProtocolType());
                }
            }

            return crossChainMessageRepository.updateAuthMessage(amMsgWrapper);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.SERVICE_CORE_PROCESS_AUTH_MSG_PROCESS_FAILED,
                    e,
                    "process auth msg failed: (src_domain: {}, id: {}, if_remote: {})",
                    amMsgWrapper.getDomain(),
                    amMsgWrapper.getAuthMsgId(),
                    amMsgWrapper.isNetworkAM()
            );
        }

    }

    private void processLocalAM(AuthMsgWrapper authMsgWrapper) {

        // 填充区块链信息
        if (!StrUtil.isAllNotEmpty(authMsgWrapper.getProduct(), authMsgWrapper.getBlockchainId())) {
            DomainCertWrapper domainCertWrapper = blockchainManager.getDomainCert(authMsgWrapper.getDomain());
            if (ObjectUtil.isNull(domainCertWrapper)) {
                throw new RuntimeException("domain cert not exist: " + authMsgWrapper.getDomain());
            }

            authMsgWrapper.setProduct(domainCertWrapper.getBlockchainProduct());
            authMsgWrapper.setBlockchainId(domainCertWrapper.getBlockchainId());
        }

        if (StrUtil.isEmpty(authMsgWrapper.getAmClientContractAddress())) {
            // 填充合约信息
            BlockchainMeta blockchainMeta = blockchainManager.getBlockchainMeta(
                    authMsgWrapper.getProduct(),
                    authMsgWrapper.getBlockchainId()
            );
            authMsgWrapper.setAmClientContractAddress(
                    blockchainMeta.getProperties().getAmClientContractAddress()
            );
        }

        if (
                authMsgWrapper.getTrustLevel() == AuthMsgTrustLevelEnum.POSITIVE_TRUST
                        && authMsgWrapper.getProcessState() == AuthMsgProcessStateEnum.PENDING
        ) {
            // TODO : 这里意味着POSITIVE_TRUST的AM还没有TP-PROOF，所以标记为PROCESSED_NO_PROOF，
            //  在后续将这些消息的证明再发送给中继或者提交给链上
            authMsgWrapper.setProcessState(AuthMsgProcessStateEnum.PROCESSED_NO_PROOF);
        }

        authMsgWrapper.setProcessState(AuthMsgProcessStateEnum.PROCESSED);
    }

    // TODO: 当支持TP-PROOF之后，应该从网络中获得到UCP，其携带着TP-PROOF
    private void processRemoteAM(AuthMsgWrapper authMsgWrapper, UniformCrosschainPacketContext ucpContext) {
        // TODO : 如果没有TP-PROOF应该标记为PROCESSED_NO_PROOF，
        //  这在PTC Ready之后版本增加
        if (
                authMsgWrapper.getTrustLevel() == AuthMsgTrustLevelEnum.POSITIVE_TRUST
                        && ObjectUtil.isNull(ucpContext)
        ) {
            // TODO : 这里意味着POSITIVE_TRUST的AM还没有TP-PROOF，所以标记为PROCESSED_NO_PROOF，
            //  发送中继应该后续对这种消息再传过来TP-PROOF
            authMsgWrapper.setProcessState(AuthMsgProcessStateEnum.PROCESSED_NO_PROOF);
        }
        authMsgWrapper.setProcessState(AuthMsgProcessStateEnum.PROCESSED);
    }

    private void processSDPMsg(AuthMsgWrapper authMsgWrapper) {

        SDPMsgWrapper sdpMsgWrapper = parseSDPMsgFrom(authMsgWrapper);

        // 接收者在本地
        if (StrUtil.isNotEmpty(sdpMsgWrapper.getReceiverBlockchainId())) {
            this.blockchainIdleDCache.setLastAMProcessTime(
                    sdpMsgWrapper.getReceiverBlockchainProduct(),
                    sdpMsgWrapper.getReceiverBlockchainId()
            );
            processLocalSDPMsg(sdpMsgWrapper);
            return;
        }

        processRemoteSDPMsg(sdpMsgWrapper);
    }

    public SDPMsgWrapper parseSDPMsgFrom(AuthMsgWrapper authMsgWrapper) {

        SDPMsgWrapper sdpMsgWrapper = new SDPMsgWrapper();

        sdpMsgWrapper.setSdpMessage(
                (AbstractSDPMessage) SDPMessageFactory.createSDPMessage(
                        authMsgWrapper.getAuthMessage().getPayload()
                )
        );

        sdpMsgWrapper.setAuthMsgWrapper(authMsgWrapper);

        if (StrUtil.isEmpty(sdpMsgWrapper.getReceiverBlockchainDomain())) {
            log.error(
                    "receiver domain is empty from am (src_domain: {}, id: {}, if_remote: {})",
                    authMsgWrapper.getDomain(),
                    authMsgWrapper.getAuthMsgId(),
                    authMsgWrapper.isNetworkAM()
            );
            sdpMsgWrapper.setProcessState(SDPMsgProcessStateEnum.MSG_ILLEGAL);
            sdpMsgWrapper.setTxFailReason("Empty receiver domain");
            return sdpMsgWrapper;
        }

        if (blockchainManager.hasBlockchain(sdpMsgWrapper.getReceiverBlockchainDomain())) {
            BlockchainMeta receiverBlockchainMeta = blockchainManager.getBlockchainMetaByDomain(
                    sdpMsgWrapper.getReceiverBlockchainDomain()
            );
            if (ObjectUtil.isNull(receiverBlockchainMeta)) {
                log.error(
                        "receiver blockchain not exist for domain {} from am (src_domain: {}, id: {}, if_remote: {})",
                        sdpMsgWrapper.getReceiverBlockchainDomain(),
                        authMsgWrapper.getDomain(),
                        authMsgWrapper.getAuthMsgId(),
                        authMsgWrapper.isNetworkAM()
                );
                sdpMsgWrapper.setProcessState(SDPMsgProcessStateEnum.MSG_ILLEGAL);
                sdpMsgWrapper.setTxFailReason("Blockchain supposed existed but not");
                return sdpMsgWrapper;
            }
            sdpMsgWrapper.setReceiverBlockchainId(receiverBlockchainMeta.getBlockchainId());
            sdpMsgWrapper.setReceiverBlockchainProduct(receiverBlockchainMeta.getProduct());
            sdpMsgWrapper.setReceiverAMClientContract(receiverBlockchainMeta.getProperties().getAmClientContractAddress());
        }

        sdpMsgWrapper.setProcessState(SDPMsgProcessStateEnum.PENDING);

        log.info(
                "parse auth msg to sdp msg : ( version: {}, from_blockchain: {}, sender: {}, receiver_blockchain: {}, receiver: {}, seq: {}, am_id: {} )",
                sdpMsgWrapper.getVersion(),
                sdpMsgWrapper.getSenderBlockchainDomain(),
                sdpMsgWrapper.getMsgSender(),
                sdpMsgWrapper.getReceiverBlockchainDomain(),
                sdpMsgWrapper.getMsgReceiver(),
                sdpMsgWrapper.getMsgSequence(),
                authMsgWrapper.getAuthMsgId()
        );

        return sdpMsgWrapper;

    }

    private String findRemoteRelayer(SDPMsgWrapper sdpMsgWrapper) {
        RelayerNetwork.Item relayerNetworkItem = relayerNetworkManager.findNetworkItemByDomainName(sdpMsgWrapper.getReceiverBlockchainDomain());
        // Network也没有该domain，就拒绝请求
        if (ObjectUtil.isNull(relayerNetworkItem)) {
            log.info("can't find receiver domain {} in all network from local data", sdpMsgWrapper.getReceiverBlockchainDomain());
            return null;
        }
        if (relayerNetworkItem.getSyncState() != RelayerNodeSyncStateEnum.SYNC) {
            log.warn("receiver domain {} router existed but not on state SYNC.", sdpMsgWrapper.getReceiverBlockchainDomain());
            return null;
        }
        return relayerNetworkItem.getNodeId();
    }

    private void processRemoteSDPMsg(SDPMsgWrapper sdpMsgWrapper) {
        String relayerNodeId = findRemoteRelayer(sdpMsgWrapper);
        try {
            if (ObjectUtil.isNull(relayerNodeId)) {
                // TODO: call BCDNS for domain router
                // TODO: 当中继之间初次建立链接的时候，应当使用锁，阻止其他的消息处理时，也去做同样的事情；
            }

            if (ObjectUtil.isNull(relayerNodeId)) {
                // Even try BCDNS but not found
                sdpMsgWrapper.setProcessState(SDPMsgProcessStateEnum.MSG_REJECTED);
                sdpMsgWrapper.setTxFailReason("Unknown receiver domain");

                crossChainMessageRepository.putSDPMessage(sdpMsgWrapper);
            }

            // TODO: 未来需要更新接口，以支持不同信任等级的流程
            relayerClientPool.getRelayerClient(relayerNetworkManager.getRelayerNode(relayerNodeId))
                    .amRequest(
                            sdpMsgWrapper.getSenderBlockchainDomain(),
                            Base64.getEncoder().encodeToString(sdpMsgWrapper.getAuthMsgWrapper().getAuthMessage().encode()),
                            "",
                            new String(sdpMsgWrapper.getAuthMsgWrapper().getRawLedgerInfo())
                    );
        } catch (Exception e) {
            log.error(
                    "failed to send message " +
                            "( version: {}, from_blockchain: {}, sender: {}, receiver_blockchain: {}, receiver: {}, seq: {}, am_id: {} ) " +
                            "to remote relayer {}",
                    sdpMsgWrapper.getVersion(),
                    sdpMsgWrapper.getSenderBlockchainDomain(),
                    sdpMsgWrapper.getMsgSender(),
                    sdpMsgWrapper.getReceiverBlockchainDomain(),
                    sdpMsgWrapper.getMsgReceiver(),
                    sdpMsgWrapper.getMsgSequence(),
                    sdpMsgWrapper.getAuthMsgWrapper().getAuthMsgId(),
                    relayerNodeId,
                    e
            );
            return;
        }

        log.info(
                "successful to send message " +
                        "( version: {}, from_blockchain: {}, sender: {}, receiver_blockchain: {}, receiver: {}, seq: {}, am_id: {} ) " +
                        "to remote relayer {}",
                sdpMsgWrapper.getVersion(),
                sdpMsgWrapper.getSenderBlockchainDomain(),
                sdpMsgWrapper.getMsgSender(),
                sdpMsgWrapper.getReceiverBlockchainDomain(),
                sdpMsgWrapper.getMsgReceiver(),
                sdpMsgWrapper.getMsgSequence(),
                sdpMsgWrapper.getAuthMsgWrapper().getAuthMsgId(),
                relayerNodeId
        );
    }

    private void processLocalSDPMsg(SDPMsgWrapper sdpMsgWrapper) {
        switch (sdpMsgWrapper.getProcessState()) {
            case PENDING:
                // 检查ACL规则，若规则不满足，则状态置为am_msg_rejected
                checkCrossChainACLRule(sdpMsgWrapper);
                if (sdpMsgWrapper.getProcessState() == SDPMsgProcessStateEnum.MSG_REJECTED) {
                    log.warn(
                            "sdp msg ( version: {}, from_blockchain: {}, sender: {}, receiver_blockchain: {}, receiver: {}, seq: {}, am_id: {} ) is rejected by ACL",
                            sdpMsgWrapper.getVersion(),
                            sdpMsgWrapper.getSenderBlockchainDomain(),
                            sdpMsgWrapper.getMsgSender(),
                            sdpMsgWrapper.getReceiverBlockchainDomain(),
                            sdpMsgWrapper.getMsgReceiver(),
                            sdpMsgWrapper.getMsgSequence(),
                            sdpMsgWrapper.getAuthMsgWrapper().getAuthMsgId()
                    );
                }
                break;
            case MSG_ILLEGAL:
                log.error(
                        "process illegal sdp msg ( version: {}, from_blockchain: {}, sender: {}, receiver_blockchain: {}, receiver: {}, seq: {}, am_id: {} ) on receiving locally",
                        sdpMsgWrapper.getVersion(),
                        sdpMsgWrapper.getSenderBlockchainDomain(),
                        sdpMsgWrapper.getMsgSender(),
                        sdpMsgWrapper.getReceiverBlockchainDomain(),
                        sdpMsgWrapper.getMsgReceiver(),
                        sdpMsgWrapper.getMsgSequence(),
                        sdpMsgWrapper.getAuthMsgWrapper().getAuthMsgId()
                );
                break;
        }

        crossChainMessageRepository.putSDPMessage(sdpMsgWrapper);
        log.info(
                "successful to process sdp msg ( version: {}, from_blockchain: {}, sender: {}, receiver_blockchain: {}, receiver: {}, seq: {}, am_id: {} ) locally",
                sdpMsgWrapper.getVersion(),
                sdpMsgWrapper.getSenderBlockchainDomain(),
                sdpMsgWrapper.getMsgSender(),
                sdpMsgWrapper.getReceiverBlockchainDomain(),
                sdpMsgWrapper.getMsgReceiver(),
                sdpMsgWrapper.getMsgSequence(),
                sdpMsgWrapper.getAuthMsgWrapper().getAuthMsgId()
        );
    }

    private void checkCrossChainACLRule(SDPMsgWrapper sdpMsgWrapper) {

        if (!sdpACLOn || sdpMsgWrapper.isBlockchainSelfCall()) {
            return;
        }

        if (
                !governManager.verifyCrossChainMsgACL(
                        sdpMsgWrapper.getSenderBlockchainDomain(),
                        sdpMsgWrapper.getMsgSender(),
                        sdpMsgWrapper.getReceiverBlockchainDomain(),
                        sdpMsgWrapper.getMsgReceiver()
                )
        ) {
            sdpMsgWrapper.setProcessState(SDPMsgProcessStateEnum.MSG_REJECTED);
            sdpMsgWrapper.setTxFailReason("msg rejected by ACL");
        }
    }
}
