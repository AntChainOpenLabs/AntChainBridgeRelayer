package com.alipay.antchain.bridge.relayer.engine.executor;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import cn.hutool.core.lang.Assert;
import com.alipay.antchain.bridge.relayer.commons.model.IDistributedTask;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

/**
 * 分布式任务基类，传入具体分布式任务，使用线程池异步执行
 */
@Slf4j
@Getter
public abstract class BaseScheduleTaskExecutor {

    private final ExecutorService executor;

    private final Map<String, Future> currentTasks = Maps.newConcurrentMap();

    public BaseScheduleTaskExecutor(ExecutorService executor) {
        Assert.notNull(executor);
        this.executor = executor;
    }

    /**
     * 分布式任务执行基类
     *
     * @param task
     */
    @Synchronized
    public void execute(IDistributedTask task) {

        // 该任务是否已经在执行
        if (currentTasks.containsKey(task.getUniqueTaskKey())) {
            if (!currentTasks.get(task.getUniqueTaskKey()).isDone()) {
                log.debug("task is running : {}", task.getUniqueTaskKey());
                return;
            } else {
                log.debug("task finish : {}", task.getUniqueTaskKey());
                currentTasks.remove(task.getUniqueTaskKey());
            }
        }

        // 判断时间片是否结束
        if (task.ifFinish()) {
            log.debug("task out of time slice : {}", task.getUniqueTaskKey());
            return;
        }

        // 触发执行
        log.debug("execute task : {}", task.getUniqueTaskKey());

        Future currentTask = executor.submit(genTask(task));

        this.currentTasks.put(task.getUniqueTaskKey(), currentTask);
    }

    //*******************************************
    // 子类实现
    //*******************************************

    public abstract Runnable genTask(IDistributedTask task);
}
