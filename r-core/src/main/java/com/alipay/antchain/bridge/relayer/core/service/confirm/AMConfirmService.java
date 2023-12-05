package com.alipay.antchain.bridge.relayer.core.service.confirm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
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
                        log.info("sdp confirmed : (tx: {}, is_success: {}, error_msg: {})",
                                receipt.getTxhash(), receipt.isSuccessful(), receipt.getErrorMsg());
                    }
                }
        );

        crossChainMessageRepository.updateSDPMessageResults(commitResults);
    }

    public void processSentToRemoteRelayer(String product, String blockchainId) {
        List<SDPMsgWrapper> sdpMsgWrappersSent = crossChainMessageRepository.peekSDPMessagesSent(
                product,
                blockchainId,
                SDPMsgProcessStateEnum.REMOTE_PENDING,
                confirmBatchSize
        );
        if (ObjectUtil.isEmpty(sdpMsgWrappersSent)) {
            return;
        }

        List<Future<Map<Long, CrossChainMessageReceipt>>> futureList = new ArrayList<>();
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
                                    Map<String, Long> ucpIdsMap = value.stream().map(
                                                    sdpMsgWrapper -> MapUtil.entry(
                                                            sdpMsgWrapper.getId(),
                                                            crossChainMessageRepository.getUcpId(
                                                                    sdpMsgWrapper.getAuthMsgWrapper().getAuthMsgId()
                                                            )
                                                    )
                                            ).filter(entry -> StrUtil.isNotEmpty(entry.getValue()))
                                            .collect(Collectors.toMap(
                                                    Map.Entry::getValue,
                                                    Map.Entry::getKey
                                            ));
                                    if (ObjectUtil.isNotEmpty(ucpIdsMap)) {
                                        Map<String, CrossChainMessageReceipt> receiptMap = relayerClient.queryCrossChainMessageReceipts(
                                                ListUtil.toList(ucpIdsMap.keySet())
                                        );
                                        if (ObjectUtil.isEmpty(receiptMap)) {
                                            return new HashMap<>();
                                        }
                                        return receiptMap.entrySet().stream()
                                                .collect(Collectors.toMap(
                                                        entry -> ucpIdsMap.get(entry.getKey()),
                                                        Map.Entry::getValue
                                                ));
                                    }
                                    return new HashMap<>();
                                }
                        )
                ));

        List<SDPMsgCommitResult> commitResults = new ArrayList<>();
        futureList.forEach(
                future -> {
                    Map<Long, CrossChainMessageReceipt> receiptMap;
                    try {
                        receiptMap = future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(
                                String.format("failed to query cross-chain receipt for ( product: %s, bid: %s )", product, blockchainId),
                                e
                        );
                    }
                    receiptMap.forEach(
                            (k, receipt) -> {
                                if (receipt.isConfirmed()) {
                                    commitResults.add(
                                            new SDPMsgCommitResult(
                                                    k,
                                                    product,
                                                    blockchainId,
                                                    receipt.getTxhash(),
                                                    receipt.isSuccessful(),
                                                    receipt.getErrorMsg(),
                                                    System.currentTimeMillis()
                                            )
                                    );
                                    log.info("sdp {} confirmed by remote relayer: (tx: {}, is_success: {}, error_msg: {})",
                                            k, receipt.getTxhash(), receipt.isSuccessful(), receipt.getErrorMsg());
                                } else {
                                    log.info("sdp {} still not confirmed by remote relayer", k);
                                }
                            }
                    );
                }
        );

        crossChainMessageRepository.updateSDPMessageResults(commitResults);
    }
}
