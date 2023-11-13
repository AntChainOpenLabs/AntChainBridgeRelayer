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

package com.alipay.antchain.bridge.relayer.core.manager.network;

import java.security.PrivateKey;
import java.security.Signature;

import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.RelayerCredentialSubject;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.types.network.request.RelayerRequest;
import com.alipay.antchain.bridge.relayer.core.types.network.response.RelayerResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class RelayerCredentialManager implements IRelayerCredentialManager {

    @Value("#{relayerCoreConfig.localRelayerCrossChainCertificate}")
    private AbstractCrossChainCertificate localRelayerCertificate;

    @Value("#{relayerCoreConfig.localRelayerCredentialSubject}")
    private RelayerCredentialSubject localRelayerCredentialSubject;

    @Value("#{relayerCoreConfig.localPrivateKey}")
    private PrivateKey localRelayerPrivateKey;

    @Value("#{relayerCoreConfig.localRelayerNodeId}")
    private String localNodeId;

    @Value("${relayer.network.node.sig_algo:SHA256WithRSA}")
    private String localNodeSigAlgo;

    @Resource
    private IBCDNSManager bcdnsManager;

    @Override
    public void signRelayerRequest(RelayerRequest relayerRequest) {
        try {
            relayerRequest.setNodeId(localNodeId);
            relayerRequest.setSenderRelayerCertificate(localRelayerCertificate);
            relayerRequest.setSigAlgo(localNodeSigAlgo);

            Signature signer = Signature.getInstance(localNodeSigAlgo);
            signer.initSign(localRelayerPrivateKey);
            signer.update(relayerRequest.rawEncode());
            relayerRequest.setSignature(signer.sign());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    e,
                    "failed to sign for request type {}", relayerRequest.getRequestType().getCode()
            );
        }
    }

    @Override
    public void signRelayerResponse(RelayerResponse relayerResponse) {
        try {
            relayerResponse.setRemoteRelayerCertificate(localRelayerCertificate);
            relayerResponse.setSigAlgo(localNodeSigAlgo);

            Signature signer = Signature.getInstance(localNodeSigAlgo);
            signer.initSign(localRelayerPrivateKey);
            signer.update(relayerResponse.rawEncode());
            relayerResponse.setSignature(signer.sign());
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.CORE_RELAYER_NETWORK_ERROR,
                    "failed to sign response",
                    e
            );
        }
    }

    @Override
    public boolean validateRelayerRequest(RelayerRequest relayerRequest) {
        if (!bcdnsManager.validateCrossChainCertificate(relayerRequest.getSenderRelayerCertificate())) {
            return false;
        }
        if (
                ObjectUtil.notEqual(
                        CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE,
                        relayerRequest.getSenderRelayerCertificate().getType()
                )
        ) {
            return false;
        }

        return relayerRequest.verify();
    }

    @Override
    public boolean validateRelayerResponse(RelayerResponse relayerResponse) {
        if (!bcdnsManager.validateCrossChainCertificate(relayerResponse.getRemoteRelayerCertificate())) {
            return false;
        }
        if (
                ObjectUtil.notEqual(
                        CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE,
                        relayerResponse.getRemoteRelayerCertificate().getType()
                )
        ) {
            return false;
        }

        return relayerResponse.verify();
    }
}
