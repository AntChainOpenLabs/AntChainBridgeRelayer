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

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alipay.antchain.bridge.bcdns.types.base.DomainRouter;
import com.alipay.antchain.bridge.relayer.commons.constant.MarkDTTaskStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.MarkDTTaskTypeEnum;
import com.alipay.antchain.bridge.relayer.commons.model.MarkDTTask;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerBlockchainContent;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerBlockchainInfo;
import com.alipay.antchain.bridge.relayer.commons.model.RelayerNodeInfo;
import com.alipay.antchain.bridge.relayer.core.manager.bcdns.IBCDNSManager;
import com.alipay.antchain.bridge.relayer.core.manager.network.IRelayerNetworkManager;
import com.alipay.antchain.bridge.relayer.core.types.network.IRelayerClientPool;
import com.alipay.antchain.bridge.relayer.core.types.network.RelayerClient;
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
                        String destDomain = task.getUniqueKey();
                        if (ObjectUtil.isNotNull(relayerNetworkManager.findNetworkItemByDomainName(destDomain))) {
                            log.info("domain router already exist for {}", destDomain);
                            return;
                        }

                        DomainRouter domainRouter = bcdnsManager.getDomainRouter(destDomain);
                        if (ObjectUtil.isNull(domainRouter)) {
                            throw new RuntimeException("found no domain router on all BCDNS");
                        }

                        if (relayerNetworkManager.hasRemoteRelayerNodeInfoByCertId(domainRouter.getDestRelayer().getRelayerCertId())) {
                            processIfRelayerExistLocally(domainRouter);
                        } else {
                            processIfUnknownRelayer(domainRouter);
                        }

                        scheduleRepository.updateMarkDTTaskState(
                                task.getTaskType(),
                                task.getNodeId(),
                                task.getUniqueKey(),
                                MarkDTTaskStateEnum.READY
                        );
                    }
                }
        );
    }

    private void processIfRelayerExistLocally(DomainRouter domainRouter) {
        RelayerNodeInfo nodeInfo = relayerNetworkManager.getRemoteRelayerNodeInfoByCertId(
                domainRouter.getDestRelayer().getRelayerCertId()
        );
        nodeInfo.getEndpoints().addAll(domainRouter.getDestRelayer().getNetAddressList());
        nodeInfo.setEndpoints(
                ListUtil.toList(new HashSet<>(nodeInfo.getEndpoints()).iterator())
        );

        RelayerClient relayerClient = relayerClientPool.getRelayerClient(nodeInfo);
        RelayerBlockchainContent blockchainContent = relayerClient.getRelayerBlockchainInfo(domainRouter.getDestDomain().getDomain());
        if (ObjectUtil.isNull(blockchainContent)) {
            throw new RuntimeException(
                    StrUtil.format("null relayer blockchain content returned from {}", nodeInfo.getNodeId())
            );
        }
        RelayerBlockchainInfo blockchainInfo = blockchainContent.getRelayerBlockchainInfo(domainRouter.getDestDomain().getDomain());
        if (ObjectUtil.isNull(blockchainInfo)) {
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

        log.info(
                "relayer {} already exist for domain router ( dest_domain: {}, relayer_cert_id: {} )",
                nodeInfo.getNodeId(),
                domainRouter.getDestDomain().getDomain(),
                domainRouter.getDestRelayer().getRelayerCertId()
        );
    }

    private void processIfUnknownRelayer(DomainRouter domainRouter) {

    }
}
