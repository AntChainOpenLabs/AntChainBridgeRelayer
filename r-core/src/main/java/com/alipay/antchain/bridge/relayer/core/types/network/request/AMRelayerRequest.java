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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AMRelayerRequest extends RelayerRequest {

    @JSONField
    private String udagProof;

    @JSONField
    private String authMsg;

    @JSONField
    private String domainName;

    @JSONField
    private String ledgerInfo;

    public AMRelayerRequest(
            String nodeId,
            AbstractCrossChainCertificate senderRelayerCertificate,
            String sigAlgo,
            String udagProof,
            String authMsg,
            String domainName,
            String ledgerInfo
    ) {
        super(
                RelayerRequestType.AM_REQUEST,
                nodeId,
                senderRelayerCertificate,
                sigAlgo
        );
        this.udagProof = udagProof;
        this.authMsg = authMsg;
        this.domainName = domainName;
        this.ledgerInfo = ledgerInfo;

        setRequestPayload(
                JSON.toJSONBytes(this)
        );
    }
}
