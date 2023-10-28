package com.alipay.antchain.bridge.relayer.core.types.blockchain;

import cn.hutool.core.codec.Base64;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AbstractBlock {

    private String product;

    private String blockchainId;

    private long height;

    private String blockBASE64Str;

    public int getRawBlockSize() {
        return Base64.decode(this.getBlockBASE64Str()).length;
    }
}
