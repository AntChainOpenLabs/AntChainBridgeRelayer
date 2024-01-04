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

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Security;
import java.security.interfaces.ECPublicKey;

import cn.ac.caict.bid.model.BIDDocumentOperation;
import cn.ac.caict.bid.model.BIDpublicKeyOperation;
import cn.bif.common.JsonUtils;
import cn.bif.module.encryption.key.PrivateKeyManager;
import cn.bif.module.encryption.key.PublicKeyManager;
import cn.bif.module.encryption.model.KeyMember;
import cn.bif.module.encryption.model.KeyType;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.ECKeyUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifBCNDSConfig;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifCertificationServiceConfig;
import com.alipay.antchain.bridge.bcdns.impl.bif.conf.BifChainConfig;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.shell.standard.*;
import sun.security.x509.AlgorithmId;

@Getter
@ShellCommandGroup(value = "Utils Commands")
@ShellComponent
public class UtilsCommands {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @ShellMethod(value = "Generate PEM files for the relayer private and public key")
    public String generateRelayerAccount(
            @ShellOption(help = "Key algorithm, default Ed25519", defaultValue = "Ed25519") String keyAlgo,
            @ShellOption(valueProvider = FileValueProvider.class, help = "Directory path to save the keys", defaultValue = "") String outDir
    ) {
        if (!StrUtil.equalsIgnoreCase(keyAlgo, "Ed25519")) {
            throw new RuntimeException("only support Ed25519 now.");
        }

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyAlgo);
            keyPairGenerator.initialize(256);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            // dump the private key into pem
            StringWriter stringWriter = new StringWriter(256);
            JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
            jcaPEMWriter.writeObject(keyPair.getPrivate());
            jcaPEMWriter.close();
            String privatePem = stringWriter.toString();
            Path privatePath = Paths.get(outDir, "private_key.pem");
            Files.write(privatePath, privatePem.getBytes());

            // dump the public key into pem
            stringWriter = new StringWriter(256);
            jcaPEMWriter = new JcaPEMWriter(stringWriter);
            jcaPEMWriter.writeObject(keyPair.getPublic());
            jcaPEMWriter.close();
            String pubkeyPem = stringWriter.toString();
            Path publicPath = Paths.get(outDir, "public_key.pem");
            Files.write(publicPath, pubkeyPem.getBytes());

