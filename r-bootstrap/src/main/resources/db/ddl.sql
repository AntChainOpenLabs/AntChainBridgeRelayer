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
    `product`       varchar(64)   DEFAULT NULL,
    `blockchain_id` varchar(128)  DEFAULT NULL,
    `alias`         varchar(128)  DEFAULT NULL,
    `description`   varchar(2096) DEFAULT NULL,
    `properties`    blob,
    `gmt_create`    datetime      DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`  datetime      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_instance` (`blockchain_id`)
) ENGINE = InnoDB
  ROW_FORMAT = DYNAMIC;

drop table if exists system_config;
CREATE TABLE `system_config`
(
    `id`           int(11) NOT NULL AUTO_INCREMENT,
    `conf_key`     varchar(128)   DEFAULT NULL,
    `conf_value`   varchar(15000) DEFAULT NULL,
    `gmt_create`   datetime       DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` datetime       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `conf_key` (`conf_key`)
) ENGINE = InnoDB
  ROW_FORMAT = DYNAMIC;

drop table if exists anchor_process;
CREATE TABLE `anchor_process`
(
    `id`                 int(11) NOT NULL AUTO_INCREMENT,
    `blockchain_product` varchar(64)  DEFAULT NULL,
    `instance`           varchar(128) DEFAULT NULL,
    `task`               varchar(64)  DEFAULT NULL,
    `block_height`       int(11)      DEFAULT NULL,
    `gmt_create`         datetime     DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`       datetime     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `blockchain_product` (`blockchain_product`, `instance`, `task`)
) ENGINE = InnoDB
  ROW_FORMAT = DYNAMIC;

drop table if exists `domain_cert`;
CREATE TABLE `domain_cert`
(
    `id`                 int(11)             NOT NULL AUTO_INCREMENT,
    `domain`             varchar(128) BINARY NOT NULL,
    `blockchain_product` varchar(64) BINARY  DEFAULT NULL,
    `instance`           varchar(128)        DEFAULT NULL,
    `subject_oid`        blob                DEFAULT NULL,
    `issuer_oid`         blob                DEFAULT NULL,
    `domain_space`       varchar(128) BINARY DEFAULT NULL,
    `domain_cert`        longblob,
    `gmt_create`         datetime            DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`       datetime            DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `domain` (`domain`)
) ENGINE = InnoDB
  ROW_FORMAT = DYNAMIC;

drop table if exists `domain_cert_application`;
CREATE TABLE `domain_cert_application`
(
    `id`            INT(11) PRIMARY KEY        NOT NULL AUTO_INCREMENT,
    `domain`        VARCHAR(128) BINARY UNIQUE NOT NULL,
    `domain_space`  VARCHAR(128) BINARY        NOT NULL,
    `apply_receipt` VARCHAR(128),
    `state`         VARCHAR(20),
    `gmt_create`    DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`  DATETIME DEFAULT CURRENT_TIMESTAMP
);

drop table if exists `domain_space_cert`;
CREATE TABLE `domain_space_cert`
(
    `id`                int(11)      NOT NULL AUTO_INCREMENT,
    `domain_space`      varchar(128) BINARY DEFAULT NULL,
    `parent_space`      varchar(128) BINARY DEFAULT NULL,
    `owner_oid_hex`     varchar(255) NOT NULL,
    `description`       varchar(128)        DEFAULT NULL,
    `domain_space_cert` longblob,
    `gmt_create`        datetime            DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`      datetime            DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `domain_space` (`domain_space`)
) ENGINE = InnoDB;
CREATE INDEX domain_space_cert_owner_oid_hex
    ON domain_space_cert (owner_oid_hex);

drop table if exists ucp_pool;
CREATE TABLE `ucp_pool`
(
    `id`                 int(11)            NOT NULL AUTO_INCREMENT,
    `ucp_id`             VARCHAR(64) UNIQUE NOT NULL,
    `blockchain_product` varchar(64) BINARY   DEFAULT NULL,
    `blockchain_id`      varchar(128) BINARY  DEFAULT NULL,
    `version`            int(11)              DEFAULT NULL,
    `src_domain`         varchar(128) BINARY  DEFAULT NULL,
    `blockhash`          varchar(66)          DEFAULT NULL,
    `txhash`             varchar(66)          DEFAULT NULL,
    `ledger_time`        TIMESTAMP,
    `udag_path`          varchar(1024) BINARY DEFAULT NULL,
    `protocol_type`      int(11)              DEFAULT NULL,
    `raw_message`        mediumblob,
    `ptc_oid`            VARBINARY(32),
    `tp_proof`           mediumblob,
    `from_network`       TINYINT(1)           DEFAULT 0,
    `relayer_id`         varchar(64)          DEFAULT NULL,
    `process_state`      varchar(64)          DEFAULT NULL,
    `gmt_create`         datetime             DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`       datetime             DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `ucp_state` (`process_state`),
    KEY `idx_srcdomain_processstate` (`src_domain`, `process_state`)
) ENGINE = InnoDB
  ROW_FORMAT = DYNAMIC;

