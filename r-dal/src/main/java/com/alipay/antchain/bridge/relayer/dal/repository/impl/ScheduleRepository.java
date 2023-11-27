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

import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.DTActiveNodeStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.ActiveNode;
import com.alipay.antchain.bridge.relayer.commons.model.BizDistributedTask;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainDistributedTask;
import com.alipay.antchain.bridge.relayer.commons.model.IDistributedTask;
import com.alipay.antchain.bridge.relayer.dal.entities.BizDTTaskEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.DTActiveNodeEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.BlockchainDTTaskEntity;
import com.alipay.antchain.bridge.relayer.dal.mapper.BizDTTaskMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.DTActiveNodeMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.BlockchainDTTaskMapper;
import com.alipay.antchain.bridge.relayer.dal.repository.IScheduleRepository;
import com.alipay.antchain.bridge.relayer.dal.utils.ConvertUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.Synchronized;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ScheduleRepository implements IScheduleRepository {

    private static final String SCHEDULE_LOCK_KEY = "RELAYER_SCHEDULE_LOCK";

    @Resource
    private RedissonClient redisson;

    @Resource
    private DTActiveNodeMapper dtActiveNodeMapper;

    @Resource
    private BlockchainDTTaskMapper blockchainDtTaskMapper;

    @Resource
    private BizDTTaskMapper bizDTTaskMapper;

    @Override
    public Lock getDispatchLock() {
        return redisson.getLock(SCHEDULE_LOCK_KEY);
    }

    @Override
    @Synchronized
    public void activate(String nodeId, String nodeIp) {
        try {
            if (
                    1 != dtActiveNodeMapper.update(
                            DTActiveNodeEntity.builder()
                                    .nodeId(nodeId)
                                    .nodeIp(nodeIp)
                                    .state(DTActiveNodeStateEnum.ONLINE)
                                    .build(),
                            new LambdaUpdateWrapper<DTActiveNodeEntity>()
                                    .eq(DTActiveNodeEntity::getNodeId, nodeId)
                    )
            ) {
                dtActiveNodeMapper.insert(
                        DTActiveNodeEntity.builder()
                                .nodeId(nodeId)
                                .nodeIp(nodeIp)
                                .state(DTActiveNodeStateEnum.ONLINE)
                                .build()
                );
            }
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    StrUtil.format("failed to activate node ( id: {}, ip: {} )", nodeId, nodeIp),
                    e
            );
        }
    }

    @Override
    public List<BlockchainDistributedTask> getAllBlockchainDistributedTasks() {
        try {
            return blockchainDtTaskMapper.selectList(null).stream()
                    .map(ConvertUtil::convertFromBlockchainDTTaskEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_TASK_ERROR,
                    "failed to get all distributed tasks",
                    e
            );
        }
    }

    @Override
    public List<BlockchainDistributedTask> getBlockchainDistributedTasksByNodeId(String nodeId) {
        try {
            return blockchainDtTaskMapper.selectList(
                            new LambdaQueryWrapper<BlockchainDTTaskEntity>()
                                    .eq(BlockchainDTTaskEntity::getNodeId, nodeId)
                    ).stream()
                    .map(ConvertUtil::convertFromBlockchainDTTaskEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_TASK_ERROR,
                    "failed to get distributed tasks for node " + nodeId,
                    e
            );
        }
    }

    @Override
    public List<ActiveNode> getAllActiveNodes() {
        try {
            return dtActiveNodeMapper.selectList(null).stream()
                    .map(ConvertUtil::convertFromDTActiveNodeEntityActiveNode)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    "failed to get all active nodes",
                    e
            );
        }
    }

    @Override
    public void batchInsertBlockchainDTTasks(List<BlockchainDistributedTask> tasks) {
        try {
            blockchainDtTaskMapper.saveDTTasks(tasks);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    StrUtil.format(
                            "failed to save distributed tasks {}",
                            tasks.stream().map(BlockchainDistributedTask::getUniqueTaskKey)
                    ),
                    e
            );
        }
    }

    @Override
    @Transactional
    public void batchUpdateBlockchainDTTasks(List<BlockchainDistributedTask> tasks) {
        try {
            tasks.forEach(
                    task -> blockchainDtTaskMapper.update(
                            BlockchainDTTaskEntity.builder()
                                    .nodeId(task.getNodeId())
                                    .timeSlice(new Date(task.getStartTime()))
                                    .build(),
                            new LambdaUpdateWrapper<BlockchainDTTaskEntity>()
                                    .eq(BlockchainDTTaskEntity::getTaskType, task.getTaskType())
                                    .eq(BlockchainDTTaskEntity::getProduct, task.getBlockchainProduct())
                                    .eq(BlockchainDTTaskEntity::getBlockchainId, task.getBlockchainId())
                    )
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    StrUtil.format(
                            "failed to save distributed tasks {}",
                            tasks.stream().map(BlockchainDistributedTask::getUniqueTaskKey)
                    ),
                    e
            );
        }
    }

    @Override
    public List<BizDistributedTask> getAllBizDistributedTasks() {
        try {
            return bizDTTaskMapper.selectList(null).stream()
                    .map(ConvertUtil::convertFromBizDTTaskEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_TASK_ERROR,
                    "failed to get all biz distributed tasks",
                    e
            );
        }
    }

    @Override
    public List<BizDistributedTask> getBizDistributedTasksByNodeId(String nodeId) {
        try {
            return bizDTTaskMapper.selectList(
                            new LambdaQueryWrapper<BizDTTaskEntity>()
                                    .eq(BizDTTaskEntity::getNodeId, nodeId)
                    ).stream()
                    .map(ConvertUtil::convertFromBizDTTaskEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_TASK_ERROR,
                    "failed to get biz distributed tasks for node " + nodeId,
                    e
            );
        }
    }

    @Override
    @Transactional
    public void batchInsertBizDTTasks(List<BizDistributedTask> tasks) {
        try {
            tasks.forEach(
                    bizDistributedTask -> bizDTTaskMapper.insert(
                            ConvertUtil.convertFromBizDistributedTask(
                                    bizDistributedTask
                            )
                    )
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    StrUtil.format(
                            "failed to save distributed tasks {}",
                            tasks.stream().map(IDistributedTask::getUniqueTaskKey)
                    ),
                    e
            );
        }
    }

    @Override
    @Transactional
    public void batchUpdateBizDTTasks(List<BizDistributedTask> tasks) {
        try {
            tasks.forEach(
                    task -> bizDTTaskMapper.update(
                            BizDTTaskEntity.builder()
                                    .nodeId(task.getNodeId())
                                    .timeSlice(new Date(task.getStartTime()))
                                    .build(),
                            new LambdaUpdateWrapper<BizDTTaskEntity>()
                                    .eq(BizDTTaskEntity::getTaskType, task.getTaskType())
                                    .eq(BizDTTaskEntity::getUniqueKey, task.getUniqueKey())
                    )
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    StrUtil.format(
                            "failed to save distributed tasks {}",
                            tasks.stream().map(IDistributedTask::getUniqueTaskKey)
                    ),
                    e
            );
        }
    }
}
