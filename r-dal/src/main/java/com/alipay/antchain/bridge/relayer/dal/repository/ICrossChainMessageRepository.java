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

package com.alipay.antchain.bridge.relayer.dal.repository;

import java.util.List;
import java.util.concurrent.locks.Lock;

import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.SDPMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgCommitResult;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.UniformCrosschainPacketContext;

public interface ICrossChainMessageRepository {

    void putUniformCrosschainPacket(UniformCrosschainPacketContext context);

    UniformCrosschainPacketContext getUniformCrosschainPacket(byte[] ucpId);

    long putAuthMessageWithIdReturned(AuthMsgWrapper authMsgWrapper);

    int putAuthMessages(List<AuthMsgWrapper> authMsgWrappers);

    void putSDPMessage(SDPMsgWrapper sdpMsgWrapper);

    boolean updateAuthMessage(AuthMsgWrapper authMsgWrapper);

    boolean updateSDPMessage(SDPMsgWrapper sdpMsgWrapper);

    List<Integer> updateSDPMessageResults(List<SDPMsgCommitResult> results);

    AuthMsgWrapper getAuthMessage(long authMsgId);

    AuthMsgWrapper getAuthMessage(long authMsgId, boolean lock);

    SDPMsgWrapper getSDPMessage(long id, boolean lock);

    SDPMsgWrapper getSDPMessage(String txHash);

    List<AuthMsgWrapper> peekAuthMessages(String domain, AuthMsgProcessStateEnum processState, int limit);

    List<SDPMsgWrapper> peekSDPMessages(String receiverBlockchainProduct, String receiverBlockchainId, SDPMsgProcessStateEnum processState, int limit);

    List<SDPMsgWrapper> peekTxPendingSDPMessageIds(String receiverBlockchainProduct, String receiverBlockchainId, int limit);

    long countSDPMessagesByState(String receiverBlockchainProduct, String receiverBlockchainId, SDPMsgProcessStateEnum processState);

    int archiveAuthMessages(List<Long> authMsgIds);

    int archiveSDPMessages(List<Long> ids);

    int deleteAuthMessages(List<Long> authMsgIds);

    int deleteSDPMessages(List<Long> ids);

    Lock getSessionLock(String session);
}
