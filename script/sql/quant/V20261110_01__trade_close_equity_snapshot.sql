-- Persist the exchange account equity captured after a verified close order.
CREATE TABLE IF NOT EXISTS quant_trade_close_equity (
    id bigint NOT NULL AUTO_INCREMENT,
    credential_id bigint NOT NULL,
    symbol varchar(32) NOT NULL,
    exchange_order_id bigint NOT NULL,
    closing_equity decimal(28,10) NOT NULL,
    captured_at datetime(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_quant_trade_close_equity_order (credential_id,symbol,exchange_order_id),
    KEY idx_quant_trade_close_equity_captured (credential_id,captured_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT='每笔平仓订单完成后的交易所账户权益快照';
