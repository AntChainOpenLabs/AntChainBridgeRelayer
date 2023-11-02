/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.relayer.commons.model;

import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.collections4.trie.PatriciaTrie;

@Getter
@Setter
@FieldNameConstants
public class RelayerBlockchainContent {

    public static RelayerBlockchainContent decodeFromJson(String jsonStr) {
        JSONObject jsonObject = JSON.parseObject(jsonStr);
        Map<String, RelayerBlockchainInfo> relayerBlockchainInfoMap = jsonObject.getJSONObject(Fields.relayerBlockchainInfoTrie)
                .entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> RelayerBlockchainInfo.decode((String) entry.getValue())
                ));
        Map<String, AbstractCrossChainCertificate> trustRootCertMap = jsonObject.getJSONObject(Fields.trustRootCertTrie)
                .entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> CrossChainCertificateFactory.createCrossChainCertificate(Base64.decode((String) entry.getValue()))
                ));
        return new RelayerBlockchainContent(
                relayerBlockchainInfoMap,
                trustRootCertMap
        );
    }

    private PatriciaTrie<RelayerBlockchainInfo> relayerBlockchainInfoTrie;

    private PatriciaTrie<AbstractCrossChainCertificate> trustRootCertTrie;

    public RelayerBlockchainContent(
            Map<String, RelayerBlockchainInfo> relayerBlockchainInfoMap,
            Map<String, AbstractCrossChainCertificate> trustRootCertMap
    ) {
        relayerBlockchainInfoTrie = new PatriciaTrie<>(
                relayerBlockchainInfoMap.entrySet().stream()
                        .map(entry -> MapUtil.entry(StrUtil.reverse(entry.getKey()), entry.getValue()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        ))
        );
        trustRootCertTrie = new PatriciaTrie<>(
                trustRootCertMap.entrySet().stream()
                        .map(entry -> MapUtil.entry(StrUtil.reverse(entry.getKey()), entry.getValue()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        ))
        );
    }

    public String encodeToJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(
                Fields.relayerBlockchainInfoTrie,
                relayerBlockchainInfoTrie.entrySet().stream().collect(
                        Collectors.toMap(
                                entry -> StrUtil.reverse(entry.getKey()),
                                entry -> entry.getValue().encode()
                        )
                )
        );
        jsonObject.put(
                Fields.trustRootCertTrie,
                trustRootCertTrie.entrySet().stream().collect(
                        Collectors.toMap(
                                entry -> StrUtil.reverse(entry.getKey()),
                                entry -> entry.getValue().encode()
                        )
                )
        );
        return jsonObject.toJSONString();
    }

    public RelayerBlockchainInfo getRelayerBlockchainInfo(String domain) {
        return this.relayerBlockchainInfoTrie.get(StrUtil.reverse(domain));
    }

    public AbstractCrossChainCertificate getDomainSpaceCert(String domainSpace) {
        return this.trustRootCertTrie.get(StrUtil.reverse(domainSpace));
    }
}
