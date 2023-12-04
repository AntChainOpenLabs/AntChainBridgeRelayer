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

package com.alipay.antchain.bridge.relayer.core.service.domainrouter;

import java.security.Signature;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.commons.bcdns.utils.CrossChainCertificateUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.MarkDTTaskStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.MarkDTTaskTypeEnum;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.commons.exception.RelayerErrorCodeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.MarkDTTask;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerBlockchainContent;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerBlockchainInfo;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerCredentialManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.core.types.network.BaseRelayerClient;
import com.alipay.antchain.bridge.relayer.core.types.network.IRelayerClientPool;
import com.alipay.antchain.bridge.relayer.core.types.network.RelayerClient;
import com.alipay.antchain.bridge.relayer.core.types.network.response.HelloStartRespPayload;
import com.alipay.antchain.bridge.relayer.core.utils.ProcessUtils;
import com.alipay.antchain.bridge.relayer.dal.repository.IScheduleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Slf4j
public class DomainRouterQueryService {

    private static final String KEY_SEPARATOR = "^^";

    public static String generateDomainRouterQueryTaskUniqueKey(String senderDomain, String receiverDomain) {
        return StrUtil.format("{}{}{}", senderDomain, KEY_SEPARATOR, receiverDomain);
    }

    @Value("${relayer.service.domain_router.batch_size:8}")
    private int domainRouterBatchSize;

    @Resource
    private IScheduleRepository scheduleRepository;

    @Resource
    private IRelayerNetworkManager relayerNetworkManager;

    @Resource
    private IRelayerClientPool relayerClientPool;

    @Resource
    private IBCDNSManager bcdnsManager;

    @Resource
    private IRelayerCredentialManager relayerCredentialManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Value("#{scheduleContext.nodeId}")
    private String localNodeId;

    @Value("#{systemConfigRepository.defaultNetworkId}")
    private String defaultNetworkId;

    @Resource(name = "domainRouterScheduleTaskExecutorThreadsPool")
    private ExecutorService domainRouterScheduleTaskExecutorThreadsPool;

    private final Lock preventReentrantLock = new ReentrantLock();

    public void process() {
        if (!preventReentrantLock.tryLock()) {
            return;
        }

        try {
            List<MarkDTTask> markDTTasks = scheduleRepository.peekReadyMarkDTTask(
                    MarkDTTaskTypeEnum.DOMAIN_ROUTER_QUERY,
                    localNodeId,
                    domainRouterBatchSize
            );
            if (ObjectUtil.isEmpty(markDTTasks)) {
                return;
            }
            ProcessUtils.waitAllFuturesDone(
                    markDTTasks.stream().map(
                            markDTTask -> domainRouterScheduleTaskExecutorThreadsPool.submit(
                                    () -> processEachTask(markDTTask)
                            )
                    ).collect(Collectors.toList()),
                    log
            );
        } finally {
            preventReentrantLock.unlock();
        }
    }

