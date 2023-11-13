/*
 * Alipay.com Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.alipay.antchain.bridge.relayer.bootstrap;

import cn.hutool.core.io.FileUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.DomainNameCredentialSubject;
import com.alipay.antchain.bridge.relayer.bootstrap.basic.BlockchainModelsTest;
import com.alipay.antchain.bridge.relayer.bootstrap.utils.MyRedisServer;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
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

    public static AbstractCrossChainCertificate catchainDotCommCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/catchain.com.crt")
    );

    public static AbstractCrossChainCertificate dogchainDotCommCert = CrossChainCertificateFactory.createCrossChainCertificateFromPem(
            FileUtil.readBytes("cc_certs/dogchain.com.crt")
    );

    public DomainNameCredentialSubject antchainSubject = DomainNameCredentialSubject.decode(antchainDotCommCert.getCredentialSubject());

    public DomainNameCredentialSubject catchainSubject = DomainNameCredentialSubject.decode(catchainDotCommCert.getCredentialSubject());

    public DomainNameCredentialSubject dogchainSubject = DomainNameCredentialSubject.decode(dogchainDotCommCert.getCredentialSubject());

    public static MyRedisServer redisServer;

    @BeforeClass
    public static void beforeTest() throws Exception {
        redisServer = new MyRedisServer(
                RedisExecProvider.defaultProvider()
                        .override(OS.MAC_OS_X, Architecture.x86_64, "/usr/local/bin/redis-server")
                        .override(OS.MAC_OS_X, Architecture.x86, "/usr/local/bin/redis-server"),
                6379
        );
        redisServer.start();
    }

    @AfterClass
    public static void after() throws Exception {
        redisServer.stop();
    }
}
