package org.dromara.quant.account;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.dromara.quant.account.domain.vo.TradeCycleFillRecord;
import org.dromara.quant.account.domain.vo.TradeCycleRecord;
import org.dromara.quant.account.domain.vo.TradeCycleSourceFill;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministically reconstructs account position lifecycles from ordered exchange fills.
 * This runs only while updating the durable projection, never in a page request.
 */
public final class TradeCycleAssembler {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final int MONEY_SCALE = 18;

    private TradeCycleAssembler() {
    }

    public static Result assemble(List<TradeCycleSourceFill> fills) {
        TradeCycleSourceFill inconsistentOrder = inconsistentOrderFill(fills);
        if (inconsistentOrder != null) {
            inconsistentOrder.setSourceConflict(true);
        }
        List<Cycle> cycles = new ArrayList<>();
        List<TradeCycleFillRecord> allocations = new ArrayList<>();
        Map<String, Cycle> explicitCycles = new LinkedHashMap<>();
        int incompleteCount = 0;
        boolean blocked = false;

        for (TradeCycleSourceFill fill : fills) {
            if (blocked) {
                continue;
            }
            try {
                validate(fill);
                String side = upper(fill.getSide());
                String positionSide = logicalSide(fill.getPositionSide());
                String exchangePositionSide = upper(fill.getExchangePositionSide());
                boolean hedgeMode = "LONG".equals(exchangePositionSide) || "SHORT".equals(exchangePositionSide);
                if (hedgeMode) {
                    processHedge(fill, side, positionSide == null ? exchangePositionSide : positionSide, cycles, allocations, explicitCycles);
                } else {
                    processOneWay(fill, side, positionSide, cycles, allocations, explicitCycles);
                }
            } catch (IllegalStateException conflict) {
                incompleteCount = 1;
                for (Cycle cycle : cycles) {
                    if (cycle.remainingQuantity.signum() > 0) {
                        cycle.reconciliationStatus = "RECONCILE_REQUIRED";
                        cycle.reconciliationReason = safeReason(conflict.getMessage());
                    }
                }
                Cycle issue = Cycle.issue(fill, safeReason(conflict.getMessage()));
                cycles.add(issue);
                allocations.add(issue.allocation(fill, 0, "UNKNOWN", fill.getQuantity(), null, null));
                blocked = true;
            }
        }

        cycles.sort(Comparator.comparing((Cycle cycle) -> cycle.openedAt,
            Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(cycle -> cycle.id, Comparator.reverseOrder()));
        int inferredCount = (int) cycles.stream().filter(cycle -> "INFERRED".equals(cycle.reconstructionSource)).count();
        return new Result(cycles.stream().map(Cycle::toRecord).toList(), List.copyOf(allocations), incompleteCount, inferredCount);
    }

    private static TradeCycleSourceFill inconsistentOrderFill(List<TradeCycleSourceFill> fills) {
        Map<String, OrderFillTotal> totals = new LinkedHashMap<>();
        for (TradeCycleSourceFill fill : fills) {
            if (fill.getOrderExecutedQuantity() == null || fill.getExchangeOrderId() == null) {
                continue;
            }
            String key = fill.getCredentialId() + ":" + fill.getSymbol() + ":" + fill.getExchangeOrderId();
            OrderFillTotal total = totals.computeIfAbsent(key,
                ignored -> new OrderFillTotal(fill.getOrderExecutedQuantity(), fill));
            if (total.executedQuantity().compareTo(fill.getOrderExecutedQuantity()) != 0) {
                return fill;
            }
            total.add(fill.getQuantity());
        }
        for (OrderFillTotal total : totals.values()) {
            if (total.quantity().compareTo(total.executedQuantity()) != 0) {
                return total.firstFill();
            }
        }
        return null;
    }

    private static void processHedge(TradeCycleSourceFill fill, String side, String direction,
                                     List<Cycle> cycles, List<TradeCycleFillRecord> allocations,
                                     Map<String, Cycle> explicitCycles) {
        String openSide = "LONG".equals(direction) ? "BUY" : "SELL";
        String role = role(fill);
        if (role == null) {
            role = openSide.equals(side) ? "OPEN" : "CLOSE";
        }
        if ("OPEN".equals(role) != openSide.equals(side)
            || (Boolean.TRUE.equals(fill.getReduceOnly()) || Boolean.TRUE.equals(fill.getClosePosition())) && "OPEN".equals(role)) {
            throw new IllegalStateException("持仓方向、成交方向和开平仓意图冲突");
        }

        String cycleKey = normalizedCycleKey(fill.getPositionCycleKey(), fill);
        if ("OPEN".equals(role)) {
            Cycle target = openCycle(fill, direction, cycleKey, cycles, explicitCycles, true, "EXCHANGE");
            applySegments(fill, List.of(new Segment(target, "OPEN", fill.getQuantity())), allocations, "EXCHANGE");
            return;
        }

        Cycle keyedTarget = cycleKey == null ? null : explicitCycles.get(cycleKey);
        if (keyedTarget != null) {
            if (!direction.equals(keyedTarget.positionSide) || !keyedTarget.active()) {
                throw new IllegalStateException("平仓成交无法关联到有效的账户持仓周期");
            }
            closeFromTargets(fill, List.of(keyedTarget), allocations, cycleKey);
        } else {
            closeFromTargets(fill, activeCycles(cycles, direction, true), allocations, null);
        }
    }

    private static void processOneWay(TradeCycleSourceFill fill, String side, String directionHint,
                                     List<Cycle> cycles, List<TradeCycleFillRecord> allocations,
                                     Map<String, Cycle> explicitCycles) {
        String sideDirection = "BUY".equals(side) ? "LONG" : "SHORT";
        String role = role(fill);
        boolean reduceOnly = Boolean.TRUE.equals(fill.getReduceOnly()) || Boolean.TRUE.equals(fill.getClosePosition());
        if (role == null && directionHint != null) {
            role = side.equals("LONG".equals(directionHint) ? "BUY" : "SELL") ? "OPEN" : "CLOSE";
        }
        if (reduceOnly && "OPEN".equals(role)) {
            throw new IllegalStateException("reduceOnly 订单不能归类为开仓");
        }
        String activeDirection = activeOneWayDirection(cycles);
        String cycleKey = normalizedCycleKey(fill.getPositionCycleKey(), fill);
        if ("CLOSE".equals(role) || reduceOnly) {
            String closeDirection = directionHint != null ? directionHint
                : activeDirection != null ? activeDirection : opposite(sideDirection);
            if (side.equals("LONG".equals(closeDirection) ? "BUY" : "SELL")) {
                throw new IllegalStateException("平仓成交方向与持仓方向不匹配");
            }
            Cycle keyedTarget = cycleKey == null ? null : explicitCycles.get(cycleKey);
            if (keyedTarget != null) {
                if (!closeDirection.equals(keyedTarget.positionSide) || !keyedTarget.oneWay || !keyedTarget.active()) {
                    throw new IllegalStateException("平仓成交无法关联到有效的账户持仓周期");
                }
                closeFromTargets(fill, List.of(keyedTarget), allocations, cycleKey);
            } else {
                closeFromTargets(fill, activeCycles(cycles, closeDirection, false), allocations, null);
            }
            return;
        }

        String openingDirection = directionHint == null ? sideDirection : directionHint;
        if (!side.equals("LONG".equals(openingDirection) ? "BUY" : "SELL")) {
            throw new IllegalStateException("开仓成交方向与持仓方向不匹配");
        }

        String reconstructionSource = role == null && directionHint == null ? "INFERRED" : "EXCHANGE";
        if (activeDirection != null && !activeDirection.equals(openingDirection)) {
            if (reduceOnly) {
                throw new IllegalStateException("只减仓成交不能反向建立新持仓");
            }
            BigDecimal activeQuantity = activeCycles(cycles, activeDirection, false).stream()
                .map(cycle -> cycle.remainingQuantity).reduce(ZERO, BigDecimal::add);
            BigDecimal closeQuantity = fill.getQuantity().min(activeQuantity);
            List<Segment> segments = closingSegments(closeQuantity, activeCyclesForMode(cycles, activeDirection, false), null);
            BigDecimal openingRemainder = fill.getQuantity().subtract(closeQuantity);
            if (openingRemainder.signum() > 0) {
                Cycle target = openCycle(fill, openingDirection, cycleKey, cycles, explicitCycles, false, reconstructionSource);
                segments.add(new Segment(target, "OPEN", openingRemainder));
            }
            applySegments(fill, segments, allocations, reconstructionSource);
            return;
        }

        Cycle target = openCycle(fill, openingDirection, cycleKey, cycles, explicitCycles, false, reconstructionSource);
        applySegments(fill, List.of(new Segment(target, "OPEN", fill.getQuantity())), allocations, reconstructionSource);
    }

    private static Cycle openCycle(TradeCycleSourceFill fill, String direction, String cycleKey,
                                   List<Cycle> cycles, Map<String, Cycle> explicitCycles,
                                   boolean hedgeMode, String reconstructionSource) {
        Cycle target = cycleKey == null ? null : explicitCycles.get(cycleKey);
        if (target != null && !target.active()) {
            throw new IllegalStateException("已结束的持仓周期标识被再次用于开仓");
        }
        if (target == null) {
            target = activeCyclesForMode(cycles, direction, hedgeMode).stream().findFirst().orElse(null);
        }
        if (target == null) {
            target = new Cycle(fill, direction, hedgeMode, cycleKey, reconstructionSource);
            cycles.add(target);
        } else if ("INFERRED".equals(reconstructionSource)) {
            target.reconstructionSource = "INFERRED";
            target.reconciliationStatus = "INFERRED";
        }
        if (cycleKey != null) {
            Cycle previous = explicitCycles.putIfAbsent(cycleKey, target);
            if (previous != null && previous != target) {
                throw new IllegalStateException("同一策略持仓周期标识关联了不同账户持仓周期");
            }
        }
        return target;
    }

    private static void closeFromTargets(TradeCycleSourceFill fill, List<Cycle> targets,
                                         List<TradeCycleFillRecord> allocations, String requiredCycleKey) {
        closeFromTargets(fill, targets, allocations, requiredCycleKey, fill.getQuantity());
    }

    private static void closeFromTargets(TradeCycleSourceFill fill, List<Cycle> targets,
                                         List<TradeCycleFillRecord> allocations, String requiredCycleKey,
                                         BigDecimal closeQuantity) {
        List<Segment> segments = closingSegments(closeQuantity, targets, requiredCycleKey);
        applySegments(fill, segments, allocations, null);
    }

    private static List<Segment> closingSegments(BigDecimal closeQuantity, List<Cycle> targets, String requiredCycleKey) {
        BigDecimal remaining = closeQuantity;
        List<Segment> segments = new ArrayList<>();
        for (Cycle cycle : targets) {
            if (remaining.signum() <= 0) {
                break;
            }
            if (!cycle.active()) {
                continue;
            }
            BigDecimal take = cycle.remainingQuantity.min(remaining);
            segments.add(new Segment(cycle, "CLOSE", take));
            remaining = remaining.subtract(take);
        }
        if (remaining.signum() > 0) {
            throw new IllegalStateException(requiredCycleKey == null
                ? "平仓成交数量超过可核实的剩余持仓数量"
                : "平仓成交数量超过指定持仓周期的剩余数量");
        }
        return segments;
    }

    private static void applySegments(TradeCycleSourceFill fill, List<Segment> segments,
                                      List<TradeCycleFillRecord> allocations, String reconstructionSource) {
        BigDecimal totalQuantity = fill.getQuantity();
        BigDecimal totalCloseQuantity = segments.stream().filter(segment -> "CLOSE".equals(segment.role()))
            .map(Segment::quantity).reduce(ZERO, BigDecimal::add);
        BigDecimal totalFee = fill.getCommission();
        BigDecimal feeAssigned = ZERO;
        BigDecimal pnlAssigned = ZERO;
        int closeIndex = 0;
        int closeCount = (int) segments.stream().filter(segment -> "CLOSE".equals(segment.role())).count();
        for (int index = 0; index < segments.size(); index++) {
            Segment segment = segments.get(index);
            boolean lastSegment = index == segments.size() - 1;
            BigDecimal feePart = totalFee == null ? null : lastSegment
                ? totalFee.subtract(feeAssigned)
                : totalFee.multiply(segment.quantity()).divide(totalQuantity, MONEY_SCALE, RoundingMode.HALF_UP);
            if (feePart != null) {
                feeAssigned = feeAssigned.add(feePart);
            }
            BigDecimal pnlPart = null;
            if ("CLOSE".equals(segment.role()) && fill.getRealizedPnl() != null) {
                closeIndex++;
                pnlPart = closeIndex == closeCount ? fill.getRealizedPnl().subtract(pnlAssigned)
                    : fill.getRealizedPnl().multiply(segment.quantity()).divide(totalCloseQuantity, MONEY_SCALE, RoundingMode.HALF_UP);
                pnlAssigned = pnlAssigned.add(pnlPart);
            }
            String source = reconstructionSource == null ? "EXCHANGE" : reconstructionSource;
            segment.cycle().apply(segment.role(), segment.quantity(), fill, feePart, pnlPart, source);
            allocations.add(segment.cycle().allocation(fill, allocationsForFill(allocations, fill) , segment.role(),
                segment.quantity(), feePart, pnlPart));
        }
    }

    private static int allocationsForFill(List<TradeCycleFillRecord> allocations, TradeCycleSourceFill fill) {
        int count = 0;
        for (int i = allocations.size() - 1; i >= 0; i--) {
            TradeCycleFillRecord item = allocations.get(i);
            if (!Objects.equals(item.getCredentialId(), fill.getCredentialId())
                || !Objects.equals(item.getSymbol(), fill.getSymbol())
                || !Objects.equals(item.getExchangeTradeId(), fill.getExchangeTradeId())) {
                break;
            }
            count++;
        }
        return count;
    }

    private static void validate(TradeCycleSourceFill fill) {
        if (fill == null || fill.getCredentialId() == null || fill.getExchangeTradeId() == null
            || fill.getExchangeOrderId() == null || fill.getSymbol() == null || fill.getSymbol().isBlank()
            || fill.getTradeTime() == null || fill.getQuantity() == null || fill.getQuantity().signum() <= 0
            || fill.getPrice() == null || fill.getPrice().signum() <= 0
            || !List.of("BUY", "SELL").contains(upper(fill.getSide()))) {
            throw new IllegalStateException("成交事实缺少账户、订单、方向、数量、价格或成交时间");
        }
        if (Boolean.TRUE.equals(fill.getSourceConflict())) {
            throw new IllegalStateException("成交来源、订单累计成交数量或订单归因事实互相冲突");
        }
        String role = role(fill);
        if (role != null && !List.of("OPEN", "CLOSE").contains(role)) {
            throw new IllegalStateException("订单操作类型不是有效的 OPEN/CLOSE");
        }
    }

    private static List<Cycle> activeCycles(List<Cycle> cycles, String direction, boolean hedgeMode) {
        return activeCyclesForMode(cycles, direction, hedgeMode);
    }

    private static List<Cycle> activeCyclesForMode(List<Cycle> cycles, String direction, boolean hedgeMode) {
        return cycles.stream()
            .filter(cycle -> cycle.remainingQuantity.signum() > 0 && cycle.oneWay == !hedgeMode
                && cycle.positionSide.equals(direction))
            .sorted(Comparator.comparing(cycle -> cycle.openedAt))
            .toList();
    }

    private static String activeOneWayDirection(List<Cycle> cycles) {
        List<String> directions = cycles.stream()
            .filter(cycle -> cycle.oneWay && cycle.remainingQuantity.signum() > 0)
            .map(cycle -> cycle.positionSide).distinct().toList();
        if (directions.size() > 1) {
            throw new IllegalStateException("单向持仓模式同时存在多空两侧活动周期，账本状态不一致");
        }
        return directions.isEmpty() ? null : directions.getFirst();
    }

    private static String normalizedCycleKey(String sourceKey, TradeCycleSourceFill fill) {
        if (sourceKey == null || sourceKey.isBlank()) {
            return null;
        }
        return digest(fill.getCredentialId() + ":" + fill.getSymbol().toUpperCase(Locale.ROOT) + ":" + sourceKey.trim());
    }

    private static String role(TradeCycleSourceFill fill) {
        String value = upper(fill.getFillRole());
        return "OPEN".equals(value) || "CLOSE".equals(value) ? value : null;
    }

    private static String logicalSide(String value) {
        String normalized = upper(value);
        return "LONG".equals(normalized) || "SHORT".equals(normalized) ? normalized : null;
    }

    private static String upper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String opposite(String direction) {
        return "LONG".equals(direction) ? "SHORT" : "LONG";
    }

    private static String safeReason(String value) {
        if (value == null || value.isBlank()) {
            return "成交周期无法可靠归因";
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private static String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static final class OrderFillTotal {
        private final BigDecimal executedQuantity;
        private final TradeCycleSourceFill firstFill;
        private BigDecimal quantity = ZERO;

        private OrderFillTotal(BigDecimal executedQuantity, TradeCycleSourceFill firstFill) {
            this.executedQuantity = executedQuantity;
            this.firstFill = firstFill;
        }

        private BigDecimal executedQuantity() {
            return executedQuantity;
        }

        private BigDecimal quantity() {
            return quantity;
        }

        private void add(BigDecimal value) {
            quantity = quantity.add(value);
        }

        private TradeCycleSourceFill firstFill() {
            return firstFill;
        }
    }

    private record Segment(Cycle cycle, String role, BigDecimal quantity) {
    }

    public record Result(List<TradeCycleRecord> cycles, List<TradeCycleFillRecord> allocations,
                         int incompleteCount, int inferredCount) {
    }

    private static final class Cycle {
        private final long id = IdWorker.getId();
        private final String cycleKey;
        private final Long credentialId;
        private final String symbol;
        private final String positionSide;
        private final boolean oneWay;
        private final String positionCycleKey;
        private LocalDateTime openedAt;
        private LocalDateTime closedAt;
        private BigDecimal openingQuantity = ZERO;
        private BigDecimal openingNotional = ZERO;
        private BigDecimal closingQuantity = ZERO;
        private BigDecimal closingNotional = ZERO;
        private BigDecimal remainingQuantity = ZERO;
        private BigDecimal realizedPnl = ZERO;
        private boolean pnlComplete = true;
        private boolean commissionComplete = true;
        private final Map<String, BigDecimal> fees = new LinkedHashMap<>();
        private String status = "OPEN";
        private String reconstructionSource;
        private String reconciliationStatus;
        private String reconciliationReason;
        private int fillCount;

        private Cycle(TradeCycleSourceFill fill, String direction, boolean hedgeMode, String positionCycleKey, String source) {
            this.cycleKey = positionCycleKey == null
                ? digest(fill.getCredentialId() + ":" + fill.getSymbol() + ":" + direction + ":" + fill.getExchangeTradeId())
                : positionCycleKey;
            this.credentialId = fill.getCredentialId();
            this.symbol = fill.getSymbol().toUpperCase(Locale.ROOT);
            this.positionSide = direction;
            this.oneWay = !hedgeMode;
            this.positionCycleKey = positionCycleKey;
            this.openedAt = fill.getTradeTime();
            this.reconstructionSource = source;
            this.reconciliationStatus = "INFERRED".equals(source) ? "INFERRED" : "VERIFIED";
        }

        private Cycle(TradeCycleSourceFill fill, String reason) {
            this.cycleKey = digest(fill.getCredentialId() + ":" + fill.getSymbol() + ":UNRESOLVED:" + fill.getExchangeTradeId());
            this.credentialId = fill.getCredentialId();
            this.symbol = fill.getSymbol() == null ? "" : fill.getSymbol().toUpperCase(Locale.ROOT);
            String side = logicalSide(fill.getPositionSide());
            this.positionSide = side == null ? "UNKNOWN" : side;
            this.oneWay = false;
            this.positionCycleKey = null;
            this.openedAt = fill.getTradeTime();
            this.status = "RECONCILE_REQUIRED";
            this.reconstructionSource = "INFERRED";
            this.reconciliationStatus = "RECONCILE_REQUIRED";
            this.reconciliationReason = reason;
            this.pnlComplete = false;
            this.commissionComplete = false;
        }

        private static Cycle issue(TradeCycleSourceFill fill, String reason) {
            return new Cycle(fill, reason);
        }

        private void apply(String role, BigDecimal quantity, TradeCycleSourceFill fill,
                           BigDecimal fee, BigDecimal pnl, String source) {
            if ("OPEN".equals(role)) {
                openingQuantity = openingQuantity.add(quantity);
                openingNotional = openingNotional.add(quantity.multiply(fill.getPrice()));
                remainingQuantity = remainingQuantity.add(quantity);
                if (openedAt == null || fill.getTradeTime().isBefore(openedAt)) {
                    openedAt = fill.getTradeTime();
                }
            } else {
                if (quantity.compareTo(remainingQuantity) > 0) {
                    throw new IllegalStateException("平仓数量超过持仓周期剩余数量");
                }
                closingQuantity = closingQuantity.add(quantity);
                closingNotional = closingNotional.add(quantity.multiply(fill.getPrice()));
                remainingQuantity = remainingQuantity.subtract(quantity);
                if (pnl == null) {
                    pnlComplete = false;
                } else {
                    realizedPnl = realizedPnl.add(pnl);
                }
                if (remainingQuantity.signum() == 0) {
                    closedAt = fill.getTradeTime();
                }
            }
            if (fee == null || fill.getCommissionAsset() == null || !fill.getCommissionAsset().matches("[A-Za-z0-9]{2,20}")) {
                commissionComplete = false;
            } else {
                String asset = fill.getCommissionAsset().toUpperCase(Locale.ROOT);
                fees.merge(asset, fee, BigDecimal::add);
            }
            if ("INFERRED".equals(source)) {
                reconstructionSource = "INFERRED";
                reconciliationStatus = "INFERRED";
            }
            fillCount++;
            status = remainingQuantity.signum() == 0 ? "CLOSED"
                : closingQuantity.signum() > 0 ? "PARTIAL" : "OPEN";
        }

        private TradeCycleRecord toRecord() {
            TradeCycleRecord row = new TradeCycleRecord();
            row.setId(id);
            row.setCycleKey(cycleKey);
            row.setCredentialId(credentialId);
            row.setSymbol(symbol);
            row.setPositionSide(positionSide);
            row.setStatus(reconciliationStatus.equals("RECONCILE_REQUIRED") ? "RECONCILE_REQUIRED" : status);
            row.setReconstructionSource(reconstructionSource);
            row.setReconciliationStatus(reconciliationStatus);
            row.setReconciliationReason(reconciliationReason);
            row.setOpenedAt(openedAt);
            row.setClosedAt(closedAt);
            row.setOpeningQuantity(openingQuantity);
            row.setOpeningNotional(openingNotional);
            row.setEntryPrice(openingQuantity.signum() == 0 ? null
                : openingNotional.divide(openingQuantity, MONEY_SCALE, RoundingMode.HALF_UP));
            row.setClosingQuantity(closingQuantity);
            row.setClosingNotional(closingNotional);
            row.setClosePrice(closingQuantity.signum() == 0 ? null
                : closingNotional.divide(closingQuantity, MONEY_SCALE, RoundingMode.HALF_UP));
            row.setRemainingQuantity(remainingQuantity);
            row.setRealizedPnl(pnlComplete ? realizedPnl : null);
            row.setCommissionDisplay(commissionDisplay());
            row.setCommissionByAssetJson(commissionJson());
            row.setFillCount(fillCount);
            return row;
        }

        private String commissionDisplay() {
            if (!commissionComplete) {
                return "待核实";
            }
            if (fees.isEmpty()) {
                return "0";
            }
            return fees.entrySet().stream().map(entry -> decimal(entry.getValue()) + " " + entry.getKey())
                .reduce((left, right) -> left + " + " + right).orElse("0");
        }

        private String commissionJson() {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, BigDecimal> fee : fees.entrySet()) {
                if (!first) {
                    json.append(',');
                }
                first = false;
                json.append('"').append(fee.getKey()).append("\":\"").append(decimal(fee.getValue())).append('"');
            }
            return json.append('}').toString();
        }

        private TradeCycleFillRecord allocation(TradeCycleSourceFill fill, int sequence, String role,
                                                BigDecimal quantity, BigDecimal fee, BigDecimal pnl) {
            TradeCycleFillRecord row = new TradeCycleFillRecord();
            row.setId(IdWorker.getId());
            row.setCycleId(id);
            row.setCredentialId(credentialId);
            row.setSymbol(symbol);
            row.setExchangeTradeId(fill.getExchangeTradeId());
            row.setExchangeOrderId(fill.getExchangeOrderId());
            row.setAllocationSeq(sequence);
            row.setFillRole(role);
            row.setPositionSide(positionSide);
            row.setAllocatedQuantity(quantity);
            row.setPrice(fill.getPrice());
            row.setCommission(fee);
            row.setCommissionAsset(fill.getCommissionAsset());
            row.setRealizedPnl(pnl);
            row.setTradeTime(fill.getTradeTime());
            return row;
        }

        private boolean active() {
            return remainingQuantity.signum() > 0;
        }

        private static String decimal(BigDecimal value) {
            return value.stripTrailingZeros().toPlainString();
        }
    }
}
