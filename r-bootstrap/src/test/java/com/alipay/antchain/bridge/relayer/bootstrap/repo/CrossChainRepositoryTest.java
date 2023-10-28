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

package com.alipay.antchain.bridge.relayer.bootstrap.repo;

import java.util.List;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageTrustLevelEnum;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageV2;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainIdentity;
import com.alipay.antchain.bridge.commons.core.sdp.AbstractSDPMessage;
import com.alipay.antchain.bridge.commons.core.sdp.SDPMessageV2;
import com.alipay.antchain.bridge.relayer.bootstrap.TestBase;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.SDPMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.UpperProtocolTypeBeyondAMEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgWrapper;
import com.alipay.antchain.bridge.relayer.dal.entities.*;
import com.alipay.antchain.bridge.relayer.dal.mapper.AuthMsgArchiveMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.AuthMsgPoolMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.SDPMsgArchiveMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.SDPMsgPoolMapper;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.helpers.BasicMarkerFactory;

@Slf4j
public class CrossChainRepositoryTest extends TestBase {

    @BeforeClass
    public static void before() throws Exception {
        AuthMessageV2 authMessageV2 = new AuthMessageV2();
        authMessageV2.setIdentity(CrossChainIdentity.fromHexStr(DigestUtil.sha256Hex("01")));
        authMessageV2.setTrustLevel(AuthMessageTrustLevelEnum.POSITIVE_TRUST);
        authMessageV2.setUpperProtocol(UpperProtocolTypeBeyondAMEnum.SDP.ordinal());
        authMessageV2.setPayload("".getBytes());

        authMessage = authMessageV2;

        SDPMessageV2 sdpMessageV2 = new SDPMessageV2();
        sdpMessageV2.setAtomic(true);
        sdpMessageV2.setSdpPayload(new SDPMessageV2.SDPPayloadV2("".getBytes()));
        sdpMessageV2.setTargetDomain(new CrossChainDomain("dest"));
        sdpMessageV2.setSequence(100);
        sdpMessageV2.setTargetIdentity(CrossChainIdentity.fromHexStr(DigestUtil.sha256Hex("02")));

        sdpMessage = sdpMessageV2;
    }

    private static IAuthMessage authMessage;

    private static AbstractSDPMessage sdpMessage;

    private boolean ifAlreadyWriteAM = false;

    private boolean ifAlreadyWriteSDP = false;

    @Resource
    private ICrossChainMessageRepository crossChainMessageRepository;

    @Resource
    private AuthMsgArchiveMapper authMsgArchiveMapper;

    @Resource
    private AuthMsgPoolMapper authMsgPoolMapper;

    @Resource
    private SDPMsgPoolMapper sdpMsgPoolMapper;

    @Resource
    private SDPMsgArchiveMapper sdpMsgArchiveMapper;

    @Test
    public void testSaveAuthMessages() {
        saveElevenAM(getAMCurrentId());
    }

    @Test
    public void testPeekAuthMessages() {
        saveElevenAM(getAMCurrentId());

        List<AuthMsgWrapper> authMsgWrappers = crossChainMessageRepository.peekAuthMessages("test", AuthMsgProcessStateEnum.PENDING, 10);
        Assert.assertEquals(10, authMsgWrappers.size());
    }

    @Test
    public void testArchiveAuthMessages() {
        saveElevenAM(getAMCurrentId());

        Assert.assertEquals(
                3,
                crossChainMessageRepository.archiveAuthMessages(ListUtil.of(1L, 3L, 5L))
        );

        List<AuthMsgArchiveEntity> archiveEntities = authMsgArchiveMapper.selectBatchIds(ListUtil.of(1L, 3L, 5L));
        Assert.assertEquals(
                new Long(1L),
                archiveEntities.get(0).getId()
        );
        Assert.assertEquals(
                new Long(3L),
                archiveEntities.get(1).getId()
        );
    }

    @Test
    public void testPutSDPMessage() throws Exception {
        sdpMsgPoolMapper.selectList(null);

        saveSomeSDP();

        System.out.println(sdpMsgPoolMapper.update(new SDPMsgPoolEntity(), new LambdaUpdateWrapper<SDPMsgPoolEntity>().eq(BaseEntity::getId, 100)));


    }

