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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.alipay.antchain.bridge.relayer.server.admin.AdminRpcServerImpl;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ServerConfig {

    @Value("${relayer.admin_server.host:127.0.0.1}")
    private String adminServerHost;

    @Value("${relayer.admin_server.port:8088}")
    private Integer adminServerPort;

    @Bean
    public Server pluginMgrServer(@Autowired AdminRpcServerImpl adminRpcServer) throws IOException {
        log.info("Starting admin managing server on {}:{}", adminServerHost, adminServerPort);
        return NettyServerBuilder.forAddress(
                        new InetSocketAddress(
                                InetAddress.getByName(adminServerHost),
                                adminServerPort
                        )
                ).addService(adminRpcServer)
                .build()
                .start();
    }
}
