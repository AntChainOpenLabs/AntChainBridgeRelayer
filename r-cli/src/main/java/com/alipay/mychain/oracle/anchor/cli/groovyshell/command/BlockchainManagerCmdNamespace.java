/**
 *  Alipay.com Inc.
 *  Copyright (c) 2004-2017 All Rights Reserved.
 */

package com.alipay.mychain.oracle.anchor.cli.groovyshell.command;

import com.alipay.mychain.oracle.anchor.cli.command.ArgsConstraint;
import com.alipay.mychain.oracle.anchor.cli.groovyshell.GroovyScriptCommandNamespace;

public class BlockchainManagerCmdNamespace extends GroovyScriptCommandNamespace {

    /**
     * the name prompt to user
     *
     * @return
     */
    @Override
    public String name() {
        return "blockchainManager";
    }

    Object getBlockchainIdByDomain(@ArgsConstraint(name = "domain") String domain) {

        return queryAPI("getBlockchainIdByDomain", domain);
    }

    Object getBlockchain(@ArgsConstraint(name = "product") String product,
                         @ArgsConstraint(name = "blockchainId") String blockchainId) {

        return queryAPI("getBlockchain", product, blockchainId);
    }

    Object addHeteroBlockchainAnchor(
            @ArgsConstraint(name = "product") String product,
            @ArgsConstraint(name = "blockchainId") String blockchainId,
            @ArgsConstraint(name = "domain") String domain,
            @ArgsConstraint(name = "pluginServerId") String pluginServerId,
            @ArgsConstraint(name = "alias") String alias,
            @ArgsConstraint(name = "desc") String desc,
            @ArgsConstraint(name = "heteroConfFilePath") String heteroConfFilePath
    ) {

        return queryAPI(
                "addHeteroBlockchainAnchor",
                product, blockchainId, domain, pluginServerId, alias, desc, heteroConfFilePath
        );
    }

    Object deployBBCContractsAsync(
            @ArgsConstraint(name = "product") String product,
            @ArgsConstraint(name = "blockchainId") String blockchainId
    ) {

        return queryAPI("deployBBCContractsAsync", product, blockchainId
        );
    }

    Object updateBlockchainAnchor(@ArgsConstraint(name = "product") String product,
                                  @ArgsConstraint(name = "blockchainId") String blockchainId,
                                  @ArgsConstraint(name = "alias") String alias,
                                  @ArgsConstraint(name = "desc") String desc,
                                  @ArgsConstraint(name = "clientConfig") String clientConfig) {

        return queryAPI("updateBlockchainAnchor", product, blockchainId, alias, desc, clientConfig);
    }

    Object updateBlockchainProperty(@ArgsConstraint(name = "product") String product,
                                    @ArgsConstraint(name = "blockchainId") String blockchainId,
                                    @ArgsConstraint(name = "confKey") String confKey,
                                    @ArgsConstraint(name = "confValue") String confValue) {

        return queryAPI("updateBlockchainProperty", product, blockchainId, confKey, confValue);
    }

    Object startBlockchainAnchor(@ArgsConstraint(name = "product") String product,
                                 @ArgsConstraint(name = "blockchainId") String blockchainId) {

        return queryAPI("startBlockchainAnchor", product, blockchainId);
    }

    Object stopBlockchainAnchor(@ArgsConstraint(name = "product") String product,
                                @ArgsConstraint(name = "blockchainId") String blockchainId) {

        return queryAPI("stopBlockchainAnchor", product, blockchainId);
    }

    Object setTxPendingLimit(@ArgsConstraint(name = "product") String product,
                             @ArgsConstraint(name = "blockchainID") String blockchainID,
                             @ArgsConstraint(name = "txPendingLimit") Integer txPendingLimit) {

        return queryAPI("setTxPendingLimit", product, blockchainID, txPendingLimit);
    }

    Object querySDPMsgSeq(@ArgsConstraint(name = "receiverProduct") String receiverProduct,
                          @ArgsConstraint(name = "receiverBlockchainId") String receiverBlockchainId,
                          @ArgsConstraint(name = "senderDomain") String senderDomain,
                          @ArgsConstraint(name = "from") String from,
                          @ArgsConstraint(name = "to") String to) {

        return queryAPI("querySDPMsgSeq", receiverProduct, receiverBlockchainId, senderDomain, from, to);
    }
}
