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

package com.alipay.antchain.bridge.relayer.core.types.blockchain;

import java.util.Date;
import java.util.Map;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.alipay.antchain.bridge.relayer.commons.model.AnchorProcessHeights;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockchainAnchorProcess {

    private static final String ANCHOR_PROCESS_LATEST_HEIGHT = "polling";

    private static final String ANCHOR_PROCESS_SPV_HEIGHT = "notify_CONTRACT_SYSTEM";

    private static final String ANCHOR_PROCESS_DATA_REQ_HEIGHT = "notify_CONTRACT_ORACLE";

    private static final String ANCHOR_PROCESS_MSG_HEIGHT = "notify_CONTRACT_AM_CLIENT";

    public static BlockchainAnchorProcess convertFrom(AnchorProcessHeights heights) {
        BlockchainAnchorProcess process = new BlockchainAnchorProcess();

        for (Map.Entry<String, Long> entry : heights.getProcessHeights().entrySet()) {
            TaskBlockHeight taskBlockHeight = new TaskBlockHeight(
                    entry.getValue(),
                    DateUtil.format(
                            new Date(heights.getModifiedTimeMap().get(entry.getKey())),
                            DatePattern.NORM_DATETIME_PATTERN
                    )
            );
            switch (entry.getKey()) {
                case ANCHOR_PROCESS_LATEST_HEIGHT:
                    process.setLatestBlockHeight(taskBlockHeight);
                    break;
                case ANCHOR_PROCESS_SPV_HEIGHT:
                    process.setSpvTaskBlockHeight(taskBlockHeight);
                    break;
                case ANCHOR_PROCESS_DATA_REQ_HEIGHT:
                    process.setDataReqTaskBlockHeight(taskBlockHeight);
                    break;
                case ANCHOR_PROCESS_MSG_HEIGHT:
                    process.setCrosschainTaskBlockHeight(taskBlockHeight);
                    break;
            }
        }
        return process;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class TaskBlockHeight {
        private long height;
        private String gmtModified;
    }

    private TaskBlockHeight latestBlockHeight;

    private TaskBlockHeight spvTaskBlockHeight;

    private TaskBlockHeight dataReqTaskBlockHeight;

    private TaskBlockHeight crosschainTaskBlockHeight;
}
