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

package com.alipay.antchain.bridge.relayer.core.manager.bcdns;

import java.util.List;
import java.util.Map;
import javax.annotation.Resource;

import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.BCDNSTrustRootCredentialSubject;
import com.alipay.antchain.bridge.relayer.commons.model.DomainSpaceCertWrapper;
import com.alipay.antchain.bridge.relayer.dal.repository.IBCDNSRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BCDNSManager implements IBCDNSManager {

    @Resource
    private IBCDNSRepository bcdnsRepository;

    @Override
    public Map<String, AbstractCrossChainCertificate> getAllTrustRootCerts() {
        return null;
    }

    @Override
    public AbstractCrossChainCertificate getTrustRootCert(String domainSpace) {
        return null;
    }

    @Override
    public Map<String, AbstractCrossChainCertificate> getTrustRootCertChain(String domainSpace) {
        return null;
    }

    @Override
    public List<String> getDomainSpaceChain(String domainSpace) {
        return null;
    }

    @Override
    public BCDNSTrustRootCredentialSubject getTrustRootCredentialSubject(String domainSpace) {
        return null;
    }

    @Override
    public AbstractCrossChainCertificate getTrustRootCertForRootDomain() {
        return null;
    }

    @Override
    public BCDNSTrustRootCredentialSubject getTrustRootCredentialSubjectForRootDomain() {
        return null;
    }

    @Override
    public boolean validateCrossChainCertificate(AbstractCrossChainCertificate certificate) {
        return false;
    }

    @Override
    public boolean validateDomainCertificate(AbstractCrossChainCertificate certificate, List<String> domainSpaceChain) {
        return false;
    }

    @Override
    public void saveDomainSpaceCerts(Map<String, AbstractCrossChainCertificate> domainSpaceCerts) {
        for (Map.Entry<String, AbstractCrossChainCertificate> entry : domainSpaceCerts.entrySet()) {
            try {
                if (bcdnsRepository.hasDomainSpaceCert(entry.getKey())) {
                    log.warn("DomainSpace {} already exists", entry.getKey());
                    continue;
                }
                bcdnsRepository.saveDomainSpaceCert(new DomainSpaceCertWrapper(entry.getValue()));
                log.info("successful to save domain space cert for {}", entry.getKey());
            } catch (Exception e) {
                log.error("failed to save domain space certs for space {} : ", entry.getKey(), e);
            }
        }
    }
}
