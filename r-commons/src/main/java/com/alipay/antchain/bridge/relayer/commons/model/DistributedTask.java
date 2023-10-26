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
import com.alipay.antchain.bridge.relayer.commons.constant.DistributedTaskTypeEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DistributedTask {

    private String nodeId = StrUtil.EMPTY;

    private DistributedTaskTypeEnum taskType;

    private String blockchainProduct;

    private String blockchainId;

    private String ext = StrUtil.EMPTY;

    private long timeSlice = 0;

    private long timeSliceLength = 0;

    public DistributedTask(DistributedTaskTypeEnum taskType, String blockchainProduct, String blockchainId) {
        this.taskType = taskType;
        this.blockchainProduct = blockchainProduct;
        this.blockchainId = blockchainId;
    }

    public DistributedTask(
            String nodeId,
            DistributedTaskTypeEnum taskType,
            String blockchainProduct,
            String blockchainId,
            String ext,
            long timeSlice
    ) {
        this.nodeId = nodeId;
        this.taskType = taskType;
        this.blockchainProduct = blockchainProduct;
        this.blockchainId = blockchainId;
        this.ext = ext;
        this.timeSlice = timeSlice;
    }

    public boolean ifFinish(long timeSliceLength) {
        return (System.currentTimeMillis() - this.timeSlice) > timeSliceLength;
    }

    public boolean ifFinish() {
        return (System.currentTimeMillis() - this.timeSlice) > timeSliceLength;
    }

    public String getUniqueTaskKey() {
        return taskType.getCode() + "_" + blockchainId;
    }
}
