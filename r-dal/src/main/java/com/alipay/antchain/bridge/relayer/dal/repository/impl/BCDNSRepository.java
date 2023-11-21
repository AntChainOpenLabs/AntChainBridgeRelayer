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
import javax.annotation.Resource;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.ObjectIdentity;
import com.alipay.antchain.bridge.relayer.commons.constant.BCDNSStateEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.BCDNSServiceDO;
import com.alipay.antchain.bridge.relayer.commons.model.DomainSpaceCertWrapper;
import com.alipay.antchain.bridge.relayer.dal.entities.BCDNSServiceEntity;
import com.alipay.antchain.bridge.relayer.dal.entities.DomainSpaceCertEntity;
import com.alipay.antchain.bridge.relayer.dal.mapper.BCDNSServiceMapper;
import com.alipay.antchain.bridge.relayer.dal.mapper.DomainSpaceCertMapper;
import com.alipay.antchain.bridge.relayer.dal.repository.IBCDNSRepository;
import com.alipay.antchain.bridge.relayer.dal.utils.ConvertUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BCDNSRepository implements IBCDNSRepository {

    @Resource
    private DomainSpaceCertMapper domainSpaceCertMapper;

    @Resource
    private BCDNSServiceMapper bcdnsServiceMapper;

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

    @Override
    public DomainSpaceCertWrapper getDomainSpaceCert(String domainSpace) {
        try {
            DomainSpaceCertEntity entity = domainSpaceCertMapper.selectOne(
                    new LambdaQueryWrapper<DomainSpaceCertEntity>()
                            .eq(DomainSpaceCertEntity::getDomainSpace, domainSpace)
            );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }
            return ConvertUtil.convertFromDomainSpaceCertEntity(entity);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DOMAIN_SPACE_ERROR,
                    e,
                    "failed to get domain space certificate for space {}",
                    domainSpace
            );
        }
    }

    @Override
    public DomainSpaceCertWrapper getDomainSpaceCert(ObjectIdentity ownerOid) {
        try {
            List<DomainSpaceCertEntity> entityList = domainSpaceCertMapper.selectList(
                    new LambdaQueryWrapper<DomainSpaceCertEntity>()
                            .eq(DomainSpaceCertEntity::getOwnerOidHex, HexUtil.encodeHexStr(ownerOid.encode()))
            );
            if (ObjectUtil.isEmpty(entityList)) {
                return null;
            }
            return ConvertUtil.convertFromDomainSpaceCertEntity(entityList.get(0));
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DOMAIN_SPACE_ERROR,
                    e,
                    "failed to get domain space certificate by owner oid {}",
                    HexUtil.encodeHexStr(ownerOid.encode())
            );
        }
    }

    @Override
    public Map<String, DomainSpaceCertWrapper> getDomainSpaceCertChain(String leafDomainSpace) {
        Map<String, DomainSpaceCertWrapper> result = new HashMap<>();
        try {
            addDomainSpaceCert(leafDomainSpace, result);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DOMAIN_SPACE_ERROR,
                    e,
                    "failed to get domain space certificate chain for space {}",
                    leafDomainSpace
            );
        }

        return result;
    }

    @Override
    public List<String> getDomainSpaceChain(String leafDomainSpace) {
        List<String> result = ListUtil.toList(leafDomainSpace);
        String currDomainSpace = leafDomainSpace;
        try {
            do {
                DomainSpaceCertEntity entity = domainSpaceCertMapper.selectOne(
                        new LambdaQueryWrapper<DomainSpaceCertEntity>()
                                .select(ListUtil.toList(DomainSpaceCertEntity::getParentSpace))
                                .eq(DomainSpaceCertEntity::getDomainSpace, leafDomainSpace)
                );
                if (ObjectUtil.isNull(entity)) {
                    throw new RuntimeException(StrUtil.format("none data found for domain space {}", currDomainSpace));
                }
                result.add(entity.getParentSpace());
                currDomainSpace = entity.getParentSpace();
            } while (StrUtil.isNotEmpty(currDomainSpace));
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_DOMAIN_SPACE_ERROR,
                    e,
                    "failed to get domain space certificate chain for space {}",
                    leafDomainSpace
            );
        }
        return result;
    }

    @Override
    @Transactional
    public boolean hasBCDNSService(String domainSpace) {
        if (!hasDomainSpaceCert(domainSpace)) {
            return false;
        }
        return bcdnsServiceMapper.exists(
                new LambdaQueryWrapper<BCDNSServiceEntity>().eq(
                        BCDNSServiceEntity::getDomainSpace, domainSpace
                )
        );
    }

    @Override
    public BCDNSServiceDO getBCDNSServiceDO(String domainSpace) {
        try {
            DomainSpaceCertEntity entity = domainSpaceCertMapper.selectOne(
                    new LambdaQueryWrapper<DomainSpaceCertEntity>()
                            .eq(DomainSpaceCertEntity::getDomainSpace, domainSpace)
            );
            if (ObjectUtil.isNull(entity)) {
                return null;
            }
            DomainSpaceCertWrapper domainSpaceCertWrapper = ConvertUtil.convertFromDomainSpaceCertEntity(entity);

            BCDNSServiceEntity bcdnsServiceEntity = bcdnsServiceMapper.selectOne(
                    new LambdaQueryWrapper<BCDNSServiceEntity>()
                            .eq(BCDNSServiceEntity::getDomainSpace, domainSpace)
            );
            if (ObjectUtil.isNull(bcdnsServiceEntity)) {
                return null;
            }
            return ConvertUtil.convertFromBCDNSServiceEntity(bcdnsServiceEntity, domainSpaceCertWrapper);
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BCDNS_ERROR,
                    e,
                    "failed to get bcdns data for space {}",
                    domainSpace
            );
        }
    }

    @Override
    public void saveBCDNSServiceDO(BCDNSServiceDO bcdnsServiceDO) {
        if (!hasDomainSpaceCert(bcdnsServiceDO.getDomainSpace())) {
            saveDomainSpaceCert(bcdnsServiceDO.getDomainSpaceCertWrapper());
        }
        try {
            bcdnsServiceMapper.insert(
                    ConvertUtil.convertFromBCDNSServiceDO(bcdnsServiceDO)
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BCDNS_ERROR,
                    e,
                    "failed to save bcdns data for space {}",
                    bcdnsServiceDO.getDomainSpace()
            );
        }
    }

    @Override
    public void updateBCDNSServiceState(String domainSpace, BCDNSStateEnum stateEnum) {
        try {
            BCDNSServiceEntity entity = new BCDNSServiceEntity();
            entity.setState(stateEnum);
            if (
                    bcdnsServiceMapper.update(
                            entity,
                            new LambdaQueryWrapper<BCDNSServiceEntity>()
                                    .eq(BCDNSServiceEntity::getDomainSpace, domainSpace)
                    ) != 1
            ) {
                throw new RuntimeException("failed to update bcdns record");
            }
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BCDNS_ERROR,
                    e,
                    "failed to update bcdns state to {} for space {}",
                    stateEnum.getCode(), domainSpace
            );
        }
    }

    @Override
    public void updateBCDNSServiceProperties(String domainSpace, byte[] rawProp) {
        try {
            BCDNSServiceEntity entity = new BCDNSServiceEntity();
            entity.setProperties(rawProp);
            if (
                    bcdnsServiceMapper.update(
                            entity,
                            new LambdaQueryWrapper<BCDNSServiceEntity>()
                                    .eq(BCDNSServiceEntity::getDomainSpace, domainSpace)
                    ) != 1
            ) {
                throw new RuntimeException("failed to update bcdns record");
            }
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.DAL_BCDNS_ERROR,
                    e,
                    "failed to update bcdns properties to {} for space {}",
                    Base64.encode(rawProp), domainSpace
            );
        }
    }

    private void addDomainSpaceCert(String currDomainSpace, Map<String, DomainSpaceCertWrapper> result) {
        DomainSpaceCertEntity entity = domainSpaceCertMapper.selectOne(
                new LambdaQueryWrapper<DomainSpaceCertEntity>()
                        .eq(DomainSpaceCertEntity::getDomainSpace, currDomainSpace)
        );
        if (ObjectUtil.isNull(entity)) {
            throw new RuntimeException(StrUtil.format("domain space cert not found for {}", currDomainSpace));
        }
        result.put(currDomainSpace, ConvertUtil.convertFromDomainSpaceCertEntity(entity));
        if (!StrUtil.equals(currDomainSpace, CrossChainDomain.ROOT_DOMAIN_SPACE)) {
            addDomainSpaceCert(entity.getParentSpace(), result);
        }
    }
}
