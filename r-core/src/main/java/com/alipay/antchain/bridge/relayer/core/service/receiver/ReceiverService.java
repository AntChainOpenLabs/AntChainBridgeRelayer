package com.alipay.antchain.bridge.relayer.core.service.receiver;

import java.util.Base64;
import java.util.List;
import javax.annotation.Resource;

import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgCommitResult;
import com.alipay.antchain.bridge.relayer.core.service.receiver.handler.AsyncReceiveHandler;
import com.alipay.antchain.bridge.relayer.core.service.receiver.handler.SyncReceiveHandler;
import com.alipay.antchain.bridge.relayer.dal.repository.impl.BlockchainIdleDCache;
import org.springframework.stereotype.Service;

/**
 * OracleService核心引擎的接收者，用于接收来自链上、链外的服务请求。
 * <p>
 * 该引擎设置有同步处理器、异步处理器，根据需求选择。
 */
@Service
public class ReceiverService {

    /**
     * 同步receiver处理器
     */
    @Resource
    private SyncReceiveHandler syncReceiveHandler;

    /**
     * 异步receiver处理器
     */
    @Resource
    private AsyncReceiveHandler asyncReceiveHandler;

    @Resource
    private BlockchainIdleDCache blockchainIdleDCache;

    /**
     * 链外请求receive接口
     *
     * @param authMsg
     * @param authMsg
     * @return
     */
    public void receiveOffChainAMRequest(String domainName, String authMsg, String udagProof, String ledgerInfo) {

        AuthMsgWrapper authMsgWrapper = new AuthMsgWrapper();
        authMsgWrapper.setDomain(domainName);
        authMsgWrapper.setLedgerInfo(ledgerInfo);
        authMsgWrapper.setAuthMessage(
                AuthMessageFactory.createAuthMessage(
                        Base64.getDecoder().decode(authMsg)
                )
        );

        try {
            syncReceiveHandler.receiveOffChainAMRequest(authMsgWrapper, udagProof);
        } catch (AntChainBridgeRelayerException e) {
            throw e;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.SERVICE_MULTI_ANCHOR_PROCESS_REMOTE_AM_PROCESS_FAILED,
                    e,
                    "failed to process remote am request from blockchain {}",
                    domainName
            );
        }
    }

    /**
     * 接收am消息的接口
     *
     * @param authMsgWrappers
     * @return
     */
    public void receiveAM(List<AuthMsgWrapper> authMsgWrappers) {
        asyncReceiveHandler.receiveAuthMessages(authMsgWrappers);
        if (!authMsgWrappers.isEmpty()) {
            blockchainIdleDCache.setLastAMReceiveTime(
                    authMsgWrappers.get(0).getProduct(),
                    authMsgWrappers.get(0).getBlockchainId()
            );
        }
    }

    /**
     * receive am client eceipt接口
     *
     * @param commitResults
     * @return
     */
    public boolean receiveAMClientReceipts(List<SDPMsgCommitResult> commitResults) {

        if (!commitResults.isEmpty()) {
            blockchainIdleDCache.setLastAMResponseTime(
                    commitResults.get(0).getReceiveProduct(),
                    commitResults.get(0).getReceiveBlockchainId()
            );
        }
        return asyncReceiveHandler.receiveAMClientReceipt(commitResults);
    }
}
