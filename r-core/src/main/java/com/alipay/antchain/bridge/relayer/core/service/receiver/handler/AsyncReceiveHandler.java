package com.alipay.antchain.bridge.relayer.core.service.receiver.handler;

import java.util.List;
import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgCommitResult;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Slf4j
public class AsyncReceiveHandler {

    @Resource
    private ICrossChainMessageRepository crossChainMessageRepository;

    @Resource
    private TransactionTemplate transactionTemplate;

    public void receiveAuthMessages(List<AuthMsgWrapper> authMsgWrappers) {

        for (AuthMsgWrapper am : authMsgWrappers) {
            log.info("receive a AuthenticMessage from {} with upper protocol {}", am.getDomain(), am.getProtocolType());
            am.setProcessState(AuthMsgProcessStateEnum.PENDING);
        }

        int rowsNum = crossChainMessageRepository.putAuthMessages(authMsgWrappers);
        if (authMsgWrappers.size() != rowsNum) {
            throw new RuntimeException(
                    StrUtil.format(
                            "failed to save auth messages: rows number {} inserted not equal to list size {}",
                            rowsNum, authMsgWrappers.size()
                    )
            );
        }
        log.info("[asyncReceiver] put am_pending AuthenticMessage to pool success");
    }

    public boolean receiveAMClientReceipt(List<SDPMsgCommitResult> commitResults) {

        // 处理空值
        if (commitResults.isEmpty()) {
            return true;
        }

        List<Integer> results = transactionTemplate.execute(
                status -> crossChainMessageRepository.updateSDPMessageResults(commitResults)
        );

        if (ObjectUtil.isEmpty(results)) {
            return false;
        }

        for (int i = 0; i < results.size(); i++) {
            if (0 == results.get(i)) {
                // sql变更行数为0，表示tx hash在DB不存在，可能有多种原因导致，可以跳过，打个warn
                log.warn(
                        "sdp msg to blockchain {} processed failed: ( tx: {}, fail_reason: {} )",
                        commitResults.get(i).getReceiveBlockchainId(),
                        commitResults.get(i).getTxHash(),
                        commitResults.get(i).getFailReason()
                );
            } else {
                log.info(
                        "sdp msg to blockchain {}-{} processed success: ( tx: {}, committed: {}, confirm: {}, )",
                        commitResults.get(i).getReceiveProduct(),
                        commitResults.get(i).getReceiveBlockchainId(),
                        commitResults.get(i).getTxHash(),
                        commitResults.get(i).isCommitSuccess(),
                        commitResults.get(i).isConfirmed()
                );
            }
        }

        return true;
    }
}
