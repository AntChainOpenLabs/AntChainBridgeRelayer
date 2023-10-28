package com.alipay.antchain.bridge.relayer.core.types.pluginserver;

import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.pluginserver.service.*;
import com.alipay.antchain.bridge.relayer.core.utils.PluginServerUtils;
import com.google.protobuf.ByteString;

public class GRpcBBCServiceClient implements IBBCServiceClient {

    private String psId;

    private final String product;

    private final String domain;

    private CrossChainServiceGrpc.CrossChainServiceBlockingStub blockingStub;

    private AbstractBBCContext bbcContext;

    public GRpcBBCServiceClient(String psId, String product, String domain, CrossChainServiceGrpc.CrossChainServiceBlockingStub blockingStub) {
        this.psId = psId;
        this.product = product;
        this.domain = domain;
        this.blockingStub = blockingStub;
    }

    @Override
    public String getProduct() {
        return this.product;
    }

    @Override
    public String getDomain() {
        return this.domain;
    }

    @Override
    public void startup(AbstractBBCContext abstractBBCContext) {
        Response response = this.blockingStub.bbcCall(
                CallBBCRequest.newBuilder()
                        .setProduct(this.getProduct())
                        .setDomain(this.getDomain())
                        .setStartUpReq(
                                StartUpRequest.newBuilder()
                                        .setRawContext(ByteString.copyFrom(JSON.toJSONBytes(abstractBBCContext)))
                        ).build()
        );
        if (response.getCode() != 0) {
            throw new RuntimeException(String.format("[GRpcBBCServiceClient (domain: %s, product: %s)] startup request failed for plugin server %s: %s",
                    this.domain, this.product, this.psId, response.getErrorMsg()));
        }

        this.bbcContext = abstractBBCContext;
    }

    @Override
    public void shutdown() {
        Response response = this.blockingStub.bbcCall(
                CallBBCRequest.newBuilder()
                        .setProduct(this.getProduct())
                        .setDomain(this.getDomain())
                        .setShutdownReq(ShutdownRequest.getDefaultInstance())
                        .build()
        );
        if (response.getCode() != 0) {
            throw new RuntimeException(String.format("[GRpcBBCServiceClient (domain: %s, product: %s)] shutdown request failed for plugin server %s: %s",
                    this.domain, this.product, this.psId, response.getErrorMsg()));
        }
    }

    @Override
    public AbstractBBCContext getContext() {
        Response response = this.blockingStub.bbcCall(
                CallBBCRequest.newBuilder()
                        .setProduct(this.getProduct())
                        .setDomain(this.getDomain())
                        .setGetContextReq(GetContextRequest.getDefaultInstance())
                        .build()
        );
        if (response.getCode() != 0) {
            throw new RuntimeException(String.format("[GRpcBBCServiceClient (domain: %s, product: %s)] getContext request failed for plugin server %s: %s",
                    this.domain, this.product, this.psId, response.getErrorMsg()));
        }
        AbstractBBCContext bbcContext = new DefaultBBCContext();
        bbcContext.decodeFromBytes(response.getBbcResp().getGetContextResp().getRawContext().toByteArray());
        this.bbcContext = bbcContext;

        return bbcContext;
    }

    @Override
    public CrossChainMessageReceipt readCrossChainMessageReceipt(String txhash) {
        Response response = this.blockingStub.bbcCall(
                CallBBCRequest.newBuilder()
                        .setProduct(this.getProduct())
                        .setDomain(this.getDomain())
                        .setReadCrossChainMessageReceiptReq(
                                ReadCrossChainMessageReceiptRequest.newBuilder().setTxhash(txhash)
                        ).build()
        );
        if (response.getCode() != 0) {
            throw new RuntimeException(
                    String.format("[GRpcBBCServiceClient (domain: %s, product: %s)] isCrossChainMessageConfirmed request failed for plugin server %s: %s",
                            this.domain, this.product, this.psId, response.getErrorMsg())
            );
        }
        return PluginServerUtils.convertFromGRpcCrossChainMessageReceipt(
                response.getBbcResp().getReadCrossChainMessageReceiptResp().getReceipt()
        );
    }

