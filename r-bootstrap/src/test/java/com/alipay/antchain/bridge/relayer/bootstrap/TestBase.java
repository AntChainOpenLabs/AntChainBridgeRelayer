/*
 * Alipay.com Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.alipay.antchain.bridge.relayer.bootstrap;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import cn.hutool.cache.Cache;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alipay.antchain.bridge.bcdns.service.BCDNSTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.DomainNameCredentialSubject;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageTrustLevelEnum;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageV2;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainIdentity;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageV2;
import com.alipay.antchain.bridge.pluginserver.service.*;
import com.alipay.antchain.bridge.relayer.bootstrap.basic.BlockchainModelsTest;
import com.alipay.antchain.bridge.relayer.bootstrap.utils.MyRedisServer;
import com.alipay.antchain.bridge.relayer.commons.constant.BCDNSStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.PluginServerStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.UpperProtocolTypeBeyondAMEnum;
import com.alipay.antchain.bridge.relayer.commons.model.*;
import com.google.protobuf.ByteString;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisExecProvider;
import redis.embedded.util.Architecture;
import redis.embedded.util.OS;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = AntChainBridgeRelayerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
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

    public static BCDNSServiceDO rootBcdnsServiceDO = new BCDNSServiceDO(
            CrossChainDomain.ROOT_DOMAIN_SPACE,
            trustRootCert.getCredentialSubjectInstance().getApplicant(),
            new DomainSpaceCertWrapper(trustRootCert),
            BCDNSTypeEnum.BIF,
            BCDNSStateEnum.WORKING,
            FileUtil.readBytes("bcdns/root_bcdns.json")
    );

    public static BCDNSServiceDO dotComBcdnsServiceDO = new BCDNSServiceDO(
            dotComDomainSpace,
            dotComDomainSpaceCert.getCredentialSubjectInstance().getApplicant(),
            new DomainSpaceCertWrapper(dotComDomainSpaceCert),
            BCDNSTypeEnum.BIF,
            BCDNSStateEnum.WORKING,
            FileUtil.readBytes("bcdns/root_bcdns.json")
    );

    public static final String PS_ID = "p-QYj86x8Zd";

    public static final String PS_ADDR = "localhost:9090";

    public static String psCert = FileUtil.readString("node_keys/ps/relayer.crt", Charset.defaultCharset());

    public static PluginServerDO pluginServerDO = new PluginServerDO(
            0,
            PS_ID,
            PS_ADDR,
            PluginServerStateEnum.INIT,
            ListUtil.toList(antChainDotComProduct, catChainDotComProduct),
            ListUtil.toList(antChainDotComDomain, catChainDotComDomain),
            new PluginServerDO.PluginServerProperties(psCert),
            new Date(),
            new Date()
    );

    public static AuthMessageV2 authMessageV2 = new AuthMessageV2();

    public static SDPMessageV2 sdpMessageV2 = new SDPMessageV2();

    @MockBean
    public Cache<String, RelayerNodeInfo> relayerNodeInfoCache;

    @MockBean
    public Cache<String, BlockchainMeta> blockchainMetaCache;

    @MockBean
    public Cache<String, DomainCertWrapper> domainCertWrapperCache;

    @MockBean
    public Cache<String, RelayerNetwork.DomainRouterItem> relayerNetworkItemCache;

    public AtomicLong currHeight = new AtomicLong(100L);

    public void initBaseBBCMock(
            CrossChainServiceGrpc.CrossChainServiceBlockingStub crossChainServiceBlockingStub,
            MockedStatic<CrossChainServiceGrpc> mockedStaticCrossChainServiceGrpc
    ) {
        mockedStaticCrossChainServiceGrpc.when(
                () -> CrossChainServiceGrpc.newBlockingStub(Mockito.any())
        ).thenReturn(crossChainServiceBlockingStub);

        Mockito.when(crossChainServiceBlockingStub.bbcCall(Mockito.argThat(
                argument -> {
                    if (ObjectUtil.isNull(argument)) {
                        return false;
                    }
                    return argument.hasGetContextReq();
                }
        ))).thenReturn(Response.newBuilder().setCode(0).setBbcResp(
                        CallBBCResponse.newBuilder().setGetContextResp(
                                GetContextResponse.newBuilder().setRawContext(
                                        ByteString.copyFrom(blockchainProperties1.getBbcContext().encodeToBytes())
                                ).build()
                        ).build()
                ).build()
        );
        Mockito.when(crossChainServiceBlockingStub.bbcCall(Mockito.argThat(
                argument -> {
                    if (ObjectUtil.isNull(argument)) {
                        return false;
                    }
                    return argument.hasQueryLatestHeightReq();
                }
        ))).thenReturn(Response.newBuilder().setCode(0).setBbcResp(
                        CallBBCResponse.newBuilder().setQueryLatestHeightResponse(
                                QueryLatestHeightResponse.newBuilder()
                                        .setHeight(currHeight.incrementAndGet()).build()
                        ).build()
                ).build(),
                Response.newBuilder().setCode(0).setBbcResp(
                        CallBBCResponse.newBuilder().setQueryLatestHeightResponse(
                                QueryLatestHeightResponse.newBuilder()
                                        .setHeight(currHeight.incrementAndGet()).build()
                        ).build()
                ).build(),
                Response.newBuilder().setCode(0).setBbcResp(
                        CallBBCResponse.newBuilder().setQueryLatestHeightResponse(
                                QueryLatestHeightResponse.newBuilder()
                                        .setHeight(currHeight.incrementAndGet()).build()
                        ).build()
                ).build()
        );
        Mockito.when(crossChainServiceBlockingStub.bbcCall(
                Mockito.argThat(
                        argument -> {
                            if (ObjectUtil.isNull(argument)) {
                                return false;
                            }
                            return argument.hasStartUpReq() || argument.hasSetupAuthMessageContractReq()
                                    || argument.hasSetupSDPMessageContractReq() || argument.hasSetProtocolReq()
                                    || argument.hasSetAmContractReq() || argument.hasSetLocalDomainReq();
                        }
                )
        )).thenReturn(Response.newBuilder().setCode(0).build());
        Mockito.when(crossChainServiceBlockingStub.heartbeat(Mockito.any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setHeartbeatResp(
                                HeartbeatResponse.newBuilder()
                                        .addAllProducts(ListUtil.toList(antChainDotComProduct))
                                        .addAllDomains(ListUtil.toList(antChainDotComDomain))
                                        .build()
                        ).build()
        );

        Mockito.when(crossChainServiceBlockingStub.ifProductSupport(Mockito.any())).thenReturn(
                Response.newBuilder()
                        .setCode(0)
                        .setIfProductSupportResp(
                                IfProductSupportResponse.newBuilder()
                                        .putResults(antChainDotComProduct, true)
                                        .putResults(catChainDotComProduct, true)
                                        .build()
                        ).build()
        );
    }

    @BeforeClass
    public static void beforeTest() throws Exception {

        sdpMessageV2.setAtomic(false);
        sdpMessageV2.setSequence(-1);
        sdpMessageV2.setSdpPayload("test"::getBytes);
        sdpMessageV2.setTargetDomain(new CrossChainDomain(catChainDotComDomain));
        sdpMessageV2.setTargetIdentity(new CrossChainIdentity(DigestUtil.sha256("receiver".getBytes())));

        authMessageV2.setTrustLevel(AuthMessageTrustLevelEnum.NEGATIVE_TRUST);
        authMessageV2.setIdentity(new CrossChainIdentity(DigestUtil.sha256("sender".getBytes())));
        authMessageV2.setUpperProtocol(UpperProtocolTypeBeyondAMEnum.SDP.getCode());
        authMessageV2.setPayload(sdpMessageV2.encode());

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
        Path dumpFile = Paths.get("src/test/resources/bins/dump.rdb");
        if (Files.exists(dumpFile)) {
            Files.delete(dumpFile);
            System.out.println("try to delete redis dump file");
        }
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
