-- Allow wallet-only or equity-only exchange snapshots to be stored independently.
-- A missing account field must not suppress the other verified field.
ALTER TABLE quant_trade_close_equity
    MODIFY COLUMN closing_equity DECIMAL(28,10) NULL COMMENT '平仓后账户权益';
