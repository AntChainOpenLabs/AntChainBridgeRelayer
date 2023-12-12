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

public class BCDNSManagerCmdNamespace extends GroovyScriptCommandNamespace {

    @Override
    public String name() {
        return "bcdns";
    }

    Object registerBCDNSService(
            @ArgsConstraint(name = "domainSpace") String domainSpace,
            @ArgsConstraint(name = "bcdnsType") String bcdnsType,
            @ArgsConstraint(name = "propFile") String propFile
    ) {

        return queryAPI("registerBCDNSService", domainSpace, bcdnsType, propFile);
    }

    Object registerBCDNSServiceWithCert(
            @ArgsConstraint(name = "domainSpace") String domainSpace,
            @ArgsConstraint(name = "bcdnsType") String bcdnsType,
            @ArgsConstraint(name = "propFile") String propFile,
            @ArgsConstraint(name = "bcdnsCertPath") String bcdnsCertPath
    ) {

        return queryAPI("registerBCDNSService", domainSpace, bcdnsType, propFile, bcdnsCertPath);
    }

    Object stopBCDNSService(@ArgsConstraint(name = "domainSpace") String domainSpace) {

        return queryAPI("stopBCDNSService", domainSpace);
    }

    Object restartBCDNSService(@ArgsConstraint(name = "domainSpace") String domainSpace) {

        return queryAPI("restartBCDNSService", domainSpace);
    }

    Object applyDomainNameCert(
            @ArgsConstraint(name = "domainSpace") String domainSpace,
            @ArgsConstraint(name = "domain") String domain,
            @ArgsConstraint(name = "applicantOidType") String applicantOidType,
            @ArgsConstraint(name = "oidFilePath") String oidFilePath
    ) {

        return queryAPI("applyDomainNameCert", domainSpace, domain, applicantOidType, oidFilePath);
    }

    Object queryDomainCertApplicationState(@ArgsConstraint(name = "domain") String domain) {

        return queryAPI("queryDomainCertApplicationState", domain);
    }

    Object fetchDomainNameCertFromBCDNS(@ArgsConstraint(name = "domain") String domain,
                                    @ArgsConstraint(name = "domainSpace") String domainSpace) {

        return queryAPI("fetchDomainNameCertFromBCDNS", domain, domainSpace);
    }

    Object registerDomainRouter(@ArgsConstraint(name = "domain") String domain) {

        return queryAPI("registerDomainRouter", domain);
    }

    Object addBlockchainTrustAnchor() {

        return queryAPI("addBlockchainTrustAnchor");
    }
}
