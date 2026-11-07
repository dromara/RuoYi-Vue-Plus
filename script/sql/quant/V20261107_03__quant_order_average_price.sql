DELIMITER $$
CREATE PROCEDURE migrate_quant_order_average_price()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema=DATABASE() AND table_name='quant_order') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema=DATABASE() AND table_name='quant_order'
                         AND column_name='average_price') THEN
            ALTER TABLE quant_order
                ADD COLUMN average_price DECIMAL(30,16) NULL DEFAULT NULL
                    COMMENT '交易所确认的累计成交均价'
                    AFTER price;
        END IF;
    END IF;
END$$
CALL migrate_quant_order_average_price()$$
DROP PROCEDURE migrate_quant_order_average_price$$
DELIMITER ;
