package com.alipay.antchain.bridge.relayer.facade.admin;

import cn.ac.caict.bid.model.BIDDocumentOperation;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest;
import com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse;
import com.alipay.antchain.bridge.relayer.facade.admin.glclient.GrpcClient;
import com.alipay.antchain.bridge.relayer.facade.admin.utils.FacadeException;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class GRpcRelayerAdminClient implements IRelayerAdminClient {

    public GRpcRelayerAdminClient(String address) {
        String[] arr = address.split(":");
        if (arr.length != 2) {
            throw new FacadeException("illegal address for admin rpc: " + address);
        }
        this.grpcClient = new GrpcClient(arr[0], Integer.parseInt(arr[1]));
    }

    private GrpcClient grpcClient;

    private void queryAPI(String name, String command, String... args) {
        AdminRequest.Builder reqBuilder = AdminRequest.newBuilder();
        reqBuilder.setCommandNamespace(name);
        reqBuilder.setCommand(command);
        if (null != args) {
            reqBuilder.addAllArgs(Lists.newArrayList(args));
        }
        try {
            AdminResponse response = grpcClient.adminRequest(reqBuilder.build());
            if (!response.getSuccess()) {
                throw new FacadeException(StrUtil.format(
                        "failed to call {}:{} with args {} : {}",
                        name, command, ArrayUtil.join(args, StrPool.COMMA), response.getErrorMsg()
                ));
            }
           log.info("successful to call call {}:{} with args {} : {}", name, command, ArrayUtil.join(args, StrPool.COMMA), response.getResult());
        } catch (FacadeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format("unexpected exceptions when calling {}:{} with args {}", name, command, ArrayUtil.join(args, StrPool.COMMA)),
                    e
            );
        }
    }

    @Override
    public String applyDomainNameCert(String domainSpace, String domainName, BIDDocumentOperation bidDocument) {
        return "";
    }

    @Override
    public AbstractCrossChainCertificate queryDomainNameCertFromBCDNS(String domainName, String domainSpace) {
        return null;
    }

    @Override
    public void addBlockchainAnchor(String domain, byte[] config) {

    }

    @Override
    public void startBlockchainAnchor(String domain) {

    }

    @Override
    public String getBlockchainContracts(String domain) {
        return "";
    }

    @Override
    public String getBlockchainHeights(String domain) {
        return "";
    }

    @Override
    public void stopBlockchainAnchor(String domain) {

    }

    @Override
    public String addCrossChainMsgACL(String grantDomain, String grantIdentity, String ownerDomain, String ownerIdentity) {
        return "";
    }

    @Override
    public String getCrossChainMsgACL(String bizId) {
        return "";
    }

    @Override
    public void deleteCrossChainMsgACL(String bizId) {

    }

    @Override
    public boolean hasMatchedCrossChainACLItems(String grantDomain, String grantIdentity, String ownerDomain, String ownerIdentity) {
        return false;
    }
}