drop table if exists auth_msg_pool;
CREATE TABLE `auth_msg_pool`
(
    `id`                        int(11)            NOT NULL AUTO_INCREMENT,
    `ucp_id`                    VARCHAR(64) UNIQUE NOT NULL,
    `blockchain_product`        varchar(64) BINARY  DEFAULT NULL,
    `blockchain_id`             varchar(128)        DEFAULT NULL,
    `domain_name`               varchar(128) BINARY DEFAULT NULL,
    `amclient_contract_address` varchar(255)        DEFAULT NULL,
    `version`                   int(11)             DEFAULT NULL,
    `msg_sender`                varchar(64)         DEFAULT NULL,
    `protocol_type`             int(11)             DEFAULT NULL,
    `trust_level`               int(11)             DEFAULT 2,
    `payload`                   mediumblob,
    `process_state`             varchar(64)         DEFAULT NULL,
    `fail_count`                int(11)             DEFAULT 0,
    `ext`                       mediumblob,
    `gmt_create`                datetime            DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`              datetime            DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `state` (`process_state`),
    KEY `idx_am_pool_peek` (`domain_name`, `trust_level`, `process_state`, `fail_count`),
    KEY `idx_domainname_processstate` (`domain_name`, `process_state`)
) ENGINE = InnoDB
  ROW_FORMAT = DYNAMIC;

drop table if exists sdp_msg_pool;
CREATE TABLE `sdp_msg_pool`
(
    `id`                          int(11) NOT NULL AUTO_INCREMENT,
    `auth_msg_id`                 int(11)             DEFAULT NULL,
    `version`                     int(11)             DEFAULT 1,
    `atomic`                      TINYINT(1)          DEFAULT 0,
    `sender_blockchain_product`   varchar(64) BINARY  DEFAULT NULL,
    `sender_instance`             varchar(128)        DEFAULT NULL,
    `sender_domain_name`          varchar(128) BINARY DEFAULT NULL,
    `sender_identity`             varchar(64)         DEFAULT NULL,
    `sender_amclient_contract`    varchar(255)        DEFAULT NULL,
    `receiver_blockchain_product` varchar(64) BINARY  DEFAULT NULL,
    `receiver_instance`           varchar(128)        DEFAULT NULL,
    `receiver_domain_name`        varchar(128) BINARY DEFAULT NULL,
    `receiver_identity`           varchar(64)         DEFAULT NULL,
    `receiver_amclient_contract`  varchar(255)        DEFAULT NULL,
    `msg_sequence`                int(11)             DEFAULT NULL,
    `process_state`               varchar(32)         DEFAULT NULL,
    `tx_hash`                     varchar(80)         DEFAULT NULL,
    `tx_success`                  tinyint(1)          DEFAULT NULL,
    `tx_fail_reason`              varchar(255)        DEFAULT NULL,
    `gmt_create`                  datetime            DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`                datetime            DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_auth_msg_id` (`auth_msg_id`),
    KEY `idx_queue_tx_hash` (`tx_hash`),
    KEY `state` (`process_state`),
    KEY `idx_receiverinstance_processstate_receiverblockchainproduct` (`receiver_instance`, `process_state`, `receiver_blockchain_product`)
) ENGINE = InnoDB
  ROW_FORMAT = DYNAMIC;

drop table if exists cross_chain_msg_acl;
CREATE TABLE `cross_chain_msg_acl`
(
    `id`                 int(11) NOT NULL AUTO_INCREMENT,
    `biz_id`             varchar(64)         DEFAULT NULL,
    `owner_domain`       varchar(128) BINARY DEFAULT NULL,
    `owner_identity`     varchar(128)        DEFAULT NULL,
    `owner_identity_hex` varchar(64)         DEFAULT NULL,
    `grant_domain`       varchar(128) BINARY DEFAULT NULL,
    `grant_identity`     varchar(128)        DEFAULT NULL,
    `grant_identity_hex` varchar(64)         DEFAULT NULL,
    `is_deleted`         int(1)              DEFAULT NULL,
    `gmt_create`         datetime            DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`       datetime            DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `biz_id` (`biz_id`),
    KEY `exact_valid_rules` (`owner_domain`, `owner_identity_hex`, `grant_domain`, `grant_identity_hex`, `is_deleted`)
) ENGINE = InnoDB
  ROW_FORMAT = DYNAMIC;

