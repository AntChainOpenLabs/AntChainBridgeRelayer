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
import cn.hutool.core.util.BooleanUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.relayer.commons.constant.AMServiceStatusEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainStateEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockchainMeta {

    @Getter
    @Setter
    public static class BlockchainProperties {

        public static String AM_CLIENT_CONTRACT_ADDRESS = "am_client_contract_address";

        public static String SDP_MSG_CONTRACT_ADDRESS = "sdp_msg_contract_address";

        public static String ANCHOR_RUNTIME_STATUS = "anchor_runtime_status";

        public static String INIT_BLOCK_HEIGHT = "init_block_height";

        public static String IS_DOMAIN_REGISTERED = "is_domain_registered";

        public static String HETEROGENEOUS_BBC_CONTEXT = "heterogeneous_bbc_context";

        public static String PLUGIN_SERVER_ID = "plugin_server_id";

        public static String AM_SERVICE_STATUS = "am_service_status";

        public static BlockchainProperties decode(byte[] rawData) {
            BlockchainProperties blockchainProperties = new BlockchainProperties();
            JSON.parseObject(new String(rawData)).getInnerMap()
                    .forEach((key, value) -> blockchainProperties.getProperties().put(key, (String) value));
            return blockchainProperties;
        }

        private Map<String, String> properties = MapUtil.newHashMap();

        public String getAmClientContractAddress() {
            return properties.get(AM_CLIENT_CONTRACT_ADDRESS);
        }

        public String getSdpMsgContractAddress() {
            return properties.get(SDP_MSG_CONTRACT_ADDRESS);
        }

        public BlockchainStateEnum getBlockchainState() {
            return BlockchainStateEnum.parseFromValue(properties.get(ANCHOR_RUNTIME_STATUS));
        }

        public Long getInitBlockHeight() {
            return Long.parseLong(properties.get(INIT_BLOCK_HEIGHT));
        }

        public boolean isDomainRegistered() {
            return BooleanUtil.toBoolean(properties.getOrDefault(IS_DOMAIN_REGISTERED, ""));
        }

        public String getHeterogeneousBbcContext() {
            return properties.get(HETEROGENEOUS_BBC_CONTEXT);
        }

        public String getPluginServerId() {
            return properties.get(PLUGIN_SERVER_ID);
        }

        public AMServiceStatusEnum getAMServiceStatus() {
            return AMServiceStatusEnum.valueOf(properties.get(AM_SERVICE_STATUS));
        }

        public String getExtraProperty(String key) {
            return properties.get(key);
        }

        public byte[] encode() {
            return JSON.toJSONBytes(properties);
        }
    }

    private String product;

    private String blockchainId;

    private String alias;

    private String desc;

    private BlockchainProperties properties;

    public BlockchainMeta(
            String product,
            String blockchainId,
            String alias,
            String desc,
            byte[] rawProperties
    ) {
        this.product = product;
        this.blockchainId = blockchainId;
        this.alias = alias;
        this.desc = desc;
        this.properties = BlockchainProperties.decode(rawProperties);
    }
}
