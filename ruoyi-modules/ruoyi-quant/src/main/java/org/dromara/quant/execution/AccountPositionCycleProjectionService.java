package org.dromara.quant.execution;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.quant.account.domain.vo.AccountPositionCycleFill;
import org.dromara.quant.exchange.ExchangeGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 将账户真实持仓数量投影为可查询的开平仓周期。
 *
 * <p>本服务与成交账本写入处于同一数据库事务。它不查询交易所、不读取策略实例周期键，
 * 只使用本地不可变成交、成交所属 Binance 订单的 positionSide，以及本轮已验证的账户持仓快照。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountPositionCycleProjectionService {
    private static final String READY = "READY";
    private static final String INITIALIZING = "INITIALIZING";
    private static final String INCOMPLETE = "INCOMPLETE";
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final AccountPositionCycleProjectionMapper mapper;

    /**
     * 对一个账户交易对执行初始化或增量投影。调用方必须处于成交账本同步事务内。
     */
    public void project(Long credentialId, String symbol, List<ExchangeGateway.PositionSnapshot> positions,
                        Instant positionReadStartedAt, Instant positionReadCompletedAt) {
        if (credentialId == null || credentialId <= 0 || symbol == null || symbol.isBlank()
            || positionReadStartedAt == null || positionReadCompletedAt == null
            || !positionReadStartedAt.isBefore(positionReadCompletedAt)) {
            throw new IllegalArgumentException("账户成交周期投影缺少有效账户、交易对或持仓快照边界");
        }
        String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
        List<AccountPositionCycleFill> allFills = mapper.selectAllFills(credentialId, normalizedSymbol);
        List<AccountPositionCycleFill> symbolFills = allFills == null ? List.of() : allFills;
        List<ExchangeGateway.PositionSnapshot> symbolPositions = positions == null ? List.of() : positions.stream()
            .filter(p -> p != null && normalizedSymbol.equalsIgnoreCase(p.symbol()))
            .toList();

        Set<String> scopes = new LinkedHashSet<>();
        for (AccountPositionCycleFill fill : symbolFills) {
            scopes.add(normalizePositionSide(fill.getPositionSide()));
        }
        for (ExchangeGateway.PositionSnapshot position : symbolPositions) {
            scopes.add(normalizePositionSide(position.side()));
        }
        if (scopes.isEmpty()) {
            return;
        }

        if (hasMixedPositionModes(scopes)) {
            for (String scope : scopes) {
                markIncomplete(credentialId, normalizedSymbol, scope, "成交历史包含 BOTH 与 LONG/SHORT 两种持仓模式，无法跨模式拼接周期");
            }
            return;
        }

        Set<Long> duplicateTradeIds = new HashSet<>();
        Set<Long> seenTradeIds = new HashSet<>();
        for (AccountPositionCycleFill fill : symbolFills) {
            if (fill.getExchangeTradeId() == null || !seenTradeIds.add(fill.getExchangeTradeId())) {
                if (fill.getExchangeTradeId() != null) {
                    duplicateTradeIds.add(fill.getExchangeTradeId());
                }
            }
        }

        Map<String, ExchangeGateway.PositionSnapshot> currentPositions = new HashMap<>();
        Set<String> duplicatePositionSides = new HashSet<>();
        for (ExchangeGateway.PositionSnapshot position : symbolPositions) {
            String side = normalizePositionSide(position.side());
            if (!validScope(side) || position.quantity() == null) {
                duplicatePositionSides.add(side);
                continue;
            }
            if (currentPositions.putIfAbsent(side, position) != null) {
                duplicatePositionSides.add(side);
            }
        }

        List<AccountPositionCycleFill> unprojected = mapper.selectUnprojectedFills(credentialId, normalizedSymbol);
        Map<String, List<AccountPositionCycleFill>> pendingByScope = groupByScope(unprojected);

        for (String scope : scopes) {
            if (!validScope(scope)) {
                markIncomplete(credentialId, normalizedSymbol, "UNKNOWN", "成交订单缺少可信的交易所 positionSide");
                continue;
            }
            if (duplicatePositionSides.contains(scope)) {
                markIncomplete(credentialId, normalizedSymbol, scope, "同一账户交易对返回重复持仓方向快照");
                continue;
            }
            List<AccountPositionCycleFill> scopeFills = symbolFills.stream()
                .filter(fill -> scope.equals(normalizePositionSide(fill.getPositionSide())))
                .toList();
            if (scopeFills.stream().anyMatch(fill -> duplicateTradeIds.contains(fill.getExchangeTradeId()))) {
                markIncomplete(credentialId, normalizedSymbol, scope, "同一交易所成交 ID 关联到多个订单事实");
                continue;
            }
            if (scopeFills.stream().anyMatch(fill -> !validFill(fill))) {
                markIncomplete(credentialId, normalizedSymbol, scope, "成交缺少数量、价格、方向或时间，无法安全投影");
                continue;
            }

            mapper.ensureState(credentialId, normalizedSymbol, scope);
            Map<String, Object> state = mapper.lockState(credentialId, normalizedSymbol, scope);
            if (state == null) {
                throw new IllegalStateException("账户持仓周期状态行无法锁定");
            }
            String status = string(state.get("status"));
            if (INCOMPLETE.equals(status)) {
                continue;
            }
            if (!READY.equals(status)) {
                initialize(credentialId, normalizedSymbol, scope, scopeFills, currentPositions.get(scope),
                    positionReadStartedAt, positionReadCompletedAt);
                continue;
            }

            List<AccountPositionCycleFill> pending = pendingByScope.getOrDefault(scope, List.of());
            if (pending.isEmpty()) {
                continue;
            }
            BigDecimal quantity = decimal(state.get("signed_quantity"));
            Long activeCycleId = nullableLong(state.get("active_cycle_id"));
            LocalDateTime lastTradeTime = localTime(state.get("last_trade_time"));
            Long lastTradeId = nullableLong(state.get("last_exchange_trade_id"));
            LocalDateTime projectedThrough = localTime(state.get("projected_through"));
            boolean incomplete = Boolean.TRUE.equals(state.get("incomplete")) || number(state.get("incomplete")) == 1;
            List<AccountPositionCycleFill> sorted = sorted(pending);
            try {
                preflight(scope, quantity, activeCycleId != null, sorted);
            } catch (IllegalStateException ex) {
                mapper.updateState(credentialId, normalizedSymbol, scope, quantity, activeCycleId, lastTradeTime, lastTradeId,
                    projectedThrough, INCOMPLETE, true, safeReason(ex.getMessage()));
                continue;
            }
            for (AccountPositionCycleFill fill : sorted) {
                if (projectedThrough != null && !fill.getTradeTime().isAfter(projectedThrough)
                    || lastTradeTime != null && compare(fill.getTradeTime(), fill.getExchangeTradeId(), lastTradeTime, lastTradeId) <= 0) {
                    markIncomplete(credentialId, normalizedSymbol, scope, "检测到晚到的历史成交，必须按完整历史重建该持仓周期");
                    activeCycleId = null;
                    status = INCOMPLETE;
                    break;
                }
                ProjectionResult next = applyFill(credentialId, normalizedSymbol, scope, quantity, activeCycleId, fill);
                quantity = next.quantity();
                activeCycleId = next.activeCycleId();
                lastTradeTime = fill.getTradeTime();
                lastTradeId = fill.getExchangeTradeId();
                projectedThrough = fill.getTradeTime();
            }
            if (READY.equals(status)) {
                if (mapper.updateState(credentialId, normalizedSymbol, scope, quantity, activeCycleId, lastTradeTime, lastTradeId,
                    projectedThrough, READY, incomplete, incomplete ? "初始成交历史之前存在未能确定开仓时间与成本的持仓" : null) != 1) {
                    throw new IllegalStateException("账户持仓周期状态更新失败");
                }
            }
        }
    }

    private void initialize(Long credentialId, String symbol, String scope, List<AccountPositionCycleFill> fills,
                             ExchangeGateway.PositionSnapshot snapshot, Instant readStarted, Instant readCompleted) {
        List<AccountPositionCycleFill> sorted = sorted(fills);
        Instant safeReadStart = readStarted.minusSeconds(1);
        Instant safeReadEnd = readCompleted.plusSeconds(1);
        boolean fillDuringSnapshotRead = sorted.stream().anyMatch(fill -> {
            Instant time = fill.getTradeTime().toInstant(ZoneOffset.UTC);
            return !time.isBefore(safeReadStart) && !time.isAfter(safeReadEnd);
        });
        if (fillDuringSnapshotRead) {
            mapper.updateState(credentialId, symbol, scope, ZERO, null, null, null, null,
                INITIALIZING, true, "持仓快照读取期间发生新成交，等待下一轮稳定快照后初始化");
            return;
        }

        BigDecimal exchangeQuantity = snapshotQuantity(scope, snapshot);
        BigDecimal historicalDelta = ZERO;
        List<AccountPositionCycleFill> beforeSnapshot = new ArrayList<>();
        List<AccountPositionCycleFill> afterSnapshot = new ArrayList<>();
        for (AccountPositionCycleFill fill : sorted) {
            Instant time = fill.getTradeTime().toInstant(ZoneOffset.UTC);
            if (time.isBefore(safeReadStart)) {
                beforeSnapshot.add(fill);
                historicalDelta = historicalDelta.add(signedDelta(scope, fill.getSide(), fill.getQuantity()));
            } else if (time.isAfter(safeReadEnd)) {
                afterSnapshot.add(fill);
            } else {
                mapper.updateState(credentialId, symbol, scope, ZERO, null, null, null, null,
                    INITIALIZING, true, "持仓快照读取期间发生新成交，等待下一轮稳定快照后初始化");
                return;
            }
        }

        BigDecimal quantity = exchangeQuantity.subtract(historicalDelta);
        if (!"BOTH".equals(scope) && quantity.signum() < 0) {
            mapper.updateState(credentialId, symbol, scope, ZERO, null, null, null, null,
                INCOMPLETE, true, "对冲模式历史净成交超过快照数量，成交历史或订单方向不完整");
            return;
        }

        Long activeCycleId = null;
        boolean incomplete = quantity.signum() != 0;
        try {
            preflight(scope, quantity, incomplete, sorted);
        } catch (IllegalStateException ex) {
            mapper.updateState(credentialId, symbol, scope, quantity, null, null, null, null,
                INCOMPLETE, true, safeReason(ex.getMessage()));
            return;
        }
        if (incomplete) {
            activeCycleId = createBaselineCycle(credentialId, symbol, scope, quantity);
        }
        LocalDateTime lastTradeTime = null;
        Long lastTradeId = null;
            for (AccountPositionCycleFill fill : beforeSnapshot) {
                ProjectionResult next = applyFill(credentialId, symbol, scope, quantity, activeCycleId, fill);
                quantity = next.quantity();
                activeCycleId = next.activeCycleId();
                lastTradeTime = fill.getTradeTime();
                lastTradeId = fill.getExchangeTradeId();
            }
            for (AccountPositionCycleFill fill : afterSnapshot) {
                ProjectionResult next = applyFill(credentialId, symbol, scope, quantity, activeCycleId, fill);
                quantity = next.quantity();
                activeCycleId = next.activeCycleId();
                lastTradeTime = fill.getTradeTime();
                lastTradeId = fill.getExchangeTradeId();
            }
            LocalDateTime projectedThrough = LocalDateTime.ofInstant(readCompleted, ZoneOffset.UTC);
            if (lastTradeTime != null && lastTradeTime.isAfter(projectedThrough)) {
                projectedThrough = lastTradeTime;
            }
            if (mapper.updateState(credentialId, symbol, scope, quantity, activeCycleId, lastTradeTime, lastTradeId,
                projectedThrough, READY, incomplete, incomplete ? "初始成交历史之前存在未能确定开仓时间与成本的持仓" : null) != 1) {
                throw new IllegalStateException("账户持仓周期初始化状态写入失败");
            }
        }
    }

    private ProjectionResult applyFill(Long credentialId, String symbol, String scope, BigDecimal currentQuantity,
                                       Long activeCycleId, AccountPositionCycleFill fill) {
        BigDecimal commission = fill.getCommission() == null ? ZERO : fill.getCommission();
        String commissionAsset = normalizeAsset(fill.getCommissionAsset());
        BigDecimal realizedPnl = fill.getRealizedPnl() == null ? ZERO : fill.getRealizedPnl();
        var transition = AccountPositionCycleAccounting.apply(scope, fill.getSide().toUpperCase(Locale.ROOT),
            currentQuantity, fill.getQuantity());
        BigDecimal closeQuantity = transition.closingQuantity();
        BigDecimal openQuantity = transition.openingQuantity();

        if (closeQuantity.signum() > 0) {
            if (activeCycleId == null) {
                throw new IllegalStateException("持仓数量非零但找不到活动周期，禁止猜测平仓归属");
            }
            BigDecimal closeFee = splitFee(commission, closeQuantity, fill.getQuantity());
            BigDecimal closeNotional = closeQuantity.multiply(fill.getPrice());
            BigDecimal remaining = currentQuantity.abs().subtract(closeQuantity);
            Map<String, Object> cycle = mapper.selectCycleState(activeCycleId);
            if (cycle == null) {
                throw new IllegalStateException("活动持仓周期记录不存在");
            }
            boolean baselineUnknown = number(cycle.get("baseline_unknown")) == 1;
            boolean closed = remaining.signum() == 0;
            String nextStatus = closed ? (baselineUnknown ? "CLOSED_INCOMPLETE" : "CLOSED")
                : (baselineUnknown ? "OPENING_UNKNOWN" : "OPEN");
            boolean crossed = openQuantity.signum() > 0;
            if (mapper.updateCloseCycle(activeCycleId, closeQuantity, closeNotional, remaining, realizedPnl,
                nextStatus, closed ? fill.getTradeTime() : null,
                crossed ? null : fill.getPostFillWalletBalance(),
                crossed ? null : fill.getPostFillBalanceAt()) != 1) {
                throw new IllegalStateException("持仓周期平仓投影更新失败");
            }
            insertAllocation(credentialId, symbol, fill, activeCycleId, "CLOSE", closeQuantity, closeFee, commissionAsset, realizedPnl);
            if (closed) {
                activeCycleId = null;
            }
        } else if (realizedPnl.signum() != 0) {
            throw new IllegalStateException("交易所成交产生已实现盈亏但无法分配到平仓数量");
        }

        if (openQuantity.signum() > 0) {
            BigDecimal openFee = commission.subtract(closeQuantity.signum() > 0
                ? splitFee(commission, closeQuantity, fill.getQuantity()) : ZERO);
            BigDecimal openNotional = openQuantity.multiply(fill.getPrice());
            if (activeCycleId == null && currentQuantity.signum() != 0) {
                throw new IllegalStateException("存在未归属的活动持仓周期，禁止自动创建重叠周期");
            }
            if (activeCycleId == null) {
                activeCycleId = createOpenCycle(credentialId, symbol, scope, transition.openingDirection(),
                    fill, openQuantity, openNotional, crossedBalanceUnavailable(closeQuantity, openQuantity)
                        ? null : fill.getPostFillWalletBalance(),
                    crossedBalanceUnavailable(closeQuantity, openQuantity) ? null : fill.getPostFillBalanceAt());
            } else {
                if (mapper.updateOpenCycle(activeCycleId, openQuantity, openNotional, fill.getLeverageSnapshot(),
                    fill.getPriceTickSizeSnapshot(), ZERO.equals(closeQuantity) ? fill.getPostFillWalletBalance() : null,
                    ZERO.equals(closeQuantity) ? fill.getPostFillBalanceAt() : null) != 1) {
                    throw new IllegalStateException("持仓周期加仓投影更新失败");
                }
            }
            insertAllocation(credentialId, symbol, fill, activeCycleId, "OPEN", openQuantity, openFee, commissionAsset, ZERO);
        }
        return new ProjectionResult(transition.nextQuantity(), activeCycleId);
    }

    private Long createBaselineCycle(Long credentialId, String symbol, String scope, BigDecimal signedQuantity) {
        String direction = "BOTH".equals(scope)
            ? (signedQuantity.signum() >= 0 ? "LONG" : "SHORT")
            : scope;
        BigDecimal quantity = signedQuantity.abs();
        long id = IdWorker.getId();
        if (mapper.insertCycle(id, UUID.randomUUID().toString(), credentialId, symbol, scope, direction,
            "OPENING_UNKNOWN", true, quantity, quantity, 0, null, null, null, null, null) != 1) {
            throw new IllegalStateException("初始未知持仓周期创建失败");
        }
        return id;
    }

    private Long createOpenCycle(Long credentialId, String symbol, String scope, String direction,
                                 AccountPositionCycleFill fill, BigDecimal quantity, BigDecimal notional,
                                 BigDecimal balance, LocalDateTime balanceAt) {
        long id = IdWorker.getId();
        if (mapper.insertCycle(id, UUID.randomUUID().toString(), credentialId, symbol, scope, direction,
            "OPEN", false, quantity, quantity, 1, notional, fill.getTradeTime(), fill.getLeverageSnapshot(),
            fill.getPriceTickSizeSnapshot(), balance, balanceAt) != 1) {
            throw new IllegalStateException("新持仓周期创建失败");
        }
        return id;
    }

    private void insertAllocation(Long credentialId, String symbol, AccountPositionCycleFill fill, Long cycleId,
                                  String role, BigDecimal quantity, BigDecimal fee, String feeAsset, BigDecimal pnl) {
        if (mapper.insertAllocation(IdWorker.getId(), credentialId, symbol, fill.getExchangeTradeId(),
            fill.getExchangeOrderId(), cycleId, role, quantity, fill.getPrice(), fee, feeAsset, pnl, fill.getTradeTime()) != 1) {
            throw new IllegalStateException("成交与持仓周期的分配关系写入失败");
        }
    }

    private void markIncomplete(Long credentialId, String symbol, String scope, String reason) {
        mapper.ensureState(credentialId, symbol, scope);
        Map<String, Object> state = mapper.lockState(credentialId, symbol, scope);
        if (state == null) {
            throw new IllegalStateException("无法锁定待隔离的账户持仓周期状态");
        }
        mapper.updateState(credentialId, symbol, scope, decimal(state.get("signed_quantity")),
            nullableLong(state.get("active_cycle_id")), localTime(state.get("last_trade_time")),
            nullableLong(state.get("last_exchange_trade_id")), localTime(state.get("projected_through")),
            INCOMPLETE, true, safeReason(reason));
    }

    private static Map<String, List<AccountPositionCycleFill>> groupByScope(List<AccountPositionCycleFill> fills) {
        Map<String, List<AccountPositionCycleFill>> grouped = new HashMap<>();
        if (fills != null) {
            for (AccountPositionCycleFill fill : fills) {
                grouped.computeIfAbsent(normalizePositionSide(fill.getPositionSide()), ignored -> new ArrayList<>()).add(fill);
            }
        }
        return grouped;
    }

    private static List<AccountPositionCycleFill> sorted(List<AccountPositionCycleFill> fills) {
        return fills.stream().sorted(Comparator.comparing(AccountPositionCycleFill::getTradeTime)
            .thenComparing(AccountPositionCycleFill::getExchangeTradeId)).toList();
    }

    private static boolean hasMixedPositionModes(Set<String> scopes) {
        return scopes.contains("BOTH") && (scopes.contains("LONG") || scopes.contains("SHORT"));
    }

    private static boolean validScope(String side) {
        return "BOTH".equals(side) || "LONG".equals(side) || "SHORT".equals(side);
    }

    private static String normalizePositionSide(String side) {
        return side == null || side.isBlank() ? "UNKNOWN" : side.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean validFill(AccountPositionCycleFill fill) {
        return fill.getExchangeTradeId() != null && fill.getExchangeTradeId() > 0
            && fill.getExchangeOrderId() != null && fill.getExchangeOrderId() > 0
            && fill.getQuantity() != null && fill.getQuantity().signum() > 0
            && fill.getPrice() != null && fill.getPrice().signum() > 0
            && fill.getCommission() != null && fill.getCommission().signum() >= 0
            && fill.getRealizedPnl() != null && fill.getTradeTime() != null
            && fill.getSide() != null && ("BUY".equalsIgnoreCase(fill.getSide()) || "SELL".equalsIgnoreCase(fill.getSide()));
    }

    private static BigDecimal snapshotQuantity(String scope, ExchangeGateway.PositionSnapshot snapshot) {
        if (snapshot == null) {
            return ZERO;
        }
        if (snapshot.quantity() == null || !scope.equalsIgnoreCase(snapshot.side())) {
            throw new IllegalStateException("账户持仓快照与周期方向不匹配");
        }
        return "BOTH".equals(scope) ? snapshot.quantity() : snapshot.quantity().abs();
    }

    private static BigDecimal signedDelta(String scope, String side, BigDecimal quantity) {
        boolean increases = "BOTH".equals(scope)
            ? "BUY".equalsIgnoreCase(side)
            : ("LONG".equals(scope) && "BUY".equalsIgnoreCase(side))
                || ("SHORT".equals(scope) && "SELL".equalsIgnoreCase(side));
        return increases ? quantity : quantity.negate();
    }

    private static void preflight(String scope, BigDecimal quantity, boolean activeCycle, List<AccountPositionCycleFill> fills) {
        for (AccountPositionCycleFill fill : fills) {
            if (quantity.signum() != 0 && !activeCycle) {
                throw new IllegalStateException("非零账户仓位缺少活动周期记录");
            }
            var transition = AccountPositionCycleAccounting.apply(scope, fill.getSide().toUpperCase(Locale.ROOT),
                quantity, fill.getQuantity());
            if (transition.closingQuantity().signum() > 0 && !activeCycle) {
                throw new IllegalStateException("成交需要平仓但没有可证明的活动周期");
            }
            if (transition.closingQuantity().signum() == 0 && fill.getRealizedPnl().signum() != 0) {
                throw new IllegalStateException("成交已实现盈亏无法对应到平仓数量");
            }
            quantity = transition.nextQuantity();
            activeCycle = quantity.signum() != 0;
        }
    }

    private static BigDecimal splitFee(BigDecimal fee, BigDecimal part, BigDecimal total) {
        return fee.multiply(part).divide(total, 16, RoundingMode.HALF_UP);
    }

    private static boolean crossedBalanceUnavailable(BigDecimal closeQuantity, BigDecimal openQuantity) {
        return closeQuantity.signum() > 0 && openQuantity.signum() > 0;
    }

    private static int compare(LocalDateTime firstTime, Long firstId, LocalDateTime secondTime, Long secondId) {
        int time = firstTime.compareTo(secondTime);
        return time != 0 ? time : Long.compare(firstId == null ? Long.MIN_VALUE : firstId, secondId == null ? Long.MIN_VALUE : secondId);
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return ZERO;
    }

    private static int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private static Long nullableLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static LocalDateTime localTime(Object value) {
        if (value instanceof LocalDateTime time) {
            return time;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }

    private static String string(Object value) {
        return value == null ? null : value.toString();
    }

    private static String normalizeAsset(String asset) {
        return asset == null || asset.isBlank() ? "UNKNOWN" : asset.trim().toUpperCase(Locale.ROOT);
    }

    private static String safeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "账户成交周期无法可靠重建";
        }
        String value = reason.replaceAll("[\\r\\n\\t]", " ").trim();
        return value.length() <= 500 ? value : value.substring(0, 500);
    }

    private record ProjectionResult(BigDecimal quantity, Long activeCycleId) {
    }
}
