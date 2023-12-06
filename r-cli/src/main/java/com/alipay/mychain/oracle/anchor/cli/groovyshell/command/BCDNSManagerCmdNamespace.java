package com.alipay.mychain.oracle.anchor.cli.groovyshell.command;

import com.alipay.mychain.oracle.anchor.cli.command.ArgsConstraint;
import com.alipay.mychain.oracle.anchor.cli.groovyshell.GroovyScriptCommandNamespace;

public class BCDNSManagerCmdNamespace extends GroovyScriptCommandNamespace {

    @Override
    public String name() {
        return "BCDNSManager";
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