            return StrUtil.format("private key path: {}\npublic key path: {}", privatePath.toAbsolutePath(), publicPath.toAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", e);
        }
    }

    @ShellMethod(value = "Generate the BID document file containing the raw public key")
    public String generateBidDocument(
            @ShellOption(valueProvider = FileValueProvider.class, help = "The path to public key in PEM") String publicKeyPath,
            @ShellOption(valueProvider = FileValueProvider.class, help = "Directory path to save the output", defaultValue = "") String outDir
    ) {
        try {
            PublicKey publicKey = readPublicKeyFromPem(Files.readAllBytes(Paths.get(publicKeyPath)));
            BIDDocumentOperation bidDocumentOperation = getBid(publicKey);
            String rawBidDoc = JsonUtils.toJSONString(bidDocumentOperation);
            Path path = Paths.get(outDir, "bid_document.json");
            Files.write(path, rawBidDoc.getBytes());

            return "file is : " + path.toAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", e);
        }
    }

    @ShellMethod(value = "Generate the config json file for BIF BCDNS client")
    public String generateBifBcdnsConf(
            @ShellOption(valueProvider = FileValueProvider.class, help = "authorized private key to apply the relayer and ptc certificates from BIF BCDNS, default using relayer key", defaultValue = "") String authPrivateKeyFile,
            @ShellOption(valueProvider = FileValueProvider.class, help = "authorized public key to apply the relayer and ptc certificates from BIF BCDNS, default using relayer key", defaultValue = "") String authPublicKeyFile,
            @ShellOption(help = "Authorized key sig algorithm, default Ed25519", defaultValue = "Ed25519") String authSigAlgo,
            @ShellOption(valueProvider = FileValueProvider.class, help = "relayer private key") String relayerPrivateKeyFile,
            @ShellOption(valueProvider = FileValueProvider.class, help = "relayer cross-chain certificate") String relayerCrossChainCertFile,
            @ShellOption(help = "Relayer key sig algorithm, default Ed25519", defaultValue = "Ed25519") String relayerSigAlgo,
            @ShellOption(help = "Certificate server url of BIF BCDNS, e.g. http://localhost:8112") String certServerUrl,
            @ShellOption(help = "The RPC url for BIF blockchain node, e.g. `http://test.bifcore.bitfactory.cn` for testnet") String bifChainRpcUrl,
            @ShellOption(help = "The RPC port for BIF blockchain node if needed", defaultValue = "-1") Integer bifChainRpcPort,
            @ShellOption(help = "Domain govern contract address on BIF chain") String bifDomainGovernContract,
            @ShellOption(help = "PTC govern contract address on BIF chain") String bifPtcGovernContract,
            @ShellOption(help = "Relayer govern contract address on BIF chain") String bifRelayerGovernContract,
            @ShellOption(valueProvider = FileValueProvider.class, help = "Directory path to save the output", defaultValue = "") String outDir
    ) {
        try {
            if (!StrUtil.equalsIgnoreCase(relayerSigAlgo, "Ed25519")) {
                return "relayer sig algo only support Ed25519 for now";
            }

            AbstractCrossChainCertificate relayerCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
                    Files.readAllBytes(Paths.get(relayerCrossChainCertFile))
            );
            String authPublicKey;
            if (ObjectUtil.isEmpty(authPrivateKeyFile)) {
                authPrivateKeyFile = relayerPrivateKeyFile;
                authPublicKey = getPemPublicKey(CrossChainCertificateUtil.getPublicKeyFromCrossChainCertificate(relayerCert));
                authSigAlgo = relayerSigAlgo;
            } else {
                authPublicKey = new String(Files.readAllBytes(Paths.get(authPublicKeyFile)));
            }

            BifCertificationServiceConfig bifCertificationServiceConfig = new BifCertificationServiceConfig();
            bifCertificationServiceConfig.setAuthorizedKeyPem(new String(Files.readAllBytes(Paths.get(authPrivateKeyFile))));
            bifCertificationServiceConfig.setAuthorizedPublicKeyPem(authPublicKey);
            bifCertificationServiceConfig.setAuthorizedSigAlgo(authSigAlgo);
            bifCertificationServiceConfig.setClientPrivateKeyPem(new String(Files.readAllBytes(Paths.get(relayerPrivateKeyFile))));
            bifCertificationServiceConfig.setSigAlgo(relayerSigAlgo);
            bifCertificationServiceConfig.setClientCrossChainCertPem(CrossChainCertificateUtil.formatCrossChainCertificateToPem(relayerCert));
            bifCertificationServiceConfig.setUrl(certServerUrl);

            BifChainConfig bifChainConfig = new BifChainConfig();
            bifChainConfig.setBifChainRpcUrl(bifChainRpcUrl);
            if (bifChainRpcPort > 0) {
                bifChainConfig.setBifChainRpcPort(bifChainRpcPort);
            }
            bifChainConfig.setBifPrivateKey(convertToBIFPrivateKey(bifCertificationServiceConfig.getClientPrivateKeyPem()));
            bifChainConfig.setBifAddress(convertToBIFAddress(
                    CrossChainCertificateUtil.getRawPublicKeyFromCrossChainCertificate(relayerCert)
            ));
            bifChainConfig.setDomainGovernContract(bifDomainGovernContract);
            bifChainConfig.setPtcGovernContract(bifPtcGovernContract);
            bifChainConfig.setRelayerGovernContract(bifRelayerGovernContract);
            bifChainConfig.setCertificatesGovernContract("");

            BifBCNDSConfig config = new BifBCNDSConfig();
            config.setChainConfig(bifChainConfig);
            config.setCertificationServiceConfig(bifCertificationServiceConfig);

            Path path = Paths.get(outDir, "bif_bcdns_conf.json");
            Files.write(path, JSON.toJSONString(config, SerializerFeature.PrettyFormat).getBytes());

            return "file is : " + path.toAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", e);
        }
    }

    @ShellMethod(value = "Convert the crosschain certificate from other format to PEM")
    public String convertCrossChainCertToPem(
            @ShellOption(help = "Base64 format string of crosschain certificate") String base64Input,
            @ShellOption(valueProvider = FileValueProvider.class, help = "Directory path to save the output", defaultValue = "") String outDir
    ) {
        try {
            AbstractCrossChainCertificate crossChainCertificate = CrossChainCertificateFactory.createCrossChainCertificate(Base64.decode(base64Input));
            if (StrUtil.isNotEmpty(outDir)) {
                Path path = Paths.get(outDir, StrUtil.format("output_{}.crt", System.currentTimeMillis()));
                Files.write(path, CrossChainCertificateUtil.formatCrossChainCertificateToPem(crossChainCertificate).getBytes());
                return StrUtil.format("certificate in pem saved here: {}", path.toAbsolutePath().toString());
            }
            return CrossChainCertificateUtil.formatCrossChainCertificateToPem(crossChainCertificate);
        } catch (Exception e) {
            throw new RuntimeException("unexpected error please input stacktrace to check the detail", e);
        }
    }

    private String convertToBIFAddress(byte[] rawPublicKey) {
        PublicKeyManager publicKeyManager = new PublicKeyManager();
        publicKeyManager.setRawPublicKey(rawPublicKey);
        publicKeyManager.setKeyType(KeyType.ED25519);
        return publicKeyManager.getEncAddress();
    }

    private String convertToBIFPrivateKey(String privateKeyPem) {
        byte[] rawOctetStr = PrivateKeyInfo.getInstance(
                PemUtil.readPem(new ByteArrayInputStream(privateKeyPem.getBytes()))
        ).getPrivateKey().getOctets();
        KeyMember keyMember = new KeyMember();
        keyMember.setRawSKey(ArrayUtil.sub(rawOctetStr, 2, rawOctetStr.length));
        keyMember.setKeyType(KeyType.ED25519);
        return PrivateKeyManager.getEncPrivateKey(keyMember.getRawSKey(), keyMember.getKeyType());
    }

    @SneakyThrows
    private String getPemPublicKey(PublicKey publicKey) {
        StringWriter stringWriter = new StringWriter(256);
        JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter);
        jcaPEMWriter.writeObject(publicKey);
        jcaPEMWriter.close();
        return stringWriter.toString();
    }

    @SneakyThrows
    private PublicKey readPublicKeyFromPem(byte[] publicKeyPem) {
        SubjectPublicKeyInfo keyInfo = SubjectPublicKeyInfo.getInstance(PemUtil.readPem(new ByteArrayInputStream(publicKeyPem)));
        return KeyUtil.generatePublicKey(
                AlgorithmId.get(keyInfo.getAlgorithm().getAlgorithm().getId()).getName(),
                keyInfo.getEncoded()
        );
    }

    private BIDDocumentOperation getBid(PublicKey publicKey) {
        byte[] rawPublicKey;
        if (StrUtil.equalsIgnoreCase(publicKey.getAlgorithm(), "Ed25519")) {
            if (publicKey instanceof BCEdDSAPublicKey) {
                rawPublicKey = ((BCEdDSAPublicKey) publicKey).getPointEncoding();
            } else {
                throw new RuntimeException("your Ed25519 public key class not support: " + publicKey.getClass().getName());
            }
        } else if (StrUtil.equalsAnyIgnoreCase(publicKey.getAlgorithm(), "SM2", "EC")) {
            if (publicKey instanceof ECPublicKey) {
                rawPublicKey = ECKeyUtil.toPublicParams(publicKey).getQ().getEncoded(false);
            } else {
                throw new RuntimeException("your SM2/EC public key class not support: " + publicKey.getClass().getName());
            }
        } else {
            throw new RuntimeException(publicKey.getAlgorithm() + " not support");
        }

        byte[] rawPublicKeyWithSignals = new byte[rawPublicKey.length + 3];
        System.arraycopy(rawPublicKey, 0, rawPublicKeyWithSignals, 3, rawPublicKey.length);
        rawPublicKeyWithSignals[0] = -80;
        rawPublicKeyWithSignals[1] = StrUtil.equalsIgnoreCase(publicKey.getAlgorithm(), "Ed25519") ? KeyType.ED25519_VALUE : KeyType.SM2_VALUE;
        rawPublicKeyWithSignals[2] = 102;

        BIDpublicKeyOperation[] biDpublicKeyOperation = new BIDpublicKeyOperation[1];
        biDpublicKeyOperation[0] = new BIDpublicKeyOperation();
        biDpublicKeyOperation[0].setPublicKeyHex(HexUtil.encodeHexStr(rawPublicKeyWithSignals));
        biDpublicKeyOperation[0].setType(StrUtil.equalsIgnoreCase(publicKey.getAlgorithm(), "Ed25519") ? KeyType.ED25519 : KeyType.SM2);
        BIDDocumentOperation bidDocumentOperation = new BIDDocumentOperation();
        bidDocumentOperation.setPublicKey(biDpublicKeyOperation);

        return bidDocumentOperation;
    }
}
