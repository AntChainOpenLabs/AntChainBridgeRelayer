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

package com.alipay.antchain.bridge.relayer.bootstrap.config;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import com.alipay.antchain.bridge.relayer.commons.model.BlockchainMeta;
import com.alipay.antchain.bridge.relayer.commons.model.DomainCertWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemCacheConfig {

    @Value("${relayer.cache.domain_cert.ttl:10000}")
    private long domainCertCacheTTL;

    @Value("${relayer.cache.domain_cert.ttl:3000}")
    private long blockchainMetaCacheTTL;

    @Value("${relayer.cache.system_conf.ttl:30000}")
    private long systemConfigCacheTTL;

    @Bean
    public Cache<String, DomainCertWrapper> domainCertWrapperCache() {
        return CacheUtil.newLRUCache(30, domainCertCacheTTL);
    }

    @Bean
    public Cache<String, BlockchainMeta> blockchainMetaCache() {
        return CacheUtil.newLRUCache(30, blockchainMetaCacheTTL);
    }

    @Bean(name = "blockchainIdToDomainCache")
    public Cache<String, String> blockchainIdToDomainCache() {
        return CacheUtil.newLRUCache(30);
    }

    @Bean(name = "systemConfigCache")
    public Cache<String, String> systemConfigCache() {
        return CacheUtil.newLRUCache(10, systemConfigCacheTTL);
    }
}
