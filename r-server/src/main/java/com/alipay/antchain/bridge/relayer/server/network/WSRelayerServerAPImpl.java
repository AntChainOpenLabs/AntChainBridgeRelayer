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

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerBlockchainContent;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerBlockchainInfo;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerCredentialManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.core.service.receiver.ReceiverService;
import com.alipay.antchain.bridge.relayer.core.types.network.exception.RejectRequestException;
import com.alipay.antchain.bridge.relayer.core.types.network.request.*;
import com.alipay.antchain.bridge.relayer.core.types.network.response.HandshakeRespPayload;
import com.alipay.antchain.bridge.relayer.core.types.network.response.RelayerResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@WebService(targetNamespace = "http://ws.offchainapi.oracle.mychain.alipay.com/")
@SOAPBinding
@Slf4j
@NoArgsConstructor
public class WSRelayerServerAPImpl extends BaseRelayerServer implements WSRelayerServerAPI {

    public WSRelayerServerAPImpl(
            IRelayerNetworkManager relayerNetworkManager,
            IRelayerCredentialManager relayerCredentialManager,
            ReceiverService receiverService,
            String defaultNetworkId,
            boolean isDiscoveryServer
    ) {
        super(relayerNetworkManager, relayerCredentialManager, receiverService, defaultNetworkId, isDiscoveryServer);
    }

    @Override
    @WebMethod
    public String request(@WebParam(name = "relayerRequest") String relayerRequest) {

        log.debug("receive ws request");

        byte[] rawResponse = doRequest(Base64.decode(relayerRequest));

        String response = Base64.encode(rawResponse);

        log.debug("finish ws request process");

        return response;
    }

    /**
     * 处理请求
     *
     * @param rawRequest
     * @return
     */
    private byte[] doRequest(byte[] rawRequest) {

        try {
            RelayerRequest request = RelayerRequest.decode(rawRequest, RelayerRequest.class);
            if (ObjectUtil.isNull(request)) {
                log.error("Invalid relayer request that failed to decode");
                return RelayerResponse.createFailureResponse(
                        "failed to decode request",
                        getRelayerCredentialManager()
                ).encode();
            }

            if (isAboutDomain(request.getRequestType()) && !isDiscoveryServer()) {
                log.error("Invalid relayer request from relayer {} that sending request about Discovery Service", request.calcRelayerNodeId());
                return RelayerResponse.createFailureResponse(
                        "node you connected isn't a relayer discovery server",
                        getRelayerCredentialManager()
                ).encode();
            }

            switch (request.getRequestType()) {
                case GET_RELAYER_NODE_INFO:
                    return processGetRelayerNodeInfo().encode();
                case GET_RELAYER_BLOCKCHAIN_INFO:
                    return processGetRelayerBlockchainInfo(
                            GetRelayerBlockchainInfoRelayerRequest.createFrom(request)
                    ).encode();
                case AM_REQUEST:
                    return processAMRequest(
                            AMRelayerRequest.createFrom(request)
                    ).encode();
                case HANDSHAKE:
                    return processHandshakeRequest(
                            HandshakeRelayerRequest.createFrom(request)
                    ).encode();
                case GET_RELAYER_BLOCKCHAIN_CONTENT:
                    return processGetRelayerBlockchainContent(
                            GetRelayerBlockchainContentRelayerRequest.createFrom(request)
                    ).encode();
                default:
                    return RelayerResponse.createFailureResponse(
                            "request type not supported: " + request.getRequestType().getCode(),
                            getRelayerCredentialManager()
                    ).encode();
            }
        } catch (Exception e) {
            log.error("unexpected exception happened: ", e);
            return RelayerResponse.createFailureResponse(
                    "unexpected exception happened",
                    getRelayerCredentialManager()
            ).encode();
        }
    }

    private RelayerResponse processGetRelayerNodeInfo() {
        return RelayerResponse.createSuccessResponse(
                () -> Base64.encode(getRelayerNetworkManager().getRelayerNodeInfo().getEncode()),
                getRelayerCredentialManager()
        );
    }

