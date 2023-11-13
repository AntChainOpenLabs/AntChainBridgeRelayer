package com.alipay.antchain.bridge.relayer.engine.core;

import java.net.InetAddress;
import java.util.UUID;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 定时任务框架上下文
 */
@Getter
public class ScheduleContext {

    public final static String NODE_ID_MODE_IP = "IP";

    public final static String NODE_ID_MODE_UUID = "UUID";

    private final String nodeIp;

    private final String nodeId;

    public ScheduleContext(String mode) {
        InetAddress localAddress = NetUtil.getLocalhost();
        if (ObjectUtil.isNull(localAddress)) {
            throw new RuntimeException("null local ip");
        }
        this.nodeIp = localAddress.getHostAddress();

        if (StrUtil.equalsIgnoreCase(mode, NODE_ID_MODE_IP)) {
            this.nodeId = this.nodeIp;
        } else {
            this.nodeId = UUID.randomUUID().toString();
        }
    }
}
