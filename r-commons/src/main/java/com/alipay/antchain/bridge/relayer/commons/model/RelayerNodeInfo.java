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

import java.io.*;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @TODO Need update
 */
@NoArgsConstructor
@Getter
@Setter
public class RelayerNodeInfo {

    @Getter
    @Setter
    public static class RelayerNodeProperties {
        /**
         * 本地对该relayer节点配置的信任根，使用oracleservice模型
         */
        public static String TRUSTED_SERVICE_ID = "trusted_service_id";

        /**
         * 节点的am合约地址
         */
        public static String RELAYER_BLOCKCHAIN_INFOS = "relayer_blockchain_infos";

        /**
         * relayer节点是否要求ssl
         */
        public static String TLS_REQUIRED = "tls_required";

        /**
         * relayer node info在properties中记录上次本地和该relayer握手的时间
         */
        public static String LAST_TIME_HANDSHAKE = "last_handshake_time";

        public static RelayerNodeProperties decodeFromJson(String json) {
            RelayerNodeProperties relayerNodeProperties = new RelayerNodeProperties();
            JSON.parseObject(json).getInnerMap()
                    .forEach((key, value) -> relayerNodeProperties.getProperties().put(key, (String) value));
            return relayerNodeProperties;
        }

        /**
         * Relayer其他属性
         */
        private final Map<String, String> properties = MapUtil.newHashMap();

        public String getRawRelayerBlockchainInfoJson() {
            return properties.getOrDefault(RELAYER_BLOCKCHAIN_INFOS, "");
        }

        public Map<String, RelayerBlockchainInfo> getRelayerBlockchainInfoMap() {
            String raw = getRawRelayerBlockchainInfoJson();
            if (StrUtil.isEmpty(raw)) {
                return MapUtil.newHashMap();
            }

            Map<String, RelayerBlockchainInfo> blockchainInfoMap = MapUtil.newHashMap();
            JSONObject jsonObject = JSONObject.parseObject(raw);
            for (String domain : jsonObject.keySet()) {
                blockchainInfoMap.put(domain, RelayerBlockchainInfo.decode(jsonObject.getString(domain)));
            }

            return blockchainInfoMap;
        }

        public void setRelayerBlockchainInfoMap(Map<String, RelayerBlockchainInfo> blockchainInfoMap) {
            JSONObject jsonObject = new JSONObject();
            for (String domain : blockchainInfoMap.keySet()) {
                jsonObject.put(domain, blockchainInfoMap.get(domain).getEncode());
            }
            properties.put(RELAYER_BLOCKCHAIN_INFOS, jsonObject.toJSONString());
        }

        public String getTrustedServiceId() {
            return properties.get(TRUSTED_SERVICE_ID);
        }

        public boolean isTLSRequired() {
            return BooleanUtil.toBoolean(properties.getOrDefault(TLS_REQUIRED, "true"));
        }

        public long getLastHandshakeTime() {
            return NumberUtil.parseLong(properties.get(LAST_TIME_HANDSHAKE), 0L);
        }

        public byte[] encode() {
            return JSON.toJSONBytes(properties);
        }
    }


    //************************************************
    // 基础属性
    //************************************************

    /**
     * 节点id，使用公钥hash的hex值，(不含'0x'前缀)
     */
    private String nodeId;

    /**
     * 公钥，X509 Public keyInfo格式公钥，使用Base64编码
     */
    private String nodePublicKey;

    /**
     * 节点接入点数组 "ip:port"
     */
    private List<String> endpoints = ListUtil.toList();

    /**
     * Relayer支持的domain数组
     */
    private List<String> domains = ListUtil.toList();

    /**
     * Relayer其他属性
     */
    private RelayerNodeProperties properties;

    /**
     * 从properties中的json解析出的RelayerBlockchainInfo
     * 用于缓存，重复利用，修改完后，需要dump回properties
     */
    public Map<String, RelayerBlockchainInfo> blockchainInfoMap = null;

    //************************************************
    // 其他属性
    //************************************************

    /**
     * Relayer对RelayerNodeInfo的签名
     */
    public byte[] signature;

