/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.mychain.oracle.anchor.cli.glclient;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import com.alipay.mychain.oracle.servicemanager.grpcserver.grpc.admin.AdminRequest;
import com.alipay.mychain.oracle.servicemanager.grpcserver.grpc.admin.AdminResponse;
import com.alipay.mychain.oracle.servicemanager.grpcserver.grpc.admin.AdministratorServiceGrpc;
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
