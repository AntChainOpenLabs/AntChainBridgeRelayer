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

package com.alipay.antchain.bridge.relayer.commons.exception;

import lombok.Getter;

/**
 * Error code for {@code antchain-bridge-relayer}
 *
 * <p>
 *     The {@code errorCode} field supposed to be hex and has two bytes.
 *     First byte represents the space code for project.
 *     Last byte represents the specific error scenarios.
 * </p>
 *
 * <p>
 *     Space code interval for {@code antchain-bridge-commons} is from 00 to 3f.
 * </p>
 */
@Getter
public enum RelayerErrorCodeEnum {

    /**
     *
     */
    DAL_ANCHOR_HEIGHTS_ERROR("0101", "wrong heights state"),

    DAL_BLOCKCHAIN_ERROR("0102", "wrong blockchain state"),

    DAL_PLUGINSERVER_ERROR("0103", "wrong pluginserver state"),

    DAL_CROSSCHAIN_MSG_ERROR("0104", "crosschain message error"),

    DAL_RELAYER_NETWORK_ERROR("0105", "relayer net data error"),

    DAL_RELAYER_NODE_ERROR("0106", "relayer node data error"),

    DAL_DT_ACTIVE_NODE_ERROR("0107", "dt active node data error"),

    /**
     *
     */
    UNKNOWN_INTERNAL_ERROR("0001", "internal error");

    /**
     * Error code for errors happened in project {@code antchain-bridge-relayer}
     */
    private final String errorCode;

    /**
     * Every code has a short message to describe the error stuff
     */
    private final String shortMsg;

    RelayerErrorCodeEnum(String errorCode, String shortMsg) {
        this.errorCode = errorCode;
        this.shortMsg = shortMsg;
    }
}
