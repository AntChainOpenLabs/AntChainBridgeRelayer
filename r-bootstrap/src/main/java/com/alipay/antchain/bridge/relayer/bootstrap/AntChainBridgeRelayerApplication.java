package com.alipay.antchain.bridge.relayer.bootstrap;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication(scanBasePackages = {"com.alipay.antchain.bridge.relayer"})
public class AntChainBridgeRelayerApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(AntChainBridgeRelayerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}