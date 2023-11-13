package com.alipay.antchain.bridge.relayer.core.service.deploy.task;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.relayer.commons.constant.OnChainServiceStatusEnum;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("amServiceAsyncTaskExecutor")
@Slf4j
public class AMServiceAsyncTaskExecutor extends AbstractAsyncTaskExecutor {

    @Override
    public boolean preCheck(BlockchainMeta blockchainMeta) {
        if (ObjectUtil.isNull(blockchainMeta)) {
            return false;
        }
        if (
                !blockchainMeta.isRunning()
                        || ObjectUtil.isNull(blockchainMeta.getProperties().getAmServiceStatus())
        ) {
            return false;
        }

        // 如果状态是am合约已部署，不需要执行
        // 其他状态需要继续推进
        return OnChainServiceStatusEnum.DEPLOY_FINISHED != blockchainMeta.getProperties().getAmServiceStatus();
    }

    @Override
    public boolean doAsyncTask(BlockchainMeta blockchainMeta) {

        // 如果是init状态，则部署合约
        if (OnChainServiceStatusEnum.INIT == blockchainMeta.getProperties().getAmServiceStatus()) {
            return processInitStatus(blockchainMeta);
        }

        return false;
    }

    /**
     * [部署am合约] --> [保存am合约地址] --> [修改状态为finish_deploy_am_contract] --> [落库]
     */
    public boolean processInitStatus(BlockchainMeta blockchainMeta) {

        log.info("processInitStatus {}-{}", blockchainMeta.getProduct(), blockchainMeta.getBlockchainId());
        try {
            getBlockchainManager().deployAMClientContract(
                    blockchainMeta.getProduct(),
                    blockchainMeta.getBlockchainId()
            );

            blockchainMeta.getProperties().setAmServiceStatus(OnChainServiceStatusEnum.DEPLOY_FINISHED);
            getBlockchainManager().updateBlockchainProperty(
                    blockchainMeta.getProduct(),
                    blockchainMeta.getBlockchainId(),
                    BlockchainMeta.BlockchainProperties.AM_SERVICE_STATUS,
                    blockchainMeta.getProperties().getAmServiceStatus().name()
            );
        } catch (Exception e) {
            log.error("failed to deploy AuthMessage contract for {}", blockchainMeta.getMetaKey(), e);
            return false;
        }
        return true;
    }
}
