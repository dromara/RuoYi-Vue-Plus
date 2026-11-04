-- V20261104_01: durable account-wide position-cycle projection.
-- Raw orders and fills remain authoritative; these tables are rebuildable query projections.
-- Existing commission assets are intentionally left NULL unless an authenticated source is available.

DELIMITER $
DROP PROCEDURE IF EXISTS migrate_account_trade_cycle_projection$
CREATE PROCEDURE migrate_account_trade_cycle_projection()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='quant_trade' AND column_name='commission_asset'
    ) THEN
        ALTER TABLE quant_trade
            ADD COLUMN commission_asset varchar(32) NULL DEFAULT NULL
                COMMENT '交易所成交手续费资产；未知时保留NULL';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema=DATABASE() AND table_name='quant_trade' AND index_name='idx_quant_trade_cycle_scope'
    ) THEN
        CREATE INDEX idx_quant_trade_cycle_scope
            ON quant_trade (credential_id,symbol,trade_time,exchange_trade_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema=DATABASE() AND table_name='quant_order' AND index_name='idx_quant_order_cycle_lookup'
    ) THEN
        CREATE INDEX idx_quant_order_cycle_lookup
            ON quant_order (credential_id,symbol,exchange_order_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema=DATABASE() AND table_name='quant_execution_record' AND index_name='idx_quant_execution_cycle_lookup'
    ) THEN
        CREATE INDEX idx_quant_execution_cycle_lookup
            ON quant_execution_record (credential_id,symbol,exchange_order_id,operation_type);
    END IF;
END$
CALL migrate_account_trade_cycle_projection()$
DROP PROCEDURE migrate_account_trade_cycle_projection$
DELIMITER ;

CREATE TABLE IF NOT EXISTS quant_trade_cycle_scope (
    credential_id bigint NOT NULL,
    symbol varchar(32) NOT NULL,
    dirty_version bigint NOT NULL DEFAULT 0,
    projected_version bigint NOT NULL DEFAULT 0,
    projection_status varchar(16) NOT NULL DEFAULT 'PENDING',
    incomplete_count int NOT NULL DEFAULT 0,
    inferred_count int NOT NULL DEFAULT 0,
    last_error varchar(1000) NULL DEFAULT NULL,
    last_projected_at datetime(3) NULL DEFAULT NULL,
    updated_at datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (credential_id,symbol),
    INDEX idx_quant_trade_cycle_pending (projection_status,updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT='账户交易周期投影范围与版本状态';

CREATE TABLE IF NOT EXISTS quant_trade_cycle (
    id bigint NOT NULL,
    cycle_key varchar(64) NOT NULL,
    credential_id bigint NOT NULL,
    symbol varchar(32) NOT NULL,
    position_side varchar(8) NOT NULL,
    status varchar(24) NOT NULL,
    reconstruction_source varchar(16) NOT NULL,
    reconciliation_status varchar(24) NOT NULL,
    reconciliation_reason varchar(500) NULL DEFAULT NULL,
    opened_at datetime(3) NULL DEFAULT NULL,
    closed_at datetime(3) NULL DEFAULT NULL,
    opening_quantity decimal(28,10) NOT NULL DEFAULT 0,
    opening_notional decimal(38,16) NOT NULL DEFAULT 0,
    entry_price decimal(28,16) NULL DEFAULT NULL,
    closing_quantity decimal(28,10) NOT NULL DEFAULT 0,
    closing_notional decimal(38,16) NOT NULL DEFAULT 0,
    close_price decimal(28,16) NULL DEFAULT NULL,
    remaining_quantity decimal(28,10) NOT NULL DEFAULT 0,
    realized_pnl decimal(28,10) NULL DEFAULT NULL,
    commission_display varchar(1000) NOT NULL DEFAULT '待核实',
    commission_by_asset json NOT NULL,
    fill_count int NOT NULL DEFAULT 0,
    created_at datetime(3) NOT NULL,
    updated_at datetime(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_quant_trade_cycle_key (cycle_key),
    KEY idx_quant_trade_cycle_account_time (credential_id,opened_at,id),
    KEY idx_quant_trade_cycle_symbol_time (symbol,opened_at,id),
    KEY idx_quant_trade_cycle_status (credential_id,status,opened_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT='由实际成交归并得到的账户持仓周期汇总账本';

CREATE TABLE IF NOT EXISTS quant_trade_cycle_fill (
    id bigint NOT NULL,
    cycle_id bigint NOT NULL,
    credential_id bigint NOT NULL,
    symbol varchar(32) NOT NULL,
    exchange_trade_id bigint NOT NULL,
    exchange_order_id bigint NOT NULL,
    allocation_seq int NOT NULL DEFAULT 0,
    fill_role varchar(8) NOT NULL,
    position_side varchar(8) NOT NULL,
    allocated_quantity decimal(28,10) NOT NULL,
    price decimal(28,16) NOT NULL,
    commission decimal(28,10) NULL DEFAULT NULL,
    commission_asset varchar(32) NULL DEFAULT NULL,
    realized_pnl decimal(28,10) NULL DEFAULT NULL,
    trade_time datetime(3) NOT NULL,
    created_at datetime(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_quant_trade_cycle_fill (credential_id,symbol,exchange_trade_id,allocation_seq),
    KEY idx_quant_trade_cycle_fill_cycle (cycle_id,trade_time,id),
    KEY idx_quant_trade_cycle_fill_order (credential_id,symbol,exchange_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT='原始成交在交易周期中的数量、角色及财务分配';

-- Bootstrap every existing account-symbol scope. Projection workers replay the complete
-- local fill chain before the page starts reading this ledger.
INSERT INTO quant_trade_cycle_scope
    (credential_id,symbol,dirty_version,projected_version,projection_status,updated_at)
SELECT credential_id,symbol,1,0,'PENDING',NOW(3)
FROM quant_trade
GROUP BY credential_id,symbol
ON DUPLICATE KEY UPDATE
    dirty_version=GREATEST(dirty_version,projected_version+1),
    projection_status='PENDING',
    updated_at=NOW(3);
