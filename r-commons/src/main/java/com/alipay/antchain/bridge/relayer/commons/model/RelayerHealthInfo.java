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

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;

@Getter
@Setter
public class RelayerHealthInfo {

    private static long activateLength;

    private long lastActiveTime;

    private String nodeIpAddress;

    private int nodePort;

    private boolean active;

    public RelayerHealthInfo(
            String nodeIpAddress,
            int nodePort,
            long lastActiveTime
    ) {
        this.nodeIpAddress = nodeIpAddress;
        this.nodePort = nodePort;
        this.lastActiveTime = lastActiveTime;
        this.active = (System.currentTimeMillis() - lastActiveTime) <= activateLength;
    }

    @Value("${dt.node.activate.length:3000}")
    public void setActivateLength(long activateLength) {
        RelayerHealthInfo.activateLength = activateLength;
    }
}
