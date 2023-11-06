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

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@NoArgsConstructor
@FieldNameConstants
public class HandshakeRelayerRequest extends RelayerRequest {

    public static HandshakeRelayerRequest createFrom(RelayerRequest relayerRequest) {
        HandshakeRelayerRequest request = BeanUtil.copyProperties(
                relayerRequest,
                HandshakeRelayerRequest.class
        );

        JSONObject jsonObject = JSON.parseObject(new String(relayerRequest.getRequestPayload()));
        request.setNetworkId(jsonObject.getString(Fields.networkId));
        request.setSenderNodeInfo(RelayerNodeInfo.decode(jsonObject.getBytes(Fields.senderNodeInfo)));
        return request;
    }

    public static byte[] createHandshakePayload(
            String networkId,
            RelayerNodeInfo relayerNodeInfo
    ) {
        return JSON.toJSONBytes(
                MapUtil.builder()
                        .put(Fields.networkId, networkId)
                        .put(Fields.senderNodeInfo, relayerNodeInfo.encodeWithProperties())
                        .build()
        );
    }

    private String networkId;

    private RelayerNodeInfo senderNodeInfo;

    public HandshakeRelayerRequest(
            RelayerNodeInfo senderNodeInfo,
            String networkId
    ) {
        super(
                RelayerRequestType.HANDSHAKE
        );

        this.networkId = networkId;
        this.senderNodeInfo = senderNodeInfo;
        setRequestPayload(
                createHandshakePayload(networkId, senderNodeInfo)
        );
    }
}
