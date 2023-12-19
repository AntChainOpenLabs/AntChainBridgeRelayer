package com.alipay.antchain.bridge.relayer.core.service.committer;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.Constants;
import com.alipay.antchain.bridge.relayer.commons.constant.SDPMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgPackage;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgCommitResult;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgWrapper;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlockchainClient;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.BlockchainClientPool;
import com.alipay.antchain.bridge.relayer.core.utils.ProcessUtils;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.ISystemConfigRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.impl.BlockchainIdleDCache;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class CommitterService {

    @Resource(name = "committerServiceThreadsPool")
    private ExecutorService committerServiceThreadsPool;

    @Resource
    private IBlockchainManager blockchainManager;

    @Resource
    private BlockchainIdleDCache blockchainIdleDCache;

    @Resource
    private ICrossChainMessageRepository crossChainMessageRepository;

    @Resource
    private BlockchainClientPool blockchainClientPool;

    @Resource
    private ISystemConfigRepository systemConfigRepository;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Value("${relayer.service.committer.ccmsg.batch_size:128}")
    private int commitBatchSize;

    @Value("${relayer.service.committer.threads.core_size:32}")
    private int committerServiceCoreSize;

    public void process(String blockchainProduct, String blockchainId) {

        if (isBusyBlockchain(blockchainProduct, blockchainId)) {
            log.info("blockchain {}-{} are too busy to receive new message", blockchainProduct, blockchainId);
            return;
        }

        List<SDPMsgWrapper> sdpMsgWrappers = new ArrayList<>();

        if (this.blockchainIdleDCache.ifAMCommitterIdle(blockchainProduct, blockchainId)) {
            log.debug("blockchain {}-{} has no messages processed recently, so skip it this committing process", blockchainProduct, blockchainId);
        } else {
            sdpMsgWrappers = crossChainMessageRepository.peekSDPMessages(
                    blockchainProduct,
                    blockchainId,
                    SDPMsgProcessStateEnum.PENDING,
                    commitBatchSize
            );
        }

        if (!sdpMsgWrappers.isEmpty()) {
            log.info("peek {} sdp msg for blockchain {} from pool", sdpMsgWrappers.size(), blockchainId);
        } else {
            this.blockchainIdleDCache.setLastEmptyAMSendQueueTime(blockchainProduct, blockchainId);
            log.debug("[committer] peek zero sdp msg for blockchain {} from pool", blockchainId);
        }

        // keyed by session key(msg.sender:msg.receiver)
        Map<String, List<SDPMsgWrapper>> sdpMsgsMap = groupSession(
                sdpMsgWrappers,
                committerServiceCoreSize
        );

        if (!sdpMsgsMap.isEmpty()) {
            log.info("peek {} sdp msg sessions for blockchain {} from pool", sdpMsgsMap.size(), blockchainId);
        } else {
            log.debug("peek zero sdp msg sessions for blockchain {} from pool", blockchainId);
        }

        List<Future> futures = new ArrayList<>();
        for (Map.Entry<String, List<SDPMsgWrapper>> entry : sdpMsgsMap.entrySet()) {
            futures.add(
                    committerServiceThreadsPool.submit(
                            wrapRequestTask(
                                    entry.getKey(),
                                    entry.getValue()
                            )
                    )
            );
        }

        // 等待执行完成
        ProcessUtils.waitAllFuturesDone(blockchainProduct, blockchainId, futures, log);
    }

    private boolean isBusyBlockchain(String blockchainProduct, String blockchainId) {

        String pendingLimit = systemConfigRepository.getSystemConfig(
                StrUtil.format("{}-{}-{}", Constants.PENDING_LIMIT, blockchainProduct, blockchainId)
        );

        boolean busy = false;
        if (!StrUtil.isEmpty(pendingLimit)) {
            busy = crossChainMessageRepository.countSDPMessagesByState(
                    blockchainProduct,
                    blockchainId,
                    SDPMsgProcessStateEnum.TX_PENDING
            ) >= Integer.parseInt(pendingLimit);
        }

        return busy;
    }

    private Map<String, List<SDPMsgWrapper>> groupSession(List<SDPMsgWrapper> sdpMsgWrappers, int remainingWorkerNum) {

        // keyed by session key(msg.sender:msg.receiver)
        Map<String, List<SDPMsgWrapper>> sdpMsgsMap = new HashMap<>();

        for (SDPMsgWrapper msg : sdpMsgWrappers) {
            String sessionKey = msg.getSessionKey();
            if (!sdpMsgsMap.containsKey(sessionKey)) {
                sdpMsgsMap.put(sessionKey, new ArrayList<>());
            }
            sdpMsgsMap.get(sessionKey).add(msg);
        }

        // 当前情况下，线程池剩余的线程数
        int leftWorkerNum = remainingWorkerNum - sdpMsgsMap.size();

        // 如果线程池有资源剩余，就将Unordered类型的消息拿出来，充分利用剩余资源
        if (leftWorkerNum >= 1) {
            // 所有的Unordered消息的Map
            // - key 使用session key
            // - value 是该session的SDP消息
            Map<String, List<SDPMsgWrapper>> unorderedMap = new HashMap<>();

            // 所有的Unordered消息的总数
            int totalSize = 0;
            for (Map.Entry<String, List<SDPMsgWrapper>> entry : sdpMsgsMap.entrySet()) {
                if (StrUtil.startWith(entry.getKey(), SDPMsgWrapper.UNORDERED_SDP_MSG_SESSION)) {
                    unorderedMap.put(entry.getKey(), entry.getValue());
                    totalSize += entry.getValue().size();
                }
            }

            // 如果无序消息的总数大于0，就按各个session的消息数目比例，均分掉剩余的线程
            if (!unorderedMap.isEmpty()) {
                // 因为要重新分配后，在add回unorderedMap，所以这里先删除
                unorderedMap.keySet().forEach(sdpMsgsMap::remove);
                leftWorkerNum += unorderedMap.size();

                // sessionNum是后面要用到多少个线程，每个session一个线程
                // 如果没有那么多的消息，就把消息总数作为新分配的session数目
                int sessionNum = Math.min(leftWorkerNum, totalSize);

                // 将原先的session，拆分到一个或者多个新session，将消息均匀分到这些新的session中
                for (Map.Entry<String, List<SDPMsgWrapper>> entry : unorderedMap.entrySet()) {
                    // count 就是该session需要拆分为新session的数目
                    // 按消息占总体的比例分配，最小为1
                    int count = Math.max(sessionNum * entry.getValue().size() / totalSize, 1);

                    // 将消息均分到信息的session中，直接add到p2pMsgsMap
                    for (int i = 0; i < entry.getValue().size(); i++) {
                        // 新的session key，利用余数均匀分配消息到新的session中
                        String key = String.format("%s-%d", entry.getKey(), i % count);
                        if (!sdpMsgsMap.containsKey(key)) {
                            sdpMsgsMap.put(key, Lists.newArrayList());
                        }
                        sdpMsgsMap.get(key).add(entry.getValue().get(i));
                    }
                }
            }
        }

        return sdpMsgsMap;
    }

    private Runnable wrapRequestTask(String sessionName, List<SDPMsgWrapper> sessionMsgs) {
        return () -> {
            Lock sessionLock = crossChainMessageRepository.getSessionLock(sessionName);
            sessionLock.lock();
            log.info("get distributed lock for session {}", sessionName);
            try {
                transactionTemplate.execute(
                        new TransactionCallbackWithoutResult() {
                            @Override
                            protected void doInTransactionWithoutResult(TransactionStatus status) {
                                // 这是个分布式并发任务，加了session锁后，要check下每个SDP消息的最新状态，防止重复处理
                                List<SDPMsgWrapper> sessionMsgsUpdate = filterOutdatedMsg(sessionMsgs);

                                // p2p按seq排序，后续需要按序提交
                                sortSDPMsgList(sessionMsgsUpdate);

                                for (SDPMsgWrapper sdpMsgWrapper : sessionMsgsUpdate) {
                                    // 逐笔提交，但包装为数组调用批量更新接口
                                    log.info("committing msg of id {} for session {}", sdpMsgWrapper.getId(), sessionName);
                                    batchCommitSDPMsg(sessionName, ListUtil.toList(sdpMsgWrapper));
                                }
                            }
                        }
                );
            } catch (AntChainBridgeRelayerException e) {
                throw e;
            } catch (Exception e) {
                throw new AntChainBridgeRelayerException(
                        RelayerErrorCodeEnum.SERVICE_COMMITTER_PROCESS_CCMSG_FAILED,
                        e,
                        "failed to commit session {} with {} messages",
                        sessionName, sessionMsgs.size()
                );
            } finally {
                sessionLock.unlock();
                log.info("release distributed lock for session {}", sessionName);
            }
        };
    }

    private List<SDPMsgWrapper> filterOutdatedMsg(List<SDPMsgWrapper> sessionMsgs) {
        return sessionMsgs.stream().filter(
                sdpMsgWrapper ->
                        crossChainMessageRepository.getSDPMessage(sdpMsgWrapper.getId(), true)
                                .getProcessState() == SDPMsgProcessStateEnum.PENDING
        ).collect(Collectors.toList());
    }

    /**
     * 已经上链过的消息直接更新为已上链（可能种种原因，之前上链时候的hash丢失了未更新到db）
     *
     * @param msgSet
     */
    private void updateExpiredMsg(ParsedSDPMsgSet msgSet) {
        if (!msgSet.getExpired().isEmpty()) {
            for (SDPMsgWrapper msg : msgSet.getExpired()) {
                msg.setProcessState(SDPMsgProcessStateEnum.TX_SUCCESS);
                log.info("AMCommitter: am {} has been committed on chain", msg.getAuthMsgWrapper().getAuthMsgId());
                if (!crossChainMessageRepository.updateSDPMessage(msg)) {
                    throw new RuntimeException("database update failed");
                }
            }
        }
    }

    private void batchCommitSDPMsg(String sessionName, List<SDPMsgWrapper> msgs) {

        String receiverProduct = msgs.get(0).getReceiverBlockchainProduct();
        String receiverBlockchainId = msgs.get(0).getReceiverBlockchainId();

        ParsedSDPMsgSet msgSet = parseSDPMsgList(receiverProduct, receiverBlockchainId, msgs);

        // 处理脏数据
        updateExpiredMsg(msgSet);

        // 处理新数据
        if (msgSet.getUpload().isEmpty()) {
            return;
        }

        log.info("AMCommitter: {} messages should uploaded for session {}", sessionName, msgSet.getUpload().size());

        // 提交上链
        try {

            msgSet.getUpload().forEach(
                    sdpMsgWrapper -> sdpMsgWrapper.setAuthMsgWrapper(
                            crossChainMessageRepository.getAuthMessage(sdpMsgWrapper.getAuthMsgWrapper().getAuthMsgId())
                    )
            );

            SDPMsgCommitResult res = commitAmPkg(
                    msgSet.getUpload().get(0).getReceiverBlockchainProduct(),
                    msgSet.getUpload().get(0).getReceiverBlockchainId(),
                    AuthMsgPackage.convertFrom(msgSet.getUpload(), null)
            );

            // Send tx result situations:
            //
            // - unknown_exception: unknown exception from upper operations
            // - tx_sent_failed: failed to send tx, returned with errcode and errmsg
            // - tx_success: tx has been sent successfully
            // - tx_pending: tx has been sent but pending to execute
            //   1. revert error, returned with REVERT_ERROR and errmsg
            //   2. other chain error, returned with errcode and errmsg
            //   3. success

            if (!res.isCommitSuccess()) {
                log.error("AMCommitter: amPkg for session {} commit failed, error msg: {}", sessionName, res.getFailReason());
                throw new RuntimeException("failed to commit msgs");
            }

            for (SDPMsgWrapper msg : msgSet.getUpload()) {
                msg.setTxSuccess(res.isCommitSuccess());
                msg.setTxHash(res.getTxHash());
                msg.setTxFailReason(res.getFailReason());
                msg.setProcessState(calculateProcessState(res));

                if (!crossChainMessageRepository.updateSDPMessage(msg)) {
                    throw new RuntimeException("database update failed");
                }
            }
            log.info("AMCommitter: messages for session {} status updated in database", sessionName);

        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.SERVICE_COMMITTER_PROCESS_COMMIT_SDP_FAILED,
                    e,
                    "failed to commit msg for session {}", sessionName
            );
        }
    }

    private SDPMsgProcessStateEnum calculateProcessState(SDPMsgCommitResult res) {
        if (res.isConfirmed()) {
            return res.isCommitSuccess() ? SDPMsgProcessStateEnum.TX_SUCCESS : SDPMsgProcessStateEnum.TX_FAILED;
        }
        return SDPMsgProcessStateEnum.TX_PENDING;
    }

    public SDPMsgCommitResult commitAmPkg(String receiverProduct, String receiverBlockchainId, AuthMsgPackage pkg) {
        SDPMsgCommitResult cr = new SDPMsgCommitResult();

        try {
            AbstractBlockchainClient client = blockchainClientPool.getClient(receiverProduct, receiverBlockchainId);
            if (ObjectUtil.isNull(client)) {
                client = blockchainClientPool.createClient(blockchainManager.getBlockchainMeta(receiverProduct, receiverBlockchainId));
            }

            AbstractBlockchainClient.SendResponseResult res = client.getAMClientContract()
                    .recvPkgFromRelayer(pkg.encode(), "");
            cr.setTxHash(res.getTxId());
            cr.setConfirmed(res.isConfirmed());
            cr.setCommitSuccess(res.isSuccess());
            cr.setFailReason(res.getErrorMessage());

            return cr;

        } catch (Exception e) {
            log.error("[txCommitter] commitAmPkg error", e);
            cr.setFailReason(e.getMessage());
            return cr;
        }
    }

    public long getSDPMsgSeqOnChain(String product, String blockchainId, SDPMsgWrapper sdpMsgWrapper) {

        try {
            return blockchainClientPool.getClient(product, blockchainId)
                    .getSDPMsgClientContract()
                    .querySDPMsgSeqOnChain(
                            sdpMsgWrapper.getSenderBlockchainDomain(),
                            sdpMsgWrapper.getMsgSender(),
                            sdpMsgWrapper.getReceiverBlockchainDomain(),
                            sdpMsgWrapper.getMsgReceiver()
                    );

        } catch (Exception e) {
            log.error(
                    "getSDPMsgSeqOnChain failed for ( sender_blockchain: {}-{}, sender: {}, receiver_blockchain: {}-{}, receiver: {} ) : ",
                    sdpMsgWrapper.getSenderBlockchainProduct(),
                    sdpMsgWrapper.getSenderBlockchainDomain(),
                    sdpMsgWrapper.getMsgSender(),
                    sdpMsgWrapper.getReceiverBlockchainProduct(),
                    sdpMsgWrapper.getReceiverBlockchainDomain(),
                    sdpMsgWrapper.getMsgReceiver(),
                    e
            );
            return 0;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class ParsedSDPMsgSet {

        /**
         * expired msg shouldn't upload to chain
         */
        private final List<SDPMsgWrapper> expired;

        /**
         * should upload to chain
         */
        private final List<SDPMsgWrapper> upload;

        public ParsedSDPMsgSet() {
            this.expired = new ArrayList<>();
            this.upload = new ArrayList<>();
        }
    }

    private ParsedSDPMsgSet parseSDPMsgList(String product, String blockchainId, List<SDPMsgWrapper> msgs) {
        long seqOnChain = -1;
        ParsedSDPMsgSet set = new ParsedSDPMsgSet();
        long lastIndex = seqOnChain; // NOTE: this is the first seq which need to send to blockchain

        for (SDPMsgWrapper msg : msgs) { // precondition: msgs was sorted

            if (SDPMsgWrapper.UNORDERED_SDP_MSG_SEQ == msg.getMsgSequence()) {
                set.getUpload().add(msg);
                continue;
            }

            if (seqOnChain == -1) {
                // NOTE: Always use the sequence number of session on blockchain
                seqOnChain = getSDPMsgSeqOnChain(product, blockchainId, msgs.get(0));
                lastIndex = seqOnChain; // NOTE: this is the first seq which need to send to blockchain

                log.info(
                        "session ( sender_blockchain: {}-{}, sender: {}, receiver_blockchain: {}-{}, receiver: {} ) seq on chain is {}",
                        msg.getSenderBlockchainProduct(),
                        msg.getSenderBlockchainDomain(),
                        msg.getMsgSender(),
                        msg.getReceiverBlockchainProduct(),
                        msg.getReceiverBlockchainDomain(),
                        msg.getMsgReceiver(),
                        seqOnChain
                );
            }

            if (msg.getMsgSequence() < seqOnChain) {
                log.info(
                        "ordered sdp msg ( sender_blockchain: {}-{}, sender: {}, receiver_blockchain: {}-{}, receiver: {} ) seq is expired: seq on chain is {} and seq in msg is {}",
                        msg.getSenderBlockchainProduct(),
                        msg.getSenderBlockchainDomain(),
                        msg.getMsgSender(),
                        msg.getReceiverBlockchainProduct(),
                        msg.getReceiverBlockchainDomain(),
                        msg.getMsgReceiver(),
                        seqOnChain,
                        msg.getMsgSequence()
                );
                set.getExpired().add(msg);
                continue;
            }

            if (msg.getMsgSequence() == lastIndex) { // collect consecutive msgs
                log.info(
                        "put ordered sdp msg with seq {} into upload list for session ( sender_blockchain: {}-{}, sender: {}, receiver_blockchain: {}-{}, receiver: {} )",
                        msg.getMsgSequence(),
                        msg.getSenderBlockchainProduct(),
                        msg.getSenderBlockchainDomain(),
                        msg.getMsgSender(),
                        msg.getReceiverBlockchainProduct(),
                        msg.getReceiverBlockchainDomain(),
                        msg.getMsgReceiver()
                );
                set.getUpload().add(msg);
                lastIndex++;
                continue;
            }

            log.warn(
                    "unhandled ordered msg seq {} for session ( sender_blockchain: {}-{}, sender: {}, receiver_blockchain: {}-{}, receiver: {} )",
                    msg.getMsgSequence(),
                    msg.getSenderBlockchainProduct(),
                    msg.getSenderBlockchainDomain(),
                    msg.getMsgSender(),
                    msg.getReceiverBlockchainProduct(),
                    msg.getReceiverBlockchainDomain(),
                    msg.getMsgReceiver()
            );
        }
        return set;
    }

    private void sortSDPMsgList(List<SDPMsgWrapper> msgs) {
        ListUtil.sort(
                msgs,
                Comparator.comparingInt(SDPMsgWrapper::getMsgSequence)
        );
    }
}