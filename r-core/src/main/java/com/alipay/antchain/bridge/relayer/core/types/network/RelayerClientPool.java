package com.alipay.antchain.bridge.relayer.core.types.network;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.annotation.Resource;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerCredentialManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.core.types.network.ws.WsSslFactory;
import com.alipay.antchain.bridge.relayer.core.types.network.ws.client.WSRelayerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RelayerClientPool implements IRelayerClientPool {

    @Resource
    private IRelayerCredentialManager relayerCredentialManager;

    @Resource(name = "wsRelayerClientThreadsPool")
    private ExecutorService wsRelayerClientThreadsPool;

    @Value("#{systemConfigRepository.defaultNetworkId}")
    private String defaultNetworkId;

    @Resource
    private WsSslFactory wsSslFactory;

    private final Map<String, RelayerClient> clientMap = MapUtil.newConcurrentHashMap();

    public RelayerClient getRelayerClient(RelayerNodeInfo remoteRelayerNodeInfo) {

        try {
            if (!clientMap.containsKey(remoteRelayerNodeInfo.getNodeId())) {
                WSRelayerClient client = new WSRelayerClient(
                        remoteRelayerNodeInfo,
                        relayerCredentialManager,
                        defaultNetworkId,
                        wsRelayerClientThreadsPool,
                        wsSslFactory.getSslContext().getSocketFactory()
                );
                client.startup();
                clientMap.put(remoteRelayerNodeInfo.getNodeId(), client);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format("failed to create relayer client for {}: ", remoteRelayerNodeInfo.getNodeId()),
                    e
            );
        }


        return clientMap.get(remoteRelayerNodeInfo.getNodeId());
    }

    public void addRelayerClient(String nodeId, RelayerClient client) {
        clientMap.put(nodeId, client);
    }
}
