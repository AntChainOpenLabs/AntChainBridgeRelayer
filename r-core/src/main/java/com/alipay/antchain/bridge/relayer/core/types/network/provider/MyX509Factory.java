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

package com.alipay.antchain.bridge.relayer.core.types.network.provider;

import java.io.InputStream;
import java.security.cert.*;
import java.util.Collection;

import lombok.extern.slf4j.Slf4j;
import sun.security.provider.X509Factory;

@Slf4j
public class MyX509Factory extends X509Factory {
    @Override
    public Certificate engineGenerateCertificate(InputStream inStream)
            throws CertificateException {
        try {
            return super.engineGenerateCertificate(inStream);
        } catch (Throwable t) {
            log.error("failed!!!", t);
            throw t;
        }
    }

    @Override
    public Collection<? extends Certificate> engineGenerateCertificates(InputStream inStream) throws CertificateException {
        try {
            return super.engineGenerateCertificates(inStream);
        } catch (Throwable t) {
            log.error("failed!!!", t);
            throw t;
        }
    }

    @Override
    public CRL engineGenerateCRL(InputStream inStream) throws CRLException {
        try {
            return super.engineGenerateCRL(inStream);
        } catch (Throwable t) {
            log.error("failed!!!", t);
            throw t;
        }
    }

    @Override
    public Collection<? extends CRL> engineGenerateCRLs(InputStream inStream) throws CRLException {
        try {
            return super.engineGenerateCRLs(inStream);
        } catch (Throwable t) {
            log.error("failed!!!", t);
            throw t;
        }
    }
}
