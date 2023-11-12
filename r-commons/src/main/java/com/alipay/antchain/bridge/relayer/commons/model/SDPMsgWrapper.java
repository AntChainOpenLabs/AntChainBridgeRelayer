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

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.commons.core.sdp.AbstractSDPMessage;
import com.alipay.antchain.bridge.relayer.commons.constant.SDPMsgProcessStateEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SDPMsgWrapper {

    public final static int UNORDERED_SDP_MSG_SEQ = -1;

    public final static String UNORDERED_SDP_MSG_SESSION = "UNORDERED";

    private long id;

    private AuthMsgWrapper authMsgWrapper;

    private String receiverBlockchainProduct;

    private String receiverBlockchainId;

    private String receiverAMClientContract;

    private SDPMsgProcessStateEnum processState;

    private String txHash;

    private boolean txSuccess;

    private String txFailReason;

    private AbstractSDPMessage sdpMessage;

    public SDPMsgWrapper(
            String receiverBlockchainProduct,
            String receiverBlockchainId,
            String receiverAMClientContract,
            SDPMsgProcessStateEnum processState,
            String txHash,
            boolean txSuccess,
            String txFailReason,
            AuthMsgWrapper authMsgWrapper,
            AbstractSDPMessage sdpMessage
    ) {
        this(
                0,
                authMsgWrapper,
                receiverBlockchainProduct,
                receiverBlockchainId,
                receiverAMClientContract,
                processState,
                txHash,
                txSuccess,
                txFailReason,
                sdpMessage
        );
    }

    public int getVersion() {
        return this.sdpMessage.getVersion();
    }

    public boolean getAtomic() {
        return this.sdpMessage.getAtomic();
    }

    public String getSenderBlockchainProduct() {
        return this.authMsgWrapper.getProduct();
    }

    public String getSenderBlockchainId() {
        return this.authMsgWrapper.getBlockchainId();
    }

    public String getSenderBlockchainDomain() {
        return this.authMsgWrapper.getDomain();
    }

    public String getMsgSender() {
        return this.authMsgWrapper.getMsgSender();
    }

    public String getSenderAMClientContract() {
        return this.authMsgWrapper.getAmClientContractAddress();
    }

    public String getReceiverBlockchainDomain() {
        return this.sdpMessage.getTargetDomain().getDomain();
    }

    public String getMsgReceiver() {
        return this.sdpMessage.getTargetIdentity().toHex();
    }

    public int getMsgSequence() {
        return this.sdpMessage.getSequence();
    }

    public boolean isBlockchainSelfCall() {
        return StrUtil.equals(getSenderBlockchainDomain(), getReceiverBlockchainDomain())
                && StrUtil.equalsIgnoreCase(getMsgSender(), getMsgReceiver());
    }

    /**
     * getSessionKey returns session key e.g: "domainA.idA:domainB.idB"
     */
    public String getSessionKey() {
        String key = String.format(
                "%s.%s:%s.%s",
                getSenderBlockchainDomain(),
                getMsgSender(),
                getReceiverBlockchainDomain(),
                getMsgReceiver()
        );
        if (UNORDERED_SDP_MSG_SEQ == getMsgSequence()) {
            // 将无序消息单拎出来，完全异步发送
            return String.format("%s-%s", UNORDERED_SDP_MSG_SESSION, key);
        }
        return key;
    }
}
