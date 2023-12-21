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

package com.alipay.antchain.bridge.relayer.cli.groovyshell.command;

import com.alipay.antchain.bridge.relayer.cli.command.ArgsConstraint;
import com.alipay.antchain.bridge.relayer.cli.groovyshell.GroovyScriptCommandNamespace;

public class BlockchainManagerCmdNamespace extends GroovyScriptCommandNamespace {

    /**
     * the name prompt to user
     *
     * @return
     */
    @Override
    public String name() {
        return "blockchain";
    }

    Object getBlockchainIdByDomain(@ArgsConstraint(name = "domain") String domain) {

        return queryAPI("getBlockchainIdByDomain", domain);
    }

    Object getBlockchain(@ArgsConstraint(name = "product") String product,
                         @ArgsConstraint(name = "blockchainId") String blockchainId) {

        return queryAPI("getBlockchain", product, blockchainId);
    }

    Object getBlockchainContracts(@ArgsConstraint(name = "product") String product,
                                  @ArgsConstraint(name = "blockchainId") String blockchainId) {

        return queryAPI("getBlockchainContracts", product, blockchainId);
    }

    Object addBlockchainAnchor(
            @ArgsConstraint(name = "product") String product,
            @ArgsConstraint(name = "blockchainId") String blockchainId,
            @ArgsConstraint(name = "domain") String domain,
            @ArgsConstraint(name = "pluginServerId") String pluginServerId,
            @ArgsConstraint(name = "alias") String alias,
            @ArgsConstraint(name = "desc") String desc,
            @ArgsConstraint(name = "heteroConfFilePath") String heteroConfFilePath
    ) {

        return queryAPI(
                "addBlockchainAnchor",
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
