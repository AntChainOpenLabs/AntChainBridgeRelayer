/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.relayer.bootstrap.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThreadsConfig {

    @Value("${relayer.network.node.server.threads.core_size:32}")
    private int wsRelayerServerCoreSize;

    @Value("${relayer.network.node.server.threads.total_size:64}")
    private int wsRelayerServerTotalSize;

    @Value("${relayer.network.node.client.threads.core_size:32}")
    private int wsRelayerClientCoreSize;

    @Value("${relayer.network.node.client.threads.total_size:64}")
    private int wsRelayerClientTotalSize;

    @Value("${relayer.service.process.threads.core_size:32}")
    private int processServiceCoreSize;

    @Value("${relayer.service.process.threads.total_size:64}")
    private int processServiceTotalSize;

    @Value("${relayer.service.committer.threads.core_size:32}")
    private int committerServiceCoreSize;

    @Value("${relayer.service.committer.threads.total_size:64}")
    private int committerServiceTotalSize;

    @Value("${relayer.service.anchor.sync_task.threads.core_size:16}")
    private int blockSyncTaskCoreSize;

    @Value("${relayer.service.anchor.sync_task.threads.total_size:256}")
    private int blockSyncTaskTotalSize;

    @Value("${relayer.service.confirm.threads.core_size:16}")
    private int confirmServiceCoreSize;

    @Value("${relayer.service.confirm.threads.total_size:64}")
    private int confirmServiceTotalSize;

    @Bean(name = "wsRelayerServerExecutorService")
    public ExecutorService wsRelayerServerExecutorService() {
        return new ThreadPoolExecutor(
                wsRelayerServerCoreSize,
                wsRelayerServerTotalSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10000),
                new ThreadFactoryBuilder().setNameFormat("ws-relayer-server-worker-%d").build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    @Bean(name = "wsRelayerClientThreadsPool")
    public ExecutorService wsRelayerClientThreadsPool() {
        return new ThreadPoolExecutor(
                wsRelayerClientCoreSize,
                wsRelayerClientTotalSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10000),
                new ThreadFactoryBuilder().setNameFormat("ws-relayer-client-worker-%d").build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    @Bean(name = "processServiceThreadsPool")
    public ExecutorService processServiceThreadsPool() {
        return new ThreadPoolExecutor(
                processServiceCoreSize,
                processServiceTotalSize,
                1000,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10000),
                new ThreadFactoryBuilder().setNameFormat("Process-worker-%d").build(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    @Bean(name = "committerServiceThreadsPool")
    public ExecutorService committerServiceThreadsPool() {
        return new ThreadPoolExecutor(
                committerServiceCoreSize,
                committerServiceTotalSize,
                5000L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(10000),
                new ThreadFactoryBuilder().setNameFormat("Committer-worker-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(name = "blockSyncTaskThreadsPool")
    public ExecutorService blockSyncTaskThreadsPool() {
        return new ThreadPoolExecutor(
                blockSyncTaskCoreSize,
                blockSyncTaskTotalSize,
                5000L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1280),
                new ThreadFactoryBuilder().setNameFormat("BlockSyncTask-worker-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(name = "confirmServiceThreadsPool")
    public ExecutorService confirmServiceThreadsPool() {
        return new ThreadPoolExecutor(
                confirmServiceCoreSize,
                confirmServiceTotalSize,
                5000L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1000),
                new ThreadFactoryBuilder().setNameFormat("AMConfirm-worker-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
