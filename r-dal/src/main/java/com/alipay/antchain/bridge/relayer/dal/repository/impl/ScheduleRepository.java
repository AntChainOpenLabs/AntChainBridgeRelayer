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
import com.alipay.antchain.bridge.relayer.commons.model.DistributedTask;
import com.alipay.antchain.bridge.relayer.dal.entities.DTActiveNodeEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.DTTaskEntity;
import com.alipay.antchain.bridge.relayer.dal.mapper.DTActiveNodeMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.DTTaskMapper;
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
    private RedissonClient redissonClient;

    @Resource
    private DTActiveNodeMapper dtActiveNodeMapper;

    @Resource
    private DTTaskMapper dtTaskMapper;

    @Override
    public Lock getDispatchLock() {
        return redissonClient.getLock(SCHEDULE_LOCK_KEY);
    }

    @Override
    @Synchronized
    public void activate(String nodeId, String nodeIp) {
        try {
            if (
                    1 != dtActiveNodeMapper.update(
                            DTActiveNodeEntity.builder().build(),
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
    public List<DistributedTask> getAllDistributedTasks() {
        try {
            return dtTaskMapper.selectList(null).stream()
                    .map(ConvertUtil::convertFromDTTaskEntity)
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
    public List<DistributedTask> getDistributedTasksByNodeId(String nodeId) {
        try {
            return dtTaskMapper.selectList(
                            new LambdaQueryWrapper<DTTaskEntity>()
                                    .eq(DTTaskEntity::getNodeId, nodeId)
                    ).stream()
                    .map(ConvertUtil::convertFromDTTaskEntity)
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
    public void batchInsertDTTasks(List<DistributedTask> tasks) {
        try {
            dtTaskMapper.saveDTTasks(tasks);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    StrUtil.format(
                            "failed to save distributed tasks {}",
                            tasks.stream().map(DistributedTask::getUniqueTaskKey)
                    ),
                    e
            );
        }
    }

    @Override
    @Transactional
    public void batchUpdateDTTasks(List<DistributedTask> tasks) {
        try {
            tasks.forEach(
                    task -> dtTaskMapper.update(
                            DTTaskEntity.builder()
                                    .nodeId(task.getNodeId())
                                    .timeSlice(new Date(task.getTimeSlice()))
                                    .build(),
                            new LambdaUpdateWrapper<DTTaskEntity>()
                                    .eq(DTTaskEntity::getTaskType, task.getTaskType())
                                    .eq(DTTaskEntity::getProduct, task.getBlockchainProduct())
                                    .eq(DTTaskEntity::getBlockchainId, task.getBlockchainId())
                    )
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    StrUtil.format(
                            "failed to save distributed tasks {}",
                            tasks.stream().map(DistributedTask::getUniqueTaskKey)
                    ),
                    e
            );
        }
    }
}
