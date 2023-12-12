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

package com.alipay.antchain.bridge.relayer.cli.glclient;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest;
import com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse;
import com.alipay.antchain.bridge.relayer.core.grpc.admin.AdministratorServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 *  @author honglin.qhl
 *  @version $Id: GrpcClient.java, v 0.1 2017-06-17 下午2:56 honglin.qhl Exp $$
 */
public class GrpcClient {

    private final ManagedChannel channel;
    private final AdministratorServiceGrpc.AdministratorServiceBlockingStub blockingStub;

    String host = "localhost";

    private int port;

    public GrpcClient(int port) {
        this(ManagedChannelBuilder.forAddress("127.0.0.1", port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext());

        this.port = port;
    }

    public GrpcClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext());

        this.port = port;
        this.host = host;
    }

    public GrpcClient(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        blockingStub = AdministratorServiceGrpc.newBlockingStub(channel);
    }

    public boolean checkServerStatus() {

        try {
            Socket socket = new Socket(host, port);

            socket.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public AdminResponse adminRequest(AdminRequest request) {
        return blockingStub.adminRequest(request);
    }
}
