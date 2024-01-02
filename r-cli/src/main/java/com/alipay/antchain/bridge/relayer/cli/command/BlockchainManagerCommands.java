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

package com.alipay.antchain.bridge.relayer.cli.command;

import javax.annotation.Resource;

import com.alipay.antchain.bridge.relayer.cli.glclient.GrpcClient;
import lombok.Getter;
import org.springframework.shell.standard.*;

@Getter
@ShellCommandGroup(value = "Commands about Blockchain")
@ShellComponent
public class BlockchainManagerCommands extends BaseCommands {

    @Resource
    private GrpcClient grpcClient;

    @Override
    public String name() {
        return "blockchain";
    }

    @ShellMethod(value = "Get the local blockchain ID for specified domain")
    Object getBlockchainIdByDomain(@ShellOption(help = "the blockchain domain name") String domain) {
        return queryAPI("getBlockchainIdByDomain", domain);
    }

    @ShellMethod(value = "Get the local blockchain data for specified domain")
    Object getBlockchain(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId
    ) {
        return queryAPI("getBlockchain", product, blockchainId);
    }

    @ShellMethod(value = "Get the local blockchain BBC contracts information")
    Object getBlockchainContracts(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId
    ) {
        return queryAPI("getBlockchainContracts", product, blockchainId);
    }

    @ShellMethod(value = "Get the local blockchain heights where anchor service runs on")
    Object getBlockchainHeights(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId
    ) {
        return queryAPI("getBlockchainHeights", product, blockchainId);
    }

    @ShellMethod(value = "Add a specified blockchain configuration to start the anchor service")
    Object addBlockchainAnchor(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId,
            @ShellOption(help = "Domain for blockchain") String domain,
            @ShellOption(help = "Plugin server ID the blockchain plugin running on") String pluginServerId,
            @ShellOption(help = "Alias for blockchain", defaultValue = "") String alias,
            @ShellOption(help = "Description for blockchain", defaultValue = "") String desc,
            @ShellOption(valueProvider = FileValueProvider.class, help = "The file path to configuration file required by the `BBCService` in blockchain plugin") String confFile
    ) {
        return queryAPI(
                "addBlockchainAnchor",
                product, blockchainId, domain, pluginServerId, alias, desc, confFile
        );
    }

    @ShellMethod(value = "Mark the blockchain to deploy BBC contracts async")
    Object deployBBCContractsAsync(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId
    ) {
        return queryAPI("deployBBCContractsAsync", product, blockchainId);
    }

    @ShellMethod(value = "Update the whole blockchain anchor configuration")
    Object updateBlockchainAnchor(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId,
            @ShellOption(help = "Alias for blockchain", defaultValue = "") String alias,
            @ShellOption(help = "Description for blockchain", defaultValue = "") String desc,
            @ShellOption(help = "The configuration value in string") String clientConfig
    ) {
        return queryAPI("updateBlockchainAnchor", product, blockchainId, alias, desc, clientConfig);
    }

    @ShellMethod(value = "Update the specified key-value in blockchain anchor configuration")
    Object updateBlockchainProperty(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId,
            @ShellOption(help = "configuration key") String confKey,
            @ShellOption(help = "configuration value") String confValue
    ) {
        return queryAPI("updateBlockchainProperty", product, blockchainId, confKey, confValue);
    }

    @ShellMethod(value = "Start the blockchain anchor service in Relayer")
    Object startBlockchainAnchor(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId
    ) {
        return queryAPI("startBlockchainAnchor", product, blockchainId);
    }

    @ShellMethod(value = "Stop the blockchain anchor service in Relayer")
    Object stopBlockchainAnchor(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId
    ) {

        return queryAPI("stopBlockchainAnchor", product, blockchainId);
    }

    @ShellMethod(value = "Set max limit for the transaction pending but not committed on blockchain receiving cross-chain messages")
    Object setTxPendingLimit(
            @ShellOption(help = "Product type for blockchain, e.g. mychain010") String product,
            @ShellOption(help = "Local blockchain ID") String blockchainId,
            @ShellOption(help = "The limit value for transaction pending") Integer txPendingLimit
    ) {
        return queryAPI("setTxPendingLimit", product, blockchainId, Integer.toString(txPendingLimit));
    }

    @ShellMethod(value = "Query SDP message sequence number on the specified direction")
    Object querySDPMsgSeq(
            @ShellOption(help = "Product type for blockchain receiving cross-chain messages") String receiverProduct,
            @ShellOption(help = "Local blockchain ID for blockchain receiving cross-chain messages") String receiverBlockchainId,
            @ShellOption(help = "Blockchain domain for blockchain sending cross-chain messages") String senderDomain,
            @ShellOption(help = "The sender contract identity, e.g. 0x1f9840a85d5aF5bf1D1762F925BDADdC4201F984") String sender,
            @ShellOption(help = "The receiver contract identity, e.g. 0x1f9840a85d5aF5bf1D1762F925BDADdC4201F984") String receiver
    ) {
        return queryAPI("querySDPMsgSeq", receiverProduct, receiverBlockchainId, senderDomain, sender, receiver);
    }
}
