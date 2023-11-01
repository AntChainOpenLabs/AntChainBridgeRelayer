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
import java.util.List;
import java.util.Map;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateFactory;
import com.alipay.antchain.bridge.commons.bcdns.CrossChainCertificateTypeEnum;
import com.alipay.antchain.bridge.commons.bcdns.RelayerCredentialSubject;
import com.alipay.antchain.bridge.commons.bcdns.utils.ObjectIdentityUtil;
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
         * relayer节点是否要求ssl
         */
        public static String TLS_REQUIRED = "tls_required";

        /**
         * relayer node info在properties中记录上次本地和该relayer握手的时间
         */
        public static String LAST_TIME_HANDSHAKE = "last_handshake_time";

        public static String RELAYER_BLOCKCHAIN_CONTENT = "relayer_blockchain_content";

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

        public void setRelayerBlockchainContent(RelayerBlockchainContent content) {
            properties.put(RELAYER_BLOCKCHAIN_CONTENT, content.encodeToJson());
        }

        public String getRelayerBlockchainContentJson() {
            return properties.getOrDefault(RELAYER_BLOCKCHAIN_CONTENT, "");
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

    private AbstractCrossChainCertificate crossChainCertificate;

    private RelayerCredentialSubject relayerCredentialSubject;

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
    private RelayerBlockchainContent relayerBlockchainContent;

    //************************************************
    // 其他属性
    //************************************************

    private String sigAlgo;

    /**
     * Relayer对RelayerNodeInfo的签名
     */
    public byte[] signature;

    public RelayerNodeInfo(
            String nodeId,
            AbstractCrossChainCertificate crossChainCertificate,
            String sigAlgo,
            List<String> endpoints,
            List<String> domains
    ) {
        Assert.equals(CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE, crossChainCertificate.getType());
        this.nodeId = nodeId;
        this.crossChainCertificate = crossChainCertificate;
        this.relayerCredentialSubject = RelayerCredentialSubject.decode(crossChainCertificate.getCredentialSubject());
        this.sigAlgo = sigAlgo;
        this.endpoints = endpoints;
        this.domains = domains;
    }

    public RelayerNodeInfo(
            String nodeId,
            AbstractCrossChainCertificate crossChainCertificate,
            RelayerCredentialSubject relayerCredentialSubject,
            String sigAlgo,
            List<String> endpoints,
            List<String> domains
    ) {
        this.nodeId = nodeId;
        this.crossChainCertificate = crossChainCertificate;
        this.relayerCredentialSubject = relayerCredentialSubject;
        this.sigAlgo = sigAlgo;
        this.endpoints = endpoints;
        this.domains = domains;
    }

    /**
     * RelayerNodeInfo编码值，用于交换信息时私钥签名
     */
    public byte[] getEncode() {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(byteArrayOutputStream);

        try {
            stream.writeUTF(nodeId);
            stream.writeUTF(
                    Base64.encode(crossChainCertificate.encode())
            );

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
            RelayerNodeInfo info = new RelayerNodeInfo();
            info.setNodeId(stream.readUTF());
            info.setCrossChainCertificate(
                    CrossChainCertificateFactory.createCrossChainCertificate(
                            Base64.decode(stream.readUTF())
                    )
            );
            Assert.equals(
                    CrossChainCertificateTypeEnum.RELAYER_CERTIFICATE,
                    info.getCrossChainCertificate().getType()
            );
            info.setRelayerCredentialSubject(
                    RelayerCredentialSubject.decode(
                            info.getCrossChainCertificate().getCredentialSubject()
                    )
            );

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
            Signature verifier = Signature.getInstance(sigAlgo);
            verifier.initVerify(getPublicKey());
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
            Signature signer = Signature.getInstance(sigAlgo);
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
                    new PKCS8EncodedKeySpec(Base64.decode(key)));

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

    public RelayerBlockchainContent getRelayerBlockchainContent() {
        if (ObjectUtil.isNull(relayerBlockchainContent)) {
            relayerBlockchainContent = RelayerBlockchainContent.decodeFromJson(properties.getRelayerBlockchainContentJson());
        }
        return relayerBlockchainContent;
    }

    public void dumpBlockchainContent() {
        properties.setRelayerBlockchainContent(relayerBlockchainContent);
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

    public PublicKey getPublicKey() {
        return ObjectIdentityUtil.getPublicKeyFromSubject(
                relayerCredentialSubject.getApplicant(),
                relayerCredentialSubject.getSubjectInfo()
        );
    }
}