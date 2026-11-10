-- Extend the close-time account snapshot with the wallet balance and unrealized PnL.
-- Keep the exchange order as the immutable reference to the verified close event.
ALTER TABLE quant_trade_close_equity
    ADD COLUMN wallet_balance DECIMAL(28,10) NULL COMMENT '平仓后钱包余额' AFTER closing_equity,
    ADD COLUMN unrealized_pnl DECIMAL(28,10) NULL COMMENT '平仓后未实现盈亏' AFTER wallet_balance;
