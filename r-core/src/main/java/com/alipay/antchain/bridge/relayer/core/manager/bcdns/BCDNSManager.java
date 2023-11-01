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

import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.BCDNSTrustRootCredentialSubject;
import org.springframework.stereotype.Component;

@Component
public class BCDNSManager implements IBCDNSManager {

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
}
