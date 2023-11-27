package com.alipay.antchain.bridge.relayer.engine;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;

import com.alipay.antchain.bridge.relayer.engine.core.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Getter
@Order(1)
public class DistributedTaskEngine implements ApplicationRunner {

    @Resource
    private Activator activator;

    @Resource
    private Dispatcher dispatcher;

    @Resource
    private Duty duty;

    @Resource
    private BizDuty bizDuty;

    @Resource
    private Cleaner cleaner;

    @Resource
    private ScheduledExecutorService distributedTaskEngineScheduleThreadsPool;

    @Value("${relayer.engine.schedule.activate.period:1000}")
    private long activatePeriod;

    @Value("${relayer.engine.schedule.cleaner.period:30000}")
    private long cleanPeriod;

    @Value("${relayer.engine.schedule.dispatcher.period:1000}")
    private long dispatchPeriod;

    @Value("${relayer.engine.schedule.duty.period:100}")
    private long dutyPeriod;

    @Value("${relayer.engine.schedule.duty.period:3000}")
    private long bizDutyPeriod;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        log.info("Starting DistributedTask Engine Now");

        // schedule activator
        distributedTaskEngineScheduleThreadsPool.scheduleWithFixedDelay(
                () -> {
                    try {
                        activator.activate();
                    } catch (Throwable e) {
                        log.error("schedule activator failed.", e);
                    }
                },
                0,
                activatePeriod,
                TimeUnit.MILLISECONDS
        );

        // schedule dispatcher
        distributedTaskEngineScheduleThreadsPool.scheduleWithFixedDelay(
                () -> {
                    try {
                        dispatcher.dispatch();
                    } catch (Throwable e) {
                        log.error("schedule dispatch failed.", e);
                    }
                },
                0,
                dispatchPeriod,
                TimeUnit.MILLISECONDS
        );

        // schedule duty
        distributedTaskEngineScheduleThreadsPool.scheduleWithFixedDelay(
                () -> {
                    try {
                        duty.duty();
                    } catch (Throwable e) {
                        log.error("schedule duty failed.", e);
                    }
                },
                0,
                dutyPeriod,
                TimeUnit.MILLISECONDS
        );

        // schedule cleaner
        distributedTaskEngineScheduleThreadsPool.scheduleWithFixedDelay(
                () -> {
                    try {
                        cleaner.clean();
                    } catch (Throwable e) {
                        log.error("schedule cleaner failed.", e);
                    }
                },
                0,
                cleanPeriod,
                TimeUnit.MILLISECONDS
        );

        // schedule biz duty
        distributedTaskEngineScheduleThreadsPool.scheduleWithFixedDelay(
                () -> {
                    try {
                        bizDuty.duty();
                    } catch (Throwable e) {
                        log.error("schedule biz duty failed.", e);
                    }
                },
                0,
                bizDutyPeriod,
                TimeUnit.MILLISECONDS
        );
    }

    public void shutdown() {
        distributedTaskEngineScheduleThreadsPool.shutdown();
    }
}
