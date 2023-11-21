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

package com.alipay.antchain.bridge.relayer.core.manager.bcdns;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.bcdns.impl.BlockChainDomainNameServiceFactory;
import com.alipay.antchain.bridge.bcdns.service.IBlockChainDomainNameService;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.BCDNSServiceDO;
import com.alipay.antchain.bridge.relayer.commons.model.DomainSpaceCertWrapper;
import com.alipay.antchain.bridge.relayer.dal.repository.IBCDNSRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BCDNSManager implements IBCDNSManager {

    @Resource
    private IBCDNSRepository bcdnsRepository;

    private final Map<String, IBlockChainDomainNameService> bcdnsClientMap = new ConcurrentHashMap<>();

    @Override
    public IBlockChainDomainNameService getBCDNSService(String domainSpace) {
        if (bcdnsClientMap.containsKey(domainSpace)) {
            return bcdnsClientMap.get(domainSpace);
        }

        BCDNSServiceDO bcdnsServiceDO = getBCDNSServiceData(domainSpace);
        if (ObjectUtil.isNull(bcdnsServiceDO)) {
            log.warn("none bcdns data found for domain space {}", domainSpace);
            return null;
        }

        IBlockChainDomainNameService service = startBCDNSService(bcdnsServiceDO);
        bcdnsClientMap.put(domainSpace, service);
        return service;
    }

    @Override
    public IBlockChainDomainNameService startBCDNSService(BCDNSServiceDO bcdnsServiceDO) {
        log.info("starting the bcdns service ( type: {}, domain_space: {} )",
                bcdnsServiceDO.getType().getCode(), bcdnsServiceDO.getDomainSpace());
        return BlockChainDomainNameServiceFactory.create(bcdnsServiceDO.getType(), bcdnsServiceDO.getProperties());
    }

    @Override
    public void saveBCDNSServiceData(BCDNSServiceDO bcdnsServiceDO) {
        if (bcdnsRepository.hasBCDNSService(bcdnsServiceDO.getDomainSpace())) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_BCDNS_MANAGER_ERROR,
                    "bcdns {} not exist or data incomplete",
                    bcdnsServiceDO.getDomainSpace()
            );
        }
        bcdnsRepository.saveBCDNSServiceDO(bcdnsServiceDO);
    }

    @Override
    public BCDNSServiceDO getBCDNSServiceData(String domainSpace) {
        return bcdnsRepository.getBCDNSServiceDO(domainSpace);
    }

    @Override
    public Map<String, AbstractCrossChainCertificate> getTrustRootCertChain(String domainSpace) {
        return bcdnsRepository.getDomainSpaceCertChain(domainSpace).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getDomainSpaceCert()
                ));
    }

    @Override
    public List<String> getDomainSpaceChain(String domainSpace) {
        return bcdnsRepository.getDomainSpaceChain(domainSpace);
    }

    @Override
    public AbstractCrossChainCertificate getTrustRootCertForRootDomain() {
        DomainSpaceCertWrapper wrapper = bcdnsRepository.getDomainSpaceCert(CrossChainDomain.ROOT_DOMAIN_SPACE);
        if (ObjectUtil.isNull(wrapper)) {
            return null;
        }
        return wrapper.getDomainSpaceCert();
    }

    @Override
    public boolean validateCrossChainCertificate(AbstractCrossChainCertificate certificate) {
        DomainSpaceCertWrapper trustRootCert = bcdnsRepository.getDomainSpaceCert(certificate.getIssuer());
        if (ObjectUtil.isNull(trustRootCert)) {
            log.warn(
                    "none trust root found for {} to verify for relayer cert: {}",
                    HexUtil.encodeHexStr(certificate.getIssuer().encode()),
                    CrossChainCertificateUtil.formatCrossChainCertificateToPem(certificate)
            );
            return false;
        }
        return trustRootCert.getDomainSpaceCert().getCredentialSubjectInstance().verifyIssueProof(
                certificate.getEncodedToSign(),
                certificate.getProof()
        );
    }

    @Override
    public void saveDomainSpaceCerts(Map<String, AbstractCrossChainCertificate> domainSpaceCerts) {
        for (Map.Entry<String, AbstractCrossChainCertificate> entry : domainSpaceCerts.entrySet()) {
            try {
                if (bcdnsRepository.hasDomainSpaceCert(entry.getKey())) {
                    log.warn("DomainSpace {} already exists", entry.getKey());
                    continue;
                }
                bcdnsRepository.saveDomainSpaceCert(new DomainSpaceCertWrapper(entry.getValue()));
                log.info("successful to save domain space cert for {}", entry.getKey());
            } catch (Exception e) {
                log.error("failed to save domain space certs for space {} : ", entry.getKey(), e);
            }
        }
    }
}
