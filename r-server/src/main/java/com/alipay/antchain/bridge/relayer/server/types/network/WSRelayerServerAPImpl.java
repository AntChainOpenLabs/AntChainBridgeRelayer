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

package com.alipay.antchain.bridge.relayer.server.types.network;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import cn.hutool.core.codec.Base64;
import lombok.extern.slf4j.Slf4j;

@WebService(targetNamespace = "http://ws.offchainapi.oracle.mychain.alipay.com/")
@SOAPBinding
@Slf4j
public class WSRelayerServerAPImpl extends BaseRelayerServer implements WSRelayerServerAPI {

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
     * @param relayerRequest
     * @return
     */
    protected byte[] doRequest(byte[] relayerRequest) {
//        RelayerResponse response = new RelayerResponse();
//
//        try {
//
//            RelayerRequest request = RelayerRequest.decode(relayerRequest);
//
//            if (request == null) {
//                response.setResponseCode(RelayerResponse.FAILED);
////                response.sign(ServerContext.getInstance().getLocalConfig().get(LocalConfig.RELAYER_NODE_PRI_KEY));
//                return response.encode();
//            }
//
//            RelayerRequestType requestType = RelayerRequestType.valueOf(request.getRequestType());
//
//            if (isAboutDomain(requestType) && !isDiscoveryServer) {
//                response.setResponseCode(RelayerResponse.FAILED);
//                response.setResponseMessage("node you connected isn't a relayer discovery server");
//                LOGGER.debug("wrong call from network");
//                return response.encode();
//            }
//
//            //*************************
//            // 以下接口无需权限验证
//            //*************************
//            if (requestType == RelayerRequestType.getRelayerNodeInfo) {
//
//                response.setResponseCode(RelayerResponse.SUCCESS);
//                response.setResponsePayload(
//                        Base64.getEncoder().encodeToString(relayerNetworkManager.getRelayerNodeInfo().getEncode()));
////                response.sign(ServerContext.getInstance().getLocalConfig().get(LocalConfig.RELAYER_NODE_PRI_KEY));
//
//                return response.encode();
//            } else if (requestType == RelayerRequestType.handshake) {
//                JSONObject object = JSON.parseObject(request.getRequestPayload());
//                RelayerNodeInfo nodeInfo = object.toJavaObject(RelayerNodeInfo.class);
////                String networkId = object.getString("networkId");
//                String networkId = OracleserviceRuntimeCache.getInstance().getSystemConfig(OracleserviceRuntimeCache.DEFAULT_RELAYER_NETWORKID_KEY);
//                LOGGER.info("[doRequest.handshake] handle relayer {} with {}", nodeInfo.getNodeId(), object.toJSONString());
//                try {
//                    doHandshake(nodeInfo, networkId);
//                } catch (Exception e) {
//                    response.setResponseCode(RelayerResponse.FAILED);
//                    response.setResponseMessage(e.getMessage());
//                    LOGGER.info("failed to handshake with relayer {}: ", nodeInfo.getNodeId(), e);
//                    return response.encode();
//                }
//
//                RelayerNodeInfo localNodeInfo = relayerNetworkManager.getRelayerNodeInfoWithBlockchainInfos();
//                JSONObject respOb = new JSONObject();
//                respOb.put("endpoints", JSONArray.toJSON(localNodeInfo.getEndpoints()));
//                respOb.put("domains", JSONArray.toJSON(localNodeInfo.getDomains()));
//                respOb.put("properties", JSON.toJSON(localNodeInfo.getProperties()));
//                respOb.put("nodePublicKey", localNodeInfo.getNodePublicKey());
//                respOb.put("nodeId", localNodeInfo.getNodeId());
//                respOb.put(
//                        "network_id",
//                        OracleserviceRuntimeCache.getInstance()
//                                .getSystemConfig(OracleserviceRuntimeCache.DEFAULT_RELAYER_NETWORKID_KEY)
//                );
//
//                response.setResponseCode(RelayerResponse.SUCCESS);
//                response.setResponsePayload(respOb.toJSONString());
//
//                LOGGER.info("successful to handshake with relayer {}", nodeInfo.getNodeId());
//
//                return response.encode();
//
//            } else if (requestType == RelayerRequestType.getRelayerForDomain) {
//                JSONObject object = JSON.parseObject(request.getRequestPayload());
//                String domain = object.getString("domain");
//                if (domain == null) {
//                    response.setResponseCode(RelayerResponse.FAILED);
//                    response.setResponseMessage("failed to parse your request payload, none domain found");
//                    LOGGER.info("failed to parse your request payload, none domain found");
//                    return response.encode();
//                }
//
//                RelayerNodeInfo nodeInfo = getRelayerForDomain(domain);
//                if (nodeInfo == null) {
//                    response.setResponseCode(RelayerResponse.FAILED);
//                    response.setResponseMessage("none RelayerNodeInfo found for domain");
//                    LOGGER.info("handle getRelayerForDomain: none RelayerNodeInfo found for domain {}", domain);
//                    return response.encode();
//                }
//
//                object = (JSONObject) JSON.toJSON(nodeInfo);
//
//                JSONObject respOb = new JSONObject();
//                respOb.put("endpoints", object.getJSONArray("endpoints"));
//                respOb.put("domains", object.getJSONArray("domains"));
//                respOb.put("nodePublicKey", object.getString("nodePublicKey"));
//                respOb.put("nodeId", object.getString("nodeId"));
////                respOb.put("network_id", OracleserviceRuntimeCache.getInstance()
////                        .getSystemConfig(OracleserviceRuntimeCache.DEFAULT_RELAYER_NETWORKID_KEY));
//
//                response.setResponseCode(RelayerResponse.SUCCESS);
//                response.setResponsePayload(respOb.toJSONString());
//                return response.encode();
//
//            } else if (requestType == RelayerRequestType.registerDomain ||
//                    requestType == RelayerRequestType.updateDomain || requestType == RelayerRequestType.deleteDomain) {
//                JSONObject object = JSON.parseObject(request.getRequestPayload());
//                RelayerNodeInfo nodeInfo = object.toJavaObject(RelayerNodeInfo.class);
//                if (nodeInfo.getDomains().size() == 0) {
//                    String msg = String.format("domain size %d is zero", nodeInfo.getDomains().size());
//                    response.setResponseCode(RelayerResponse.FAILED);
//                    response.setResponseMessage(msg);
//                    LOGGER.info("handle {}: {}", requestType.name(), msg);
//                    return response.encode();
//                }
//
//                try {
//                    switch (requestType) {
//                        case registerDomain:
//                            registerDomains(nodeInfo, OracleserviceRuntimeCache.getInstance().getSystemConfig(
//                                    OracleserviceRuntimeCache.DEFAULT_RELAYER_NETWORKID_KEY));
//                            break;
//                        case updateDomain:
//                            updateDomains(nodeInfo);
//                            break;
//                        case deleteDomain:
//                            deleteDomains(nodeInfo);
//                            break;
//                        default:
//                            throw new RuntimeException("none request type matched. ");
//                    }
//                } catch (Exception e) {
//                    response.setResponseCode(RelayerResponse.FAILED);
//                    response.setResponseMessage(e.getMessage());
//                    LOGGER.info("handle {}: exception happened: ", requestType.name(), e);
//                    return response.encode();
//                }
//
//                object = new JSONObject();
//                object.put("result", true);
//                response.setResponseCode(RelayerResponse.SUCCESS);
//                response.setResponsePayload(object.toJSONString());
//                return response.encode();
//            }
//
//            //*************************
//            // 以下接口需权限验证
//            //*************************
//
//            // node必须是本地已添加的才可以请求后续的接口
//            String nodeId = request.getNodeId();
//            RelayerNodeInfo nodeInfo = getRelayerNetworkManager().getRelayerNode(nodeId);
//
//            if (null == nodeInfo || !request.verify(nodeInfo.getRelayerCrossChainCertificate(), nodeInfo.getSigAlgo())) {
//                response.setResponseCode(RelayerResponse.FAILED);
//                response.sign(ServerContext.getInstance().getLocalConfig().get(LocalConfig.RELAYER_NODE_PRI_KEY));
//                return response.encode();
//            }
//
//            if (requestType == RelayerRequestType.getOracle) {
//
//                String supportedDomain = request.getRequestPayload();
//
//                List<OracleVO> oracles = getOracles(supportedDomain);
//
//                response.setResponseCode(RelayerResponse.SUCCESS);
//                response.setResponsePayload(new Gson().toJson(oracles));
//                response.sign(ServerContext.getInstance().getLocalConfig().get(LocalConfig.RELAYER_NODE_PRI_KEY));
//
//                return response.encode();
//            } else if (requestType == RelayerRequestType.getBlockchainInfo) {
//
//                String supportedDomain = request.getRequestPayload();
//
//                RelayerBlockchainInfo blockchainInfo = this.relayerNetworkManager.getRelayerBlockchainInfo(supportedDomain);
//
//                if (null == blockchainInfo) {
//                    response.setResponseCode(RelayerResponse.FAILED);
//                    response.sign(ServerContext.getInstance().getLocalConfig().get(LocalConfig.RELAYER_NODE_PRI_KEY));
//                    return response.encode();
//                }
//
//                response.setResponseCode(RelayerResponse.SUCCESS);
//                response.setResponsePayload(blockchainInfo.getEncode());
//                response.sign(ServerContext.getInstance().getLocalConfig().get(LocalConfig.RELAYER_NODE_PRI_KEY));
//
//                return response.encode();
//            } else if (requestType
//                == RelayerRequestType.udagRequest) {
//
//                JSONObject jsonObject = JSON.parseObject(request.getRequestPayload());
//                String requestId = jsonObject.getString("requestId");
//                String udagCmd = jsonObject.getString("udag");
//                String requestDomain = jsonObject.getString("requestDomain");
//
//                // 校验nodeid与domain对应关系
//                if (!relayerNetworkManager.getRelayerNode(nodeId).getDomains().contains(requestDomain)) {
//                    response.setResponseCode(RelayerResponse.FAILED);
//                    response.setResponseMessage(String.format("your domain %s is not found in my storage. ", requestDomain));
//                    response.sign(ServerContext.getInstance().getLocalConfig().get(LocalConfig.RELAYER_NODE_PRI_KEY));
//                    return response.encode();
//                }
//
//                String udagResponse = udagRequest(requestId, udagCmd, requestDomain);
//
//                if (null == udagResponse) {
//                    response.setResponseCode(RelayerResponse.FAILED);
//                    response.sign(ServerContext.getInstance().getLocalConfig().get(LocalConfig.RELAYER_NODE_PRI_KEY));
//                    return response.encode();
//                }
//
//                response.setResponseCode(RelayerResponse.SUCCESS);
//                response.setResponsePayload(udagResponse);
//                response.sign(ServerContext.getInstance().getLocalConfig().get(LocalConfig.RELAYER_NODE_PRI_KEY));
//
//                return response.encode();
//
//            } else if (requestType
//                == RelayerRequestType.amRequest) {
//
//                JSONObject jsonObject = JSON.parseObject(request.getRequestPayload());
//                String udagProof = jsonObject.getString("udagProof");
//                String authMsg = jsonObject.getString("authMsg");
//                String domainName = jsonObject.getString("domainName");
//                String ledgerInfo = jsonObject.getString("ledgerInfo");
//
//                try {
//                    amRequest(domainName, authMsg, udagProof, ledgerInfo);
//                } catch (RejectRequestException e) {
//                    LOGGER.error("reject amRequest from {} failed: ", domainName, e);
//                    response.setResponseCode(e.getErrorCode());
//                    response.setResponseMessage(e.getErrorString());
//                    response.sign(ServerContext.getInstance().getLocalConfig().get(LocalConfig.RELAYER_NODE_PRI_KEY));
//                    return response.encode();
//                } catch (Exception e) {
//                    LOGGER.error("handle amRequest from {} failed: ", domainName, e);
//                    response.setResponseCode(RelayerResponse.FAILED);
//                    response.sign(ServerContext.getInstance().getLocalConfig().get(LocalConfig.RELAYER_NODE_PRI_KEY));
//                    return response.encode();
//                }
//
//                response.setResponseCode(RelayerResponse.SUCCESS);
//                response.sign(ServerContext.getInstance().getLocalConfig().get(LocalConfig.RELAYER_NODE_PRI_KEY));
//                return response.encode();
//
//            } else {
//                response.setResponseCode(RelayerResponse.FAILED);
//                response.sign(ServerContext.getInstance().getLocalConfig().get(LocalConfig.RELAYER_NODE_PRI_KEY));
//                return response.encode();
//            }
//
//        } catch (Exception e) {
//
//            LOGGER.error("BaseEndpointServer doRequest fail", e);
//
//            response.setResponseCode(RelayerResponse.FAILED);
//            response.sign(ServerContext.getInstance().getLocalConfig().get(LocalConfig.RELAYER_NODE_PRI_KEY));
//            return response.encode();
//        }

        return null;
    }
}
