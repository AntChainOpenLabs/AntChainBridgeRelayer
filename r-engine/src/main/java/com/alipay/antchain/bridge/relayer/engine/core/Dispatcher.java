package com.alipay.antchain.bridge.relayer.engine.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.DistributedTaskTypeEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.PluginServerStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.ActiveNode;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.commons.model.DistributedTask;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.IBBCPluginManager;
import com.alipay.antchain.bridge.relayer.core.manager.blockchain.IBlockchainManager;
import com.alipay.antchain.bridge.relayer.dal.repository.IScheduleRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Dispatcher负责拆分区块链任务，并根据节点心跳表获取在线节点排值班表
 */
@Component
@Slf4j
public class Dispatcher {

    @Resource
    private IBlockchainManager blockchainManager;

    @Resource
    private IBBCPluginManager bbcPluginManager;

    @Resource
    private IScheduleRepository scheduleRepository;

    @Value("${relayer.engine.schedule.dispatcher.dt_task.time_slice:180000}")
    private long timeSliceLength;

    @Value("${relayer.engine.schedule.activate.ttl:3000}")
    private long nodeTimeToLive;

//    @Value("${relayer.engine.schedule.dispatcher.task_diff_map:{anchor:5, committer:3, process:2}}")
//    private Map<String, Integer> taskTypeDiffMap;

    public void dispatch() {
        Lock lock = getDistributeLock();
        if (!lock.tryLock()) {
            log.debug("not my dispatch lock.");
            return;
        }

        try {
            log.info("dispatch distributed tasks now.");

            // 运行的区块链
            List<BlockchainMeta> runningBlockchains = getRunningBlockchains();
            if (ObjectUtil.isEmpty(runningBlockchains)) {
                log.debug("empty running blockchains to dispatch");
                return;
            }

            // 拆分任务
            List<DistributedTask> allTasks = splitTask(runningBlockchains);

            // 剔除已分配过时间片的任务
            List<DistributedTask> tasksToDispatch = filterTasksInTimeSlice(allTasks);
            if (ObjectUtil.isEmpty(tasksToDispatch.isEmpty())) {
                log.info("empty tasks to dispatch");
                return;
            }

            // 获取在线节点
            List<ActiveNode> onlineNodes = getOnlineNode();
            log.info("size of online node : {}", onlineNodes.size());

            // 给剩余任务分配时间片
            doDispatch(onlineNodes, tasksToDispatch);
        } catch (Exception e) {
            log.error("failed to dispatch distributed task: ", e);
        } finally {
            lock.unlock();
        }
    }

    private Lock getDistributeLock() {
        return scheduleRepository.getDispatchLock();
    }

    @Synchronized
    private List<BlockchainMeta> getRunningBlockchains() {

        List<BlockchainMeta> blockchainMetas = blockchainManager.getAllServingBlockchains();
        if (ObjectUtil.isNull(blockchainMetas)) {
            return ListUtil.empty();
        }
        return blockchainMetas.stream().filter(
                blockchainMeta ->
                        PluginServerStateEnum.READY == bbcPluginManager.getPluginServerState(
                                blockchainMeta.getProperties().getPluginServerId()
                        )
        ).collect(Collectors.toList());
    }

    private List<DistributedTask> splitTask(List<BlockchainMeta> runningBlockchains) {
        return runningBlockchains.stream().map(
                blockchainMeta ->
                        ListUtil.toList(
                                new DistributedTask(
                                        DistributedTaskTypeEnum.ANCHOR_TASK,
                                        blockchainMeta.getProduct(),
                                        blockchainMeta.getBlockchainId()
                                ),
                                new DistributedTask(
                                        DistributedTaskTypeEnum.COMMIT_TASK,
                                        blockchainMeta.getProduct(),
                                        blockchainMeta.getBlockchainId()
                                ),
                                new DistributedTask(
                                        DistributedTaskTypeEnum.PROCESS_TASK,
                                        blockchainMeta.getProduct(),
                                        blockchainMeta.getBlockchainId()
                                ),
                                new DistributedTask(
                                        DistributedTaskTypeEnum.AM_CONFIRM_TASK,
                                        blockchainMeta.getProduct(),
                                        blockchainMeta.getBlockchainId()
                                ),
                                new DistributedTask(
                                        DistributedTaskTypeEnum.ARCHIVE_TASK,
                                        blockchainMeta.getProduct(),
                                        blockchainMeta.getBlockchainId()
                                ),
                                new DistributedTask(
                                        DistributedTaskTypeEnum.DEPLOY_SERVICE_TASK,
                                        blockchainMeta.getProduct(),
                                        blockchainMeta.getBlockchainId()
                                )
                        )
        ).reduce((a, b) -> {
            a.addAll(b);
            return a;
        }).get();
    }

