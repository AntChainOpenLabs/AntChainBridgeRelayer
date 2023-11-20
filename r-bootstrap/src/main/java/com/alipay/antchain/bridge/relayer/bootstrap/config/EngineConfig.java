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

package com.alipay.antchain.bridge.relayer.bootstrap.config;

import java.util.Map;

import cn.hutool.core.map.MapUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.DistributedTaskTypeEnum;
import com.alipay.antchain.bridge.relayer.engine.core.ScheduleContext;
import com.alipay.antchain.bridge.relayer.engine.executor.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class EngineConfig {

    @Value("${relayer.engine.node_id_mode:IP}")
    private String nodeIdMode;

    @Bean
    public ScheduleContext scheduleContext() {
        return new ScheduleContext(nodeIdMode);
    }

    @Bean
    @Autowired
    public Map<DistributedTaskTypeEnum, BaseScheduleTaskExecutor> scheduleTaskExecutorMap(
            AnchorScheduleTaskExecutor anchorScheduleTaskExecutor,
            CommitterScheduleTaskExecutor committerScheduleTaskExecutor,
            ProcessScheduleTaskExecutor processScheduleTaskExecutor,
            TxConfirmScheduleTaskExecutor txConfirmScheduleTaskExecutor,
            ArchiveScheduleTaskExecutor archiveScheduleTaskExecutor,
            AsyncDeployScheduleTaskExecutor asyncDeployScheduleTaskExecutor
    ) {
        Map<DistributedTaskTypeEnum, BaseScheduleTaskExecutor> res = MapUtil.newHashMap();
        res.put(DistributedTaskTypeEnum.ANCHOR_TASK, anchorScheduleTaskExecutor);
        res.put(DistributedTaskTypeEnum.COMMIT_TASK, committerScheduleTaskExecutor);
        res.put(DistributedTaskTypeEnum.PROCESS_TASK, processScheduleTaskExecutor);
        res.put(DistributedTaskTypeEnum.AM_CONFIRM_TASK, txConfirmScheduleTaskExecutor);
        res.put(DistributedTaskTypeEnum.ARCHIVE_TASK, archiveScheduleTaskExecutor);
        res.put(DistributedTaskTypeEnum.DEPLOY_SERVICE_TASK, asyncDeployScheduleTaskExecutor);
        return res;
    }
}