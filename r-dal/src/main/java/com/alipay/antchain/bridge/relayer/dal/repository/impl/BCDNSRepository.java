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

import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.DomainSpaceCertWrapper;
import com.alipay.antchain.bridge.relayer.dal.entities.DomainSpaceCertEntity;
import com.alipay.antchain.bridge.relayer.dal.mapper.DomainSpaceCertMapper;
import com.alipay.antchain.bridge.relayer.dal.repository.IBCDNSRepository;
import com.alipay.antchain.bridge.relayer.dal.utils.ConvertUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;

@Component
public class BCDNSRepository implements IBCDNSRepository {

    @Resource
    private DomainSpaceCertMapper domainSpaceCertMapper;

    @Override
    public boolean hasDomainSpaceCert(String domainSpace) {
        return domainSpaceCertMapper.exists(
                new LambdaQueryWrapper<DomainSpaceCertEntity>()
                        .eq(DomainSpaceCertEntity::getDomainSpace, domainSpace)
        );
    }

    @Override
    public void saveDomainSpaceCert(DomainSpaceCertWrapper domainSpaceCertWrapper) {
        try {
            domainSpaceCertMapper.insert(ConvertUtil.convertFromDomainSpaceCertWrapper(domainSpaceCertWrapper));
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DOMAIN_SPACE_ERROR,
                    e,
                    "failed to insert domain space certificate for space {}",
                    domainSpaceCertWrapper.getDomainSpace()
            );
        }
    }
}