    private List<DistributedTask> filterTasksInTimeSlice(List<DistributedTask> allTasks) {

        // to map
        Map<String, DistributedTask> allTasksMap = Maps.newHashMap();
        for (DistributedTask task : allTasks) {
            allTasksMap.put(task.getUniqueTaskKey(), task);
        }

        List<DistributedTask> timeSliceTasks = scheduleRepository.getAllDistributedTasks();
        // 如果是新任务，差入执行记录到DB
        Map<String, DistributedTask> newTaskMap = Maps.newHashMap(allTasksMap);
        for (DistributedTask existedTask : timeSliceTasks) {
            newTaskMap.remove(existedTask.getUniqueTaskKey());
            if (!existedTask.ifFinish(timeSliceLength)) {
                allTasksMap.remove(existedTask.getUniqueTaskKey());
            }
        }
        if (!newTaskMap.isEmpty()) {
            scheduleRepository.batchInsertDTTasks(ListUtil.toList(newTaskMap.values()));
        }

        return Lists.newArrayList(allTasksMap.values());
    }

    private List<ActiveNode> getOnlineNode() {
        List<ActiveNode> nodes = scheduleRepository.getAllActiveNodes();
        List<ActiveNode> onlineNodes = Lists.newArrayList();
        for (ActiveNode node : nodes) {
            if (node.ifActive(nodeTimeToLive)) {
                onlineNodes.add(node);
            }
        }
        return onlineNodes;
    }

    private void doDispatch(List<ActiveNode> nodes, List<DistributedTask> tasks) {
        Collections.shuffle(nodes);
        roundRobin(nodes, tasks);
        // TODO: give a better algorithm for balancing tasks
        scheduleRepository.batchUpdateDTTasks(tasks);
    }

//    private void averageDiffPerBlockchainForEachNode(List<ActiveNode> nodes, List<DistributedTask> tasks) {
//        Map<String, Map<String, Integer>> nodeCounterMap = nodes.stream().collect(Collectors.toMap(
//                ActiveNode::getNodeId,
//                node -> new HashMap<>()
//        ));
//        Map<String, ActiveNode> nodeMap = nodes.stream().collect(Collectors.toMap(
//                ActiveNode::getNodeId,
//                node -> node
//        ));
//
//        for (int i = 0; i < tasks.size(); ++i) {
//
//            DistributedTask task = tasks.get(i);
//            int diffNum = taskTypeDiffMap.getOrDefault(task.getTaskType().getCode(), 1);
//
//            getTheMinDiffSumForBlockchain(nodeCounterMap, )
//
//            ActiveNode node = nodes.get(i % nodes.size());
//            tasks.get(i).setNodeId(node.getNodeId());
//            tasks.get(i).setStartTime(System.currentTimeMillis());
//        }
//    }
//
//    private String getTheMinDiffSumForBlockchain(Map<String, Map<String, Integer>> nodeCounterMap, String blockchainId) {
//        String nodeId = "";
//        Integer minDiff = Integer.MAX_VALUE;
//        for (Map.Entry<String, Map<String, Integer>> entry : nodeCounterMap.entrySet()) {
//            Integer diff = entry.getValue().getOrDefault(blockchainId, 0);
//            if (diff < minDiff) {
//                nodeId = entry.getKey();
//                minDiff = diff;
//            }
//        }
//
//        return nodeId;
//    }
//
//    private int calculateTotalDiff(Map<String, Map<String, Integer>> nodeCounterMap, String nodeId) {
//        return nodeCounterMap.entrySet().stream()
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        entry -> entry.getValue().values().stream().reduce(Integer::sum).orElse(0)
//                )).getOrDefault(nodeId, 0);
//    }

    private void roundRobin(List<ActiveNode> nodes, List<DistributedTask> tasks) {
        Collections.shuffle(tasks);
        for (int i = 0; i < tasks.size(); ++i) {
            ActiveNode node = nodes.get(i % nodes.size());
            tasks.get(i).setNodeId(node.getNodeId());
            tasks.get(i).setStartTime(System.currentTimeMillis());
        }
    }
}
