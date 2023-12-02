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

package com.alipay.antchain.bridge.relayer.commons.model;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import lombok.Getter;
import sun.security.x509.AlgorithmId;

@Getter
public class X509CrossChainCertificate extends X509Certificate {

    private final AbstractCrossChainCertificate crossChainCertificate;

    /**
     * PublicKey that has previously been used to verify
     * the signature of this certificate. Null if the certificate has not
     * yet been verified.
     */
    private PublicKey verifiedPublicKey;

    /**
     * If verifiedPublicKey is not null, name of the provider used to
     * successfully verify the signature of this certificate, or the
     * empty String if no provider was explicitly specified.
     */
    private String verifiedProvider;

    /**
     * If verifiedPublicKey is not null, result of the verification using
     * verifiedPublicKey and verifiedProvider. If true, verification was
     * successful, if false, it failed.
     */
    private boolean verificationResult;

    private AlgorithmId algoId;

    public X509CrossChainCertificate(AbstractCrossChainCertificate crossChainCertificate) {
        this.crossChainCertificate = crossChainCertificate;
        try {
            this.algoId = AlgorithmId.get(crossChainCertificate.getProof().getSigAlgo());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("failed to get oid string for " + crossChainCertificate.getProof().getSigAlgo(), e);
        }
    }

    @Override
    public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
        if (System.currentTimeMillis() < crossChainCertificate.getIssuanceDate()) {
            throw new CertificateNotYetValidException("not valid yet");
        }
        if (System.currentTimeMillis() > crossChainCertificate.getExpirationDate()) {
            throw new CertificateExpiredException("already expired");
        }
    }

    @Override
    public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException {
        if (date.getTime() < crossChainCertificate.getIssuanceDate()) {
            throw new CertificateNotYetValidException("not valid yet");
        }
        if (date.getTime() > crossChainCertificate.getExpirationDate()) {
            throw new CertificateExpiredException("already expired");
        }
    }

    @Override
    public int getVersion() {
        // mock x509v3
        return 3;
    }

    @Override
    public BigInteger getSerialNumber() {
        // mock x509v3
        return new BigInteger("15713616444353549339");
    }

    @Override
    public Principal getIssuerDN() {
        return null;
    }

    @Override
    public Principal getSubjectDN() {
        return null;
    }

    @Override
    public Date getNotBefore() {
        return new Date(crossChainCertificate.getIssuanceDate());
    }

    @Override
    public Date getNotAfter() {
        return new Date(crossChainCertificate.getExpirationDate());
    }

    @Override
    public byte[] getTBSCertificate() throws CertificateEncodingException {
        return crossChainCertificate.getEncodedToSign();
    }

    @Override
    public byte[] getSignature() {
        return crossChainCertificate.getProof().getRawProof().clone();
    }

    @Override
    public String getSigAlgName() {
        return crossChainCertificate.getProof().getSigAlgo();
    }

    @Override
    public String getSigAlgOID() {
        if (algoId == null) {
            return null;
        }
        return algoId.getOID().toString();
    }

    @Override
    public byte[] getSigAlgParams() {
        if (algoId == null) {
            return null;
        }
        try {
            return algoId.getEncodedParams();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public boolean[] getIssuerUniqueID() {
        return null;
    }

    @Override
    public boolean[] getSubjectUniqueID() {
        return null;
    }

    @Override
    public boolean[] getKeyUsage() {
        return null;
    }

    @Override
    public int getBasicConstraints() {
        return -1;
    }

    @Override
    public byte[] getEncoded() throws CertificateEncodingException {
        return crossChainCertificate.encode();
    }

    @Override
    public void verify(PublicKey key) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        verify(key, "");
    }

    @Override
    public void verify(PublicKey key, String sigProvider) throws CertificateException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException {
        sigProvider = StrUtil.emptyIfNull(sigProvider);
        if ((verifiedPublicKey != null) && verifiedPublicKey.equals(key)) {
            // this certificate has already been verified using
            // this public key. Make sure providers match, too.
            if (sigProvider.equals(verifiedProvider)) {
                if (verificationResult) {
                    return;
                } else {
                    throw new SignatureException("Signature does not match.");
                }
            }
        }

        if (ObjectUtil.isNull(crossChainCertificate)) {
            throw new CertificateEncodingException("Uninitialized certificate");
        }

        Signature sigVerf;
        // Verify the signature ...
        if (StrUtil.isEmpty(sigProvider)) {
            sigVerf = Signature.getInstance(crossChainCertificate.getProof().getSigAlgo());
        } else {
            sigVerf = Signature.getInstance(crossChainCertificate.getProof().getSigAlgo(), sigProvider);
        }
        sigVerf.update(crossChainCertificate.getEncodedToSign());

        // verify may throw SignatureException for invalid encodings, etc.
        verificationResult = sigVerf.verify(crossChainCertificate.getProof().getRawProof());
        verifiedPublicKey = key;
        verifiedProvider = sigProvider;

        if (!verificationResult) {
            throw new SignatureException("Signature does not match.");
        }
    }

    @Override
    public String toString() {
        return CrossChainCertificateUtil.formatCrossChainCertificateToPem(crossChainCertificate);
    }

    @Override
    public PublicKey getPublicKey() {
        return CrossChainCertificateUtil.getPublicKeyFromCrossChainCertificate(crossChainCertificate);
    }

    @Override
    public boolean hasUnsupportedCriticalExtension() {
        return false;
    }

    @Override
    public Set<String> getCriticalExtensionOIDs() {
        return new HashSet<>();
    }

    @Override
    public Set<String> getNonCriticalExtensionOIDs() {
        // mock x509v3
        Set<String> res = new HashSet<>();
        res.add("2.5.29.19");
        res.add("2.5.29.37");
        return res;
    }

    @Override
    public byte[] getExtensionValue(String oid) {
        // mock x509v3
        if (StrUtil.equals("2.5.29.37", oid)) {
            return HexUtil.decodeHex("0416301406082b0601050507030106082b06010505070302");
        }
        if (StrUtil.equals("2.5.29.19", oid)) {
            return HexUtil.decodeHex("04023000");
        }
        return null;
    }
}
