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

import java.security.Signature;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerBlockchainContent;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerBlockchainInfo;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerCredentialManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.core.service.receiver.ReceiverService;
import com.alipay.antchain.bridge.relayer.core.types.network.exception.RejectRequestException;
import com.alipay.antchain.bridge.relayer.core.types.network.request.*;
import com.alipay.antchain.bridge.relayer.core.types.network.response.HandshakeRespPayload;
import com.alipay.antchain.bridge.relayer.core.types.network.response.HelloCompleteRespPayload;
import com.alipay.antchain.bridge.relayer.core.types.network.response.HelloStartRespPayload;
import com.alipay.antchain.bridge.relayer.core.types.network.response.RelayerResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.ByteArrayCodec;

@WebService(targetNamespace = "http://ws.offchainapi.oracle.mychain.alipay.com/")
@SOAPBinding
@Slf4j
@NoArgsConstructor
public class WSRelayerServerAPImpl extends BaseRelayerServer implements WSRelayerServerAPI {

    private static final String RELAYER_HELLO_RAND_KEY_PREFIX = "RELAYER_HELLO_RAND_";

    public WSRelayerServerAPImpl(
            IRelayerNetworkManager relayerNetworkManager,
            IBCDNSManager bcdnsManager,
            IRelayerCredentialManager relayerCredentialManager,
            ReceiverService receiverService,
            RedissonClient redisson,
            String defaultNetworkId,
            boolean isDiscoveryServer
    ) {
        super(relayerNetworkManager, bcdnsManager, relayerCredentialManager, receiverService, redisson, defaultNetworkId, isDiscoveryServer);
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
                case HELLO_START:
                    return processHelloStart(HelloStartRequest.createFrom(request)).encode();
                case HELLO_COMPLETE:
                    return processHelloComplete(HelloCompleteRequest.createFrom(request)).encode();
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
                    "verify crosschain cert failed",
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
                () -> new RelayerBlockchainContent(
                        MapUtil.builder(blockchainInfo.getDomainCert().getDomain(), blockchainInfo).build(),
                        getBcdnsManager().getTrustRootCertChain(blockchainInfo.getDomainCert().getDomainSpace())
                ).encodeToJson(),
                getRelayerCredentialManager()
        );
    }

    private RelayerResponse processGetRelayerBlockchainContent(GetRelayerBlockchainContentRelayerRequest request) {
        if (!getRelayerCredentialManager().validateRelayerRequest(request)) {
            log.error("failed to validate request from relayer {}", request.calcRelayerNodeId());
            return RelayerResponse.createFailureResponse(
                    "verify crosschain cert failed",
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
                    "verify crosschain cert failed",
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

        log.info("handle am request from relayer {} success: ", request.calcRelayerNodeId());

        return RelayerResponse.createSuccessResponse(
                () -> null,
                getRelayerCredentialManager()
        );
    }

    private RelayerResponse processHandshakeRequest(HandshakeRelayerRequest request) {
        if (!getRelayerCredentialManager().validateRelayerRequest(request)) {
            log.error("failed to validate request from relayer {}", request.calcRelayerNodeId());
            return RelayerResponse.createFailureResponse(
                    "verify crosschain cert failed",
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

        log.info("handle handshake request from relayer {} success: ", request.calcRelayerNodeId());

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

    private RelayerResponse processHelloStart(HelloStartRequest request) {
        log.info("process hello start from {}", request.getRelayerNodeId());

        byte[] myRand = RandomUtil.randomBytes(32);
        setMyRelayerHelloRand(request.getRelayerNodeId(), myRand);
        return RelayerResponse.createSuccessResponse(
                new HelloStartRespPayload(
                        Base64.encode(getRelayerNetworkManager().getRelayerNodeInfo().getEncode()),
                        getBcdnsManager().getTrustRootCertChain(getRelayerCredentialManager().getLocalRelayerIssuerDomainSpace()),
                        getRelayerCredentialManager().getLocalNodeSigAlgo(),
                        getRelayerCredentialManager().signHelloRand(request.getRand()),
                        myRand
                ),
                getRelayerCredentialManager()
        );
    }

    private RelayerResponse processHelloComplete(HelloCompleteRequest request) {
        RelayerNodeInfo remoteNodeInfo = RelayerNodeInfo.decode(
                Base64.decode(request.getRemoteNodeInfo())
        );
        log.info("process hello complete from {}", remoteNodeInfo.getNodeId());

        if (
                getBcdnsManager().validateCrossChainCertificate(
                        remoteNodeInfo.getRelayerCrossChainCertificate(),
                        request.getDomainSpaceCertPath()
                )
        ) {
            throw new RuntimeException("failed to verify the relayer cert with cert path");
        }

        byte[] myRand = getMyRelayerHelloRand(remoteNodeInfo.getNodeId());
        if (ObjectUtil.isEmpty(myRand)) {
            throw new RuntimeException("none my rand found");
        }

        try {
            Signature verifier = Signature.getInstance(request.getSigAlgo());
            verifier.initVerify(
                    CrossChainCertificateUtil.getPublicKeyFromCrossChainCertificate(remoteNodeInfo.getRelayerCrossChainCertificate())
            );
            verifier.update(myRand);
            if (!verifier.verify(request.getSig())) {
                throw new RuntimeException("not pass");
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format("failed to verify sig for rand: ( rand: {}, sig: {} )",
                            HexUtil.encodeHexStr(myRand), HexUtil.encodeHexStr(request.getSig()), e)
            );
        }

        getRelayerNetworkManager().addRelayerNode(remoteNodeInfo);

        return RelayerResponse.createSuccessResponse(
                new HelloCompleteRespPayload(),
                getRelayerCredentialManager()
        );
    }

    private void setMyRelayerHelloRand(String relayerNodeId, byte[] myRand) {
        RBucket<byte[]> bucket = getRedisson().getBucket(
                RELAYER_HELLO_RAND_KEY_PREFIX + relayerNodeId,
                ByteArrayCodec.INSTANCE
        );
        bucket.set(myRand, Duration.of(3, ChronoUnit.MINUTES));
    }

    private byte[] getMyRelayerHelloRand(String relayerNodeId) {
        RBucket<byte[]> bucket = getRedisson().getBucket(
                RELAYER_HELLO_RAND_KEY_PREFIX + relayerNodeId,
                ByteArrayCodec.INSTANCE
        );
        return bucket.getAndDelete();
    }
}
