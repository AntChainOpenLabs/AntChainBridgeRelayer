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

package com.alipay.antchain.bridge.relayer.commons.model;

import java.util.Map;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.HexUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageV2;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgTrustLevelEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.UpperProtocolTypeBeyondAMEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthMsgWrapper {

    private long authMsgId;

    private String product;

    private String blockchainId;

    private String domain;

    private byte[] ucpId;

    private String amClientContractAddress;

    private int version;

    private String msgSender;

    private UpperProtocolTypeBeyondAMEnum protocolType;

    private AuthMsgTrustLevelEnum trustLevel;

    private AuthMsgProcessStateEnum processState;

    private IAuthMessage authMessage;

    private Map<String, String> ledgerInfo = MapUtil.newHashMap();

    public AuthMsgWrapper(
            String product,
            String blockchainId,
            String domain,
            byte[] ucpId,
            String amClientContractAddress,
            AuthMsgProcessStateEnum processState,
            IAuthMessage authMessage
    ) {
        this(0, product, blockchainId, domain, ucpId, amClientContractAddress, processState, authMessage);
    }

    public AuthMsgWrapper(
            long authMsgId,
            String product,
            String blockchainId,
            String domain,
            byte[] ucpId,
            String amClientContractAddress,
            AuthMsgProcessStateEnum processState,
            IAuthMessage authMessage
    ) {
        this.authMsgId = authMsgId;
        this.authMessage = authMessage;
        this.product = product;
        this.domain = domain;
        this.blockchainId = blockchainId;
        this.ucpId = ucpId;
        this.amClientContractAddress = amClientContractAddress;
        this.version = authMessage.getVersion();
        this.msgSender = authMessage.getIdentity().toHex();
        this.protocolType = UpperProtocolTypeBeyondAMEnum.parseFromValue(authMessage.getUpperProtocol());
        this.processState = processState;
        this.trustLevel = authMessage.getVersion() >= 2 ?
                AuthMsgTrustLevelEnum.parseFromValue(((AuthMessageV2) authMessage).getTrustLevel().ordinal())
                : AuthMsgTrustLevelEnum.NEGATIVE_TRUST;
    }

    public byte[] getPayload() {
        return this.authMessage.getPayload();
    }

    public String getUcpIdHex() {
        return HexUtil.encodeHexStr(this.getUcpId());
    }

    public byte[] getRawLedgerInfo() {
        return JSON.toJSONBytes(ledgerInfo);
    }
}
