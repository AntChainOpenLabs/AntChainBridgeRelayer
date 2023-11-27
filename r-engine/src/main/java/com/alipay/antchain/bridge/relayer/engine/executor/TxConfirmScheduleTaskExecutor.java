package com.alipay.antchain.bridge.relayer.engine.executor;

import java.util.concurrent.ExecutorService;
import javax.annotation.Resource;

import com.alipay.antchain.bridge.relayer.commons.model.BlockchainDistributedTask;
import com.alipay.antchain.bridge.relayer.commons.model.IDistributedTask;
import com.alipay.antchain.bridge.relayer.core.service.confirm.AMConfirmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TxConfirmScheduleTaskExecutor extends BaseScheduleTaskExecutor {

    @Resource
    private AMConfirmService amConfirmService;

    public TxConfirmScheduleTaskExecutor(@Qualifier("confirmScheduleTaskExecutorThreadsPool") ExecutorService executorService) {
        super(executorService);
    }

    @Override
    public Runnable genTask(IDistributedTask task) {
        return () -> {
            if (task instanceof BlockchainDistributedTask) {
                try {
                    amConfirmService.process(
                            ((BlockchainDistributedTask) task).getBlockchainProduct(),
                            ((BlockchainDistributedTask) task).getBlockchainId()
                    );
                } catch (Exception e) {
                    log.error(
                            "failed to process am confirm task for ( product: {}, bid: {} )",
                            ((BlockchainDistributedTask) task).getBlockchainProduct(),
                            ((BlockchainDistributedTask) task).getBlockchainId(),
                            e
                    );
                }
            }
        };
    }
}
