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

package com.alipay.antchain.bridge.relayer.server.network;

import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerCredentialManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.core.service.receiver.ReceiverService;
import com.alipay.antchain.bridge.relayer.dal.repository.ICrossChainMessageRepository;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;

@Getter
@Setter
@Slf4j
@NoArgsConstructor
public abstract class BaseRelayerServer {

    private IRelayerNetworkManager relayerNetworkManager;

    private IBCDNSManager bcdnsManager;

    private IRelayerCredentialManager relayerCredentialManager;

    private boolean isDiscoveryServer;

    private String defaultNetworkId;

    private ReceiverService receiverService;

    private ICrossChainMessageRepository crossChainMessageRepository;

    private RedissonClient redisson;

    public BaseRelayerServer(
            IRelayerNetworkManager relayerNetworkManager,
            IBCDNSManager bcdnsManager,
            IRelayerCredentialManager relayerCredentialManager,
            ReceiverService receiverService,
            ICrossChainMessageRepository crossChainMessageRepository,
            RedissonClient redisson,
            String defaultNetworkId,
            boolean isDiscoveryServer
    ) {
        this.relayerNetworkManager = relayerNetworkManager;
        this.bcdnsManager = bcdnsManager;
        this.relayerCredentialManager = relayerCredentialManager;
        this.receiverService = receiverService;
        this.crossChainMessageRepository = crossChainMessageRepository;
        this.defaultNetworkId = defaultNetworkId;
        this.isDiscoveryServer = isDiscoveryServer;
        this.redisson = redisson;
    }

    public void propagateCrossChainMsg(String domainName, String ucpId, String authMsg, String udagProof, String ledgerInfo) {
        receiverService.receiveOffChainAMRequest(domainName, ucpId, authMsg, udagProof, ledgerInfo);
    }
}
