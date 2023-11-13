package com.alipay.antchain.bridge.relayer.core.service.confirm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.relayer.commons.constant.SDPMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgCommitResult;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgWrapper;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlockchainClient;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.BlockchainClientPool;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.HeteroBlockchainClient;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AMConfirmService {

    @Value("${relayer.service.confirm.batch_size:64}")
    private int confirmBatchSize;

    @Resource
    private ICrossChainMessageRepository crossChainMessageRepository;

    @Resource(name = "confirmServiceThreadsPool")
    private ExecutorService confirmServiceThreadsPool;

    @Resource
    private BlockchainClientPool blockchainClientPool;

    public void process(String product, String blockchainId) {
        AbstractBlockchainClient client = blockchainClientPool.getClient(product, blockchainId);
        if (ObjectUtil.isNull(client)) {
            log.info("waiting for hetero-client to start for blockchain {} - {}", product, blockchainId);
            return;
        }

        HeteroBlockchainClient heteroBlockchainClient = (HeteroBlockchainClient) client;
        List<SDPMsgWrapper> sdpMsgWrappers = crossChainMessageRepository.peekSDPMessages(
                product,
                blockchainId,
                SDPMsgProcessStateEnum.TX_PENDING,
                confirmBatchSize
        );
        if (sdpMsgWrappers.size() == 0) {
            return;
        }

        List<Future<CrossChainMessageReceipt>> futureList = new ArrayList<>();
        sdpMsgWrappers.forEach(
                sdpMsgWrapper -> futureList.add(
                        confirmServiceThreadsPool.submit(
                                () -> heteroBlockchainClient.queryCommittedTxReceipt(sdpMsgWrapper.getTxHash())
                        )
                )
        );

        List<SDPMsgCommitResult> commitResults = new ArrayList<>();
        futureList.forEach(
                future -> {
                    CrossChainMessageReceipt receipt;
                    try {
                        receipt = future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(
                                String.format("failed to query cross-chain receipt for ( product: %s, bid: %s )", product, blockchainId),
                                e
                        );
                    }
                    if (receipt.isConfirmed()) {
                        commitResults.add(
                                new SDPMsgCommitResult(
                                        product,
                                        blockchainId,
                                        receipt.getTxhash(),
                                        receipt.isSuccessful(),
                                        receipt.getErrorMsg(),
                                        System.currentTimeMillis()
                                )
                        );
                    }
                }
        );

        crossChainMessageRepository.updateSDPMessageResults(commitResults);
    }
}
