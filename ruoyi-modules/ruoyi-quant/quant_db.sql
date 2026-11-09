/*
 ruoyi-quant V5 Complete Schema SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80400 (8.4.0)
 Source Host           : localhost:3306
 Source Schema         : quantdb

 Target Server Type    : MySQL
 Target Server Version : 80400 (8.4.0)
 File Encoding         : 65001

 Date: 28/09/2026 10:36:14
*/

SET NAMES utf8mb4;
SET
FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for quant_account_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `quant_account_snapshot`;
CREATE TABLE `quant_account_snapshot`
(
    `id`                bigint          NOT NULL COMMENT '主键ID',
    `credential_id`     bigint          NOT NULL COMMENT '量化凭证ID',
    `wallet_balance`    decimal(32, 16) NOT NULL DEFAULT 0.0000000000000000 COMMENT '钱包余额',
    `available_balance` decimal(32, 16) NOT NULL DEFAULT 0.0000000000000000 COMMENT '可用余额',
    `margin_balance`    decimal(32, 16) NOT NULL DEFAULT 0.0000000000000000 COMMENT '保证金余额',
    `unrealized_pnl`    decimal(32, 16) NOT NULL DEFAULT 0.0000000000000000 COMMENT '未实现盈亏',
    `realized_pnl`      decimal(32, 16) NOT NULL DEFAULT 0.0000000000000000 COMMENT '已实现盈亏',
    `funding_fee`       decimal(32, 16) NOT NULL DEFAULT 0.0000000000000000 COMMENT '资金费',
    `observed_at`       datetime        NOT NULL COMMENT '观测时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_quant_account_snapshot`(`credential_id` ASC, `observed_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '量化账户资产快照表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_paper_account
-- ----------------------------
DROP TABLE IF EXISTS `quant_paper_account`;
CREATE TABLE `quant_paper_account`
(
    `id`                bigint          NOT NULL COMMENT '主键ID',
    `credential_id`     bigint          NOT NULL COMMENT 'PAPER凭证ID',
    `quote_asset`       varchar(20)     NOT NULL DEFAULT 'USDT' COMMENT '计价资产',
    `initial_balance`   decimal(32, 16) NOT NULL DEFAULT 1000.0000000000000000 COMMENT '初始虚拟余额',
    `balance`           decimal(32, 16) NOT NULL DEFAULT 1000.0000000000000000 COMMENT '当前余额',
    `available_balance` decimal(32, 16) NOT NULL DEFAULT 1000.0000000000000000 COMMENT '可用余额',
    `realized_pnl`      decimal(32, 16) NOT NULL DEFAULT 0.0000000000000000 COMMENT '已实现盈亏',
    `unrealized_pnl`    decimal(32, 16) NOT NULL DEFAULT 0.0000000000000000 COMMENT '未实现盈亏',
    `updated_at`        datetime        NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quant_paper_account_credential` (`credential_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='PAPER虚拟资金账户';

-- ----------------------------
-- Table structure for quant_credential
-- ----------------------------
DROP TABLE IF EXISTS `quant_credential`;
CREATE TABLE `quant_credential`
(
    `id`                  bigint                                                        NOT NULL COMMENT '主键ID',
    `user_id`             bigint                                                        NOT NULL COMMENT '用户ID',
    `account_name`        varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '账户名称',
    `api_key`             varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'API Key；PAPER 可为空',
    `secret_key`          varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Secret Key；PAPER 可为空',
    `signature_type`      varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '签名类型；PAPER 可为空',
    `run_mode`            varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL DEFAULT 'PAPER' COMMENT '运行模式',
    `status`              char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '状态',
    `connection_status`   varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL DEFAULT 'UNKNOWN' COMMENT '连接状态',
    `trade_permission`    char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '交易权限',
    `withdrawal_disabled` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '禁止提币标记',
    `remark`              varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
    `last_connect_time`   datetime NULL DEFAULT NULL COMMENT '最近连接时间',
    `create_dept`         bigint NULL DEFAULT NULL COMMENT '创建部门ID',
    `create_by`           bigint NULL DEFAULT NULL COMMENT '创建人ID',
    `create_time`         datetime NULL DEFAULT NULL COMMENT '创建时间',
    `update_by`           bigint NULL DEFAULT NULL COMMENT '更新人ID',
    `update_time`         datetime NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag`            int                                                           NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_quant_credential_user_name`(`user_id` ASC, `account_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '量化交易凭证表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_credential_user
-- ----------------------------
DROP TABLE IF EXISTS `quant_credential_user`;
CREATE TABLE `quant_credential_user`
(
    `id`            bigint NOT NULL COMMENT '主键ID',
    `credential_id` bigint NOT NULL COMMENT '量化凭证ID',
    `user_id`       bigint NOT NULL COMMENT '用户ID',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_quant_credential_user`(`credential_id` ASC, `user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '量化凭证用户关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_notification
-- ----------------------------
DROP TABLE IF EXISTS `quant_notification`;
CREATE TABLE `quant_notification`
(
    `id`            bigint                                                         NOT NULL COMMENT '主键ID',
    `credential_id` bigint NULL DEFAULT NULL COMMENT '量化凭证ID',
    `channel`       varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NOT NULL COMMENT '通知渠道',
    `recipient`     varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '接收人',
    `subject`       varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '通知主题',
    `content`       text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '通知内容',
    `status`        varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci   NOT NULL COMMENT '状态',
    `retry_count`   int                                                            NOT NULL DEFAULT 0 COMMENT '重试次数',
    `next_retry_at` datetime NULL DEFAULT NULL COMMENT '下次重试时间',
    `created_at`    datetime                                                       NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX           `idx_quant_notification_retry`(`channel` ASC, `status` ASC, `next_retry_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '量化通知记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_order
-- ----------------------------
DROP TABLE IF EXISTS `quant_order`;
CREATE TABLE `quant_order`
(
    `id`                   bigint                                                        NOT NULL COMMENT '主键ID',
    `credential_id`        bigint                                                        NOT NULL COMMENT '量化凭证ID',
    `strategy_id`          bigint NULL DEFAULT NULL COMMENT '来源策略ID，外部/手工订单可为空',
    `strategy_instance_id` bigint NULL DEFAULT NULL COMMENT '来源策略实例ID，外部/手工订单可为空',
    `symbol`               varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '交易对',
    `exchange_order_id`    bigint NULL DEFAULT NULL COMMENT '交易所订单ID',
    `client_order_id`      varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '客户端订单ID',
    `side`                 varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '买卖方向',
    `order_type`           varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '订单类型',
    `status`               varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '状态',
    `position_side`        varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '持仓方向',
    `price`                decimal(32, 16) NULL DEFAULT NULL COMMENT '委托价格',
    `original_quantity`    decimal(32, 16)                                               NOT NULL COMMENT '原始数量',
    `executed_quantity`    decimal(32, 16)                                               NOT NULL DEFAULT 0.0000000000000000 COMMENT '已成交数量',
    `reduce_only`          char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '只减仓标记；未知时保留NULL',
    `leverage_snapshot`    int NULL DEFAULT NULL COMMENT '订单受理时冻结的杠杆',
    `price_tick_size_snapshot` decimal(32, 16) NULL DEFAULT NULL COMMENT '订单受理时冻结的价格最小变动单位',
    `post_fill_wallet_balance` decimal(32, 16) NULL DEFAULT NULL COMMENT '成交后账户钱包余额快照，不代表该笔交易造成的余额变化',
    `post_fill_balance_at` datetime NULL DEFAULT NULL COMMENT '成交后账户余额快照核实时间（UTC）',
    `post_fill_balance_requested_at` datetime NULL DEFAULT NULL COMMENT '最近一次成交后余额快照请求时间（UTC）',
    `idempotency_key`      varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '幂等键',
    `created_at`           datetime                                                      NOT NULL COMMENT '创建时间',
    `updated_at`           datetime                                                      NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_quant_order_idempotency`(`credential_id` ASC, `idempotency_key` ASC) USING BTREE,
    UNIQUE INDEX `uk_quant_order_client`(`credential_id` ASC, `client_order_id` ASC) USING BTREE,
    INDEX                  `idx_quant_order_strategy_instance`(`strategy_instance_id` ASC, `created_at` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '量化订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_position
-- ----------------------------
DROP TABLE IF EXISTS `quant_position`;
CREATE TABLE `quant_position`
(
    `id`                bigint                                                       NOT NULL COMMENT '主键ID',
    `credential_id`     bigint                                                       NOT NULL COMMENT '量化凭证ID',
    `symbol`            varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '交易对',
    `position_side`     varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '持仓方向',
    `quantity`          decimal(32, 16)                                              NOT NULL COMMENT '持仓数量',
    `entry_price`       decimal(32, 16)                                              NOT NULL COMMENT '开仓均价',
    `mark_price`        decimal(32, 16)                                              NOT NULL COMMENT '标记价格',
    `unrealized_pnl`    decimal(32, 16)                                              NOT NULL COMMENT '未实现盈亏',
    `liquidation_price` decimal(32, 16) NULL DEFAULT NULL COMMENT '强平价格',
    `leverage`          int NULL DEFAULT NULL COMMENT '杠杆倍数',
    `margin`            decimal(32, 16) NULL DEFAULT NULL COMMENT '保证金',
    `updated_at`        datetime                                                     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_quant_position`(`credential_id` ASC, `symbol` ASC, `position_side` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '量化持仓表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_protection_order
-- ----------------------------
DROP TABLE IF EXISTS `quant_protection_order`;
CREATE TABLE `quant_protection_order`
(
    `id`                bigint                                                        NOT NULL COMMENT '主键ID',
    `credential_id`     bigint                                                        NOT NULL COMMENT '量化凭证ID',
    `strategy_id`       bigint NULL COMMENT '创建保护意图时冻结的策略ID',
    `strategy_instance_id` bigint NULL COMMENT '创建保护意图时冻结的策略实例ID',
    `position_id`       bigint NULL DEFAULT NULL COMMENT '本地持仓ID，暂时无法归属时允许为空',
    `symbol`            varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '交易对',
    `position_side`     varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '持仓方向',
    `exchange_order_id` bigint NULL DEFAULT NULL COMMENT '交易所条件单ID',
    `client_order_id`   varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '客户端条件单ID',
    `created_at_epoch_ms` bigint NULL DEFAULT NULL COMMENT '本地保护意图创建时间(epoch毫秒)，历史未知保持NULL',
    `protection_type`   varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '保护单类型',
    `order_type`        varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '条件单类型',
    `quantity`          decimal(32, 16) NULL DEFAULT NULL COMMENT '条件单数量',
    `stop_price`        decimal(32, 16)                                               NOT NULL COMMENT '触发价格',
    `status`            varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '状态',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_quant_protection_client`(`credential_id` ASC, `client_order_id` ASC) USING BTREE,
    KEY                 `idx_quant_protection_position`(`credential_id` ASC, `position_id` ASC),
    KEY                 `idx_quant_protection_symbol_status`(`credential_id` ASC, `symbol` ASC, `status` ASC)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '量化持仓保护单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_reconcile_record
-- ----------------------------
DROP TABLE IF EXISTS `quant_reconcile_record`;
CREATE TABLE `quant_reconcile_record`
(
    `id`             bigint                                                       NOT NULL COMMENT '主键ID',
    `credential_id`  bigint                                                       NOT NULL COMMENT '量化凭证ID',
    `reconcile_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '对账类型',
    `result`         varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '对账结果',
    `detail_json`    json NULL COMMENT '详情JSON',
    `reconciled_at`  datetime                                                     NOT NULL COMMENT '对账时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '量化对账记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_risk_config
-- ----------------------------
DROP TABLE IF EXISTS `quant_risk_config`;
CREATE TABLE `quant_risk_config`
(
    `id`                          bigint                                                       NOT NULL COMMENT '主键ID',
    `credential_id`               bigint                                                       NOT NULL COMMENT '量化凭证ID',
    `live_enabled`                char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NOT NULL DEFAULT '0' COMMENT '实盘开关',
    `max_order_notional`          decimal(32, 16)                                              NOT NULL DEFAULT 0.0000000000000000 COMMENT '单笔最大名义价值',
    `daily_loss_limit`            decimal(32, 16)                                              NOT NULL DEFAULT 0.0000000000000000 COMMENT '日亏损限额',
    `max_open_positions`          int                                                          NOT NULL DEFAULT 1 COMMENT '最大持仓数',
    `max_orders_per_minute`       int                                                          NOT NULL DEFAULT 1 COMMENT '每分钟最大下单数',
    `api_permission_check`        char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NOT NULL DEFAULT '1' COMMENT 'API权限校验开关',
    `websocket_disconnect_action` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ALERT' COMMENT 'WebSocket断连处理动作',
    `position_mismatch_action`    varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'HALT' COMMENT '持仓不一致处理动作',
    `protection_failure_action`   varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'HALT' COMMENT '保护单失败处理动作',
    `whitelist`                   char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NOT NULL DEFAULT '0' COMMENT '白名单开关',
    `notification_emails`         varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '通知邮箱列表',
    `create_time`                 datetime NULL DEFAULT NULL COMMENT '创建时间',
    `update_time`                 datetime NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_quant_risk_credential`(`credential_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '量化风控配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_strategy
-- ----------------------------
DROP TABLE IF EXISTS `quant_strategy`;
CREATE TABLE `quant_strategy`
(
    `id`                          bigint                                                        NOT NULL COMMENT '主键ID',
    `strategy_name`               varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '策略名称',
    `plugin_code`                 varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '插件编码',
    `plugin_version`              varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '插件版本',
    `strategy_type`               varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '策略类型',
    `max_open_count`              int                                                           NOT NULL DEFAULT 1 COMMENT '最大开仓数',
    `default_interval`            varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci  NOT NULL COMMENT '默认周期',
    `default_open_ratio`          decimal(12, 6)                                                NOT NULL COMMENT '默认开仓比例',
    `default_stop_loss_ratio`     decimal(12, 6)                                                NOT NULL COMMENT '默认止损比例(兼容字段)',
    `stop_loss_ratio`             decimal(12, 6)                                                NOT NULL DEFAULT 0.100000 COMMENT '策略固定硬止损回报率；KlineSlopeRateStrategy 不从 params 读取',
    `hard_stop_margin_loss_ratio` decimal(12, 6)                                                NOT NULL DEFAULT 0.200000 COMMENT '交易所固定硬止损对应保证金亏损比例',
    `default_leverage`            int                                                           NOT NULL DEFAULT 1 COMMENT '默认杠杆',
    `params_json`                 json                                                          NOT NULL COMMENT '策略参数JSON',
    `status`                      char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci      NOT NULL DEFAULT '0' COMMENT '状态',
    `create_time`                 datetime NULL DEFAULT NULL COMMENT '创建时间',
    `update_time`                 datetime NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '量化策略定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_strategy_account
-- ----------------------------
DROP TABLE IF EXISTS `quant_strategy_account`;
CREATE TABLE `quant_strategy_account`
(
    `id`            bigint                                                   NOT NULL COMMENT '主键ID',
    `strategy_id`   bigint                                                   NOT NULL COMMENT '策略ID',
    `credential_id` bigint                                                   NOT NULL COMMENT '量化凭证ID',
    `status`        char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '0' COMMENT '状态',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_quant_strategy_account`(`strategy_id` ASC, `credential_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '策略账户关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_strategy_entry_guard
-- ----------------------------
DROP TABLE IF EXISTS `quant_strategy_entry_guard`;
CREATE TABLE `quant_strategy_entry_guard`
(
    `id`              bigint                                                       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `credential_id`   bigint                                                       NOT NULL COMMENT '量化凭证ID',
    `symbol`          varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '交易对',
    `instance_id`     bigint                                                       NOT NULL COMMENT '策略实例ID',
    `kline_open_time` datetime                                                     NOT NULL COMMENT 'K线开盘时间',
    `order_id`        bigint NULL DEFAULT NULL COMMENT '订单ID',
    `guard_type`      varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ENTRY' COMMENT '保护类型：ENTRY开仓、CLOSE平仓K线封锁',
    `created_at`      datetime                                                     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_quant_strategy_entry`(`credential_id` ASC, `symbol` ASC, `kline_open_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '策略开仓防重复记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_strategy_instance
-- ----------------------------
DROP TABLE IF EXISTS `quant_strategy_instance`;
CREATE TABLE `quant_strategy_instance`
(
    `id`                 bigint                                                       NOT NULL COMMENT '主键ID',
    `strategy_id`        bigint                                                       NOT NULL COMMENT '策略ID',
    `strategy_symbol_id` bigint                                                       NOT NULL COMMENT '策略交易对配置ID',
    `credential_id`      bigint                                                       NOT NULL COMMENT '量化凭证ID',
    `status`             varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'STOPPED' COMMENT '状态',
    `last_signal_at`     datetime NULL DEFAULT NULL COMMENT '最近信号时间',
    `last_error`         varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最近错误信息',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_quant_strategy_instance`(`strategy_symbol_id` ASC, `credential_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '策略运行实例表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_strategy_symbol
-- ----------------------------
DROP TABLE IF EXISTS `quant_strategy_symbol`;
CREATE TABLE `quant_strategy_symbol`
(
    `id`                          bigint                                                       NOT NULL COMMENT '主键ID',
    `strategy_id`                 bigint                                                       NOT NULL COMMENT '策略ID',
    `symbol_id`                   bigint                                                       NOT NULL COMMENT '交易对ID',
    `interval_code`               varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '周期编码',
    `open_ratio`                  decimal(12, 6)                                               NOT NULL COMMENT '开仓比例',
    `stop_loss_ratio`             decimal(12, 6)                                               NOT NULL COMMENT '止损比例',
    `hard_stop_margin_loss_ratio` decimal(12, 6)                                               NOT NULL DEFAULT 0.200000 COMMENT '交易所固定硬止损对应保证金亏损比例',
    `leverage`                    int                                                          NOT NULL COMMENT '杠杆倍数',
    `params_json`                 json                                                         NOT NULL COMMENT '策略参数JSON',
    `status`                      char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NOT NULL DEFAULT '0' COMMENT '状态',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_quant_strategy_symbol`(`strategy_id` ASC, `symbol_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '策略交易对配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_symbol
-- ----------------------------
DROP TABLE IF EXISTS `quant_symbol`;
CREATE TABLE `quant_symbol`
(
    `id`                   bigint                                                       NOT NULL COMMENT '主键ID',
    `symbol`               varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '交易对',
    `quote_asset`          varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '报价资产',
    `price_precision`      int                                                          NOT NULL COMMENT '价格精度',
    `quantity_precision`   int                                                          NOT NULL COMMENT '数量精度',
    `price_tick_size`      decimal(32, 16)                                              NOT NULL COMMENT '价格最小变动单位',
    `quantity_step`        decimal(32, 16)                                              NOT NULL COMMENT '数量步长',
    `market_quantity_step` decimal(32, 16)                                              NOT NULL COMMENT '市价数量步长',
    `min_notional`         decimal(32, 16)                                              NOT NULL COMMENT '最小名义价值',
    `max_leverage`         int                                                          NOT NULL DEFAULT 1 COMMENT '最大杠杆',
    `margin_mode`          varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '保证金模式',
    `exchange_status`      varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '交易所状态',
    `status`               char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci     NOT NULL DEFAULT '0' COMMENT '状态',
    `synced_at`            datetime NULL DEFAULT NULL COMMENT '同步时间',
    `create_dept`          bigint NULL DEFAULT NULL COMMENT '创建部门ID',
    `create_by`            bigint NULL DEFAULT NULL COMMENT '创建人ID',
    `create_time`          datetime NULL DEFAULT NULL COMMENT '创建时间',
    `update_by`            bigint NULL DEFAULT NULL COMMENT '更新人ID',
    `update_time`          datetime NULL DEFAULT NULL COMMENT '更新时间',
    `del_flag`             int                                                          NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_quant_symbol`(`symbol` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '量化交易对信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_symbol_favorite
-- ----------------------------
DROP TABLE IF EXISTS `quant_symbol_favorite`;
CREATE TABLE `quant_symbol_favorite`
(
    `id`        bigint NOT NULL COMMENT '主键ID',
    `symbol_id` bigint NOT NULL COMMENT '交易对ID',
    `user_id`   bigint NOT NULL COMMENT '用户ID',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_quant_symbol_favorite`(`symbol_id` ASC, `user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户收藏交易对表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_trade
-- ----------------------------
DROP TABLE IF EXISTS `quant_trade`;
CREATE TABLE `quant_trade`
(
    `id`                   bigint                                                       NOT NULL COMMENT '主键ID',
    `credential_id`        bigint                                                       NOT NULL COMMENT '量化凭证ID',
    `strategy_id`          bigint NULL DEFAULT NULL COMMENT '来源策略ID，外部/手工成交可为空',
    `strategy_instance_id` bigint NULL DEFAULT NULL COMMENT '来源策略实例ID，外部/手工成交可为空',
    `symbol`               varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '交易对',
    `exchange_trade_id`    bigint                                                       NOT NULL COMMENT '交易所成交ID',
    `exchange_order_id`    bigint                                                       NOT NULL COMMENT '交易所订单ID',
    `side`                 varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '买卖方向',
    `position_side`        varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Binance 成交返回的持仓方向',
    `quantity`             decimal(32, 16)                                              NOT NULL COMMENT '成交数量',
    `price`                decimal(32, 16)                                              NOT NULL COMMENT '成交价格',
    `commission`           decimal(32, 16)                                              NOT NULL DEFAULT 0.0000000000000000 COMMENT '手续费',
    `funding_fee`          decimal(32, 16)                                              NOT NULL DEFAULT 0.0000000000000000 COMMENT '资金费',
    `realized_pnl`         decimal(32, 16)                                              NOT NULL DEFAULT 0.0000000000000000 COMMENT '已实现盈亏',
    `trade_time`           datetime                                                     NOT NULL COMMENT '成交时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_quant_trade_symbol`(`credential_id` ASC, `symbol` ASC, `exchange_trade_id` ASC) USING BTREE,
    INDEX                  `idx_quant_trade_strategy_instance`(`strategy_instance_id` ASC, `trade_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '量化成交记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for quant_financial_event
-- ----------------------------
DROP TABLE IF EXISTS `quant_financial_event`;
CREATE TABLE `quant_financial_event`
(
    `id`                      bigint          NOT NULL COMMENT '主键ID',
    `credential_id`           bigint          NOT NULL COMMENT '量化凭证ID',
    `strategy_id`             bigint NULL COMMENT '来源策略ID；可通过订单/成交归因，无法识别时为空',
    `strategy_instance_id`    bigint NULL COMMENT '来源策略实例ID；无法识别时为空',
    `symbol`                  varchar(32) NULL COMMENT '交易对',
    `event_type`              varchar(40)     NOT NULL COMMENT 'REALIZED_PNL/COMMISSION/FUNDING_FEE/REBATE/TRANSFER/LIQUIDATION等',
    `amount`                  decimal(32, 16) NOT NULL DEFAULT 0 COMMENT '金额，收入为正、支出为负',
    `asset`                   varchar(20) NULL COMMENT '资产',
    `exchange_transaction_id` bigint          NOT NULL COMMENT '交易所资金流水ID',
    `event_identity_key`      varchar(128)    NOT NULL COMMENT 'Canonical immutable exchange financial-event identity',
    `info`                    varchar(500) NULL COMMENT '交易所附加信息',
    `event_time`              datetime        NOT NULL COMMENT '事件时间',
    `created_at`              datetime        NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quant_financial_event_identity` (`credential_id`,`event_identity_key`),
    KEY                       `idx_quant_financial_event_time` (`credential_id`,`event_time`),
    KEY                       `idx_quant_financial_event_strategy` (`strategy_instance_id`,`event_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='交易财务事件账本';

-- ============================================================
-- V5 runtime / execution / reconciliation tables
-- ============================================================
DROP TABLE IF EXISTS `quant_execution_record`;
CREATE TABLE `quant_execution_record`
(
    `id`                    bigint       NOT NULL COMMENT '主键ID',
    `credential_id`         bigint       NOT NULL COMMENT '量化凭证ID',
    `strategy_id`           bigint NULL COMMENT '来源策略ID，手工执行可为空',
    `strategy_instance_id`  bigint NULL COMMENT '来源策略实例ID，手工执行可为空',
    `operation_type`        varchar(20)  NOT NULL COMMENT 'OPEN/CLOSE/CANCEL',
    `symbol`                varchar(32)  NOT NULL COMMENT '交易对',
    `idempotency_key`       varchar(128) NOT NULL COMMENT '幂等键',
    `client_order_id`       varchar(128) NOT NULL COMMENT '客户端订单ID',
    `exchange_order_id`     bigint NULL COMMENT '交易所订单ID',
    `execution_status`      varchar(20)  NOT NULL COMMENT 'PENDING/CONFIRMED/UNKNOWN/FAILED/RECOVERED',
    `exchange_order_status` varchar(30) NULL COMMENT 'NEW/PARTIALLY_FILLED/FILLED/CANCELED/EXPIRED/REJECTED 等交易所订单状态',
    `error_message`         varchar(2000) NULL COMMENT '执行错误',
    `created_at`            datetime     NOT NULL COMMENT '创建时间',
    `recovery_attempts`     int          NOT NULL DEFAULT 0 COMMENT '恢复检查次数',
    `last_recovery_at`      datetime NULL COMMENT '最近恢复检查时间',
    `paper_settled`         char(1)      NOT NULL DEFAULT '0' COMMENT 'PAPER是否已结算',
    `updated_at`            datetime     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quant_execution_idempotency` (`credential_id`,`idempotency_key`),
    KEY                     `idx_quant_execution_status` (`execution_status`,`updated_at`),
    KEY                     `idx_quant_execution_pending` (`execution_status`,`created_at`),
    KEY                     `idx_quant_execution_strategy_instance` (`strategy_instance_id`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='统一交易执行幂等账本';

DROP TABLE IF EXISTS `quant_strategy_runtime_lease`;
CREATE TABLE `quant_strategy_runtime_lease`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `instance_id`   bigint       NOT NULL COMMENT '策略实例ID',
    `credential_id` bigint       NOT NULL COMMENT '凭证ID',
    `symbol`        varchar(32)  NOT NULL COMMENT '交易对',
    `owner_token`   varchar(128) NOT NULL COMMENT '运行实例标识',
    `lease_until`   datetime     NOT NULL COMMENT '租约截止时间',
    `create_time`   datetime     NOT NULL COMMENT '创建时间',
    `update_time`   datetime     NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quant_runtime_lease` (`credential_id`,`symbol`),
    KEY             `idx_quant_runtime_lease_until` (`lease_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='策略运行分布式租约';

DROP TABLE IF EXISTS `quant_strategy_position_state`;
CREATE TABLE `quant_strategy_position_state`
(
    `id`                              bigint      NOT NULL COMMENT '主键ID',
    `instance_id`                     bigint      NOT NULL COMMENT '策略实例ID',
    `position_id`                     bigint NULL COMMENT '本地持仓ID',
    `status`                          varchar(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/OPEN_PENDING/EXIT_PENDING/CLOSED',
    `direction`                       varchar(10) NULL COMMENT 'LONG/SHORT',
    `entry_price`                     decimal(32, 16) NULL COMMENT '开仓均价',
    `entry_atr`                       decimal(32, 16) NULL COMMENT '入场ATR',
    `position_qty`                    decimal(32, 16) NULL COMMENT '当前仓位数量',
    `initial_position_qty`            decimal(32, 16) NULL COMMENT '初始仓位数量',
    `initial_margin`                  decimal(32, 16) NULL COMMENT '初始保证金',
    `frozen_stop_loss_ratio`          decimal(16, 8) NULL COMMENT '冻结止损比例',
    `strategy_params_snapshot`        json NULL COMMENT '开仓时冻结策略参数',
    `highest_price_since_entry`       decimal(32, 16) NULL COMMENT '入场后最高价',
    `lowest_price_since_entry`        decimal(32, 16) NULL COMMENT '入场后最低价',
    `maximum_return_rate`             decimal(32, 16) NULL COMMENT '持仓周期实时标记价回报率峰值',
    `highest_step`                    int         NOT NULL DEFAULT 0 COMMENT '最高止盈阶梯',
    `first_take_profit_triggered`     char(1)     NOT NULL DEFAULT '0' COMMENT '首次止盈是否触发',
    `stop_loss_price`                 decimal(32, 16) NULL COMMENT '程序/保护止损价',
    `pending_action`                  varchar(30) NULL COMMENT '待执行动作',
    `pending_idempotency_key`         varchar(128) NULL COMMENT '待执行幂等键',
    `pending_client_order_id`         varchar(128) NULL COMMENT '待执行客户端订单ID',
    `pending_order_id`                bigint NULL COMMENT '待执行订单ID',
    `pending_order_time`              bigint NULL COMMENT '待执行时间戳',
    `recovery_required`               char(1)     NOT NULL DEFAULT '0' COMMENT '是否经历恢复',
    `unsafe_recovery`                 char(1)     NOT NULL DEFAULT '0' COMMENT '恢复是否不安全',
    `pending_not_found_confirmations` int         NOT NULL DEFAULT 0 COMMENT '待定订单未发现确认次数',
    `opened_at`                       datetime NULL COMMENT '开仓时间',
    `updated_at`                      datetime    NOT NULL COMMENT '更新时间',
    `version`                         int         NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quant_strategy_position_state_instance` (`instance_id`),
    KEY                               `idx_quant_strategy_position_state_position` (`position_id`),
    KEY                               `idx_quant_strategy_state_recovery` (`status`,`recovery_required`,`unsafe_recovery`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='策略持仓运行状态事实表';

SET
FOREIGN_KEY_CHECKS = 1;
