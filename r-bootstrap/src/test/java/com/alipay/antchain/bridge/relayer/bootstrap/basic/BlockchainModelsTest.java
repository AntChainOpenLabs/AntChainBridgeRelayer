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

package com.alipay.antchain.bridge.relayer.bootstrap.basic;

import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.AMServiceStatusEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.BlockchainStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import org.junit.Assert;
import org.junit.Test;

public class BlockchainModelsTest {

    private static final String BLOCKCHAIN_META_EXAMPLE = "{\n" +
            "  \"init_block_height\" : \"13947633\",\n" +
            "  \"anchor_runtime_status\" : \"RUNNING\",\n" +
            "  \"sdp_msg_contract_address\" : \"0x098310f3921eb1f7488ee169298e92759caa4e14\",\n" +
            "  \"is_domain_registered\" : \"true\",\n" +
            "  \"heterogeneous_bbc_context\" : \"{\\\"am_contract\\\":{\\\"contractAddress\\\":\\\"0x72e82e6aa48fca141ceb5914382be199fa514f96\\\",\\\"status\\\":\\\"CONTRACT_READY\\\"},\\\"raw_conf\\\":\\\"eyJ0ZXN0IjoidGVzdCJ9\\\",\\\"sdp_contract\\\":{\\\"contractAddress\\\":\\\"0x098310f3921eb1f7488ee169298e92759caa4e14\\\",\\\"status\\\":\\\"CONTRACT_READY\\\"}}\",\n" +
            "  \"am_service_status\" : \"FINISH_DEPLOY_AM_CONTRACT\",\n" +
            "  \"am_client_contract_address\" : \"0x72e82e6aa48fca141ceb5914382be199fa514f96\",\n" +
            "  \"plugin_server_id\" : \"p-QYj86x8Zd\"\n" +
            "}";

    private static final String BLOCKCHAIN_META_EXAMPLE_OBJ = "{\n" +
            "    \"init_block_height\":\"13947633\",\n" +
            "    \"anchor_runtime_status\":\"RUNNING\",\n" +
            "    \"sdp_msg_contract_address\":\"0x098310f3921eb1f7488ee169298e92759caa4e14\",\n" +
            "    \"is_domain_registered\":\"true\",\n" +
            "    \"heterogeneous_bbc_context\":{\n" +
            "        \"am_contract\":{\n" +
            "            \"contractAddress\":\"0x72e82e6aa48fca141ceb5914382be199fa514f96\",\n" +
            "            \"status\":\"CONTRACT_READY\"\n" +
            "        },\n" +
            "        \"raw_conf\":\"eyJ0ZXN0IjoidGVzdCJ9\",\n" +
            "        \"sdp_contract\":{\n" +
            "            \"contractAddress\":\"0x098310f3921eb1f7488ee169298e92759caa4e14\",\n" +
            "            \"status\":\"CONTRACT_READY\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"am_service_status\":\"FINISH_DEPLOY_AM_CONTRACT\",\n" +
            "    \"am_client_contract_address\":\"0x72e82e6aa48fca141ceb5914382be199fa514f96\",\n" +
            "    \"plugin_server_id\":\"p-QYj86x8Zd\"\n" +
            "}";

    private static final String BLOCKCHAIN_META_EXAMPLE_OBJ_LESS_INFO = "{\n" +
            "    \"heterogeneous_bbc_context\":{\n" +
            "        \"am_contract\":{\n" +
            "            \"contractAddress\":\"0x72e82e6aa48fca141ceb5914382be199fa514f96\",\n" +
            "            \"status\":\"CONTRACT_READY\"\n" +
            "        },\n" +
            "        \"raw_conf\":\"eyJ0ZXN0IjoidGVzdCJ9\",\n" +
            "        \"sdp_contract\":{\n" +
            "            \"contractAddress\":\"0x098310f3921eb1f7488ee169298e92759caa4e14\",\n" +
            "            \"status\":\"CONTRACT_READY\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"plugin_server_id\":\"p-QYj86x8Zd\"\n" +
            "}";

    @Test
    public void testBlockchainPropertiesDeserialization() throws Exception {
        BlockchainMeta.BlockchainProperties properties = BlockchainMeta.BlockchainProperties.decode(BLOCKCHAIN_META_EXAMPLE.getBytes());
        Assert.assertEquals(BlockchainStateEnum.RUNNING, properties.getAnchorRuntimeStatus());
        Assert.assertEquals(AMServiceStatusEnum.FINISH_DEPLOY_AM_CONTRACT, properties.getAmServiceStatus());
        Assert.assertNotNull(properties.getBbcContext());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, properties.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testBlockchainPropertiesDeserializationObj() throws Exception {
        BlockchainMeta.BlockchainProperties properties = BlockchainMeta.BlockchainProperties.decode(BLOCKCHAIN_META_EXAMPLE_OBJ.getBytes());
        Assert.assertEquals(BlockchainStateEnum.RUNNING, properties.getAnchorRuntimeStatus());
        Assert.assertEquals(AMServiceStatusEnum.FINISH_DEPLOY_AM_CONTRACT, properties.getAmServiceStatus());
        Assert.assertNotNull(properties.getBbcContext());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, properties.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testBlockchainPropertiesDeserializationObjLessInfo() throws Exception {
        BlockchainMeta.BlockchainProperties properties = BlockchainMeta.BlockchainProperties.decode(BLOCKCHAIN_META_EXAMPLE_OBJ_LESS_INFO.getBytes());
        Assert.assertNull(properties.getAnchorRuntimeStatus());
        Assert.assertNotNull(properties.getBbcContext());
        Assert.assertEquals(ContractStatusEnum.CONTRACT_READY, properties.getBbcContext().getSdpContract().getStatus());
    }

    @Test
    public void testBlockchainPropertiesSerialization() throws Exception {
        BlockchainMeta.BlockchainProperties properties = BlockchainMeta.BlockchainProperties.decode(BLOCKCHAIN_META_EXAMPLE_OBJ.getBytes());

        System.out.println(new String(properties.encode()));
    }
}
