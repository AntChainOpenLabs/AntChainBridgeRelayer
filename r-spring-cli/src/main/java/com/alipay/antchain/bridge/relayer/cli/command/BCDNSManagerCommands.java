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
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@Getter
@ShellCommandGroup(value = "Commands about BCDNS")
@ShellComponent
public class BCDNSManagerCommands extends BaseCommands {

    @Resource
    private GrpcClient grpcClient;

    @Override
    public String name() {
        return "bcdns";
    }

    @ShellMethod(value = "Register a new BCDNS bound with specified domain space into Relayer")
    Object registerBCDNSService(
            @ShellOption(help = "The domain space owned by the BCDNS") String domainSpace,
            @ShellOption(help = "The type of the BCDNS, e.g. embedded, bif") String bcdnsType,
            @ShellOption(help = "The properties file path needed to initialize the service stub, e.g. /path/to/your/prop.json") String propFile
    ) {
        return queryAPI("registerBCDNSService", domainSpace, bcdnsType, propFile);
    }

    @ShellMethod(value = "Register a new BCDNS bound with specified domain space into Relayer")
    Object registerBCDNSServiceWithCert(
            @ShellOption(help = "The domain space owned by the BCDNS") String domainSpace,
            @ShellOption(help = "The type of the BCDNS, e.g. 0 for EMBEDDED, 1 for BIF") String bcdnsType,
            @ShellOption(help = "The properties file path needed to initialize the service stub, e.g. /path/to/your/prop.json") String propFile,
            @ShellOption(help = "The path to BCDNS trust root certificate file") String bcdnsCertPath
    ) {
        return queryAPI("registerBCDNSService", domainSpace, bcdnsType, propFile, bcdnsCertPath);
    }

    @ShellMethod(value = "Get the BCDNS data bound with specified domain space")
    Object getBCDNSService(@ShellOption(help = "The domain space bound with BCDNS") String domainSpace) {
        return queryAPI("getBCDNSService", domainSpace);
    }

    @ShellMethod(value = "Delete the BCDNS bound with specified domain space")
    Object deleteBCDNSService(@ShellOption(help = "The domain space bound with BCDNS") String domainSpace) {
        return queryAPI("deleteBCDNSService", domainSpace);
    }

    @ShellMethod(value = "Get the BCDNS trust root certificate bound with specified domain space")
    Object getBCDNSCertificate(@ShellOption(help = "The domain space bound with BCDNS") String domainSpace) {
        return queryAPI("getBCDNSCertificate", domainSpace);
    }

    @ShellMethod(value = "Stop the local BCDNS service stub")
    Object stopBCDNSService(@ShellOption(help = "The domain space bound with BCDNS") String domainSpace) {
        return queryAPI("stopBCDNSService", domainSpace);
    }

    @ShellMethod(value = "Restart the local BCDNS service stub from stop")
    Object restartBCDNSService(@ShellOption(help = "domainSpace") String domainSpace) {

        return queryAPI("restartBCDNSService", domainSpace);
    }

    @ShellMethod(value = "Apply a domain certificate for a blockchain from the BCDNS with specified domain space")
    Object applyDomainNameCert(
            @ShellOption(help = "The domain space bound with BCDNS") String domainSpace,
            @ShellOption(help = "The domain applying") String domain,
            @ShellOption(help = "The type for applicant subject, e.g. 0 for `X509_PUBLIC_KEY_INFO`, 1 for `BID`") String applicantOidType,
            @ShellOption(help = "oidFilePath") String oidFilePath
    ) {
        return queryAPI("applyDomainNameCert", domainSpace, domain, applicantOidType, oidFilePath);
    }

    @ShellMethod(value = "Query the state of application for a specified blockchain domain")
    Object queryDomainCertApplicationState(@ShellOption(help = "The specified domain") String domain) {
        return queryAPI("queryDomainCertApplicationState", domain);
    }

    @ShellMethod(value = "Fetch the certificate for a specified blockchain domain from the BCDNS with the domain space")
    Object fetchDomainNameCertFromBCDNS(
            @ShellOption(help = "The specified domain") String domain,
            @ShellOption(help = "The BCDNS domain space") String domainSpace
    ) {

        return queryAPI("fetchDomainNameCertFromBCDNS", domain, domainSpace);
    }

    @ShellMethod(value = "Query the domain name certificate from the BCDNS with the domain space")
    Object queryDomainNameCertFromBCDNS(
            @ShellOption(help = "The specified domain") String domain,
            @ShellOption(help = "The BCDNS domain space") String domainSpace
    ) {
        return queryAPI("queryDomainNameCertFromBCDNS", domain, domainSpace);
    }

    @ShellMethod(value = "Register the domain router including the specified domain name and " +
            "local relayer information to the BCDNS with the parent domain space for the domain")
    Object registerDomainRouter(@ShellOption(help = "The specified domain") String domain) {
        return queryAPI("registerDomainRouter", domain);
    }
    @ShellMethod(value = "Query the domain router for the domain from the BCDNS with the domain space")
    Object queryDomainRouter(
            @ShellOption(help = "The specified domain") String domain,
            @ShellOption(help = "The BCDNS domain space") String domainSpace
    ) {

        return queryAPI("queryDomainRouter", domainSpace, domain);
    }

    Object addBlockchainTrustAnchor() {
        return queryAPI("addBlockchainTrustAnchor");
    }
}
