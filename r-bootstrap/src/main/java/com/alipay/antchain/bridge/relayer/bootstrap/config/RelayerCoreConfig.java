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

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.crypto.PemUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.RelayerCredentialSubject;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.GRpcBBCPluginManager;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.IBBCPluginManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerCredentialManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.core.types.network.ws.WsSslFactory;
import com.alipay.antchain.bridge.relayer.dal.repository.IPluginServerRepository;
import com.alipay.antchain.bridge.relayer.server.network.WSRelayerServer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class RelayerCoreConfig {

    @Value("${relayer.plugin_server_manager.grpc.auth.tls.client.key.path:config/relayer.key}")
    private String clientKeyPath;

    @Value("${relayer.plugin_server_manager.grpc.auth.tls.client.ca.path:config/relayer.crt}")
    private String clientCaPath;

    @Value("${relayer.plugin_server_manager.grpc.thread.num:32}")
    private int clientThreadNum;

    @Value("${relayer.plugin_server_manager.grpc.heartbeat.thread.num:4}")
    private int clientHeartbeatThreadNum;

    @Value("${relayer.plugin_server_manager.grpc.heartbeat.delayed_time:5000}")
    private long heartbeatDelayedTime;

    @Value("${relayer.plugin_server_manager.grpc.heartbeat.error_limit:5}")
    private int errorLimitForHeartbeat;

    @Value("${relayer.network.node.crosschain_cert_path:null}")
    private String relayerCrossChainCertPath;

    @Value("${relayer.network.node.private_key_path}")
    private String relayerPrivateKeyPath;

    @Value("${relayer.network.node.server.mode:https}")
    private String localNodeServerMode;

    @Value("${relayer.network.node.server.port:8082}")
    private int localNodeServerPort;

    @Value("#{systemConfigRepository.defaultNetworkId}")
    private String defaultNetworkId;

    @Value("${relayer.network.node.server.as_discovery:false}")
    private boolean isDiscoveryService;

    @Resource
    private TransactionTemplate transactionTemplate;

    public AbstractCrossChainCertificate getLocalRelayerCrossChainCertificate() {
        AbstractCrossChainCertificate relayerCertificate = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
                FileUtil.readBytes(relayerCrossChainCertPath)
        );
        Assert.equals(
                CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE,
                relayerCertificate.getType()
        );
        return relayerCertificate;
    }

    public RelayerCredentialSubject getLocalRelayerCredentialSubject() {
        return RelayerCredentialSubject.decode(getLocalRelayerCrossChainCertificate().getCredentialSubject());
    }

    public PrivateKey getLocalPrivateKey() {
        return PemUtil.readPemPrivateKey(new ByteArrayInputStream(FileUtil.readBytes(relayerPrivateKeyPath)));
    }

    public String getLocalRelayerNodeId() {
        return RelayerNodeInfo.calculateNodeId(getLocalRelayerCrossChainCertificate());
    }

    @Bean
    @Autowired
    public IBBCPluginManager bbcPluginManager(IPluginServerRepository pluginServerRepository, RedissonClient redisson) {
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
                        new ArrayBlockingQueue<>(clientThreadNum * 20),
                        new ThreadFactoryBuilder().setNameFormat("plugin_manager-grpc-%d").build(),
                        new ThreadPoolExecutor.CallerRunsPolicy()
                ),
                redisson.getExecutorService(
                        "plugin_manager-heartbeat-service"
                ),
                heartbeatDelayedTime,
                errorLimitForHeartbeat
        );
    }

    @Bean
    @Autowired
    public WSRelayerServer wsRelayerServer(
            @Qualifier("wsRelayerServerExecutorService") ExecutorService wsRelayerServerExecutorService,
            WsSslFactory wsSslFactory,
            IRelayerNetworkManager relayerNetworkManager,
            IRelayerCredentialManager relayerCredentialManager
    ) {
        try {
            return new WSRelayerServer(
                    localNodeServerMode,
                    localNodeServerPort,
                    defaultNetworkId,
                    wsRelayerServerExecutorService,
                    wsSslFactory,
                    relayerNetworkManager,
                    relayerCredentialManager,
                    isDiscoveryService
            );
        } catch (Exception e) {
            throw new BeanInitializationException(
                    "failed to initialize bean wsRelayerServer",
                    e
            );
        }
    }
}
