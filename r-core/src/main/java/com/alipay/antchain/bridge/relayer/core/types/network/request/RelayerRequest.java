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

package com.alipay.antchain.bridge.relayer.core.types.network.request;

import java.security.PublicKey;
import java.security.Signature;

import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.RelayerCredentialSubject;
import com.alipay.antchain.bridge.commons.bcdns.utils.ObjectIdentityUtil;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVTypeEnum;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.TLVUtils;
import com.alipay.antchain.bridge.commons.utils.codec.tlv.annotation.TLVField;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Relayer请求
 */
@Getter
@Setter
@Slf4j
@NoArgsConstructor
public class RelayerRequest {

    public static final short TLV_TYPE_RELAYER_REQUEST_TYPE = 0;

    public static final short TLV_TYPE_RELAYER_REQUEST_NODE_ID = 1;

    public static final short TLV_TYPE_RELAYER_REQUEST_RELAYER_CERT = 2;

    public static final short TLV_TYPE_RELAYER_REQUEST_PAYLOAD = 3;

    public static final short TLV_TYPE_RELAYER_REQUEST_SIG_ALGO = 4;

    public static final short TLV_TYPE_RELAYER_REQUEST_SIGNATURE = 5;

    public static RelayerRequest decode(byte[] rawData, Class<? extends RelayerRequest> requestClass) {
        return TLVUtils.decode(rawData, requestClass);
    }

    public RelayerRequest(
            RelayerRequestType relayerRequestType,
            String nodeId,
            AbstractCrossChainCertificate senderRelayerCertificate,
            String sigAlgo
    ) {
        this.requestType = relayerRequestType;
        this.nodeId = nodeId;
        this.senderRelayerCertificate = senderRelayerCertificate;
        this.sigAlgo = sigAlgo;
    }

    @TLVField(tag = TLV_TYPE_RELAYER_REQUEST_TYPE, type = TLVTypeEnum.UINT8)
    private RelayerRequestType requestType;

    @TLVField(tag = TLV_TYPE_RELAYER_REQUEST_NODE_ID, type = TLVTypeEnum.STRING, order = TLV_TYPE_RELAYER_REQUEST_NODE_ID)
    private String nodeId;

    @TLVField(tag = TLV_TYPE_RELAYER_REQUEST_RELAYER_CERT, type = TLVTypeEnum.BYTES, order = TLV_TYPE_RELAYER_REQUEST_RELAYER_CERT)
    private AbstractCrossChainCertificate senderRelayerCertificate;

    @TLVField(tag = TLV_TYPE_RELAYER_REQUEST_PAYLOAD, type = TLVTypeEnum.BYTES, order = TLV_TYPE_RELAYER_REQUEST_PAYLOAD)
    private byte[] requestPayload;

    @TLVField(tag = TLV_TYPE_RELAYER_REQUEST_SIG_ALGO, type = TLVTypeEnum.STRING, order = TLV_TYPE_RELAYER_REQUEST_SIG_ALGO)
    private String sigAlgo;

    @TLVField(tag = TLV_TYPE_RELAYER_REQUEST_SIGNATURE, type = TLVTypeEnum.BYTES, order = TLV_TYPE_RELAYER_REQUEST_SIGNATURE)
    private byte[] signature;

    public byte[] rawEncode() {
        return TLVUtils.encode(this, TLV_TYPE_RELAYER_REQUEST_SIGNATURE);
    }

    public byte[] encode() {
        return TLVUtils.encode(this);
    }

    /**
     * 验签
     *
     * @return
     */
    public boolean verify() {

        try {
            RelayerCredentialSubject relayerCredentialSubject = RelayerCredentialSubject.decode(
                    senderRelayerCertificate.getCredentialSubject()
            );
            PublicKey publicKey = ObjectIdentityUtil.getPublicKeyFromSubject(
                    relayerCredentialSubject.getApplicant(),
                    relayerCredentialSubject.getSubjectInfo()
            );

            Signature verifier = Signature.getInstance(sigAlgo);
            verifier.initVerify(publicKey);
            verifier.update(rawEncode());

            return verifier.verify(signature);

        } catch (Exception e) {
            throw new RuntimeException("failed to verify request sig", e);
        }
    }

    public void setRequestTypeCode(String requestType) {
        this.requestType = RelayerRequestType.parseFromValue(requestType);
    }
}
