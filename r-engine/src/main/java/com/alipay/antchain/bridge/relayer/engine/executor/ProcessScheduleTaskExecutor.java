package com.alipay.antchain.bridge.relayer.engine.executor;

import java.util.concurrent.ExecutorService;
import javax.annotation.Resource;

import com.alipay.antchain.bridge.relayer.commons.model.DistributedTask;
import com.alipay.antchain.bridge.relayer.core.service.process.ProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProcessScheduleTaskExecutor extends BaseScheduleTaskExecutor {

    @Resource
    private ProcessService processService;

    @Autowired
    public ProcessScheduleTaskExecutor(@Qualifier("processScheduleTaskExecutorThreadsPool") ExecutorService executorService) {
        super(executorService);
    }

    @Override
    public Runnable genTask(DistributedTask task) {
        return () -> {
            try {
                processService.process(task.getBlockchainProduct(), task.getBlockchainId());
            } catch (Throwable e) {
                log.error("ProcessScheduleTaskExecutor failed for blockchain {}", task.getBlockchainId(), e);
            }
        };
    }
}
