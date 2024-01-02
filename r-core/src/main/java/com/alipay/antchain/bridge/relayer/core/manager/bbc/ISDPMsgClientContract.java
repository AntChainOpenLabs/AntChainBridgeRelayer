package com.alipay.antchain.bridge.relayer.core.manager.bbc;

/**
 * 抽象的SDPMsgClient服务合约。
 * <p>
 * 不同的链有不同的实现。
 */
public interface ISDPMsgClientContract {

    /**
     * 设置AM协议合约
     *
     * @param amContract am协议合约地址
     * @return
     */
    void setAmContract(String amContract);

    /**
     * Query the sequence number of the cross-chain direction
     *
     * @param senderDomain
     * @param from
     * @param receiverDomain
     * @param to
     * @return long
     */
    long querySDPMsgSeqOnChain(String senderDomain, String from, String receiverDomain, String to);

    /**
     * 部署合约
     *
     * @param contractId
     * @return
     */
    void deployContract();
}