package com.alipay.antchain.bridge.relayer.server.admin;

import java.util.HashMap;
import java.util.Map;

import com.alipay.antchain.bridge.relayer.server.admin.impl.BCDNSNamespace;
import com.alipay.antchain.bridge.relayer.server.admin.impl.BlockchainNamespace;
import com.alipay.antchain.bridge.relayer.server.admin.impl.RelayerNamespace;
import com.alipay.antchain.bridge.relayer.server.admin.impl.ServiceNamespace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NamespaceManager {

    private final Map<String, Namespace> namespaces = new HashMap<>();

    @Autowired
    public NamespaceManager(
            BlockchainNamespace blockchainNamespace,
            BCDNSNamespace bcdnsNamespace,
            ServiceNamespace serviceNamespace,
            RelayerNamespace relayerNamespace
    ) {
        namespaces.put("service", serviceNamespace);
        namespaces.put("blockchain", blockchainNamespace);
        namespaces.put("relayer", relayerNamespace);
        namespaces.put("bcdns", bcdnsNamespace);
    }

    public Namespace get(String name) {
        return namespaces.get(name);
    }
}
