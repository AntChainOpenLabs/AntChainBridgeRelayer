package com.alipay.antchain.bridge.relayer.facade.admin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.ac.caict.bid.model.BIDDocumentOperation;
import cn.bif.common.JsonUtils;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest;
import com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse;
import com.alipay.antchain.bridge.relayer.facade.admin.glclient.GrpcClient;
import com.alipay.antchain.bridge.relayer.facade.admin.types.BlockchainId;
import com.alipay.antchain.bridge.relayer.facade.admin.types.CrossChainMsgACLItem;
import com.alipay.antchain.bridge.relayer.facade.admin.types.SysContractsInfo;
import com.alipay.antchain.bridge.relayer.facade.admin.utils.FacadeException;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class GRpcRelayerAdminClient implements IRelayerAdminClient {

    private static final String BCDNS = "bcdns";

    private static final String BLOCKCHAIN = "blockchain";

    private Map<String, BlockchainId> blockchainIdCache = new ConcurrentHashMap<>();

    public GRpcRelayerAdminClient(String address) {
        String[] arr = address.split(":");
        if (arr.length != 2) {
            throw new FacadeException("illegal address for admin rpc: " + address);
        }
        this.grpcClient = new GrpcClient(arr[0], Integer.parseInt(arr[1]));
    }

    private GrpcClient grpcClient;

    private String queryAPI(String name, String command, String... args) {
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
            return response.getResult();
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
        String result = queryAPI(BCDNS, "applyDomainNameCert", domainSpace, domainName, "1", JsonUtils.toJSONString(bidDocument));
        if (!StrUtil.startWith(result, "your receipt is")) {
            throw new FacadeException(
                    StrUtil.format("failed to query domain cert {}: ", domainName) + result
            );
        }
        return StrUtil.removePrefix(result, "your receipt is ");
    }

    @Override
    public AbstractCrossChainCertificate queryDomainNameCertFromBCDNS(String domainName, String domainSpace) {
        String result = queryAPI(BCDNS, "queryDomainNameCertFromBCDNS", domainName, domainSpace);
        if (!StrUtil.startWith(result, "the cert is : \n")) {
            throw new FacadeException(
                    StrUtil.format("failed to query domain cert {}: ", domainName) + result
            );
        }
        return CrossChainCertificateUtil.readCrossChainCertificateFromPem(
                StrUtil.removePrefix(result, "the cert is : \n").getBytes()
        );
    }

    @Override
    public void addBlockchainAnchor(String product, String blockchainId, String domain, String pluginServerId, byte[] config) {
        String result = queryAPI(
                BLOCKCHAIN, "addBlockchainAnchor",
                product,
                blockchainId,
                domain,
                pluginServerId,
                "", "",
                new String(config)
        );
        if (!StrUtil.equalsIgnoreCase("success", result)) {
            throw new FacadeException(
                    StrUtil.format("failed to add {}: ", domain) + result
            );
        }
    }

    @Override
    public void startBlockchainAnchor(String domain) {
        BlockchainId blockchainId = getBlockchainId(domain);
        if (ObjectUtil.isNull(blockchainId)) {
            throw new FacadeException(StrUtil.format("none blockchain found for {}", domain));
        }
        String result = queryAPI(
                BLOCKCHAIN, "startBlockchainAnchor",
                blockchainId.getProduct(),
                blockchainId.getBlockchainId()
        );
        if (!StrUtil.equalsIgnoreCase("success", result)) {
            throw new FacadeException(
                    StrUtil.format("failed to start {}: ", domain) + result
            );
        }
    }

    @Override
    public SysContractsInfo getBlockchainContracts(String domain) {
        BlockchainId blockchainId = getBlockchainId(domain);
        if (ObjectUtil.isNull(blockchainId)) {
            throw new FacadeException(StrUtil.format("none blockchain found for {}", domain));
        }
        String result = queryAPI(
                BLOCKCHAIN, "getBlockchainContracts",
                blockchainId.getProduct(),
                blockchainId.getBlockchainId()
        );
        if (!JSONUtil.isTypeJSONObject(result)) {
            throw new FacadeException(
                    StrUtil.format("failed to get blockchain contracts {}: ", domain) + result
            );
        };

        return JSON.parseObject(result, SysContractsInfo.class);
    }

    @Override
    public String getBlockchainHeights(String domain) {
        BlockchainId blockchainId = getBlockchainId(domain);
        if (ObjectUtil.isNull(blockchainId)) {
            throw new FacadeException(StrUtil.format("none blockchain found for {}", domain));
        }

        String result = queryAPI(
                BLOCKCHAIN, "getBlockchainHeights",
                blockchainId.getProduct(),
                blockchainId.getBlockchainId()
        );
        if (!JSONUtil.isTypeJSON(result)) {
            throw new FacadeException(
                    StrUtil.format("failed to get blockchain heights {}: ", domain) + result
            );
        }
        return result;
    }

    @Override
    public void stopBlockchainAnchor(String domain) {
        BlockchainId blockchainId = getBlockchainId(domain);
        if (ObjectUtil.isNull(blockchainId)) {
            throw new FacadeException(StrUtil.format("none blockchain found for {}", domain));
        }
        String result = queryAPI(
                BLOCKCHAIN, "stopBlockchainAnchor",
                blockchainId.getProduct(),
                blockchainId.getBlockchainId()
        );
        if (!StrUtil.equalsIgnoreCase("success", result)) {
            throw new FacadeException(
                    StrUtil.format("failed to stop {}: ", domain) + result
            );
        }
    }

    @Override
    public String addCrossChainMsgACL(String grantDomain, String grantIdentity, String ownerDomain, String ownerIdentity) {
        String result = queryAPI(
                BLOCKCHAIN, "addCrossChainMsgACL",
                grantDomain, grantIdentity, ownerDomain, ownerIdentity
        );
        if (result.startsWith("unexpected")) {
            throw new FacadeException("call addCrossChainMsgACL failed: " + result);
        }
        return result;
    }

    @Override
    public CrossChainMsgACLItem getCrossChainMsgACL(String bizId) {
        String result = queryAPI(
                BLOCKCHAIN, "getCrossChainMsgACL",
                bizId
        );
        if (!JSONUtil.isTypeJSON(result)) {
            throw new FacadeException(
                    StrUtil.format("failed to get ACL {}: ", bizId) + result
            );
        }
        return JSON.parseObject(result, CrossChainMsgACLItem.class);
    }

    @Override
    public void deleteCrossChainMsgACL(String bizId) {
        String result = queryAPI(
                BLOCKCHAIN, "deleteCrossChainMsgACL",
                bizId
        );
        if (!StrUtil.equalsIgnoreCase("success", result)) {
            throw new FacadeException(
                    StrUtil.format("failed to delete ACL {}: ", bizId) + result
            );
        }
    }

    @Override
    public boolean hasMatchedCrossChainACLItems(String grantDomain, String grantIdentity, String ownerDomain, String ownerIdentity) {
        String result = queryAPI(
                BLOCKCHAIN, "getCrossChainMsgACL",
                grantDomain, grantIdentity, ownerDomain, ownerIdentity
        );
        if (StrUtil.isEmpty(result) || StrUtil.startWith(result, "unexpected")) {
            throw new FacadeException(
                    StrUtil.format(
                            "failed to get matched ACL for ({} - {} : {} - {}): ",
                            grantDomain, grantIdentity, ownerDomain, ownerIdentity
                    ) + result
            );
        }
        return !StrUtil.equals("not found", result);
    }

    private BlockchainId getBlockchainId(String domain) {
        if (blockchainIdCache.containsKey(domain)) {
            return blockchainIdCache.get(domain);
        }
        BlockchainId blockchainId = JSON.parseObject(queryAPI(BLOCKCHAIN, "getBlockchainIdByDomain", domain), BlockchainId.class);
        if (ObjectUtil.isNotNull(blockchainId)) {
            blockchainIdCache.put(domain, blockchainId);
            return blockchainId;
        }
        return null;
    }
}
