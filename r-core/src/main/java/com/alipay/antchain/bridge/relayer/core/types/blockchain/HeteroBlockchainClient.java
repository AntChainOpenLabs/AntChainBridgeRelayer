package com.alipay.antchain.bridge.relayer.core.types.blockchain;

import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.bbc.AbstractBBCContext;
import com.alipay.antchain.bridge.commons.bbc.DefaultBBCContext;
import com.alipay.antchain.bridge.commons.bbc.syscontract.ContractStatusEnum;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.AMClientContractHeteroBlockchainImpl;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.IAMClientContract;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.ISDPMsgClientContract;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.SDPMsgClientHeteroBlockchainImpl;
import com.alipay.antchain.bridge.relayer.core.types.pluginserver.IBBCServiceClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HeteroBlockchainClient extends AbstractBlockchainClient {

    private final IBBCServiceClient bbcClient;

    private final IAMClientContract amClientContract;

    private final ISDPMsgClientContract sdpMsgClient;

    public HeteroBlockchainClient(IBBCServiceClient bbcClient, BlockchainMeta blockchainMeta) {
        super(blockchainMeta, bbcClient.getDomain());
        this.bbcClient = bbcClient;
        this.amClientContract = new AMClientContractHeteroBlockchainImpl(bbcClient);
        this.sdpMsgClient = new SDPMsgClientHeteroBlockchainImpl(bbcClient);
    }

    @Override
    public boolean start() {

        DefaultBBCContext bbcContext = getBlockchainMeta().getProperties().getBbcContext();
        if (ObjectUtil.isNull(bbcContext)) {
            log.error(
                    "bbcContext is null for ( plugin_server: {}, product: {}, domain: {} )",
                    getBlockchainMeta().getProperties().getPluginServerId(),
                    bbcClient.getProduct(),
                    bbcClient.getDomain()
            );
            return false;
        }
        try {
            this.bbcClient.startup(bbcContext);
        } catch (Exception e) {
            log.error(
                    "failed to start heterogeneous blockchain client ( plugin_server: {}, product: {}, domain: {} )",
                    getBlockchainMeta().getProperties().getPluginServerId(),
                    bbcClient.getProduct(),
                    bbcClient.getDomain(),
                    e
            );
            return false;
        }

        return true;
    }

    @Override
    public boolean shutdown() {
        try {
            this.bbcClient.shutdown();
        } catch (Exception e) {
            log.error(
                    "failed to shutdown heterogeneous blockchain client ( plugin_server: {}, product: {}, domain: {} )",
                    getBlockchainMeta().getProperties().getPluginServerId(),
                    this.bbcClient.getProduct(),
                    this.bbcClient.getDomain(),
                    e
            );
            return false;
        }

        return true;
    }

    @Override
    public boolean ifHasDeployedAMClientContract() {
        if (checkIfHasAMDeployedLocally()) {
            return true;
        }
        return checkIfHasAMDeployedRemotely();
    }

    private boolean checkContractsStatus(AbstractBBCContext bbcContext) {
        if (ObjectUtil.isNull(bbcContext.getAuthMessageContract()) || ObjectUtil.isNull(bbcContext.getSdpContract())) {
            log.info(
                    "local bbc context for ( product: {}, domain: {} ) is incomplete",
                    getBlockchainMeta().getProduct(), getDomain()
            );
            return false;
        }

        boolean ifAMPrepared = ContractStatusEnum.CONTRACT_READY == bbcContext.getAuthMessageContract().getStatus();
        if (!ifAMPrepared) {
            log.info(
                    "AM contract of heterogeneous blockchain client ( product: {} , domain: {} ) is {} instead of ready",
                    getBlockchainMeta().getProduct(), getDomain(), bbcContext.getAuthMessageContract().getStatus()
            );
            return false;
        }
        boolean ifSDPPrepared = ContractStatusEnum.CONTRACT_READY == bbcContext.getSdpContract().getStatus();
        if (!ifSDPPrepared) {
            log.info(
                    "SDP contract of heterogeneous blockchain client ( product: {} , domain: {} ) is not ready",
                    getBlockchainMeta().getProduct(), getDomain()
            );
            return false;
        }
        return true;
    }

    private boolean checkIfHasAMDeployedLocally() {
        return checkContractsStatus(
                getBlockchainMeta().getProperties().getBbcContext()
        );
    }

    private boolean checkIfHasAMDeployedRemotely() {
        AbstractBBCContext bbcContext = this.bbcClient.getContext();
        if (ObjectUtil.isNull(bbcContext)) {
            log.error("get null bbc context for {}-{}", getBlockchainMeta().getProduct(), getDomain());
            return false;
        }
        getBlockchainMeta().getProperties().setBbcContext((DefaultBBCContext) bbcContext);
        return checkContractsStatus(bbcContext);
    }

    @Override
    public long getLastBlockHeight() {
        return this.bbcClient.queryLatestHeight();
    }

    @Override
    public AbstractBlock getEssentialHeaderByHeight(long height) {
        List<CrossChainMessage> messages = this.bbcClient.readCrossChainMessagesByHeight(height);
        return new HeterogeneousBlock(
                getBlockchainMeta().getProduct(),
                getDomain(),
                getBlockchainMeta().getBlockchainId(),
                height,
                messages
        );
    }

    @Override
    public IAMClientContract getAMClientContract() {
        return this.amClientContract;
    }

    @Override
    public ISDPMsgClientContract getSDPMsgClientContract() {
        return this.sdpMsgClient;
    }

    @Override
    public CrossChainMessageReceipt queryCommittedTxReceipt(String txhash) {
        return this.bbcClient.readCrossChainMessageReceipt(txhash);
    }

    public AbstractBBCContext queryBBCContext() {
        return this.bbcClient.getContext();
    }
}
