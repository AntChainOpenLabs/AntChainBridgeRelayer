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

package com.alipay.antchain.bridge.relayer.server.admin.impl;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import javax.annotation.Resource;

import cn.ac.caict.bid.model.BIDDocumentOperation;
import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import com.alipay.antchain.bridge.bcdns.service.BCDNSTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.utils.BIDHelper;
import com.alipay.antchain.bridge.commons.core.base.BIDInfoObjectIdentity;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentityType;
import com.alipay.antchain.bridge.commons.core.base.X509PubkeyInfoObjectIdentity;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.server.admin.AbstractNamespace;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.springframework.stereotype.Component;
import sun.security.x509.AlgorithmId;

@Component
@Slf4j
public class BCDNSNamespace extends AbstractNamespace {

    @Resource
    private IBCDNSManager bcdnsManager;

    public BCDNSNamespace() {
        addCommand("registerBCDNSService", this::registerBCDNSService);
        addCommand("stopBCDNSService", this::stopBCDNSService);
        addCommand("restartBCDNSService", this::restartBCDNSService);
        addCommand("applyDomainNameCert", this::applyDomainNameCert);
    }

    Object registerBCDNSService(String... args) {
        if (args.length != 3 && args.length != 4) {
            return "wrong number of arguments";
        }
        try {
            String domainSpace = args[0];
            String bcdnsType = args[1];
            String propFile = args[2];

            bcdnsManager.registerBCDNSService(
                    domainSpace,
                    BCDNSTypeEnum.parseFromValue(bcdnsType),
                    propFile,
                    args.length == 4 ? args[3] : ""
            );

            return "success";
        } catch (Throwable e) {
            log.error("failed to register BCDNS for domain space {}", args[0], e);
            return "failed to register BCDNS: " + e.getMessage();
        }
    }

    Object stopBCDNSService(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }
        try {
            bcdnsManager.stopBCDNSService(args[0]);
            return "success";
        } catch (Throwable e) {
            log.error("failed to stop BCDNS for domain space {}", args[0], e);
            return "failed to stop BCDNS: " + e.getMessage();
        }
    }

    Object restartBCDNSService(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }
        try {
            bcdnsManager.restartBCDNSService(args[0]);
            return "success";
        } catch (Throwable e) {
            log.error("failed to restart BCDNS for domain space {}", args[0], e);
            return "failed to restart BCDNS: " + e.getMessage();
        }
    }

    Object applyDomainNameCert(String... args) {
        if (args.length != 4) {
            return "wrong number of arguments";
        }

        String domainSpace = args[0];
        String domain = args[1];
        int applicantOidType = Integer.parseInt(args[2]);
        String oidFilePath = args[3];

        try {

            byte[] rawSubject = null;
            ObjectIdentity oid = null;
            if (ObjectIdentityType.parseFromValue(applicantOidType) == ObjectIdentityType.BID) {
                rawSubject = FileUtil.readBytes("file:" + oidFilePath);
                BIDDocumentOperation bidDocumentOperation = BIDHelper.getBIDDocumentFromRawSubject(rawSubject);
                oid = new BIDInfoObjectIdentity(
                        BIDHelper.encAddress(
                                bidDocumentOperation.getPublicKey()[0].getType(),
                                BIDHelper.getRawPublicKeyFromBIDDocument(bidDocumentOperation)
                        )
                );
            } else if (ObjectIdentityType.parseFromValue(applicantOidType) == ObjectIdentityType.X509_PUBLIC_KEY_INFO) {
                PublicKey publicKey = readPublicKeyFromPem(FileUtil.readBytes("file:" + oidFilePath));
                oid = new X509PubkeyInfoObjectIdentity(publicKey.getEncoded());
                rawSubject = new byte[]{};
            }

            String receipt = bcdnsManager.applyDomainCertificate(
                    domainSpace,
                    domain,
                    oid,
                    rawSubject
            );
            return "your receipt is " + receipt;
        } catch (Throwable e) {
            log.error("failed to restart BCDNS for domain space {}", args[0], e);
            return "failed to restart BCDNS: " + e.getMessage();
        }
    }

    @SneakyThrows
    private PublicKey readPublicKeyFromPem(byte[] publicKeyPem) {
        SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(PemUtil.readPem(new ByteArrayInputStream(publicKeyPem)));
        return KeyUtil.generatePublicKey(
                AlgorithmId.get(keyInfo.getAlgorithm().getAlgorithm().getId()).getName(),
                keyInfo.getEncoded()
        );
    }
}
