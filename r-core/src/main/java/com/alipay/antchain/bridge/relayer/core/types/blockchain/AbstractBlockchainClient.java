package com.alipay.antchain.bridge.relayer.core.types.blockchain;

import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.IAMClientContract;
import com.alipay.antchain.bridge.relayer.core.manager.bbc.ISDPMsgClientContract;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public abstract class AbstractBlockchainClient {

    private BlockchainMeta blockchainMeta;

    private String domain;

    public abstract boolean start();

    public abstract boolean shutdown();

    public abstract boolean ifHasDeployedAMClientContract();

    public abstract long getLastBlockHeight();

    public abstract AbstractBlock getEssentialHeaderByHeight(long height);

    public abstract IAMClientContract getAMClientContract();

    public abstract ISDPMsgClientContract getSDPMsgClientContract();

    public abstract CrossChainMessageReceipt queryCommittedTxReceipt(String txhash);

    @Getter
    @Setter
    public static class SendResponseResult {
        private String txId;
        private boolean confirmed;
        private boolean success;
        private String errorCode;
        private String errorMessage;

        public SendResponseResult(String txId, boolean success, String errorCode, String errorMessage) {
            this.txId = txId;
            this.success = success;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public SendResponseResult(String txId, boolean confirmed, boolean success, String errorCode, String errorMessage) {
            this.txId = txId;
            this.confirmed = confirmed;
            this.success = success;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public SendResponseResult(String txId, boolean success, int errorCode, String errorMessage) {
            this(txId, success, String.valueOf(errorCode), errorMessage);
        }
    }
}