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

import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.core.base.UniformCrosschainPacket;
import com.alipay.antchain.bridge.relayer.commons.constant.UniformCrosschainPacketStateEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UniformCrosschainPacketContext {

    private long id;

    private byte[] ucpId;

    private String product;

    private String blockchainId;

    private UniformCrosschainPacket ucp;

    private String udagPath;

    private UniformCrosschainPacketStateEnum processState;

    private boolean fromNetwork;

    private String relayerId;

    private RelayerNodeInfo remoteRelayerNodeInfo;

    public UniformCrosschainPacketContext() {

    }

    private void generateUcpId() {
        this.ucpId = StrUtil.isEmpty(this.udagPath) ? RandomUtil.randomBytes(32) : DigestUtil.sha256(this.udagPath);
    }

    public int getVersion() {
        return this.ucp.getVersion();
    }

    public String getSrcDomain() {
        return this.ucp.getSrcDomain().getDomain();
    }

    public String getBlockHash() {
        return HexUtil.encodeHexStr(this.ucp.getSrcMessage().getProvableData().getBlockHash());
    }

    public String getTxHash() {
        return HexUtil.encodeHexStr(this.ucp.getSrcMessage().getProvableData().getTxHash());
    }

    public int getProtocolType() {
        return this.ucp.getSrcMessage().getType().ordinal();
    }

    public byte[] getSrcMessage() {
        return JSON.toJSONBytes(this.ucp.getSrcMessage());
    }

    public byte[] getPtcOid() {
        return this.ucp.getPtcId().encode();
    }

    public byte[] getTpProof() {
        return this.ucp.getTpProof();
    }

    public long getLedgerTime() {
        return this.ucp.getSrcMessage().getProvableData().getTimestamp();
    }
}