    private void processEachTask(MarkDTTask task) {
        transactionTemplate.execute(
                new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        List<String> domains = StrUtil.split(task.getUniqueKey(), KEY_SEPARATOR);
                        String senderDomain = domains.get(0);
                        String destDomain = domains.get(1);

                        log.info("start to query domain router with channel {}-{} and handshake with relayer", senderDomain, destDomain);

                        if (ObjectUtil.isNotNull(relayerNetworkManager.findNetworkItemByDomainName(destDomain))) {
                            log.info("domain router already exist for {}", destDomain);
                            return;
                        }

                        DomainRouter domainRouter = bcdnsManager.getDomainRouter(destDomain);
                        if (ObjectUtil.isNull(domainRouter)) {
                            throw new RuntimeException(
                                    StrUtil.format(
                                            "found no domain router for channel {}-{} on all BCDNS",
                                            senderDomain, destDomain
                                    )
                            );
                        }

                        if (relayerNetworkManager.hasRemoteRelayerNode(
                                RelayerNodeInfo.calculateNodeId(domainRouter.getDestRelayer().getRelayerCert()))
                        ) {
                            processIfRelayerExistLocally(senderDomain, domainRouter);
                        } else {
                            processIfUnknownRelayer(senderDomain, domainRouter);
                        }

                        scheduleRepository.updateMarkDTTaskState(
                                task.getTaskType(),
                                task.getNodeId(),
                                task.getUniqueKey(),
                                MarkDTTaskStateEnum.DONE
                        );

                        log.info("successful to save domain router with channel {}-{} and handshake with relayer", senderDomain, destDomain);
                    }
                }
        );
    }

    private void processIfRelayerExistLocally(String senderDomain, DomainRouter domainRouter) {

        log.info("process domain router for channel {}-{} with relayer existed", senderDomain, domainRouter.getDestDomain().getDomain());

        RelayerNodeInfo nodeInfo = relayerNetworkManager.getRelayerNode(
                RelayerNodeInfo.calculateNodeId(domainRouter.getDestRelayer().getRelayerCert()),
                false
        );
        nodeInfo.getEndpoints().addAll(domainRouter.getDestRelayer().getNetAddressList());
        nodeInfo.setEndpoints(
                ListUtil.toList(new HashSet<>(nodeInfo.getEndpoints()).iterator())
        );

        buildCrossChainChannel(nodeInfo, senderDomain, domainRouter);
    }

    private void processIfUnknownRelayer(String senderDomain, DomainRouter domainRouter) {

        log.info("process domain router for channel {}-{} with relayer not existed", senderDomain, domainRouter.getDestDomain().getDomain());

        buildCrossChainChannel(
                helloWithRelayer(domainRouter),
                senderDomain,
                domainRouter
        );
    }

    private RelayerNodeInfo helloWithRelayer(DomainRouter domainRouter) {
        log.info(
                "hello with endpoints {} for domain {}",
                StrUtil.join(", ", domainRouter.getDestRelayer().getNetAddressList()),
                domainRouter.getDestDomain().getDomain()
        );

        try {
            RelayerClient relayerClient = relayerClientPool.createRelayerClient(domainRouter.getDestRelayer());
            return helloComplete(
                    helloStart(relayerClient, domainRouter),
                    relayerClient
            );
        } catch (Exception e) {
            throw new AntChainBridgeRelayerException(
                    RelayerErrorCodeEnum.SERVER_RELAYER_HELLO_ERROR,
                    e,
                    "hello with endpoints {} failed: ",
                    StrUtil.join(", ", domainRouter.getDestRelayer().getNetAddressList())
            );
        }
    }

    private HelloStartRespPayload helloStart(RelayerClient relayerClient, DomainRouter domainRouter) {
        byte[] myRand = RandomUtil.randomBytes(32);
        HelloStartRespPayload respPayload = relayerClient.helloStart(myRand, relayerCredentialManager.getLocalNodeId());
        if (ObjectUtil.isNull(respPayload)) {
            throw new RuntimeException("null resp of hello start from relayer endpoints : " + StrUtil.join(",", domainRouter.getDestRelayer().getNetAddressList()));
        }

        RelayerNodeInfo remoteNodeInfo = RelayerNodeInfo.decode(
                Base64.decode(respPayload.getRemoteNodeInfo())
        );
        log.info("hello start with relayer {}-[{}]", remoteNodeInfo.getNodeId(), StrUtil.join(", ", domainRouter.getDestRelayer().getNetAddressList()));

        if (
                bcdnsManager.validateCrossChainCertificate(
                        remoteNodeInfo.getRelayerCrossChainCertificate(),
                        respPayload.getDomainSpaceCertPath()
                )
        ) {
            throw new RuntimeException(
                    StrUtil.format(
                            "failed to verify the relayer {} 's cert {} with cert path [ {} ]",
                            remoteNodeInfo.getNodeId(),
                            remoteNodeInfo.getRelayerCrossChainCertificate().encodeToBase64(),
                            respPayload.getDomainSpaceCertPath().entrySet().stream()
                                    .map(entry -> StrUtil.format("{} : {}", entry.getKey(), entry.getValue().encodeToBase64()))
                                    .reduce((s1, s2) -> s1 + ", " + s2)
                                    .orElse("")
                    )
            );
        }

        try {
            Signature verifier = Signature.getInstance(respPayload.getSigAlgo());
            verifier.initVerify(
                    CrossChainCertificateUtil.getPublicKeyFromCrossChainCertificate(remoteNodeInfo.getRelayerCrossChainCertificate())
            );
            verifier.update(myRand);
            if (!verifier.verify(respPayload.getSig())) {
                throw new RuntimeException("not pass");
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format("failed to verify sig from relayer {} for rand: ( rand: {}, sig: {} )",
                            remoteNodeInfo.getNodeId(), HexUtil.encodeHexStr(myRand), HexUtil.encodeHexStr(respPayload.getSig()), e)
            );
        }

        relayerNetworkManager.addRelayerNode(remoteNodeInfo);
        ((BaseRelayerClient) relayerClient).setRemoteNodeInfo(remoteNodeInfo);

        return respPayload;
    }

    private RelayerNodeInfo helloComplete(HelloStartRespPayload respPayload, RelayerClient relayerClient) {
        RelayerNodeInfo remoteNodeInfo = ((BaseRelayerClient) relayerClient).getRemoteNodeInfo();
        log.info(
                "hello complete with relayer {}-[{}]",
                remoteNodeInfo.getNodeId(),
                StrUtil.join(", ", ((BaseRelayerClient) relayerClient).getRemoteNodeInfo().getEndpoints())
        );

        relayerClient.helloComplete(
                relayerNetworkManager.getRelayerNodeInfo(),
                bcdnsManager.getTrustRootCertChain(relayerCredentialManager.getLocalRelayerIssuerDomainSpace()),
                respPayload.getRand()
        );

        relayerClientPool.addRelayerClient(remoteNodeInfo.getNodeId(), relayerClient);

        return remoteNodeInfo;
    }

    private void buildCrossChainChannel(RelayerNodeInfo nodeInfo, String senderDomain, DomainRouter domainRouter) {
        log.info("channel start request with relayer {} for domain {}", nodeInfo.getNodeId(), domainRouter.getDestDomain());
        RelayerClient relayerClient = relayerClientPool.getRelayerClient(nodeInfo, domainRouter.getDestDomain().getDomain());

        RelayerBlockchainContent blockchainContent = relayerClient.channelStart(domainRouter.getDestDomain().getDomain());
        if (ObjectUtil.isNull(blockchainContent)) {
            throw new RuntimeException(
                    StrUtil.format("null relayer blockchain content returned from {}", nodeInfo.getNodeId())
            );
        }
        if (ObjectUtil.isNull(blockchainContent.getRelayerBlockchainInfo(domainRouter.getDestDomain().getDomain()))) {
            throw new RuntimeException(
                    StrUtil.format("null relayer blockchain info returned from {}", nodeInfo.getNodeId())
            );
        }

        relayerNetworkManager.validateAndSaveBlockchainContent(
                defaultNetworkId,
                nodeInfo,
                blockchainContent,
                false
        );

        RelayerBlockchainInfo relayerBlockchainInfo = relayerNetworkManager.getRelayerBlockchainInfo(senderDomain);
        if (ObjectUtil.isNull(relayerBlockchainInfo)) {
            throw new RuntimeException(StrUtil.format("null blockchain info found for domain {}", senderDomain));
        }

        log.info("channel complete request with relayer {} for domain {}", nodeInfo.getNodeId(), senderDomain);
        relayerClient.channelComplete(
                senderDomain,
                new RelayerBlockchainContent(
                        MapUtil.builder(senderDomain, relayerBlockchainInfo).build(),
                        bcdnsManager.getTrustRootCertChain(relayerBlockchainInfo.getDomainCert().getDomainSpace())
                )
        );

        log.info(
                "built channel ( send: {} , receive: {} ) with relayer {} that its ( relayer_node_id: {} )",
                senderDomain,
                domainRouter.getDestDomain().getDomain(),
                nodeInfo.getNodeId(),
                RelayerNodeInfo.calculateNodeId(domainRouter.getDestRelayer().getRelayerCert())
        );
    }
}