    @Test
    public void testArchiveSDPMessages() {
        saveSomeSDP();

        Assert.assertEquals(
                3,
                crossChainMessageRepository.archiveSDPMessages(ListUtil.of(1L, 3L, 5L))
        );

        List<SDPMsgArchiveEntity> archiveEntities = sdpMsgArchiveMapper.selectBatchIds(ListUtil.of(1L, 3L, 5L));
        Assert.assertEquals(
                new Long(1L),
                archiveEntities.get(0).getId()
        );
        Assert.assertEquals(
                new Long(3L),
                archiveEntities.get(1).getId()
        );
    }

    @Test
    public void testUpdateSDPMessage() {
        saveSomeSDP();
        String txHash = HexUtil.encodeHexStr(RandomUtil.randomBytes(32));
        Assert.assertTrue(
                crossChainMessageRepository.updateSDPMessage(
                        new SDPMsgWrapper(
                                10,
                                new AuthMsgWrapper(
                                        10,
                                        "test",
                                        "test",
                                        "test",
                                        ByteUtil.intToBytes(9),
                                        "am",
                                        AuthMsgProcessStateEnum.PENDING,
                                        authMessage
                                ),
                                "eth",
                                "ethid",
                                null,
                                SDPMsgProcessStateEnum.TX_SUCCESS,
                                txHash,
                                true,
                                "",
                                sdpMessage
                        )
                )
        );

        SDPMsgWrapper sdpMsgWrapper = crossChainMessageRepository.getSDPMessage(10, false);
        Assert.assertNotNull(sdpMsgWrapper.getReceiverAMClientContract());

        Assert.assertEquals(
                txHash,
                sdpMsgWrapper.getTxHash()
        );
        Assert.assertEquals(
                SDPMsgProcessStateEnum.TX_SUCCESS,
                sdpMsgWrapper.getProcessState()
        );
        Assert.assertTrue(sdpMsgWrapper.isTxSuccess());
    }

    private long getAMCurrentId() {
        AuthMsgPoolEntity entity = new AuthMsgPoolEntity();
        entity.setId(0L);
        return ObjectUtil.defaultIfNull(
                authMsgPoolMapper.selectOne(new QueryWrapper<AuthMsgPoolEntity>().select("max(id) as id")),
                entity
        ).getId();
    }

    private void saveElevenAM(long startId) {
        if (ifAlreadyWriteAM) {
            return;
        }

        for (int i = 0; i < 11; i++) {
            Assert.assertEquals(
                    startId + i + 1,
                    crossChainMessageRepository.putAuthMessageWithIdReturned(
                            new AuthMsgWrapper(
                                    "test",
                                    "test",
                                    "test",
                                    ByteUtil.intToBytes(i),
                                    "am",
                                    AuthMsgProcessStateEnum.PENDING,
                                    authMessage
                            )
                    )
            );
        }

        ifAlreadyWriteAM = true;
    }

    private long getSDPCurrentId() {
        SDPMsgPoolEntity entity = new SDPMsgPoolEntity();
        entity.setId(0L);
        return ObjectUtil.defaultIfNull(
                sdpMsgPoolMapper.selectOne(new QueryWrapper<SDPMsgPoolEntity>().select("max(id) as id")),
                entity
        ).getId();
    }

    private void saveSomeSDP() {
        if (ifAlreadyWriteSDP) {
            return;
        }

        for (int i = 0; i < 11; i++) {
            crossChainMessageRepository.putSDPMessage(
                    new SDPMsgWrapper(
                            "eth",
                            "ethid",
                            "am",
                            SDPMsgProcessStateEnum.PENDING,
                            "",
                            false,
                            "",
                            new AuthMsgWrapper(
                                    i + 1,
                                    "test",
                                    "test",
                                    "test",
                                    ByteUtil.intToBytes(i),
                                    "am",
                                    AuthMsgProcessStateEnum.PENDING,
                                    authMessage
                            ),
                            sdpMessage
                    )
            );
        }

        ifAlreadyWriteSDP = true;
    }
}
