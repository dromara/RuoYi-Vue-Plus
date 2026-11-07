-- V20261107_01: persist Binance positionSide on each fill and preserve unknown reduceOnly facts.
-- Safe to re-run: each added column is checked before ALTER TABLE.
DELIMITER $
DROP PROCEDURE IF EXISTS migrate_trade_fill_position_side_and_order_backfill$
CREATE PROCEDURE migrate_trade_fill_position_side_and_order_backfill()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema=DATABASE() AND table_name='quant_trade'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='quant_trade' AND column_name='position_side'
    ) THEN
        ALTER TABLE quant_trade
            ADD COLUMN position_side varchar(20) NULL DEFAULT NULL
                COMMENT 'Binance 成交返回的持仓方向，例如 LONG/SHORT/BOTH' AFTER side;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema=DATABASE() AND table_name='quant_strategy_fill_journal'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name='quant_strategy_fill_journal' AND column_name='position_side'
    ) THEN
        ALTER TABLE quant_strategy_fill_journal
            ADD COLUMN position_side varchar(20) NULL DEFAULT NULL
                COMMENT 'Binance 原始成交返回的持仓方向，例如 LONG/SHORT/BOTH' AFTER side;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema=DATABASE() AND table_name='quant_order'
    ) THEN
        ALTER TABLE quant_order
            MODIFY COLUMN reduce_only char(1) NULL DEFAULT NULL COMMENT '只减仓标记；未知时保留NULL';
    END IF;
END$
CALL migrate_trade_fill_position_side_and_order_backfill()$
DROP PROCEDURE migrate_trade_fill_position_side_and_order_backfill$
DELIMITER ;
