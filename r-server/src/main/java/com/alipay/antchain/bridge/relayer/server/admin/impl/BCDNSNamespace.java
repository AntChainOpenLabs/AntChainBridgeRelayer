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

package com.alipay.antchain.bridge.relayer.server.admin.impl;

import javax.annotation.Resource;

import com.alipay.antchain.bridge.bcdns.service.BCDNSTypeEnum;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.server.admin.AbstractNamespace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BCDNSNamespace extends AbstractNamespace {

    @Resource
    private IBCDNSManager bcdnsManager;

    public BCDNSNamespace() {
        addCommand("registerBCDNSService", this::registerBCDNSService);
        addCommand("stopBCDNSService", this::stopBCDNSService);
        addCommand("restartBCDNSService", this::restartBCDNSService);
    }

    Object registerBCDNSService(String... args) {
        if (args.length != 3 && args.length != 4) {
            return "wrong number of arguments";
        }
        try {
            String domainSpace = args[0];
            String bcdnsType = args[1];
            String propFile = args[2];

            bcdnsManager.registerBCDNSService(
                    domainSpace,
                    BCDNSTypeEnum.parseFromValue(bcdnsType),
                    propFile,
                    args.length == 4 ? args[3] : ""
            );

            return "success";
        } catch (Throwable e) {
            log.error("failed to register BCDNS for domain space {}", args[0], e);
            return "failed to register BCDNS: " + e.getMessage();
        }
    }

    Object stopBCDNSService(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }
        try {
            bcdnsManager.stopBCDNSService(args[0]);
            return "success";
        } catch (Throwable e) {
            log.error("failed to stop BCDNS for domain space {}", args[0], e);
            return "failed to stop BCDNS: " + e.getMessage();
        }
    }

    Object restartBCDNSService(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }
        try {
            bcdnsManager.restartBCDNSService(args[0]);
            return "success";
        } catch (Throwable e) {
            log.error("failed to restart BCDNS for domain space {}", args[0], e);
            return "failed to restart BCDNS: " + e.getMessage();
        }
    }
}
