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

package com.alipay.antchain.bridge.relayer.core.types.network.ws;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.SSLUtil;
import cn.hutool.crypto.PemUtil;
import io.grpc.util.CertificateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WsSslFactory {

    @Value("${relayer.network.node.tls.private_key_path}")
    private String privateKeyPath;

    @Value("${relayer.network.node.tls.trust_ca_path}")
    private String trustCaPath;

    public SSLContext getSslContext() throws Exception {
        PrivateKey privateKey = PemUtil.readPemPrivateKey(
                new ByteArrayInputStream(FileUtil.readBytes(privateKeyPath))
        );
        Certificate[] trustCertificates = CertificateUtils.getX509Certificates(
                new ByteArrayInputStream(FileUtil.readBytes(trustCaPath))
        );

        char[] keyStorePassword = new char[0];
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        int count = 0;
        for (Certificate cert : trustCertificates) {
            keyStore.setCertificateEntry("cert" + count, cert);
            count++;
        }
        keyStore.setKeyEntry("key", privateKey, keyStorePassword, trustCertificates);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
        );
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm()
        );

        trustManagerFactory.init(keyStore);
        keyManagerFactory.init(keyStore, keyStorePassword);

        return SSLUtil.createSSLContext(
                "TLSv1.2",
                keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers()
        );
    }
}
