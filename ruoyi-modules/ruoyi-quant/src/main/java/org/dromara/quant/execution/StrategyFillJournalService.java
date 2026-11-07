package org.dromara.quant.execution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.execution.mapper.StrategyFillJournalMapper;
import org.dromara.quant.order.domain.QuantOrder;
import org.dromara.quant.order.mapper.QuantOrderMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Stores original exchange fills independently from strategy attribution and retries only exact-order matches.
 */
@Service
@RequiredArgsConstructor
public class StrategyFillJournalService {
    private static final int RETRY_BATCH_SIZE = 100;
    private static final int MAX_REASON_LENGTH = 500;
    private static final String ATTRIBUTED = "ATTRIBUTED";
    private static final String WAITING_ORDER = "WAITING_ORDER";
    private static final String PENDING_FACTS = "PENDING_FACTS";
    private static final String CONFLICT = "CONFLICT";

    private final StrategyFillJournalMapper journal;
    private final StrategyFillAllocationService allocations;
    private final QuantOrderMapper orders;

    /** Persist the immutable exchange fact, then attempt exact strategy attribution. */
    public String attribute(ExchangeGateway.TradeSnapshot fill, QuantOrder order, Long credentialId) {
        StrategyFillJournalRecord record = capture(fill, credentialId);
        if (record.getAttributionStatus() != null && CONFLICT.equals(record.getAttributionStatus())) {
            return CONFLICT;
        }
        if (order == null || order.getStrategyId() == null || order.getStrategyInstanceId() == null) {
            // A capture-only pass must not regress a previously attributed raw fill.
            if (ATTRIBUTED.equals(record.getAttributionStatus())) {
                return ATTRIBUTED;
            }
            return updateStatus(record, WAITING_ORDER, "未找到具有明确策略实例归属的本地订单");
        }
        try {
            allocations.allocate(fill, order, credentialId);
            return updateStatus(record, ATTRIBUTED, null);
        } catch (IllegalStateException incomplete) {
            String message = safeReason(incomplete.getMessage());
            if (message == null) {
                message = "成交归属校验未通过";
            }
            String status = message.contains("与新事实冲突") ? CONFLICT : PENDING_FACTS;
            return updateStatus(record, status, message);
        }
    }

    /**
     * Persist a cross-ledger conflict without rewriting the first exchange fact.
     * Callers must not throw afterward if this quarantine is to commit.
     */
    public void quarantineConflict(Long credentialId, String symbol, Long tradeId, String reason) {
        if (credentialId == null || credentialId <= 0 || symbol == null || symbol.isBlank() || tradeId == null || tradeId <= 0) {
            throw new IllegalArgumentException("Valid raw fill identity required for conflict quarantine");
        }
        StrategyFillJournalRecord record = journal.selectByKey(credentialId, symbol.trim().toUpperCase(Locale.ROOT), tradeId);
        if (record == null) {
            throw new IllegalStateException("Cannot quarantine a fill conflict without its persisted raw exchange fact");
        }
        updateStatus(record, CONFLICT, reason == null || reason.isBlank() ? "跨账本成交事实冲突，需要人工核验" : reason);
    }

    /** Retry durable unresolved fills after their exact local order may have become available. */
    public int retryPending(Long credentialId) {
        List<StrategyFillJournalRecord> pending = journal.selectPending(credentialId, RETRY_BATCH_SIZE);
        int attributed = 0;
        for (StrategyFillJournalRecord record : pending) {
            QuantOrder order = record.getExchangeOrderId() == null ? null : orders.selectOne(new LambdaQueryWrapper<QuantOrder>().eq(QuantOrder::getCredentialId, credentialId)
                .eq(QuantOrder::getSymbol, record.getSymbol()).eq(QuantOrder::getExchangeOrderId, record.getExchangeOrderId()));
            String status = attribute(toSnapshot(record), order, credentialId);
            if (ATTRIBUTED.equals(status)) {
                attributed++;
            }
        }
        return attributed;
    }

    /** List persisted fills that still need attribution review. */
    public List<UnresolvedFill> unresolved(Long credentialId, String symbol, int limit) {
        if (credentialId == null || credentialId <= 0) {
            throw new IllegalArgumentException("缺少有效的量化凭证 ID");
        }
        String normalizedSymbol = symbol == null || symbol.isBlank() ? null : symbol.trim().toUpperCase(Locale.ROOT);
        if (normalizedSymbol != null && !normalizedSymbol.matches("[A-Z0-9]{2,32}")) {
            throw new IllegalArgumentException("交易对格式无效");
        }
        int boundedLimit = Math.max(1, Math.min(200, limit));
        return journal.selectUnresolved(credentialId, normalizedSymbol, boundedLimit).stream().map(StrategyFillJournalService::toUnresolved).toList();
    }

