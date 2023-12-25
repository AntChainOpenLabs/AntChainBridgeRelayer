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
import cn.bif.module.encryption.model.KeyType;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.ECKeyUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import lombok.Getter;
import lombok.SneakyThrows;
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