drop table if exists relayer_network;
CREATE TABLE `relayer_network`
(
    `id`           int(11) NOT NULL AUTO_INCREMENT,
    `network_id`   varchar(64)         DEFAULT NULL,
    `domain`       varchar(128) BINARY DEFAULT NULL,
    `node_id`      varchar(64)         DEFAULT NULL,
    `sync_state`   varchar(64)         DEFAULT NULL,
    `gmt_create`   datetime            DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` datetime            DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_item` (`network_id`, `domain`, `node_id`)
) ENGINE = InnoDB
  ROW_FORMAT = DYNAMIC;

DROP TABLE IF EXISTS crosschain_channel;
CREATE TABLE `crosschain_channel`
(
    `id`              INT(11) NOT NULL AUTO_INCREMENT,
    `local_domain`    VARCHAR(128) BINARY DEFAULT NULL,
    `remote_domain`   VARCHAR(128) BINARY DEFAULT NULL,
    `relayer_node_id` VARCHAR(64)         DEFAULT NULL,
    `state`           VARCHAR(64)         DEFAULT NULL,
    `gmt_create`      DATETIME            DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`    DATETIME            DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `cc_channel_domains` (`local_domain`, `remote_domain`)
);

drop table if exists relayer_node;
CREATE TABLE `relayer_node`
(
    `id`                   int(11) NOT NULL AUTO_INCREMENT,
    `node_id`              varchar(64)          DEFAULT NULL,
    `relayer_cert_id`      varchar(128)         DEFAULT NULL,
    `node_crosschain_cert` BLOB                 DEFAULT NULL,
    `node_sig_algo`        varchar(255)         DEFAULT NULL,
    `domains`              varchar(2048) BINARY DEFAULT NULL,
    `endpoints`            varchar(1024)        DEFAULT NULL,
    `blockchain_content`   BLOB                 DEFAULT NULL,
    `properties`           longblob,
    `gmt_create`           datetime             DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`         datetime             DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_relayer_node` (`node_id`)
) ENGINE = InnoDB
  ROW_FORMAT = DYNAMIC;

drop table if exists auth_msg_archive;
CREATE TABLE `auth_msg_archive`
(
    `id`                        int(11)            NOT NULL AUTO_INCREMENT,
    `ucp_id`                    VARCHAR(64) UNIQUE NOT NULL,
    `blockchain_product`        varchar(64) BINARY  DEFAULT NULL,
    `blockchain_id`             varchar(128)        DEFAULT NULL,
    `domain_name`               varchar(128) BINARY DEFAULT NULL,
    `amclient_contract_address` varchar(255)        DEFAULT NULL,
    `version`                   int(11)             DEFAULT NULL,
    `msg_sender`                varchar(64)         DEFAULT NULL,
    `protocol_type`             int(11)             DEFAULT NULL,
    `trust_level`               int(11)             DEFAULT 2,
    `payload`                   mediumblob,
    `process_state`             varchar(64)         DEFAULT NULL,
    `fail_count`                int(11)             DEFAULT 0,
    `ext`                       mediumblob,
    `gmt_create`                datetime            DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`              datetime            DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  ROW_FORMAT = DYNAMIC;

drop table if exists sdp_msg_archive;
CREATE TABLE `sdp_msg_archive`
(
    `id`                          int(11) NOT NULL AUTO_INCREMENT,
    `auth_msg_id`                 int(11)             DEFAULT NULL,
    `version`                     int(11)             DEFAULT 1,
    `atomic`                      TINYINT(1)          DEFAULT 0,
    `sender_blockchain_product`   varchar(64) BINARY  DEFAULT NULL,
    `sender_instance`             varchar(128)        DEFAULT NULL,
    `sender_domain_name`          varchar(128) BINARY DEFAULT NULL,
    `sender_identity`             varchar(64)         DEFAULT NULL,
    `sender_amclient_contract`    varchar(255)        DEFAULT NULL,
    `receiver_blockchain_product` varchar(64) BINARY  DEFAULT NULL,
    `receiver_instance`           varchar(128)        DEFAULT NULL,
    `receiver_domain_name`        varchar(128) BINARY DEFAULT NULL,
    `receiver_identity`           varchar(64)         DEFAULT NULL,
    `receiver_amclient_contract`  varchar(255)        DEFAULT NULL,
    `msg_sequence`                int(11)             DEFAULT NULL,
    `process_state`               varchar(32)         DEFAULT NULL,
    `tx_hash`                     varchar(80)         DEFAULT NULL,
    `tx_success`                  tinyint(1)          DEFAULT NULL,
    `tx_fail_reason`              varchar(255)        DEFAULT NULL,
    `gmt_create`                  datetime            DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`                datetime            DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  ROW_FORMAT = DYNAMIC;

drop table if exists blockchain_dt_task;
CREATE TABLE `blockchain_dt_task`
(
    `id`                 int(11) NOT NULL AUTO_INCREMENT,
    `node_id`            varchar(64)        DEFAULT NULL,
    `task_type`          varchar(64)        DEFAULT NULL,
    `blockchain_product` varchar(64) BINARY DEFAULT NULL,
    `blockchain_id`      varchar(128)       DEFAULT NULL,
    `ext`                varchar(255)       DEFAULT NULL,
    `timeslice`          datetime(3)        DEFAULT CURRENT_TIMESTAMP(3),
    `gmt_create`         datetime           DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`       datetime           DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task` (`node_id`, `task_type`, `blockchain_id`)
) ENGINE = InnoDB
  ROW_FORMAT = DYNAMIC;

drop table if exists biz_dt_task;
CREATE TABLE `biz_dt_task`
(
    `id`           int(11) NOT NULL AUTO_INCREMENT,
    `node_id`      varchar(64)  DEFAULT NULL,
    `task_type`    varchar(64)  DEFAULT NULL,
    `unique_key`   varchar(128) DEFAULT NULL,
    `ext`          varchar(255) DEFAULT NULL,
    `timeslice`    datetime(3)  DEFAULT CURRENT_TIMESTAMP(3),
    `gmt_create`   datetime     DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` datetime     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_type_unique_key` (`task_type`, `unique_key`),
    UNIQUE KEY `uk_task` (`node_id`, `task_type`, `unique_key`)
) ENGINE = InnoDB
  ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `mark_dt_task`
(
    `id`           INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `task_type`    INT(11)             NOT NULL,
    `unique_key`   varchar(128) BINARY DEFAULT NULL,
    `node_id`      varchar(64)         DEFAULT NULL,
    `state`        INT(11)             NOT NULL,
    `end_time`     DATETIME(3),
    `gmt_create`   DATETIME            DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME            DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX mark_dt_task_idx1
    ON mark_dt_task (task_type, unique_key, node_id);
CREATE INDEX mark_dt_task_idx2
    ON mark_dt_task (state, task_type, node_id);

drop table if exists dt_active_node;
CREATE TABLE `dt_active_node`
(
    `id`               int(11) NOT NULL AUTO_INCREMENT,
    `node_id`          varchar(64) DEFAULT NULL,
    `node_ip`          varchar(64) DEFAULT NULL,
    `state`            varchar(64) DEFAULT NULL,
    `last_active_time` datetime(3) DEFAULT CURRENT_TIMESTAMP(3),
    `gmt_create`       datetime    DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`     datetime    DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_node` (`node_id`)
) ENGINE = InnoDB
  ROW_FORMAT = DYNAMIC;

DROP TABLE IF EXISTS `plugin_server_objects`;
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

DROP TABLE IF EXISTS `bcdns_service`;
CREATE TABLE IF NOT EXISTS `bcdns_service`
(
    `id`           INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
    `domain_space` VARCHAR(128) BINARY NOT NULL,
    `owner_oid`    VARCHAR(255)        NOT NULL,
    `type`         VARCHAR(32)         NOT NULL,
    `state`        INT                 NOT NULL,
    `properties`   BLOB,
    `gmt_create`   DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX bcdns_network_id_domain_space
    ON bcdns_service (domain_space);