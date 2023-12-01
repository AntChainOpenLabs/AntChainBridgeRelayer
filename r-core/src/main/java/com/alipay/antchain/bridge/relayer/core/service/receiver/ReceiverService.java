package com.alipay.antchain.bridge.relayer.core.service.receiver;

import java.util.Base64;
import java.util.List;
import javax.annotation.Resource;

import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgCommitResult;
import com.alipay.antchain.bridge.relayer.commons.model.UniformCrosschainPacketContext;
import com.alipay.antchain.bridge.relayer.core.service.receiver.handler.AsyncReceiveHandler;
import com.alipay.antchain.bridge.relayer.core.service.receiver.handler.SyncReceiveHandler;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.HeterogeneousBlock;
import com.alipay.antchain.bridge.relayer.dal.repository.impl.BlockchainIdleDCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

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

    @Resource
    private TransactionTemplate transactionTemplate;

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

    public void receiveBlock(HeterogeneousBlock block) {
        transactionTemplate.execute(
                new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        if (block.getUniformCrosschainPacketContexts().isEmpty()) {
                            return;
                        }
                        receiveUCP(block.getUniformCrosschainPacketContexts());

                        List<AuthMsgWrapper> authMessages = block.toAuthMsgWrappers();
                        if (authMessages.isEmpty()) {
                            return;
                        }
                        receiveAM(authMessages);
                    }
                }
        );
    }

    /**
     * 接收am消息的接口
     *
     * @param authMsgWrappers
     * @return
     */
    private void receiveAM(List<AuthMsgWrapper> authMsgWrappers) {
        asyncReceiveHandler.receiveAuthMessages(authMsgWrappers);
        if (!authMsgWrappers.isEmpty()) {
            blockchainIdleDCache.setLastAMReceiveTime(
                    authMsgWrappers.get(0).getProduct(),
                    authMsgWrappers.get(0).getBlockchainId()
            );
        }
    }

    private void receiveUCP(List<UniformCrosschainPacketContext> ucpContexts) {
        asyncReceiveHandler.receiveUniformCrosschainPackets(ucpContexts);
        if (!ucpContexts.isEmpty()) {
            blockchainIdleDCache.setLastAMReceiveTime(
                    ucpContexts.get(0).getProduct(),
                    ucpContexts.get(0).getBlockchainId()
            );
        }
    }

    /**
     * receive am client receipt接口
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
