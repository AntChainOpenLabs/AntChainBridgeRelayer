package com.alipay.antchain.bridge.relayer.core.service.anchor.tasks;

import com.alipay.antchain.bridge.relayer.core.service.anchor.context.AnchorProcessContext;
import lombok.Getter;

@Getter
public abstract class BlockBaseTask {

    private final BlockTaskTypeEnum taskType;

    private final AnchorProcessContext processContext;

    public BlockBaseTask(
            BlockTaskTypeEnum taskName,
            AnchorProcessContext processContext
    ) {
        this.taskType = taskName;
        this.processContext = processContext;
    }

    public abstract void doProcess();

    public void saveRemoteBlockHeaderHeight(long height) {
        processContext.getBlockchainRepository().setAnchorProcessHeight(
                processContext.getBlockchainMeta().getProduct(),
                processContext.getBlockchainMeta().getBlockchainId(),
                BlockTaskTypeEnum.POLLING.getCode(),
                height
        );
    }

    protected long getRemoteBlockHeaderHeight() {
        return processContext.getBlockchainRepository().getAnchorProcessHeight(
                processContext.getBlockchainMeta().getProduct(),
                processContext.getBlockchainMeta().getBlockchainId(),
                BlockTaskTypeEnum.POLLING.getCode()
        );
    }

    protected long getLocalBlockHeaderHeight() {
        return Math.max(
                processContext.getBlockchainMeta().getProperties().getInitBlockHeight(),
                processContext.getBlockchainRepository().getAnchorProcessHeight(
                        processContext.getBlockchainMeta().getProduct(),
                        processContext.getBlockchainMeta().getBlockchainId(),
                        BlockTaskTypeEnum.SYNC.getCode()
                )
        );
    }

    protected void saveLocalBlockHeaderHeight(long height) {
        processContext.getBlockchainRepository().setAnchorProcessHeight(
                processContext.getBlockchainMeta().getProduct(),
                processContext.getBlockchainMeta().getBlockchainId(),
                BlockTaskTypeEnum.SYNC.getCode(),
                height
        );
    }

    public long getNotifyBlockHeaderHeight(String workerType) {
        return Math.max(
                processContext.getBlockchainRepository().getAnchorProcessHeight(
                        processContext.getBlockchainMeta().getProduct(),
                        processContext.getBlockchainMeta().getBlockchainId(),
                        BlockTaskTypeEnum.NOTIFY.toNotifyWorkerHeightType(workerType)
                ),
                processContext.getBlockchainMeta().getProperties().getInitBlockHeight()
        );
    }

    public long getSystemNotifyBlockHeaderHeight(String contract) {
        return Long.MAX_VALUE;
    }

    public void saveNotifyBlockHeaderHeight(String workerType, long height) {
        processContext.getBlockchainRepository().setAnchorProcessHeight(
                processContext.getBlockchainMeta().getProduct(),
                processContext.getBlockchainMeta().getBlockchainId(),
                BlockTaskTypeEnum.NOTIFY.toNotifyWorkerHeightType(workerType),
                height
        );
    }
}