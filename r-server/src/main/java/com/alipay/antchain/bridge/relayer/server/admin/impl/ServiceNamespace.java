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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.relayer.commons.model.CrossChainMsgACLItem;
import com.alipay.antchain.bridge.relayer.commons.model.PluginServerDO;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.IBBCPluginManager;
import com.alipay.antchain.bridge.relayer.core.manager.gov.IGovernManager;
import com.alipay.antchain.bridge.relayer.server.admin.AbstractNamespace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ServiceNamespace extends AbstractNamespace {

    @Resource
    private IGovernManager governManager;

    @Resource
    private IBBCPluginManager bbcPluginManager;

    public ServiceNamespace() {
        addCommand("addCrossChainMsgACL", this::addCrossChainMsgACL);
        addCommand("getCrossChainMsgACL", this::getCrossChainMsgACL);
        addCommand("deleteCrossChainMsgACL", this::deleteCrossChainMsgACL);

        addCommand("registerPluginServer", this::registerPluginServer);
        addCommand("stopPluginServer", this::stopPluginServer);
        addCommand("startPluginServer", this::startPluginServer);
    }

    Object addCrossChainMsgACL(String... args) {
        if (args.length != 4) {
            return "wrong number of arguments";
        }

        String grantDomain = args[0];
        String grantIdentity = args[1];
        String ownerDomain = args[2];
        String ownerIdentity = args[3];

        try {
            if (ObjectUtil.isNotEmpty(
                    governManager.getMatchedCrossChainACLItems(ownerDomain, ownerIdentity, grantDomain, grantIdentity)
            )) {
                return "already exist";
            }

            governManager.addCrossChainMsgACL(
                    new CrossChainMsgACLItem(
                            UUID.randomUUID().toString(),
                            ownerDomain,
                            ownerIdentity,
                            grantDomain,
                            grantIdentity,
                            0
                    )
            );
        } catch (Exception e) {
            log.error("failed to create crosschain ACL item : ( grant_domain: {}, grant_id: {}, owner_domain: {}, owner_id: {} ) ",
                    grantDomain, grantIdentity, ownerDomain, ownerIdentity, e);
            return "unexpected error : " + e.getMessage();
        }
        return "success";
    }

    Object getCrossChainMsgACL(String... args) {
        try {
            switch (args.length) {
                case 1:
                    String bizId = args[0];
                    CrossChainMsgACLItem item = governManager.getCrossChainMsgACL(bizId);
                    if (ObjectUtil.isNull(item)) {
                        return "not found";
                    }
                    return JSON.toJSONString(item);
                case 4:
                    String grantDomain = args[0];
                    String grantIdentity = args[1];
                    String ownerDomain = args[2];
                    String ownerIdentity = args[3];
                    List<CrossChainMsgACLItem> itemList = governManager.getMatchedCrossChainACLItems(ownerDomain, ownerIdentity, grantDomain, grantIdentity);
                    if (ObjectUtil.isNotEmpty(itemList)) {
                        return "your input matched ACL rules : "
                                + StrUtil.join(",", itemList.stream().map(CrossChainMsgACLItem::getBizId).collect(Collectors.toList()));
                    }
                    return "not found";
                default:
                    return "wrong number of arguments";
            }
        } catch (Exception e) {
            log.error("failed to get crosschain ACL item with args {}", ArrayUtil.join(args, ","), e);
            return "unexpected error : " + e.getMessage();
        }
    }

    Object deleteCrossChainMsgACL(String... args) {
        try {
            if (args.length == 1) {
                String bizId = args[0];
                governManager.delCrossChainMsgACL(bizId);
                return "success";
            }
            return "wrong number of arguments";
        } catch (Exception e) {
            log.error("failed to get crosschain ACL item with args {}", ArrayUtil.join(args, ","), e);
            return "unexpected error : " + e.getMessage();
        }
    }

    Object registerPluginServer(String... args) {
        if (args.length != 3) {
            return "wrong number of arguments";
        }

        String pluginServerId = args[0];
        String address = args[1];
        String pluginServerCAPath = args[2];
        try {
            byte[] caFile = Files.readAllBytes(Paths.get(pluginServerCAPath));
            PluginServerDO.PluginServerProperties properties = new PluginServerDO.PluginServerProperties();
            properties.setPluginServerCert(new String(caFile));
            bbcPluginManager.registerPluginServer(pluginServerId, address, properties.toString());
        } catch (Exception e) {
            log.error("failed to register plugin server: ", e);
            return "get some exception: " + e.getMessage();
        }

        return "success";
    }

    Object stopPluginServer(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }

        String pluginServerId = args[0];

        try {
            bbcPluginManager.stopPluginServer(pluginServerId);
        } catch (Exception e) {
            log.error("failed to register plugin server: ", e);
            return "get some exception: " + e.getMessage();
        }

        return "success";
    }

    Object startPluginServer(String... args) {
        if (args.length != 1) {
            return "wrong number of arguments";
        }

        String pluginServerId = args[0];

        try {
            bbcPluginManager.startPluginServer(pluginServerId);
        } catch (Exception e) {
            log.error("failed to register plugin server: ", e);
            return "get some exception: " + e.getMessage();
        }

        return "success";
    }
}