    private RelayerResponse processGetRelayerBlockchainInfo(GetRelayerBlockchainInfoRelayerRequest request) {
        if (!getRelayerCredentialManager().validateRelayerRequest(request)) {
            log.error("failed to validate request from relayer {}", request.calcRelayerNodeId());
            return RelayerResponse.createFailureResponse(
                    "verify sig failed",
                    getRelayerCredentialManager()
            );
        }

        RelayerBlockchainInfo blockchainInfo;
        try {
            blockchainInfo = getRelayerNetworkManager().getRelayerBlockchainInfo(
                    request.getDomainToQuery()
            );
        } catch (AntChainBridgeRelayerException e) {
            log.error("failed to query blockchain info for domain {}", request.getDomainToQuery(), e);
            return RelayerResponse.createFailureResponse(
                    e.getMsg(),
                    getRelayerCredentialManager()
            );
        }
        if (ObjectUtil.isNull(blockchainInfo)) {
            return RelayerResponse.createFailureResponse(
                    "empty result",
                    getRelayerCredentialManager()
            );
        }

        return RelayerResponse.createSuccessResponse(
                blockchainInfo::encode,
                getRelayerCredentialManager()
        );
    }

    private RelayerResponse processGetRelayerBlockchainContent(GetRelayerBlockchainContentRelayerRequest request) {
        if (!getRelayerCredentialManager().validateRelayerRequest(request)) {
            log.error("failed to validate request from relayer {}", request.calcRelayerNodeId());
            return RelayerResponse.createFailureResponse(
                    "verify sig failed",
                    getRelayerCredentialManager()
            );
        }

        RelayerBlockchainContent blockchainContent;
        try {
            blockchainContent = getRelayerNetworkManager().getRelayerNodeInfoWithContent()
                    .getRelayerBlockchainContent();
        } catch (AntChainBridgeRelayerException e) {
            log.error("failed to query local blockchain content", e);
            return RelayerResponse.createFailureResponse(
                    e.getMsg(),
                    getRelayerCredentialManager()
            );
        }
        if (ObjectUtil.isNull(blockchainContent)) {
            return RelayerResponse.createFailureResponse(
                    "empty result",
                    getRelayerCredentialManager()
            );
        }

        return RelayerResponse.createSuccessResponse(
                blockchainContent::encodeToJson,
                getRelayerCredentialManager()
        );
    }

    private RelayerResponse processAMRequest(AMRelayerRequest request) {
        if (!getRelayerCredentialManager().validateRelayerRequest(request)) {
            log.error("failed to validate request from relayer {}", request.calcRelayerNodeId());
            return RelayerResponse.createFailureResponse(
                    "verify sig failed",
                    getRelayerCredentialManager()
            );
        }

        try {
            amRequest(
                    request.getDomainName(),
                    request.getAuthMsg(),
                    request.getUdagProof(),
                    request.getLedgerInfo()
            );
        } catch (RejectRequestException e) {
            log.error(
                    "reject am request from (blockchain: {}, relayer: {}) failed: ",
                    request.getDomainName(), request.calcRelayerNodeId(),
                    e
            );
            return RelayerResponse.createFailureResponse(
                    e.getErrorMsg(),
                    getRelayerCredentialManager()
            );
        } catch (AntChainBridgeRelayerException e) {
            log.error(
                    "handle am request from (blockchain: {}, relayer: {}) failed: ",
                    request.getDomainName(), request.calcRelayerNodeId(),
                    e
            );
            return RelayerResponse.createFailureResponse(
                    e.getMsg(),
                    getRelayerCredentialManager()
            );
        }

        log.info( "handle am request from relayer {} success: ", request.calcRelayerNodeId());

        return RelayerResponse.createSuccessResponse(
                () -> null,
                getRelayerCredentialManager()
        );
    }

    private RelayerResponse processHandshakeRequest(HandshakeRelayerRequest request) {
        if (!getRelayerCredentialManager().validateRelayerRequest(request)) {
            log.error("failed to validate request from relayer {}", request.calcRelayerNodeId());
            return RelayerResponse.createFailureResponse(
                    "verify sig failed",
                    getRelayerCredentialManager()
            );
        }

        try {
            doHandshake(
                    request.getSenderNodeInfo(),
                    request.getNetworkId()
            );
        } catch (AntChainBridgeRelayerException e) {
            log.error(
                    "handle handshake request from relayer {} failed: ",
                    request.calcRelayerNodeId(),
                    e
            );
            return RelayerResponse.createFailureResponse(
                    e.getMsg(),
                    getRelayerCredentialManager()
            );
        }

        log.info( "handle handshake request from relayer {} success: ", request.calcRelayerNodeId());

        return RelayerResponse.createSuccessResponse(
                new HandshakeRespPayload(
                        getDefaultNetworkId(),
                        Base64.encode(
                                getRelayerNetworkManager().getRelayerNodeInfoWithContent().encodeWithProperties()
                        )
                ),
                getRelayerCredentialManager()
        );
    }
}
