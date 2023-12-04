package com.alipay.antchain.bridge.relayer.core.service.confirm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.relayer.commons.constant.SDPMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgCommitResult;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgWrapper;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlockchainClient;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.BlockchainClientPool;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.HeteroBlockchainClient;
import com.alipay.antchain.bridge.relayer.core.types.network.IRelayerClientPool;
import com.alipay.antchain.bridge.relayer.core.types.network.RelayerClient;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AMConfirmService {

    @Value("${relayer.service.confirm.batch_size:32}")
    private int confirmBatchSize;

    @Resource
    private ICrossChainMessageRepository crossChainMessageRepository;

    @Resource(name = "confirmServiceThreadsPool")
    private ExecutorService confirmServiceThreadsPool;

    @Resource
    private BlockchainClientPool blockchainClientPool;

    @Resource
    private IRelayerClientPool relayerClientPool;

    @Resource
    private IRelayerNetworkManager relayerNetworkManager;

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
        List<SDPMsgWrapper> sdpMsgWrappersSent = crossChainMessageRepository.peekSDPMessagesSent(
                product,
                blockchainId,
                SDPMsgProcessStateEnum.REMOTE_PENDING,
                confirmBatchSize
        );

        List<Future<List<CrossChainMessageReceipt>>> futureList = new ArrayList<>();
        sdpMsgWrappers.forEach(
                sdpMsgWrapper -> futureList.add(
                        confirmServiceThreadsPool.submit(
                                () -> ListUtil.toList(heteroBlockchainClient.queryCommittedTxReceipt(sdpMsgWrapper.getTxHash()))
                        )
                )
        );
        sdpMsgWrappersSent.stream().collect(Collectors.groupingBy(SDPMsgWrapper::getReceiverBlockchainDomain))
                .forEach((key, value) -> futureList.add(
                        confirmServiceThreadsPool.submit(
                                () -> {
                                    RelayerClient relayerClient = relayerClientPool.getRelayerClientByDomain(key);
                                    if (ObjectUtil.isNull(relayerClient)) {
                                        relayerClient = relayerClientPool.getRelayerClient(
                                                relayerNetworkManager.getRelayerNode(relayerNetworkManager.findRemoteRelayer(key), false),
                                                key
                                        );
                                    }
                                    List<String> ucpIds = value.stream().map(
                                                    sdpMsgWrapper -> crossChainMessageRepository.getUcpId(
                                                            sdpMsgWrapper.getAuthMsgWrapper().getAuthMsgId()
                                                    )
                                            ).filter(StrUtil::isNotEmpty)
                                            .collect(Collectors.toList());
                                    if (ObjectUtil.isNotEmpty(ucpIds)) {
                                        return relayerClient.queryCrossChainMessageReceipts(ucpIds);
                                    }
                                    return new ArrayList<>();
                                }
                        )
                ));

        List<SDPMsgCommitResult> commitResults = new ArrayList<>();
        futureList.forEach(
                future -> {
                    List<CrossChainMessageReceipt> receipts;
                    try {
                        receipts = future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(
                                String.format("failed to query cross-chain receipt for ( product: %s, bid: %s )", product, blockchainId),
                                e
                        );
                    }
                    receipts.forEach(
                            receipt -> {
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
                }
        );

        crossChainMessageRepository.updateSDPMessageResults(commitResults);
    }
}
