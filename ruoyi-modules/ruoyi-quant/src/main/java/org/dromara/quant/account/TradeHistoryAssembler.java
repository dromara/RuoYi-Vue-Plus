package org.dromara.quant.account;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * 仅根据可关联的实际成交重建持仓周期，不使用当前配置补填历史事实。
 */
public final class TradeHistoryAssembler {
    private TradeHistoryAssembler() {
    }

    /**
     * 按账户、交易对和持仓方向归组成交并生成开平仓周期记录。
     */
    public static Result assemble(List<Fill> fills, Map<Long, String> accountNames) {
        Map<String, Cycle> active = new HashMap<>();
        List<Cycle> cycles = new ArrayList<>();
        Set<String> incomplete = new HashSet<>();
        // 调用方按成交时间、主键排序；不能在时间筛选后截断开仓链。
        for (Fill fill : fills) {
            String scope = fill.credentialId() + ":" + fill.symbol();
            if (incomplete.contains(scope)) {
                continue;
            }
            Classification classification = classify(fill);
            String direction = classification == null ? null : classification.direction();
            boolean open = classification != null && classification.open();
            if (direction == null || fill.tradeTime() == null || !positive(fill.quantity()) || !positive(fill.price())) {
                incomplete.add(scope);
                continue;
            }
            String key = scope + ":" + direction;
            Cycle cycle = active.get(key);
            if (open) {
                if (!("LONG".equals(direction) ? "BUY" : "SELL").equals(fill.side())) {
                    incomplete.add(scope);
                    continue;
                }
                if (cycle == null) {
                    cycle = new Cycle(fill, direction, scope);
                    active.put(key, cycle);
                    cycles.add(cycle);
                }
                cycle.openQty = cycle.openQty.add(fill.quantity());
                cycle.openingFillCount++;
                if (fill.exchangeOrderId() != null) cycle.openingOrderIds.add(String.valueOf(fill.exchangeOrderId()));
                if (fill.exchangeTradeId() != null) cycle.openingTradeIds.add(String.valueOf(fill.exchangeTradeId()));
                cycle.openValue = cycle.openValue.add(fill.quantity().multiply(fill.price()));
                cycle.remaining = cycle.remaining.add(fill.quantity());
                BigDecimal tickSize = positive(fill.priceTickSize()) ? fill.priceTickSize() : fill.currentPriceTickSize();
                if (cycle.priceScale == null && positive(tickSize)) {
                    cycle.priceScale = Math.max(0, Math.min(16, tickSize.stripTrailingZeros().scale()));
                }
                if (cycle.leverage == null && fill.leverage() != null && fill.leverage() > 0) {
                    cycle.leverage = fill.leverage();
                }
                if (fill.postFillWalletBalance() != null) {
                    cycle.openingBalance = fill.postFillWalletBalance();
                    cycle.openingBalanceAt = fill.postFillBalanceAt();
                }
            } else {
                if (cycle == null || fill.quantity().compareTo(cycle.remaining) > 0 || !("LONG".equals(direction) ? "SELL" : "BUY").equals(fill.side())) {
                    incomplete.add(scope);
                    continue;
                }
                cycle.closeQty = cycle.closeQty.add(fill.quantity());
                cycle.closingFillCount++;
                if (fill.exchangeOrderId() != null) cycle.closingOrderIds.add(String.valueOf(fill.exchangeOrderId()));
                if (fill.exchangeTradeId() != null) cycle.closingTradeIds.add(String.valueOf(fill.exchangeTradeId()));
                cycle.closeValue = cycle.closeValue.add(fill.quantity().multiply(fill.price()));
                cycle.remaining = cycle.remaining.subtract(fill.quantity());
                cycle.pnl = addKnown(cycle.pnl, fill.realizedPnl());
                if (fill.postFillWalletBalance() != null) {
                    cycle.closingBalance = fill.postFillWalletBalance();
                    cycle.closingBalanceAt = fill.postFillBalanceAt();
                }
                if (cycle.remaining.signum() == 0) {
                    cycle.closedAt = fill.tradeTime();
                    active.remove(key);
                }
            }
            cycle.fee = addKnown(cycle.fee, fill.commission());
        }
        List<Map<String, Object>> rows =
            cycles.stream().filter(c -> !incomplete.contains(c.scope)).sorted(Comparator.comparing((Cycle c) -> c.openedAt).thenComparing(c -> c.id).reversed()).map(c -> c.row(accountNames)).toList();
        return new Result(rows, incomplete.size());
    }

