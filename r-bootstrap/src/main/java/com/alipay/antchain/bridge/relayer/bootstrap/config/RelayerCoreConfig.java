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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;

import com.alipay.antchain.bridge.relayer.core.manager.bbc.GRpcBBCPluginManager;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.IBBCPluginManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.RelayerNetworkManagerImpl;
import com.alipay.antchain.bridge.relayer.dal.repository.IBlockchainRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.IPluginServerRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.IRelayerNetworkRepository;
import com.alipay.antchain.bridge.relayer.dal.repository.ISystemConfigRepository;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class RelayerCoreConfig {

    @Value("${plugin_server_manager.grpc.auth.tls.client.key.path:config/relayer.key}")
    private String clientKeyPath;

    @Value("${plugin_server_manager.grpc.auth.tls.client.ca.path:config/relayer.crt}")
    private String clientCaPath;

    @Value("${plugin_server_manager.grpc.thread.num:32}")
    private int clientThreadNum;

    @Value("${plugin_server_manager.grpc.heartbeat.thread.num:4}")
    private int clientHeartbeatThreadNum;

    @Value("${plugin_server_manager.grpc.heartbeat.delayed_time:5000}")
    private long heartbeatDelayedTime;

    @Value("${plugin_server_manager.grpc.heartbeat.error_limit:5}")
    private int errorLimitForHeartbeat;

    @Value("${relayer.network.node.pubkey_algo:RSA}")
    private String pubkeyAlgo;

    @Value("${relayer.network.node.pubkey:null}")
    private String pubkeyBase64;

    @Value("${relayer.network.node.pubkey_file:null}")
    private String pubkeyFile;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Bean
    @Autowired
    public IBBCPluginManager bbcPluginManager(IPluginServerRepository pluginServerRepository) {
        return new GRpcBBCPluginManager(
                clientKeyPath,
                clientCaPath,
                pluginServerRepository,
                transactionTemplate,
                new ThreadPoolExecutor(
                        clientThreadNum,
                        clientThreadNum,
                        3000,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<Runnable>(clientThreadNum * 20),
                        new ThreadFactoryBuilder().setNameFormat("plugin_manager-grpc-%d").build(),
                        new ThreadPoolExecutor.CallerRunsPolicy()
                ),
                new ScheduledThreadPoolExecutor(
                        clientHeartbeatThreadNum,
                        new ThreadFactoryBuilder().setNameFormat("plugin_manager-heartbeat-%d").build()
                ),
                heartbeatDelayedTime,
                errorLimitForHeartbeat
        );
    }

    @Bean
    @Autowired
    public IRelayerNetworkManager relayerNetworkManager(
            IRelayerNetworkRepository relayerNetworkRepository,
            IBlockchainRepository blockchainRepository,
            ISystemConfigRepository systemConfigRepository
    ) {
        return new RelayerNetworkManagerImpl(
                pubkeyAlgo,
                pubkeyBase64,
                pubkeyFile,
                relayerNetworkRepository,
                blockchainRepository,
                systemConfigRepository
        );
    }
}
