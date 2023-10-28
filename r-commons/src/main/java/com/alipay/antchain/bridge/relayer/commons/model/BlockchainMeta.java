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
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.relayer.commons.constant.AMServiceStatusEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainStateEnum;
import com.alipay.antchain.bridge.relayer.commons.utils.HeteroBBCContextDeserializer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockchainMeta {

    public static String createMetaKey(String product, String blockchainId) {
        return product + "_" + blockchainId;
    }

    @Getter
    @Setter
    public static class BlockchainProperties {

        public static BlockchainProperties decode(byte[] rawData) {
            return JSON.parseObject(rawData, BlockchainProperties.class);
        }

        @JSONField(name = "am_client_contract_address")
        private String amClientContractAddress;

        @JSONField(name = "sdp_msg_contract_address")
        private String sdpMsgContractAddress;

        @JSONField(name = "anchor_runtime_status")
        private BlockchainStateEnum anchorRuntimeStatus;

        @JSONField(name = "init_block_height")
        private Long initBlockHeight;

        @JSONField(name = "is_domain_registered")
        private Boolean isDomainRegistered;

        @JSONField(name = "heterogeneous_bbc_context", deserializeUsing = HeteroBBCContextDeserializer.class)
        private DefaultBBCContext bbcContext;

        @JSONField(name = "plugin_server_id")
        private String pluginServerId;

        @JSONField(name = "am_service_status")
        private AMServiceStatusEnum amServiceStatus;

        @JSONField(name = "extra_properties")
        private Map<String, String> extraProperties = MapUtil.newHashMap();

        public byte[] encode() {
            return JSON.toJSONBytes(this);
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
        this(product, blockchainId, alias, desc, BlockchainProperties.decode(rawProperties));
    }

    public BlockchainMeta(
            String product,
            String blockchainId,
            String alias,
            String desc,
            BlockchainProperties properties
    ) {
        this.product = product;
        this.blockchainId = blockchainId;
        this.alias = alias;
        this.desc = desc;
        this.properties = properties;
    }

    public String getMetaKey() {
        return createMetaKey(this.product, this.blockchainId);
    }
}