    private static Classification classify(Fill fill) {
        String direction = direction(fill);
        if (direction == null || (!"BUY".equals(fill.side()) && !"SELL".equals(fill.side()))) return null;
        Boolean open = null;
        if ("OPEN".equals(fill.operationType())) open = true;
        else if ("CLOSE".equals(fill.operationType()) || Boolean.TRUE.equals(fill.reduceOnly())) open = false;
        else if ("BOTH".equalsIgnoreCase(fill.positionSide()) && fill.reduceOnly() != null) open = !fill.reduceOnly();
        else if ("LONG".equals(fill.positionSide()) || "SHORT".equals(fill.positionSide())) {
            open = ("LONG".equals(direction) ? "BUY" : "SELL").equals(fill.side());
        }
        if (open == null) return null;
        boolean expectedOpenSide = ("LONG".equals(direction) ? "BUY" : "SELL").equals(fill.side());
        if ((open && !expectedOpenSide) || (!open && expectedOpenSide)) return null;
        return new Classification(direction, open);
    }

    private static String direction(Fill fill) {
        if ("LONG".equals(fill.positionSide()) || "SHORT".equals(fill.positionSide())) {
            return fill.positionSide();
        }
        // 单向仓的 BUY/SELL 不能直接当作开仓方向；必须有执行账本归因。
        if ("OPEN".equals(fill.operationType())) {
            return "BUY".equals(fill.side()) ? "LONG" : "SELL".equals(fill.side()) ? "SHORT" : null;
        }
        if ("CLOSE".equals(fill.operationType()) || Boolean.TRUE.equals(fill.reduceOnly())) {
            return "SELL".equals(fill.side()) ? "LONG" : "BUY".equals(fill.side()) ? "SHORT" : null;
        }
        if ("BOTH".equalsIgnoreCase(fill.positionSide()) && fill.reduceOnly() != null) {
            return Boolean.TRUE.equals(fill.reduceOnly()) ? ("SELL".equals(fill.side()) ? "LONG" : "BUY".equals(fill.side()) ? "SHORT" : null)
                : ("BUY".equals(fill.side()) ? "LONG" : "SELL".equals(fill.side()) ? "SHORT" : null);
        }
        return null;
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static BigDecimal addKnown(BigDecimal left, BigDecimal right) {
        return left == null || right == null ? null : left.add(right);
    }

    private static String decimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private static String timestamp(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC).toString();
    }

    /**
     * 一笔用于还原开平仓周期的成交事实。
     */
    public record Fill(Long id, Long credentialId, String symbol, String side, String positionSide, String operationType, Boolean reduceOnly, BigDecimal quantity, BigDecimal price,
                       BigDecimal commission, BigDecimal realizedPnl, LocalDateTime tradeTime, Integer leverage, BigDecimal priceTickSize, BigDecimal postFillWalletBalance,
                       LocalDateTime postFillBalanceAt, BigDecimal currentPriceTickSize, Long exchangeTradeId, Long exchangeOrderId) {
        public Fill(Long id, Long credentialId, String symbol, String side, String positionSide, String operationType, Boolean reduceOnly, BigDecimal quantity, BigDecimal price,
                    BigDecimal commission, BigDecimal realizedPnl, LocalDateTime tradeTime, Integer leverage, BigDecimal priceTickSize, BigDecimal postFillWalletBalance,
                    LocalDateTime postFillBalanceAt, BigDecimal currentPriceTickSize, Long exchangeTradeId) {
            this(id, credentialId, symbol, side, positionSide, operationType, reduceOnly, quantity, price, commission, realizedPnl, tradeTime, leverage, priceTickSize,
                postFillWalletBalance, postFillBalanceAt, currentPriceTickSize, exchangeTradeId, null);
        }
        public Fill(Long id, Long credentialId, String symbol, String side, String positionSide, String operationType, Boolean reduceOnly, BigDecimal quantity, BigDecimal price,
                    BigDecimal commission, BigDecimal realizedPnl, LocalDateTime tradeTime, Integer leverage, BigDecimal priceTickSize, BigDecimal postFillWalletBalance,
                    LocalDateTime postFillBalanceAt, BigDecimal currentPriceTickSize) {
            this(id, credentialId, symbol, side, positionSide, operationType, reduceOnly, quantity, price, commission, realizedPnl, tradeTime, leverage, priceTickSize,
                postFillWalletBalance, postFillBalanceAt, currentPriceTickSize, null);
        }

        public Fill(Long id, Long credentialId, String symbol, String side, String positionSide, String operationType, Boolean reduceOnly, BigDecimal quantity, BigDecimal price,
                    BigDecimal commission, BigDecimal realizedPnl, LocalDateTime tradeTime) {
            this(id, credentialId, symbol, side, positionSide, operationType, reduceOnly, quantity, price, commission, realizedPnl, tradeTime, null, null, null, null, null, null);
        }
    }

