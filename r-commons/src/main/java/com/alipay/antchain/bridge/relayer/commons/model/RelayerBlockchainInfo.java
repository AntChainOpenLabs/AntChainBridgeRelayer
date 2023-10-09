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

import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.collection.ListUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 区块链信息 <br>
 * relayer间握手交换的区块链信息，主要是区块链的跨链信任根
 *
 * <pre>
 *     // 区块链的跨链认证信任链
 *      {
 * 	        // 1. UDAG层信任根
 * 	        // 2. 证明转化层信任根
 * 	        // 3. AM层信任根
 *      }
 * </pre>
 */
public class RelayerBlockchainInfo {

    //**********************************
    // 1. UDAG层信任根
    //**********************************
    /**
     * 区块链域名的UDNS CA
     */
    private String udnsCA;

    /**
     * 区块链域名证书
     */
    private String domainCert;

    /**
     * UDNS，可选，如果有证明转化协议，可以使用SignUDNS
     */
    private String udns;

    //**********************************
    // 2. 证明转化层信任根
    //**********************************

    /**
     * tee oracle信任根：信任的SGX远程认证服务方IAS，配置IAS的X509证书
     */
    private String teeOracleIasCert;

    /**
     * tee oracle信任根：信任的SGXOracle程序版本，配置SGXOracle的开发者
     */
    private String teeOracleMRSigner;

    /**
     * tee oracle信任根：信任的SGXOracle程序版本，配置SGXOracle的代码摘要值
     */
    private String teeOracleMREnclave;

    /**
     * SGXOracle节点AVR，及其signUDNS
     * <pre>
     *  pair结构为(AVR, signUDNS)
     * </pre>
     */
    private List<List<String>> teeOracleProofs;

    //**********************************
    // 3. AM层信任根
    //**********************************

    /**
     * 信任的am合约，配置合约的id
     */
    private List<String> amContractClientAddresses;

    /**
     * 链的拓展特征，比如mychain是否支持TEE等.
     * 方便跨链时候传递一些链的特性，在逻辑上适配不同的链。
     */
    private Map<String, String> chainFeatures;

    /**
     * json编码值
     */
    public String getEncode() {

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("udnsCA", this.udnsCA);
        jsonObject.put("domainCert", this.domainCert);
        jsonObject.put("udns", this.udns);

        jsonObject.put("teeOracleIasCert", this.teeOracleIasCert);
        jsonObject.put("teeOracleMRSigner", this.teeOracleMRSigner);
        jsonObject.put("teeOracleMREnclave", this.teeOracleMREnclave);

        JSONArray jsonArray = new JSONArray();
        for (List<String> proof : teeOracleProofs) {
            JSONObject teeOracleProofJson = new JSONObject();
            teeOracleProofJson.put("avr", proof.get(0));
            teeOracleProofJson.put("signUDNS", proof.get(1));

            jsonArray.add(teeOracleProofJson);
        }

        jsonObject.put("teeOracleProofs", jsonArray);

        jsonArray = new JSONArray();
        for (String address : amContractClientAddresses) {
            jsonArray.add(address);
        }

        jsonObject.put("amContractClientAddresses", jsonArray);
        jsonObject.put("chainFeatures", JSON.toJSONString(chainFeatures));

        return jsonObject.toJSONString();
    }

