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

package com.alipay.antchain.bridge.relayer.core.types.network;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerBlockchainInfo;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.core.types.network.request.*;
import com.alipay.antchain.bridge.relayer.core.types.network.response.HandshakeRespPayload;
import com.alipay.antchain.bridge.relayer.core.types.network.response.RelayerResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Endpoint Client基类
 */
@Getter
@Setter
@Slf4j
public abstract class BaseRelayerClient implements RelayerClient {

    private RelayerNodeInfo remoteNodeInfo;

    private IRelayerNetworkManager relayerNetworkManager;

    private String defaultNetworkId;

    public BaseRelayerClient(
            RelayerNodeInfo remoteNodeInfo,
            IRelayerNetworkManager relayerNetworkManager,
            String defaultNetworkId
    ) {
        this.remoteNodeInfo = remoteNodeInfo;
        this.relayerNetworkManager = relayerNetworkManager;
        this.defaultNetworkId = defaultNetworkId;
    }

    /**
     * 发送请求
     *
     * @param relayerRequest
     * @return
     */
    public abstract RelayerResponse sendRequest(RelayerRequest relayerRequest);

    public abstract void startup();

    public abstract boolean shutdown();

    @Override
    public RelayerNodeInfo getRelayerNodeInfo() {
        RelayerRequest request = new GetRelayerNodeInfoRelayerRequest(
                relayerNetworkManager.getLocalNodeId(),
                relayerNetworkManager.getLocalRelayerCertificate(),
                relayerNetworkManager.getLocalNodeSigAlgo()
        );
        relayerNetworkManager.signRelayerRequest(request);

        RelayerResponse response = sendRequest(request);
        if (ObjectUtil.isNull(response) || !response.isSuccess()) {
            throw new RuntimeException(
                    StrUtil.format(
                            "failed to getRelayerNodeInfo: {} - {}",
                            response.getResponseCode(), response.getResponseMessage()
                    )
            );
        }

        return RelayerNodeInfo.decode(Base64.decode(response.getResponsePayload()));
    }

    @Override
    public RelayerBlockchainInfo getRelayerBlockchainInfo(String domainToQuery) {
        RelayerRequest request = new GetRelayerBlockchainInfoRelayerRequest(
                relayerNetworkManager.getLocalNodeId(),
                relayerNetworkManager.getLocalRelayerCertificate(),
                relayerNetworkManager.getLocalNodeSigAlgo(),
                domainToQuery
        );
        relayerNetworkManager.signRelayerRequest(request);

        RelayerResponse response = sendRequest(request);
        if (ObjectUtil.isNull(response) || !response.isSuccess()) {
            throw new RuntimeException(
                    StrUtil.format(
                            "getRelayerBlockchainInfo for domain {} failed: {} - {}",
                            domainToQuery,
                            response.getResponseCode(),
                            response.getResponseMessage()
                    )
            );
        }

        return RelayerBlockchainInfo.decode(response.getResponsePayload());
    }

    @Override
    public void amRequest(String domainName, String authMsg, String udagProof, String ledgerInfo) {
        RelayerRequest request = new AMRelayerRequest(
                relayerNetworkManager.getLocalNodeId(),
                relayerNetworkManager.getLocalRelayerCertificate(),
                relayerNetworkManager.getLocalNodeSigAlgo(),
                udagProof,
                authMsg,
                domainName,
                ledgerInfo
        );
        relayerNetworkManager.signRelayerRequest(request);

        RelayerResponse response = sendRequest(request);
        if (ObjectUtil.isNull(response)) {
            throw new RuntimeException(
                    StrUtil.format(
                            "am request from domain {} failed: empty response found",
                            domainName
                    )
            );
        } else if (!response.isSuccess()) {
            throw new RuntimeException(
                    StrUtil.format("am request from domain {} failed: (code: {}, msg: {})",
                            domainName, response.getResponseCode(), response.getResponseMessage()
                    )
            );
        }
    }

    @Override
    public RelayerNodeInfo handshake(RelayerNodeInfo senderNodeInfo, String networkId) {
        RelayerRequest request = new HandshakeRelayerRequest(
                relayerNetworkManager.getLocalNodeId(),
                relayerNetworkManager.getLocalRelayerCertificate(),
                relayerNetworkManager.getLocalNodeSigAlgo(),
                senderNodeInfo,
                defaultNetworkId
        );

        RelayerResponse response = sendRequest(request);
        if (ObjectUtil.isNull(response)) {
            throw new RuntimeException(
                    StrUtil.format(
                            "handshake with relayer {} failed: response is empty",
                            senderNodeInfo.getNodeId()
                    )
            );
        } else if (!response.isSuccess()) {
            throw new RuntimeException(
                    String.format("handshake with relayer {} failed: (code: %d, msg: %s)",
                            response.getResponseCode(), response.getResponseMessage()
                    )
            );
        }

        HandshakeRespPayload handshakeRespPayload = HandshakeRespPayload.decodeFromJson(response.getResponsePayload());
        RelayerNodeInfo remoteNodeInfo = RelayerNodeInfo.decode(
                Base64.decode(handshakeRespPayload.getRemoteNodeInfo())
        );

        remoteNodeInfo.getProperties().getProperties().put(
                "network_id",
                ObjectUtil.defaultIfEmpty(
                        handshakeRespPayload.getRemoteNetworkId(),
                        defaultNetworkId
                )
        );

        log.debug("handshake with relayer {} success with response: {}", this.remoteNodeInfo.getNodeId(), response.getResponsePayload());

        return remoteNodeInfo;
    }
}
