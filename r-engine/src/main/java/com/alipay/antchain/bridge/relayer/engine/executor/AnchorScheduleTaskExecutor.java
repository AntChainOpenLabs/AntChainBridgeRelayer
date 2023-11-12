package com.alipay.antchain.bridge.relayer.engine.executor;

import java.util.concurrent.ExecutorService;
import javax.annotation.Resource;

import com.alipay.antchain.bridge.relayer.commons.model.DistributedTask;
import com.alipay.antchain.bridge.relayer.core.service.anchor.MultiAnchorProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AnchorScheduleTaskExecutor extends BaseScheduleTaskExecutor {

    @Resource
    private MultiAnchorProcessService multiAnchorProcessService;

    @Autowired
    public AnchorScheduleTaskExecutor(@Qualifier("anchorScheduleTaskExecutorThreadsPool") ExecutorService executorService) {
        super(executorService);
    }

    @Override
    public Runnable genTask(DistributedTask task) {
        return () -> {
            try {
                multiAnchorProcessService.runAnchorProcess(task.getBlockchainProduct(), task.getBlockchainId());
            } catch (Throwable e) {
                log.error("AnchorScheduleTaskExecutor failed, blockchainId is {}", task.getBlockchainId(), e);
            }
        };
    }
}
