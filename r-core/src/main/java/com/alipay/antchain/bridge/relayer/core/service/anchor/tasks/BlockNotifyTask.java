package com.alipay.antchain.bridge.relayer.core.service.anchor.tasks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.relayer.core.service.anchor.context.AnchorProcessContext;
import com.alipay.antchain.bridge.relayer.core.service.anchor.workers.CrossChainMessageWorker;
import com.alipay.antchain.bridge.relayer.core.service.anchor.workers.BlockWorker;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlock;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * notify任务，用于处理区块里面的数据，将区块里的相关请求转发给核心引擎
 * <p>
 * anchor会处理链上多种合约，每种合约的处理进度可能不一样，故每种合约会有对应一个进度。
 * <p>
 * 该任务在处理的时候，会读取不同的合约进度条去处理。
 *
 * <pre>
 * NotifyTask类的结构：
 *
 * BlockNotifyTask
 *  |-- Set(contract) // 要处理的合约集合
 *  |     |-- notify_height // 每个合约有个已处理高度
 *  |     |-- set(worker) // 每种合约有对应的workers
 *  |
 *  |-- processBlock()
 *        // NotifyTask的主逻辑：每个合约读取对应的高度，批量读取区块，使用对应的workers去处理
 *        set(contract).each().getEssentialHeader(notify_height + 1).set(worker).each.process()
 *
 * </pre>
 */
@Getter
@Slf4j
public class BlockNotifyTask extends BlockBaseTask {

    /**
     * 合约的workers
     */
    private final Map<NotifyTaskTypeEnum, List<BlockWorker>> workersByTask = new HashMap<>();

    public BlockNotifyTask(
            AnchorProcessContext processContext
    ) {
        super(
                BlockTaskTypeEnum.NOTIFY,
                processContext
        );

        // AM合约需要解析请求及上链结果
        workersByTask.put(
                NotifyTaskTypeEnum.CROSSCHAIN_MSG_WORKER,
                ListUtil.toList(
                        new CrossChainMessageWorker(processContext)
                )
        );
    }

    @Override
    public void doProcess() {
        try {
            processBlock();
        } catch (Exception e) {
            throw new RuntimeException("process block fail.", e);
        }
    }

    private void processBlock() {

        // 每个合约读取对应的高度，批量读取区块，使用对应的workers去处理
        for (NotifyTaskTypeEnum notifyTaskType : workersByTask.keySet()) {

            // 如果合约还未部署，不进行该任务
            if (!ifDeployContract(notifyTaskType)) {
                log.info(
                        "blockchain {} has not deployed {} contract yeah, wait for it.",
                        getProcessContext().getBlockchainMeta().getMetaKey(), notifyTaskType.getCode()
                );
                continue;
            }

            // 查看已处理、未处理的区块
            long localBlockHeaderHeight = getLocalBlockHeaderHeight();
            long notifyBlockHeaderHeight = notifyTaskType == NotifyTaskTypeEnum.SYSTEM_WORKER ?
                    getSystemNotifyBlockHeaderHeight(notifyTaskType.getCode()) :
                    getNotifyBlockHeaderHeight(notifyTaskType.getCode());
            log.debug(
                    "blockchain {} notify task {} has localBlockHeaderHeight {} and notifyBlockHeaderHeight {} now",
                    getProcessContext().getBlockchainMeta().getMetaKey(),
                    notifyTaskType.getCode(),
                    localBlockHeaderHeight,
                    notifyBlockHeaderHeight
            );

            if (notifyBlockHeaderHeight >= localBlockHeaderHeight) {
                log.debug(
                        "height {} of notify task {} equals to local height {} for blockchain {}",
                        notifyBlockHeaderHeight,
                        notifyTaskType.getCode(),
                        localBlockHeaderHeight,
                        getProcessContext().getBlockchainMeta().getMetaKey()
                );
                continue;
            }

            // 批量处理
            // each process task will process the gap of local block header and notify header now.
            long endHeight = Math.min(
                    localBlockHeaderHeight,
                    notifyBlockHeaderHeight + getProcessContext().getNotifyBatchSize()
            );
            long currentHeight = notifyBlockHeaderHeight + 1;

            log.info(
                    "notify task {} for blockchain {} is processing from blockHeight {} to endHeight {}",
                    notifyTaskType.getCode(),
                    getProcessContext().getBlockchainMeta().getMetaKey(),
                    currentHeight,
                    endHeight
            );
            
            for (; currentHeight <= endHeight; ++currentHeight) {
                
                AbstractBlock block = getProcessContext().getBlockQueue().getBlockFromQueue(currentHeight);
                if (ObjectUtil.isNull(block)) {
                    log.error(
                            "blockchain {} notify task {} can't find block {} from block queue so skip the failed task",
                            getProcessContext().getBlockchainMeta().getMetaKey(),
                            notifyTaskType.getCode(),
                            currentHeight
                    );
                    break;
                }
                log.info(
                        "blockchain {} notify task {} is processing the block {}",
                        getProcessContext().getBlockchainMeta().getMetaKey(),
                        notifyTaskType.getCode(),
                        currentHeight
                );

                boolean processResult = true;

                // 责任链模式，一个区块交给各个worker各处理一遍，且都要处理成功
                // TODO 一个worker处理失败，会导致该区块会全部重做一遍，这样子worker可能会收到同一个区块多次，需要能有幂等处理能力，这点可以优化
                for (BlockWorker worker : workersByTask.get(notifyTaskType)) {
                    if (!worker.process(block)) {
                        log.error(
                                "worker {} process block failed: [ blockchain: {}, height: {} ]",
                                notifyTaskType,
                                getProcessContext().getBlockchainMeta().getMetaKey(),
                                currentHeight
                        );
                        processResult = false;
                        break;
                    }
                }

                // 处理成功，则持久化区块高度
                if (processResult) {
                    saveNotifyBlockHeaderHeight(notifyTaskType.getCode(), currentHeight);
                    log.info(
                            "successful to process block (height: {}) in notify task {} from chain (product: {}, blockchain_id: {})",
                            block.getHeight(),
                            notifyTaskType.getCode(),
                            block.getProduct(),
                            block.getBlockchainId()
                    );
                } else {
                    log.error(
                            "failed to process block (height: {}) in notify task {} from chain (product: {}, blockchain_id: {})",
                            block.getHeight(),
                            notifyTaskType.getCode(),
                            block.getProduct(),
                            block.getBlockchainId()
                    );
                    break;
                }
            }
        }
    }

    private boolean ifDeployContract(NotifyTaskTypeEnum taskType) {

        // 这里面用了processContext里的内存变量（合约地址）来判断是否已部署合约，所以需要为何该内存变量是最新的
        // （如果serviceManager部署了合约，anchorProcess的这个processContext相关变量也要更新）
        if (NotifyTaskTypeEnum.CROSSCHAIN_MSG_WORKER == taskType) {
            return getProcessContext().getBlockchainClient().ifHasDeployedAMClientContract();
        }
        return NotifyTaskTypeEnum.SYSTEM_WORKER == taskType;
    }
}