    @Override
    public List<CrossChainMessage> readCrossChainMessagesByHeight(long height) {
        Response response = this.blockingStub.bbcCall(
                CallBBCRequest.newBuilder()
                        .setProduct(this.getProduct())
                        .setDomain(this.getDomain())
                        .setReadCrossChainMessagesByHeightReq(
                                ReadCrossChainMessagesByHeightRequest.newBuilder()
                                        .setHeight(height)
                        ).build()
        );
        if (response.getCode() != 0) {
            try {
                handleErrorCode(response);
            } catch (Exception e) {
                throw new RuntimeException(
                        String.format("[GRpcBBCServiceClient (domain: %s, product: %s)] readCrossChainMessagesByHeight request failed :",
                                this.domain, this.product), e
                );
            }
        }
        return response.getBbcResp().getReadCrossChainMessagesByHeightResp().getMessageListList().stream()
                .map(PluginServerUtils::convertFromGRpcCrossChainMessage)
                .collect(Collectors.toList());
    }

    @Override
    public long querySDPMessageSeq(String senderDomain, String fromAddress, String receiverDomain, String toAddress) {
        Response response = this.blockingStub.bbcCall(
                CallBBCRequest.newBuilder()
                        .setProduct(this.getProduct())
                        .setDomain(this.getDomain())
                        .setQuerySDPMessageSeqReq(
                                QuerySDPMessageSeqRequest.newBuilder()
                                        .setSenderDomain(senderDomain)
                                        .setFromAddress(fromAddress)
                                        .setReceiverDomain(receiverDomain)
                                        .setToAddress(toAddress)
                        ).build()
        );
        if (response.getCode() != 0) {
            throw new RuntimeException(
                    String.format("[GRpcBBCServiceClient (domain: %s, product: %s)] querySDPMessageSeq request failed for plugin server %s: %s",
                            this.domain, this.product, this.psId, response.getErrorMsg())
            );
        }
        return response.getBbcResp().getQuerySDPMsgSeqResp().getSequence();
    }

    @Override
    public void setupAuthMessageContract() {
        Response response = this.blockingStub.bbcCall(
                CallBBCRequest.newBuilder()
                        .setProduct(this.getProduct())
                        .setDomain(this.getDomain())
                        .setSetupAuthMessageContractReq(SetupAuthMessageContractRequest.getDefaultInstance())
                        .build()
        );
        if (response.getCode() != 0) {
            throw new RuntimeException(
                    String.format("[GRpcBBCServiceClient (domain: %s, product: %s)] setupAuthMessageContract request failed for plugin server %s: %s",
                            this.domain, this.product, this.psId, response.getErrorMsg())
            );
        }
    }

    @Override
    public void setupSDPMessageContract() {
        Response response = this.blockingStub.bbcCall(
                CallBBCRequest.newBuilder()
                        .setProduct(this.getProduct())
                        .setDomain(this.getDomain())
                        .setSetupSDPMessageContractReq(SetupSDPMessageContractRequest.getDefaultInstance())
                        .build()
        );
        if (response.getCode() != 0) {
            throw new RuntimeException(
                    String.format("[GRpcBBCServiceClient (domain: %s, product: %s)] setupSDPMessageContract request failed for plugin server %s: %s",
                            this.domain, this.product, this.psId, response.getErrorMsg())
            );
        }
    }

    @Override
    public void setProtocol(String protocolAddress, String protocolType) {
        Response response = this.blockingStub.bbcCall(
                CallBBCRequest.newBuilder()
                        .setProduct(this.getProduct())
                        .setDomain(this.getDomain())
                        .setSetProtocolReq(
                                SetProtocolRequest.newBuilder()
                                        .setProtocolType(protocolType)
                                        .setProtocolAddress(protocolAddress)
                        ).build()
        );
        if (response.getCode() != 0) {
            throw new RuntimeException(
                    String.format("[GRpcBBCServiceClient (domain: %s, product: %s)] setProtocol request failed for plugin server %s: %s",
                            this.domain, this.product, this.psId, response.getErrorMsg())
            );
        }
    }

