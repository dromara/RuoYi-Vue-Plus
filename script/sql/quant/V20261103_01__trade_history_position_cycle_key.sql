-- V20261103_01: persist strategy position-cycle identity from pre-dispatch through accepted order intent.
-- Existing rows are intentionally left NULL: historical cycle ownership must not be guessed from instance IDs or timestamps.

DELIMITER $
DROP PROCEDURE IF EXISTS migrate_trade_history_position_cycle_key$
CREATE PROCEDURE migrate_trade_history_position_cycle_key()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'quant_strategy_pre_dispatch_intent'
    ) OR NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'quant_strategy_order_intent'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Position-cycle migration blocked: apply the strategy pre-dispatch and order-intent table migrations first';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'quant_strategy_pre_dispatch_intent'
          AND column_name = 'position_cycle_key'
    ) THEN
        ALTER TABLE quant_strategy_pre_dispatch_intent
            ADD COLUMN position_cycle_key varchar(36) NULL DEFAULT NULL
                COMMENT '持仓周期UUID；发单前从策略持仓状态冻结';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'quant_strategy_order_intent'
          AND column_name = 'position_cycle_key'
    ) THEN
        ALTER TABLE quant_strategy_order_intent
            ADD COLUMN position_cycle_key varchar(36) NULL DEFAULT NULL
                COMMENT '订单所属持仓周期UUID；从不可变预派发意图复制';
    END IF;
END$
CALL migrate_trade_history_position_cycle_key()$
DROP PROCEDURE migrate_trade_history_position_cycle_key$
DELIMITER ;
