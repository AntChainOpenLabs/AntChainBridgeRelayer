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

create database if not exists relayer;
use relayer;

drop table if exists blockchain;
CREATE TABLE `blockchain`
(
    `id`            int(11) NOT NULL AUTO_INCREMENT,
    `product`       varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci   DEFAULT NULL,
    `blockchain_id` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `alias`         varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `description`   varchar(2096) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `properties`    blob,
    `gmt_create`    datetime                                                       DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`  datetime                                                       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_instance` (`blockchain_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC;

drop table if exists system_config;
CREATE TABLE `system_config`
(
    `id`           int(11) NOT NULL AUTO_INCREMENT,
    `conf_key`     varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `conf_value`   varchar(15000) COLLATE utf8mb4_general_ci                     DEFAULT NULL,
    `gmt_create`   datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `conf_key` (`conf_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;

drop table if exists anchor_process;
CREATE TABLE `anchor_process`
(
    `id`                 int(11) NOT NULL AUTO_INCREMENT,
    `blockchain_product` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `instance`           varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `task`               varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `block_height`       int(11)                                                       DEFAULT NULL,
    `gmt_create`         datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`       datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `blockchain_product` (`blockchain_product`, `instance`, `task`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC;

drop table if exists anchor_system_config;
CREATE TABLE `anchor_system_config`
(
    `id`           int(11) NOT NULL AUTO_INCREMENT,
    `conf_key`     varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `conf_value`   varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
    `gmt_create`   datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `conf_key` (`conf_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  ROW_FORMAT = DYNAMIC;

drop table if exists `domain_cert`;
CREATE TABLE `domain_cert`
(
    `id`                 int(11) NOT NULL AUTO_INCREMENT,
    `domain`             varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `blockchain_product` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `instance`           varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `subject_oid`        blob                                                          DEFAULT NULL,
    `issuer_oid`         blob                                                          DEFAULT NULL,
    `domain_space`       varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `domain_cert`        longblob,
    `gmt_create`         datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`       datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `domain` (`domain`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC;

drop table if exists `domain_cert_application`;
CREATE TABLE `domain_cert_application`
(
    `id`            INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `domain`        VARCHAR(128) UNIQUE NOT NULL,
    `domain_space`  VARCHAR(128)        NOT NULL,
    `apply_receipt` BINARY,
    `state`         VARCHAR(20),
    `gmt_create`    DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`  DATETIME DEFAULT CURRENT_TIMESTAMP
);

drop table if exists `domain_space_cert`;
CREATE TABLE `domain_space_cert`
(
    `id`                int(11) NOT NULL AUTO_INCREMENT,
    `domain_space`      varchar(128) DEFAULT NULL,
    `parent_space`      varchar(128) DEFAULT NULL,
    `owner_oid_hex`     BINARY  NOT NULL,
    `description`       varchar(128) DEFAULT NULL,
    `domain_space_cert` longblob,
    `gmt_create`        datetime     DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`      datetime     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `domain_space` (`domain_space`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
CREATE INDEX domain_space_cert_owner_oid_hex
    ON domain_space_cert (owner_oid_hex);

drop table if exists ucp_pool;
CREATE TABLE `ucp_pool`
(
    `id`                 int(11)              NOT NULL AUTO_INCREMENT,
    `ucp_id`             VARBINARY(64) UNIQUE NOT NULL,
    `blockchain_product` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci   DEFAULT NULL,
    `blockchain_id`      varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `version`            int(11)                                                        DEFAULT NULL,
    `src_domain`         varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `blockhash`          varchar(66)                                                    DEFAULT NULL,
    `txhash`             varchar(66)                                                    DEFAULT NULL,
    `ledger_time`        TIMESTAMP                                                      DEFAULT NULL,
    `udag_path`          varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `protocol_type`      int(11)                                                        DEFAULT NULL,
    `raw_message`        mediumblob,
    `ptc_oid`            VARBINARY(32),
    `tp_proof`           mediumblob,
    `from_network`       TINYINT(1)                                                     DEFAULT 0,
    `relayer_id`         varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci   DEFAULT NULL,
    `process_state`      varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci   DEFAULT NULL,
    `gmt_create`         datetime                                                       DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`       datetime                                                       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `ucp_state` (`process_state`),
    KEY `idx_srcdomain_processstate` (`src_domain`, `process_state`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC;

drop table if exists auth_msg_pool;
CREATE TABLE `auth_msg_pool`
(
    `id`                        int(11)              NOT NULL AUTO_INCREMENT,
    `ucp_id`                    VARBINARY(64) UNIQUE NOT NULL,
    `blockchain_product`        varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `blockchain_id`             varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `domain_name`               varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `amclient_contract_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `version`                   int(11)                                                       DEFAULT NULL,
    `msg_sender`                varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `protocol_type`             int(11)                                                       DEFAULT NULL,
    `trust_level`               int(11)                                                       DEFAULT 2,
    `payload`                   mediumblob,
    `process_state`             varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `ext`                       mediumblob,
    `gmt_create`                datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`              datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `state` (`process_state`),
    KEY `idx_domainname_trustlevel_processstate` (`domain_name`, `trust_level`, `process_state`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC;

drop table if exists sdp_msg_pool;
CREATE TABLE `sdp_msg_pool`
(
    `id`                          int(11) NOT NULL AUTO_INCREMENT,
    `auth_msg_id`                 int(11)                                                       DEFAULT NULL,
    `version`                     int(11)                                                       DEFAULT 1,
    `atomic`                      TINYINT(1)                                                    DEFAULT 0,
    `sender_blockchain_product`   varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `sender_instance`             varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `sender_domain_name`          varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `sender_identity`             varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `sender_amclient_contract`    varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `receiver_blockchain_product` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `receiver_instance`           varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `receiver_domain_name`        varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `receiver_identity`           varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `receiver_amclient_contract`  varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `msg_sequence`                int(11)                                                       DEFAULT NULL,
    `process_state`               varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `tx_hash`                     varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `tx_success`                  tinyint(1)                                                    DEFAULT NULL,
    `tx_fail_reason`              varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `gmt_create`                  datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`                datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_auth_msg_id` (`auth_msg_id`),
    KEY `idx_queue_tx_hash` (`tx_hash`),
    KEY `state` (`process_state`),
    KEY `idx_receiverinstance_processstate_receiverblockchainproduct` (`receiver_instance`, `process_state`, `receiver_blockchain_product`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC;

drop table if exists cross_chain_msg_acl;
CREATE TABLE `cross_chain_msg_acl`
(
    `id`                 int(11) NOT NULL AUTO_INCREMENT,
    `biz_id`             varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `owner_domain`       varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `owner_identity`     varchar(128)                                                  DEFAULT NULL,
    `owner_identity_hex` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `grant_domain`       varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `grant_identity`     varchar(128)                                                  DEFAULT NULL,
    `grant_identity_hex` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `is_deleted`         int(1)                                                        DEFAULT NULL,
    `gmt_create`         datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`       datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `biz_id` (`biz_id`),
    KEY `exact_valid_rules` (`owner_domain`, `owner_identity_hex`, `grant_domain`, `grant_identity_hex`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC;

drop table if exists relayer_network;
CREATE TABLE `relayer_network`
(
    `id`           int(11) NOT NULL AUTO_INCREMENT,
    `network_id`   varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `domain`       varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `node_id`      varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `sync_state`   varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `gmt_create`   datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_item` (`network_id`, `domain`, `node_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC;

drop table if exists relayer_node;
CREATE TABLE `relayer_node`
(
    `id`                   int(11) NOT NULL AUTO_INCREMENT,
    `node_id`              varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci   DEFAULT NULL,
    `node_crosschain_cert` binary                                                         DEFAULT NULL,
    `node_sig_algo`        varchar(255)                                                   DEFAULT NULL,
    `domains`              varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `endpoints`            varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `blockchain_content`   binary                                                         DEFAULT NULL,
    `properties`           longblob,
    `gmt_create`           datetime                                                       DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`         datetime                                                       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_relayer_node` (`node_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC;

drop table if exists auth_msg_archive;
CREATE TABLE `auth_msg_archive`
(
    `id`                        int(11)              NOT NULL AUTO_INCREMENT,
    `ucp_id`                    VARBINARY(64) UNIQUE NOT NULL,
    `blockchain_product`        varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `blockchain_id`             varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `domain_name`               varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `amclient_contract_address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `version`                   int(11)                                                       DEFAULT NULL,
    `msg_sender`                varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `protocol_type`             int(11)                                                       DEFAULT NULL,
    `trust_level`               int(11)                                                       DEFAULT 2,
    `payload`                   mediumblob,
    `process_state`             varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `ext`                       mediumblob,
    `gmt_create`                datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`              datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC;

drop table if exists sdp_msg_archive;
CREATE TABLE `sdp_msg_archive`
(
    `id`                          int(11) NOT NULL AUTO_INCREMENT,
    `auth_msg_id`                 int(11)                                                       DEFAULT NULL,
    `version`                     int(11)                                                       DEFAULT 1,
    `atomic`                      TINYINT(1)                                                    DEFAULT 0,
    `sender_blockchain_product`   varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `sender_instance`             varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `sender_domain_name`          varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `sender_identity`             varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `sender_amclient_contract`    varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `receiver_blockchain_product` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `receiver_instance`           varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `receiver_domain_name`        varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `receiver_identity`           varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `receiver_amclient_contract`  varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `msg_sequence`                int(11)                                                       DEFAULT NULL,
    `process_state`               varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `tx_hash`                     varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `tx_success`                  tinyint(1)                                                    DEFAULT NULL,
    `tx_fail_reason`              varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `gmt_create`                  datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`                datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC;

drop table if exists blockchain_dt_task;
CREATE TABLE `blockchain_dt_task`
(
    `id`                 int(11) NOT NULL AUTO_INCREMENT,
    `node_id`            varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `task_type`          varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `blockchain_product` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `blockchain_id`      varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `ext`                varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `timeslice`          datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    `gmt_create`         datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`       datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task` (`node_id`, `task_type`, `blockchain_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC;

drop table if exists biz_dt_task;
CREATE TABLE `biz_dt_task`
(
    `id`           int(11) NOT NULL AUTO_INCREMENT,
    `node_id`      varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `task_type`    varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL,
    `unique_key`   varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `ext`          varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `timeslice`    datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    `gmt_create`   datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` datetime                                                      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_type_unique_key` (`task_type`, `unique_key`),
    UNIQUE KEY `uk_task` (`node_id`, `task_type`, `unique_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC;

drop table if exists dt_active_node;
CREATE TABLE `dt_active_node`
(
    `id`               int(11) NOT NULL AUTO_INCREMENT,
    `node_id`          varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `node_ip`          varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `state`            varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `last_active_time` datetime                                                     DEFAULT CURRENT_TIMESTAMP,
    `gmt_create`       datetime                                                     DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`     datetime                                                     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_node` (`node_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `plugin_server_objects`
(
    `id`                 int(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `ps_id`              varchar(64) UNIQUE  NOT NULL,
    `address`            varchar(128)        NOT NULL,
    `state`              INT                 NOT NULL,
    `products_supported` TEXT,
    `domains_serving`    TEXT,
    `properties`         BLOB,
    `gmt_create`         datetime DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`       datetime DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `bcdns_service`
(
    `id`           INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `domain_space` VARCHAR(128)        NOT NULL,
    `owner_oid`    BINARY              NOT NULL,
    `type`         VARCHAR(32)         NOT NULL,
    `state`        INT                 NOT NULL,
    `properties`   BLOB,
    `gmt_create`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX bcdns_network_id_domain_space
    ON bcdns_service (domain_space);