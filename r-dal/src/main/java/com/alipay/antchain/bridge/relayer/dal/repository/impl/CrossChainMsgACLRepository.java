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

import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.CrossChainMsgACLItem;
import com.alipay.antchain.bridge.relayer.dal.entities.CrossChainMsgACLEntity;
import com.alipay.antchain.bridge.relayer.dal.mapper.CrossChainMsgACLMapper;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMsgACLRepository;
import com.alipay.antchain.bridge.relayer.dal.utils.ConvertUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

@Component
public class CrossChainMsgACLRepository implements ICrossChainMsgACLRepository {

    @Resource
    private CrossChainMsgACLMapper crossChainMsgACLMapper;

    @Override
    public void saveItem(CrossChainMsgACLItem item) {
        try {
            crossChainMsgACLMapper.insert(
                    ConvertUtil.convertFromCrossChainMsgACLItem(item)
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ACL_ERROR,
                    StrUtil.format("failed to insert acl item {}", item.getBizId()),
                    e
            );
        }
    }

    @Override
    public void deleteItem(String bizId) {
        try {
            crossChainMsgACLMapper.delete(
                    new LambdaQueryWrapper<CrossChainMsgACLEntity>().eq(CrossChainMsgACLEntity::getBizId, bizId)
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ACL_ERROR,
                    StrUtil.format("failed to delete acl item {}", bizId),
                    e
            );
        }
    }

    @Override
    public CrossChainMsgACLItem getItemByBizId(String bizId) {
        try {
            CrossChainMsgACLEntity entity = crossChainMsgACLMapper.selectOne(
                    new LambdaQueryWrapper<CrossChainMsgACLEntity>().eq(CrossChainMsgACLEntity::getBizId, bizId)
            );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }
            return ConvertUtil.convertFromCrossChainMsgACLItem(entity);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ACL_ERROR,
                    StrUtil.format("failed to delete acl item {}", bizId),
                    e
            );
        }
    }

    @Override
    public boolean checkItem(CrossChainMsgACLItem item) {
        try {
            CrossChainMsgACLEntity entity = crossChainMsgACLMapper.selectOne(
                    new LambdaQueryWrapper<CrossChainMsgACLEntity>()
                            .select(ListUtil.of(CrossChainMsgACLEntity::getIsDeleted))
                            .eq(CrossChainMsgACLEntity::getOwnerDomain, item.getOwnerDomain())
                            .eq(CrossChainMsgACLEntity::getOwnerIdHex, item.getOwnerIdentityHex().toLowerCase())
                            .eq(CrossChainMsgACLEntity::getGrantDomain, item.getGrantDomain())
                            .eq(CrossChainMsgACLEntity::getGrantIdHex, item.getGrantIdentityHex().toLowerCase())
                            .or(
                                    wrapper -> wrapper.eq(CrossChainMsgACLEntity::getOwnerDomain, item.getOwnerDomain())
                                            .eq(CrossChainMsgACLEntity::getGrantDomain, item.getGrantDomain())
                                            .eq(CrossChainMsgACLEntity::getOwnerIdHex, null)
                                            .eq(CrossChainMsgACLEntity::getGrantIdHex, null)
                            )
            );
            if (ObjectUtil.isNull(entity)) {
                return false;
            }
            return 0 == entity.getIsDeleted();
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_CROSSCHAIN_MSG_ACL_ERROR,
                    StrUtil.format(
                            "failed to check acl item: ( owner_domain: {}, owner_id_hex: {}, grant_domain: {}, grant_id_hex: {} )",
                            item.getOwnerDomain(), item.getOwnerIdentityHex(), item.getGrantDomain(), item.getGrantIdentityHex()
                    ),
                    e
            );
        }
    }
}
