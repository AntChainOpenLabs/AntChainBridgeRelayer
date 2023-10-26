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

package com.alipay.antchain.bridge.relayer.dal.entities;

import com.alipay.antchain.bridge.relayer.commons.constant.UniformCrosschainPacketStateEnum;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("ucp_pool")
public class UCPPoolEntity extends BaseEntity {

    @TableField("ucp_id")
    private byte[] ucpId;

    @TableField("blockchain_product")
    private String product;

    @TableField("blockchain_id")
    private String blockchainId;

    @TableField("version")
    private int version;

    @TableField("src_domain")
    private String srcDomain;

    @TableField("blockhash")
    private String blockHash;

    @TableField("txhash")
    private String txHash;

    @TableField("ledger_time")
    private long ledgerTime;

    @TableField("udag_path")
    private String udagPath;

    @TableField("protocol_type")
    private int protocolType;

    @TableField("raw_message")
    private byte[] rawMessage;

    @TableField("ptc_oid")
    private byte[] ptcOid;

    @TableField("tp_proof")
    private byte[] tpProof;

    @TableField("from_network")
    private boolean fromNetwork;

    @TableField("relayer_id")
    private String relayerId;

    @TableField("process_state")
    private UniformCrosschainPacketStateEnum processState;
}
