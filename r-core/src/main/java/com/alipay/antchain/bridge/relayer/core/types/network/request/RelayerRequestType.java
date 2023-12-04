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
    DELETE_DOMAIN("deleteDomain"),

    GET_RELAYER_BLOCKCHAIN_CONTENT("getRelayerBlockChainContent"),

    HELLO_START("helloStart"),

    HELLO_COMPLETE("helloComplete"),

    CROSSCHAIN_CHANNEL_START("crosschainChannelStart"),

    // TODO:
    //  发送没有且接收端也没有：先完成relayer 连接建立，然后对from链和to链建立channel，
    //   发送有而接收端没有：如果在消息发送之后，接收端发现本地没有发送链信息，就拒绝该消息，建立一个domainRouterQuery任务，
    //   将路由和链信息保存下来；不过发送端消息，会持续发不出去，可能会影响某条链发往其他链。
    CROSSCHAIN_CHANNEL_COMPLETE("crosschainChannelComplete"),

    QUERY_CROSSCHAIN_MSG_RECEIPT("queryCrossChainMsgReceipt");

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
        } else if (StrUtil.equals(value, GET_RELAYER_BLOCKCHAIN_CONTENT.code)) {
            return GET_RELAYER_BLOCKCHAIN_CONTENT;
        } else if (StrUtil.equals(value, HELLO_START.code)) {
            return HELLO_START;
        } else if (StrUtil.equals(value, HELLO_COMPLETE.code)) {
            return HELLO_COMPLETE;
        } else if (StrUtil.equals(value, CROSSCHAIN_CHANNEL_START.code)) {
            return CROSSCHAIN_CHANNEL_START;
        } else if (StrUtil.equals(value, CROSSCHAIN_CHANNEL_COMPLETE.code)) {
            return CROSSCHAIN_CHANNEL_COMPLETE;
        } else if (StrUtil.equals(value, QUERY_CROSSCHAIN_MSG_RECEIPT.code)) {
            return QUERY_CROSSCHAIN_MSG_RECEIPT;
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
            case 8:
                return GET_RELAYER_BLOCKCHAIN_CONTENT;
            case 9:
                return HELLO_START;
            case 10:
                return HELLO_COMPLETE;
            case 11:
                return CROSSCHAIN_CHANNEL_START;
            case 12:
                return CROSSCHAIN_CHANNEL_COMPLETE;
            case 13:
                return QUERY_CROSSCHAIN_MSG_RECEIPT;
            default:
                return null;
        }
    }
}