    /**
     * 从json解码
     *
     * @param jsonEncode
     * @return
     */
    public static RelayerBlockchainInfo decode(String jsonEncode) {

        JSONObject jsonObject = JSONObject.parseObject(jsonEncode);

        RelayerBlockchainInfo blockchainInfo = new RelayerBlockchainInfo();

        blockchainInfo.setUdnsCA(jsonObject.getString("udnsCA"));
        blockchainInfo.setDomainCert(jsonObject.getString("domainCert"));
        blockchainInfo.setUdns(jsonObject.getString("udns"));

        blockchainInfo.setTeeOracleIasCert(jsonObject.getString("teeOracleIasCert"));
        blockchainInfo.setTeeOracleMRSigner(jsonObject.getString("teeOracleMRSigner"));
        blockchainInfo.setTeeOracleMREnclave(jsonObject.getString("teeOracleMREnclave"));

        JSONArray jsonArray = jsonObject.getJSONArray("teeOracleProofs");

        List<List<String>> teeOracleProofs = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); ++i) {

            teeOracleProofs.add(ListUtil.toList(jsonArray.getJSONObject(i).getString("avr"),
                    jsonArray.getJSONObject(i).getString("signUDNS")));
        }

        blockchainInfo.setTeeOracleProofs(teeOracleProofs);

        List<String> addresses = new ArrayList<>();

        for (int i = 0; i < jsonObject.getJSONArray("amContractClientAddresses").size(); ++i) {
            addresses.add(jsonObject.getJSONArray("amContractClientAddresses").getString(i));
        }

        blockchainInfo.setAmContractClientAddresses(addresses);

        blockchainInfo.chainFeatures = new HashMap<>();
        JSONObject features = jsonObject.getJSONObject("chainFeatures");
        if (null != features) {
            features.forEach((s, o) -> blockchainInfo.chainFeatures.put(s, o.toString()));
        }

        return blockchainInfo;
    }

    public String getUdnsCA() {
        return udnsCA;
    }

    public void setUdnsCA(String udnsCA) {
        this.udnsCA = udnsCA;
    }

    public String getDomainCert() {
        return domainCert;
    }

    public void setDomainCert(String domainCert) {
        this.domainCert = domainCert;
    }

    public String getUdns() {
        return udns;
    }

    public void setUdns(String udns) {
        this.udns = udns;
    }

    public String getTeeOracleIasCert() {
        return teeOracleIasCert;
    }

    public Certificate getTeeOracleIasCertObject() {
        try {
            String rawCert = this.teeOracleIasCert;
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            if (rawCert.contains("%20")) {
                rawCert = URLDecoder.decode(rawCert, "UTF-8");
            }
            return certificateFactory.generateCertificate(new ByteArrayInputStream(rawCert.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("failed to decode certificate: ", e);
        }
    }

    public void setTeeOracleIasCert(String teeOracleIasCert) {
        this.teeOracleIasCert = teeOracleIasCert;
    }

    public String getTeeOracleMRSigner() {
        return teeOracleMRSigner;
    }

    public void setTeeOracleMRSigner(String teeOracleMRSigner) {
        this.teeOracleMRSigner = teeOracleMRSigner;
    }

    public String getTeeOracleMREnclave() {
        return teeOracleMREnclave;
    }

    public void setTeeOracleMREnclave(String teeOracleMREnclave) {
        this.teeOracleMREnclave = teeOracleMREnclave;
    }

    public List<String> getAmContractClientAddresses() {
        return amContractClientAddresses;
    }

    public void setAmContractClientAddresses(List<String> amContractClientAddresses) {
        this.amContractClientAddresses = amContractClientAddresses;
    }

    public void setTeeOracleProofs(
            List<List<String>> teeOracleProofs) {
        this.teeOracleProofs = teeOracleProofs;
    }

    public List<List<String>> getTeeOracleProofs() {
        return teeOracleProofs;
    }

    /**
     * Getter method for property <tt>chainFeatures</tt>.
     *
     * @return property value of chainFeatures
     */
    public Map<String, String> getChainFeatures() {
        return chainFeatures;
    }

    /**
     * 添加链的特性。
     * @param key
     * @param value
     */
    public void addChainFeature(String key, String value) {
        if (null == chainFeatures) {
            chainFeatures = new HashMap<>();
        }
        this.chainFeatures.put(key, value);
    }

    public void deleteChainFeature(String key) {
        this.chainFeatures.remove(key);
    }
}
