package org.dromara.quant.protection;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.exchange.QuantityNormalizer;
import org.dromara.quant.execution.LiveOrderDispatcher;
import org.dromara.quant.execution.TradeExecutionApplicationService;
import org.dromara.quant.position.domain.QuantPosition;
import org.dromara.quant.protection.domain.QuantProtectionOrder;
import org.dromara.quant.protection.mapper.QuantProtectionOrderMapper;
import org.dromara.quant.strategy.ProtectionSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 持仓保护单生命周期入口。实际保护单统一由该服务负责创建、更新、撤销和恢复。
 * 策略只给出 ProtectionSignal，不直接访问交易所。
 */
@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ProtectionOrderService {
    private static final Logger log = LoggerFactory.getLogger(ProtectionOrderService.class);
    /**
     * 由策略插件动态维护的保护单类型集合。
     */
    private static final List<String> ACTIVE_STATUSES = List.of("NEW", "WORKING", "ACCEPTED", "PAPER_ACCEPTED", "CANCEL_FAILED", "PREPARED", "DISPATCHING", "PENDING", "UNKNOWN", "REJECTED");
    private static final Set<String> STRATEGY_PROTECTION_TYPES = Set.of("LOCAL_STOP", "SYMBOL_STOP", "FIRST_TP", "STEP_TP");
    private static final List<String> STOP_PROTECTION_TYPES = List.of("SYMBOL_STOP", "LOCAL_STOP", "HARD_STOP", "STEP_TP");
    private final ExchangeGateway gateway;
    /**
     * 当前领域 Mapper。
     */
    private final QuantProtectionOrderMapper mapper;
    /**
     * 交易执行应用服务。
     */
    private final TradeExecutionApplicationService executionApplicationService;
    private final ProtectionTargetCoordinator targets;
    private final ThreadLocal<ProtectionTargetCoordinator.Lease> targetLease = new ThreadLocal<>();
    @Autowired
    private org.dromara.quant.decision.TradeDecisionLogService decisionLogs;
    private volatile LiveOrderDispatcher liveOrderDispatcher;
    @Autowired
    private ProtectionOrderJournalService protectionJournal;
    /**
     * 兼容仅提供保护目标协调器以外依赖的测试与调用方。
     */
    public ProtectionOrderService(ExchangeGateway gateway, QuantProtectionOrderMapper mapper, TradeExecutionApplicationService executionApplicationService) {
        this(gateway, mapper, executionApplicationService, null);
    }

    /**
     * 判断当前持仓是否具备策略/保护计算所需的有效字段。
     */
    private static boolean validPosition(QuantPosition position) {
        return position != null && position.getId() != null && position.getEntryPrice() != null && position.getEntryPrice().signum() > 0 && position.getQuantity() != null && position.getQuantity().signum() > 0;
    }

    /**
     * 注入账户交易对串行派发器，供交易所保护单请求使用。
     */
    @Autowired
    public void setLiveOrderDispatcher(LiveOrderDispatcher dispatcher) {
        this.liveOrderDispatcher = java.util.Objects.requireNonNull(dispatcher);
    }

    private LiveOrderDispatcher requireLiveDispatcher() {
        var value = liveOrderDispatcher;
        if (value == null) {
            throw new IllegalStateException("交易所保护单调度器不可用，拒绝直连交易所");
        }
        return value;
    }

    /**
     * 策略订阅启动前检查账户交易对门禁。只恢复持久化身份与交易所精确事实一致的保护创建/撤销，
     * 或由交易所实际保证金模式和杠杆共同证明已生效的配置操作；其他未知状态继续阻止策略启动。
     */
    public boolean recoverDispatchBeforeStrategyStart(Long credentialId, String symbol) {
        LiveOrderDispatcher.ActiveDispatch active = requireLiveDispatcher().activeDispatch(credentialId, symbol);
        if (active == null) {
            return true;
        }
        if (active.clientOrderId() == null || active.dispatchKey() == null) {
            log.warn("策略启动前发现不可自动恢复的派发占用，保持启动阻断 accountId={} symbol={} operation={} dispatchStatus={}", credentialId, symbol,
                active.operationType(), active.dispatchStatus());
            return false;
        }
        if ("PROTECT_CANCEL".equals(active.operationType())) {
            return recoverProtectionCancel(active);
        }
        if ("PROTECT_CREATE".equals(active.operationType())) {
            return recoverProtectionCreate(active);
        }
        if ("CONFIG_SETTINGS".equals(active.operationType())) {
            // Only CONFIG_APPLIED proves both configuration calls returned successfully and
            // that confirmation was committed before guard release. DISPATCHING/UNKNOWN may
            // still have a remote side effect in flight, so a matching snapshot is insufficient.
            if (!"CONFIG_APPLIED".equals(active.dispatchStatus())) {
                log.warn("配置派发未持久化确认完成，无法证明远端请求已结束，保持策略启动阻断 accountId={} symbol={} dispatchStatus={}",
                    credentialId, symbol, active.dispatchStatus());
                return false;
            }
            return recoverConfigurationSettings(active);
        }
        log.warn("策略启动前发现不可自动恢复的派发占用，保持启动阻断 accountId={} symbol={} operation={} dispatchStatus={}", credentialId, symbol,
            active.operationType(), active.dispatchStatus());
        return false;
    }

    /**
     * CONFIG_SETTINGS has no exchange order ID. Release its guard only when an
     * independent Binance read proves both desired settings match the durable intent.
     * Caller must establish CONFIG_APPLIED status first. DISPATCHING/UNKNOWN may still
     * have a remote side effect in flight and are intentionally not auto-recovered.
     * A mismatch or unavailable query remains blocked for explicit manual review.
     */
    private boolean recoverConfigurationSettings(LiveOrderDispatcher.ActiveDispatch active) {
        if (active == null || active.credentialId() == null || active.symbol() == null || active.dispatchKey() == null
            || active.clientOrderId() == null) {
            return false;
        }
        String[] identity = active.clientOrderId().split(":", -1);
        if (identity.length != 3 || !"SETTINGS".equals(identity[0])
            || !Set.of("ISOLATED", "CROSSED").contains(identity[1])) {
            log.warn("配置派发票据缺少可核实的目标配置身份，保持启动阻断 accountId={} symbol={} dispatchKey={}",
                active.credentialId(), active.symbol(), active.dispatchKey());
            return false;
        }
        final int expectedLeverage;
        try {
            expectedLeverage = Integer.parseInt(identity[2]);
        } catch (NumberFormatException ex) {
            log.warn("配置派发票据杠杆字段无效，保持启动阻断 accountId={} symbol={} dispatchKey={}",
                active.credentialId(), active.symbol(), active.dispatchKey());
            return false;
        }
        if (expectedLeverage < 1 || expectedLeverage > 125) {
            log.warn("配置派发票据杠杆超出有效范围，保持启动阻断 accountId={} symbol={} dispatchKey={}",
                active.credentialId(), active.symbol(), active.dispatchKey());
            return false;
        }
        try {
            var credentialEntity = executionApplicationService.requireCredential(active.credentialId());
            ExchangeGateway.Credential credential = executionApplicationService.toCredential(credentialEntity);
            org.dromara.quant.exchange.ExchangeAccountPort.SymbolTradingSettings actual = gateway.querySymbolTradingSettings(credential, active.symbol());
            if (actual == null || actual.symbol() == null || !active.symbol().equalsIgnoreCase(actual.symbol())
                || !identity[1].equalsIgnoreCase(actual.marginMode()) || expectedLeverage != actual.leverage()) {
                log.warn("配置派发未知状态与交易所实际配置不一致，继续保持启动阻断 accountId={} symbol={} desiredMarginMode={} desiredLeverage={} actualMarginMode={} actualLeverage={}",
                    active.credentialId(), active.symbol(), identity[1], expectedLeverage,
                    actual == null ? null : actual.marginMode(), actual == null ? null : actual.leverage());
                return false;
            }
            boolean recovered = requireLiveDispatcher().recoveredConfigurationFromExchange(active.credentialId(), active.symbol(), active.dispatchKey(),
                active.clientOrderId(), identity[1], expectedLeverage);
            if (recovered) {
                log.info("配置派发未知状态已通过交易所实际配置核实并恢复 accountId={} symbol={} marginMode={} leverage={}",
                    active.credentialId(), active.symbol(), identity[1], expectedLeverage);
            } else {
                log.warn("交易所配置已匹配，但派发票据或持久化配置意图不匹配，继续保持启动阻断 accountId={} symbol={} dispatchKey={}",
                    active.credentialId(), active.symbol(), active.dispatchKey());
            }
            return recovered;
        } catch (RuntimeException ex) {
            log.warn("配置派发未知状态核实失败，继续保持启动阻断 accountId={} symbol={} dispatchKey={} errorType={}",
                active.credentialId(), active.symbol(), active.dispatchKey(), ex.getClass().getSimpleName());
            return false;
        }
    }

    private boolean recoverProtectionCreate(LiveOrderDispatcher.ActiveDispatch active) {
        QuantProtectionOrder local = mapper.selectOne(new LambdaQueryWrapper<QuantProtectionOrder>()
            .eq(QuantProtectionOrder::getCredentialId, active.credentialId())
            .eq(QuantProtectionOrder::getSymbol, active.symbol())
            .eq(QuantProtectionOrder::getClientOrderId, active.clientOrderId())
            .last("LIMIT 1"));
        if (local == null || !active.clientOrderId().equals(local.getClientOrderId())) {
            log.warn("策略启动前保护创建票据缺少精确本地意图，保持启动阻断 accountId={} symbol={} clientAlgoId={}", active.credentialId(), active.symbol(), active.clientOrderId());
            return false;
        }
        try {
            confirmOnExchange(active.credentialId(), local, false);
            boolean recovered = requireLiveDispatcher().activeDispatch(active.credentialId(), active.symbol()) == null;
            if (!recovered) {
                log.warn("策略启动前保护创建票据仍未恢复，保持启动阻断 accountId={} symbol={} clientAlgoId={}", active.credentialId(), active.symbol(), active.clientOrderId());
            }
            return recovered;
        } catch (RuntimeException ex) {
            log.warn("策略启动前保护创建事实核实失败，保持启动阻断 accountId={} symbol={} clientAlgoId={} errorType={}", active.credentialId(), active.symbol(),
                active.clientOrderId(), ex.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * 私有流事件或连接重建后，异步核实账户当前未决保护撤单；调用方应在工作线程执行。
     */
    public int recoverUnresolvedProtectionCancels(Long credentialId, String symbol) {
        int recovered = 0;
        for (LiveOrderDispatcher.ActiveDispatch active : requireLiveDispatcher().activeProtectionCancels(credentialId, symbol)) {
            if (recoverProtectionCancel(active)) {
                recovered++;
            }
        }
        return recovered;
    }

    /**
     * Private-stream and dispatch-block recovery for the single active account/symbol slot.
     * Only exact protection creation and cancellation identities are eligible for automatic recovery.
     */
    public int recoverUnresolvedProtectionDispatches(Long credentialId, String symbol) {
        if (symbol == null || symbol.isBlank()) {
            int recovered = 0;
            for (LiveOrderDispatcher.ActiveDispatch active : requireLiveDispatcher().activeProtectionDispatches(credentialId, null)) {
                if (recoverProtectionDispatch(active)) {
                    recovered++;
                }
            }
            return recovered;
        }
        LiveOrderDispatcher.ActiveDispatch active = requireLiveDispatcher().activeDispatch(credentialId, symbol);
        return recoverProtectionDispatch(active) ? 1 : 0;
    }

    /**
     * 人工触发单张保护派发票据的事实核验。先匹配页面提交的完整票据身份，
     * 再进入与自动恢复完全相同的核验路径，避免误处理后来产生的新票据。
     */
    public boolean recheckProtectionDispatch(Long credentialId, String symbol, String dispatchKey, String clientOrderId) {
        if (credentialId == null || credentialId <= 0 || symbol == null || symbol.isBlank()
            || dispatchKey == null || dispatchKey.isBlank() || clientOrderId == null || clientOrderId.isBlank()) {
            throw new IllegalArgumentException("保护派发核查缺少完整票据身份");
        }
        LiveOrderDispatcher.ActiveDispatch active = requireLiveDispatcher().activeDispatch(credentialId, symbol);
        if (active == null || !dispatchKey.equals(active.dispatchKey()) || !clientOrderId.equals(active.clientOrderId())
            || !Set.of("PROTECT_CREATE", "PROTECT_CANCEL").contains(active.operationType())) {
            return false;
        }
        return recoverProtectionDispatch(active);
    }

    /** 判断账户交易对是否仍有必须通过精确交易所查询恢复的保护派发票据。 */
    public boolean hasUnresolvedProtectionDispatches(Long credentialId, String symbol) {
        return !requireLiveDispatcher().activeProtectionDispatches(credentialId, symbol).isEmpty();
    }

    private boolean recoverProtectionDispatch(LiveOrderDispatcher.ActiveDispatch active) {
        if (active == null) {
            return false;
        }
        return switch (active.operationType() == null ? "" : active.operationType()) {
            case "PROTECT_CREATE" -> recoverProtectionCreate(active);
            case "PROTECT_CANCEL" -> recoverProtectionCancel(active);
            default -> false;
        };
    }

    private boolean recoverProtectionCancel(LiveOrderDispatcher.ActiveDispatch active) {
        if (active == null || active.credentialId() == null || active.symbol() == null || active.dispatchKey() == null || active.clientOrderId() == null) {
            return false;
        }
        QuantProtectionOrder local = mapper.selectOne(new LambdaQueryWrapper<QuantProtectionOrder>()
            .eq(QuantProtectionOrder::getCredentialId, active.credentialId())
            .eq(QuantProtectionOrder::getSymbol, active.symbol())
            .eq(QuantProtectionOrder::getClientOrderId, active.clientOrderId())
            .last("LIMIT 1"));
        if (local == null) {
            log.warn("未决保护撤单没有精确本地账本记录，继续占用派发门禁 accountId={} symbol={} dispatchKey={} clientAlgoId={}", active.credentialId(), active.symbol(),
                active.dispatchKey(), active.clientOrderId());
            return false;
        }
        String stage = "LOAD_CREDENTIAL";
        try {
            var credentialEntity = executionApplicationService.requireCredential(active.credentialId());
            ExchangeGateway.Credential credential = executionApplicationService.toCredential(credentialEntity);
            if (!"PROTECT_CANCEL".equals(active.operationType()) || !local.getClientOrderId().equals(active.clientOrderId())) {
                log.warn("未决保护撤单身份校验未通过，继续占用派发门禁 accountId={} symbol={} dispatchKey={} clientAlgoId={} operationType={} localClientAlgoId={}",
                    active.credentialId(), active.symbol(), active.dispatchKey(), active.clientOrderId(), active.operationType(), local.getClientOrderId());
                return false;
            }
            stage = "QUERY_EXCHANGE_PROTECTION";
            ExchangeGateway.ProtectionOrderSnapshot current = gateway.queryProtectionOrder(credential, local.getExchangeOrderId(),
                local.getClientOrderId(), local.getSymbol(), local.getCreatedAtEpochMillis());
            if (isExactUnfilledWorkingProtection(local, current)) {
                // A precise active-order fact proves that the prior cancel did not reach a terminal state.
                // Repeating cancellation with the same exchange/client identity is idempotent; never create a new ticket.
                stage = "RETRY_EXCHANGE_CANCEL";
                try {
                    gateway.cancelProtectionOrder(new ExchangeGateway.CancelProtectionOrderRequest(credential, local.getExchangeOrderId(), local.getClientOrderId(), true));
                } catch (RuntimeException retryFailure) {
                    log.debug("恢复期间重复保护撤单没有明确回执，随后按同一客户端标识查询终态 accountId={} symbol={} clientAlgoId={} errorType={} errorMessage={}",
                        active.credentialId(), active.symbol(), active.clientOrderId(), retryFailure.getClass().getSimpleName(), retryFailure.getMessage());
                }
            }
            stage = "VERIFY_AND_PERSIST_TERMINAL";
            Long terminalAlgoId = "FINISHED".equalsIgnoreCase(current == null ? null : current.status())
                ? persistVerifiedTriggeredProtection(active.credentialId(), local, credential)
                : persistVerifiedCancelledProtection(active.credentialId(), local, credential, Set.of("CANCELED", "EXPIRED", "REJECTED"), true);
            if (terminalAlgoId == null) {
                return false;
            }
            stage = "RELEASE_DISPATCH_GATE";
            boolean recovered = requireLiveDispatcher().recoveredFromExchange(active.credentialId(), active.symbol(), active.dispatchKey(), active.clientOrderId(), terminalAlgoId);
            if (recovered) {
                String recoveryType = "FINISHED".equalsIgnoreCase(current == null ? null : current.status()) ? "触发成交" : "撤单";
                log.info("未决保护{}已按交易所终态恢复并释放派发门禁 accountId={} symbol={} dispatchKey={} clientAlgoId={} algoId={}", recoveryType,
                    active.credentialId(), active.symbol(), active.dispatchKey(), active.clientOrderId(), terminalAlgoId);
            } else {
                log.warn("未决保护终态已写入，但派发门禁未能释放 accountId={} symbol={} dispatchKey={} clientAlgoId={} algoId={}",
                    active.credentialId(), active.symbol(), active.dispatchKey(), active.clientOrderId(), terminalAlgoId);
            }
            return recovered;
        } catch (RuntimeException ex) {
            String errorMessage = Objects.toString(ex.getMessage(), "");
            if (errorMessage.length() > 1200) {
                errorMessage = errorMessage.substring(0, 1200);
            }
            log.warn("未决保护撤单核实失败，继续占用派发门禁 accountId={} symbol={} dispatchKey={} clientAlgoId={} stage={} errorType={} errorMessage={}",
                active.credentialId(), active.symbol(), active.dispatchKey(), active.clientOrderId(), stage, ex.getClass().getSimpleName(), errorMessage, ex);
            return false;
        }
    }

    /** 只有完整身份匹配、未成交且仍活动的精确委托，才允许重试原撤单。 */
    private boolean isExactUnfilledWorkingProtection(QuantProtectionOrder local, ExchangeGateway.ProtectionOrderSnapshot actual) {
        return actual != null && Set.of("NEW", "WORKING", "ACCEPTED").contains(actual.status())
            && actual.executedQuantity() != null && actual.executedQuantity().signum() == 0
            && local.getClientOrderId() != null && local.getClientOrderId().equals(actual.clientAlgoId())
            && actual.algoId() != null && actual.algoId() > 0
            && (local.getExchangeOrderId() == null || local.getExchangeOrderId().equals(actual.algoId()))
            && local.getSymbol() != null && local.getSymbol().equalsIgnoreCase(actual.symbol())
            && local.getPositionSide() != null
            && sameTarget(local, actual.type(), actual.quantity(), actual.triggerPrice())
            && ("SHORT".equalsIgnoreCase(local.getPositionSide()) ? "BUY" : "SELL").equalsIgnoreCase(actual.side())
            && ("BOTH".equalsIgnoreCase(actual.positionSide()) || local.getPositionSide().equalsIgnoreCase(actual.positionSide()));
    }

    /**
     * 在目标租约保护下执行单个保护类型的同步操作。
     */
    private <T> T coordinated(Long credentialId, QuantPosition position, String type, java.util.function.Supplier<T> action) {
        if (targets == null) {
            synchronized (this) {
                return action.get();
            }
        }
        try (var lease = targets.acquire(credentialId, position, type)) {
            var previous = targetLease.get();
            targetLease.set(lease);
            try {
                return action.get();
            } finally {
                if (previous == null) {
                    targetLease.remove();
                } else {
                    targetLease.set(previous);
                }
            }
        }
    }

    private void checkTargetLease() {
        if (targetLease.get() != null) {
            targetLease.get().check();
        }
    }

    /**
     * 返回该持仓周期在交易所已确认的最高保护阶梯。
     */
    public int confirmedStep(Long credentialId, QuantPosition position) {
        return targets == null ? 0 : targets.confirmedStep(credentialId, position);
    }

    /**
     * 固定硬止损。marginLossRatio 为保证金回报率亏损比例，例如 0.10 表示 -10% ROI。
     */
    public QuantProtectionOrder ensureMarginHardStop(Long credentialId, QuantPosition position, BigDecimal marginLossRatio, boolean live) {
        if (!validPosition(position)) {
            return null;
        }
        if (live && !Integer.valueOf(2).equals(position.getProtectionPolicyVersion())) {
            BigDecimal quantity = normalizeQuantity(credentialId, position.getSymbol(), position.getQuantity(), true);
            BigDecimal price = QuantityNormalizer.protectionPrice(hardStopPrice(position, marginLossRatio), rule(credentialId, position.getSymbol()), position.getPositionSide(), "STOP_MARKET");
            // 已冻结的硬止损不能因均价变化而被更弱的替代单覆盖。
            for (var old : scopedStops(credentialId, position)) {
                if ("HARD_STOP".equals(old.getProtectionType()) && "WORKING".equals(old.getStatus()) && old.getStopPrice() != null) {
                    price = "SHORT".equalsIgnoreCase(position.getPositionSide()) ? price.min(old.getStopPrice()) : price.max(old.getStopPrice());
                }
            }
            var covering = confirmedCover(credentialId, position, "HARD_STOP", quantity, price);
            if (covering != null) {
                pruneCoveredStops(credentialId, position);
                return covering;
            }
        }
        return coordinated(credentialId, position, "HARD_STOP", () -> ensureMarginHardStopTarget(credentialId, position, marginLossRatio, live));
    }

    /**
     * 按保证金损失比例计算并确认硬止损目标。
     */
    private QuantProtectionOrder ensureMarginHardStopTarget(Long credentialId, QuantPosition position, BigDecimal marginLossRatio, boolean live) {
        if (!validPosition(position)) {
            return null;
        }
        BigDecimal quantity = normalizeQuantity(credentialId, position.getSymbol(), position.getQuantity(), live);
        BigDecimal stopPrice = normalizeProtectionPrice(credentialId, position, hardStopPrice(position, marginLossRatio), "STOP_MARKET", live);
        QuantProtectionOrder existing = findActive(credentialId, position, "HARD_STOP");
        boolean working = existing != null && (!live || confirmOnExchange(credentialId, existing, true, position.getQuantity()));
        // 固定保护价在该持仓生命周期内冻结，保证金波动不能驱动撤单重挂。
        if (working && existing.getStopPrice() != null) {
            stopPrice = existing.getStopPrice();
        }
        if (working && targetLease.get() != null) {
            targetLease.get().confirmed(existing, 0);
            cancelSuperseded(credentialId, position.getId(), position.getProtectionCycleKey(), "HARD_STOP", existing.getId(), live);
        }
        if (working && sameTarget(existing, "STOP_MARKET", quantity, stopPrice)) {
            cancelSuperseded(credentialId, position.getId(), position.getProtectionCycleKey(), "HARD_STOP", existing.getId(), live);
            return existing;
        }
        // 新保护确认后才撤旧，下单或确认失败时保留旧保护。
        logReplacement(position, "HARD_STOP", existing, quantity, stopPrice, working);
        QuantProtectionOrder replacement = placeProtection(credentialId, position, "HARD_STOP", "STOP_MARKET", quantity, stopPrice, live);
        if (existing != null && !cancelOne(credentialId, existing, live)) {
            throw new IllegalStateException("新硬止损已生效，旧硬止损撤销失败，等待对账");
        }
        cancelSuperseded(credentialId, position.getId(), position.getProtectionCycleKey(), "HARD_STOP", replacement.getId(), live);
        return replacement;
    }

    /**
     * 创建或替换固定硬止损条件单。
     */
    public QuantProtectionOrder placeMarginHardStop(Long credentialId, QuantPosition position, BigDecimal marginLossRatio, boolean live) {
        if (!validPosition(position)) {
            return null;
        }
        return placeProtection(credentialId, position, "HARD_STOP", "STOP_MARKET", normalizeQuantity(credentialId, position.getSymbol(), position.getQuantity(), live),
            normalizeProtectionPrice(credentialId, position, hardStopPrice(position, marginLossRatio), "STOP_MARKET", live), live);
    }

    /**
     * 将保证金损失率换算为符合方向的止损触发价格。
     */
    private BigDecimal hardStopPrice(QuantPosition position, BigDecimal marginLossRatio) {
        if (marginLossRatio == null || marginLossRatio.signum() <= 0 || marginLossRatio.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("硬止损回报率必须 > 0 且 < 1");
        }
        if (Integer.valueOf(2).equals(position.getProtectionPolicyVersion()) && position.getProtectionStopLossPrice() != null
            && position.getProtectionStopLossPrice().signum() > 0) {
            return position.getProtectionStopLossPrice();
        }
        BigDecimal price = org.dromara.quant.strategy.KlineSlopeRateStrategy.priceForReturn(position.getEntryPrice(), position.getPositionSide(), marginLossRatio.negate(),
            position.getLeverage() == null ? 1 : position.getLeverage());
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("计算出的硬止损价格无效");
        }
        return price;
    }

    /**
     * 同步策略保护单：首次止盈目标全部成交确认后才设置 STEP_TP；未完成首平时安全清理旧版本提前建立的阶梯单。
     * 只有目标发生变化才撤旧挂新，高频实时价不会持续打 REST。
     */
    public void syncStrategyProtections(Long credentialId, QuantPosition position, BigDecimal initialPositionQuantity, List<ProtectionSignal> desiredSignals, boolean live) {
        syncStrategyProtections(credentialId, position, initialPositionQuantity, desiredSignals, null, live);
    }

    /**
     * 根据策略目标同步保护单；新单未确认前保留旧保护单。
     */
    public void syncStrategyProtections(Long credentialId, QuantPosition position, BigDecimal initialPositionQuantity, List<ProtectionSignal> desiredSignals,
                                        BigDecimal firstRemaining, boolean live) {
        syncStrategyProtections(credentialId, position, initialPositionQuantity, desiredSignals, firstRemaining, null, live);
    }

    /** V2 首平创建前核对新鲜交易所累计成交，并扣除本地已确认成交，防止旧工作快照重建已完成目标。 */
    public void syncStrategyProtections(Long credentialId, QuantPosition position, BigDecimal initialPositionQuantity, List<ProtectionSignal> desiredSignals,
                                        BigDecimal firstRemaining, BigDecimal firstLocalExecuted, boolean live) {
        if (!validPosition(position)) {
            return;
        }
        List<ProtectionSignal> desired = desiredSignals == null ? List.of() : desiredSignals;
        boolean firstProfitPending = desired.stream().anyMatch(signal -> signal != null && "FIRST_TP".equals(signal.type()))
            && desired.stream().noneMatch(signal -> signal != null && "STEP_TP".equals(signal.type()));
        if (firstProfitPending && live) {
            List<QuantProtectionOrder> previousSteps = mapper.selectList(new LambdaQueryWrapper<QuantProtectionOrder>()
                .eq(QuantProtectionOrder::getCredentialId, credentialId)
                .eq(QuantProtectionOrder::getPositionId, position.getId())
                .eq(QuantProtectionOrder::getPositionCycleKey, position.getProtectionCycleKey())
                .eq(QuantProtectionOrder::getProtectionType, "STEP_TP"));
            for (QuantProtectionOrder previousStep : previousSteps) {
                String status = Objects.toString(previousStep.getStatus(), "");
                if (previousStep.getExecutedQuantity() != null && previousStep.getExecutedQuantity().signum() > 0
                    || Set.of("TRIGGERING", "TRIGGERED", "FINISHED", "PARTIALLY_FILLED", "FILLED").contains(status)) {
                    throw new ProtectionExecutionPendingException();
                }
                if (!Set.of("CANCELED", "EXPIRED", "REJECTED", "NOT_SENT").contains(status)
                    && !cancelOne(credentialId, previousStep, true, true)) {
                    throw new ProtectionExecutionPendingException();
                }
            }
        }
        if (targets != null && desired.stream().anyMatch(signal -> signal != null && "FIRST_TP".equals(signal.type())) && desired.stream().noneMatch(signal -> signal != null &&
            "STEP_TP".equals(signal.type())) && !firstProfitPending && confirmedStep(credentialId, position) > 0) {
            throw new IllegalStateException("已确认阶梯保护不能回退为首次止盈，先核实首平事实");
        }
        Set<String> desiredTypes = new HashSet<>();
        RuntimeException syncFailure = null;
        for (ProtectionSignal signal : desired) {
            if (signal == null || signal.type() == null || !STRATEGY_PROTECTION_TYPES.contains(signal.type())) {
                continue;
            }
            if (Integer.valueOf(2).equals(position.getProtectionPolicyVersion()) && !Set.of("FIRST_TP", "STEP_TP").contains(signal.type())) {
                continue;
            }
            desiredTypes.add(signal.type());
            try {
                if (live && Set.of("LOCAL_STOP", "SYMBOL_STOP").contains(signal.type()) && "STOP_MARKET".equalsIgnoreCase(signal.orderType())) {
                    BigDecimal base = signal.quantityBase() == ProtectionSignal.QuantityBase.INITIAL && initialPositionQuantity != null ? initialPositionQuantity : position.getQuantity();
                    BigDecimal quantity = normalizeQuantity(credentialId, position.getSymbol(), base.multiply(signal.quantityRate() == null ? BigDecimal.ONE : signal.quantityRate()).min(position.getQuantity()), true);
                    BigDecimal price = QuantityNormalizer.protectionPrice(signal.triggerPrice(), rule(credentialId, position.getSymbol()), position.getPositionSide(), signal.orderType());
                    if (confirmedCover(credentialId, position, signal.type(), quantity, price) != null) {
                        continue;
                    }
                }
                coordinated(credentialId, position, signal.type(), () -> {
                    syncStrategyProtectionTarget(credentialId, position, initialPositionQuantity, signal, firstRemaining, firstLocalExecuted, live);
                    return null;
                });
            } catch (RuntimeException ex) {
                if (syncFailure == null) {
                    syncFailure = ex;
                } else if (syncFailure != ex) {
                    syncFailure.addSuppressed(ex);
                }
            }
        }
        if (syncFailure != null) {
            // 每个目标独立尝试，避免 LOCAL_STOP/首止盈故障跳过 STEP_TP；任何目标未确认时保留所有旧保护。
            throw syncFailure;
        }
        List<QuantProtectionOrder> stale =
            mapper.selectList(new LambdaQueryWrapper<QuantProtectionOrder>().eq(QuantProtectionOrder::getCredentialId, credentialId).eq(QuantProtectionOrder::getPositionId,
                position.getId()).eq(position.getProtectionCycleKey() != null, QuantProtectionOrder::getPositionCycleKey, position.getProtectionCycleKey()).in(QuantProtectionOrder::getProtectionType, STRATEGY_PROTECTION_TYPES).in(QuantProtectionOrder::getStatus, ACTIVE_STATUSES));
        for (QuantProtectionOrder order : stale) {
            if (!desiredTypes.contains(order.getProtectionType())) {
                // 某张策略止损可能已经代替硬止损；目标暂时缺失不等于可以撤掉唯一的整仓保护。
                if (live && STOP_PROTECTION_TYPES.contains(order.getProtectionType())
                    && confirmedCover(credentialId, position, order.getProtectionType(),
                        normalizeQuantity(credentialId, position.getSymbol(), position.getQuantity(), true), order.getStopPrice()) == null) {
                    throw new IllegalStateException("缺少已确认的替代保护，保留已有止损: " + order.getProtectionType());
                }
                boolean requireUnfilled = firstProfitPending && "STEP_TP".equals(order.getProtectionType());
                if (!cancelOne(credentialId, order, live, requireUnfilled)) {
                    throw new IllegalStateException("过期策略保护单撤销失败: " + order.getProtectionType());
                }
            }
        }
        if (live) {
            pruneCoveredStops(credentialId, position);
        }
    }

    /** 只比较同一持仓周期的止损；不跨周期复用，也不把分批止盈当成整仓保护。 */
    private List<QuantProtectionOrder> scopedStops(Long credentialId, QuantPosition position) {
        if (position.getProtectionCycleKey() == null || position.getProtectionCycleKey().isBlank()) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<QuantProtectionOrder>()
            .eq(QuantProtectionOrder::getCredentialId, credentialId).eq(QuantProtectionOrder::getPositionId, position.getId())
            .eq(QuantProtectionOrder::getPositionCycleKey, position.getProtectionCycleKey()).in(QuantProtectionOrder::getProtectionType, STOP_PROTECTION_TYPES)
            .in(QuantProtectionOrder::getStatus, ACTIVE_STATUSES)).stream()
            .filter(order -> credentialId.equals(order.getCredentialId()) && position.getId().equals(order.getPositionId())
                && position.getProtectionCycleKey().equals(order.getPositionCycleKey()) && position.getSymbol().equals(order.getSymbol())
                && position.getPositionSide().equalsIgnoreCase(order.getPositionSide()) && STOP_PROTECTION_TYPES.contains(order.getProtectionType())
                && ACTIVE_STATUSES.contains(order.getStatus())).toList();
    }

    /**
     * 数量必须一致，做多触发价只能更高、做空只能更低。相同价位按阶梯、硬止损、策略止损的顺序保留，
     * 避免两张单互相认作冗余。覆盖单在自己的目标租约下精确查询，不能把它写入另一保护类型的目标记录。
     */
    private QuantProtectionOrder confirmedCover(Long credentialId, QuantPosition position, String type, BigDecimal quantity, BigDecimal price) {
        if (quantity == null || quantity.signum() <= 0 || price == null || price.signum() <= 0) {
            return null;
        }
        for (var candidate : scopedStops(credentialId, position)) {
            if (type.equals(candidate.getProtectionType()) || !"WORKING".equals(candidate.getStatus())
                || !"STOP_MARKET".equalsIgnoreCase(candidate.getOrderType()) || candidate.getQuantity() == null
                || candidate.getQuantity().compareTo(quantity) != 0 || candidate.getStopPrice() == null
                || candidate.getExecutedQuantity() != null && candidate.getExecutedQuantity().signum() > 0) {
                continue;
            }
            int tightening = candidate.getStopPrice().compareTo(price) * ("SHORT".equalsIgnoreCase(position.getPositionSide()) ? -1 : 1);
            if (tightening < 0 || tightening == 0 && STOP_PROTECTION_TYPES.indexOf(candidate.getProtectionType()) <= STOP_PROTECTION_TYPES.indexOf(type)) {
                continue;
            }
            if (coordinated(credentialId, position, candidate.getProtectionType(), () -> confirmOnExchange(credentialId, candidate, false))) {
                return candidate;
            }
        }
        return null;
    }

    /** 先精确确认足额覆盖保护，再清理较弱止损；本方法不撤销阶梯单。 */
    private void pruneCoveredStops(Long credentialId, QuantPosition position) {
        BigDecimal quantity = normalizeQuantity(credentialId, position.getSymbol(), position.getQuantity(), true);
        for (var old : scopedStops(credentialId, position)) {
            if ("STEP_TP".equals(old.getProtectionType()) || Integer.valueOf(2).equals(position.getProtectionPolicyVersion()) && "HARD_STOP".equals(old.getProtectionType()) || !"STOP_MARKET".equalsIgnoreCase(old.getOrderType()) || old.getStopPrice() == null) {
                continue;
            }
            var covering = confirmedCover(credentialId, position, old.getProtectionType(), quantity, old.getStopPrice());
            if (covering != null) {
                if (!coordinated(credentialId, position, old.getProtectionType(), () -> cancelOne(credentialId, old, true))) {
                    throw new IllegalStateException("覆盖保护已生效，旧止损撤销仍待确认");
                }
                log.info("已确认保护覆盖旧止损: positionId={} cycle={} oldType={} oldOrderId={} coveringType={} coveringOrderId={}",
                    position.getId(), position.getProtectionCycleKey(), old.getProtectionType(), old.getExchangeOrderId(), covering.getProtectionType(), covering.getExchangeOrderId());
            }
        }
    }

    /**
     * 协调保护目标的持久意图、下单确认和旧单清理顺序。
     */
    private void syncStrategyProtectionTarget(Long credentialId, QuantPosition position, BigDecimal initialPositionQuantity, ProtectionSignal signal, BigDecimal firstRemaining,
                                              BigDecimal firstLocalExecuted, boolean live) {
        BigDecimal baseQty = signal.quantityBase() == ProtectionSignal.QuantityBase.INITIAL && initialPositionQuantity != null ? initialPositionQuantity : position.getQuantity();
        BigDecimal targetQty = baseQty.multiply(signal.quantityRate() == null ? BigDecimal.ONE : signal.quantityRate());
        boolean firstWasWorking = false;
        if ("FIRST_TP".equals(signal.type())) {
            if (firstRemaining != null && firstRemaining.signum() <= 0) {
                return;
            }
            if (live && Integer.valueOf(2).equals(position.getProtectionPolicyVersion())) {
                if (firstLocalExecuted == null || firstLocalExecuted.signum() < 0) {
                    throw new IllegalStateException("首平缺少可信本地累计成交量，禁止创建新目标");
                }
                var fact = firstProfitFacts(credentialId, position);
                firstWasWorking = fact.working();
                targetQty = normalizeQuantity(credentialId, position.getSymbol(), targetQty, true)
                    .subtract(fact.executed()).subtract(firstLocalExecuted).max(BigDecimal.ZERO);
                if (firstRemaining != null) {
                    targetQty = targetQty.min(firstRemaining);
                }
            } else if (firstRemaining != null) {
                targetQty = firstRemaining;
            }
        }
        targetQty = targetQty.min(position.getQuantity());
        if (targetQty.signum() <= 0) {
            return;
        }
        targetQty = normalizeQuantity(credentialId, position.getSymbol(), targetQty, live);
        BigDecimal triggerPrice = normalizeProtectionPrice(credentialId, position, signal.triggerPrice(), signal.orderType(), live);
        if (targetQty.signum() <= 0 || triggerPrice == null || triggerPrice.signum() <= 0) {
            return;
        }

        QuantProtectionOrder existing = findActive(credentialId, position, signal.type());
        if (existing == null && firstWasWorking) {
            // 查询时仍活动的首平突然离开活动集合，可能刚成交；下一轮重核终态，绝不能创建第二份意图。
            throw new ProtectionExecutionPendingException();
        }
        boolean working = existing != null && (!live || confirmOnExchange(credentialId, existing, true, position.getQuantity()));
        if (working && existing.getStopPrice() != null && (signal.orderType() == null || signal.orderType().equalsIgnoreCase(existing.getOrderType()))) {
            if (!"STEP_TP".equals(signal.type())) {
                triggerPrice = existing.getStopPrice();
            } else {
                // 阶梯只能收紧，数量调整也必须保留已经锁定的更优价格。
                triggerPrice = "SHORT".equalsIgnoreCase(position.getPositionSide()) ? triggerPrice.min(existing.getStopPrice()) : triggerPrice.max(existing.getStopPrice());
            }
        }
        if (working && targetLease.get() != null) {
            targetLease.get().confirmed(existing, existing.getStepIndex() == null ? 0 : existing.getStepIndex());
            cancelSuperseded(credentialId, position.getId(), position.getProtectionCycleKey(), signal.type(), existing.getId(), live);
        }
        if (working && sameTarget(existing, signal.orderType(), targetQty, triggerPrice)) {
            if (targetLease.get() != null) {
                targetLease.get().confirmed(existing, signal.stepIndex());
            }
            cancelSuperseded(credentialId, position.getId(), position.getProtectionCycleKey(), signal.type(), existing.getId(), live);
            return;
        }
        logReplacement(position, signal.type(), existing, targetQty, triggerPrice, working);
        if ("FIRST_TP".equals(signal.type()) && existing != null) {
            // 分批止盈先停旧单。全量止损的“先新后旧”不能用于固定首平目标，否则撤单竞争会重复减仓。
            if (!cancelOne(credentialId, existing, live)) {
                throw new IllegalStateException("首次止盈旧单撤销结果未知，等待恢复");
            }
            if (existing.getExecutedQuantity() != null && existing.getExecutedQuantity().signum() > 0) {
                throw new ProtectionExecutionPendingException();
            }
            existing = null;
        }
        QuantProtectionOrder replacement = placeProtection(credentialId, position, signal.type(), signal.orderType(), targetQty, triggerPrice, live, signal.stepIndex());
        if (existing != null && !cancelOne(credentialId, existing, live)) {
            throw new IllegalStateException("新保护已生效，旧保护撤销失败: " + signal.type());
        }
        cancelSuperseded(credentialId, position.getId(), position.getProtectionCycleKey(), signal.type(), replacement.getId(), live);
    }

    /**
     * 创建保护单本地记录，并在 LIVE 下经确认流程派发交易所请求。
     */
    private QuantProtectionOrder placeProtection(Long credentialId, QuantPosition position, String protectionType, String orderType, BigDecimal quantity, BigDecimal stopPrice,
                                                 boolean live) {
        return placeProtection(credentialId, position, protectionType, orderType, quantity, stopPrice, live, 0);
    }

    private QuantProtectionOrder placeProtection(Long credentialId, QuantPosition position, String protectionType, String orderType, BigDecimal quantity, BigDecimal stopPrice,
                                                 boolean live, int stepIndex) {
        ExchangeGateway.Credential credential = executionApplicationService.toCredential(executionApplicationService.requireCredential(credentialId));
        validateProtectionMode(credential, live);
        String exchangePositionSide = resolveExchangePositionSide(credential, position.getPositionSide());
        String prefix = switch (protectionType) {
            case "HARD_STOP" -> "QHARD_";
            case "SYMBOL_STOP" -> "QSYM_";
            case "FIRST_TP" -> "QFTP_";
            case "STEP_TP" -> "QSTEP_";
            default -> "QSTOP_";
        };
        // 标识先写入持久化意图，超时后沿用该标识精确查询，禁止换标识盲目重试。
        String normalizedOrderType = orderType == null ? "STOP_MARKET" : orderType;
        QuantProtectionOrder entity = null;
        if (position.getProtectionCycleKey() != null && !position.getProtectionCycleKey().isBlank()) {
            entity = mapper.selectList(new LambdaQueryWrapper<QuantProtectionOrder>()
                .eq(QuantProtectionOrder::getCredentialId, credentialId)
                .eq(QuantProtectionOrder::getPositionId, position.getId())
                .eq(QuantProtectionOrder::getPositionCycleKey, position.getProtectionCycleKey())
                .eq(QuantProtectionOrder::getSymbol, position.getSymbol())
                .eq(QuantProtectionOrder::getPositionSide, position.getPositionSide())
                .eq(QuantProtectionOrder::getProtectionType, protectionType)
                .eq(QuantProtectionOrder::getOrderType, normalizedOrderType)
                .eq(QuantProtectionOrder::getQuantity, quantity)
                .eq(QuantProtectionOrder::getStopPrice, stopPrice)
                .eq(QuantProtectionOrder::getStepIndex, stepIndex)
                .eq(QuantProtectionOrder::getStatus, "NOT_SENT")
                .isNull(QuantProtectionOrder::getExchangeOrderId)
                .eq(QuantProtectionOrder::getExecutedQuantity, BigDecimal.ZERO)
                .orderByDesc(QuantProtectionOrder::getId)).stream().findFirst().orElse(null);
        }
        if (entity == null) {
            entity = new QuantProtectionOrder();
            entity.setCredentialId(credentialId);
            entity.setCreatedAtEpochMillis(System.currentTimeMillis());
            entity.setPositionId(position.getId());
            entity.setPositionCycleKey(position.getProtectionCycleKey());
            entity.setExecutedQuantity(BigDecimal.ZERO);
            entity.setSymbol(position.getSymbol());
            entity.setPositionSide(position.getPositionSide());
            entity.setClientOrderId(prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 28));
            entity.setProtectionType(protectionType);
            entity.setOrderType(normalizedOrderType);
            entity.setQuantity(quantity);
            entity.setStopPrice(stopPrice);
            entity.setStepIndex(stepIndex);
        }
        String clientAlgoId = entity.getClientOrderId();
        // Reuse is limited to a durable intent proven not to have reached Binance.
        // Keep its original clientAlgoId so retries cannot create a second identity.
        entity.setStatus("PAPER".equalsIgnoreCase(credential.runMode()) ? "PENDING" : "PREPARED");
        if (protectionJournal != null) {
            protectionJournal.captureOwner(entity);
        }
        if (targetLease.get() == null) {
            int affected = entity.getId() == null ? mapper.insert(entity) : mapper.updateById(entity);
            if (affected != 1 || entity.getId() == null) {
                throw new IllegalStateException("保护单意图未可靠持久化，拒绝发送交易所请求");
            }
        } else {
            QuantProtectionOrder previous = findActive(credentialId, position, protectionType);
            targetLease.get().prepare(entity, stepIndex, previous, row -> {
                int affected = row.getId() == null ? mapper.insert(row) : mapper.updateById(row);
                if (affected != 1 || row.getId() == null) {
                    throw new IllegalStateException("保护意图持久化失败");
                }
            });
        }
        log.info("止盈止损提交: accountId={}, positionId={}, symbol={}, direction={}, protectionType={}, orderType={}, quantity={}, triggerPrice={}, clientAlgoId={}, runMode={}",
            credentialId, position.getId(), position.getSymbol(), position.getPositionSide(), protectionType, entity.getOrderType(), quantity, stopPrice, clientAlgoId,
            credential.runMode());
        try {
            checkTargetLeaseBeforeDispatch();
            ExchangeGateway.ProtectionOrderResult result = submitProtected(credentialId, entity, new ExchangeGateway.ProtectionOrderRequest(credential, position.getSymbol(),
                "SHORT".equalsIgnoreCase(position.getPositionSide()) ? "BUY" : "SELL", exchangePositionSide, entity.getOrderType(), quantity, stopPrice, clientAlgoId, live));
            entity.setExchangeOrderId(result.orderId());
            if (!live) {
                entity.setStatus("PAPER_ACCEPTED");
                mapper.updateById(entity);
                log.info("止盈止损模拟设置完成: symbol={}, protectionType={}, clientAlgoId={}, quantity={}, triggerPrice={}", entity.getSymbol(), protectionType, clientAlgoId, quantity,
                    stopPrice);
            } else if (!confirmOnExchange(credentialId, entity)) {
                throw new IllegalStateException("Binance 保护单尚未确认生效");
            }
        } catch (RuntimeException ex) {
            if (ex instanceof ProtectionIntentChangedException) {
                throw ex;
            }
            entity.setStatus(protectionFailureStatus(ex));
            String failureSummary = ex instanceof org.dromara.quant.exchange.ExchangeTradingPort.ExchangeRequestException
                || ex instanceof org.dromara.quant.execution.LiveOrderDispatcher.DispatchBlockedException ? ex.getMessage() : ex.getClass().getSimpleName();
            String outcome = "NOT_SENT".equals(entity.getStatus()) ? "止盈止损未发送"
                : ex instanceof ProtectionConfirmationPendingException ? "止盈止损提交回执已收到，等待交易所精确确认"
                : "止盈止损提交或确认失败，结果待对账";
            log.warn("{}: accountId={}, positionId={}, symbol={}, protectionType={}, clientAlgoId={}, exchangeOrderId={}, quantity={}, triggerPrice={}, " +
                "runMode={}, errorType={}, failure={}", outcome, credentialId, position.getId(), position.getSymbol(), protectionType, clientAlgoId, entity.getExchangeOrderId(), quantity, stopPrice,
                credential.runMode(), ex.getClass().getSimpleName(), failureSummary);
            mapper.updateById(entity);
            if (("NOT_SENT".equals(entity.getStatus()) || "REJECTED".equals(entity.getStatus())) && targetLease.get() != null) {
                targetLease.get().terminal(entity);
            }
            if (confirmUncertainSubmission(credentialId, entity, ex)) {
                if (targetLease.get() != null) {
                    targetLease.get().confirmed(entity, stepIndex);
                }
                return entity;
            }
            throw ex;
        }
        if (targetLease.get() != null) {
            targetLease.get().confirmed(entity, stepIndex);
        }
        return entity;
    }

    /**
     * LIVE and TESTNET protections use the same durable account-symbol
     * dispatch guard as ordinary orders. Acknowledge only after the accepted Binance
     * algo order ID is persisted; unknown outcomes keep the guard occupied.
     */
    private ExchangeGateway.ProtectionOrderResult submitProtected(Long credentialId, QuantProtectionOrder local, ExchangeGateway.ProtectionOrderRequest request) {
        String runMode = validateProtectionMode(request.credential(), request.live());
        boolean exchangeBacked = !"PAPER".equals(runMode);
        if (!exchangeBacked) {
            return gateway.placeProtectionOrder(request);
        }
        var dispatcher = requireLiveDispatcher();
        var ticket = dispatcher.begin(credentialId, local.getSymbol(), "PROT_CREATE:" + local.getId() + ":" + UUID.randomUUID(), "PROTECT_CREATE", local.getClientOrderId(), null
            , null);
        boolean attempted = false;
        boolean finished = false;
        try {
            String preparedStatus = local.getStatus();
            QuantProtectionOrder dispatching = new QuantProtectionOrder();
            dispatching.setStatus("DISPATCHING");
            if (mapper.update(dispatching, new LambdaQueryWrapper<QuantProtectionOrder>()
                .eq(QuantProtectionOrder::getId, local.getId())
                .eq(QuantProtectionOrder::getStatus, preparedStatus)
                .isNull(QuantProtectionOrder::getExchangeOrderId)) != 1) {
                throw new ProtectionIntentChangedException();
            }
            local.setStatus("DISPATCHING");
            // The durable dispatch guard and target lease are separate safety fences.
            // Recheck target ownership after reserving the account-symbol slot and before REST.
            checkTargetLease();
            attempted = true;
            var result = gateway.placeProtectionOrder(request);
            if (result == null || result.orderId() == null || result.orderId() <= 0 || !local.getClientOrderId().equals(result.clientOrderId())) {
                throw new IllegalStateException("Binance 保护单回执缺少准确订单 ID 或客户端标识");
            }
            local.setExchangeOrderId(result.orderId());
            if (mapper.updateById(local) != 1) {
                throw new IllegalStateException("Binance 已接受保护单，但本地意图未可靠写入");
            }
            dispatcher.acknowledged(ticket, result.orderId());
            finished = true;
            return result;
        } catch (RuntimeException ex) {
            if (!finished) {
                try {
                    if (!attempted || ex instanceof org.dromara.quant.exchange.ExchangeTradingPort.OrderNotSentException || ex instanceof org.dromara.quant.exchange.ExchangeTradingPort.OrderRejectedException) {
                        dispatcher.notSent(ticket);
                    } else {
                        dispatcher.unknown(ticket);
                    }
                } catch (RuntimeException dispatchError) {
                    ex.addSuppressed(dispatchError);
                }
            }
            if (!attempted && !(ex instanceof org.dromara.quant.exchange.ExchangeTradingPort.OrderNotSentException)
                && !(ex instanceof ProtectionIntentChangedException)) {
                throw new ProtectionOrderNotSentException("保护目标租约失效，保护单未发送", ex);
            }
            throw ex;
        }
    }

    private void checkTargetLeaseBeforeDispatch() {
        try {
            checkTargetLease();
        } catch (RuntimeException ex) {
            throw new ProtectionOrderNotSentException("保护目标租约失效，保护单未发送", ex);
        }
    }

    private static String normalizeProtectionRunMode(ExchangeGateway.Credential credential) {
        if (credential == null || credential.runMode() == null) {
            throw new IllegalStateException("保护单缺少明确的交易运行模式");
        }
        String runMode = credential.runMode().trim().toUpperCase(java.util.Locale.ROOT);
        if (!Set.of("PAPER", "TESTNET", "LIVE").contains(runMode)) {
            throw new IllegalStateException("保护单交易运行模式无效");
        }
        return runMode;
    }

    private static String validateProtectionMode(ExchangeGateway.Credential credential, boolean exchangeBacked) {
        String runMode = normalizeProtectionRunMode(credential);
        if (exchangeBacked != !"PAPER".equals(runMode)) {
            throw new IllegalStateException("保护单运行模式与交易凭证不一致");
        }
        return runMode;
    }

    /**
     * A cancel remains UNKNOWN unless an exact exchange query proves a terminal
     * outcome and that fact has been persisted in the local protection ledger.
     */
    private boolean cancelProtected(Long credentialId, QuantProtectionOrder local, boolean live, Set<String> allowedTerminalStatuses, boolean requireUnfilled) {
        var credential = executionApplicationService.toCredential(executionApplicationService.requireCredential(credentialId));
        String runMode = validateProtectionMode(credential, live);
        // This local intent is known never to have reached the exchange. Treat it
        // as already quiesced; querying or canceling by client ID could create a
        // false UNKNOWN dispatch guard for an algo order that never existed.
        if ("NOT_SENT".equals(local.getStatus())) {
            return true;
        }
        if (Set.of("PREPARED", "REJECTED").contains(Objects.toString(local.getStatus(), "")) && local.getExchangeOrderId() == null) {
            try {
                if (!confirmOnExchange(credentialId, local, false)) {
                    return "NOT_SENT".equals(local.getStatus());
                }
            } catch (RuntimeException ex) {
                log.warn("保护单尚未进入可安全撤销状态，等待精确创建结果: accountId={}, symbol={}, clientAlgoId={}, runMode={}, errorType={}", credentialId,
                    local.getSymbol(), local.getClientOrderId(), credential.runMode(), ex.getClass().getSimpleName());
                return false;
            }
        }
        boolean exchangeBacked = !"PAPER".equals(runMode);
        if (!exchangeBacked) {
            gateway.cancelProtectionOrder(new ExchangeGateway.CancelProtectionOrderRequest(credential, local.getExchangeOrderId(), local.getClientOrderId(), false));
            local.setStatus("CANCELED");
            return mapper.updateById(local) == 1;
        }
        var dispatcher = requireLiveDispatcher();
        // Never resend a cancel when this precise algo order has an unresolved
        // dispatch ticket. First query and reconcile the exchange terminal fact.
        String unresolved = dispatcher.activeProtectionTicket(credentialId, local.getSymbol(), local.getClientOrderId(), "PROTECT_CANCEL");
        if (unresolved != null) {
            try {
                Long terminalAlgoId = persistVerifiedCancelledProtection(credentialId, local, credential, allowedTerminalStatuses, requireUnfilled);
                if (terminalAlgoId == null) {
                    log.warn("未知保护撤单尚无可释放派发票据的精确终态事实: accountId={}, symbol={}, clientAlgoId={}, runMode={}", credentialId, local.getSymbol(),
                        local.getClientOrderId(), credential.runMode());
                    return false;
                }
                return dispatcher.recoveredFromExchange(credentialId, local.getSymbol(), unresolved, local.getClientOrderId(), terminalAlgoId);
            } catch (RuntimeException ex) {
                log.warn("既有保护撤单仍未知，继续保留调度占用: accountId={}, symbol={}, clientAlgoId={}, runMode={}, errorType={}", credentialId, local.getSymbol(),
                    local.getClientOrderId(), credential.runMode(), ex.getClass().getSimpleName());
                return false;
            }
        }
        final LiveOrderDispatcher.Ticket ticket;
        try {
            ticket = dispatcher.begin(credentialId, local.getSymbol(), "PROT_CANCEL:" + local.getId() + ":" + UUID.randomUUID(), "PROTECT_CANCEL", local.getClientOrderId(), null,
                null);
        } catch (LiveOrderDispatcher.DispatchBlockedException blocked) {
            // Recover the ticket that owns this account-symbol slot, not the new cancel request.
            // The exchange request for the new cancel has not been sent.
            int recovered = recoverUnresolvedProtectionCancels(credentialId, local.getSymbol());
            log.warn("保护撤单被账户交易对派发门禁阻止，未发送 Binance 请求: accountId={}, symbol={}, clientAlgoId={}, runMode={}", credentialId, local.getSymbol(),
                local.getClientOrderId(), credential.runMode());
            if (recovered > 0) {
                log.info("门禁阻塞期间已核实并恢复旧保护撤单；当前保护操作等待下次运行周期 accountId={} symbol={} recoveredCount={}", credentialId, local.getSymbol(), recovered);
            }
            return false;
        }
        boolean finished = false;
        try {
            try {
                gateway.cancelProtectionOrder(new ExchangeGateway.CancelProtectionOrderRequest(credential, local.getExchangeOrderId(), local.getClientOrderId(), true));
            } catch (RuntimeException ex) {
                // A cancel can race with a fill: the exact Binance snapshot decides,
                // not the transport exception or absence from an open-order list.
            }
            Long terminalAlgoId = persistVerifiedCancelledProtection(credentialId, local, credential, allowedTerminalStatuses, requireUnfilled);
            if (terminalAlgoId == null) {
                return false;
            }
            dispatcher.acknowledged(ticket, terminalAlgoId);
            finished = true;
            return true;
        } catch (ProtectionExecutionPendingException pending) {
            throw pending;
        } catch (RuntimeException ex) {
            log.warn("保护撤单终态仍无法确认: accountId={}, symbol={}, clientAlgoId={}, runMode={}, errorType={}", credentialId, local.getSymbol(), local.getClientOrderId(),
                credential.runMode(), ex.getClass().getSimpleName());
            return false;
        } finally {
            if (!finished) {
                try {
                    dispatcher.unknown(ticket);
                } catch (RuntimeException ex) {
                    log.error("保护撤单调度结果未知，账户交易对必须保持阻断: accountId={}, symbol={}, runMode={}", credentialId, local.getSymbol(), credential.runMode(), ex);
                }
            }
        }
    }

    /**
     * Algo FINISHED is a triggered execution, not a cancellation. Release only after the exact
     * child order is fully filled and the remaining quantity is explained by prior protection fills.
     */
    private Long persistVerifiedTriggeredProtection(Long credentialId, QuantProtectionOrder local, ExchangeGateway.Credential credential) {
        ExchangeGateway.ProtectionOrderSnapshot actual = gateway.queryProtectionOrder(credential, local.getExchangeOrderId(),
            local.getClientOrderId(), local.getSymbol(), local.getCreatedAtEpochMillis());
        if (actual == null || !"FINISHED".equalsIgnoreCase(actual.status()) || actual.algoId() == null || actual.algoId() <= 0
            || actual.actualOrderId() == null || actual.actualOrderId() <= 0
            || !Objects.equals(local.getClientOrderId(), actual.clientAlgoId())
            || local.getExchangeOrderId() != null && !local.getExchangeOrderId().equals(actual.algoId())
            || local.getSymbol() == null || !local.getSymbol().equalsIgnoreCase(actual.symbol())
            || local.getPositionSide() == null
            || !("SHORT".equalsIgnoreCase(local.getPositionSide()) ? "BUY" : "SELL").equalsIgnoreCase(actual.side())
            || !("BOTH".equalsIgnoreCase(actual.positionSide()) || local.getPositionSide().equalsIgnoreCase(actual.positionSide()))
            || local.getOrderType() == null || !local.getOrderType().equalsIgnoreCase(actual.type())
            || local.getStopPrice() == null || actual.triggerPrice() == null || local.getStopPrice().compareTo(actual.triggerPrice()) != 0
            || !Boolean.TRUE.equals(actual.reduceOnly()) || !Boolean.FALSE.equals(actual.closePosition())
            || actual.quantity() == null || actual.quantity().signum() <= 0) {
            throw new IllegalStateException("Algo 触发终态与本地保护单身份或参数不一致");
        }
        if (local.getPositionId() == null || local.getPositionCycleKey() == null || local.getQuantity() == null) {
            throw new IllegalStateException("保护单缺少持仓周期或原始数量，无法证明 Algo 剩余数量");
        }
        List<QuantProtectionOrder> cycleOrders = mapper.selectList(new LambdaQueryWrapper<QuantProtectionOrder>()
            .eq(QuantProtectionOrder::getCredentialId, credentialId)
            .eq(QuantProtectionOrder::getPositionId, local.getPositionId())
            .eq(QuantProtectionOrder::getPositionCycleKey, local.getPositionCycleKey()));
        BigDecimal priorExecuted = cycleOrders.stream()
            .filter(order -> !Objects.equals(order.getId(), local.getId()))
            .filter(order -> "FINISHED".equalsIgnoreCase(order.getStatus()))
            .map(order -> order.getExecutedQuantity() == null ? BigDecimal.ZERO : order.getExecutedQuantity())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expectedRemaining = local.getQuantity().subtract(priorExecuted);
        if (expectedRemaining.signum() <= 0 || expectedRemaining.compareTo(actual.quantity()) != 0) {
            throw new IllegalStateException("Algo 数量变化无法由同一持仓周期已确认保护成交解释；localQuantity=" + local.getQuantity()
                + "; priorProtectionExecuted=" + priorExecuted + "; algoQuantity=" + actual.quantity());
        }

        ExchangeGateway.OrderSnapshot child = gateway.queryOrder(credential, local.getSymbol(), actual.actualOrderId(), null);
        if (child == null || !Objects.equals(actual.actualOrderId(), child.orderId())
            || !Objects.equals(local.getClientOrderId(), child.clientOrderId())
            || !local.getSymbol().equalsIgnoreCase(child.symbol())
            || !("SHORT".equalsIgnoreCase(local.getPositionSide()) ? "BUY" : "SELL").equalsIgnoreCase(child.side())
            || child.side() == null || !child.side().equalsIgnoreCase(actual.side())
            || child.positionSide() == null
            || !(child.positionSide().equalsIgnoreCase(actual.positionSide())
                || "BOTH".equalsIgnoreCase(child.positionSide()) && "BOTH".equalsIgnoreCase(actual.positionSide()))
            || !"MARKET".equalsIgnoreCase(child.type())
            || !"FILLED".equalsIgnoreCase(child.status())
            || child.originalQuantity() == null || child.executedQuantity() == null
            || child.originalQuantity().compareTo(actual.quantity()) != 0
            || child.executedQuantity().compareTo(child.originalQuantity()) != 0
            || actual.executedQuantity() == null || actual.executedQuantity().compareTo(child.executedQuantity()) != 0
            || !Boolean.TRUE.equals(child.reduceOnly()) || !Boolean.FALSE.equals(child.closePosition())) {
            throw new IllegalStateException("Algo 实际子订单未能证明身份一致且完整成交");
        }
        if (protectionJournal != null) {
            protectionJournal.bindChild(credential, local, actual, child);
        }
        if (decisionLogs != null) {
            decisionLogs.protection(credentialId, local, actual);
        }
        local.setExchangeOrderId(actual.algoId());
        local.setStatus("FINISHED");
        local.setExecutedQuantity(child.executedQuantity().max(local.getExecutedQuantity() == null ? BigDecimal.ZERO : local.getExecutedQuantity()));
        if (mapper.updateById(local) != 1) {
            throw new IllegalStateException("Algo 触发成交终态未可靠写入本地账本");
        }
        markTerminalTargetReference(local);
        log.info("保护单 Algo 已触发且实际子订单完整成交，持久化终态并准备释放派发门禁 accountId={} symbol={} clientAlgoId={} algoId={} actualOrderId={} executedQuantity={}",
            credentialId, local.getSymbol(), local.getClientOrderId(), actual.algoId(), actual.actualOrderId(), child.executedQuantity());
        return actual.algoId();
    }

    /** 在独立恢复线程中清除仍精确引用该终态保护单的目标关联。 */
    private void markTerminalTargetReference(QuantProtectionOrder local) {
        if (targets == null || local.getCredentialId() == null || local.getPositionId() == null
            || local.getPositionCycleKey() == null || local.getPositionSide() == null || local.getProtectionType() == null) {
            return;
        }
        QuantPosition position = new QuantPosition();
        position.setId(local.getPositionId());
        position.setProtectionCycleKey(local.getPositionCycleKey());
        position.setPositionSide(local.getPositionSide());
        try (ProtectionTargetCoordinator.Lease lease = targets.acquire(local.getCredentialId(), position, local.getProtectionType())) {
            ProtectionTargetCoordinator.State state = lease.state();
            if (Objects.equals(state.pendingOrderId(), local.getId())) {
                lease.terminal(local);
            }
            if (Objects.equals(state.currentOrderId(), local.getId()) || Objects.equals(state.previousOrderId(), local.getId())) {
                lease.markOrderTerminal(local);
            }
        }
    }

    /**
     * Persist exact exchange cancellation/trigger result before freeing any dispatch guard.
     */
    private Long persistVerifiedCancelledProtection(Long credentialId, QuantProtectionOrder local, ExchangeGateway.Credential credential, Set<String> allowedStatuses,
                                                    boolean requireUnfilled) {
        var actual = readProtectionSnapshot(credential, local);
        if (actual == null) {
            log.warn("保护单终态查询没有返回精确记录，保持派发阻断: accountId={}, symbol={}, clientAlgoId={}, runMode={}", credentialId, local.getSymbol(), local.getClientOrderId(),
                credential.runMode());
            return null;
        }
        boolean statusAllowed = allowedStatuses.contains(actual.status());
        boolean executionKnown = actual.executedQuantity() != null;
        boolean unfilledWhenRequired = !requireUnfilled || executionKnown && actual.executedQuantity().signum() == 0;
        boolean exchangeIdKnown = actual.algoId() != null && actual.algoId() > 0;
        boolean clientIdMatches = local.getClientOrderId().equals(actual.clientAlgoId());
        boolean storedIdMatches = local.getExchangeOrderId() == null || local.getExchangeOrderId().equals(actual.algoId());
        if (!statusAllowed || !executionKnown || !unfilledWhenRequired || !exchangeIdKnown || !clientIdMatches || !storedIdMatches) {
            log.warn("保护单终态记录不满足释放条件，保持派发阻断: accountId={}, symbol={}, clientAlgoId={}, runMode={}, exchangeStatus={}, executedQuantity={}, " +
                "statusAllowed={}, executionKnown={}, unfilledWhenRequired={}, exchangeIdKnown={}, clientIdMatches={}, storedIdMatches={}", credentialId, local.getSymbol(),
                local.getClientOrderId(), credential.runMode(), actual.status(), actual.executedQuantity(), statusAllowed, executionKnown, unfilledWhenRequired, exchangeIdKnown,
                clientIdMatches, storedIdMatches);
            return null;
        }
        if (decisionLogs != null) {
            decisionLogs.protection(credentialId, local, actual);
        }
        local.setExchangeOrderId(actual.algoId());
        local.setStatus(actual.status());
        local.setExecutedQuantity(actual.executedQuantity().max(local.getExecutedQuantity() == null ? BigDecimal.ZERO : local.getExecutedQuantity()));
        if (mapper.updateById(local) != 1) {
            throw new IllegalStateException("保护撤单终态未可靠写入本地账本");
        }
        return actual.algoId();
    }

    /**
     * 包括重复客户端标识在内的未知提交结果，只精确查询原意图，不再次发送。
     */
    private static String protectionFailureStatus(RuntimeException failure) {
        if (failure instanceof org.dromara.quant.exchange.ExchangeTradingPort.OrderRejectedException) {
            return "REJECTED";
        }
        if (failure instanceof org.dromara.quant.execution.LiveOrderDispatcher.DispatchBlockedException
            || failure instanceof org.dromara.quant.exchange.ExchangeTradingPort.OrderNotSentException) {
            return "NOT_SENT";
        }
        return "UNKNOWN";
    }

    private boolean confirmUncertainSubmission(Long credentialId, QuantProtectionOrder local, RuntimeException failure) {
        if (!(failure instanceof org.dromara.quant.exchange.ExchangeTradingPort.ExchangeRequestException)
            || failure instanceof org.dromara.quant.exchange.ExchangeTradingPort.OrderRejectedException
            || failure instanceof org.dromara.quant.exchange.ExchangeTradingPort.OrderNotSentException) {
            return false;
        }
        try {
            return confirmOnExchange(credentialId, local, false);
        } catch (ProtectionExecutionPendingException pending) {
            throw pending;
        } catch (RuntimeException unresolved) {
            return false;
        }
    }

    /**
     * 确认实际订单字段，未知结果不得当作保护生效。
     */
    private boolean confirmOnExchange(Long credentialId, QuantProtectionOrder local) {
        return confirmOnExchange(credentialId, local, true);
    }

    /**
     * 按订单 ID 或客户端 ID 精确查询保护单；未知结果不盲目重发。
     */
    private boolean confirmOnExchange(Long credentialId, QuantProtectionOrder local, boolean allowResubmit) {
        return confirmOnExchange(credentialId, local, allowResubmit, null);
    }

    private boolean confirmOnExchange(Long credentialId, QuantProtectionOrder local, boolean allowResubmit, BigDecimal currentQuantity) {
        if (recoverNeverSentIntent(credentialId, local)) {
            return false;
        }
        ExchangeGateway.Credential credential = executionApplicationService.toCredential(executionApplicationService.requireCredential(credentialId));
        ExchangeGateway.ProtectionOrderSnapshot actual;
        boolean definitelyAbsent = false;
        try {
            actual = readProtectionSnapshot(credential, local);
        } catch (org.dromara.quant.exchange.ExchangeTradingPort.ProtectionOrderAbsentException absent) {
            definitelyAbsent = true;
            if (allowResubmit && ("PENDING".equals(local.getStatus()) || "UNKNOWN".equals(local.getStatus()) || "REJECTED".equals(local.getStatus()))) {
                if (!"PAPER".equalsIgnoreCase(credential.runMode())
                    && requireLiveDispatcher().activeProtectionTicket(credentialId, local.getSymbol(), local.getClientOrderId(), "PROTECT_CREATE") != null) {
                    throw new IllegalStateException("原保护创建票据仍未决，等待该票据精确对账后再评估重挂");
                }
                local.setQuantity(normalizeQuantity(credentialId, local.getSymbol(), currentQuantity == null ? local.getQuantity() : local.getQuantity().min(currentQuantity),
                    true));
                local.setStopPrice(QuantityNormalizer.protectionPrice(local.getStopPrice(), rule(credentialId, local.getSymbol()), local.getPositionSide(), local.getOrderType()));
                local.setStatus("PENDING");
                if (mapper.updateById(local) != 1) {
                    throw new IllegalStateException("恢复保护单意图未可靠持久化，拒绝重送");
                }
                // 明确不存在才允许沿用持久化标识重送；不换标识，不生成第二份意图。
                log.info("止盈止损明确不存在，沿用原标识重送: accountId={}, symbol={}, protectionType={}, clientAlgoId={}, quantity={}, triggerPrice={}", credentialId, local.getSymbol(),
                    local.getProtectionType(), local.getClientOrderId(), local.getQuantity(), local.getStopPrice());
                String exchangeSide = resolveExchangePositionSide(credential, local.getPositionSide());
                ExchangeGateway.ProtectionOrderResult result;
                try {
                    checkTargetLeaseBeforeDispatch();
                    result = submitProtected(credentialId, local, new ExchangeGateway.ProtectionOrderRequest(credential, local.getSymbol(),
                        "SHORT".equalsIgnoreCase(local.getPositionSide()) ? "BUY" : "SELL", exchangeSide, local.getOrderType(), local.getQuantity(), local.getStopPrice(),
                        local.getClientOrderId(), true));
                } catch (RuntimeException ex) {
                    if (ex instanceof ProtectionIntentChangedException) {
                        throw ex;
                    }
                    local.setStatus(protectionFailureStatus(ex));
                    mapper.updateById(local);
                    if (("NOT_SENT".equals(local.getStatus()) || "REJECTED".equals(local.getStatus())) && targetLease.get() != null) {
                        targetLease.get().terminal(local);
                    }
                    if (confirmUncertainSubmission(credentialId, local, ex)) {
                        return true;
                    }
                    throw ex;
                }
                local.setExchangeOrderId(result.orderId());
                mapper.updateById(local);
                actual = readProtectionSnapshot(credential, local);
            } else {
                actual = null;
            }
        }
        if (actual == null) {
            if (!definitelyAbsent) {
                throw new ProtectionConfirmationPendingException();
            }
            if ("PENDING".equals(local.getStatus()) || "UNKNOWN".equals(local.getStatus()) || "DISPATCHING".equals(local.getStatus())) {
                throw new IllegalStateException("保护单提交结果未知，保留原意图等待精确查询: " + local.getClientOrderId());
            }
            local.setStatus("MISSING");
            mapper.updateById(local);
            if (targetLease.get() != null) {
                targetLease.get().terminal(local);
            }
            return false;
        }
        if (local.getClientOrderId() == null || !local.getClientOrderId().equals(actual.clientAlgoId()) || actual.algoId() == null || actual.algoId() <= 0 || local.getExchangeOrderId() != null && !local.getExchangeOrderId().equals(actual.algoId())) {
            throw new IllegalStateException("Binance 保护单恢复返回了冲突的算法订单身份");
        }
        if (decisionLogs != null) {
            decisionLogs.protection(credentialId, local, actual);
        }
        local.setExchangeOrderId(actual.algoId());
        if (Set.of("TRIGGERING", "TRIGGERED", "FINISHED").contains(actual.status()) || actual.executedQuantity() != null && actual.executedQuantity().signum() > 0) {
            // readProtectionSnapshot returns FINISHED only after the exact child order is terminal.
            // Persist that exchange fact before forcing position reconciliation; otherwise the
            // parent protection remains WORKING forever and its previous-target lease never clears.
            if ("FINISHED".equals(actual.status()) && actual.actualOrderId() != null && actual.actualOrderId() > 0) {
                local.setStatus("FINISHED");
                BigDecimal executed = actual.executedQuantity() == null ? BigDecimal.ZERO : actual.executedQuantity();
                local.setExecutedQuantity(executed.max(local.getExecutedQuantity() == null ? BigDecimal.ZERO : local.getExecutedQuantity()));
                if (mapper.updateById(local) != 1) {
                    throw new IllegalStateException("保护单实际子订单已终态，但本地保护账本更新失败");
                }
                if (targetLease.get() != null) {
                    ProtectionTargetCoordinator.State target = targetLease.get().state();
                    if (Objects.equals(target.pendingOrderId(), local.getId())) {
                        targetLease.get().terminal(local);
                    }
                    if (Objects.equals(target.currentOrderId(), local.getId()) || Objects.equals(target.previousOrderId(), local.getId())) {
                        targetLease.get().markOrderTerminal(local);
                    }
                } else if (targets != null && local.getPositionCycleKey() != null) {
                    QuantPosition scope = new QuantPosition();
                    scope.setId(local.getPositionId());
                    scope.setPositionSide(local.getPositionSide());
                    scope.setProtectionCycleKey(local.getPositionCycleKey());
                    coordinated(credentialId, scope, local.getProtectionType(), () -> {
                        ProtectionTargetCoordinator.State target = targetLease.get().state();
                        if (Objects.equals(target.pendingOrderId(), local.getId())) {
                            targetLease.get().terminal(local);
                        }
                        if (Objects.equals(target.currentOrderId(), local.getId()) || Objects.equals(target.previousOrderId(), local.getId())) {
                            targetLease.get().markOrderTerminal(local);
                        }
                        return null;
                    });
                }
            }
            throw new ProtectionExecutionPendingException();
        }
        if (!Set.of("NEW", "WORKING", "ACCEPTED").contains(actual.status())) {
            if (!"REJECTED".equals(actual.status()) && actual.executedQuantity() == null) {
                log.warn("保护单状态无法确认，保持待核实: accountId={}, symbol={}, protectionType={}, clientAlgoId={}, runMode={}, exchangeStatus={}", credentialId,
                    local.getSymbol(), local.getProtectionType(), local.getClientOrderId(), credential.runMode(), actual.status());
                throw new ProtectionExecutionPendingException();
            }
            local.setStatus(actual.status());
            if (actual.executedQuantity() != null) {
                local.setExecutedQuantity(actual.executedQuantity().max(local.getExecutedQuantity() == null ? BigDecimal.ZERO : local.getExecutedQuantity()));
            }
            if (mapper.updateById(local) != 1) {
                throw new IllegalStateException("保护单终态已确认但本地账本更新失败，保持派发占用");
            }
            if (targetLease.get() != null) {
                targetLease.get().terminal(local);
            }
            if (!"PAPER".equalsIgnoreCase(credential.runMode()) && Set.of("CANCELED", "EXPIRED", "REJECTED").contains(actual.status())
                && actual.executedQuantity() != null && actual.executedQuantity().signum() == 0) {
                requireLiveDispatcher().recoverVerifiedProtection(credentialId, local.getSymbol(), local.getClientOrderId(), "PROTECT_CREATE", actual.algoId());
            }
            return false;
        }
        boolean matching =
            actual.type() != null && sameTarget(local, actual.type(), actual.quantity(), actual.triggerPrice()) && ("SHORT".equalsIgnoreCase(local.getPositionSide()) ? "BUY" :
                "SELL").equalsIgnoreCase(actual.side()) && ("BOTH".equalsIgnoreCase(actual.positionSide()) || local.getPositionSide().equalsIgnoreCase(actual.positionSide()));
        if (!matching) {
            throw new IllegalStateException("Binance 保护单字段与持久化意图不一致");
        }
        boolean newlyConfirmed = !"WORKING".equals(local.getStatus());
        local.setStatus("WORKING");
        if (mapper.updateById(local) != 1) {
            throw new IllegalStateException("保护单已生效但本地账本更新失败，保持调度占用");
        }
        // Restore a previously uncertain LIVE placement only after the exact
        // Binance algo ID, client algo ID, parameters and durable local status agree.
        if (!"PAPER".equalsIgnoreCase(credential.runMode())) {
            requireLiveDispatcher().recoverVerifiedProtection(credentialId, local.getSymbol(), local.getClientOrderId(), "PROTECT_CREATE", actual.algoId());
        }
        if (newlyConfirmed) {
            log.info("止盈止损交易所确认生效: accountId={}, positionId={}, symbol={}, protectionType={}, exchangeOrderId={}, clientAlgoId={}, quantity={}, triggerPrice={}, " +
                "runMode={}, exchangeStatus={}", credentialId, local.getPositionId(), local.getSymbol(), local.getProtectionType(), actual.algoId(), local.getClientOrderId(),
                actual.quantity(), actual.triggerPrice(), credential.runMode(), actual.status());
        }
        return true;
    }

    /**
     * PREPARED 尚未进入派发，或请求已明确 REJECTED 且无交易所 ID；与创建方竞争同一条件更新。
     */
    private boolean recoverNeverSentIntent(Long credentialId, QuantProtectionOrder local) {
        if (!Set.of("PREPARED", "REJECTED").contains(Objects.toString(local.getStatus(), "")) || local.getExchangeOrderId() != null
            || local.getExecutedQuantity() != null && local.getExecutedQuantity().signum() != 0) {
            return false;
        }
        if (targetLease.get() == null) {
            QuantPosition scope = new QuantPosition();
            scope.setId(local.getPositionId());
            scope.setPositionSide(local.getPositionSide());
            scope.setProtectionCycleKey(local.getPositionCycleKey());
            return coordinated(credentialId, scope, local.getProtectionType(), () -> markNeverSentIntent(credentialId, local));
        }
        return markNeverSentIntent(credentialId, local);
    }

    private boolean markNeverSentIntent(Long credentialId, QuantProtectionOrder local) {
        checkTargetLease();
        if (requireLiveDispatcher().activeProtectionTicket(credentialId, local.getSymbol(), local.getClientOrderId(), "PROTECT_CREATE") != null) {
            return false;
        }
        QuantProtectionOrder update = new QuantProtectionOrder();
        update.setStatus("NOT_SENT");
        update.setExecutedQuantity(BigDecimal.ZERO);
        if (mapper.update(update, new LambdaQueryWrapper<QuantProtectionOrder>()
            .eq(QuantProtectionOrder::getId, local.getId())
            .eq(QuantProtectionOrder::getStatus, local.getStatus())
            .isNull(QuantProtectionOrder::getExchangeOrderId)
            .and(query -> query.isNull(QuantProtectionOrder::getExecutedQuantity).or().eq(QuantProtectionOrder::getExecutedQuantity, BigDecimal.ZERO))) != 1) {
            throw new IllegalStateException("保护意图已被并发派发或更新，保持待核实");
        }
        local.setStatus("NOT_SENT");
        local.setExecutedQuantity(BigDecimal.ZERO);
        if (targetLease.get() != null) {
            targetLease.get().terminal(local);
        }
        return true;
    }

    private void cancelSuperseded(Long credentialId, Long positionId, String cycleKey, String type, Long keepId, boolean live) {
        List<QuantProtectionOrder> older =
            mapper.selectList(new LambdaQueryWrapper<QuantProtectionOrder>().eq(QuantProtectionOrder::getCredentialId, credentialId).eq(QuantProtectionOrder::getPositionId,
                positionId).eq(cycleKey != null, QuantProtectionOrder::getPositionCycleKey, cycleKey).eq(QuantProtectionOrder::getProtectionType, type).ne(QuantProtectionOrder::getId, keepId).in(QuantProtectionOrder::getStatus, ACTIVE_STATUSES));
        for (QuantProtectionOrder order : older) {
            if (!cancelOne(credentialId, order, live)) {
                throw new IllegalStateException("旧保护单仍需撤销对账");
            }
        }
        if (targetLease.get() != null) {
            targetLease.get().cleaned();
        }
    }

    private void logReplacement(QuantPosition position, String type, QuantProtectionOrder old, BigDecimal quantity, BigDecimal price, boolean working) {
        String reason = old == null ? "首次挂载" : !working ? "交易所订单失效" : old.getQuantity().compareTo(quantity) != 0 ? "持仓数量变化" : "阶梯升级或订单类型变化";
        log.info("止盈止损目标更新: positionId={}, symbol={}, direction={}, protectionType={}, reason={}, oldOrderId={}, oldQuantity={}, newQuantity={}, oldTriggerPrice={}, " +
            "newTriggerPrice={}", position.getId(), position.getSymbol(), position.getPositionSide(), type, reason, old == null ? null : old.getExchangeOrderId(), old == null ?
            null : old.getQuantity(), quantity, old == null ? null : old.getStopPrice(), price);
    }

    private ExchangeGateway.SymbolRule rule(Long credentialId, String symbol) {
        ExchangeGateway.Credential credential = executionApplicationService.toCredential(executionApplicationService.requireCredential(credentialId));
        // 网关按 LIVE/TESTNET 缓存实际规则，保护单不能复用本地导入的交易对精度。
        return gateway.queryTradingRule(credential, symbol);
    }

    /**
     * 按实际交易环境的数量步长标准化保护单。
     */
    private BigDecimal normalizeQuantity(Long credentialId, String symbol, BigDecimal quantity, boolean live) {
        return live ? QuantityNormalizer.quantity(quantity, rule(credentialId, symbol), "MARKET") : quantity;
    }

    /**
     * 按实际交易环境的 PRICE_FILTER 标准化触发价。
     */
    private BigDecimal normalizeProtectionPrice(Long credentialId, QuantPosition position, BigDecimal price, String orderType, boolean live) {
        if (price == null || !live) {
            return price;
        }
        return QuantityNormalizer.protectionPrice(price, rule(credentialId, position.getSymbol()), position.getPositionSide(), orderType);
    }

    private BigDecimal normalizePrice(Long credentialId, String symbol, BigDecimal price, boolean live) {
        return price == null || !live ? price : QuantityNormalizer.price(price, rule(credentialId, symbol));
    }

    /**
     * 判断现有保护单是否与目标订单类型、数量和触发价一致。
     */
    private boolean sameTarget(QuantProtectionOrder existing, String orderType, BigDecimal quantity, BigDecimal triggerPrice) {
        if (existing == null || existing.getQuantity() == null || existing.getStopPrice() == null || quantity == null || triggerPrice == null) {
            return false;
        }
        return (orderType == null || orderType.equalsIgnoreCase(existing.getOrderType())) && existing.getQuantity().compareTo(quantity) == 0 && existing.getStopPrice().compareTo(triggerPrice) == 0;
    }

    /**
     * 查询指定持仓当前有效的保护单。
     */
    private QuantProtectionOrder findActive(Long credentialId, QuantPosition position, String type) {
        if (targetLease.get() != null && targetLease.get().state().pendingOrderId() != null) {
            var pending = mapper.selectById(targetLease.get().state().pendingOrderId());
            if (pending == null || !credentialId.equals(pending.getCredentialId()) || !position.getProtectionCycleKey().equals(pending.getPositionCycleKey()) || !type.equals(pending.getProtectionType())) {
                throw new IllegalStateException("持久化待确认保护意图关联不一致，禁止新建");
            }
            if (ACTIVE_STATUSES.contains(pending.getStatus())) {
                return pending;
            }
            if (pending.getExecutedQuantity() != null && pending.getExecutedQuantity().signum() > 0) {
                throw new ProtectionExecutionPendingException();
            }
            targetLease.get().terminal(pending);
        }
        var query = new LambdaQueryWrapper<QuantProtectionOrder>().eq(QuantProtectionOrder::getCredentialId, credentialId).eq(QuantProtectionOrder::getPositionId,
            position.getId()).eq(position.getProtectionCycleKey() != null, QuantProtectionOrder::getPositionCycleKey, position.getProtectionCycleKey()).eq(QuantProtectionOrder::getProtectionType, type).in(QuantProtectionOrder::getStatus, ACTIVE_STATUSES).orderByDesc(QuantProtectionOrder::getId);
        var candidates = mapper.selectList(query).stream().filter(order -> type.equals(order.getProtectionType())).toList();
        if (candidates.isEmpty()) {
            return targets == null ? mapper.selectOne(query.last("LIMIT 1")) : null;
        }
        if ("FIRST_TP".equals(type)) {
            return candidates.getFirst();
        }
        // 同周期止损重复单优先保留已验证的最严格目标，不能因ID较新选中更弱的保护。
        return candidates.stream().max((left, right) -> {
            int price = left.getStopPrice() == null ? -1 : right.getStopPrice() == null ? 1 : "SHORT".equalsIgnoreCase(position.getPositionSide()) ?
                right.getStopPrice().compareTo(left.getStopPrice()) : left.getStopPrice().compareTo(right.getStopPrice());
            return price != 0 ? price : Long.compare(left.getId(), right.getId());
        }).orElseThrow();
    }

    /**
     * 撤销单张保护单并更新本地状态。
     */
    private boolean cancelOne(Long credentialId, QuantProtectionOrder order, boolean live) {
        return cancelOne(credentialId, order, live, false);
    }

    private boolean cancelOne(Long credentialId, QuantProtectionOrder order, boolean live, boolean requireUnfilled) {
        if (targets != null && targetLease.get() == null) {
            QuantPosition scope = new QuantPosition();
            scope.setId(order.getPositionId());
            scope.setPositionSide(order.getPositionSide());
            scope.setProtectionCycleKey(order.getPositionCycleKey());
            return coordinated(credentialId, scope, order.getProtectionType(), () -> cancelOne(credentialId, order, live, requireUnfilled));
        }
        checkTargetLease();
        if ("MISSING".equals(order.getStatus()) && !live) {
            return true;
        }
        Set<String> terminalStatuses = requireUnfilled
            ? Set.of("CANCELED", "EXPIRED", "REJECTED")
            : Set.of("CANCELED", "EXPIRED", "REJECTED", "FINISHED");
        if (cancelProtected(credentialId, order, live, terminalStatuses, requireUnfilled)) {
            return true;
        }
        // No terminal exchange fact means cancellation is still unresolved; do not
        // persist CANCEL_FAILED, which would incorrectly imply a completed attempt.
        return false;
    }

    /**
     * 条件单触发后，普通子订单也必须确认终态；不能仅凭算法订单 FINISHED 再发减仓。
     */
    private ExchangeGateway.ProtectionOrderSnapshot readProtectionSnapshot(ExchangeGateway.Credential credential, QuantProtectionOrder order) {
        ExchangeGateway.ProtectionOrderSnapshot actual = gateway.queryProtectionOrder(credential, order.getExchangeOrderId(), order.getClientOrderId(), order.getSymbol(), order.getCreatedAtEpochMillis());
        if (actual == null || actual.actualOrderId() == null || actual.actualOrderId() <= 0) {
            return actual;
        }
        ExchangeGateway.OrderSnapshot child = gateway.queryOrder(credential, order.getSymbol(), actual.actualOrderId(), null);
        if (child != null && protectionJournal != null) {
            protectionJournal.bindChild(credential, order, actual, child);
        }
        if (child == null || child.executedQuantity() == null || !Set.of("FILLED", "CANCELED", "EXPIRED", "REJECTED").contains(child.status())) {
            throw new ProtectionExecutionPendingException();
        }
        return new ExchangeGateway.ProtectionOrderSnapshot(actual.symbol(), actual.algoId(), actual.clientAlgoId(), "FINISHED", actual.side(), actual.positionSide(),
            actual.type(), actual.quantity(), actual.triggerPrice(), actual.updateTime(), child.executedQuantity(), actual.reduceOnly(), actual.closePosition(),
            actual.actualOrderId(), child.averagePrice());
    }

    /**
     * 根据账户持仓模式映射交易所 positionSide。
     */
    private String resolveExchangePositionSide(ExchangeGateway.Credential credential, String localDirection) {
        if ("PAPER".equalsIgnoreCase(credential.runMode())) {
            return localDirection;
        }
        return gateway.queryPositionMode(credential).hedgeMode() ? localDirection : "BOTH";
    }

    /**
     * 判断指定策略保护是否仍真实存在于交易所。PAPER 返回 false，确保本地撮合/退出逻辑照常执行。
     */
    public boolean isStrategyProtectionWorking(Long credentialId, QuantPosition position, String protectionType, boolean live) {
        if (!live || position == null || position.getId() == null || protectionType == null || protectionType.isBlank()) {
            return false;
        }
        QuantProtectionOrder local = findActive(credentialId, position, protectionType);
        if (local == null) {
            return false;
        }
        try {
            return confirmOnExchange(credentialId, local, false);
        } catch (ProtectionExecutionPendingException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            // 无法确认时交由本地退出流程刷新持仓，再决定是否减仓。
            return false;
        }
    }

    /**
     * 判断固定硬止损是否处于有效状态。
     */
    public boolean isHardStopWorking(Long credentialId, QuantPosition position) {
        if (position == null || position.getId() == null) {
            return false;
        }
        return isStrategyProtectionWorking(credentialId, position, "HARD_STOP", true);
    }

    /**
     * 部分减仓 fallback 前必须确认对应条件单已撤销，避免双重首止盈。
     */
    public boolean cancelStrategyProtection(Long credentialId, QuantPosition position, String type) {
        QuantProtectionOrder local =
            mapper.selectOne(new LambdaQueryWrapper<QuantProtectionOrder>().eq(QuantProtectionOrder::getCredentialId, credentialId).eq(QuantProtectionOrder::getPositionId,
                position.getId()).eq(QuantProtectionOrder::getProtectionType, type).orderByDesc(QuantProtectionOrder::getId).last("LIMIT 1"));
        if (local == null) {
            return true;
        }
        // Cancel + exact terminal proof + durable state update execute under one dispatch ticket.
        return cancelProtected(credentialId, local, true, Set.of("CANCELED", "EXPIRED", "REJECTED"), true);
    }

    /**
     * 精确读取本周期全部首止盈订单的成交量；重挂后的成交也保留，人工减仓不计入。
     */
    public BigDecimal firstTakeProfitExecutedQuantity(Long credentialId, QuantPosition position) {
        return firstProfitFacts(credentialId, position).executed();
    }

    private record FirstProfitFacts(BigDecimal executed, boolean working) { }

    private FirstProfitFacts firstProfitFacts(Long credentialId, QuantPosition position) {
        List<QuantProtectionOrder> orders =
            mapper.selectList(new LambdaQueryWrapper<QuantProtectionOrder>().eq(QuantProtectionOrder::getCredentialId, credentialId).eq(QuantProtectionOrder::getPositionId,
                position.getId()).eq(QuantProtectionOrder::getPositionCycleKey, position.getProtectionCycleKey()).eq(QuantProtectionOrder::getProtectionType, "FIRST_TP"));
        ExchangeGateway.Credential credential = executionApplicationService.toCredential(executionApplicationService.requireCredential(credentialId));
        BigDecimal total = BigDecimal.ZERO;
        boolean working = false;
        for (QuantProtectionOrder order : orders) {
            if (recoverNeverSentIntent(credentialId, order)
                || Set.of("NOT_SENT", "REJECTED").contains(order.getStatus()) && order.getExchangeOrderId() == null
                    && (order.getExecutedQuantity() == null || order.getExecutedQuantity().signum() == 0)) {
                continue;
            }
            ExchangeGateway.ProtectionOrderSnapshot actual = readProtectionSnapshot(credential, order);
            if (actual == null || Set.of("TRIGGERING", "TRIGGERED").contains(actual.status())) {
                throw new ProtectionExecutionPendingException();
            }
            if (actual.executedQuantity() == null && !Set.of("NEW", "WORKING", "ACCEPTED").contains(actual.status())) {
                throw new ProtectionExecutionPendingException();
            }
            if (!Objects.equals(order.getSymbol(), actual.symbol()) || !Objects.equals(order.getClientOrderId(), actual.clientAlgoId())
                || actual.algoId() == null || actual.algoId() <= 0 || order.getExchangeOrderId() != null && !order.getExchangeOrderId().equals(actual.algoId())
                || !sameTarget(order, actual.type(), actual.quantity(), actual.triggerPrice())
                || !("SHORT".equalsIgnoreCase(order.getPositionSide()) ? "BUY" : "SELL").equalsIgnoreCase(actual.side())
                || !("BOTH".equalsIgnoreCase(actual.positionSide()) || Objects.equals(order.getPositionSide(), actual.positionSide()))) {
                throw new IllegalStateException("首平精确查询与持久化身份或参数冲突");
            }
            boolean active = Set.of("NEW", "WORKING", "ACCEPTED").contains(actual.status());
            if (!active && (!Set.of("FINISHED", "CANCELED", "EXPIRED", "REJECTED").contains(actual.status())
                || actual.executedQuantity() == null && !"REJECTED".equals(actual.status()))) {
                throw new ProtectionExecutionPendingException();
            }
            working |= active;
            BigDecimal executed = actual.executedQuantity() == null ? BigDecimal.ZERO : actual.executedQuantity();
            order.setExecutedQuantity(executed.max(order.getExecutedQuantity() == null ? BigDecimal.ZERO : order.getExecutedQuantity()));
            order.setStatus(actual.status());
            if (mapper.updateById(order) != 1) {
                throw new IllegalStateException("首平成交事实未可靠持久化，禁止重建目标");
            }
            total = total.add(order.getExecutedQuantity());
        }
        return new FirstProfitFacts(total, working);
    }

    /**
     * 本地兜底前停止本周期本系统所有条件单，精确终态确认后才允许刷新持仓并补平。
     */
    public boolean quiesceCycleProtections(Long credentialId, QuantPosition position) {
        List<QuantProtectionOrder> orders =
            mapper.selectList(new LambdaQueryWrapper<QuantProtectionOrder>().eq(QuantProtectionOrder::getCredentialId, credentialId).eq(QuantProtectionOrder::getPositionId,
                position.getId()).eq(QuantProtectionOrder::getPositionCycleKey, position.getProtectionCycleKey()).in(QuantProtectionOrder::getProtectionType, "HARD_STOP",
                "LOCAL_STOP", "SYMBOL_STOP", "FIRST_TP", "STEP_TP").in(QuantProtectionOrder::getStatus, ACTIVE_STATUSES));
        for (QuantProtectionOrder order : orders) {
            if (!cancelProtected(credentialId, order, true, Set.of("CANCELED", "EXPIRED", "REJECTED", "FINISHED"), false)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 首止盈只由该持仓关联条件单的真实成交事实确认，不以普通减仓推断。
     */
    public boolean isFirstTakeProfitFilled(Long credentialId, QuantPosition position, BigDecimal initialQuantity) {
        QuantProtectionOrder local =
            mapper.selectOne(new LambdaQueryWrapper<QuantProtectionOrder>().eq(QuantProtectionOrder::getCredentialId, credentialId).eq(QuantProtectionOrder::getPositionId,
                position.getId()).eq(QuantProtectionOrder::getProtectionType, "FIRST_TP").orderByDesc(QuantProtectionOrder::getId).last("LIMIT 1"));
        if (local == null) {
            return false;
        }
        ExchangeGateway.Credential credential = executionApplicationService.toCredential(executionApplicationService.requireCredential(credentialId));
        ExchangeGateway.ProtectionOrderSnapshot actual;
        try {
            actual = readProtectionSnapshot(credential, local);
        } catch (org.dromara.quant.exchange.ExchangeTradingPort.ProtectionOrderAbsentException absent) {
            // 不存在不是成交证明；提交结果未知的原意图仍交由保护恢复流程精确处理。
            return false;
        }
        if (actual == null) {
            return false;
        }
        if (decisionLogs != null) {
            decisionLogs.protection(credentialId, local, actual);
        }
        if ("FINISHED".equalsIgnoreCase(actual.status()) && actual.executedQuantity() != null && actual.executedQuantity().signum() > 0 && local.getQuantity() != null && actual.executedQuantity().compareTo(local.getQuantity()) >= 0 && initialQuantity != null && position.getQuantity() != null && position.getQuantity().compareTo(initialQuantity.subtract(actual.executedQuantity()).max(BigDecimal.ZERO)) <= 0) {
            local.setStatus("FINISHED");
            mapper.updateById(local);
            return true;
        }
        return false;
    }

    /**
     * 撤销指定持仓的全部活动保护单。
     */
    public boolean cancelActiveProtectionOrders(Long credentialId, Long positionId, boolean live) {
        return cancelActiveProtectionOrders(credentialId, positionId, null, live);
    }

    /**
     * 撤销指定持仓周期仍有效的保护订单，并返回是否全部确认撤销。
     */
    public boolean cancelActiveProtectionOrders(Long credentialId, Long positionId, String cycleKey, boolean live) {
        if (credentialId == null || positionId == null) {
            return true;
        }
        List<QuantProtectionOrder> orders =
            mapper.selectList(new LambdaQueryWrapper<QuantProtectionOrder>().eq(QuantProtectionOrder::getCredentialId, credentialId).eq(QuantProtectionOrder::getPositionId,
                positionId).eq(cycleKey != null, QuantProtectionOrder::getPositionCycleKey, cycleKey).in(QuantProtectionOrder::getProtectionType, "HARD_STOP", "LOCAL_STOP",
                "SYMBOL_STOP", "FIRST_TP", "STEP_TP").and(query -> query.in(QuantProtectionOrder::getStatus, ACTIVE_STATUSES).or().eq(QuantProtectionOrder::getStatus, "MISSING")));
        for (QuantProtectionOrder order : orders) {
            if (!cancelOne(credentialId, order, live)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 在交易所已确认指定交易对空仓后，撤销该交易对所有遗留保护单。
     * 用于旧状态缺少 positionId 的恢复场景；限定 credential + symbol，避免影响其他交易对。
     */
    public boolean cancelActiveProtectionOrdersForSymbol(Long credentialId, String symbol, boolean live) {
        if (credentialId == null || symbol == null || symbol.isBlank()) {
            return false;
        }
        List<QuantProtectionOrder> orders = mapper.selectList(
            new LambdaQueryWrapper<QuantProtectionOrder>()
                .eq(QuantProtectionOrder::getCredentialId, credentialId)
                .eq(QuantProtectionOrder::getSymbol, symbol)
                .in(QuantProtectionOrder::getProtectionType, "HARD_STOP", "LOCAL_STOP", "SYMBOL_STOP", "FIRST_TP", "STEP_TP")
                .and(query -> query.in(QuantProtectionOrder::getStatus, ACTIVE_STATUSES).or().eq(QuantProtectionOrder::getStatus, "MISSING")));
        for (QuantProtectionOrder order : orders) {
            if (!cancelOne(credentialId, order, live)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 开仓前的空仓清理屏障。网络调用由运行时恢复线程执行；列表缺单不作为撤单终态证明，
     * 手工委托不撤，带本系统前缀但缺少账本归属的委托阻断开仓并等待核查。
     */
    public boolean prepareEntryProtectionCleanup(Long credentialId, String symbol, boolean live) {
        var credential = executionApplicationService.toCredential(executionApplicationService.requireCredential(credentialId));
        validateProtectionMode(credential, live);
        if (live && (!exchangeFlatAndIdle(credential, symbol) || !recoverDispatchBeforeStrategyStart(credentialId, symbol))) {
            return false;
        }
        var locals = mapper.selectList(new LambdaQueryWrapper<QuantProtectionOrder>()
            .eq(QuantProtectionOrder::getCredentialId, credentialId).eq(QuantProtectionOrder::getSymbol, symbol)).stream()
            .filter(local -> local.getProtectionType() != null && (STOP_PROTECTION_TYPES.contains(local.getProtectionType()) || "FIRST_TP".equals(local.getProtectionType()))).toList();
        if (live) {
            var active = gateway.queryProtectionOrders(credential, symbol);
            if (active == null) {
                return false;
            }
            for (var actual : active) {
                if (!symbol.equals(actual.symbol())) {
                    return false;
                }
                var matches = locals.stream().filter(local -> actual.clientAlgoId() != null && actual.clientAlgoId().equals(local.getClientOrderId())
                    || actual.algoId() != null && actual.algoId().equals(local.getExchangeOrderId())).toList();
                if (matches.isEmpty()) {
                    if (systemProtectionId(actual.clientAlgoId())) {
                        return false;
                    }
                    continue;
                }
                if (matches.size() != 1 || "NOT_SENT".equals(matches.getFirst().getStatus())
                    || !Objects.equals(matches.getFirst().getClientOrderId(), actual.clientAlgoId())
                    || matches.getFirst().getExchangeOrderId() != null && !matches.getFirst().getExchangeOrderId().equals(actual.algoId())
                    || !cancelOne(credentialId, matches.getFirst(), true)) {
                    return false;
                }
            }
        }
        for (var local : locals) {
            if ((ACTIVE_STATUSES.contains(local.getStatus()) || "MISSING".equals(local.getStatus())) && !cancelOne(credentialId, local, live)) {
                return false;
            }
        }
        if (!live) {
            return true;
        }
        if (!exchangeFlatAndIdle(credential, symbol) || requireLiveDispatcher().activeDispatch(credentialId, symbol) != null) {
            return false;
        }
        var remaining = gateway.queryProtectionOrders(credential, symbol);
        return remaining != null && remaining.stream().allMatch(actual -> symbol.equals(actual.symbol())) && remaining.stream().noneMatch(actual -> systemProtectionId(actual.clientAlgoId())
            || locals.stream().anyMatch(local -> actual.clientAlgoId() != null && actual.clientAlgoId().equals(local.getClientOrderId())
                || actual.algoId() != null && actual.algoId().equals(local.getExchangeOrderId())));
    }

    private boolean exchangeFlatAndIdle(ExchangeGateway.Credential credential, String symbol) {
        var positions = gateway.queryPositions(credential, symbol);
        if (positions == null || positions.stream().anyMatch(position -> !symbol.equals(position.symbol())
            || position.quantity() == null || position.quantity().signum() != 0)) {
            return false;
        }
        var orders = gateway.queryOrders(credential, symbol);
        return orders != null && orders.isEmpty();
    }

    private boolean systemProtectionId(String clientId) {
        return clientId != null && List.of("QHARD_", "QFTP_", "QSTEP_", "QSTOP_", "QSYM_").stream().anyMatch(clientId::startsWith);
    }

    /**
     * 本地兜底接管时只撤销本次触发的同类型交易所保护单，其它保护继续保留。
     * 精确核实整个周期的在途条件单与子订单；触发中或结果未知时不允许补平，返回后仍须刷新持仓并由持久化派发门禁提交只减仓订单。
     */
    public boolean prepareLocalExit(Long credentialId, QuantPosition position, String protectionType) {
        if (position == null || position.getId() == null || position.getProtectionCycleKey() == null) {
            return false;
        }
        var credential = executionApplicationService.toCredential(executionApplicationService.requireCredential(credentialId));
        var statuses = new java.util.ArrayList<>(ACTIVE_STATUSES);
        statuses.add("FINISHED");
        statuses.add("MISSING");
        var orders = mapper.selectList(new LambdaQueryWrapper<QuantProtectionOrder>()
            .eq(QuantProtectionOrder::getCredentialId, credentialId).eq(QuantProtectionOrder::getPositionId, position.getId())
            .eq(QuantProtectionOrder::getPositionCycleKey, position.getProtectionCycleKey()).in(QuantProtectionOrder::getStatus, statuses));
        try {
            // 先核实所有事实，再停首平；不能先撤单后才发现另一张条件单正在成交。
            for (var order : orders) {
                if (!Objects.equals(credentialId, order.getCredentialId()) || !Objects.equals(position.getId(), order.getPositionId())
                    || !Objects.equals(position.getProtectionCycleKey(), order.getPositionCycleKey())) {
                    return false;
                }
                if (recoverNeverSentIntent(credentialId, order)) {
                    continue;
                }
                var actual = readProtectionSnapshot(credential, order);
                if (actual == null || !Objects.equals(order.getSymbol(), actual.symbol()) || !Objects.equals(order.getClientOrderId(), actual.clientAlgoId())
                    || actual.algoId() == null || actual.algoId() <= 0
                    || order.getExchangeOrderId() != null && !order.getExchangeOrderId().equals(actual.algoId())
                    || !sameTarget(order, actual.type(), actual.quantity(), actual.triggerPrice())
                    || !("SHORT".equalsIgnoreCase(order.getPositionSide()) ? "BUY" : "SELL").equalsIgnoreCase(actual.side())
                    || !("BOTH".equalsIgnoreCase(actual.positionSide()) || Objects.equals(order.getPositionSide(), actual.positionSide()))) {
                    return false;
                }
                if (Set.of("NEW", "WORKING", "ACCEPTED").contains(actual.status())) {
                    if (actual.executedQuantity() != null && actual.executedQuantity().signum() > 0) {
                        return false;
                    }
                    order.setStatus("WORKING");
                } else if (Set.of("FINISHED", "CANCELED", "EXPIRED", "REJECTED").contains(actual.status()) && actual.executedQuantity() != null) {
                    order.setStatus(actual.status());
                    order.setExecutedQuantity(actual.executedQuantity().max(order.getExecutedQuantity() == null ? BigDecimal.ZERO : order.getExecutedQuantity()));
                } else {
                    return false;
                }
                order.setExchangeOrderId(actual.algoId());
                if (mapper.updateById(order) != 1) {
                    return false;
                }
            }
            // 本地接管哪个保护目标，就先撤销并精确确认同类型交易所条件单；
            // 其它类型保护继续保留，尤其不能因止盈回退而撤掉整仓硬止损。
            for (var order : orders) {
                if (Objects.equals(protectionType, order.getProtectionType()) && ACTIVE_STATUSES.contains(order.getStatus())
                    && !cancelOne(credentialId, order, true, true)) {
                    return false;
                }
            }
            return true;
        } catch (RuntimeException ex) {
            log.warn("本地退出等待保护事实核实，保留整仓保护 accountId={} symbol={} cycle={} errorType={}", credentialId,
                position.getSymbol(), position.getProtectionCycleKey(), ex.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * 撤销无法关联有效持仓的孤儿保护单。
     */
    public boolean cancelOrphanProtectionOrders(Long credentialId, boolean live) {
        if (credentialId == null) {
            return true;
        }
        List<QuantProtectionOrder> orders =
            mapper.selectList(new LambdaQueryWrapper<QuantProtectionOrder>().eq(QuantProtectionOrder::getCredentialId, credentialId).isNull(QuantProtectionOrder::getPositionId).in(QuantProtectionOrder::getStatus, "NEW", "WORKING", "ACCEPTED"));
        for (QuantProtectionOrder order : orders) {
            if (!cancelOne(credentialId, order, live)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 交易所统一网关。
     */
    public static class ProtectionExecutionPendingException extends IllegalStateException {
        public ProtectionExecutionPendingException() {
            this("保护单正在触发/已成交，等待持仓及成交对账");
        }

        protected ProtectionExecutionPendingException(String message) {
            super(message);
        }
    }

    /**
     * 创建回执已收到，但精确查询暂时没有返回该保护单；保留原意图等待后续核实。
     */
    public static final class ProtectionConfirmationPendingException extends ProtectionExecutionPendingException {
        public ProtectionConfirmationPendingException() {
            super("保护单查询暂未返回精确记录，保留原意图等待核实");
        }
    }

    private static final class ProtectionOrderNotSentException extends org.dromara.quant.exchange.ExchangeTradingPort.OrderNotSentException {
        private ProtectionOrderNotSentException(String reason, RuntimeException cause) {
            super(reason);
            addSuppressed(cause);
        }
    }

    /** CAS 失败仅说明本线程失去意图所有权，不能用旧实体覆盖另一线程的派发事实。 */
    private static final class ProtectionIntentChangedException extends IllegalStateException {
        private ProtectionIntentChangedException() {
            super("保护意图已被并发恢复或派发，本线程未发送且不覆盖最新状态");
        }
    }

}