    /** Number of persisted exchange fills that are not yet safely attributed. */
    public long countUnresolved(Long credentialId) {
        return journal.countUnresolved(credentialId);
    }

    private StrategyFillJournalRecord capture(ExchangeGateway.TradeSnapshot fill, Long credentialId) {
        if (fill == null || credentialId == null || credentialId <= 0 || fill.symbol() == null || fill.symbol().isBlank() || fill.tradeId() == null || fill.tradeId() <= 0) {
            throw new IllegalStateException("缺少账户、交易对或交易所成交 ID，无法持久化原始成交");
        }
        String symbol = fill.symbol().trim().toUpperCase(Locale.ROOT);
        StrategyFillJournalRecord existing = journal.selectByKey(credentialId, symbol, fill.tradeId());
        if (existing != null) {
            List<String> conflicts = conflictingFields(existing, fill, credentialId, symbol);
            if (!conflicts.isEmpty()) {
                updateStatus(existing, CONFLICT, "同一成交 ID 对应的 Binance 原始成交字段发生冲突，保留首次事实; conflictingFields=" + String.join(",", conflicts));
                existing.setAttributionStatus(CONFLICT);
                return existing;
            }
            // Early WebSocket/history snapshots can omit commission, realized PnL, or other
            // money facts. A later authenticated REST replay may fill those unknown fields,
            // but may never overwrite a previously persisted non-null exchange fact.
            StrategyFillJournalRecord incoming = fromSnapshot(fill, credentialId, symbol);
            incoming.setId(existing.getId());
            if (journal.enrichMissingFacts(incoming) > 0) {
                existing = journal.selectByKey(credentialId, symbol, fill.tradeId());
            }
            return existing;
        }
        StrategyFillJournalRecord record = fromSnapshot(fill, credentialId, symbol);
        record.setId(IdWorker.getId());
        try {
            if (journal.insertRaw(record) != 1) {
                throw new IllegalStateException("Binance 原始成交事实未写入成交账本");
            }
            record.setAttributionStatus("CAPTURED");
            return record;
        } catch (DuplicateKeyException race) {
            existing = journal.selectByKey(credentialId, symbol, fill.tradeId());
            if (existing == null) {
                throw race;
            }
            List<String> conflicts = conflictingFields(existing, fill, credentialId, symbol);
            if (!conflicts.isEmpty()) {
                updateStatus(existing, CONFLICT, "同一成交 ID 对应的 Binance 原始成交字段发生冲突，保留首次事实; conflictingFields=" + String.join(",", conflicts));
                existing.setAttributionStatus(CONFLICT);
            }
            return existing;
        }
    }

    private String updateStatus(StrategyFillJournalRecord record, String status, String reason) {
        String normalizedReason = safeReason(reason);
        if (Objects.equals(record.getAttributionStatus(), status) && Objects.equals(record.getStatusReason(), normalizedReason) && ATTRIBUTED.equals(status)) {
            return status;
        }
        if (journal.updateStatus(record.getId(), status, normalizedReason) != 1) {
            throw new IllegalStateException("Binance 成交归属处理状态未能持久化");
        }
        record.setAttributionStatus(status);
        record.setStatusReason(normalizedReason);
        return status;
    }

    private static List<String> conflictingFields(StrategyFillJournalRecord record, ExchangeGateway.TradeSnapshot fill, Long credentialId, String symbol) {
        List<String> conflicts = new ArrayList<>();
        if (!Objects.equals(record.getCredentialId(), credentialId)) conflicts.add("credentialId");
        if (!symbol.equalsIgnoreCase(record.getSymbol())) conflicts.add("symbol");
        if (!Objects.equals(record.getExchangeTradeId(), fill.tradeId())) conflicts.add("exchangeTradeId");
        if (bothKnown(record.getExchangeOrderId(), fill.orderId()) && !Objects.equals(record.getExchangeOrderId(), fill.orderId())) conflicts.add("exchangeOrderId");
        if (bothKnown(record.getSide(), fill.side()) && !equalText(record.getSide(), fill.side(), true)) conflicts.add("side");
        if (bothKnown(record.getPositionSide(), fill.positionSide()) && !equalText(record.getPositionSide(), fill.positionSide(), true)) conflicts.add("positionSide");
        if (bothKnown(record.getQuantity(), fill.quantity()) && !equalDecimal(record.getQuantity(), fill.quantity())) conflicts.add("quantity");
        if (bothKnown(record.getPrice(), fill.price()) && !equalDecimal(record.getPrice(), fill.price())) conflicts.add("price");
        if (bothKnown(record.getCommission(), fill.commission()) && !equalDecimal(record.getCommission(), fill.commission())) conflicts.add("commission");
        if (bothKnown(record.getCommissionAsset(), fill.commissionAsset()) && !equalText(record.getCommissionAsset(), fill.commissionAsset(), true)) conflicts.add("commissionAsset");
        if (bothKnown(record.getRealizedPnl(), fill.realizedPnl()) && !equalDecimal(record.getRealizedPnl(), fill.realizedPnl())) conflicts.add("realizedPnl");
        if (record.getTradeTime() != null && fill.time() != null && !equalTime(record.getTradeTime(), fill.time())) conflicts.add("tradeTime");
        return conflicts;
    }

