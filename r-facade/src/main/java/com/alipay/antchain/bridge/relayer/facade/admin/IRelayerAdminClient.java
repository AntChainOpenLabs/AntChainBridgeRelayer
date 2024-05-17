package com.alipay.antchain.bridge.relayer.facade.admin;

import java.util.List;

import cn.ac.caict.bid.model.BIDDocumentOperation;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.relayer.facade.admin.types.CrossChainMsgACLItem;
import com.alipay.antchain.bridge.relayer.facade.admin.types.SysContractsInfo;

public interface IRelayerAdminClient {

    String applyDomainNameCert(String domainSpace, String domainName, BIDDocumentOperation bidDocument);

    AbstractCrossChainCertificate queryDomainNameCertFromBCDNS(String domainName, String domainSpace);

    void addBlockchainAnchor(String product, String blockchainId, String domain, String pluginServerId, byte[] config);

    void startBlockchainAnchor(String domain);

    SysContractsInfo getBlockchainContracts(String domain);

    String getBlockchainHeights(String domain);

    void stopBlockchainAnchor(String domain);

    String addCrossChainMsgACL(String grantDomain, String grantIdentity, String ownerDomain, String ownerIdentity);

    CrossChainMsgACLItem getCrossChainMsgACL(String bizId);

    void deleteCrossChainMsgACL(String bizId);

    boolean hasMatchedCrossChainACLItems(String grantDomain, String grantIdentity, String ownerDomain, String ownerIdentity);

    List<String> getMatchedCrossChainACLBizIds(String grantDomain, String grantIdentity, String ownerDomain, String ownerIdentity);
}
