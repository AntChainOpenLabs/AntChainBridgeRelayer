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

package com.alipay.antchain.bridge.relayer.dal.repository.impl;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgTrustLevelEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.SDPMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.UniformCrosschainPacketStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgCommitResult;
import com.alipay.antchain.bridge.relayer.commons.model.SDPMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.UniformCrosschainPacketContext;
import com.alipay.antchain.bridge.relayer.dal.entities.*;
import com.alipay.antchain.bridge.relayer.dal.mapper.*;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import com.alipay.antchain.bridge.relayer.dal.utils.ConvertUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CrossChainMessageRepository implements ICrossChainMessageRepository {

    private static final String CCMSG_SESSION_LOCK = "CCMSG_SESSION_LOCK:";

    @Resource
    private UCPPoolMapper ucpPoolMapper;

    @Resource
    private AuthMsgPoolMapper authMsgPoolMapper;

    @Resource
    private SDPMsgPoolMapper sdpMsgPoolMapper;

    @Resource
    private AuthMsgArchiveMapper authMsgArchiveMapper;

    @Resource
    private SDPMsgArchiveMapper sdpMsgArchiveMapper;

    @Resource
    private RedissonClient redisson;

    @Transactional(rollbackFor = AntChainBridgeRelayerException.class)
    @Override
    public long putAuthMessageWithIdReturned(AuthMsgWrapper authMsgWrapper) {
        try {
            authMsgPoolMapper.saveAuthMessages(ListUtil.toList(authMsgWrapper));
            return authMsgPoolMapper.lastInsertId();
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    String.format(
                            "failed to put am (ucp_id: %s) for chain %s",
                            authMsgWrapper.getUcpIdHex(), authMsgWrapper.getDomain()
                    ), e
            );
        }
    }

    @Override
    public int putAuthMessages(List<AuthMsgWrapper> authMsgWrappers) {
        try {
            return authMsgPoolMapper.saveAuthMessages(authMsgWrappers);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    String.format(
                            "failed to put multiple am for chain %s",
                            authMsgWrappers.isEmpty() ? "empty list" : authMsgWrappers.get(0).getDomain()
                    ), e
            );
        }
    }

    @Override
    public void putSDPMessage(SDPMsgWrapper sdpMsgWrapper) {
        try {
            sdpMsgPoolMapper.saveSDPMessages(ListUtil.toList(sdpMsgWrapper));
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    String.format(
                            "failed to put sdp msg (auth_msg_id: %d, from: %s, to: %s)",
                            sdpMsgWrapper.getAuthMsgWrapper().getAuthMsgId(),
                            sdpMsgWrapper.getSenderBlockchainDomain(),
                            sdpMsgWrapper.getReceiverBlockchainDomain()
                    ), e
            );
        }
    }

    public List<AuthMsgWrapper> peekAuthMessages(String domain, int limit, int failLimit) {
        try {
            List<AuthMsgPoolEntity> entities = authMsgPoolMapper.selectList(
                    new LambdaQueryWrapper<AuthMsgPoolEntity>()
                            .eq(AuthMsgPoolEntity::getDomain, domain)
                            .and(
                                    wrapper -> wrapper.eq(AuthMsgPoolEntity::getTrustLevel, AuthMsgTrustLevelEnum.NEGATIVE_TRUST)
                                            .eq(AuthMsgPoolEntity::getProcessState, AuthMsgProcessStateEnum.PROVED)
                                            .or(
                                                    wrapper1 -> wrapper1.eq(AuthMsgPoolEntity::getTrustLevel, AuthMsgTrustLevelEnum.POSITIVE_TRUST)
                                                            .eq(AuthMsgPoolEntity::getProcessState, AuthMsgProcessStateEnum.PENDING)
                                            ).or(
                                                    wrapper1 -> wrapper1.eq(AuthMsgPoolEntity::getTrustLevel, AuthMsgTrustLevelEnum.ZERO_TRUST)
                                                            .eq(AuthMsgPoolEntity::getProcessState, AuthMsgProcessStateEnum.PROVED)
                                            )
                            ).lt(AuthMsgPoolEntity::getFailCount, failLimit)
                            .last("limit " + limit)
            );
            if (ObjectUtil.isEmpty(entities)) {
                return ListUtil.empty();
            }

            return entities.stream()
                    .map(ConvertUtil::convertFromAuthMsgPoolEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    String.format("failed to peek auth messages for chain %s", domain),
                    e
            );
        }

    }

    @Override
    public List<AuthMsgWrapper> peekNotReadyAuthMessages(String domain, int limit) {
        try {
            List<AuthMsgPoolEntity> entities = authMsgPoolMapper.selectList(
                    new LambdaQueryWrapper<AuthMsgPoolEntity>()
                            .eq(AuthMsgPoolEntity::getDomain, domain)
                            .eq(AuthMsgPoolEntity::getProcessState, AuthMsgProcessStateEnum.NOT_READY)
                            .last("limit " + limit)
            );
            if (ObjectUtil.isEmpty(entities)) {
                return ListUtil.empty();
            }
            return entities.stream()
                    .map(ConvertUtil::convertFromAuthMsgPoolEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    String.format("failed to peek auth messages for chain %s", domain),
                    e
            );
        }
    }

    @Override
    public boolean hasNotReadyAuthMessages(String domain) {
        return authMsgPoolMapper.exists(
                new LambdaQueryWrapper<AuthMsgPoolEntity>()
                        .eq(AuthMsgPoolEntity::getDomain, domain)
                        .eq(AuthMsgPoolEntity::getProcessState, AuthMsgProcessStateEnum.NOT_READY)
        );
    }

    @Override
    public void putUniformCrosschainPacket(UniformCrosschainPacketContext context) {
        try {
            ucpPoolMapper.saveUCPMessages(ListUtil.toList(context));
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    String.format(
                            "failed to put ucp (id: %s) for chain %s",
                            HexUtil.encodeHexStr(context.getUcpId()), context.getSrcDomain()
                    ), e
            );
        }
    }

    @Override
    public int putUniformCrosschainPackets(List<UniformCrosschainPacketContext> contexts) {
        try {
            return ucpPoolMapper.saveUCPMessages(contexts);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    e,
                    "failed to put multiple ucp on block {} for chain {}",
                    contexts.get(0).getBlockHash(),
                    contexts.get(0).getBlockchainId()
            );
        }
    }

    @Override
    public UniformCrosschainPacketContext getUniformCrosschainPacket(String ucpId, boolean lock) {
        try {
            UCPPoolEntity entity = lock ?
                    ucpPoolMapper.selectOne(
                            new LambdaQueryWrapper<UCPPoolEntity>()
                                    .eq(UCPPoolEntity::getUcpId, ucpId)
                                    .last("for update")
                    ) :
                    ucpPoolMapper.selectOne(
                            new LambdaQueryWrapper<UCPPoolEntity>()
                                    .eq(UCPPoolEntity::getUcpId, ucpId)
                    );
            return ConvertUtil.convertFromUCPPoolEntity(entity);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    String.format("failed to get ucp (id: %s)", HexUtil.encodeHexStr(ucpId)),
                    e
            );
        }
    }

    @Override
    public boolean updateUniformCrosschainPacketState(String ucpId, UniformCrosschainPacketStateEnum state) {
        try {
            return ucpPoolMapper.update(
                    UCPPoolEntity.builder()
                            .processState(state)
                            .build(),
                    new LambdaUpdateWrapper<UCPPoolEntity>()
                            .eq(UCPPoolEntity::getUcpId, ucpId)
            ) == 1;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    e,
                    "failed to update state of UCP message (ucp_id: {}) to {}",
                    ucpId, state.getCode()
            );
        }
    }

    @Override
    public boolean updateAuthMessage(AuthMsgWrapper authMsgWrapper) {
        try {
            return authMsgPoolMapper.updateById(ConvertUtil.convertFromAuthMsgWrapper(authMsgWrapper)) == 1;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    String.format(
                            "failed to get auth message (ucp_id: %s, auth_msg_id: %d) from chain %s",
                            HexUtil.encodeHexStr(authMsgWrapper.getUcpId()), authMsgWrapper.getAuthMsgId(), authMsgWrapper.getDomain()
                    ),
                    e
            );
        }
    }

    @Override
    public boolean updateAuthMessageState(String ucpId, AuthMsgProcessStateEnum state) {
        try {
            return authMsgPoolMapper.update(
                    AuthMsgPoolEntity.builder()
                            .processState(state)
                            .build(),
                    new LambdaUpdateWrapper<AuthMsgPoolEntity>()
                            .eq(AuthMsgPoolEntity::getUcpId, ucpId)
            ) == 1;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    e,
                    "failed to update state of auth message (ucp_id: {}) to {}",
                    ucpId, state.getCode()
            );
        }
    }

    @Override
    public AuthMsgProcessStateEnum getAuthMessageState(String ucpId) {
        try {
            AuthMsgPoolEntity entity = authMsgPoolMapper.selectOne(
                    new LambdaQueryWrapper<AuthMsgPoolEntity>()
                            .select(ListUtil.toList(AuthMsgPoolEntity::getProcessState))
                            .eq(AuthMsgPoolEntity::getUcpId, ucpId)
            );
            return entity.getProcessState();
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    e,
                    "failed to get state of auth message (ucp_id: {})",
                    ucpId
            );
        }
    }

    @Override
    public boolean updateSDPMessage(SDPMsgWrapper sdpMsgWrapper) {
        try {
            return sdpMsgPoolMapper.updateById(ConvertUtil.convertFromSDPMsgWrapper(sdpMsgWrapper)) == 1;
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    String.format(
                            "failed to get auth message (auth_msg_id: %d) from chain %s to chain %s",
                            sdpMsgWrapper.getAuthMsgWrapper().getAuthMsgId(),
                            sdpMsgWrapper.getSenderBlockchainDomain(),
                            sdpMsgWrapper.getReceiverBlockchainDomain()
                    ), e
            );
        }
    }

    @Override
    public List<Integer> updateSDPMessageResults(List<SDPMsgCommitResult> results) {
        try {
            return results.stream()
                    .map(
                            result -> {
                                try {
                                    SDPMsgPoolEntity entity = new SDPMsgPoolEntity();
                                    entity.setTxSuccess(result.isCommitSuccess());
                                    entity.setTxFailReason(result.getFailReasonTruncated());
                                    entity.setProcessState(result.getProcessState());
                                    entity.setTxHash(result.getTxHash());
                                    if (ObjectUtil.isNotNull(result.getSdpMsgId())) {
                                        return sdpMsgPoolMapper.update(
                                                entity,
                                                new LambdaUpdateWrapper<SDPMsgPoolEntity>()
                                                        .eq(SDPMsgPoolEntity::getId, result.getSdpMsgId())
                                        );
                                    }
                                    return sdpMsgPoolMapper.update(
                                            entity,
                                            new LambdaUpdateWrapper<SDPMsgPoolEntity>()
                                                    .eq(SDPMsgPoolEntity::getTxHash, result.getTxHash())
                                    );
                                } catch (Exception e) {
                                    throw new RuntimeException(
                                            StrUtil.format(
                                                    "failed to update sdp message with txhash {} from ( product: {} , blockchain_id: {} )",
                                                    result.getTxHash(), result.getReceiveProduct(), result.getReceiveBlockchainId(),
                                                    e
                                            )
                                    );
                                }
                            }
                    ).collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    "failed to save sdp message commit results", e
            );
        }
    }

    @Override
    public AuthMsgWrapper getAuthMessage(long authMsgId) {
        return getAuthMessage(authMsgId, false);
    }

    @Override
    public String getUcpId(long authMsgId) {
        try {
            AuthMsgPoolEntity entity =
                    this.authMsgPoolMapper.selectOne(
                            new LambdaQueryWrapper<AuthMsgPoolEntity>()
                                    .select(ListUtil.toList(AuthMsgPoolEntity::getUcpId))
                                    .eq(BaseEntity::getId, authMsgId)
                    );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }
            return entity.getUcpId();
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    StrUtil.format("failed to get ucp id for {}", authMsgId),
                    e
            );
        }
    }

    @Override
    public AuthMsgWrapper getAuthMessage(long authMsgId, boolean lock) {
        try {
            AuthMsgPoolEntity entity = lock ?
                    this.authMsgPoolMapper.selectOne(
                            new LambdaQueryWrapper<AuthMsgPoolEntity>()
                                    .eq(BaseEntity::getId, authMsgId)
                                    .last("for update")
                    ) :
                    this.authMsgPoolMapper.selectById(authMsgId);
            if (ObjectUtil.isNull(entity)) {
                return null;
            }
            return ConvertUtil.convertFromAuthMsgPoolEntity(entity);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    StrUtil.format("failed to get am message {} with lock {}", authMsgId, lock),
                    e
            );
        }
    }

    @Override
    public SDPMsgWrapper getSDPMessage(long id, boolean lock) {
        try {
            SDPMsgPoolEntity entity = lock ?
                    this.sdpMsgPoolMapper.selectOne(
                            new LambdaQueryWrapper<SDPMsgPoolEntity>()
                                    .eq(BaseEntity::getId, id)
                                    .last("for update")
                    ) :
                    this.sdpMsgPoolMapper.selectById(id);
            if (ObjectUtil.isNull(entity)) {
                return null;
            }
            return ConvertUtil.convertFromSDPMsgPoolEntity(entity);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    StrUtil.format("failed to get sdp message {} with lock {}", id, lock),
                    e
            );
        }
    }

    @Override
    public SDPMsgWrapper getSDPMessage(String txHash) {
        try {
            SDPMsgPoolEntity entity = this.sdpMsgPoolMapper.selectOne(
                    new LambdaQueryWrapper<SDPMsgPoolEntity>()
                            .eq(SDPMsgPoolEntity::getTxHash, txHash)
            );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }
            return ConvertUtil.convertFromSDPMsgPoolEntity(entity);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    StrUtil.format("failed to get sdp message with txhash {}", txHash),
                    e
            );
        }
    }

    @Override
    public List<UniformCrosschainPacketContext> peekUCPMessages(String domain, UniformCrosschainPacketStateEnum processState, int limit) {
        try {
            List<UCPPoolEntity> entities = ucpPoolMapper.selectList(
                    new LambdaQueryWrapper<UCPPoolEntity>()
                            .eq(UCPPoolEntity::getSrcDomain, domain)
                            .eq(UCPPoolEntity::getProcessState, processState)
                            .last("limit " + limit)
            );
            if (ObjectUtil.isEmpty(entities)) {
                return ListUtil.empty();
            }

            return entities.stream()
                    .map(ConvertUtil::convertFromUCPPoolEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    e,
                    "failed to peek {} UCP messages for chain {}",
                    processState.getCode(), domain
            );
        }
    }

    @Override
    @Transactional
    public SDPMsgWrapper querySDPMessage(String ucpId) {
        try {
            Long amId = getAuthMsgId(ucpId);
            if (ObjectUtil.isNull(amId)) {
                return null;
            }

            return getSDPMessageByAmId(amId);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    e,
                    "failed to get sdp message for chain for ucp id {}",
                    ucpId
            );
        }
    }

    private Long getAuthMsgId(String ucpId) {
        AuthMsgPoolEntity entity = authMsgPoolMapper.selectOne(
                new LambdaQueryWrapper<AuthMsgPoolEntity>()
                        .select(ListUtil.toList(AuthMsgPoolEntity::getId))
                        .eq(AuthMsgPoolEntity::getUcpId, ucpId)
        );
        if (ObjectUtil.isNotNull(entity)) {
            return entity.getId();
        }

        AuthMsgArchiveEntity authMsgArchiveEntity = authMsgArchiveMapper.selectOne(
                new LambdaQueryWrapper<AuthMsgArchiveEntity>()
                        .select(ListUtil.toList(AuthMsgArchiveEntity::getId))
                        .eq(AuthMsgArchiveEntity::getUcpId, ucpId)
        );
        if (ObjectUtil.isNull(authMsgArchiveEntity)) {
            return null;
        }

        return authMsgArchiveEntity.getId();
    }

    private SDPMsgWrapper getSDPMessageByAmId(Long amId) {
        SDPMsgPoolEntity sdpMsgPoolEntity = sdpMsgPoolMapper.selectOne(
                new LambdaQueryWrapper<SDPMsgPoolEntity>()
                        .select(ListUtil.toList(
                                SDPMsgPoolEntity::getTxHash,
                                SDPMsgPoolEntity::getProcessState,
                                SDPMsgPoolEntity::getTxSuccess,
                                SDPMsgPoolEntity::getTxFailReason
                        )).eq(SDPMsgPoolEntity::getAuthMsgId, amId)
        );
        if (ObjectUtil.isNotNull(sdpMsgPoolEntity)) {
            return ConvertUtil.convertFromSDPMsgPoolEntity(sdpMsgPoolEntity);
        }

        SDPMsgArchiveEntity sdpMsgArchiveEntity = sdpMsgArchiveMapper.selectOne(
                new LambdaQueryWrapper<SDPMsgArchiveEntity>()
                        .select(ListUtil.toList(
                                SDPMsgArchiveEntity::getTxHash,
                                SDPMsgArchiveEntity::getProcessState,
                                SDPMsgArchiveEntity::getTxSuccess,
                                SDPMsgArchiveEntity::getTxFailReason
                        )).eq(SDPMsgArchiveEntity::getAuthMsgId, amId)
        );
        if (ObjectUtil.isNull(sdpMsgArchiveEntity)) {
            return null;
        }
        return ConvertUtil.convertFromSDPMsgPoolEntity(BeanUtil.copyProperties(sdpMsgArchiveEntity, SDPMsgPoolEntity.class));
    }

    @Override
    public List<SDPMsgWrapper> peekSDPMessages(String receiverBlockchainProduct, String receiverBlockchainId, SDPMsgProcessStateEnum processState, int limit) {
        try {
            List<SDPMsgPoolEntity> entities = sdpMsgPoolMapper.selectList(
                    new LambdaQueryWrapper<SDPMsgPoolEntity>()
                            .eq(SDPMsgPoolEntity::getReceiverBlockchainProduct, receiverBlockchainProduct)
                            .eq(SDPMsgPoolEntity::getReceiverBlockchainId, receiverBlockchainId)
                            .eq(SDPMsgPoolEntity::getProcessState, processState)
                            .last("limit " + limit)
            );
            if (ObjectUtil.isEmpty(entities)) {
                return ListUtil.empty();
            }

            return entities.stream()
                    .map(ConvertUtil::convertFromSDPMsgPoolEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    StrUtil.format(
                            "failed to peek {} sdp messages for chain (product: {}, blockchain_id: {})",
                            processState.getCode(), receiverBlockchainProduct, receiverBlockchainId
                    ), e
            );
        }
    }

    @Override
    public List<SDPMsgWrapper> peekSDPMessagesSent(String senderBlockchainProduct, String senderBlockchainId, SDPMsgProcessStateEnum processState, int limit) {
        try {
            List<SDPMsgPoolEntity> entities = sdpMsgPoolMapper.selectList(
                    new LambdaQueryWrapper<SDPMsgPoolEntity>()
                            .eq(SDPMsgPoolEntity::getSenderBlockchainProduct, senderBlockchainProduct)
                            .eq(SDPMsgPoolEntity::getSenderBlockchainId, senderBlockchainId)
                            .eq(SDPMsgPoolEntity::getProcessState, processState)
                            .last("limit " + limit)
            );
            if (ObjectUtil.isEmpty(entities)) {
                return ListUtil.empty();
            }

            return entities.stream()
                    .map(ConvertUtil::convertFromSDPMsgPoolEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    StrUtil.format(
                            "failed to peek {} sdp messages sent to network for chain (product: {}, blockchain_id: {})",
                            processState.getCode(), senderBlockchainProduct, senderBlockchainId
                    ), e
            );
        }
    }

    @Override
    public List<SDPMsgWrapper> peekTxFinishedSDPMessageIds(String receiverBlockchainProduct, String receiverBlockchainId, int limit) {
        try {
            List<SDPMsgPoolEntity> entities = sdpMsgPoolMapper.selectList(
                    new LambdaQueryWrapper<SDPMsgPoolEntity>()
                            .select(ListUtil.toList(BaseEntity::getId, SDPMsgPoolEntity::getAuthMsgId))
                            .eq(SDPMsgPoolEntity::getReceiverBlockchainProduct, receiverBlockchainProduct)
                            .eq(SDPMsgPoolEntity::getReceiverBlockchainId, receiverBlockchainId)
                            .and(
                                    wrapper -> wrapper.eq(SDPMsgPoolEntity::getProcessState, SDPMsgProcessStateEnum.TX_SUCCESS)
                                            .or(
                                                    wrapper1 -> wrapper1.eq(SDPMsgPoolEntity::getProcessState, SDPMsgProcessStateEnum.TX_FAILED)
                                            )
                            ).last("limit " + limit)
            );
            if (ObjectUtil.isEmpty(entities)) {
                return ListUtil.empty();
            }

            return entities.stream()
                    .map(sdpMsgPoolEntity -> BeanUtil.copyProperties(sdpMsgPoolEntity, SDPMsgWrapper.class))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    StrUtil.format(
                            "failed to peek tx_pending sdp messages ids for chain (product: {}, blockchain_id: {})",
                            receiverBlockchainProduct, receiverBlockchainId
                    ), e
            );
        }
    }

    @Override
    public long countSDPMessagesByState(String receiverBlockchainProduct, String receiverBlockchainId, SDPMsgProcessStateEnum processState) {
        try {
            return this.sdpMsgPoolMapper.selectCount(
                    new LambdaQueryWrapper<SDPMsgPoolEntity>()
                            .eq(SDPMsgPoolEntity::getReceiverBlockchainProduct, receiverBlockchainProduct)
                            .eq(SDPMsgPoolEntity::getReceiverBlockchainId, receiverBlockchainId)
                            .eq(SDPMsgPoolEntity::getProcessState, processState)
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    StrUtil.format(
                            "failed to count {} sdp messages for chain (product: {}, blockchain_id: {})",
                            processState.getCode(), receiverBlockchainProduct, receiverBlockchainId
                    ), e
            );
        }
    }

    @Override
    public int archiveAuthMessages(List<Long> authMsgIds) {
        try {
            return this.authMsgPoolMapper.archiveAuthMessages(authMsgIds);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    StrUtil.format(
                            "failed to archive am messages with ids {}",
                            authMsgIds
                    ), e
            );
        }
    }

    @Override
    public int archiveSDPMessages(List<Long> ids) {
        try {
            return this.sdpMsgPoolMapper.archiveSDPMessages(ids);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    StrUtil.format(
                            "failed to archive sdp messages with ids {}",
                            ids
                    ), e
            );
        }
    }

    @Override
    public int deleteAuthMessages(List<Long> authMsgIds) {
        try {
            return this.authMsgPoolMapper.deleteBatchIds(authMsgIds);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    StrUtil.format(
                            "failed to delete am messages from pool with ids {}",
                            authMsgIds
                    ), e
            );
        }
    }

    @Override
    public int deleteSDPMessages(List<Long> ids) {
        try {
            return this.sdpMsgPoolMapper.deleteBatchIds(ids);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ERROR,
                    StrUtil.format(
                            "failed to delete sdp messages from pool with ids {}",
                            ids
                    ), e
            );
        }
    }

    @Override
    public Lock getSessionLock(String session) {
        return redisson.getLock(getCCMsgSessionLock(session));
    }

    private String getCCMsgSessionLock(String session) {
        return String.format("%s%s", CCMSG_SESSION_LOCK, session);
    }
}