    private static boolean bothKnown(Object left, Object right) {
        return left != null && right != null;
    }

    private static boolean equalText(String left, String right, boolean ignoreCase) {
        return left == null || right == null ? left == null && right == null : ignoreCase ? left.equalsIgnoreCase(right) : left.equals(right);
    }

    private static boolean equalDecimal(BigDecimal left, BigDecimal right) {
        return left == null || right == null ? left == null && right == null : left.compareTo(right) == 0;
    }

    private static boolean equalTime(LocalDateTime stored, Instant incoming) {
        if (stored == null || incoming == null) {
            return stored == null && incoming == null;
        }
        return stored.truncatedTo(ChronoUnit.MILLIS).equals(LocalDateTime.ofInstant(incoming, ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS));
    }

    private static StrategyFillJournalRecord fromSnapshot(ExchangeGateway.TradeSnapshot fill, Long credentialId, String symbol) {
        StrategyFillJournalRecord record = new StrategyFillJournalRecord();
        record.setCredentialId(credentialId);
        record.setSymbol(symbol);
        record.setExchangeTradeId(fill.tradeId());
        record.setExchangeOrderId(fill.orderId());
        record.setSide(fill.side() == null ? null : fill.side().toUpperCase(Locale.ROOT));
        record.setPositionSide(fill.positionSide() == null ? null : fill.positionSide().toUpperCase(Locale.ROOT));
        record.setQuantity(fill.quantity());
        record.setPrice(fill.price());
        record.setCommission(fill.commission());
        record.setCommissionAsset(fill.commissionAsset() == null ? null : fill.commissionAsset().toUpperCase(Locale.ROOT));
        record.setRealizedPnl(fill.realizedPnl());
        record.setTradeTime(fill.time() == null ? null : LocalDateTime.ofInstant(fill.time(), ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS));
        return record;
    }

    private static ExchangeGateway.TradeSnapshot toSnapshot(StrategyFillJournalRecord record) {
        Instant time = record.getTradeTime() == null ? null : record.getTradeTime().toInstant(ZoneOffset.UTC);
        return new ExchangeGateway.TradeSnapshot(record.getSymbol(), record.getExchangeTradeId(), record.getExchangeOrderId(), record.getSide(), record.getQuantity(),
            record.getPrice(), record.getCommission(), record.getRealizedPnl(), time, record.getCommissionAsset(), record.getPositionSide());
    }

    private static UnresolvedFill toUnresolved(StrategyFillJournalRecord record) {
        return new UnresolvedFill(record.getCredentialId().toString(), record.getSymbol(), record.getExchangeTradeId().toString(),
            record.getExchangeOrderId() == null ? null : record.getExchangeOrderId().toString(), record.getSide(), record.getPositionSide(), decimal(record.getQuantity()), decimal(record.getPrice()),
            decimal(record.getCommission()), record.getCommissionAsset(), decimal(record.getRealizedPnl()), record.getTradeTime(), record.getAttributionStatus(),
            record.getStatusReason(), record.getAttemptCount(), record.getLastAttemptAt());
    }

    private static String decimal(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    private static String safeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String sanitized = reason.replaceAll("[\\r\\n\\t]", " ").trim();
        return sanitized.length() <= MAX_REASON_LENGTH ? sanitized : sanitized.substring(0, MAX_REASON_LENGTH);
    }

    /** Lossless review view for raw fills that are not yet attributed. */
    public record UnresolvedFill(String credentialId, String symbol, String exchangeTradeId, String exchangeOrderId, String side, String positionSide, String quantity, String price,
                                 String commission, String commissionAsset, String realizedPnl, LocalDateTime tradeTime, String attributionStatus, String statusReason,
                                 Integer attemptCount, LocalDateTime lastAttemptAt) {
    }
}
