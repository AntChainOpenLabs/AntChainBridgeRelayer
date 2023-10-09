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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.RelayerNodeSyncStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerHealthInfo;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNetwork;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import com.alipay.antchain.bridge.relayer.dal.entities.DTActiveNodeEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.RelayerNetworkEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.RelayerNodeEntity;
import com.alipay.antchain.bridge.relayer.dal.mapper.DTActiveNodeMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.RelayerNetworkMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.RelayerNodeMapper;
import com.alipay.antchain.bridge.relayer.dal.repository.IRelayerNetworkRepository;
import com.alipay.antchain.bridge.relayer.dal.utils.ConvertUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RelayerNetworkRepository implements IRelayerNetworkRepository {

    @Resource
    private RelayerNetworkMapper relayerNetworkMapper;

    @Resource
    private RelayerNodeMapper relayerNodeMapper;

    @Resource
    private DTActiveNodeMapper dtActiveNodeMapper;

    @Value("${os.grpc.port:8089}")
    private int grpcServerPort;

    @Override
    public void addNetworkItems(String networkId, Map<String, RelayerNetwork.Item> relayerNetworkItems) {
        try {
            relayerNetworkMapper.addNetworkItems(
                    relayerNetworkItems.entrySet().stream()
                            .map(entry -> ConvertUtil.convertFromRelayerNetworkItem(networkId, entry.getKey(), entry.getValue()))
                            .collect(Collectors.toList())
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_RELAYER_NETWORK_ERROR,
                    "failed to add network items for network " + networkId,
                    e
            );
        }
    }

    @Override
    public boolean deleteNetworkItem(String domain, String nodeId) {
        try {
            return 1 == relayerNetworkMapper.delete(
                    new LambdaQueryWrapper<RelayerNetworkEntity>()
                            .eq(RelayerNetworkEntity::getDomain, domain)
                            .eq(RelayerNetworkEntity::getNodeId, nodeId)
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_RELAYER_NETWORK_ERROR,
                    StrUtil.format("failed to delete network item ( domain: {}, node_id: {} )", domain, nodeId),
                    e
            );
        }
    }

    @Override
    public RelayerNetwork.Item getNetworkItem(String networkId, String domain, String nodeId) {
        try {
            return ConvertUtil.convertFromRelayerNetworkEntity(
                    relayerNetworkMapper.selectOne(
                            new LambdaQueryWrapper<RelayerNetworkEntity>()
                                    .eq(RelayerNetworkEntity::getNetworkId, networkId)
                                    .eq(RelayerNetworkEntity::getDomain, domain)
                                    .eq(RelayerNetworkEntity::getNodeId, nodeId)
                    )
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_RELAYER_NETWORK_ERROR,
                    StrUtil.format(
                            "failed to get network item ( network_id: {}, domain: {}, node_id: {} )",
                            networkId, domain, nodeId
                    ), e
            );
        }
    }

    @Override
    public void addNetworkItem(String networkId, String domain, String nodeId, RelayerNodeSyncStateEnum syncState) {
        try {
            relayerNetworkMapper.insert(
                    ConvertUtil.convertFromRelayerNetworkItem(
                            networkId,
                            domain,
                            new RelayerNetwork.Item(nodeId, syncState)
                    )
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_RELAYER_NETWORK_ERROR,
                    StrUtil.format(
                            "failed to add network item ( network_id: {}, domain: {}, node_id: {} )",
                            networkId, domain, nodeId
                    ), e
            );
        }
    }

    @Override
    public boolean updateNetworkItem(String networkId, String domain, String nodeId, RelayerNodeSyncStateEnum syncState) {
        try {
            RelayerNetworkEntity entity = new RelayerNetworkEntity();
            entity.setSyncState(syncState);
            return 1 == relayerNetworkMapper.update(
                    entity,
                    new LambdaQueryWrapper<RelayerNetworkEntity>()
                            .eq(RelayerNetworkEntity::getNetworkId, networkId)
                            .eq(RelayerNetworkEntity::getDomain, domain)
                            .eq(RelayerNetworkEntity::getNodeId, nodeId)
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_RELAYER_NETWORK_ERROR,
                    StrUtil.format(
                            "failed to update network item ( network_id: {}, domain: {}, node_id: {} ) to {}",
                            networkId, domain, nodeId, syncState.getCode()
                    ), e
            );
        }
    }

    @Override
    public Map<String, RelayerNetwork.Item> getNetworkItems(String networkId) {
        try {
            List<RelayerNetworkEntity> entities = relayerNetworkMapper.selectList(
                    new LambdaQueryWrapper<RelayerNetworkEntity>()
                            .eq(RelayerNetworkEntity::getNetworkId, networkId)
            );
            if (ObjectUtil.isEmpty(entities)) {
                return MapUtil.empty();
            }
            return entities.stream().collect(Collectors.toMap(
                    RelayerNetworkEntity::getDomain,
                    entity -> new RelayerNetwork.Item(entity.getNodeId(), entity.getSyncState())
            ));
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_RELAYER_NETWORK_ERROR,
                    StrUtil.format(
                            "failed to get network items ( network_id: {} )",
                            networkId
                    ), e
            );
        }
    }

    @Override
    public List<RelayerNetwork> getAllNetworks() {
        try {
            List<RelayerNetworkEntity> entities = relayerNetworkMapper.selectList(null);
            if (ObjectUtil.isEmpty(entities)) {
                return ListUtil.empty();
            }

            return entities.stream()
                    .collect(
                            Collectors.groupingBy(
                                    RelayerNetworkEntity::getNetworkId,
                                    HashMap::new,
                                    Collectors.toMap(
                                            RelayerNetworkEntity::getDomain,
                                            entity -> new RelayerNetwork.Item(entity.getNodeId(), entity.getSyncState())
                                    )
                            )
                    ).entrySet().stream()
                    .map(
                            entry -> {
                                RelayerNetwork network = new RelayerNetwork(entry.getKey());
                                network.addItem(entry.getValue());
                                return network;
                            }
                    ).collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_RELAYER_NETWORK_ERROR,
                    "failed to get all networks",
                    e
            );
        }
    }

    @Override
    public RelayerNetwork getRelayerNetwork(String networkId) {
        try {
            List<RelayerNetworkEntity> entities = relayerNetworkMapper.selectList(
                    new LambdaQueryWrapper<RelayerNetworkEntity>()
                            .eq(RelayerNetworkEntity::getNetworkId, networkId)
            );
            if (ObjectUtil.isEmpty(entities)) {
                return null;
            }

            RelayerNetwork network = new RelayerNetwork(networkId);
            entities.forEach(
                    entity -> network.addItem(entity.getDomain(), entity.getNodeId(), entity.getSyncState())
            );

            return network;

        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_RELAYER_NETWORK_ERROR,
                    "failed to get network for network " + networkId,
                    e
            );
        }
    }

    @Override
    public String getRelayerNodeIdForDomain(String domain) {
        try {
            RelayerNetworkEntity entity = relayerNetworkMapper.selectOne(
                    new LambdaQueryWrapper<RelayerNetworkEntity>()
                            .eq(RelayerNetworkEntity::getDomain, domain)
            );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }

            return entity.getNodeId();

        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_RELAYER_NETWORK_ERROR,
                    "failed to get relayer node id for domain " + domain,
                    e
            );
        }
    }

    @Override
    public void addRelayerNode(RelayerNodeInfo nodeInfo) {
        try {
            relayerNodeMapper.insert(
                    ConvertUtil.convertFromRelayerNodeInfo(nodeInfo)
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_RELAYER_NODE_ERROR,
                    "failed to add relayer node " + nodeInfo.getNodeId(),
                    e
            );
        }
    }

    @Override
    public boolean updateRelayerNode(RelayerNodeInfo nodeInfo) {
        try {
            return 1 == relayerNodeMapper.update(
                    ConvertUtil.convertFromRelayerNodeInfo(nodeInfo),
                    new LambdaQueryWrapper<RelayerNodeEntity>()
                            .eq(RelayerNodeEntity::getNodeId, nodeInfo.getNodeId())
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_RELAYER_NODE_ERROR,
                    "failed to update relayer node " + nodeInfo.getNodeId(),
                    e
            );
        }
    }

    @Override
    @Transactional
    public void updateRelayerNodeProperty(String nodeId, String key, String value) {
        try {
            RelayerNodeInfo nodeInfo = getRelayerNode(nodeId);
            if (ObjectUtil.isNull(nodeInfo)) {
                throw new RuntimeException("node info not exist");
            }
            nodeInfo.getProperties().getProperties().put(key, value);
            updateRelayerNode(nodeInfo);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_RELAYER_NODE_ERROR,
                    StrUtil.format("failed to update relayer node {} properties ( key: {}, val: {} )", nodeId, key, value),
                    e
            );
        }
    }

    @Override
    public RelayerNodeInfo getRelayerNode(String nodeId) {
        try {
            RelayerNodeEntity entity = relayerNodeMapper.selectOne(
                    new LambdaQueryWrapper<RelayerNodeEntity>()
                            .eq(RelayerNodeEntity::getNodeId, nodeId)
            );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }

            return ConvertUtil.convertFromRelayerNodeEntity(entity);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_RELAYER_NODE_ERROR,
                    "failed to get relayer node " + nodeId,
                    e
            );
        }
    }

    @Override
    public List<RelayerHealthInfo> getAllRelayerHealthInfo() {
        try {
            List<DTActiveNodeEntity> entities = dtActiveNodeMapper.selectList(null);
            if (ObjectUtil.isEmpty(entities)) {
                return ListUtil.empty();
            }

            return entities.stream().map(
                    entity -> ConvertUtil.convertFromDTActiveNodeEntity(grpcServerPort, entity)
            ).collect(Collectors.toList());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DT_ACTIVE_NODE_ERROR,
                    "failed to get all relayer health info from dtActiveNode",
                    e
            );
        }
    }
}
