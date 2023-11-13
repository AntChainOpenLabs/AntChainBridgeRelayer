package com.alipay.antchain.bridge.relayer.core.service.anchor.workers;

import java.util.List;

import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.core.service.anchor.context.AnchorProcessContext;
import com.alipay.antchain.bridge.relayer.core.service.receiver.ReceiverService;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlock;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.HeterogeneousBlock;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * amclient合约worker，负责处理区块里面的am消息，转发am消息给OracleServiceAM消息执行引擎
 */
@Slf4j
@Getter
public class AuthMessageWorker extends BlockWorker {

    private final ReceiverService receiver;

    public AuthMessageWorker(AnchorProcessContext processContext) {
        super(processContext);
        this.receiver = processContext.getReceiverService();
    }

    @Override
    public boolean process(AbstractBlock block) {
        return dealHeteroBlockchain(block);
    }

    public boolean dealHeteroBlockchain(AbstractBlock block) {
        try {
            HeterogeneousBlock heteroBlock = (HeterogeneousBlock) block;
            if (heteroBlock.getCrossChainMessages().isEmpty()) {
                return true;
            }

            List<AuthMsgWrapper> authMessages = heteroBlock.toAuthMsgWrappers();
            if (authMessages.isEmpty()) {
                return true;
            }

            receiver.receiveAM(authMessages);
        } catch (Exception e) {
            log.error(
                    "failed to process block {} from blockchain {}-{}",
                    block.getHeight(),
                    block.getProduct(),
                    block.getBlockchainId(),
                    e
            );
            return false;
        }
        return true;
    }
}