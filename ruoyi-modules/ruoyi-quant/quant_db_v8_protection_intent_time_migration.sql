-- V8: persist the local protection-intent creation time.
-- Existing rows intentionally remain NULL: their original creation time cannot be reconstructed safely.
ALTER TABLE `quant_protection_order`
    ADD COLUMN `created_at_epoch_ms` bigint NULL DEFAULT NULL COMMENT '本地保护意图创建时间(epoch毫秒)，历史未知保持NULL' AFTER `client_order_id`;
