/*
 * Alipay.com Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.alipay.antchain.bridge.relayer.bootstrap;

import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.PemUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.DomainNameCredentialSubject;
import com.alipay.antchain.bridge.relayer.bootstrap.basic.BlockchainModelsTest;
import com.alipay.antchain.bridge.relayer.bootstrap.utils.MyRedisServer;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisExecProvider;
import redis.embedded.util.Architecture;
import redis.embedded.util.OS;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = AntChainBridgeRelayerApplication.class)
@Sql(scripts = {"classpath:data/ddl.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/drop_all.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public abstract class TestBase {

    public static final BlockchainMeta.BlockchainProperties blockchainProperties1
            = BlockchainMeta.BlockchainProperties.decode(BlockchainModelsTest.BLOCKCHAIN_META_EXAMPLE_OBJ.getBytes());

    public static final BlockchainMeta.BlockchainProperties blockchainProperties2
            = BlockchainMeta.BlockchainProperties.decode(BlockchainModelsTest.BLOCKCHAIN_META_EXAMPLE_OBJ.getBytes());

    public static final BlockchainMeta.BlockchainProperties blockchainProperties3
            = BlockchainMeta.BlockchainProperties.decode(BlockchainModelsTest.BLOCKCHAIN_META_EXAMPLE_OBJ.getBytes());

    public static final BlockchainMeta testchain1Meta = new BlockchainMeta("testchain", "testchain_1.id", "", "", blockchainProperties1);

    public static final BlockchainMeta testchain2Meta = new BlockchainMeta("testchain", "testchain_2.id", "", "", blockchainProperties2);

    public static final BlockchainMeta testchain3Meta = new BlockchainMeta("testchain", "testchain_3.id", "", "", blockchainProperties3);

    public static AbstractCrossChainCertificate antchainDotCommCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/antchain.com.crt")
    );

    public static String antChainDotComDomain = "antchain.com";

    public static String antChainDotComProduct = "mychain";

    public static String antChainDotComBlockchainId = antChainDotComDomain + ".id";

    public static AbstractCrossChainCertificate catchainDotCommCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/catchain.com.crt")
    );

    public static String catChainDotComProduct = "ethereum";

    public static String catChainDotComDomain = "catchain.com";

    public static String catChainDotComBlockchainId = catChainDotComDomain + ".id";

    public static AbstractCrossChainCertificate dogchainDotCommCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/dogchain.com.crt")
    );

    public DomainNameCredentialSubject antchainSubject = DomainNameCredentialSubject.decode(antchainDotCommCert.getCredentialSubject());

    public DomainNameCredentialSubject catchainSubject = DomainNameCredentialSubject.decode(catchainDotCommCert.getCredentialSubject());

    public DomainNameCredentialSubject dogchainSubject = DomainNameCredentialSubject.decode(dogchainDotCommCert.getCredentialSubject());

    public static AbstractCrossChainCertificate dotComDomainSpaceCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/x.com.crt")
    );

    public static AbstractCrossChainCertificate dotComDomainSpaceCertWrongIssuer = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/x.com_wrong_issuer.crt")
    );

    public static String dotComDomainSpace = ".com";

    public static AbstractCrossChainCertificate relayerCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/relayer.crt")
    );

    public static AbstractCrossChainCertificate trustRootCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/trust_root.crt")
    );

    public static AbstractCrossChainCertificate relayerCertWrongIssuer = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/relayer_wrong_issuer.crt")
    );
    
    public static byte[] rawBIDDocument = FileUtil.readBytes("cc_certs/bid_document.json");

    public static String bid = "did:bid:efbThy5sbG7P3mFUp2EWN5oQGX6LUGwg";

    public static PrivateKey privateKey;

    public static MyRedisServer redisServer;

    @BeforeClass
    public static void beforeTest() throws Exception {
        // if the embedded redis can't start correctly,
        // try to use local redis server binary to start it.
        redisServer = new MyRedisServer(
                RedisExecProvider.defaultProvider()
                        .override(OS.MAC_OS_X, Architecture.x86_64, "src/test/resources/bins/redis-server")
                        .override(OS.MAC_OS_X, Architecture.x86, "src/test/resources/bins/redis-server"),
                6379
        );
        redisServer.start();
        privateKey = getLocalPrivateKey("cc_certs/private_key.pem");
    }

    @AfterClass
    public static void after() throws Exception {
        redisServer.stop();
    }

    public static PrivateKey getLocalPrivateKey(String path) throws NoSuchAlgorithmException, InvalidKeySpecException {
        try {
            return PemUtil.readPemPrivateKey(new ByteArrayInputStream(FileUtil.readBytes(path)));
        } catch (Exception e) {
            byte[] rawPemOb = PemUtil.readPem(new ByteArrayInputStream(FileUtil.readBytes(path)));
            KeyFactory keyFactory = KeyFactory.getInstance(
                    PrivateKeyInfo.getInstance(rawPemOb).getPrivateKeyAlgorithm().getAlgorithm().getId()
            );
            return keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(
                            rawPemOb
                    )
            );
        }
    }
}