    private record Classification(String direction, boolean open) {
    }

    /**
     * 周期汇总结果及无法可靠归组的记录数。
     */
    public record Result(List<Map<String, Object>> rows, int incompleteCount) {
    }

    private static final class Cycle {
        private final Long id;
        private final Long credentialId;
        private final String symbol;
        private final String direction;
        private final String scope;
        private final LocalDateTime openedAt;
        private LocalDateTime closedAt;
        private BigDecimal openQty = BigDecimal.ZERO, openValue = BigDecimal.ZERO, remaining = BigDecimal.ZERO;
        private BigDecimal closeQty = BigDecimal.ZERO, closeValue = BigDecimal.ZERO;
        private BigDecimal pnl = BigDecimal.ZERO, fee = BigDecimal.ZERO;
        private int openingFillCount, closingFillCount;
        private final Set<String> openingOrderIds = new LinkedHashSet<>(), closingOrderIds = new LinkedHashSet<>();
        private final Set<String> openingTradeIds = new LinkedHashSet<>(), closingTradeIds = new LinkedHashSet<>();
        private Integer leverage;
        private Integer priceScale;
        private BigDecimal openingBalance, closingBalance;
        private LocalDateTime openingBalanceAt, closingBalanceAt;

        private Cycle(Fill fill, String direction, String scope) {
            this.id = fill.id();
            this.credentialId = fill.credentialId();
            this.symbol = fill.symbol();
            this.direction = direction;
            this.scope = scope;
            this.openedAt = fill.tradeTime();
        }

        private Map<String, Object> row(Map<Long, String> names) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", String.valueOf(id));
            row.put("credentialId", String.valueOf(credentialId));
            row.put("accountName", names.get(credentialId));
            row.put("symbol", symbol);
            row.put("direction", direction);
            row.put("openingQuantity", decimal(openQty));
            row.put("closingQuantity", decimal(closeQty));
            row.put("remainingQuantity", decimal(remaining));
            row.put("openingFillCount", openingFillCount);
            row.put("closingFillCount", closingFillCount);
            row.put("openingOrderIds", List.copyOf(openingOrderIds));
            row.put("closingOrderIds", List.copyOf(closingOrderIds));
            row.put("openingTradeIds", List.copyOf(openingTradeIds));
            row.put("closingTradeIds", List.copyOf(closingTradeIds));
            Integer displayScale = priceScale;
            row.put("entryPrice", price(openValue.divide(openQty, 18, RoundingMode.HALF_UP), displayScale));
            row.put("openingNotional", decimal(openValue));
            row.put("leverage", leverage);
            row.put("openingBalance", fixedDecimal(openingBalance, 4));
            row.put("openingBalanceAt", timestamp(openingBalanceAt));
            row.put("openedAt", timestamp(openedAt));
            row.put("closePrice", closedAt == null ? null : price(closeValue.divide(closeQty, 18, RoundingMode.HALF_UP), displayScale));
            row.put("closingBalance", fixedDecimal(closingBalance, 4));
            row.put("closingBalanceAt", timestamp(closingBalanceAt));
            row.put("realizedPnl", decimal(pnl));
            row.put("commission", price(fee, displayScale));
            row.put("closedAt", timestamp(closedAt));
            row.put("status", closedAt == null ? "OPEN" : "CLOSED");
            return row;
        }

        private static String price(BigDecimal value, Integer scale) {
            return value == null ? null : scale == null ? decimal(value) : value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
        }

        private static String fixedDecimal(BigDecimal value, int scale) {
            return value == null ? null : value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
        }
    }
}
