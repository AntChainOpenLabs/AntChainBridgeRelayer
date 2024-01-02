package com.alipay.antchain.bridge.relayer.core.service.anchor.workers;

import com.alipay.antchain.bridge.relayer.core.service.anchor.context.AnchorProcessContext;
import com.alipay.antchain.bridge.relayer.core.service.receiver.ReceiverService;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlock;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.HeterogeneousBlock;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class CrossChainMessageWorker extends BlockWorker {

    private final ReceiverService receiver;

    public CrossChainMessageWorker(AnchorProcessContext processContext) {
        super(processContext);
        this.receiver = processContext.getReceiverService();
    }

    @Override
    public boolean process(AbstractBlock block) {
        return dealHeteroBlockchain(block);
    }

    public boolean dealHeteroBlockchain(AbstractBlock block) {
        try {
            receiver.receiveBlock((HeterogeneousBlock) block);
            log.info("success to process crosschain messages on block {} from blockchain {}", block.getHeight(), block.getBlockchainId());
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