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

@ShellCommandGroup(value = "Service Control")
@ShellComponent
@Getter
public class ServiceManagerCommands extends BaseCommands {

    @Override
    public String name() {
        return "service";
    }

    @Resource
    private GrpcClient grpcClient;

    @ShellMethod(value = "Add new item to cross-chain message access control list")
    public Object addCrossChainMsgACL(
            @ShellOption(help = "Blockchain domain applying permission to send msg") String grantDomain,
            @ShellOption(help = "Blockchain account identity applying permission to send msg") String grantIdentity,
            @ShellOption(help = "Blockchain domain receiving msg") String ownerDomain,
            @ShellOption(help = "Blockchain domain account identity receiving msg") String ownerIdentity
    ) {
        return queryAPI("addCrossChainMsgACL", grantDomain, grantIdentity, ownerDomain, ownerIdentity);
    }

    @ShellMethod(value = "Get a item from cross-chain message access control list")
    Object getCrossChainMsgACL(@ShellOption(help = "The ID for cross-chain msg ACL item") String bizId) {
        return queryAPI("getCrossChainMsgACL", bizId);
    }

    Object getMatchedCrossChainACLItems(String grantDomain, String grantIdentity, String ownerDomain, String ownerIdentity) {
        return queryAPI("getCrossChainMsgACL", grantDomain, grantIdentity, ownerDomain, ownerIdentity);
    }

    Object deleteCrossChainMsgACL(String bizId) {
        return queryAPI("deleteCrossChainMsgACL", bizId);
    }


    Object registerPluginServer(String pluginServerId, String address, String pluginServerCAPath) {
        return queryAPI("registerPluginServer", pluginServerId, address, pluginServerCAPath);
    }

    Object stopPluginServer(String pluginServerId) {
        return queryAPI("stopPluginServer", pluginServerId);
    }

    Object startPluginServer(String pluginServerId) {
        return queryAPI("startPluginServer", pluginServerId);
    }
}
