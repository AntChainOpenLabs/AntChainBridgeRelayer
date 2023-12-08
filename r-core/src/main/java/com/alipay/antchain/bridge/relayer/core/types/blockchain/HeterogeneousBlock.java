package com.alipay.antchain.bridge.relayer.core.types.blockchain;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageFactory;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageTrustLevelEnum;
import com.alipay.antchain.bridge.commons.core.am.AuthMessageV2;
import com.alipay.antchain.bridge.commons.core.am.IAuthMessage;
import com.alipay.antchain.bridge.commons.core.base.CrossChainDomain;
import com.alipay.antchain.bridge.commons.core.base.CrossChainMessage;
import com.alipay.antchain.bridge.commons.core.base.UniformCrosschainPacket;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgProcessStateEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.AuthMsgTrustLevelEnum;
import com.alipay.antchain.bridge.relayer.commons.constant.UniformCrosschainPacketStateEnum;
import com.alipay.antchain.bridge.relayer.commons.model.AuthMsgWrapper;
import com.alipay.antchain.bridge.relayer.commons.model.UniformCrosschainPacketContext;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@NoArgsConstructor
public class HeterogeneousBlock extends AbstractBlock {

    @JSONField
    private List<UniformCrosschainPacketContext> uniformCrosschainPacketContexts;

    @JSONField
    private String domain;

    public HeterogeneousBlock(String product, String domain, String blockchainId, Long height, List<CrossChainMessage> crossChainMessages) {
        super(
                product,
                blockchainId,
                height
        );
        this.domain = domain;
        this.uniformCrosschainPacketContexts = crossChainMessages.stream()
                .map(
                        crossChainMessage -> {
                            UniformCrosschainPacketStateEnum stateEnum = UniformCrosschainPacketStateEnum.PENDING;
                            if (crossChainMessage.getType() == CrossChainMessage.CrossChainMessageType.AUTH_MSG) {
                                IAuthMessage authMessage = AuthMessageFactory.createAuthMessage(crossChainMessage.getMessage());
                                if (AuthMessageV2.MY_VERSION == authMessage.getVersion()) {
                                    stateEnum = ((AuthMessageV2) authMessage).getTrustLevel() == AuthMessageTrustLevelEnum.ZERO_TRUST ?
                                            UniformCrosschainPacketStateEnum.PROVED : UniformCrosschainPacketStateEnum.PENDING;
                                }
                            }
                            UniformCrosschainPacketContext ucpContext = new UniformCrosschainPacketContext();
                            ucpContext.setUcp(
                                    UniformCrosschainPacket.builder()
                                            .srcDomain(new CrossChainDomain(domain))
                                            .srcMessage(crossChainMessage)
                                            .build()
                            );
                            ucpContext.setFromNetwork(false);
                            ucpContext.setProduct(getProduct());
                            ucpContext.setBlockchainId(getBlockchainId());
                            ucpContext.setProcessState(stateEnum);
                            return ucpContext;
                        }
                ).collect(Collectors.toList());
    }

    @Override
    public byte[] encode() {
        return JSON.toJSONBytes(this);
    }

    @Override
    public void decode(byte[] data) {
        BeanUtil.copyProperties(JSON.parseObject(data, HeterogeneousBlock.class), this);
    }

    public List<AuthMsgWrapper> toAuthMsgWrappers() {
        return this.uniformCrosschainPacketContexts.stream()
                .filter(ucpContext -> ucpContext.getUcp().getSrcMessage().getType() == CrossChainMessage.CrossChainMessageType.AUTH_MSG)
                .map(
                        ucpContext -> {
                            CrossChainMessage crossChainMessage = ucpContext.getUcp().getSrcMessage();
                            AuthMsgWrapper wrapper = AuthMsgWrapper.buildFrom(
                                    getProduct(),
                                    getBlockchainId(),
                                    domain,
                                    ucpContext.getUcpId(),
                                    crossChainMessage
                            );

                            wrapper.addLedgerInfo(
                                    AuthMsgWrapper.AM_BLOCK_HEIGHT,
                                    Long.toString(getHeight())
                            );
                            wrapper.addLedgerInfo(
                                    AuthMsgWrapper.AM_BLOCK_HASH,
                                    HexUtil.encodeHexStr(crossChainMessage.getProvableData().getBlockHash())
                            );
                            wrapper.addLedgerInfo(
                                    AuthMsgWrapper.AM_BLOCK_TIMESTAMP,
                                    String.valueOf(crossChainMessage.getProvableData().getTimestamp())
                            );
                            wrapper.addLedgerInfo(
                                    AuthMsgWrapper.AM_CAPTURE_TIMESTAMP,
                                    String.valueOf(System.currentTimeMillis())
                            );
                            wrapper.addLedgerInfo(
                                    AuthMsgWrapper.AM_SENDER_GAS_USED,
                                    "0"
                            );
                            wrapper.addLedgerInfo(
                                    AuthMsgWrapper.AM_HINTS,
                                    StrUtil.EMPTY
                            );
                            wrapper.addLedgerInfo(
                                    AuthMsgWrapper.AM_TX_ID,
                                    HexUtil.encodeHexStr(crossChainMessage.getProvableData().getTxHash())
                            );
                            wrapper.addLedgerInfo(
                                    AuthMsgWrapper.AM_HETEROGENEOUS_RANDOM_UUID,
                                    UUID.randomUUID().toString()
                            );
                            wrapper.setProcessState(
                                    wrapper.getTrustLevel() == AuthMsgTrustLevelEnum.ZERO_TRUST ?
                                            AuthMsgProcessStateEnum.PROVED : AuthMsgProcessStateEnum.PENDING
                            );

                            return wrapper;
                        }
                ).collect(Collectors.toList());
    }
}
