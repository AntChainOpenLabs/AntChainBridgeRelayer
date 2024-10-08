package com.alipay.antchain.bridge.relayer.facade.admin.types;

import java.util.Arrays;
import java.util.Objects;

import cn.hutool.core.util.*;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrossChainMsgACLItem {

    public static final String MEANS_ANY = "*";

    public static String getIdentityHex(String identity) {
        if (StrUtil.equals(MEANS_ANY, identity)) {
            return null;
        }
        if (NumberUtil.isOdd(identity.length()) || !HexUtil.isHexNumber(identity)) {
            return DigestUtil.sha256Hex(identity);
        } else {
            byte[] rawId = HexUtil.decodeHex(StrUtil.removePrefix(identity, "0x"));
            if (ObjectUtil.isNull(rawId) || rawId.length > 32) {
                throw new RuntimeException("Invalid identity over 32B: " + identity);
            } else if (rawId.length < 32) {
                byte[] data = new byte[32 - rawId.length];
                Arrays.fill(data, (byte) 0);
                return HexUtil.encodeHexStr(ArrayUtil.addAll(data, rawId));
            }
            return HexUtil.encodeHexStr(rawId);
        }
    }

    /**
     * 用户定义规则ID, bytes32 hex
     */
    private String bizId;

    /**
     * 接收消息的所在区块链域名
     */
    private String ownerDomain;

    /**
     * 接收者的身份，合约或者链码名字
     */
    private String ownerIdentity;

    /**
     * 接收者的身份，合约或者链码名字 hex32字符串
     */
    private String ownerIdentityHex;

    /**
     * 被授权的域名
     */
    private String grantDomain;

    /**
     * 发送消息的身份，合约或者链码名字
     */
    private String grantIdentity;

    /**
     * 发送消息的身份，合约或者链码名字 hex32字符串
     */
    private String grantIdentityHex;

    /**
     * 用于标记删除的标志位,1表示已删除，0表示未删除
     */
    private int isDeleted;

    public CrossChainMsgACLItem(
            String bizId,
            String ownerDomain, String ownerIdentity,
            String grantDomain, String grantIdentity,
            int isDeleted
    ) {
        this.bizId = bizId;
        this.ownerDomain = ownerDomain;
        this.ownerIdentity = ownerIdentity;
        this.ownerIdentityHex = getIdentityHex(ownerIdentity);
        this.grantDomain = grantDomain;
        this.grantIdentity = grantIdentity;
        this.grantIdentityHex = getIdentityHex(grantIdentity);
        this.isDeleted = isDeleted;
    }

    public CrossChainMsgACLItem(String ownerDomain, String ownerIdentity, String grantDomain, String grantIdentity) {
        this.ownerDomain = ownerDomain;
        this.ownerIdentity = ownerIdentity;
        this.ownerIdentityHex = getIdentityHex(ownerIdentity);
        this.grantDomain = grantDomain;
        this.grantIdentity = grantIdentity;
        this.grantIdentityHex = getIdentityHex(grantIdentity);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CrossChainMsgACLItem that = (CrossChainMsgACLItem) o;
        return isDeleted == that.isDeleted &&
                Objects.equals(bizId, that.bizId) &&
                Objects.equals(ownerDomain, that.ownerDomain) &&
                Objects.equals(ownerIdentity, that.ownerIdentity) &&
                Objects.equals(grantDomain, that.grantDomain) &&
                Objects.equals(grantIdentity, that.grantIdentity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bizId, ownerDomain, ownerIdentity, grantDomain, grantIdentity, isDeleted);
    }
}
