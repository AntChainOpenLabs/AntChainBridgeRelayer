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

public class ServiceManagerCmdNamespace extends GroovyScriptCommandNamespace {

    /**
     * the name prompt to user
     *
     * @return
     */
    @Override
    public String name() {
        return "service";
    }


    Object addCrossChainMsgACL(@ArgsConstraint(name = "grantDomain") String grantDomain,
                               @ArgsConstraint(name = "grantIdentity") String grantIdentity,
                               @ArgsConstraint(name = "ownerDomain") String ownerDomain,
                               @ArgsConstraint(name = "ownerIdentity") String ownerIdentity) {

        return queryAPI("addCrossChainMsgACL", grantDomain, grantIdentity, ownerDomain, ownerIdentity);
    }

    Object getCrossChainMsgACL(@ArgsConstraint(name = "bizId") String bizId) {

        return queryAPI("getCrossChainMsgACL", bizId);
    }

    Object getMatchedCrossChainACLItems(@ArgsConstraint(name = "grantDomain") String grantDomain,
                                        @ArgsConstraint(name = "grantIdentity") String grantIdentity,
                                        @ArgsConstraint(name = "ownerDomain") String ownerDomain,
                                        @ArgsConstraint(name = "ownerIdentity") String ownerIdentity) {

        return queryAPI("getCrossChainMsgACL", grantDomain, grantIdentity, ownerDomain, ownerIdentity);
    }

    Object deleteCrossChainMsgACL(@ArgsConstraint(name = "bizId") String bizId) {

        return queryAPI("deleteCrossChainMsgACL", bizId);
    }


       Object registerPluginServer(
            @ArgsConstraint(name = "pluginServerId") String pluginServerId,
            @ArgsConstraint(name = "address") String address,
            @ArgsConstraint(name = "pluginServerCAPath") String pluginServerCAPath
    ) {
        return queryAPI("registerPluginServer", pluginServerId, address, pluginServerCAPath);
    }

    Object stopPluginServer(
            @ArgsConstraint(name = "pluginServerId") String pluginServerId
    ) {
        return queryAPI("stopPluginServer", pluginServerId);
    }

    Object startPluginServer(
            @ArgsConstraint(name = "pluginServerId") String pluginServerId
    ) {
        return queryAPI("startPluginServer", pluginServerId);
    }
}
