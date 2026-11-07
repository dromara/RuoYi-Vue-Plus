-- V20261107_02: record provenance for reduceOnly and persist closePosition.
-- Existing reduce_only values were written with a DEFAULT '0'; they cannot be
-- distinguished from an explicit Binance false and must remain unknown.
DELIMITER $
DROP PROCEDURE IF EXISTS migrate_quant_order_reduce_only_provenance$
CREATE PROCEDURE migrate_quant_order_reduce_only_provenance()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema=DATABASE() AND table_name='quant_order') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema=DATABASE() AND table_name='quant_order'
                         AND column_name='reduce_only_source') THEN
            ALTER TABLE quant_order
                ADD COLUMN reduce_only_source varchar(24) NULL
                    COMMENT 'reduceOnly 来源：BINANCE_REST/LOCAL_INTENT/LEGACY_UNKNOWN/UNAVAILABLE'
                    AFTER reduce_only;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema=DATABASE() AND table_name='quant_order'
                         AND column_name='close_position') THEN
            ALTER TABLE quant_order
                ADD COLUMN close_position char(1) NULL DEFAULT NULL
                    COMMENT 'Binance closePosition 原始标记'
                    AFTER reduce_only_source;
        END IF;
        ALTER TABLE quant_order
            MODIFY COLUMN reduce_only char(1) NULL DEFAULT NULL COMMENT '只减仓标记；未知时保留NULL';
        UPDATE quant_order
        SET reduce_only=NULL, reduce_only_source='LEGACY_UNKNOWN'
        WHERE reduce_only_source IS NULL
          AND updated_at >= DATE_SUB(UTC_TIMESTAMP(), INTERVAL 90 DAY);
    END IF;
END$
CALL migrate_quant_order_reduce_only_provenance()$
DROP PROCEDURE migrate_quant_order_reduce_only_provenance$
DELIMITER ;