    @Override
    public CrossChainMessageReceipt relayAuthMessage(byte[] rawMessage) {
        Response response = this.blockingStub.bbcCall(
                CallBBCRequest.newBuilder()
                        .setProduct(this.getProduct())
                        .setDomain(this.getDomain())
                        .setRelayAuthMessageReq(
                                RelayAuthMessageRequest.newBuilder()
                                        .setRawMessage(ByteString.copyFrom(rawMessage))
                        ).build()
        );
        if (response.getCode() != 0) {
            throw new RuntimeException(
                    String.format("[GRpcBBCServiceClient (domain: %s, product: %s)] relayAuthMessage request failed for plugin server %s: %s",
                            this.domain, this.product, this.psId, response.getErrorMsg())
            );
        }

        return PluginServerUtils.convertFromGRpcCrossChainMessageReceipt(response.getBbcResp().getRelayAuthMessageResponse().getReceipt());
    }

    @Override
    public void setAmContract(String contractAddress) {
        Response response = this.blockingStub.bbcCall(
                CallBBCRequest.newBuilder()
                        .setProduct(this.getProduct())
                        .setDomain(this.getDomain())
                        .setSetAmContractReq(SetAmContractRequest.newBuilder().setContractAddress(contractAddress))
                        .build()
        );
        if (response.getCode() != 0) {
            throw new RuntimeException(
                    String.format("[GRpcBBCServiceClient (domain: %s, product: %s)] setAmContract request failed for plugin server %s: %s",
                            this.domain, this.product, this.psId, response.getErrorMsg())
            );
        }
    }

    @Override
    public void setLocalDomain(String domain) {
        Response response = this.blockingStub.bbcCall(
                CallBBCRequest.newBuilder()
                        .setProduct(this.getProduct())
                        .setDomain(this.getDomain())
                        .setSetLocalDomainReq(SetLocalDomainRequest.newBuilder().setDomain(domain))
                        .build()
        );
        if (response.getCode() != 0) {
            throw new RuntimeException(
                    String.format("[GRpcBBCServiceClient (domain: %s, product: %s)] setLocalDomain request failed for plugin server %s: %s",
                            this.domain, this.product, this.psId, response.getErrorMsg())
            );
        }
    }

    @Override
    public Long queryLatestHeight() {
        Response response = this.blockingStub.bbcCall(
                CallBBCRequest.newBuilder()
                        .setProduct(this.getProduct())
                        .setDomain(this.getDomain())
                        .setQueryLatestHeightReq(
                                QueryLatestHeightRequest.getDefaultInstance()
                        ).build()
        );
        if (response.getCode() != 0) {
            try {
                handleErrorCode(response);
            } catch (Exception e) {
                throw new RuntimeException(
                        String.format("[GRpcBBCServiceClient (domain: %s, product: %s)] queryLatestHeight request failed :",
                                this.domain, this.product), e
                );
            }
        }
        return response.getBbcResp().getQueryLatestHeightResponse().getHeight();
    }

    @Override
    public boolean hasTPBTA(String s) {
        return false;
    }

    @Override
    public byte[] getTPBTA(String s) {
        return new byte[0];
    }

    @Override
    public boolean ifBlockchainProductSupported(String s) {
        return false;
    }

    @Override
    public void addTPBTA(String s, byte[] bytes) {

    }

    @Override
    public void approveProtocol(String s) {

    }

    @Override
    public void disapproveProtocol(String s) {

    }

    @Override
    public void setOwner(String s) {

    }

    private void handleErrorCode(Response response) {
        if (response.getCode() == 217) {
            response = this.blockingStub.bbcCall(
                    CallBBCRequest.newBuilder()
                            .setProduct(this.getProduct())
                            .setDomain(this.getDomain())
                            .setStartUpReq(
                                    StartUpRequest.newBuilder()
                                            .setRawContext(ByteString.copyFrom(JSON.toJSONBytes(this.bbcContext)))
                            ).build()
            );
            if (response.getCode() != 0) {
                throw new RuntimeException(String.format("restart request failed for plugin server %s: %s",
                        this.psId, response.getErrorMsg()));
            }
            return;
        }
        throw new RuntimeException(
                String.format("error code %d for plugin server %s: %s", response.getCode(), this.psId, response.getErrorMsg())
        );
    }
}
