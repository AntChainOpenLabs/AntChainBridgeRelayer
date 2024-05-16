package com.alipay.antchain.bridge.relayer.facade.admin;

import cn.ac.caict.bid.model.BIDDocumentOperation;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;

public interface IRelayerAdminClient {

    String applyDomainNameCert(String domainSpace, String domainName, BIDDocumentOperation bidDocument);

    AbstractCrossChainCertificate queryDomainNameCertFromBCDNS(String domainName, String domainSpace);

    void addBlockchainAnchor(String domain, byte[] config);

    void startBlockchainAnchor(String domain);

    String getBlockchainContracts(String domain);

    String getBlockchainHeights(String domain);

    void stopBlockchainAnchor(String domain);

    String addCrossChainMsgACL(String grantDomain, String grantIdentity, String ownerDomain, String ownerIdentity);

    String getCrossChainMsgACL(String bizId);

    void deleteCrossChainMsgACL(String bizId);

    boolean hasMatchedCrossChainACLItems(String grantDomain, String grantIdentity, String ownerDomain, String ownerIdentity);
}
