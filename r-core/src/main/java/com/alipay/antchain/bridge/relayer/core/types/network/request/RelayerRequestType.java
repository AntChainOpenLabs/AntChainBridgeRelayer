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

import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Relayer请求类型
 */
@Getter
@AllArgsConstructor
public enum RelayerRequestType {

    // relayer之间的请求
    GET_RELAYER_NODE_INFO("getRelayerNodeInfo"),

    GET_RELAYER_BLOCKCHAIN_INFO("getBlockchainInfo"),

    AM_REQUEST("amRequest"),

    /**
     * 建立可信连接，进行握手
     */
    HANDSHAKE("handshake"),

    /**
     * 获取指定的域名信息
     */
    GET_RELAYER_FOR_DOMAIN("getRelayerForDomain"),

    /**
     * 注册域名
     */
    REGISTER_DOMAIN("registerDomain"),

    /**
     * 更新指定域名
     */
    UPDATE_DOMAIN("updateDomain"),

    /**
     * 让relayer配置中心删除对应域名
     */
    DELETE_DOMAIN("deleteDomain");

    private final String code;

    public static RelayerRequestType parseFromValue(String value) {
        if (StrUtil.equals(value, GET_RELAYER_NODE_INFO.code)) {
            return GET_RELAYER_NODE_INFO;
        } else if (StrUtil.equals(value, GET_RELAYER_BLOCKCHAIN_INFO.code)) {
            return GET_RELAYER_BLOCKCHAIN_INFO;
        } else if (StrUtil.equals(value, AM_REQUEST.code)) {
            return AM_REQUEST;
        } else if (StrUtil.equals(value, HANDSHAKE.code)) {
            return HANDSHAKE;
        } else if (StrUtil.equals(value, GET_RELAYER_FOR_DOMAIN.code)) {
            return GET_RELAYER_FOR_DOMAIN;
        } else if (StrUtil.equals(value, REGISTER_DOMAIN.code)) {
            return REGISTER_DOMAIN;
        } else if (StrUtil.equals(value, UPDATE_DOMAIN.code)) {
            return UPDATE_DOMAIN;
        } else if (StrUtil.equals(value, DELETE_DOMAIN.code)) {
            return DELETE_DOMAIN;
        }
        throw new AntChainBridgeRelayerException(
                RelayerErrorCodeEnum.UNKNOWN_INTERNAL_ERROR,
                "Invalid value for relayer request type: " + value
        );
    }

    public static RelayerRequestType valueOf(Byte value) {
        switch (value) {
            case 0:
                return GET_RELAYER_NODE_INFO;
            case 1:
                return GET_RELAYER_BLOCKCHAIN_INFO;
            case 2:
                return AM_REQUEST;
            case 3:
                return HANDSHAKE;
            case 4:
                return GET_RELAYER_FOR_DOMAIN;
            case 5:
                return REGISTER_DOMAIN;
            case 6:
                return UPDATE_DOMAIN;
            case 7:
                return DELETE_DOMAIN;
            default:
                return null;
        }
    }
}
