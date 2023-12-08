package com.alipay.antchain.bridge.relayer.server.admin;

import java.util.HashMap;
import java.util.Map;

import com.alipay.antchain.bridge.relayer.server.admin.impl.BCDNSNamespace;
import com.alipay.antchain.bridge.relayer.server.admin.impl.BlockchainNamespace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NamespaceManager {

    private final Map<String, Namespace> namespaces = new HashMap<>();

    @Autowired
    public NamespaceManager(
            BlockchainNamespace blockchainNamespace,
            BCDNSNamespace bcdnsNamespace
    ) {
        namespaces.put("service", null);
        namespaces.put("blockchain", blockchainNamespace);
        namespaces.put("relayer", null);
        namespaces.put("bcdns", bcdnsNamespace);
    }

    public Namespace get(String name) {
        return namespaces.get(name);
    }
}
