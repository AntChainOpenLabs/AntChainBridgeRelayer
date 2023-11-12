package com.alipay.antchain.bridge.relayer.core.service.process;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.impl.BlockchainIdleDCache;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
@Getter
public class ProcessService {

    @Resource
    private AuthenticMessageProcess authenticMessageProcess;

    @Resource
    private ICrossChainMessageRepository crossChainMessageRepository;

    @Resource
    private IBlockchainManager blockchainManager;

    @Resource
    private BlockchainIdleDCache blockchainIdleDCache;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource(name = "processServiceThreadsPool")
    private ExecutorService processServiceThreadsPool;

    @Value("${relayer.service.process.ccmsg.batch_size:64}")
    private int ccmsgBatchSize;

    /**
     * 执行指定区块的分布式调度任务
     *
     * @param blockchainProduct
     * @param blockchainId
     */
    public void process(String blockchainProduct, String blockchainId) {

        log.debug("process service run with blockchain {}", blockchainId);

        List<AuthMsgWrapper> authMsgWrapperList = ListUtil.toList();

        String domainName = blockchainManager.getBlockchainDomain(blockchainProduct, blockchainId);

        if (this.blockchainIdleDCache.ifAMProcessIdle(blockchainProduct, blockchainId)) {
            log.info("am process : blockchain is idle {}-{}.", blockchainProduct, blockchainId);
        } else if (StrUtil.isNotEmpty(domainName)) {
            authMsgWrapperList = crossChainMessageRepository.peekAuthMessages(
                    domainName,
                    AuthMsgProcessStateEnum.PENDING,
                    ccmsgBatchSize
            );
        }

        if (authMsgWrapperList.size() > 0) {
            log.info("peek {} auth msg from pool: {}-{}", authMsgWrapperList.size(), blockchainProduct, blockchainId);
        } else {
            this.blockchainIdleDCache.setLastEmptyAMPoolTime(blockchainProduct, blockchainId);
            log.debug("{}-{} for auth msg is idle", blockchainProduct, blockchainId);
        }

        // 使用线程池并发执行
        List<Future> futures = authMsgWrapperList.stream().map(
                authMsgWrapper -> processServiceThreadsPool.submit(
                        wrapAMTask(authMsgWrapper.getAuthMsgId())
                )
        ).collect(Collectors.toList());

        // 等待执行完成
        do {
            for (Future future : ListUtil.reverse(ListUtil.toList(futures))) {
                try {
                    future.get(30 * 1000L, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    log.error("worker interrupted exception for blockchain {}-{}.", blockchainProduct, blockchainId, e);
                } catch (ExecutionException e) {
                    log.error("worker execution fail for blockchain {}-{}.", blockchainProduct, blockchainId, e);
                } catch (TimeoutException e) {
                    log.warn("worker query timeout exception for blockchain {}-{}.", blockchainProduct, blockchainId, e);
                } finally {
                    if (future.isDone()) {
                        futures.remove(future);
                    }
                }
            }
        } while (!futures.isEmpty());
    }

    private Runnable wrapAMTask(long amId) {
        return () -> transactionTemplate.execute(
                new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {

                        AuthMsgWrapper am = crossChainMessageRepository.getAuthMessage(amId, true);
                        if (ObjectUtil.isNull(am)) {
                            log.error("none auth message found for auth id {}", amId);
                            return;
                        }

                        try {
                            if (!authenticMessageProcess.doProcess(am)) {
                                throw new RuntimeException(
                                        StrUtil.format("failed to process auth message for auth id {} for unknown reason", amId)
                                );
                            }

                        } catch (AntChainBridgeRelayerException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new AntChainBridgeRelayerException(
                                    RelayerErrorCodeEnum.SERVICE_CORE_PROCESS_PROCESS_CCMSG_FAILED,
                                    e,
                                    "failed to process auth message for auth id {}",
                                    amId
                            );
                        }
                    }
                }
        );
    }
}
