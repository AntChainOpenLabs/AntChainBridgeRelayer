package com.alipay.antchain.bridge.relayer.engine.executor;

import java.util.concurrent.ExecutorService;
import javax.annotation.Resource;

import com.alipay.antchain.bridge.relayer.commons.model.DistributedTask;
import com.alipay.antchain.bridge.relayer.core.service.committer.CommitterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CommitterScheduleTaskExecutor extends BaseScheduleTaskExecutor {

    @Resource
    private CommitterService committerService;

    @Autowired
    public CommitterScheduleTaskExecutor(@Qualifier("committerScheduleTaskExecutorThreadsPool") ExecutorService executorService) {
        super(executorService);
    }

    @Override
    public Runnable genTask(DistributedTask task) {
        return () -> {
            try {
                committerService.process(task.getBlockchainProduct(), task.getBlockchainId());
            } catch (Exception e) {
                log.error("CommitterScheduleTaskExecutor failed for blockchain {}", task.getBlockchainId(), e);
            }
        };
    }
}
