/*
 * Alipay.com Inc.
 * Copyright (c) 2004-2022 All Rights Reserved.
 */
package com.alipay.antchain.bridge.relayer.bootstrap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = AntChainBridgeRelayerApplication.class)
@Sql(scripts = {"classpath:data/ddl.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:data/drop_all.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public abstract class TestBase {

    @BeforeClass
    public static void before() throws Exception {

    }

    @AfterClass
    public static void after() throws Exception {

    }

    public void test() throws Exception {
//        blockchainService.getBaseMapper().insertBlockchain("test", "test", "test", "test", new byte[]{1, 2, 3});
//
//        System.out.println(blockchainService.list().get(0).getGmtModified());
//
//        Thread.sleep(2_000);
//
////        blockchainService.lambdaUpdate()
////                .set(BlockchainEntity::getAlias, "")
////                .set(BlockchainEntity::getDesc, "")
////                .eq(BlockchainEntity::getProduct, "test")
////                .eq(BlockchainEntity::getBlockchainId, "test")
////                .update();
//
//        blockchainService.getBaseMapper().update(
//                BlockchainEntity.builder()
//                        .desc("test123")
//                        .build(),
//                new LambdaQueryWrapper<BlockchainEntity>()
//                        .eq(BlockchainEntity::getBlockchainId, "test")
//        );
//
//        System.out.println(blockchainService.list().get(0).getGmtModified());
//
//        JSON.parseObject(new byte[]{}, BlockchainMeta.class);
//
//        System.out.println(blockchainService.count());

//        pluginServerObjectsMapper.insertPluginServer(
//                PluginServerObjectsEntity.builder()
//                        .state(PluginServerStateEnum.NOT_FOUND)
//                        .psId("test")
//                        .address("test")
//                        .build()
//        );
//
//        List<PluginServerObjectsEntity> entity = pluginServerObjectsMapper.selectList(null);
    }

}
