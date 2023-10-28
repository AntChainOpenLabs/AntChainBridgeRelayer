package com.alipay.antchain.bridge.relayer.core.manager.bbc;

import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessageReceipt;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlock;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.AbstractBlockchainClient;
import com.alipay.antchain.bridge.relayer.core.types.blockchain.HeterogeneousBlock;
import com.alipay.antchain.bridge.relayer.core.types.pluginserver.IBBCServiceClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AMClientContractHeteroBlockchainImpl implements IAMClientContract {

    public static int RECEIVER_IDENTITY_BYTEARRAY_LEN = 32;
    
    public static int SENDER_DOMAIN_LEN = 128 + 1; // 1 byte for len ; 128 bytes for domain
    
    public static int FLAGS_LEN = 4;

    public static byte[] extractProofs(byte[] encodedAmMsgPkg) {
        int offset = RECEIVER_IDENTITY_BYTEARRAY_LEN + SENDER_DOMAIN_LEN + FLAGS_LEN;
        int proofs_len = encodedAmMsgPkg.length - offset;
        byte[] proofs = new byte[proofs_len];
        System.arraycopy(encodedAmMsgPkg, offset, proofs, 0, proofs_len);
        return proofs;
    }

    private IBBCServiceClient bbcServiceClient;

    public AMClientContractHeteroBlockchainImpl(IBBCServiceClient bbcServiceClient) {
        this.bbcServiceClient = bbcServiceClient;
    }

    @Override
    public AbstractBlockchainClient.SendResponseResult recvPkgFromRelayer(byte[] pkg, String serviceId) {
        try {
            CrossChainMessageReceipt receipt = bbcServiceClient.relayAuthMessage(extractProofs(pkg));
            if (ObjectUtil.isNull(receipt)) {
                return new AbstractBlockchainClient.SendResponseResult(
                        "",
                        false,
                        false,
                        "EMPTY RESP",
                        "EMPTY RESP"
                );
            }
            if (!receipt.isSuccessful()) {
                return new AbstractBlockchainClient.SendResponseResult(
                        receipt.getTxhash(),
                        false,
                        false,
                        "HETERO COMMIT FAILED",
                        receipt.getErrorMsg()
                );
            }

            log.info("successful to relay message to domain {} with tx {}", this.bbcServiceClient.getDomain(), receipt.getTxhash());
            return new AbstractBlockchainClient.SendResponseResult(
                    receipt.getTxhash(),
                    receipt.isConfirmed(),
                    receipt.isSuccessful(),
                    "SUCCESS",
                    receipt.getErrorMsg()
            );
        } catch (Exception e) {
            log.error("failed to relay message to {}", this.bbcServiceClient.getDomain(), e);
            return new AbstractBlockchainClient.SendResponseResult(
                    "",
                    false,
                    false,
                    "UNKNOWN INTERNAL ERROR",
                    "UNKNOWN INTERNAL ERROR"
            );
        }
    }

    @Override
    public boolean setProtocol(String protocolContract, String protocolType) {
        try {
            this.bbcServiceClient.setProtocol(protocolContract, protocolType);
        } catch (Exception e) {
            log.error("failed to set protocol {} of type {} for AM on blockchain {}",
                    protocolContract, protocolType, this.bbcServiceClient.getDomain(), e);
            return false;
        }
        return true;
    }

    @Override
    public boolean addRelayers(String relayer) {
        return true;
    }


    @Override
    public List<AuthMsgWrapper> parseAMRequest(AbstractBlock abstractBlock) {
        if (!(abstractBlock instanceof HeterogeneousBlock)) {
            throw new RuntimeException("Invalid abstract block type");
        }
        return ((HeterogeneousBlock) abstractBlock).toAuthMsgWrappers();
    }

    @Override
    public boolean deployContract(String contractId) {
        try {
            this.bbcServiceClient.setupAuthMessageContract();
        } catch (Exception e) {
            log.error("failed to setup AM contract on blockchain {}", this.bbcServiceClient.getDomain(), e);
            return false;
        }

        return true;
    }

}
