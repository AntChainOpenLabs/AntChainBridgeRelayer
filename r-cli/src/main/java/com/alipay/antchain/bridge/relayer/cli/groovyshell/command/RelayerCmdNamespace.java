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

public class RelayerCmdNamespace extends GroovyScriptCommandNamespace {

    /**
     * the name prompt to user
     *
     * @return
     */
    @Override
    public String name() {
        return "relayer";
    }

    Object setLocalEndpoints(@ArgsConstraint(name = "endpoints") String endpoints) {
        return queryAPI("setLocalEndpoints", endpoints);
    }

    Object getLocalEndpoints() {
        return queryAPI("getLocalEndpoints");
    }

    Object getLocalRelayerId() {
        return queryAPI("getLocalRelayerId");
    }

    Object getLocalRelayerCrossChainCertificate() {
        return queryAPI("getLocalRelayerCrossChainCertificate");
    }

    Object getLocalDomainRouter(@ArgsConstraint(name = "domain") String domain) {
        return queryAPI("getLocalDomainRouter", domain);
    }

    Object getCrossChainChannel(@ArgsConstraint(name = "localDomain") String localDomain,
                                @ArgsConstraint(name = "remoteDomain") String remoteDomain) {
        return queryAPI("getCrossChainChannel", localDomain, remoteDomain);
    }

    Object getRemoteRelayerInfo(@ArgsConstraint(name = "nodeId") String nodeId) {
        return queryAPI("getRemoteRelayerInfo", nodeId);
    }
}
