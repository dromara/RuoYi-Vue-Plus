-- V20261103_02: persist account-level futures position cycles and per-fill allocations.
-- Existing strategy-instance position-cycle keys remain separate and are not reused here.

DELIMITER $

DROP PROCEDURE IF EXISTS migrate_account_position_cycle_projection$
CREATE PROCEDURE migrate_account_position_cycle_projection()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema=DATABASE() AND table_name='quant_trade'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema=DATABASE() AND table_name='quant_order'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT='Account position-cycle migration blocked: quant_trade and quant_order must exist first';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='quant_trade'
          AND column_name='commission_asset'
    ) THEN
        ALTER TABLE quant_trade
            ADD COLUMN commission_asset varchar(20) NULL
                COMMENT 'Binance 成交手续费资产；不同资产不得混合求和' AFTER commission;
    END IF;
END$

CALL migrate_account_position_cycle_projection()$
DROP PROCEDURE migrate_account_position_cycle_projection$

DELIMITER ;

CREATE TABLE IF NOT EXISTS quant_position_cycle_state
(
    credential_id             bigint          NOT NULL COMMENT '量化凭证ID',
    symbol                    varchar(32)     NOT NULL COMMENT '交易对',
    position_side             varchar(8)      NOT NULL COMMENT '交易所持仓模式方向：BOTH/LONG/SHORT/UNKNOWN',
    signed_quantity           decimal(32,16)  NOT NULL DEFAULT 0 COMMENT '当前投影数量；BOTH为有符号净仓，对冲方向为非负数量',
    active_cycle_id           bigint          NULL COMMENT '当前活动账户持仓周期ID',
    last_trade_time           datetime        NULL COMMENT '最近已投影成交时间（UTC）',
    last_exchange_trade_id    bigint          NULL COMMENT '最近已投影的交易所成交ID',
    projected_through         datetime(3)     NULL COMMENT '初始化快照已覆盖至的时间（UTC）',
    status                    varchar(20)     NOT NULL DEFAULT 'INITIALIZING' COMMENT 'READY/INITIALIZING/INCOMPLETE',
    incomplete                tinyint(1)      NOT NULL DEFAULT 0 COMMENT '是否存在未知历史基准或不完整事实',
    status_reason             varchar(500)    NULL COMMENT '投影隔离原因',
    created_at                datetime(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at                datetime(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (credential_id,symbol,position_side),
    KEY idx_quant_position_cycle_state_status (status,incomplete,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='账户级持仓周期投影状态';

CREATE TABLE IF NOT EXISTS quant_position_cycle
(
    id                       bigint          NOT NULL COMMENT '主键ID',
    cycle_key                varchar(36)     NOT NULL COMMENT '账户级持仓周期UUID',
    credential_id            bigint          NOT NULL COMMENT '量化凭证ID',
    symbol                   varchar(32)     NOT NULL COMMENT '交易对',
    position_side            varchar(8)      NOT NULL COMMENT '交易所持仓模式方向：BOTH/LONG/SHORT',
    direction                varchar(8)      NOT NULL COMMENT '本周期方向：LONG/SHORT',
    status                   varchar(24)     NOT NULL COMMENT 'OPEN/CLOSED/OPENING_UNKNOWN/CLOSED_INCOMPLETE',
    baseline_unknown         tinyint(1)      NOT NULL DEFAULT 0 COMMENT '周期开始于本地可用成交历史之前',
    opening_quantity         decimal(32,16)  NOT NULL DEFAULT 0 COMMENT '本周期开仓数量；未知基准只记录数量',
    closing_quantity         decimal(32,16)  NOT NULL DEFAULT 0 COMMENT '本周期累计平仓数量',
    remaining_quantity       decimal(32,16)  NOT NULL DEFAULT 0 COMMENT '本周期剩余数量',
    opening_notional         decimal(32,16)  NULL COMMENT '开仓成交额；基准未知时为空',
    closing_notional         decimal(32,16)  NOT NULL DEFAULT 0 COMMENT '累计平仓成交额',
    realized_pnl             decimal(32,16)  NOT NULL DEFAULT 0 COMMENT '本周期已实现盈亏',
    opening_fill_count       int             NOT NULL DEFAULT 0 COMMENT '开仓成交笔数',
    closing_fill_count       int             NOT NULL DEFAULT 0 COMMENT '平仓成交笔数',
    opened_at                datetime        NULL COMMENT '本周期首次开仓成交时间（UTC）；未知基准为空',
    closed_at                datetime        NULL COMMENT '本周期完全平仓时间（UTC）',
    leverage_snapshot        int             NULL COMMENT '开仓时杠杆快照',
    price_tick_size_snapshot decimal(32,16)  NULL COMMENT '开仓时价格最小变动单位',
    opening_balance          decimal(32,16)  NULL COMMENT '可核实的开仓成交后账户钱包余额',
    opening_balance_at       datetime        NULL COMMENT '开仓余额快照时间（UTC）',
    closing_balance          decimal(32,16)  NULL COMMENT '可核实的平仓成交后账户钱包余额',
    closing_balance_at       datetime        NULL COMMENT '平仓余额快照时间（UTC）',
    created_at               datetime(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at               datetime(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quant_position_cycle_key (cycle_key),
    KEY idx_quant_position_cycle_page (credential_id,symbol,opened_at,id),
    KEY idx_quant_position_cycle_status (credential_id,symbol,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='账户级开平仓周期投影';

CREATE TABLE IF NOT EXISTS quant_trade_cycle_allocation
(
    id                    bigint          NOT NULL COMMENT '主键ID',
    credential_id         bigint          NOT NULL COMMENT '量化凭证ID',
    symbol                varchar(32)     NOT NULL COMMENT '交易对',
    exchange_trade_id     bigint          NOT NULL COMMENT '交易所成交ID',
    exchange_order_id     bigint          NOT NULL COMMENT '交易所订单ID',
    position_cycle_id     bigint          NOT NULL COMMENT '账户持仓周期ID',
    allocation_role       varchar(8)      NOT NULL COMMENT 'OPEN/CLOSE',
    allocated_quantity    decimal(32,16)  NOT NULL COMMENT '分配给该周期的成交数量',
    allocated_price       decimal(32,16)  NOT NULL COMMENT '交易所成交价格',
    commission            decimal(32,16)  NOT NULL DEFAULT 0 COMMENT '按分配数量拆分的手续费金额',
    commission_asset      varchar(20)     NULL COMMENT '手续费资产；UNKNOWN/NULL 不与其他资产混算',
    realized_pnl          decimal(32,16)  NOT NULL DEFAULT 0 COMMENT '分配给本次平仓的已实现盈亏',
    trade_time            datetime        NOT NULL COMMENT '成交时间（UTC）',
    created_at            datetime(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_quant_trade_cycle_fill_role (credential_id,symbol,exchange_trade_id,allocation_role),
    KEY idx_quant_trade_cycle_allocation_cycle (position_cycle_id,trade_time,exchange_trade_id),
    KEY idx_quant_trade_cycle_allocation_fill (credential_id,symbol,exchange_trade_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='账户成交到持仓周期的逐笔分配';

DELIMITER ;