    public RelayerNodeInfo(String nodeId, String nodePublicKey, List<String> endpoints,
                           List<String> domains) {
        this.nodeId = nodeId;
        this.nodePublicKey = nodePublicKey;
        this.endpoints = endpoints;
        this.domains = domains;
    }

    public RelayerNodeInfo(String nodeId, String nodePublicKey, String endpointsStr,
                           String domainsStr) {
        this.nodeId = nodeId;
        this.nodePublicKey = nodePublicKey;
        this.endpoints = ListUtil.toList(StrUtil.split(endpointsStr, "^"));
        this.domains = ListUtil.toList(StrUtil.split(domainsStr, "^"));
    }

    /**
     * RelayerNodeInfo编码值，用于交换信息时私钥签名
     */
    public byte[] getEncode() {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteArrayOutputStream);

        try {
            stream.writeUTF(nodeId);
            stream.writeUTF(nodePublicKey);

            stream.writeInt(endpoints.size());
            for (String endpoint : endpoints) {
                stream.writeUTF(endpoint);
            }

            stream.writeInt(domains.size());
            for (String domain : domains) {
                stream.writeUTF(domain);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return byteArrayOutputStream.toByteArray();
    }

    public static RelayerNodeInfo decode(byte[] encode) {

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(encode);
        DataInputStream stream = new DataInputStream(byteArrayInputStream);

        try {
            RelayerNodeInfo
                    info = new RelayerNodeInfo();
            info.setNodeId(stream.readUTF());
            info.setNodePublicKey(stream.readUTF());

            int endpointSize = stream.readInt();

            while (endpointSize > 0) {
                info.addEndpoint(stream.readUTF());
                endpointSize--;
            }

            int domainSize = stream.readInt();

            while (domainSize > 0) {
                info.addDomain(stream.readUTF());
                domainSize--;
            }

            return info;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 验签
     *
     * @return
     */
    public boolean verify() {

        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");

            PublicKey publicKey = factory.generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(nodePublicKey)));

            Signature verifier = Signature.getInstance("SHA256WithRSA");
            verifier.initVerify(publicKey);
            verifier.update(getEncode());

            return verifier.verify(signature);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 签名
     *
     * @param key
     */
    public void sign(PrivateKey key) {

        try {
            Signature signer = Signature.getInstance("SHA256WithRSA");
            signer.initSign(key);
            signer.update(getEncode());

            signature = signer.sign();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 签名
     *
     * @param key
     */
    public void sign(String key) {

        try {

            KeyFactory factory = KeyFactory.getInstance("RSA");

            PrivateKey privateKey = factory.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(key)));

            sign(privateKey);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void unmarshalProperties(String properties) {
        this.properties = RelayerNodeProperties.decodeFromJson(properties);
    }

    public String marshalProperties() {
        return JSON.toJSONString(properties.getProperties());
    }

    public void addProperty(String key, String value) {
        this.properties.getProperties().put(key, value);
    }

    public Map<String, RelayerBlockchainInfo> getBlockchainInfoMap() {
        if (ObjectUtil.isNull(blockchainInfoMap)) {
            blockchainInfoMap = properties.getRelayerBlockchainInfoMap();
        }
        return blockchainInfoMap;
    }

    public void setRelayerNodeInfos(Map<String, RelayerBlockchainInfo> map) {
        blockchainInfoMap = map;
        dumpBlockchainInfos();
    }

    public void addBlockchainInfo(String domain, RelayerBlockchainInfo info) {
        if (null == blockchainInfoMap) {
            getBlockchainInfoMap();
        }
        blockchainInfoMap.put(domain, info);
    }

    public void dumpBlockchainInfos() {
        properties.setRelayerBlockchainInfoMap(blockchainInfoMap);
    }

    public void addEndpoint(String endpoint) {
        this.endpoints.add(endpoint);
    }

    public void addDomain(String domain) {
        this.domains.add(domain);
    }

    public void addDomains(List<String> domains) {
        this.domains.addAll(domains);
    }

    public Long getLastTimeHandshake() {
        return properties.getLastHandshakeTime();
    }
}