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

package com.alipay.antchain.bridge.relayer.server.network;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.xml.ws.Endpoint;

import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerCredentialManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.core.service.receiver.ReceiverService;
import com.alipay.antchain.bridge.relayer.core.types.network.ws.WsSslFactory;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class WSRelayerServer {

    private final String serverMode;

    private final int port;

    private Endpoint endpoint;

    private HttpsServer httpsServer;

    private HttpServer httpServer;

    private final WSRelayerServerAPI wsRelayerServerAPI;

    private final ExecutorService workers;

    private final WsSslFactory wsSslFactory;

    public WSRelayerServer(
            String serverMode,
            int port,
            String defaultNetworkId,
            ExecutorService wsRelayerServerExecutorService,
            WsSslFactory wsSslFactory,
            IRelayerNetworkManager relayerNetworkManager,
            IRelayerCredentialManager relayerCredentialManager,
            ReceiverService receiverService,
            boolean isDiscoveryService
    ) {
        this.serverMode = serverMode;
        this.port = port;
        this.wsRelayerServerAPI = new WSRelayerServerAPImpl(
                relayerNetworkManager,
                relayerCredentialManager,
                receiverService,
                defaultNetworkId,
                isDiscoveryService
        );
        this.workers = wsRelayerServerExecutorService;
        this.wsSslFactory = wsSslFactory;

        startup();
    }

    public void startup() {

        try {
            log.info("your mode for webservice relayer node server is {}", serverMode);
            switch (serverMode) {
                case "http":
                    startWithHttp();
                    break;
                case "https":
                    startWithHttps(false);
                    break;
                case "https_client_auth":
                    startWithHttps(true);
                    break;
                default:
                    log.warn("mode for webservice connection is not found and start in https with two way authentication. ");
                    startWithHttps(true);
                    break;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void startWithHttps(boolean needClientAuth) throws Exception {
        httpsServer = HttpsServer.create(new InetSocketAddress(port), 0);
        httpsServer.setHttpsConfigurator(
                new HttpsConfigurator(wsSslFactory.getSslContext()) {
                    public void configure(HttpsParameters params) {
                        SSLContext c = getSSLContext();
                        SSLParameters sslparams = c.getDefaultSSLParameters();
                        sslparams.setNeedClientAuth(needClientAuth);
                        params.setSSLParameters(sslparams);
                    }
                }
        );

        log.info("endpoint startup webservice : {}", httpsServer.getAddress().toString());
        endpoint = Endpoint.create(this.wsRelayerServerAPI);
        endpoint.publish(httpsServer.createContext("/WSEndpointServer"));

        httpsServer.setExecutor(workers);
        httpsServer.start();
    }

    private void startWithHttp() throws Exception {
        httpServer = HttpServer.create(new InetSocketAddress(port), 16);
        log.info("endpoint startup webservice : {}", httpServer.getAddress().toString());

        endpoint = Endpoint.create(this.wsRelayerServerAPI);
        endpoint.publish(httpServer.createContext("/WSEndpointServer"));

        httpServer.setExecutor(workers);
        httpServer.start();
    }

    public boolean shutdown() {
        if (null != endpoint) {
            endpoint.stop();
        }

        if (null != httpsServer) {
            httpsServer.stop(0);
        }

        if (null != httpServer) {
            httpServer.stop(0);
        }

        return true;
    }
}
