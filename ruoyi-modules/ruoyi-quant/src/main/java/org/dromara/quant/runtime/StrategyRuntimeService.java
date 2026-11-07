package org.dromara.quant.runtime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.dromara.quant.config.QuantProperties;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.credential.mapper.QuantCredentialMapper;
import org.dromara.quant.decision.DecisionLogCursor;
import org.dromara.quant.decision.DecisionLogEvent;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.exchange.ExchangeTradingPort;
import org.dromara.quant.exchange.ExchangeMarketPort;
import org.dromara.quant.execution.ExecutionRecordService;
import org.dromara.quant.execution.ExecutionResult;
import org.dromara.quant.execution.TradeExecutionApplicationService;
import org.dromara.quant.execution.domain.QuantExecutionRecord;
import org.dromara.quant.market.MarketStreamHub;
import org.dromara.quant.market.UserDataStreamHub;
import org.dromara.quant.market.domain.QuantSymbol;
import org.dromara.quant.market.mapper.QuantSymbolMapper;
import org.dromara.quant.notification.QuantTradeNotificationService;
import org.dromara.quant.paper.PaperTradingService;
import org.dromara.quant.position.domain.QuantPosition;
import org.dromara.quant.position.mapper.QuantPositionMapper;
import org.dromara.quant.protection.ProtectionOrderService;
import org.dromara.quant.protection.ProtectionTargetCoordinator;
import org.dromara.quant.reconcile.TradingReconciliationService;
import org.dromara.quant.risk.LiveTradingGate;
import org.dromara.quant.risk.OpenCapacityService;
import org.dromara.quant.runtime.coordination.StrategyEntryGuardService;
import org.dromara.quant.runtime.coordination.StrategyRuntimeLeaseService;
import org.dromara.quant.runtime.coordination.mapper.StrategyRuntimeLedgerMapper;
import org.dromara.quant.runtime.domain.QuantStrategyPositionState;
import org.dromara.quant.runtime.service.StrategyPositionStateService;
import org.dromara.quant.strategy.*;
import org.dromara.quant.strategy.domain.QuantStrategy;
import org.dromara.quant.strategy.domain.QuantStrategyInstance;
import org.dromara.quant.strategy.domain.QuantStrategySymbol;
import org.dromara.quant.strategy.mapper.QuantStrategyInstanceMapper;
import org.dromara.quant.strategy.mapper.QuantStrategyMapper;
import org.dromara.quant.strategy.mapper.QuantStrategySymbolMapper;
import org.dromara.quant.util.QuantJsonMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 事件驱动策略运行时。
 * <p>运行时通过 MarketStreamHub 接收实时行情；定时任务只负责发现实例、续租和 REST 对账，不再轮询每个实例的实时价格。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class StrategyRuntimeService {
    /**
     * 量化模块配置。
     */
    private final QuantProperties properties;
    /** 用于跨节点串行化交易对暂停操作与开仓派单。 */
    private final RedissonClient redissonClient;
    /**
     * 策略实例 Mapper。
     */
    private final QuantStrategyInstanceMapper instanceMapper;
    /**
     * 策略定义 Mapper。
     */
    private final QuantStrategyMapper strategyMapper;
    /**
     * 策略交易对 Mapper。
     */
    private final QuantStrategySymbolMapper strategySymbolMapper;
    /**
     * 交易凭证 Mapper。
     */
    private final QuantCredentialMapper credentialMapper;
    /**
     * 交易对 Mapper。
     */
    private final QuantSymbolMapper symbolMapper;
    /**
     * 持仓 Mapper。
     */
    private final QuantPositionMapper positionMapper;
    /**
     * 策略插件注册表。
     */
    private final StrategyPluginRegistry pluginRegistry;
    /**
     * 交易所统一网关。
     */
    private final ExchangeGateway gateway;
    /** 同环境、同交易对共享的交易规则缓存；缓存失效不改变开仓风控判定。 */
    private final org.dromara.quant.exchange.ExchangeTradingRuleCache tradingRuleCache;
    /**
     * 公共行情流中心。
     */
    private final MarketStreamHub marketStreamHub;
    private final UserDataStreamHub userDataStreamHub;
    private final org.dromara.quant.market.AccountBalanceCache balanceCache;
    private final org.dromara.quant.risk.OpenCapacityService openCapacityService;
    private final org.dromara.quant.execution.TradingPreparationService preparationService;
    private final TradingReconciliationService reconciliationService;
    private final ExecutorService recoveryExecutor = Executors.newVirtualThreadPerTaskExecutor();
    /** Dedicated executor prevents slow account renewals from serializing unrelated strategy leases. */
    private final ExecutorService leaseRenewalExecutor = Executors.newVirtualThreadPerTaskExecutor();
    /**
     * 策略斜率结果 SSE 事件中心。
     */
    private final StrategySlopeTelemetryHub slopeTelemetryHub;
    /**
     * 策略运行租约服务。
     */
    private final StrategyRuntimeLeaseService leaseService;
    /**
     * 同 K 线开仓保护服务。
     */
    private final StrategyEntryGuardService entryGuardService;
    /**
     * 策略持仓状态服务。
     */
    private final StrategyPositionStateService stateService;
    /**
     * 交易执行应用服务。
     */
    private final TradeExecutionApplicationService executionService;
    /**
     * 保护单服务。
     */
    private final ProtectionOrderService protectionOrderService;
    /**
     * PAPER 模拟交易服务。
     */
    private final PaperTradingService paperTradingService;
    /**
     * 执行记录服务。
     */
    private final ExecutionRecordService executionRecordService;
    /**
     * 交易通知服务。
     */
    private final QuantTradeNotificationService notificationService;
    /**
     * 用于执行需要数据库原子语义的 SQL 操作。
     */
    private final StrategyRuntimeLedgerMapper runtimeLedgerMapper;
    /**
     * 当前 JVM 节点正在运行的策略实例上下文。
     */
    private final Map<Long, ActiveRuntime> activeRuntimes = new ConcurrentHashMap<>();
    /** 同一策略交易对的实例初始化与运行开关更新使用同一把锁。 */
    private final ConcurrentHashMap<Long, Object> strategySymbolLocks = new ConcurrentHashMap<>();
    /**
     * Limit repeated blocked LIVE startup diagnostics to one per strategy per minute.
     */
    private final Map<Long, Long> nextLiveStreamOwnershipLogAt = new ConcurrentHashMap<>();
    /**
     * Limit detailed stale-exposure diagnostics while the strategy is blocked.
     */
    private final Map<Long, Long> nextExposureDiagnosticLogAt = new ConcurrentHashMap<>();
    @Autowired
    private org.dromara.quant.decision.TradeDecisionLogService decisionLogs;
    @Autowired
    private StrategyPositionPerformanceHub positionPerformanceHub;

    /**
     * 返回运行时生效的保证金模式；交易对覆盖优先于策略默认值。
     */
    private static String effectiveMarginMode(ActiveRuntime runtime) {
        String mode = runtime.strategySymbol.getMarginMode();
        if (mode == null || mode.isBlank()) {
            mode = runtime.strategy.getDefaultMarginMode();
        }
        return mode == null || mode.isBlank() ? "ISOLATED" : mode.toUpperCase(java.util.Locale.ROOT);
    }

    /**
     * 计算基础价格收益率。
     */
    private static BigDecimal calculateReturnRate(String direction, BigDecimal closePrice, BigDecimal entryPrice) {
        if (closePrice == null || entryPrice == null || entryPrice.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal delta = "SHORT".equalsIgnoreCase(direction) ? entryPrice.subtract(closePrice) : closePrice.subtract(entryPrice);
        return delta.divide(entryPrice, 12, RoundingMode.HALF_UP);
    }

    /**
     * 计算包含杠杆后的策略回报率。
     */
    private static BigDecimal leveragedReturnRate(ActiveRuntime runtime, String direction, BigDecimal closePrice, BigDecimal entryPrice) {
        BigDecimal raw = calculateReturnRate(direction, closePrice, entryPrice);
        int leverage = runtime == null || runtime.strategySymbol == null || runtime.strategySymbol.getLeverage() == null ? (runtime == null || runtime.strategy == null ? 1 :
         Math.max(1, runtime.strategy.getDefaultLeverage())) : Math.max(1, runtime.strategySymbol.getLeverage());
        if (runtime != null && runtime.state != null && runtime.state.getFrozenLeverage() != null) {
            leverage = runtime.state.getFrozenLeverage();
        }
        return raw.multiply(BigDecimal.valueOf(leverage));
    }

    /**
     * 根据价格变化和数量计算盈亏金额。
     */
    private static BigDecimal calculateProfitAmount(String direction, BigDecimal closePrice, BigDecimal entryPrice, BigDecimal quantity) {
        if (closePrice == null || entryPrice == null || quantity == null) {
            return null;
        }
        BigDecimal delta = "SHORT".equalsIgnoreCase(direction) ? entryPrice.subtract(closePrice) : closePrice.subtract(entryPrice);
        return delta.multiply(quantity);
    }

    /**
     * 持仓减仓或平仓事实已确认后再入队通知；通知设施异常不能回滚或阻断交易状态恢复。
     */
    private void notifyCloseSafely(ActiveRuntime runtime, String direction, BigDecimal quantity, BigDecimal entryPrice, BigDecimal closePrice,
                                   BigDecimal profitAmount, BigDecimal profitRate, String closeType, String reason) {
        try {
            notificationService.notifyClose(runtime.strategy, runtime.credential, runtime.symbol.getSymbol(), direction, quantity, entryPrice, closePrice,
                profitAmount, profitRate, closeType, reason);
        } catch (RuntimeException ex) {
            log.error("已确认平仓通知处理失败 instanceId={} symbol={} closeType={}", runtime.instance.getId(), runtime.symbol.getSymbol(), closeType, ex);
        }
    }

    /**
     * 清理已确认完成的待执行动作。
     */
    private static void clearPending(QuantStrategyPositionState state) {
        state.setPendingAction(null);
        state.setPendingClientOrderId(null);
        state.setPendingOrderId(null);
        state.setPendingOrderTime(null);
        state.setPendingIdempotencyKey(null);
        state.setPendingNotFoundConfirmations(0);
    }

    /**
     * 生成带业务语义的交易执行幂等键。
     */
    private static String buildIdempotencyKey(ActiveRuntime runtime, String action, Instant time) {
        return "STRATEGY:" + runtime.instance.getId() + ":" + action + ":" + time.toEpochMilli();
    }

    /**
     * 读取正整数配置，非法时使用默认值。
     */
    private static int positiveInt(Object value, int fallback) {
        if (value instanceof Number number && number.intValue() > 0) {
            return number.intValue();
        }
        if (value != null) {
            try {
                int parsed = Integer.parseInt(String.valueOf(value));
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return fallback;
    }

    /**
     * 在策略启动时校验保护参数范围，阻止非法比例进入下单链路。
     */
    private static void validateProtectionRatios(QuantStrategy strategy, QuantStrategySymbol symbol) {
        for (BigDecimal ratio : Arrays.asList(strategy.getDefaultStopLossRatio(), symbol.getStopLossRatio())) {
            if (ratio != null && (ratio.signum() <= 0 || ratio.compareTo(BigDecimal.ONE) >= 0)) {
                throw new IllegalArgumentException("默认/绑定止损比例必须 > 0 且 < 1");
            }
        }
    }

    private static void freezeOrderEntryPrice(QuantStrategyPositionState state, ExchangeGateway.OrderSnapshot order) {
        freezeOrderEntryPrice(state, order, false);
    }

    private static void freezeOrderEntryPrice(QuantStrategyPositionState state, ExchangeGateway.OrderSnapshot order, boolean preserveExtrema) {
        if (order != null && order.averagePrice() != null && order.averagePrice().signum() > 0) {
            state.setEntryPrice(order.averagePrice());
            state.setHighestPriceSinceEntry(preserveExtrema && state.getHighestPriceSinceEntry() != null
                ? state.getHighestPriceSinceEntry().max(order.averagePrice()) : order.averagePrice());
            state.setLowestPriceSinceEntry(preserveExtrema && state.getLowestPriceSinceEntry() != null
                ? state.getLowestPriceSinceEntry().min(order.averagePrice()) : order.averagePrice());
        }
    }

    /**
     * 合并交易所侧完全相同的条件单目标。策略退出判断仍保留全部信号，
     * 这里只避免硬止损、策略止损和交易对止损重复创建相同的 Binance 单。
     */
    static List<ProtectionSignal> deduplicateExchangeProtectionSignals(List<ProtectionSignal> signals, QuantPosition position, BigDecimal initialQuantity,
     BigDecimal hardStopRatio, ExchangeGateway.SymbolRule rule, int leverage) {
        if (signals == null || signals.isEmpty() || position == null || position.getQuantity() == null) {
            return List.of();
        }
        Set<String> targets = new HashSet<>();
        if (hardStopRatio != null && hardStopRatio.signum() > 0 && hardStopRatio.compareTo(BigDecimal.ONE) < 0 && position.getEntryPrice() != null && position.getEntryPrice().signum() > 0) {
            BigDecimal stop = KlineSlopeRateStrategy.priceForReturn(position.getEntryPrice(), position.getPositionSide(), hardStopRatio.negate(), leverage);
            addProtectionTarget(targets, "STOP_MARKET", position.getQuantity(), stop, rule);
        }
        List<ProtectionSignal> result = new ArrayList<>();
        for (ProtectionSignal signal : signals) {
            if (signal == null) {
                continue;
            }
            if (signal.triggerPrice() == null) {
                result.add(signal);
                continue;
            }
            if (!Set.of("LOCAL_STOP", "SYMBOL_STOP").contains(signal.type())) {
                result.add(signal);
                continue;
            }
            BigDecimal base = signal.quantityBase() == ProtectionSignal.QuantityBase.INITIAL && initialQuantity != null ? initialQuantity : position.getQuantity();
            BigDecimal quantity = base.multiply(signal.quantityRate() == null ? BigDecimal.ONE : signal.quantityRate()).min(position.getQuantity());
            if (addProtectionTarget(targets, signal.orderType(), quantity, signal.triggerPrice(), rule)) {
                result.add(signal);
            }
        }
        return List.copyOf(result);
    }

    private static boolean addProtectionTarget(Set<String> targets, String orderType, BigDecimal quantity, BigDecimal triggerPrice, ExchangeGateway.SymbolRule rule) {
        if (quantity == null || quantity.signum() <= 0 || triggerPrice == null || triggerPrice.signum() <= 0) {
            return true;
        }
        if (rule != null) {
            quantity = org.dromara.quant.exchange.QuantityNormalizer.quantity(quantity, rule, "MARKET");
            triggerPrice = org.dromara.quant.exchange.QuantityNormalizer.price(triggerPrice, rule);
        }
        String key =
         (orderType == null ? "STOP_MARKET" : orderType.toUpperCase(Locale.ROOT)) + ":" + quantity.stripTrailingZeros().toPlainString() + ":" + triggerPrice.stripTrailingZeros().toPlainString();
        return targets.add(key);
    }

    /**
     * 确保指定策略实例拥有有效运行上下文和租约。
     */

    /**
     * 更新指定策略交易对的独立运行开关。等待当前开仓决策/派单临界区结束后再返回。
     */
    public void setStrategySymbolRuntimeEnabled(Long strategySymbolId, boolean enabled) {
        if (strategySymbolId == null) {
            throw new org.dromara.common.core.exception.ServiceException("策略交易对绑定标识不能为空");
        }
        // 分布式锁让暂停接口等待其他节点正在执行的开仓决策/派单结束后再返回。
        RLock distributedLock = redissonClient.getLock(strategySymbolRuntimeLockKey(strategySymbolId));
        distributedLock.lock();
        try {
            QuantStrategySymbol binding = strategySymbolMapper.selectById(strategySymbolId);
            if (binding == null) {
                throw new org.dromara.common.core.exception.ServiceException("策略交易对绑定不存在");
            }
            boolean stateChanged = !Objects.equals(binding.getRuntimeEnabled(), enabled);
            binding.setRuntimeEnabled(enabled);
            // 相同状态的重复请求直接视为成功，避免受 JDBC affected-rows 配置影响。
            if (stateChanged && strategySymbolMapper.updateById(binding) != 1) {
                throw new org.dromara.common.core.exception.ServiceException("保存策略交易对运行状态失败");
            }
            activeRuntimes.values().stream()
                .filter(runtime -> runtime.strategySymbol != null && strategySymbolId.equals(runtime.strategySymbol.getId()))
                .forEach(runtime -> {
                    synchronized (runtime.entryDispatchLock) {
                        runtime.runtimeEnabled = enabled;
                        runtime.strategySymbol.setRuntimeEnabled(enabled);
                    }
                });
        } finally {
            if (distributedLock.isHeldByCurrentThread()) distributedLock.unlock();
        }
    }

    private static String strategySymbolRuntimeLockKey(Long strategySymbolId) {
        return "quant:strategy-symbol-runtime:" + strategySymbolId;
    }

    /**
     * 按配置周期发现/停止实例，并同步一次真实持仓；实时价格不在这里轮询。
     */
    public void supervise() {
        log.debug("策略运行时监督任务开始 runtimeEnabled={}", properties.getRuntime().isEnabled());
        if (!properties.getRuntime().isEnabled()) {
            stopAll();
            return;
        }
        List<QuantStrategyInstance> instances = instanceMapper.selectList(new LambdaQueryWrapper<QuantStrategyInstance>().eq(QuantStrategyInstance::getStatus, "RUNNING"));
        // 停止开仓不等于停止已有持仓的保护，重启后仍恢复未平仓实例。
        Set<Long> discovered = new HashSet<>();
        instances.forEach(instance -> discovered.add(instance.getId()));
        for (QuantStrategyPositionState state : stateService.findUnclosed()) {
            if (discovered.add(state.getInstanceId())) {
                QuantStrategyInstance instance = instanceMapper.selectById(state.getInstanceId());
                if (instance != null && "STOPPED".equals(instance.getStatus())) {
                    instances.add(instance);
                }
            }
        }
        log.debug("策略运行时监督任务发现运行/持仓保护实例数量={}", instances.size());
        Set<Long> activeIds = new HashSet<>();
        for (QuantStrategyInstance instance : instances) {
            try {
                if (ensureRuntime(instance)) {
                    activeIds.add(instance.getId());
                }
            } catch (RuntimeException ex) {
                recordError(instance, ex);
            }
        }
        activeRuntimes.keySet().removeIf(id -> {
            if (!activeIds.contains(id)) {
                ActiveRuntime runtime = activeRuntimes.remove(id);
                closeRuntime(runtime);
                return true;
            }
            return false;
        });
    }

    /**
     * 按配置间隔恢复运行实例的持仓事实；同一凭证的实例共享账户级快照。
     */
    public void reconcileRunningPositions() {
        activeRuntimes.values().forEach(this::requestReconciliation);
    }

    /**
     * Renew strategy leases independently of market-event delivery. Any failed or
     * ambiguous renewal immediately fences this JVM's event-driven decisions;
     * final order dispatch still performs its own database ownership check.
     */
    public void renewActiveRuntimeLeases() {
        for (ActiveRuntime runtime : activeRuntimes.values()) {
            if (runtime == null || runtime.closed || "PAPER".equalsIgnoreCase(runtime.credential.getRunMode())
                || !runtime.leaseRenewing.compareAndSet(false, true)) {
                continue;
            }
            // Fail closed for the duration of the DB renewal. If the query hangs or the
            // executor stalls past the lease deadline, event decisions remain fenced.
            runtime.leaseValid = false;
            try {
                leaseRenewalExecutor.execute(() -> {
                    try {
                        boolean renewed;
                        runtime.leaseOperationLock.lock();
                        try {
                            // closeRuntime owns the final release. A renewal task that starts
                            // after shutdown begins must not reacquire the lease behind it.
                            renewed = !runtime.closed && leaseService.acquireOrRenew(
                                runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol());
                            synchronized (runtime) {
                                runtime.leaseValid = renewed && !runtime.closed;
                            }
                        } finally {
                            runtime.leaseOperationLock.unlock();
                        }
                        if (!renewed && !runtime.closed) {
                            log.error("策略运行租约独立续期失败，已阻止行情驱动决策 instanceId={} credentialId={} symbol={}",
                                runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol());
                        }
                    } catch (RuntimeException ex) {
                        // A database/coordination exception is an ambiguous lease result.
                        // Keep the runtime fenced and log the failure instead of silently
                        // losing the renewal task's exception on its virtual thread.
                        runtime.leaseValid = false;
                        log.error("策略运行租约续期异常，已阻止行情驱动决策 instanceId={} credentialId={} symbol={}",
                            runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(), ex);
                    } finally {
                        runtime.leaseRenewing.set(false);
                    }
                });
            } catch (RuntimeException ex) {
                runtime.leaseRenewing.set(false);
                runtime.leaseValid = false;
                log.error("策略运行租约续期任务无法提交，已阻止行情驱动决策 instanceId={}", runtime.instance.getId(), ex);
            }
        }
    }

    /** Invalidate cached position facts atomically with the in-flight reconciliation fence. */
    private void invalidatePositionFacts(ActiveRuntime runtime) {
        synchronized (runtime) {
            runtime.positionFactsInvalidationGeneration.incrementAndGet();
            runtime.positionFactsReady = false;
        }
    }

    /**
     * 将账户事实刷新请求合并排队，避免每条行情事件都发起 REST 查询。
     */
    private void requestReconciliation(ActiveRuntime runtime) {
        synchronized (runtime) {
            if (runtime.closed || System.currentTimeMillis() < runtime.nextReconcileAt) {
                return;
            }
            runtime.reconcileRequested.set(true);
            if (runtime.exitCoordinating.get() || !runtime.reconciling.compareAndSet(false, true)) {
                return;
            }
        }
        try {
            recoveryExecutor.execute(() -> {
                try {
                    do {
                        runtime.reconcileRequested.set(false);
                        reconcilePosition(runtime);
                    } while (!runtime.closed && runtime.reconcileRequested.get() && System.currentTimeMillis() >= runtime.nextReconcileAt);
                } finally {
                    runtime.reconciling.set(false);
                    if (!runtime.closed && runtime.reconcileRequested.get()) {
                        requestReconciliation(runtime);
                    }
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            // 提交任务失败时必须释放 in-flight 标志；否则该 Runtime 会永久表现为正在对账。
            // Executor 已关闭时不在这里递归重试，保留请求标志供存活期监督/事件恢复。
            runtime.reconciling.set(false);
            synchronized (runtime) {
                if (!runtime.closed) {
                    runtime.reconcileRequested.set(true);
                    runtime.nextReconcileAt = Math.max(runtime.nextReconcileAt, System.currentTimeMillis() + 5_000L);
                }
            }
            log.warn("策略对账任务未能提交，已释放对账占用标志 instanceId={} credentialId={} symbol={} executorShutdown={}",
                runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(), recoveryExecutor.isShutdown());
        }
    }

    /**
     * Schedule a real retry rather than only storing a throttle timestamp. Otherwise a failed
     * lease check or stale snapshot can leave entry facts unverified until an unrelated event
     * or the next full reconciliation interval.
     */
    private void scheduleReconciliationRetry(ActiveRuntime runtime) {
        long now = System.currentTimeMillis();
        long retryAt;
        synchronized (runtime) {
            if (runtime.closed || runtime.exitCoordinating.get()) {
                return;
            }
            if (runtime.nextReconcileAt <= now) {
                runtime.nextReconcileAt = now + 5_000L;
            }
            retryAt = runtime.nextReconcileAt;
        }
        // Add a small margin so the callback cannot run before requestReconciliation's throttle expires.
        long delayMillis = Math.max(1L, retryAt - now) + 25L;
        log.debug("策略持仓核验/恢复尚未解除开仓阻断，安排自动重试 instanceId={} credentialId={} symbol={} runMode={} reason={} retryAfterMillis={}",
            runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(), runtime.credential.getRunMode(),
            runtime.positionReconciliationBlockReason, delayMillis);
        try {
            // 定时等待使用默认延迟调度器；到期后再由 requestReconciliation 提交到 recoveryExecutor。
            // 若直接把 recoveryExecutor 作为 delayedExecutor 的目标，目标执行器在延迟期间关闭时，
            // 拒绝发生在异步触发阶段，外层 try/catch 捕获不到。
            java.util.concurrent.CompletableFuture.delayedExecutor(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
                .execute(() -> {
                    if (!runtime.closed) {
                        requestReconciliation(runtime);
                    }
                });
        } catch (java.util.concurrent.RejectedExecutionException ex) {
            // 关闭阶段不再安排新任务；Runtime 仍保持事实未核实，不能因调度失败而放开开仓。
            log.debug("策略对账延迟重试未能提交，通常表示运行时正在关闭 instanceId={} symbol={} executorShutdown={}",
                runtime.instance.getId(), runtime.symbol.getSymbol(), recoveryExecutor.isShutdown());
        }
    }

    /**
     * 仅在持仓事实完成核验后同步策略状态；失败时保留暂停决策状态。
     */
    private void reconcilePosition(ActiveRuntime runtime) {
        boolean reconciliationCompleted = false;
        long reconcileStartedAt = System.nanoTime();
        long positionFactsInvalidationGeneration = runtime.positionFactsInvalidationGeneration.get();
        synchronized (runtime) {
            if (runtime.closed || runtime.exitCoordinating.get()) {
                return;
            }
            runtime.positionReconciling = true;
            runtime.positionReconciliationBlockReason = "IN_PROGRESS";
        }
        try {
            if (reconcilePendingExecution(runtime)) {
                synchronized (runtime) {
                    runtime.positionReconciliationBlockReason = "PENDING_EXECUTION";
                    if (!runtime.closed && runtime.position != null) {
                        ensureHardStop(runtime);
                    }
                    // UNKNOWN 长时间未决时逐步拉长只读核查间隔，降低 REST 限流压力；
                    // 最长 30 秒仍持续核查，期间继续保留开仓阻断和已有持仓保护。
                    if (!runtime.closed) {
                        int confirmations = runtime.state == null || runtime.state.getPendingNotFoundConfirmations() == null
                            ? 0 : runtime.state.getPendingNotFoundConfirmations();
                        runtime.nextReconcileAt = System.currentTimeMillis() + pendingExecutionRetryDelayMillis(confirmations);
                    }
                }
                return;
            }
            if (retryProtectionCleanupIfNeeded(runtime)) {
                runtime.positionReconciliationBlockReason = "PROTECTION_CLEANUP_PENDING";
                return;
            }
            QuantStrategyPositionState expectedState;
            boolean expectedPending;
            List<Object> expectedPendingIdentity;
            QuantPosition expectedPosition;
            BigDecimal expectedQuantity;
            synchronized (runtime) {
                if (runtime.closed) {
                    return;
                }
                if (runtime.hasPendingAction()) {
                    runtime.positionReconciliationBlockReason = "PENDING_ACTION";
                    runtime.nextReconcileAt = System.currentTimeMillis() + 5_000L;
                    runtime.reconcileRequested.set(true);
                    return;
                }
                expectedState = runtime.state;
                expectedPending = runtime.hasPendingAction();
                expectedPendingIdentity = expectedState == null ? List.of() : pendingIdentity(expectedState);
                expectedPosition = runtime.position;
                expectedQuantity = expectedPosition == null ? null : expectedPosition.getQuantity();
            }
            if (!leaseService.acquireOrRenew(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol())) {
                runtime.positionReconciliationBlockReason = "STRATEGY_LEASE_NOT_HELD";
                runtime.nextReconcileAt = System.currentTimeMillis() + 5_000L;
                log.warn("策略持仓核验因租约未确认而延期 instanceId={} credentialId={} symbol={} retryAfterSeconds=5",
                    runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol());
                return;
            }
            // 在REST前捕获数据库基线；慢查询或并发平仓后只能条件更新，不能覆盖新账本。
            List<QuantPosition> baseline =
        positionMapper.selectList(new LambdaQueryWrapper<QuantPosition>().eq(QuantPosition::getCredentialId, runtime.credential.getId()).eq(QuantPosition::getSymbol,
            runtime.symbol.getSymbol())).stream().map(position -> {
                QuantPosition copy = new QuantPosition();
                BeanUtils.copyProperties(position, copy);
                return copy;
            }).toList();
            List<ExchangeGateway.PositionSnapshot> snapshots = "PAPER".equalsIgnoreCase(runtime.credential.getRunMode()) ? null :
                reconciliationService.runtimePositionSnapshot(runtime.credential).stream()
                    .filter(snapshot -> snapshot != null && runtime.symbol.getSymbol().equalsIgnoreCase(snapshot.symbol())).toList();
            synchronized (runtime) {
                if (runtime.closed) {
                    return;
                }
                if (runtime.exitCoordinating.get() || runtime.state != expectedState || runtime.hasPendingAction() != expectedPending || !Objects.equals(expectedPendingIdentity, runtime.state == null ?
List.of() : pendingIdentity(runtime.state)) || runtime.position != expectedPosition || runtime.position != null && !Objects.equals(runtime.position.getQuantity(), expectedQuantity)) {
                    runtime.positionReconciliationBlockReason = "RUNTIME_STATE_CHANGED_DURING_SNAPSHOT";
                    runtime.reconcileRequested.set(true);
                    return;
                }
            }
            QuantStrategyPositionState latestState = stateService.findByInstance(runtime.instance.getId());
            if (hasStateVersionChanged(expectedState, latestState)) {
                synchronized (runtime) {
                    if (!runtime.closed) {
                        if (runtime.state == expectedState) {
                            replaceRuntimeStatePreservingExtrema(runtime, latestState);
                        }
                        runtime.stateRefreshRequired = true;
                        runtime.positionReconciliationBlockReason = "STATE_CHANGED_DURING_RECONCILIATION";
                        // 持久化状态在本次核验期间发生变化；退避后重新读取，避免连续发起快照请求。
                        runtime.nextReconcileAt = System.currentTimeMillis() + 5_000L;
                        runtime.reconcileRequested.set(true);
                    }
                }
                return;
            }
            QuantPosition refreshed = loadReconciledPosition(runtime, baseline, snapshots);
            BigDecimal firstExecuted = null;
            if (refreshed != null && expectedState != null && expectedState.getPositionCycleKey() != null && !Boolean.TRUE.equals(expectedState.getFirstTakeProfitTriggered()) && !"PAPER".equalsIgnoreCase(runtime.credential.getRunMode())) {
                refreshed.setProtectionCycleKey(expectedState.getPositionCycleKey());
                firstExecuted = protectionOrderService.firstTakeProfitExecutedQuantity(runtime.credential.getId(), refreshed);
            }
            if (firstExecuted != null && expectedState.getInitialPositionQty() != null && refreshed.getQuantity().compareTo(expectedState.getInitialPositionQty().subtract(firstExecuted).subtract(expectedState.getFirstTakeProfitLocalExecutedQty() == null ? BigDecimal.ZERO : expectedState.getFirstTakeProfitLocalExecutedQty()).max(BigDecimal.ZERO)) > 0) {
                throw new ProtectionOrderService.ProtectionExecutionPendingException();
            }
            boolean firstTpFilled =
             firstExecuted != null && expectedState.getFirstTakeProfitTargetQty() != null && firstExecuted.add(expectedState.getFirstTakeProfitLocalExecutedQty() == null ?
            BigDecimal.ZERO : expectedState.getFirstTakeProfitLocalExecutedQty()).compareTo(expectedState.getFirstTakeProfitTargetQty()) >= 0;
            boolean leaseHeld = leaseService.acquireOrRenew(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol());
            synchronized (runtime) {
                if (runtime.exitCoordinating.get() || runtime.state != expectedState || runtime.hasPendingAction() != expectedPending || !Objects.equals(expectedPendingIdentity, runtime.state == null ?
List.of() : pendingIdentity(runtime.state)) || runtime.position != expectedPosition || runtime.position != null && !Objects.equals(runtime.position.getQuantity(), expectedQuantity)) {
                    runtime.positionReconciliationBlockReason = "RUNTIME_STATE_CHANGED_DURING_SNAPSHOT";
                    runtime.reconcileRequested.set(true);
                    return;
                }
                if (runtime.closed) {
                    return;
                }
                if (!leaseHeld) {
                    runtime.positionReconciliationBlockReason = "STRATEGY_LEASE_LOST_AFTER_SNAPSHOT";
                    runtime.nextReconcileAt = System.currentTimeMillis() + 5_000L;
                    log.warn("策略持仓核验快照完成后租约未确认，保持开仓阻断 instanceId={} credentialId={} symbol={} retryAfterSeconds=5",
                        runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol());
                    return;
                }
                QuantPosition previousPosition = runtime.position;
                runtime.position = refreshed;
                if (runtime.position != null) {
                    // 交易所确认当前仍有持仓时，不能继续沿用“空仓后的保护单清理”状态。
                    runtime.protectionCleanupPending = false;
                    if (previousPosition != null && previousPosition.getQuantity() != null && runtime.position.getQuantity() != null && runtime.position.getQuantity().compareTo(previousPosition.getQuantity()) < 0) {
                        BigDecimal closedQty = previousPosition.getQuantity().subtract(runtime.position.getQuantity());
                        recordPositionDecrease(runtime, previousPosition, closedQty, firstTpFilled ? "交易所首次止盈保护已确认减仓" : "交易所保护单或外部操作导致部分减仓，具体触发原因待确认");
                        BigDecimal closePrice = runtime.currentPrice() != null ? runtime.currentPrice() : runtime.position.getMarkPrice();
                        BigDecimal profitRate = leveragedReturnRate(runtime, previousPosition.getPositionSide(), closePrice, previousPosition.getEntryPrice());
                        BigDecimal profitAmount = calculateProfitAmount(previousPosition.getPositionSide(), closePrice, previousPosition.getEntryPrice(), closedQty);
                        notifyCloseSafely(runtime, previousPosition.getPositionSide(), closedQty, previousPosition.getEntryPrice(), closePrice, profitAmount, profitRate,
                            "EXCHANGE_PARTIAL_CLOSE", "交易所保护单/外部操作导致部分减仓，经 REST 对账确认");
                    }
                    if (firstExecuted != null) {
                        PositionProtectionState.confirmExchangeFirstProfit(runtime.state, firstExecuted);
                    }
                    syncRuntimeStateWithPosition(runtime, firstTpFilled);
                } else if (runtime.state != null && !runtime.hasPendingAction() && (!"CLOSED".equals(runtime.state.getStatus())
                    || Boolean.TRUE.equals(runtime.state.getRecoveryRequired()) || Boolean.TRUE.equals(runtime.state.getUnsafeRecovery()))) {
                    // FIXED: 交易所已确认空仓。无论保护单清理成功与否，state 都必须归零，
                    // 否则残留的 status=OPEN/EXIT_PENDING + position_qty>0 会永远占用策略最大持仓数名额，
                    // 尤其在 LIVE 模式（真实挂过 STOP/TP 条件单，撤销可能失败）反复出现。
                    // 清理失败时改用 protectionCleanupPending 独立阻断开新仓，不再锁住 state。
                    // 先持久化本次确认空仓所在 K 线的封锁，再清理保护单或释放恢复状态。
                    // 全账户快照可能已把状态标记为待封锁，而运行时本地尚无旧持仓对象。
                    blockCloseCandle(runtime, runtime.lastEvaluatedKlineOpenTime);
                    boolean flatPositionConfirmed = "PAPER".equalsIgnoreCase(runtime.credential.getRunMode())
                        || snapshots != null && snapshots.stream().allMatch(snapshot -> snapshot != null
                            && runtime.symbol.getSymbol().equalsIgnoreCase(snapshot.symbol())
                            && snapshot.quantity() != null && snapshot.quantity().signum() == 0);
                    if (!flatPositionConfirmed) {
                        runtime.protectionCleanupPending = true;
                        runtime.positionReconciliationBlockReason = "EXCHANGE_FLAT_SNAPSHOT_INCOMPLETE";
                        runtime.nextReconcileAt = System.currentTimeMillis() + 5_000L;
                        log.warn("交易所空仓快照不完整，保留策略恢复状态并等待重试 instanceId={} symbol={} state={}",
                            runtime.instance.getId(), runtime.symbol.getSymbol(), runtime.state.getStatus());
                        return;
                    }
                    if (previousPosition != null) {
                        recordPositionDecrease(runtime, previousPosition, previousPosition.getQuantity(), "交易所保护单或外部操作导致持仓关闭，具体触发原因待确认");
                        BigDecimal closePrice = runtime.currentPrice() != null ? runtime.currentPrice() : previousPosition.getMarkPrice();
                        BigDecimal profitRate = leveragedReturnRate(runtime, previousPosition.getPositionSide(), closePrice, previousPosition.getEntryPrice());
                        BigDecimal profitAmount = calculateProfitAmount(previousPosition.getPositionSide(), closePrice, previousPosition.getEntryPrice(),
                         previousPosition.getQuantity());
                        notifyCloseSafely(runtime, previousPosition.getPositionSide(), previousPosition.getQuantity(), previousPosition.getEntryPrice(), closePrice,
                            profitAmount, profitRate, "EXCHANGE_PROTECTION_OR_EXTERNAL", "交易所保护单/外部操作导致持仓关闭，经 REST 对账确认");
                    } else if (runtime.state.getPositionQty() != null && runtime.state.getPositionQty().signum() > 0
                        && runtime.state.getEntryPrice() != null && runtime.state.getDirection() != null) {
                        BigDecimal closePrice = runtime.currentPrice() != null ? runtime.currentPrice() : runtime.state.getEntryPrice();
                        BigDecimal qty = runtime.state.getPositionQty();
                        BigDecimal profitRate = leveragedReturnRate(runtime, runtime.state.getDirection(), closePrice, runtime.state.getEntryPrice());
                        BigDecimal profitAmount = calculateProfitAmount(runtime.state.getDirection(), closePrice, runtime.state.getEntryPrice(), qty);
                        notifyCloseSafely(runtime, runtime.state.getDirection(), qty, runtime.state.getEntryPrice(), closePrice, profitAmount, profitRate,
                            "EXCHANGE_PROTECTION_OR_EXTERNAL", "策略恢复时经账户快照确认持仓已关闭");
                    }
                    boolean protectionCleaned = cancelStaleProtection(runtime);
                    // 无条件将 state 归零：status=CLOSED + position_qty=0，从"最大持仓数"统计中排除。
                    QuantStrategyPositionState closedState = runtime.state;
                    stateService.markClosed(closedState);
                    // 正常情况下可从持久层重载关闭状态；若并发删除/测试替身返回 null，保留已标记关闭的对象，
                    // 避免随后设置保护清理阻断时发生 NPE，并确保本地运行时仍有明确的恢复状态。
                    QuantStrategyPositionState reloadedClosedState = stateService.findByInstance(runtime.instance.getId());
                    runtime.state = reloadedClosedState != null ? reloadedClosedState : closedState;
                    // 清理成功必须同时解除运行时阻断；失败则保留重试标志，避免遗留保护单误触发。
                    runtime.protectionCleanupPending = !protectionCleaned;
                    if (!protectionCleaned) {
                        // markClosed 会清除恢复位；保护单未清理时必须重新持久化阻断，确保重启后能继续恢复。
                        runtime.state.setRecoveryRequired(true);
                        runtime.state.setUnsafeRecovery(true);
                        stateService.save(runtime.state);
                        log.warn("交易所持仓快照已确认空仓，但保护单终态尚未确认；已将策略持仓归零并继续阻断开仓 instanceId={} positionId={} symbol={}",
                            runtime.instance.getId(), runtime.state.getPositionId(), runtime.symbol.getSymbol());
                    } else {
                        // 已确认空仓且本持仓保护已确认清理，旧持仓期间的保护降级不应永久阻断新周期。
                        runtime.protectionDegraded = false;
                    }
                }
                if (runtime.position != null && runtime.state == null) {
                    runtime.state = recoverState(runtime);
                }
                if (runtime.position != null && runtime.state != null) {
                    ensureHardStop(runtime);
                    BigDecimal current = runtime.currentPrice() != null ? runtime.currentPrice() : runtime.position.getMarkPrice();
                    if (current != null && current.signum() > 0) {
                        syncPluginProtections(runtime, buildContext(runtime, current, new ExchangeGateway.MarketEvent("RECONCILE", runtime.symbol.getSymbol(),
 runtime.strategySymbol.getIntervalCode(), current, null, Instant.now())));
                    }
                }
            }
            boolean recoveryStillRequired = runtime.state != null
                && (Boolean.TRUE.equals(runtime.state.getRecoveryRequired())
                    || Boolean.TRUE.equals(runtime.state.getUnsafeRecovery())
                    || "PROTECTION_CLEANUP".equals(runtime.state.getStatus())
                    || runtime.protectionCleanupPending);
            runtime.nextReconcileAt = recoveryStillRequired ? System.currentTimeMillis() + 5_000L : 0L;
            if (runtime.instance.getLastError() != null && runtime.instance.getLastError().startsWith("Binance REST 请求排队")) {
                runtime.instance.setLastError("");
                instanceMapper.updateById(runtime.instance);
            }
            runtime.positionReconciliationBlockReason = recoveryStillRequired ? "RECOVERY_OR_PROTECTION_STILL_REQUIRED" : "COMPLETE";
            reconciliationCompleted = true;
        } catch (TradingReconciliationService.SnapshotSupersededException ex) {
            runtime.positionReconciliationBlockReason = "SNAPSHOT_SUPERSEDED";
            runtime.nextReconcileAt = System.currentTimeMillis() + 5_000L;
            log.debug("较早发起的交易对持仓快照已被新快照取代，等待后续对账 instanceId={} symbol={}", runtime.instance.getId(), runtime.symbol.getSymbol());
        } catch (ProtectionOrderService.ProtectionExecutionPendingException ex) {
            // Binance 已报告保护单正在触发或成交尚未完成；这是等待成交/持仓事实收敛的暂态，不记作运行错误。
            runtime.positionReconciliationBlockReason = "PROTECTION_EXECUTION_PENDING";
            runtime.nextReconcileAt = System.currentTimeMillis() + 5_000L;
            log.debug("保护单执行事实尚未收敛，保持决策阻断并等待重核 instanceId={} symbol={} runMode={} retryAfterSeconds=5", runtime.instance.getId(),
                runtime.symbol.getSymbol(), runtime.credential.getRunMode());
        } catch (RuntimeException ex) {
            runtime.positionReconciliationBlockReason = "RECONCILIATION_ERROR:" + ex.getClass().getSimpleName();
            boolean stateConflict = containsMessage(ex, "策略持仓状态乐观锁冲突");
            log.warn("策略持仓事实核实失败 instanceId={} credentialId={} symbol={} runMode={} elapsedMs={} error={}", runtime.instance.getId(), runtime.credential.getId(),
                runtime.symbol.getSymbol(), runtime.credential.getRunMode(), java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - reconcileStartedAt), ex.getMessage());
            if (stateConflict) {
                QuantStrategyPositionState latestState = stateService.findByInstance(runtime.instance.getId());
                synchronized (runtime) {
                    if (!runtime.closed) {
                        if (latestState != null) {
                            replaceRuntimeStatePreservingExtrema(runtime, latestState);
                        } else if (runtime.state != null) {
                            // 状态行缺失时不能把本地旧状态当作空仓事实；留存阻断并等待人工/后续恢复。
                            runtime.state.setRecoveryRequired(true);
                            runtime.state.setUnsafeRecovery(true);
                        }
                        // 先刷新持久化状态，再退避重试，避免乐观锁冲突时在同一工作线程内热循环。
                        runtime.nextReconcileAt = System.currentTimeMillis() + 5_000L;
                        runtime.reconcileRequested.set(true);
                    }
                }
            }
            if (!stateConflict) {
                long exchangeRetryDelay = exchangeRetryAfterMillis(ex);
                runtime.nextReconcileAt = System.currentTimeMillis() + Math.max(5_000L, exchangeRetryDelay);
            }
            recordError(runtime.instance, ex);
        } finally {
            synchronized (runtime) {
                boolean positionFactsStillCurrent = positionFactsInvalidationGeneration == runtime.positionFactsInvalidationGeneration.get();
                if (reconciliationCompleted && !positionFactsStillCurrent) {
                    reconciliationCompleted = false;
                    runtime.positionReconciliationBlockReason = "POSITION_FACTS_INVALIDATED_DURING_RECONCILIATION";
                }
                runtime.positionReconciling = false;
                if (reconciliationCompleted) {
                    runtime.stateRefreshRequired = false;
                    runtime.positionFactsReady = true;
                    runtime.positionFactsVerifiedAt = System.currentTimeMillis();
                    String entryReadiness;
                    if (runtime.position != null) {
                        entryReadiness = "持仓仍存在，继续持仓管理";
                    } else if (!"RUNNING".equals(runtime.instance.getStatus())) {
                        entryReadiness = "实例未运行，禁止开仓";
                    } else if (runtime.hasPendingAction() || runtime.state != null && (Boolean.TRUE.equals(runtime.state.getRecoveryRequired())
                        || Boolean.TRUE.equals(runtime.state.getUnsafeRecovery()) || !"CLOSED".equals(runtime.state.getStatus()))
                        || runtime.protectionCleanupPending || runtime.protectionDegraded) {
                        entryReadiness = "恢复或保护阻断仍在，禁止开仓";
                    } else {
                        entryReadiness = "恢复阻断已解除，等待新鲜行情进入开仓评估（不代表信号通过或已下单）";
                    }
                    log.debug("持仓核实完成 instanceId={} credentialId={} symbol={} runMode={} position={} elapsedMs={} entryReadiness={}",
                        runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(), runtime.credential.getRunMode(),
                        runtime.position == null ? "FLAT" : runtime.position.getQuantity(), java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - reconcileStartedAt), entryReadiness);
                }
            }
            if (reconciliationCompleted) {
                schedulePositionFactsRefresh(runtime, runtime.positionFactsVerifiedAt);
            }
            if (!reconciliationCompleted || "RECOVERY_OR_PROTECTION_STILL_REQUIRED".equals(runtime.positionReconciliationBlockReason)) {
                scheduleReconciliationRetry(runtime);
            }
        }
    }

    /**
     * 在快照有效期结束前主动核验，避免策略先进入过期阻断再等待 REST 刷新。
     * 同一 Runtime 的旧定时任务通过 verifiedAt 比较自动失效。
     */
    private void schedulePositionFactsRefresh(ActiveRuntime runtime, long verifiedAt) {
        long maxAgeMillis = Math.max(1, properties.getReconcile().getIntervalSeconds()) * 1_000L + 15_000L;
        long refreshAfterMillis = Math.max(1_000L, maxAgeMillis - Math.min(10_000L, maxAgeMillis / 4));
        java.util.concurrent.CompletableFuture.delayedExecutor(refreshAfterMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            .execute(() -> {
                if (!runtime.closed && runtime.positionFactsReady && runtime.positionFactsVerifiedAt == verifiedAt) {
                    log.debug("策略账户持仓事实即将过期，提前触发核验 instanceId={} credentialId={} symbol={}",
                        runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol());
                    requestReconciliation(runtime);
                }
            });
    }

    /**
     * 确保指定策略实例拥有有效运行上下文和租约。
     * FIXED: 策略/凭证/交易对被停用但存在未平仓持仓时，进入保护模式而不是完全停止。
     */
    private boolean ensureRuntime(QuantStrategyInstance instance) {
        Long strategySymbolId = instance.getStrategySymbolId();
        if (strategySymbolId == null) return ensureRuntimeLocked(instance);
        Object symbolLock = strategySymbolLocks.computeIfAbsent(strategySymbolId, ignored -> new Object());
        synchronized (symbolLock) {
            return ensureRuntimeLocked(instance);
        }
    }

    private boolean ensureRuntimeLocked(QuantStrategyInstance instance) {
        QuantStrategy strategy = strategyMapper.selectById(instance.getStrategyId());
        QuantStrategySymbol strategySymbol = strategySymbolMapper.selectById(instance.getStrategySymbolId());
        QuantCredential credential = credentialMapper.selectById(instance.getCredentialId());
        if (strategy == null || strategySymbol == null || credential == null) {
            stopInactiveRuntime(instance.getId());
            return false;
        }
        QuantSymbol symbol = symbolMapper.selectById(strategySymbol.getSymbolId());
        if (symbol == null) {
            stopInactiveRuntime(instance.getId());
            return false;
        }

        QuantStrategyPositionState existingState = stateService.findByInstance(instance.getId());
        boolean hasOpenPosition =
                            existingState != null && !"CLOSED".equals(existingState.getStatus()) && ((existingState.getPositionQty() != null && existingState.getPositionQty().signum() > 0) || existingState.getPendingAction() != null || "PROTECTION_CLEANUP".equals(existingState.getStatus()));

        boolean tradingAllowed =
         "1".equals(strategy.getStatus()) && "0".equals(strategySymbol.getStatus()) && !Boolean.FALSE.equals(strategySymbol.getRuntimeEnabled()) && "0".equals(credential.getStatus()) && "0".equals(symbol.getStatus()) && (!"PAPER".equalsIgnoreCase(credential.getRunMode()) || properties.getPaper().isEnabled());

        if (!hasOpenPosition && !tradingAllowed) {
            stopInactiveRuntime(instance.getId());
            return false;
        }
        boolean protectionOnly = !tradingAllowed && hasOpenPosition;

        // The account-level private-stream lease is established independently by the LIVE
        // supervisor. A strategy on a non-owner node must not start or hold a position lease.
        // During startup the owner may not exist yet; retry at the next supervision tick.
        if ("LIVE".equalsIgnoreCase(credential.getRunMode()) && !userDataStreamHub.hasLiveStreamOwnership(credential.getId())) {
            long now = System.currentTimeMillis();
            Long nextLogAt = nextLiveStreamOwnershipLogAt.get(instance.getId());
            if (nextLogAt == null || now >= nextLogAt) {
                nextLiveStreamOwnershipLogAt.put(instance.getId(), now + 60_000L);
                log.warn("LIVE策略尚未建立本节点私有数据流租约，暂不启动行情订阅 instanceId={} credentialId={} symbol={}：请检查LivePositionStreamTriggers租约与私有WS连接", instance.getId(), credential.getId(),
                 symbol.getSymbol());
            }
            return false;
        }
        nextLiveStreamOwnershipLogAt.remove(instance.getId());

        ActiveRuntime existingRuntime = activeRuntimes.get(instance.getId());
        boolean runtimeAlreadyActive = existingRuntime != null && existingRuntime.matches(strategy.getPluginCode(), symbol.getSymbol(), strategySymbol.getIntervalCode(), credential.getId());
        // Check only before creating a runtime. Existing runtimes must remain alive to recover pending exchange actions.
        if (!runtimeAlreadyActive && !"PAPER".equalsIgnoreCase(credential.getRunMode())
            && !protectionOrderService.recoverDispatchBeforeStrategyStart(credential.getId(), symbol.getSymbol())) {
            return false;
        }

        if (!leaseService.acquireOrRenew(instance.getId(), credential.getId(), symbol.getSymbol())) {
            // 无法确认物理仓位独占权时保守停止本地运行时，避免两个实例同时操作同一交易所仓位。
            log.warn("策略运行时获取租约失败 instanceId={} credentialId={} symbol={}", instance.getId(), credential.getId(), symbol.getSymbol());
            stopInactiveRuntime(instance.getId());
            return false;
        }

        ActiveRuntime current = activeRuntimes.get(instance.getId());
        if (current != null && current.matches(strategy.getPluginCode(), symbol.getSymbol(), strategySymbol.getIntervalCode(), credential.getId())) {
            synchronized (current) {
                Map<String, Object> refreshed = mergedParams(strategy, strategySymbol);
                current.plugin.validateConfig(refreshed);
                validateProtectionRatios(strategy, strategySymbol);
                current.instance = instance;
                current.strategy = strategy;
                current.strategySymbol = strategySymbol;
                current.runtimeEnabled = !Boolean.FALSE.equals(strategySymbol.getRuntimeEnabled());
                current.credential = credential;
                current.params = refreshed;
                current.protectionOnly = protectionOnly;
                    current.leaseValid = true;
            }
            prepareTrading(current);
            resumeRecoveryIfNeeded(current);
            return true;
        }
        if (current != null) {
            closeRuntime(current);
            activeRuntimes.remove(instance.getId(), current);
            if (!leaseService.acquireOrRenew(instance.getId(), credential.getId(), symbol.getSymbol())) {
                return false;
            }
        }

        StrategyPlugin plugin = pluginRegistry.require(strategy.getPluginCode());
        Map<String, Object> params = mergedParams(strategy, strategySymbol);
        plugin.validateConfig(params);
        validateProtectionRatios(strategy, strategySymbol);
        ActiveRuntime runtime = new ActiveRuntime(instance, strategy, strategySymbol, credential, symbol, plugin, params);
        runtime.protectionOnly = protectionOnly;
        runtime.leaseValid = true;
        runtime.protectionRule = "PAPER".equalsIgnoreCase(credential.getRunMode()) ? null :
            tradingRuleCache.get(credential.getRunMode(), executionService.toCredential(credential), symbol.getSymbol());
        if (!"PAPER".equalsIgnoreCase(credential.getRunMode()) && runtime.protectionRule == null) {
            throw new IllegalStateException("交易规则未就绪，拒绝启动策略");
        }
        runtime.state = stateService.findByInstance(instance.getId());
        List<ExchangeGateway.PositionSnapshot> startupPositionSnapshot = "PAPER".equalsIgnoreCase(credential.getRunMode()) ? null :
            reconciliationService.runtimePositionSnapshot(credential).stream()
                .filter(snapshot -> snapshot != null && symbol.getSymbol().equalsIgnoreCase(snapshot.symbol())).toList();
        runtime.position = loadPosition(credential, symbol.getSymbol(), executionService.toCredential(credential), startupPositionSnapshot);
        runtime.positionFactsReady = true;
        runtime.positionFactsVerifiedAt = System.currentTimeMillis();
        schedulePositionFactsRefresh(runtime, runtime.positionFactsVerifiedAt);
        if (runtime.position == null && runtime.state != null && !runtime.hasPendingAction()
            && (Boolean.TRUE.equals(runtime.state.getRecoveryRequired()) || Boolean.TRUE.equals(runtime.state.getUnsafeRecovery())
                || "PROTECTION_CLEANUP".equals(runtime.state.getStatus()))) {
            // protectionCleanupPending 是内存字段；从持久化状态重建，防止重启后丢失保护单清理重试。
            runtime.protectionCleanupPending = true;
        }
        if (runtime.position != null && runtime.state == null) {
            runtime.state = recoverState(runtime);
        }
        if (runtime.position != null && !hasFrozenProtectionFacts(runtime)) {
            runtime.state.setRecoveryRequired(true);
            runtime.state.setUnsafeRecovery(true);
            stateService.save(runtime.state);
            runtime.instance.setLastError("既有持仓冻结参数/首次止盈事实不完整，保留现有委托，需人工核查");
            instanceMapper.updateById(runtime.instance);
            log.error("既有持仓保护事实不完整，保留现有委托，需人工核查 instanceId={}", instance.getId());
        }
        try {
            synchronized (runtime) {
                activeRuntimes.put(instance.getId(), runtime);
                if (runtime.position != null) {
                    ensureHardStop(runtime);
                }
                runtime.subscription = marketStreamHub.subscribe(credential.getRunMode(), symbol.getSymbol(), strategySymbol.getIntervalCode(), event -> onMarketEvent(runtime,
            event), ready -> runtime.marketStreamReady = ready);
                if (!"PAPER".equalsIgnoreCase(credential.getRunMode())) {
                    runtime.userSubscription = userDataStreamHub.subscribe(credential, event -> {
                        if (event != null) {
                            invalidatePositionFacts(runtime);
                            invalidateEntryProtectionCleanup(runtime);
                            reconciliationService.invalidateRuntimePositionSnapshot(credential.getId());
                            requestReconciliation(runtime);
                        }
                    });
                }
            }
            prepareTrading(runtime);
        } catch (RuntimeException ex) {
            // A failed private-stream subscription or trading preparation must
            // not leave a half-initialized runtime with a live market socket.
            activeRuntimes.remove(instance.getId(), runtime);
            try {
                closeRuntime(runtime);
            } catch (RuntimeException cleanupError) {
                ex.addSuppressed(cleanupError);
            }
            throw ex;
        }
        resumeRecoveryIfNeeded(runtime);
        log.info("策略运行时已启动 instanceId={} strategy={} plugin={} symbol={} interval={} mode={} protectionOnly={}", instance.getId(), strategy.getStrategyName(),
                strategy.getPluginCode(), symbol.getSymbol(), strategySymbol.getIntervalCode(), credential.getRunMode(), protectionOnly);
        return true;
    }

    /**
     * 对持久化的恢复阻断立即触发账户事实对账，不能只等待后续行情事件。
     * 空仓时恢复内存中的保护单清理重试标志；真正解除阻断仍须通过交易所核实。
     */
    private void resumeRecoveryIfNeeded(ActiveRuntime runtime) {
        boolean recoveryNeeded;
        synchronized (runtime) {
            if (runtime.closed || runtime.state == null) {
                return;
            }
            recoveryNeeded = runtime.hasPendingAction()
                || Boolean.TRUE.equals(runtime.state.getRecoveryRequired())
                || Boolean.TRUE.equals(runtime.state.getUnsafeRecovery())
                || "PROTECTION_CLEANUP".equals(runtime.state.getStatus());
            if (recoveryNeeded && runtime.position == null && !runtime.hasPendingAction()) {
                runtime.protectionCleanupPending = true;
            }
        }
        if (recoveryNeeded) {
            requestReconciliation(runtime);
        }
    }

    /**
     * 在首次决策前确认账户模式、杠杆和交易对规则均已准备完成。
     */
    private void prepareTrading(ActiveRuntime runtime) {
        // FIXED: 保护模式不准备新交易设置。
        if (runtime.protectionOnly) {
            return;
        }
        if (!"PAPER".equalsIgnoreCase(runtime.credential.getRunMode())) {
            int leverage = runtime.strategySymbol.getLeverage() == null ? runtime.strategy.getDefaultLeverage() : runtime.strategySymbol.getLeverage();
            executionService.prepareOpenForRuntime(runtime.credential.getId(), runtime.symbol.getSymbol(), leverage, effectiveMarginMode(runtime));
        }
    }

    /**
     * 停止禁用、失效或失去租约的策略 Runtime。
     */
    private void stopInactiveRuntime(Long instanceId) {
        ActiveRuntime runtime = activeRuntimes.remove(instanceId);
        if (runtime != null) {
            closeRuntime(runtime);
        }
    }

    /**
     * 处理实时事件前确认当前节点仍持有有效租约。
     */
    private boolean ensureEventLease(ActiveRuntime runtime) {
        // Market callbacks must never renew a database lease synchronously.
        // The independent scheduler updates leaseValid; dispatch boundaries re-check ownership.
        return "PAPER".equalsIgnoreCase(runtime.credential.getRunMode()) || runtime.leaseValid;
    }

    /**
     * A fresh live trade/kline proves data is flowing only after the shared stream has
     * completed its own recovery handshake. Replay and status events cannot reopen the gate.
     */
    static boolean shouldRestoreMarketHealth(boolean marketStreamReady, boolean replay, String eventType) {
        return marketStreamReady && !replay && eventType != null
            && ("TRADE".equalsIgnoreCase(eventType) || "KLINE".equalsIgnoreCase(eventType));
    }

    /**
     * 处理实时行情事件并驱动策略计算。
     */
    private void onMarketEvent(ActiveRuntime runtime, ExchangeGateway.MarketEvent event) {
        if (runtime == null || event == null) {
            return;
        }
        if (isExpiredMarketEvent(runtime, event)) {
            reportExpiredMarketEvent(runtime, event);
            return;
        }
        synchronized (runtime) {
            if (runtime.closed) {
                return;
            }
            if (!ensureEventLease(runtime)) {
                // Losing the strategy lease fences decisions, but does not mean the public
                // market feed is unhealthy. MarketStreamHub reports feed readiness separately.
                return;
            }
            if ("CONNECTING".equals(event.type())) {
                // 包括K线换周期后的REST校验；WS仍可能保持连接，不能报告为断线。
                // 必须保留交易门控，直到MarketStreamHub重新确认行情就绪。
                runtime.marketHealthy = false;
                runtime.latestTradePrice = null;
                runtime.latestTradeEventAt = null;
                return;
            }
            if ("DISCONNECTED".equals(event.type()) || "ERROR".equals(event.type())) {
                log.warn("策略行情流断开 instanceId={} symbol={} status={}", runtime.instance.getId(), runtime.symbol.getSymbol(), event.type());
                runtime.marketHealthy = false;
                runtime.latestTradePrice = null;
                runtime.latestTradeEventAt = null;
                return;
            }
            if ("CONNECTED".equals(event.type())) {
                runtime.marketHealthy = true;
                return;
            }
            // CONNECTED may have reached this runtime while its strategy lease was temporarily
            // fenced. Once the hub reports ready, the next fresh live data event is also a safe
            // recovery signal; otherwise this runtime could remain marketHealthy=false forever.
            if (shouldRestoreMarketHealth(runtime.marketStreamReady, event.replay(), event.type())) {
                if (!runtime.marketHealthy) {
                    log.info("策略行情健康门控随实时行情恢复 instanceId={} mode={} symbol={} eventType={}",
                        runtime.instance.getId(), runtime.credential.getRunMode(), runtime.symbol.getSymbol(), event.type());
                }
                runtime.marketHealthy = true;
            }
            if (event.kline() != null) {
                runtime.updateKline(event.kline());
                runtime.market.lastKlineEventAt = event.eventTime();
                runtime.lastEvaluatedKlineOpenTime = event.kline().openTime();
            }
            if (!event.replay() && "MARK".equalsIgnoreCase(event.type()) && event.price() != null && runtime.position != null && !runtime.exitCoordinating.get()) {
                updateRuntimeMaximumReturnRate(runtime, event.price());
            }
            if (event.replay()) {
                // 成交补偿仅恢复开仓后的峰值，不产生回溯下单。
                Instant localEventTime = event.eventTime() == null ? null : event.eventTime().minusMillis(gateway.exchangeTimeOffsetMillis(runtime.credential.getRunMode()));
                if (!runtime.exitCoordinating.get() && event.price() != null && runtime.state != null && runtime.state.getOpenedAt() != null && localEventTime != null &&
                    !localEventTime.isBefore(runtime.state.getOpenedAt().atZone(java.time.ZoneId.systemDefault()).toInstant())) {
                    updateRuntimeExtrema(runtime, event.price());
                }
                return;
            }
            if (event.price() != null && "TRADE".equals(event.type())) {
                runtime.latestTradePrice = event.price();
                runtime.latestTradeEventAt = event.eventTime();
                if (runtime.position != null && !runtime.exitCoordinating.get()) {
                    if (!updateRuntimeExtrema(runtime, event.price())) {
                        return;
                    }
                }
                if (runtime.exitCoordinating.get()) {
                    runtime.exitWindowHigh = runtime.exitWindowHigh == null ? event.price() : runtime.exitWindowHigh.max(event.price());
                    runtime.exitWindowLow = runtime.exitWindowLow == null ? event.price() : runtime.exitWindowLow.min(event.price());
                }
            }
            if (event.price() != null || event.kline() != null) {
                if (runtime.position != null && !runtime.exitCoordinating.get() && "PAPER".equalsIgnoreCase(runtime.credential.getRunMode()) && event.price() != null) {
                    paperTradingService.updateMarkPrice(runtime.position, event.price());
                }
                evaluate(runtime, event);
            }
        }
    }

    /**
     * 基于最新行情、持仓和策略状态执行一次决策。
     */
    private void evaluate(ActiveRuntime runtime, ExchangeGateway.MarketEvent event) {
        long nowMillis = System.currentTimeMillis();
        long exchangeOffsetMillis = gateway.exchangeTimeOffsetMillis(runtime.credential.getRunMode());
        Instant localNow = Instant.now();
        Instant exchangeNow = localNow.plusMillis(exchangeOffsetMillis);
        long maxEventAgeMillis = Math.max(1, properties.getMarket().getMaxEventAgeMillis());
        if (!event.replay()) {
            if (runtime.marketHealthy && runtime.marketStreamReady) {
                runtime.marketUnreadySinceAt = 0;
            } else if (runtime.marketUnreadySinceAt == 0) {
                runtime.marketUnreadySinceAt = nowMillis;
            }
        }
        if (!runtime.marketHealthy || !runtime.marketStreamReady || event.replay()) {
            // 行情未就绪时，新鲜的实时 TRADE/KLINE 仍可更新斜率遥测；回放或未就绪行情不能触发开仓、平仓判断。
            if (!event.replay() && runtime.plugin instanceof KlineSlopeRateStrategy && ("TRADE".equalsIgnoreCase(event.type()) || "KLINE".equalsIgnoreCase(event.type())) &&
                org.dromara.quant.market.MarketEventFreshness.isFresh(event.eventTime(), event.receivedAt(), localNow, exchangeNow, maxEventAgeMillis)) {
                BigDecimal displayPrice = event.price() != null ? event.price() : runtime.currentPrice();
                if (displayPrice != null && displayPrice.signum() > 0) {
                    publishSlopeTelemetry(runtime, buildContext(runtime, displayPrice, event));
                }
            }
            if (!event.replay()) {
                logUnavailableEntry(runtime, event, "MARKET_UNREADY", "行情尚未就绪，暂停策略决策");
            }
            // 所有交易环境共用该门控；斜率遥测可更新不代表行情已满足交易决策条件。
            long unreadyGraceMillis = Math.max(1, properties.getMarket().getIdleTimeoutSeconds()) * 1_000L;
            if (!event.replay() && ("TRADE".equals(event.type()) || "KLINE".equals(event.type())) && runtime.marketUnreadySinceAt > 0 &&
                nowMillis - runtime.marketUnreadySinceAt >= unreadyGraceMillis && nowMillis >= runtime.nextMarketUnreadyLogAt) {
                runtime.nextMarketUnreadyLogAt = nowMillis + 60_000L;
                log.warn("策略行情门控持续未就绪 instanceId={} mode={} symbol={} eventType={} marketHealthy={} marketStreamReady={} unreadyDurationMillis={} eventTime={}",
                    runtime.instance.getId(), runtime.credential.getRunMode(), runtime.symbol.getSymbol(), event.type(), runtime.marketHealthy, runtime.marketStreamReady,
                    nowMillis - runtime.marketUnreadySinceAt, event.eventTime());
            }
            // 限频输出行情未就绪的调试信息，避免高频行情事件刷屏。
            if (log.isDebugEnabled() && System.currentTimeMillis() >= runtime.nextLatencyLogAt) {
                runtime.nextLatencyLogAt = System.currentTimeMillis() + 5_000L;
                log.debug("evaluate 早退[MARKET_UNREADY] instanceId={} symbol={} marketHealthy={} marketStreamReady={} replay={} eventType={}", runtime.instance.getId(),
                 runtime.symbol.getSymbol(), runtime.marketHealthy, runtime.marketStreamReady, event.replay(), event.type());
            }
            return;
        }
        exchangeOffsetMillis = gateway.exchangeTimeOffsetMillis(runtime.credential.getRunMode());
        Instant decisionAt = Instant.now();
        exchangeNow = decisionAt.plusMillis(exchangeOffsetMillis);
        if (log.isDebugEnabled() && System.currentTimeMillis() >= runtime.nextLatencyLogAt) {
            runtime.nextLatencyLogAt = System.currentTimeMillis() + 5_000L;
            log.debug("策略行情分段延迟 instanceId={} symbol={} receiveDelayMillis={} queueWaitMillis={} eventAgeMillis={}", runtime.instance.getId(), runtime.symbol.getSymbol(),
                event.eventTime() == null || event.receivedAt() == null ? null : java.time.Duration.between(event.eventTime(), event.receivedAt().plusMillis(exchangeOffsetMillis)).toMillis(),
                java.time.Duration.between(event.receivedAt(), decisionAt).toMillis(), event.eventTime() == null ? null : java.time.Duration.between(event.eventTime(), exchangeNow).toMillis());
        }
        if (!org.dromara.quant.market.MarketEventFreshness.isFresh(event.eventTime(), event.receivedAt(), decisionAt, exchangeNow, maxEventAgeMillis)) {
            reportExpiredMarketEvent(runtime, event);
            return;
        }
        // 策略开平仓判断使用成交价或 K 线价；MARK 价格不直接触发策略开平仓。
        if (!"TRADE".equalsIgnoreCase(event.type()) && !"KLINE".equalsIgnoreCase(event.type())) {
            return;
        }
        BigDecimal price = event.price() != null ? event.price() : runtime.currentPrice();
        if (price == null || price.signum() <= 0) {
            return;
        }
        if (runtime.position != null && !runtime.exitCoordinating.get()) {
            if (!updateRuntimeExtrema(runtime, price)) {
                return;
            }
        }
        if (runtime.stateRefreshRequired) {
            return;
        }
        StrategyContext context = buildContext(runtime, price, event);
        if (decisionLogs != null && runtime.plugin instanceof KlineSlopeRateStrategy) {
            var slope = KlineSlopeRateStrategy.calculateSlope(context, runtime.params);
            if (slope == null || !slope.entryApproved()) {
                runtime.decisionLogCursor.reset();
            }
        }
        if (log.isDebugEnabled() && System.currentTimeMillis() >= runtime.nextLatencyLogAt) {
            log.debug("evaluate 到达遥测 instanceId={} symbol={} eventType={} price={} prevKlineOpen={} klineOpen={} klineClosed={}", runtime.instance.getId(),
            runtime.symbol.getSymbol(), event.type(), price, context.previousKlineOpenPrice(), context.klineOpenTime(), context.klineClosed());
        }
        publishSlopeTelemetry(runtime, context);
        fenceSnapshotClosedCandle(runtime, context);
        clearProtectionDegradedAfterConfirmedFlat(runtime);
        // 待确认交易或退出协调会暂停本轮开仓、退出和保护同步；实时斜率仍随有效行情更新。
        if (runtime.hasPendingAction() || runtime.exitCoordinating.get()) {
            logEntryDecision(runtime, context, "PENDING_ACTION", "存在待确认交易或退出协调尚未完成，等待状态恢复");
            long now = System.currentTimeMillis();
            if (now >= runtime.nextPendingActionWarnAt) {
                runtime.nextPendingActionWarnAt = now + 30_000L;
                log.warn("策略决策暂停：存在待确认交易或退出协调尚未完成 instanceId={} credentialId={} symbol={} runMode={} pendingAction={} pendingOrderId={} pendingClientOrderId={} idempotencyKey={} exitCoordinating={}",
                    runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(), runtime.credential.getRunMode(),
                    runtime.state == null ? null : runtime.state.getPendingAction(),
                    runtime.state == null ? null : runtime.state.getPendingOrderId(),
                    runtime.state == null ? null : runtime.state.getPendingClientOrderId(),
                    runtime.state == null ? null : runtime.state.getPendingIdempotencyKey(), runtime.exitCoordinating.get());
            }
            return;
        }
        if (runtime.position == null) {
            if (runtime.positionReconciling && !runtime.positionFactsReady) {
                logEntryDecision(runtime, context, "POSITION_RECONCILING", "正在核实交易所持仓");
                long now = System.currentTimeMillis();
                if (now >= runtime.nextPositionReconcilingLogAt) {
                    runtime.nextPositionReconcilingLogAt = now + 30_000L;
                    log.warn("开仓暂停：正在核实交易所持仓 instanceId={} credentialId={} symbol={} runMode={} reason={} reconcileRequested={} nextRetryAt={}",
                        runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(), runtime.credential.getRunMode(),
                        runtime.positionReconciliationBlockReason, runtime.reconcileRequested.get(), runtime.nextReconcileAt);
                }
                return;
            }
            if (!runtime.positionFactsReady) {
                logEntryDecision(runtime, context, "POSITION_FACTS_UNVERIFIED", "账户持仓事实核验未完成，暂停开仓");
                long now = System.currentTimeMillis();
                if (now >= runtime.nextPositionReconcilingLogAt) {
                    runtime.nextPositionReconcilingLogAt = now + 30_000L;
                    log.info("开仓暂停：账户持仓事实尚未完成核验 instanceId={} credentialId={} symbol={}", runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol());
                }
                return;
            }
            // Allow bounded scheduler/network jitter while retaining fail-closed behavior for sustained staleness.
            long positionFactsMaxAgeMillis = Math.max(1, properties.getReconcile().getIntervalSeconds()) * 1_000L + 15_000L;
            if (System.currentTimeMillis() - runtime.positionFactsVerifiedAt > positionFactsMaxAgeMillis) {
                logEntryDecision(runtime, context, "POSITION_FACTS_STALE", "账户持仓事实已过期，等待新快照后再开仓");
                // Staleness must actively trigger reconciliation; otherwise an idle or low-volume
                // symbol can remain blocked indefinitely because no later event requests a refresh.
                // requestReconciliation is bounded by nextReconcileAt and coalesces concurrent requests.
                requestReconciliation(runtime);
                long now = System.currentTimeMillis();
                if (now >= runtime.nextPositionReconcilingLogAt) {
                    runtime.nextPositionReconcilingLogAt = now + 30_000L;
                    log.warn("开仓暂停：账户级持仓快照已过期 instanceId={} credentialId={} symbol={} ageMs={} maxAgeMs={}", runtime.instance.getId(), runtime.credential.getId(),
                        runtime.symbol.getSymbol(), now - runtime.positionFactsVerifiedAt, positionFactsMaxAgeMillis);
                }
                return;
            }
            if (!"RUNNING".equals(runtime.instance.getStatus())) {
                logEntryDecision(runtime, context, "INSTANCE_NOT_RUNNING", "策略实例未处于 RUNNING 状态，是：" + runtime.instance.getStatus());
                log.warn("开仓暂停：策略实例状态不是 RUNNING instanceId={} symbol={} status={}", runtime.instance.getId(), runtime.symbol.getSymbol(), runtime.instance.getStatus());
                return;
            }
            if (runtime.state != null && (Boolean.TRUE.equals(runtime.state.getRecoveryRequired()) || Boolean.TRUE.equals(runtime.state.getUnsafeRecovery()))) {
                logEntryDecision(runtime, context, "RECOVERY_REQUIRED", "遗留交易状态尚未恢复核实");
                long now = System.currentTimeMillis();
                if (now >= runtime.nextRecoveryRequiredLogAt) {
                    runtime.nextRecoveryRequiredLogAt = now + 30_000L;
                    if (runtime.protectionCleanupPending && "CLOSED".equals(runtime.state.getStatus())) {
                        log.warn("开仓暂停：交易所持仓已确认归零，但保护单清理尚未确认 instanceId={} credentialId={} symbol={} positionId={}",
                            runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(), runtime.state.getPositionId());
                    } else {
                        log.warn("开仓暂停：遗留交易状态尚未恢复核实 instanceId={} credentialId={} symbol={} recoveryRequired={} unsafeRecovery={} positionState={} positionQty={}",
                            runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(), runtime.state.getRecoveryRequired(), runtime.state.getUnsafeRecovery(), runtime.state.getStatus(),
                            runtime.state.getPositionQty());
                    }
                }
                return;
            }
            if (runtime.state != null && !"CLOSED".equals(runtime.state.getStatus()) && runtime.state.getPositionQty() != null && runtime.state.getPositionQty().signum() > 0) {
                logEntryDecision(runtime, context, "LOCAL_POSITION_PENDING", "本地存在尚未确认关闭的持仓");
                log.warn("计算是否产生开仓信号前判断问题4：{}，交易对：{}", "本地存在尚未确认关闭的持仓", runtime.symbol);
                return;
            }
            evaluateEntry(runtime, context);
        } else {
            logEntryDecision(runtime, context, "POSITION_OPEN", "该策略实例已有持仓，不重复开仓");
            log.debug("持仓处理：跳过开仓并检查退出条件和保护单 instanceId={} symbol={}", runtime.instance.getId(), runtime.symbol.getSymbol());
            // 先处理已经越过阈值的本地退出，避免先挂已穿价的条件单。
            evaluateExit(runtime, context);
            if (runtime.position != null && !runtime.hasPendingAction() && System.currentTimeMillis() >= runtime.nextProtectionSyncAt) {
                runtime.nextProtectionSyncAt = System.currentTimeMillis() + 2000;
                syncPluginProtections(runtime, context);
            }
        }
    }

    /**
     * 全账户快照可能在运行时未活跃时确认平仓；首次收到新鲜行情时封锁当前 K 线并重启保护清理对账。
     */
    private void fenceSnapshotClosedCandle(ActiveRuntime runtime, StrategyContext context) {
        QuantStrategyPositionState state = runtime.state;
        if (runtime.position != null || state == null || !"CLOSED".equals(state.getStatus()) || context.klineOpenTime() == null
            || !Boolean.TRUE.equals(state.getRecoveryRequired()) && !Boolean.TRUE.equals(state.getUnsafeRecovery())) {
            return;
        }
        blockCloseCandle(runtime, context.klineOpenTime());
        runtime.nextReconcileAt = 0L;
        requestReconciliation(runtime);
    }

    private void clearProtectionDegradedAfterConfirmedFlat(ActiveRuntime runtime) {
        QuantStrategyPositionState state = runtime.state;
        if (runtime.position == null && state != null && "CLOSED".equals(state.getStatus()) && !runtime.hasPendingAction()
            && !Boolean.TRUE.equals(state.getRecoveryRequired()) && !Boolean.TRUE.equals(state.getUnsafeRecovery())
            && !runtime.protectionCleanupPending && !runtime.protectionSyncing.get()) {
            runtime.protectionDegraded = false;
        }
    }

    private boolean hasStateVersionChanged(QuantStrategyPositionState expected, QuantStrategyPositionState latest) {
        if (expected == null) {
            return expected != latest;
        }
        if (latest == null) {
            return false;
        }
        return !Objects.equals(expected.getVersion(), latest.getVersion());
    }

    /**
     * Preserve exchange/scheduler retry windows during reconciliation. Retrying at the generic
     * five-second interval while Binance has explicitly deferred requests only creates a local
     * retry loop; entry remains blocked until facts are verified.
     */
    /**
     * 待确认交易持续无法从交易所核实时采用有界退避，避免每个策略每 5 秒重复打 REST。
     * 不清除 pending、不放开开仓；30 秒上限确保仍会持续进行只读恢复核查。
     */
    static long pendingExecutionRetryDelayMillis(int notFoundConfirmations) {
        if (notFoundConfirmations < 0) {
            return 5_000L;
        }
        if (notFoundConfirmations < 6) {
            return 5_000L;
        }
        if (notFoundConfirmations < 18) {
            return 10_000L;
        }
        if (notFoundConfirmations < 36) {
            return 15_000L;
        }
        return 30_000L;
    }

    static long exchangeRetryAfterMillis(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof ExchangeMarketPort.RequestDeferredException deferred) {
                return deferred.retryAfterMillis();
            }
            if (current instanceof ExchangeMarketPort.RateLimitException rateLimited) {
                return rateLimited.retryAfterMillis();
            }
        }
        return 0L;
    }

    private boolean containsMessage(Throwable error, String message) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(message)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExpiredMarketEvent(ActiveRuntime runtime, ExchangeGateway.MarketEvent event) {
        String type = event.type();
        if (event.replay() || !("TRADE".equalsIgnoreCase(type) || "MARK".equalsIgnoreCase(type) || "KLINE".equalsIgnoreCase(type))) {
            return false;
        }
        long offsetMillis = gateway.exchangeTimeOffsetMillis(runtime.credential.getRunMode());
        Instant now = Instant.now();
        Instant exchangeNow = now.plusMillis(offsetMillis);
        return !org.dromara.quant.market.MarketEventFreshness.isFresh(event.eventTime(), event.receivedAt(), now, exchangeNow, properties.getMarket().getMaxEventAgeMillis());
    }

    private void reportExpiredMarketEvent(ActiveRuntime runtime, ExchangeGateway.MarketEvent event) {
        long now = System.currentTimeMillis();
        if (now < runtime.nextExpiredMarketLogAt) {
            return;
        }
        runtime.nextExpiredMarketLogAt = now + 10_000L;
        synchronized (runtime) {
            if (runtime.closed) {
                return;
            }
            logUnavailableEntry(runtime, event, "STALE_MARKET", "行情事件过期或时间异常，已丢弃本次决策");
        }
        long offsetMillis = gateway.exchangeTimeOffsetMillis(runtime.credential.getRunMode());
        Instant observedAt = Instant.now();
        Instant observedExchangeTime = observedAt.plusMillis(offsetMillis);
        log.warn("策略丢弃过期或时间异常行情，未更新本地行情状态 instanceId={} symbol={} eventTime={} receivedAt={} ageMillis={} queueWaitMillis={}", runtime.instance.getId(),
            runtime.symbol.getSymbol(), event.eventTime(), event.receivedAt(), event.eventTime() == null ? null : java.time.Duration.between(event.eventTime(), observedExchangeTime).toMillis(),
            event.receivedAt() == null ? null : java.time.Duration.between(event.receivedAt(), observedAt).toMillis());
    }

    /** 私有事件或新周期准备使旧清理证明失效；在途清理只能完成核实，不能放行开仓。 */
    private void invalidateEntryProtectionCleanup(ActiveRuntime runtime) {
        // 行情评估可能正持有 runtime 锁等待准入；私有事件必须立即使证明失效。
        runtime.entryProtectionCleanupGeneration.incrementAndGet();
        runtime.entryProtectionCleanupReady = false;
    }

    /** 清理只在恢复线程执行；证明绑定旧周期、私有事件代次和租约代次，不把清理前的信号重放成开仓。 */
    private boolean entryProtectionReady(ActiveRuntime runtime) {
        if (!(runtime.plugin instanceof KlineSlopeRateStrategy)) {
            return true;
        }
        synchronized (runtime) {
            if (runtime.closed || runtime.position != null || runtime.hasPendingAction() || !runtime.positionFactsReady) {
                return false;
            }
            try {
                if (runtime.entryProtectionCleanupReady && runtime.entryProtectionCleanupProofGeneration == runtime.entryProtectionCleanupGeneration.get()
                    && runtime.entryProtectionCleanupEpoch == leaseService.requireCurrentEpoch(
                    runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol())) {
                    return true;
                }
            } catch (RuntimeException ex) {
                runtime.entryProtectionCleanupReady = false;
            }
            if (System.currentTimeMillis() < runtime.nextEntryProtectionCleanupAt || runtime.entryProtectionCleanupRunning) {
                return false;
            }
            runtime.entryProtectionCleanupReady = false;
            runtime.entryProtectionCleanupRunning = true;
            long generation = runtime.entryProtectionCleanupGeneration.get();
            String cycle = runtime.state == null ? null : runtime.state.getPositionCycleKey();
            runtime.entryProtectionCleanupTask = recoveryExecutor.submit(() -> {
                try {
                    if (!leaseService.acquireOrRenew(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol())) {
                        return;
                    }
                    long epoch = leaseService.requireCurrentEpoch(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol());
                    boolean cleaned = protectionOrderService.prepareEntryProtectionCleanup(runtime.credential.getId(), runtime.symbol.getSymbol(),
                        !"PAPER".equalsIgnoreCase(runtime.credential.getRunMode()));
                    synchronized (runtime) {
                        if (cleaned && !runtime.closed && runtime.position == null && !runtime.hasPendingAction() && runtime.positionFactsReady
                            && generation == runtime.entryProtectionCleanupGeneration.get()
                            && Objects.equals(cycle, runtime.state == null ? null : runtime.state.getPositionCycleKey())
                            && epoch == leaseService.requireCurrentEpoch(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol())) {
                            runtime.entryProtectionCleanupEpoch = epoch;
                            runtime.entryProtectionCleanupProofGeneration = generation;
                            runtime.entryProtectionCleanupConfirmedAt = Instant.now().plusMillis(gateway.exchangeTimeOffsetMillis(runtime.credential.getRunMode()));
                            runtime.entryProtectionCleanupReady = true;
                        }
                    }
                } catch (RuntimeException ex) {
                    log.warn("开仓前保护清理尚未确认 instanceId={} symbol={} errorType={}", runtime.instance.getId(), runtime.symbol.getSymbol(), ex.getClass().getSimpleName());
                } finally {
                    synchronized (runtime) {
                        runtime.nextEntryProtectionCleanupAt = System.currentTimeMillis() + 2000L;
                        runtime.entryProtectionCleanupRunning = false;
                    }
                }
            });
            return false;
        }
    }

    /** 清理期间排队的行情不能作为清理完成后的新信号；时间口径与既有行情新鲜度门禁一致。 */
    private boolean entryProtectionReadyForEvent(ActiveRuntime runtime, StrategyContext context) {
        if (!(runtime.plugin instanceof KlineSlopeRateStrategy)) {
            return true;
        }
        return entryProtectionReady(runtime) && runtime.entryProtectionCleanupConfirmedAt != null
            && context.eventTime() != null && context.eventTime().isAfter(runtime.entryProtectionCleanupConfirmedAt);
    }

    /** 在原有准入门禁后核实历史保护清理证明，再使用当前有效行情开仓。 */
    private void evaluateEntry(ActiveRuntime runtime, StrategyContext context) {
        Long strategySymbolId = runtime.strategySymbol == null ? null : runtime.strategySymbol.getId();
        if (strategySymbolId == null) {
            // 未持久化的测试/构造上下文不应发起分布式锁请求；生产实例始终有绑定 ID。
            synchronized (runtime.entryDispatchLock) {
                evaluateEntryLocked(runtime, context);
            }
            return;
        }
        RLock distributedLock = redissonClient.getLock(strategySymbolRuntimeLockKey(strategySymbolId));
        distributedLock.lock();
        try {
            synchronized (runtime.entryDispatchLock) {
                evaluateEntryLocked(runtime, context);
            }
        } finally {
            if (distributedLock.isHeldByCurrentThread()) distributedLock.unlock();
        }
    }

    /** 开仓决策与实际派单共用同一临界区，避免暂停请求与开仓请求交错。 */
    private void evaluateEntryLocked(ActiveRuntime runtime, StrategyContext context) {
        // FIXED: 策略/凭证/交易对被停用后仅保留保护，不再允许开仓。
        if (!runtime.runtimeEnabled || Boolean.FALSE.equals(runtime.strategySymbol.getRuntimeEnabled())) {
            logEntryDecision(runtime, context, "SYMBOL_RUNTIME_DISABLED", "该策略交易对已单独暂停开仓");
            return;
        }
        if (runtime.protectionOnly) {
            logEntryDecision(runtime, context, "PROTECTION_ONLY", "策略已停止开仓，仅保留持仓保护");
            return;
        }
        if (runtime.hasPendingAction() || runtime.state != null && (Boolean.TRUE.equals(runtime.state.getRecoveryRequired()) || Boolean.TRUE.equals(runtime.state.getUnsafeRecovery()))) {
            return;
        }
        if (context.klineOpenTime() == null) {
            logEntryDecision(runtime, context, "KLINE_TIME_MISSING", "缺少当前 K 线时间");
            return;
        }
        if (runtime.protectionSyncing.get() || runtime.protectionCleanupPending) {
            logEntryDecision(runtime, context, "PROTECTION_PENDING", "保护单同步或清理尚未完成");
            return;
        }
        if (runtime.protectionDegraded) {
            logEntryDecision(runtime, context, "PROTECTION_UNCONFIRMED", "当前持仓保护单未全部确认，禁止开仓");
            return;
        }
        Optional<EntrySignal> signal = runtime.plugin.evaluateEntry(context, runtime.params);
        if (signal.isEmpty()) {
            return;
        }

        if (!runtime.entryRetryGate.due(System.currentTimeMillis())) {
            return;
        }
        BigDecimal openRatio = runtime.strategySymbol.getOpenRatio() == null ? runtime.strategy.getDefaultOpenRatio() : runtime.strategySymbol.getOpenRatio();
        int leverage = runtime.strategySymbol.getLeverage() == null ? runtime.strategy.getDefaultLeverage() : runtime.strategySymbol.getLeverage();
        if (openRatio == null || openRatio.signum() <= 0 || openRatio.compareTo(BigDecimal.ONE) > 0 || leverage <= 0) {
            logEntryDecision(runtime, context, "INVALID_OPEN_CONFIG", "开仓比例或杠杆无效");
            log.warn("策略开仓跳过：开仓比例或杠杆无效 instanceId={} symbol={}", runtime.instance.getId(), runtime.symbol.getSymbol());
            return;
        }

        Integer maxOpenCount = runtime.strategy.getMaxOpenCount();
        if (maxOpenCount != null && maxOpenCount > 0) {
            Long openCount;
            try {
                // FIXED: 统计状态包含 OPEN_PENDING，避免并发下突破最大开仓数。
                openCount = runtimeLedgerMapper.countStrategyExposures(runtime.strategy.getId(), runtime.credential.getId());
                if (openCount == null) {
                    throw new org.springframework.dao.DataRetrievalFailureException("Strategy exposure count unavailable");
                }
            } catch (org.springframework.dao.DataAccessException ex) {
                // 持仓事实不可读时必须停止本次开仓，不能在数据库连接异常期间放行交易。
                logEntryDecision(runtime, context, "POSITION_COUNT_UNAVAILABLE", "最大持仓数查询失败，禁止放行");
                log.warn("策略开仓跳过：最大持仓数查询失败 instanceId={} symbol={} error={}", runtime.instance.getId(), runtime.symbol.getSymbol(), ex.getMessage());
                return;
            }
            if (openCount != null && openCount >= maxOpenCount) {
                logEntryDecision(runtime, context, "MAX_POSITIONS", "达到策略最大持仓数");
                log.warn("策略开仓跳过：达到最大持仓数 instanceId={} symbol={} openCount={} maxOpenCount={}", runtime.instance.getId(), runtime.symbol.getSymbol(), openCount, maxOpenCount);
                logExposureDiagnostic(runtime, openCount, maxOpenCount);
                return;
            }
        }

        // 先保留并核实旧关闭事实，不能等 OPEN_PENDING 覆盖后才尝试释放历史预留。
        if (runtime.state != null && "CLOSED".equals(runtime.state.getStatus()) && !runtime.hasPendingAction()) {
            try {
                executionService.releaseClosedOpenReservationsForRuntime(runtime.credential.getId(), runtime.instance.getId(), runtime.symbol.getSymbol(),
                 runtime.credential.getRunMode());
            } catch (RuntimeException ex) {
                logEntryDecision(runtime, context, "CLOSED_RESERVATION_UNAVAILABLE", org.dromara.quant.decision.TradeDecisionLogService.safeReason(ex));
                log.warn("策略开仓等待历史预留核实 instanceId={} symbol={} reason={}", runtime.instance.getId(), runtime.symbol.getSymbol(), ex.getMessage());
                return;
            }
        }

        boolean paper = "PAPER".equalsIgnoreCase(runtime.credential.getRunMode());
        BigDecimal availableBalance;
        org.dromara.quant.market.AccountBalanceSnapshot balanceSnapshot;
        if (paper) {
            balanceSnapshot = null;
            availableBalance = paperTradingService.availableBalance(runtime.credential.getId());
        } else {
            try {
                balanceSnapshot = balanceCache.requireSnapshot(runtime.credential.getId());
                availableBalance = openCapacityService.availableBalance(runtime.credential.getId(), balanceSnapshot);
                preparationService.requireReady(runtime.credential.getId(), runtime.symbol.getSymbol(), leverage, effectiveMarginMode(runtime));
            } catch (RuntimeException ex) {
                logEntryDecision(runtime, context, "ACCOUNT_NOT_READY", org.dromara.quant.decision.TradeDecisionLogService.safeReason(ex));
                log.debug("策略开仓等待账户余额或交易设置校准 instanceId={} symbol={} reason={}", runtime.instance.getId(), runtime.symbol.getSymbol(), ex.getMessage());
                if (ex instanceof LiveTradingGate.LiveTradingBlockedException) {
                    notifyPreparationBlocked(runtime, ex.getMessage());
                }
                return;
            }
        }
        if (availableBalance == null || availableBalance.signum() <= 0) {
            logEntryDecision(runtime, context, "INSUFFICIENT_BALANCE", "可用余额不足");
            log.debug("策略开仓跳过：可用余额不足 instanceId={} symbol={}", runtime.instance.getId(), runtime.symbol.getSymbol());
            return;
        }

        BigDecimal targetMargin = availableBalance.multiply(openRatio);
        BigDecimal targetNotional = targetMargin.multiply(BigDecimal.valueOf(leverage));
        BigDecimal quantity = targetNotional.divide(context.currentPrice(), 16, RoundingMode.DOWN);
        if (quantity.signum() <= 0) {
            logEntryDecision(runtime, context, "ZERO_QUANTITY", "下单数量为零");
            log.debug("策略开仓跳过：下单数量为零 instanceId={} symbol={}", runtime.instance.getId(), runtime.symbol.getSymbol());
            return;
        }
        if (runtime.protectionRule != null) {
            try {
                quantity = org.dromara.quant.exchange.QuantityNormalizer.quantity(quantity, runtime.protectionRule, "MARKET");
                if (runtime.plugin instanceof KlineSlopeRateStrategy) {
                    org.dromara.quant.exchange.QuantityNormalizer.quantity(quantity.multiply(KlineSlopeRateStrategy.firstClosePositionRate(runtime.params)),
                     runtime.protectionRule, "MARKET");
                }
            } catch (IllegalArgumentException ex) {
                logEntryDecision(runtime, context, "PROTECTION_QUANTITY_INVALID", "开仓或首次止盈数量不满足交易步长：" + ex.getMessage());
                return;
            }
        }
        BigDecimal margin = quantity.multiply(context.currentPrice()).divide(BigDecimal.valueOf(leverage), 16, RoundingMode.HALF_UP);
        if (margin.signum() <= 0) {
            logEntryDecision(runtime, context, "INVALID_MARGIN", "计算保证金非正数");
            log.warn("策略开仓跳过：计算保证金非正数 instanceId={} symbol={} margin={} quantity={} currentPrice={} leverage={}", runtime.instance.getId(), runtime.symbol.getSymbol(), margin,
             quantity, context.currentPrice(), leverage);
            return;
        }
        BigDecimal retryQuantity = quantity;
        try {
            if (!runtime.entryRetryGate.allow(System.currentTimeMillis(), () -> executionService.requireOpenCapacityForRuntime(runtime.credential.getId(),
             runtime.strategy.getId(), runtime.instance.getId(), runtime.symbol.getSymbol(), signal.get().direction() == EntrySignal.Direction.LONG ? "BUY" : "SELL",
        retryQuantity, context.currentPrice(), leverage, effectiveMarginMode(runtime), runtime.credential.getRunMode(), balanceSnapshot))) {
                return;
            }
        } catch (RuntimeException ex) {
            logEntryDecision(runtime, context, "RETRY_STATE_UNAVAILABLE", org.dromara.quant.decision.TradeDecisionLogService.safeReason(ex));
            return;
        }

        // 资金与风控准入后再次验证原始行情时间，禁止按过期点位追单。
        long exchangeOffsetMillis = gateway.exchangeTimeOffsetMillis(runtime.credential.getRunMode());
        Instant exchangeNow = Instant.now().plusMillis(exchangeOffsetMillis);
        if (!runtime.marketHealthy || !runtime.marketStreamReady || !org.dromara.quant.market.MarketEventFreshness.isExchangeTimeFresh(context.eventTime(), exchangeNow,
            properties.getMarket().getMaxEventAgeMillis())) {
            logEntryDecision(runtime, context, "STALE_AFTER_ADMISSION", "资金准入后行情不可用或事件已过期");
            log.warn("策略开仓跳过：资金准入后行情不可用或事件已过期 instanceId={} symbol={} marketHealthy={} marketStreamReady={} eventTime={} maxEventAgeMillis={}", runtime.instance.getId(),
             runtime.symbol.getSymbol(), runtime.marketHealthy, runtime.marketStreamReady, context.eventTime(), Math.max(1, properties.getMarket().getMaxEventAgeMillis()));
            return;
        }
        // 准备期间的私有事件或租约转移会使清理证明失效，不能沿用旧证明开仓。
        if (!entryProtectionReadyForEvent(runtime, context)) {
            logEntryDecision(runtime, context, "ENTRY_PROTECTION_CLEANUP", "历史保护清理或清理后的新行情尚未就绪");
            return;
        }
        // 开仓前重新读取持久化开关，避免其他节点/监督周期尚未刷新缓存时沿用旧状态。
        // 查询失败时必须 fail closed；不能因数据库暂时不可读而放行新开仓。
        // 生产 Runtime 必须有持久化绑定 ID；无 ID 仅用于单元测试/内部构造上下文，不能查询 null 主键并误判为绑定不存在。
        Long persistedBindingId = runtime.strategySymbol.getId();
        if (persistedBindingId != null) {
            QuantStrategySymbol latestBinding;
            try {
                latestBinding = strategySymbolMapper.selectById(persistedBindingId);
            } catch (RuntimeException ex) {
                log.warn("开仓暂停：无法核实交易对运行开关 instanceId={} strategySymbolId={} errorType={}",
                    runtime.instance.getId(), persistedBindingId, ex.getClass().getSimpleName());
                return;
            }
            if (latestBinding == null || Boolean.FALSE.equals(latestBinding.getRuntimeEnabled())) {
                runtime.runtimeEnabled = false;
                runtime.strategySymbol.setRuntimeEnabled(false);
                logEntryDecision(runtime, context, "SYMBOL_RUNTIME_DISABLED", "数据库中的交易对运行开关已关闭或绑定已不存在");
                return;
            }
            runtime.runtimeEnabled = true;
            runtime.strategySymbol.setRuntimeEnabled(latestBinding.getRuntimeEnabled());
        }

        // 仅在开仓前置条件满足后占用本根 K 线；余额或持仓条件暂不满足时允许后续行情重新评估。
        if (!entryGuardService.tryClaim(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(), context.klineOpenTime())) {
            logEntryDecision(runtime, context, "KLINE_ALREADY_CLAIMED", "本根 K 线已占用开仓保护记录");
            log.debug("策略开仓跳过：本根K线已占用开仓保护记录 instanceId={} symbol={} klineOpenTime={}", runtime.instance.getId(), runtime.symbol.getSymbol(), context.klineOpenTime());
            return;
        }

        String direction = signal.get().direction().name();
        String side = signal.get().direction() == EntrySignal.Direction.LONG ? "BUY" : "SELL";
        String idempotencyKey = buildIdempotencyKey(runtime, "OPEN", context.klineOpenTime());
        QuantExecutionRecord previousAttempt = executionRecordService.find(runtime.credential.getId(), idempotencyKey);
        if (previousAttempt != null && "FAILED".equals(previousAttempt.getExecutionStatus())) {
            idempotencyKey += ":" + UUID.randomUUID();
        }
        QuantStrategyPositionState state;
        try {
            state = preparePendingStateForOpen(runtime, direction, quantity, context.currentPrice(), context.klineOpenTime(), idempotencyKey);
        } catch (RuntimeException ex) {
            recordTradeDecision(runtime, context, "OPEN", signal.get().direction().name(), quantity, margin, idempotencyKey, "FAILED", "PENDING_STATE_FAILED",
                org.dromara.quant.decision.TradeDecisionLogService.safeReason(ex), null, false);
            entryGuardService.release(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(), context.klineOpenTime());
            throw ex;
        }
        runtime.state = state;
        recordTradeDecision(runtime, context, "OPEN", direction, quantity, margin, idempotencyKey, "REQUESTED", "SLOPE_ENTRY", signal.get().reason(), null, false);
        runtime.lastEntryAttemptEventTime = context.eventTime();
        runtime.lastEntryAttemptPrice = context.currentPrice();
        boolean marginReserved = false;
        ExecutionResult executionResult = null;
        try {
            if (paper) {
                paperTradingService.reserveMargin(runtime.credential.getId(), margin);
                marginReserved = true;
            }
            executionResult = executionService.openByRuntime(runtime.credential.getId(), runtime.strategy.getId(), runtime.instance.getId(), runtime.symbol.getSymbol(), side,
             null, quantity, context.currentPrice(), leverage, effectiveMarginMode(runtime), idempotencyKey, context.eventTime());
            recordTradeDecision(runtime, context, "OPEN", direction, quantity, margin, idempotencyKey, "ACCEPTED", "EXECUTION_RESULT", signal.get().reason(), executionResult,
             false);
            runtime.entryRetryGate.reset();
            entryGuardService.bindOrder(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(), context.klineOpenTime(), executionResult.orderId());
            state.setPendingOrderId(executionResult.orderId());
            state.setPendingClientOrderId(executionResult.clientOrderId());
            runtime.state = state;
            runtime.instance.setLastSignalAt(LocalDateTime.now());
            instanceMapper.updateById(runtime.instance);
            if (paper) {
                runtime.position = paperTradingService.recordOpenFill(runtime.credential.getId(), idempotencyKey, runtime.symbol.getSymbol(), direction, quantity,
                 context.currentPrice(), leverage);
                recordTradeDecision(runtime, context, "OPEN", direction, quantity, margin, idempotencyKey, "FILLED", "EXECUTION_RESULT", signal.get().reason(), executionResult,
                 true);
            } else {
                runtime.position = loadPosition(runtime.credential, runtime.symbol.getSymbol(), executionService.toCredential(runtime.credential));
            }
            if (runtime.position == null) {
                state.setRecoveryRequired(true);
                state.setUnsafeRecovery(!paper);
                stateService.save(state);
                return;
            }
            state.setPositionId(runtime.position.getId());
            state.setPositionQty(runtime.position.getQuantity());
            state.setEntryPrice(runtime.position.getEntryPrice());
            state.setInitialPositionQty(runtime.position.getQuantity());
            ExchangeGateway.OrderSnapshot filledOpen = null;
            if (!paper) {
                filledOpen = gateway.queryOrder(executionService.toCredential(runtime.credential), runtime.symbol.getSymbol(), executionResult.orderId(),
                 executionResult.clientOrderId());
                // 主动查询取得的订单事实也必须入日志，不能只依赖私有流或异常恢复补记成交。
                if (decisionLogs != null && filledOpen != null) {
                    decisionLogs.order(runtime.credential.getId(), filledOpen);
                }
                if (filledOpen == null || !"FILLED".equalsIgnoreCase(filledOpen.status()) || filledOpen.executedQuantity() == null || filledOpen.executedQuantity().signum() <= 0 || runtime.position.getQuantity().compareTo(filledOpen.executedQuantity()) > 0 || !direction.equalsIgnoreCase(runtime.position.getPositionSide())) {
                    state.setRecoveryRequired(true);
                    state.setUnsafeRecovery(true);
                    stateService.save(state);
                    return;
                }
                state.setInitialPositionQty(filledOpen.executedQuantity());
            }
            state.setInitialMargin(runtime.position.getMargin());
            freezeFilledProtection(runtime);
            freezeOrderEntryPrice(state, filledOpen);
            state.setStatus("OPEN");
            if (state.getOpenedAt() == null) {
                state.setOpenedAt(LocalDateTime.now());
            }
            clearPending(state);
            stateService.save(state);
            runtime.state = state;
            StrategyContext openedContext = buildContext(runtime, context.currentPrice(), new ExchangeGateway.MarketEvent("TRADE", runtime.symbol.getSymbol(),
            runtime.strategySymbol.getIntervalCode(), context.currentPrice(), null, context.eventTime()));
            syncPluginProtections(runtime, openedContext);   // 内部会 requestProtectionSync(runtime, true)
            notificationService.notifyOpen(runtime.strategy, runtime.credential, runtime.symbol.getSymbol(), direction, runtime.position.getQuantity(),
             runtime.position.getEntryPrice(), leverage, openRatio, signal.get().reason());
            log.info("实时策略开仓 instanceId={} symbol={} direction={} price={} quantity={} reason={}", runtime.instance.getId(), runtime.symbol.getSymbol(), direction,
             context.currentPrice(), quantity, signal.get().reason());
        } catch (RuntimeException ex) {
            QuantExecutionRecordStatus status = findExecutionStatus(runtime.credential.getId(), idempotencyKey);
            if (executionResult != null && executionResult.accepted()) {
                state.setRecoveryRequired(true);
                state.setUnsafeRecovery(!paper);
                stateService.save(state);
            } else if ("FAILED".equals(status.value())) {
                entryGuardService.release(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(), context.klineOpenTime());
                if (paper && marginReserved) {
                    releasePaperMargin(runtime.credential.getId(), margin);
                }
                stateService.markClosed(state);
                boolean waitForChange = ex instanceof OpenCapacityService.CapacityBlockedException blocked && blocked.waitForCapacityChange();
                runtime.entryRetryGate.failed(System.currentTimeMillis(), waitForChange);
                if (ex instanceof LiveTradingGate.LiveTradingBlockedException || ex instanceof org.dromara.quant.risk.OpenCapacityService.CapacityBlockedException) {
                    notificationService.notifyLiveBlocked(runtime.credential, runtime.symbol.getSymbol(), ex.getMessage());
                }
            } else {
                state.setRecoveryRequired(true);
                state.setUnsafeRecovery(!paper);
                stateService.save(state);
            }
            String reason = ex instanceof ExchangeTradingPort.ExchangeRequestException || ex instanceof OpenCapacityService.CapacityBlockedException ? ex.getMessage() :
             ex.getClass().getSimpleName();
            runtime.instance.setLastError(reason);
            instanceMapper.updateById(runtime.instance);
            recordTradeDecision(runtime, context, "OPEN", direction, quantity, margin, idempotencyKey, "FAILED".equals(status.value()) ? "FAILED" : "UNKNOWN", "EXECUTION_FAILURE"
 , org.dromara.quant.decision.TradeDecisionLogService.safeReason(ex), executionResult, false);
            log.warn("策略开仓执行未完成，保留行情连接 instanceId={} symbol={} executionStatus={} reason={}", runtime.instance.getId(), runtime.symbol.getSymbol(), status.value(), reason);
            if (!"FAILED".equals(status.value())) {
                requestReconciliation(runtime);
            }
        }
    }

    private void logExposureDiagnostic(ActiveRuntime runtime, Long openCount, Integer maxOpenCount) {
        long now = System.currentTimeMillis();
        Long nextLogAt = nextExposureDiagnosticLogAt.get(runtime.instance.getId());
        if (nextLogAt != null && now < nextLogAt) {
            return;
        }
        nextExposureDiagnosticLogAt.put(runtime.instance.getId(), now + 60_000L);
        try {
            List<Map<String, Object>> exposures = runtimeLedgerMapper.findStrategyExposures(runtime.strategy.getId(), runtime.credential.getId());
            log.warn("策略最大持仓占用明细 instanceId={} symbol={} runMode={} strategyId={} credentialId={} openCount={} maxOpenCount={} exposures={}", runtime.instance.getId(),
             runtime.symbol.getSymbol(), runtime.credential.getRunMode(), runtime.strategy.getId(), runtime.credential.getId(), openCount, maxOpenCount, exposures);
        } catch (RuntimeException ex) {
            log.warn("策略最大持仓占用明细查询失败 instanceId={} strategyId={} credentialId={} error={}", runtime.instance.getId(), runtime.strategy.getId(), runtime.credential.getId(),
             ex.getMessage());
        }
    }

    /**
     * 记录经成交事实确认的持仓减少，不用估值变化推断平仓。
     */
    private void recordPositionDecrease(ActiveRuntime runtime, QuantPosition before, BigDecimal closedQuantity, String reason) {
        if (before == null || closedQuantity == null || closedQuantity.signum() <= 0) {
            return;
        }
        blockCloseCandle(runtime, runtime.lastEvaluatedKlineOpenTime);
        if (decisionLogs == null) {
            return;
        }
        try {
            BigDecimal remaining = runtime.position == null ? BigDecimal.ZERO : runtime.position.getQuantity();
            decisionLogs.record(org.dromara.quant.decision.DecisionLogEvent.builder().eventKey("P:" + runtime.credential.getId() + ":" + before.getId() + ":" + remaining).recordType("POSITION_CHANGE").credentialId(runtime.credential.getId()).accountName(runtime.credential.getAccountName()).runMode(runtime.credential.getRunMode()).strategyName("外部交易／策略未确认").symbol(runtime.symbol.getSymbol()).operation("CLOSE").direction(before.getPositionSide()).quantity(closedQuantity).status("POSITION_CONFIRMED").reasonCode("EXCHANGE_POSITION_CHANGE").reason(reason + "；持仓变化已确认，实际成交价格和操作来源以订单成交记录为准").eventTime(Instant.now()).build());
        } catch (RuntimeException ignored) {
        }
    }

    /** 持仓减少已由交易所事实确认后，持久化封锁发生减少的账户/交易对 K 线。 */
    private void blockCloseCandle(ActiveRuntime runtime, Instant klineOpenTime) {
        Instant candle = klineOpenTime == null ? runtime.lastEvaluatedKlineOpenTime : klineOpenTime;
        if (candle == null) {
            throw new IllegalStateException("无法确认平仓所属 K 线，禁止解除账户交易对开仓封锁");
        }
        entryGuardService.blockClosedCandle(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(), candle);
    }

    static Instant klineOpenAt(Instant eventTime, String intervalCode) {
        if (eventTime == null || intervalCode == null || intervalCode.length() < 2) {
            return null;
        }
        try {
            int amount = Integer.parseInt(intervalCode.substring(0, intervalCode.length() - 1));
            if (amount <= 0) {
                return null;
            }
            char unit = intervalCode.charAt(intervalCode.length() - 1);
            if (unit == 'M') {
                java.time.ZonedDateTime utc = eventTime.atZone(java.time.ZoneOffset.UTC);
                long monthIndex = (long) utc.getYear() * 12 + utc.getMonthValue() - 1;
                long bucket = Math.floorDiv(monthIndex, amount) * amount;
                return java.time.YearMonth.of(Math.toIntExact(Math.floorDiv(bucket, 12)), (int) Math.floorMod(bucket, 12) + 1)
                    .atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            }
            long secondsPerUnit = switch (unit) {
                case 's' -> 1L;
                case 'm' -> 60L;
                case 'h' -> 3600L;
                case 'd' -> 86_400L;
                case 'w' -> 604_800L;
                default -> 0L;
            };
            if (secondsPerUnit == 0) {
                return null;
            }
            long width = Math.multiplyExact(secondsPerUnit, amount);
            long anchor = unit == 'w' ? 4 * 86_400L : 0L;
            long bucket = Math.floorDiv(eventTime.getEpochSecond() - anchor, width) * width + anchor;
            return Instant.ofEpochSecond(bucket);
        } catch (RuntimeException invalidInterval) {
            return null;
        }
    }

    private void blockCloseCandleAtExchangeTime(ActiveRuntime runtime, Instant exchangeTime) {
        Instant candleOpen = klineOpenAt(exchangeTime, runtime.strategySymbol.getIntervalCode());
        if (candleOpen == null) {
            blockCloseCandle(runtime, runtime.lastEvaluatedKlineOpenTime);
            log.warn("成交时间或策略K线周期无法映射到所属K线，已封锁最近已观察K线 instanceId={} symbol={} exchangeTime={} interval={}", runtime.instance.getId(), runtime.symbol.getSymbol(),
                exchangeTime, runtime.strategySymbol.getIntervalCode());
            return;
        }
        blockCloseCandle(runtime, candleOpen);
    }

    private void blockCloseCandleForExecution(ActiveRuntime runtime, ExecutionResult result, Instant fallbackKlineOpen) {
        if (result != null && (result.orderId() != null || result.clientOrderId() != null && !result.clientOrderId().isBlank())) {
            try {
                ExchangeGateway.OrderSnapshot order = gateway.queryOrder(executionService.toCredential(runtime.credential), runtime.symbol.getSymbol(), result.orderId(),
                    result.clientOrderId());
                if (order != null && "FILLED".equalsIgnoreCase(order.status()) && order.executedQuantity() != null && order.executedQuantity().signum() > 0
                    && order.updateTime() != null) {
                    blockCloseCandleAtExchangeTime(runtime, order.updateTime());
                    return;
                }
            } catch (RuntimeException ex) {
                log.warn("平仓已确认但无法读取交易所成交时间，保留信号K线封锁并封锁最近已观察K线 instanceId={} symbol={} orderId={} errorType={}", runtime.instance.getId(),
                    runtime.symbol.getSymbol(), result.orderId(), ex.getClass().getSimpleName());
            }
        }
        blockCloseCandle(runtime, fallbackKlineOpen);
        if (!Objects.equals(fallbackKlineOpen, runtime.lastEvaluatedKlineOpenTime)) {
            blockCloseCandle(runtime, runtime.lastEvaluatedKlineOpenTime);
        }
    }

    private void logUnavailableEntry(ActiveRuntime runtime, ExchangeGateway.MarketEvent event, String code, String reason) {
        if (decisionLogs == null || event.replay() || !("TRADE".equals(event.type()) || "KLINE".equals(event.type()))) {
            return;
        }
        BigDecimal price = event.price() == null ? runtime.currentPrice() : event.price();
        if (price != null && price.signum() > 0) {
            logEntryDecision(runtime, buildContext(runtime, price, event), code, reason);
        }
    }

    /**
     * 构造决策日志快照，只保存本次判断所需的行情与策略上下文。
     */
    private DecisionLogEvent.DecisionLogEventBuilder decisionSnapshot(ActiveRuntime runtime, StrategyContext context) {
        var slope = KlineSlopeRateStrategy.calculateSlope(context, runtime.params);
        return DecisionLogEvent.builder().credentialId(runtime.credential.getId()).accountName(runtime.credential.getAccountName()).runMode(runtime.credential.getRunMode()).strategyId(runtime.strategy.getId()).strategyInstanceId(runtime.instance.getId()).strategyName(runtime.strategy.getStrategyName()).symbol(runtime.symbol.getSymbol()).klineTime(context.klineOpenTime()).eventTime(context.eventTime() == null ? Instant.now() : context.eventTime()).price(context.currentPrice()).angle(slope == null ? null : slope.degrees()).threshold(slope == null ? null : slope.entryThreshold());
    }

    private void logEntryDecision(ActiveRuntime runtime, StrategyContext context, String code, String reason) {
        if (decisionLogs == null || !(runtime.plugin instanceof KlineSlopeRateStrategy)) {
            return;
        }
        try {
            var slope = KlineSlopeRateStrategy.calculateSlope(context, runtime.params);
            if (slope == null || !slope.entryApproved()) {
                runtime.decisionLogCursor.reset();
                return;
            }
            decisionLogs.record(decisionSnapshot(runtime, context).eventKey(runtime.decisionLogCursor.key(context.klineOpenTime(), code + ":" + reason.replaceAll("[+-]?\\d+(?:\\" +
             ".\\d+)?", "#"))).recordType("EVALUATION").operation("OPEN").direction(slope.rawDegrees() > 0 ? "LONG" : "SHORT").status("BLOCKED").reasonCode(code).reason(reason).build());
        } catch (RuntimeException ignored) { /* 日志不能影响策略。 */ }
    }

    /**
     * 统一记录开平仓判断结果，日志写入失败不得改变交易决策结果。
     */
    private void recordTradeDecision(ActiveRuntime runtime, StrategyContext context, String operation, String direction, BigDecimal quantity, BigDecimal margin,
    String executionKey, String status, String reasonCode, String reason, ExecutionResult result, boolean paperFill) {
        if (decisionLogs == null) {
            return;
        }
        try {
            String logReason = org.dromara.quant.decision.TradeDecisionLogService.withPlannedQuantityAndNotional(reason, operation, quantity,
                context == null ? null : context.currentPrice());
            decisionLogs.record(decisionSnapshot(runtime, context).eventKey(org.dromara.quant.decision.TradeDecisionLogRepository.executionKey(runtime.credential.getId(),
 executionKey)).recordType("EXECUTION").operation(operation).direction(direction).quantity(quantity).margin(margin).status(paperFill ? "FILLED" : status).reasonCode(reasonCode).reason(logReason).idempotencyKey(executionKey).orderId(result == null ? null : result.orderId()).clientOrderId(result == null ? null : result.clientOrderId()).tradeTime(paperFill ? Instant.now() : null).build());
            runtime.decisionLogCursor.reset();
        } catch (RuntimeException ignored) { /* 日志不能影响交易。 */ }
    }

    /**
     * 后台发送准备阶段拒绝原因；数据库通知写入不得阻塞行情，按实例限频。
     */
    private void notifyPreparationBlocked(ActiveRuntime runtime, String reason) {
        long now = System.currentTimeMillis();
        if (now < runtime.nextPreparationNoticeAt) {
            return;
        }
        runtime.nextPreparationNoticeAt = now + 60_000L;
        Thread.ofVirtual().name("quant-preparation-notice").start(() -> notificationService.notifyLiveBlocked(runtime.credential, runtime.symbol.getSymbol(), reason));
    }

    /**
     * 推送策略插件实际使用的斜率结果；交易决策仍由插件原逻辑执行。
     */
    private void publishSlopeTelemetry(ActiveRuntime runtime, StrategyContext context) {
        if (!(runtime.plugin instanceof KlineSlopeRateStrategy)) {
            return;
        }
        KlineSlopeRateStrategy.SlopeEvaluation evaluation = KlineSlopeRateStrategy.calculateSlope(context, runtime.params);
        if (runtime.hasPendingAction() && System.nanoTime() >= runtime.nextPendingSlopeLogNanos) {
            runtime.nextPendingSlopeLogNanos = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            if (evaluation == null) {
                log.warn("策略实时斜率无法计算 instanceId={} symbol={} currentPrice={} previousKlineOpenPrice={} pendingAction={}", runtime.instance.getId(), context.symbol(),
context.currentPrice(), context.previousKlineOpenPrice(), runtime.state.getPendingAction());
            } else {
                log.info("策略实时斜率 +++ 时间：{} 交易对：{} 上根K线开盘价：{} 当前价：{} 坡度百分：{} 度数：{}° 开仓暂停 pendingAction={} instanceId={}", context.eventTime(), context.symbol(),
evaluation.referenceOpenPrice(), evaluation.currentPrice(), evaluation.slopePercent(), evaluation.degrees(), runtime.state.getPendingAction(),
                  runtime.instance.getId());
            }
        }
        if (evaluation == null) {
            long now = System.currentTimeMillis();
            if (now - runtime.nextSlopeDiagLogAt >= 30_000L) {
                runtime.nextSlopeDiagLogAt = now;
                log.warn("斜率计算返回 null instanceId={} symbol={} currentPrice={} previousKlineOpen={} klineOpen={} klineClosed={}", runtime.instance.getId(), context.symbol(),
                 context.currentPrice(), context.previousKlineOpenPrice(), context.klineOpenTime(), context.klineClosed());
            }
            return;
        }
        slopeTelemetryHub.publish(new StrategySlopeTelemetry(runtime.strategy.getId(), runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(),
    runtime.strategySymbol.getIntervalCode(), evaluation.currentPrice(), evaluation.referenceOpenPrice(), evaluation.slopePercent(), evaluation.degrees(),
          evaluation.entryThreshold(), evaluation.entryApproved(), context.eventTime()));
    }

    /**
     * 冻结实际成交事实；参数快照和周期在发单前已经持久化。
     */
    private void freezeFilledProtection(ActiveRuntime runtime) {
        QuantStrategyPositionState state = runtime.state;
        if (state.getPositionCycleKey() == null) {
            state.setRecoveryRequired(true);
            state.setUnsafeRecovery(true);
            throw new IllegalStateException("历史持仓缺少周期及冻结事实，保留现有保护，需人工核查");
        }
        state.setEntryPrice(runtime.position.getEntryPrice());
        if (runtime.position.getLeverage() != null && runtime.position.getLeverage() > 0) {
            state.setFrozenLeverage(runtime.position.getLeverage());
        }
        state.setHighestPriceSinceEntry(runtime.partialOpenProtectionVerified && state.getHighestPriceSinceEntry() != null
            ? state.getHighestPriceSinceEntry().max(state.getEntryPrice()) : state.getEntryPrice());
        state.setLowestPriceSinceEntry(runtime.partialOpenProtectionVerified && state.getLowestPriceSinceEntry() != null
            ? state.getLowestPriceSinceEntry().min(state.getEntryPrice()) : state.getEntryPrice());
        runtime.position.setProtectionCycleKey(state.getPositionCycleKey());
        if (runtime.plugin instanceof KlineSlopeRateStrategy) {
            BigDecimal target = state.getInitialPositionQty().multiply(KlineSlopeRateStrategy.firstClosePositionRate(frozenParams(runtime)));
            if (runtime.protectionRule != null) {
                target = org.dromara.quant.exchange.QuantityNormalizer.quantity(target, runtime.protectionRule, "MARKET");
            }
            if (target.signum() <= 0) {
                throw new IllegalStateException("首次止盈数量低于交易规则最小步长，需人工核查");
            }
            state.setFirstTakeProfitTargetQty(target);
        }
    }

    /**
     * 判断保护状态是否已有不可逆成交事实，避免再按旧周期覆盖。
     */
    private boolean hasFrozenProtectionFacts(ActiveRuntime runtime) {
        return !(runtime.plugin instanceof KlineSlopeRateStrategy) || runtime.state != null && runtime.state.getPositionCycleKey() != null && runtime.state.getFrozenLeverage() != null && runtime.state.getEntryPrice() != null && runtime.state.getFirstTakeProfitTargetQty() != null;
    }

    /**
     * 三层止损并存；插件仍只负责自身信号，不访问交易所。
     */
    private List<ProtectionSignal> protectionSignals(ActiveRuntime runtime, StrategyContext context, Map<String, Object> params) {
        List<ProtectionSignal> signals = new ArrayList<>(runtime.plugin.evaluateProtection(context, params));
        if (runtime.plugin instanceof KlineSlopeRateStrategy && runtime.state != null && runtime.state.getFrozenSymbolStopLossRatio() != null && !KlineSlopeRateStrategy.unifiedProtection(params)) {
            signals.add(new ProtectionSignal("SYMBOL_STOP", "STOP_MARKET", BigDecimal.ONE, ProtectionSignal.QuantityBase.CURRENT,
KlineSlopeRateStrategy.priceForReturn(context.entryPrice(), context.direction(), runtime.state.getFrozenSymbolStopLossRatio().negate(), context.leverage()),
"交易对绑定止损"));
        }
        return signals;
    }

    /**
     * 本地按与 Binance 相同的规范化触发价格判断，方向和杠杆均来自开仓冻结事实。
     */
    private List<ExitSignal> protectionExits(ActiveRuntime runtime, StrategyContext context) {
        if (!(runtime.plugin instanceof KlineSlopeRateStrategy)) {
            return runtime.plugin.evaluateExit(context, frozenParams(runtime));
        }
        if (!hasFrozenProtectionFacts(runtime)) {
            return List.of();
        }
        List<ProtectionSignal> signals = protectionSignals(runtime, context, frozenParams(runtime));
        BigDecimal hard = resolveHardStopRatio(runtime);
        if (hard != null) {
            signals.addFirst(new ProtectionSignal("HARD_STOP", "STOP_MARKET", BigDecimal.ONE, ProtectionSignal.QuantityBase.CURRENT,
             KlineSlopeRateStrategy.unifiedProtection(frozenParams(runtime)) && runtime.state.getStopLossPrice() != null ? runtime.state.getStopLossPrice()
                : KlineSlopeRateStrategy.priceForReturn(context.entryPrice(), context.direction(), hard.negate(), context.leverage()), "固定硬止损"));
        }
        for (ProtectionSignal signal : signals) {
            BigDecimal trigger = runtime.protectionRule == null ? signal.triggerPrice() : KlineSlopeRateStrategy.unifiedProtection(frozenParams(runtime))
                ? org.dromara.quant.exchange.QuantityNormalizer.protectionPrice(signal.triggerPrice(), runtime.protectionRule, context.direction())
                : org.dromara.quant.exchange.QuantityNormalizer.price(signal.triggerPrice(), runtime.protectionRule);
            boolean shortPosition = "SHORT".equalsIgnoreCase(context.direction());
            boolean profit = "TAKE_PROFIT_MARKET".equals(signal.orderType());
            boolean triggered = shortPosition == profit ? context.currentPrice().compareTo(trigger) <= 0 : context.currentPrice().compareTo(trigger) >= 0;
            if (!triggered) {
                continue;
            }
            String type = switch (signal.type()) {
                case "LOCAL_STOP" -> KlineSlopeRateStrategy.EXIT_LOCAL_STOP;
                case "FIRST_TP" -> KlineSlopeRateStrategy.EXIT_FIRST_TP;
                case "STEP_TP" -> KlineSlopeRateStrategy.EXIT_STEP_TP;
                default -> signal.type() + "_LOSS";
            };
            return List.of(new ExitSignal(type, signal.quantityRate(), "本地镜像保护触发，规范化价格=" + trigger, "FIRST_TP".equals(signal.type()), signal.type(),
                "FIRST_TP".equals(signal.type()) ? ExitSignal.QuantityBase.INITIAL : ExitSignal.QuantityBase.CURRENT));
        }
        return List.of();
    }

    /**
     * 根据当前持仓和实时价格计算退出信号。
     */
    private void evaluateExit(ActiveRuntime runtime, StrategyContext context) {
        synchronized (runtime) {
            List<ExitSignal> evaluated = protectionExits(runtime, context);
            if (runtime.position == null || runtime.hasPendingAction() || evaluated.isEmpty() || !runtime.exitCoordinating.compareAndSet(false, true)) {
                return;
            }
            String cycle = runtime.state == null ? null : runtime.state.getPositionCycleKey();
            runtime.exitTask = recoveryExecutor.submit(() -> {
                AtomicBoolean renewing = new AtomicBoolean(true);
                AtomicBoolean leaseHeld = new AtomicBoolean(true);
                Thread heartbeat = Thread.ofVirtual().name("quant-exit-lease").start(() -> {
                    try {
                        while (renewing.get()) {
                            Thread.sleep(5000);
                            if (renewing.get() && !leaseService.acquireOrRenew(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol())) {
                                leaseHeld.set(false);
                                break;
                            }
                        }
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    } catch (RuntimeException ex) {
                        leaseHeld.set(false);
                    }
                });
                try {
                    if (!"PAPER".equalsIgnoreCase(runtime.credential.getRunMode())) {
                        Thread.sleep(Math.max(0, properties.getProtection().getConfirmationWaitMillis()));
                    }
                    java.util.concurrent.Future<?> inFlightProtection = runtime.protectionTask;
                    if (inFlightProtection != null) {
                        try {
                            inFlightProtection.get(Math.max(1, properties.getProtection().getSyncWaitMillis()), java.util.concurrent.TimeUnit.MILLISECONDS);
                        } catch (java.util.concurrent.ExecutionException ex) { /* 失败事实由保护任务记录，兜底继续精确核验。 */ } catch (java.util.concurrent.TimeoutException ex) {
                            // 请求可能仍在交易所执行；不能取消任务后假定未发送，也不能并发补平。
                            throw new IllegalStateException("在途保护任务超时，释放退出协调并等待精确对账", ex);
                        }
                    }
                    synchronized (runtime) {
                        if (runtime.closed || runtime.state == null || !Objects.equals(cycle, runtime.state.getPositionCycleKey()) || runtime.hasPendingAction() || runtime.position == null) {
                            return;
                        }
                    }
                    if (!leaseHeld.get() || !leaseService.acquireOrRenew(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol())) {
                        return;
                    }
                    // 触发事实已经锁定；确认窗口中即便价格反弹仍完成该保护意图，不追用旧信号开仓。
                    executeProtectedExit(runtime, context, leaseHeld);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException ex) {
                    synchronized (runtime) {
                        if (!runtime.closed && runtime.state != null) {
                            runtime.state.setRecoveryRequired(true);
                            // 普通订单结果未知由执行路径保留 unsafe；等待保护核实或明确未发送的失败不能否定持仓归属。
                            stateService.save(runtime.state);
                        }
                    }
                    log.error("本地保护兜底等待恢复 instanceId={} symbol={}", runtime.instance.getId(), runtime.symbol.getSymbol(), ex);
                } finally {
                    renewing.set(false);
                    heartbeat.interrupt();
                    boolean interrupted = false;
                    for (; ; ) {
                        try {
                            heartbeat.join();
                            break;
                        } catch (InterruptedException ex) {
                            interrupted = true;
                        }
                    }
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                    synchronized (runtime) {
                        if (runtime.position != null && runtime.state != null && Objects.equals(cycle, runtime.state.getPositionCycleKey())) {
                            if (runtime.exitWindowHigh != null) {
                                updateRuntimeExtrema(runtime, runtime.exitWindowHigh);
                            }
                            if (runtime.exitWindowLow != null) {
                                updateRuntimeExtrema(runtime, runtime.exitWindowLow);
                            }
                        }
                        runtime.exitWindowHigh = null;
                        runtime.exitWindowLow = null;
                        runtime.exitCoordinating.set(false);
                        if (!runtime.closed && runtime.position != null && !runtime.hasPendingAction()) {
                            requestProtectionSync(runtime, true);
                        }
                    }
                    requestReconciliation(runtime);
                }
            });
        }
    }

    /**
     * 在持仓租约和新鲜账户事实有效时提交保护退出。
     */
    private void executeProtectedExit(ActiveRuntime runtime, StrategyContext context, AtomicBoolean leaseHeld) {
        List<ExitSignal> exits = protectionExits(runtime, context);
        if (exits.isEmpty() || runtime.position == null || runtime.hasPendingAction()) {
            return;
        }
        ExitSignal exit = exits.get(0);
        // 部分退出不能与仍在挂载的快照保护并行，否则撤掉旧条件单后可能被异步重新挂回。
        if (exit.quantityRate() != null && exit.quantityRate().compareTo(BigDecimal.ONE) < 0 && runtime.protectionSyncing.get()) {
            return;
        }
        if (handleExchangeProtectedExit(runtime, context, exit)) {
            return;
        }
        BigDecimal rate = exit.quantityRate() == null ? BigDecimal.ONE : exit.quantityRate().min(BigDecimal.ONE).max(BigDecimal.ZERO);
        // FIXED: 首次止盈按初始总仓位比例计算；其它退出按当前仓位。
        BigDecimal baseQty;
        if (exit.quantityBase() == ExitSignal.QuantityBase.INITIAL && runtime.state != null && runtime.state.getInitialPositionQty() != null && runtime.state.getInitialPositionQty().signum() > 0) {
            baseQty = runtime.state.getInitialPositionQty();
        } else {
            baseQty = runtime.position.getQuantity();
        }
        BigDecimal quantity = exit.markFirstTakeProfitTriggered() && runtime.state.getFirstTakeProfitTargetQty() != null ?
         PositionProtectionState.remainingFirstProfit(runtime.state).min(runtime.position.getQuantity()) : baseQty.multiply(rate).min(runtime.position.getQuantity()).setScale(16
         , RoundingMode.DOWN);
        if (runtime.protectionRule != null) {
            quantity = org.dromara.quant.exchange.QuantityNormalizer.quantity(quantity, runtime.protectionRule, "MARKET");
        }
        if (quantity.signum() <= 0) {
            return;
        }
        String direction = runtime.position.getPositionSide();
        String side = "SHORT".equalsIgnoreCase(direction) ? "BUY" : "SELL";
        Instant klineOpenTime = context.klineOpenTime() == null ? Instant.ofEpochMilli(System.currentTimeMillis()) : context.klineOpenTime();
        String idempotencyKey = nextExitAttemptKey(runtime, exit.type(), klineOpenTime);
        if (!leaseHeld.get() || !leaseService.acquireOrRenew(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol())) {
            throw new IllegalStateException("退出前租约失效，等待恢复");
        }
        QuantStrategyPositionState state = preparePendingStateForExit(runtime, exit.type(), quantity, klineOpenTime, idempotencyKey);
        runtime.state = state;
        // 先封锁平仓意图所在 K 线，覆盖平仓成交确认与持仓缓存刷新之间的竞态窗口。
        blockCloseCandle(runtime, klineOpenTime);
        recordTradeDecision(runtime, context, "CLOSE", direction, quantity, null, idempotencyKey, "REQUESTED", exit.type(), exit.reason(), null, false);
        BigDecimal entryPriceBeforeClose = runtime.position.getEntryPrice();
        String directionBeforeClose = runtime.position.getPositionSide();
        ExecutionResult executionResult = null;
        try {
            executionResult = executionService.closeByRuntime(runtime.credential.getId(), runtime.strategy.getId(), runtime.instance.getId(), runtime.symbol.getSymbol(), side,
             direction, quantity, idempotencyKey);
            recordTradeDecision(runtime, context, "CLOSE", direction, quantity, null, idempotencyKey, "ACCEPTED", "EXECUTION_RESULT", exit.reason(), executionResult, false);
            state.setPendingOrderId(executionResult.orderId());
            state.setPendingClientOrderId(executionResult.clientOrderId());
            runtime.state = state;
            if ("PAPER".equalsIgnoreCase(runtime.credential.getRunMode())) {
                PaperTradingService.PaperCloseSettlement settlement = paperTradingService.settlePaperExecution(runtime.credential.getId(), idempotencyKey,
                 runtime.position.getId(), quantity, context.currentPrice());
                recordTradeDecision(runtime, context, "CLOSE", direction, quantity, null, idempotencyKey, "FILLED", "EXECUTION_RESULT", exit.reason(), executionResult, true);
                runtime.position = settlement.position();
                BigDecimal profitRate = leveragedReturnRate(runtime, directionBeforeClose, context.currentPrice(), entryPriceBeforeClose);
                BigDecimal profitAmount = calculateProfitAmount(directionBeforeClose, context.currentPrice(), entryPriceBeforeClose, quantity);
                notifyCloseSafely(runtime, directionBeforeClose, quantity, entryPriceBeforeClose, context.currentPrice(), profitAmount, profitRate, exit.type(), exit.reason());
                blockCloseCandle(runtime, klineOpenTime);
                if (runtime.position == null) {
                    if (!cancelStaleProtection(runtime)) {
                        return;
                    }
                    stateService.markClosed(state);
                    runtime.position = null;
                    runtime.state = stateService.findByInstance(runtime.instance.getId());
                } else {
                    state.setPositionQty(runtime.position.getQuantity());
                    state.setStatus("OPEN");
                    if (exit.markFirstTakeProfitTriggered()) {
                        PositionProtectionState.confirmLocalFirstProfit(state, quantity);
                    }
                    clearPending(state);
                    stateService.save(state);
                }
            } else {
                BigDecimal previousQuantity = state.getPositionQty();
                runtime.position = loadPosition(runtime.credential, runtime.symbol.getSymbol(), executionService.toCredential(runtime.credential));
                if (executionResult == null || !"FILLED".equalsIgnoreCase(executionResult.status())) {
                    state.setStatus("EXIT_PENDING");
                    state.setRecoveryRequired(true);
                    state.setUnsafeRecovery(true);
                    stateService.save(state);
                    return;
                }
                blockCloseCandleForExecution(runtime, executionResult, klineOpenTime);
                blockCloseCandle(runtime, klineOpenTime);
                if (runtime.position == null) {
                    if (previousQuantity != null && previousQuantity.signum() > 0) {
                        BigDecimal profitRate = leveragedReturnRate(runtime, directionBeforeClose, context.currentPrice(), entryPriceBeforeClose);
                        BigDecimal profitAmount = calculateProfitAmount(directionBeforeClose, context.currentPrice(), entryPriceBeforeClose, previousQuantity);
                        notifyCloseSafely(runtime, directionBeforeClose, previousQuantity, entryPriceBeforeClose, context.currentPrice(), profitAmount, profitRate,
                            exit.type(), exit.reason());
                    }
                    if (!cancelStaleProtection(runtime)) {
                        return;
                    }
                    stateService.markClosed(state);
                    runtime.position = null;
                    runtime.state = stateService.findByInstance(runtime.instance.getId());
                } else if (executionResult == null || !"FILLED".equalsIgnoreCase(executionResult.status()) || previousQuantity == null || runtime.position.getQuantity().compareTo(previousQuantity.subtract(quantity).max(BigDecimal.ZERO)) > 0) {
                    state.setStatus("EXIT_PENDING");
                    state.setRecoveryRequired(true);
                    state.setUnsafeRecovery(true);
                    stateService.save(state);
                    return;
                } else {
                    BigDecimal closedQuantity = previousQuantity.subtract(runtime.position.getQuantity()).max(BigDecimal.ZERO);
                    if (closedQuantity.signum() > 0) {
                        BigDecimal profitRate = leveragedReturnRate(runtime, directionBeforeClose, context.currentPrice(), entryPriceBeforeClose);
                        BigDecimal profitAmount = calculateProfitAmount(directionBeforeClose, context.currentPrice(), entryPriceBeforeClose, closedQuantity);
                        notifyCloseSafely(runtime, directionBeforeClose, closedQuantity, entryPriceBeforeClose, context.currentPrice(), profitAmount, profitRate,
                            exit.type(), exit.reason());
                    }
                    state.setStatus("OPEN");
                    state.setPositionQty(runtime.position.getQuantity());
                    if (exit.markFirstTakeProfitTriggered()) {
                        PositionProtectionState.confirmLocalFirstProfit(state, quantity);
                    }
                    clearPending(state);
                    state.setRecoveryRequired(false);
                    state.setUnsafeRecovery(false);
                    stateService.save(state);
                }
            }
            if (exit.markFirstTakeProfitTriggered() && runtime.state != null && runtime.position != null) {
                runtime.state.setPositionQty(runtime.position.getQuantity());
                runtime.state.setStatus("OPEN");
                stateService.save(runtime.state);
                ensureHardStop(runtime);
                syncPluginProtections(runtime, buildContext(runtime, context.currentPrice(), new ExchangeGateway.MarketEvent("TRADE", runtime.symbol.getSymbol(),
                    runtime.strategySymbol.getIntervalCode(), context.currentPrice(), null, context.eventTime())));
            }
            log.info("实时策略平仓 instanceId={} symbol={} type={} price={} quantity={} reason={}", runtime.instance.getId(), runtime.symbol.getSymbol(), exit.type(),
            context.currentPrice(), quantity, exit.reason());
        } catch (RuntimeException ex) {
            QuantExecutionRecordStatus status = findExecutionStatus(runtime.credential.getId(), idempotencyKey);
            recordTradeDecision(runtime, context, "CLOSE", direction, quantity, null, idempotencyKey, "FAILED".equals(status.value()) ? "FAILED" : "UNKNOWN", "EXECUTION_FAILURE"
                , org.dromara.quant.decision.TradeDecisionLogService.safeReason(ex), executionResult, false);
            if (executionResult == null && "FAILED".equals(status.value())) {
                state.setStatus("OPEN");
                clearPending(state);
            } else {
                state.setStatus("EXIT_PENDING");
                state.setRecoveryRequired(true);
                state.setUnsafeRecovery(!"PAPER".equalsIgnoreCase(runtime.credential.getRunMode()));
            }
            stateService.save(state);
            throw ex;
        }
    }

    /** 只有已明确失败或已终结的部分减仓才生成下一次尝试；未知请求继续沿用原身份核实。 */
    private String nextExitAttemptKey(ActiveRuntime runtime, String action, Instant candle) {
        String base = buildIdempotencyKey(runtime, action, candle);
        QuantExecutionRecord previous = executionRecordService.find(runtime.credential.getId(), base);
        if (previous == null) {
            return base;
        }
        boolean failed = "FAILED".equals(previous.getExecutionStatus());
        boolean terminal = Set.of("CONFIRMED", "RECOVERED").contains(Objects.toString(previous.getExecutionStatus(), ""))
            && Set.of("FILLED", "CANCELED", "EXPIRED", "REJECTED").contains(Objects.toString(previous.getExchangeOrderStatus(), ""));
        // 重试键先随 pending 意图持久化。重启后仍恢复原尝试，不能因为重新生成 UUID 而重发未知请求。
        if (!runtime.hasPendingAction() && (failed || terminal && !Boolean.TRUE.equals(runtime.state.getUnsafeRecovery()))) {
            return base + ":RETRY:" + UUID.randomUUID();
        }
        throw new IllegalStateException("原平仓执行尚未完成恢复核实，禁止生成新的减仓尝试");
    }

    /**
     * 对已经挂载 Binance 条件单的退出信号，本地 MARKET 仅作为 fallback：
     * 保护单仍在交易所工作时不重复平仓；保护单已经触发时先以交易所持仓事实更新本地状态。
     */
    private boolean handleExchangeProtectedExit(ActiveRuntime runtime, StrategyContext context, ExitSignal exit) {
        boolean live = !"PAPER".equalsIgnoreCase(runtime.credential.getRunMode());
        if (!live || exit.exchangeProtectionType() == null || exit.exchangeProtectionType().isBlank() || runtime.position == null) {
            return false;
        }
        runtime.position.setProtectionCycleKey(runtime.state.getPositionCycleKey());
        if (!protectionOrderService.prepareLocalExit(runtime.credential.getId(), runtime.position, exit.exchangeProtectionType())) {
            runtime.state.setRecoveryRequired(true);
            // 仍未确认停单时绝不发补平，但保护未知不等于原持仓归属发生变化。
            stateService.save(runtime.state);
            return true;
        }
        BigDecimal firstExecuted = protectionOrderService.firstTakeProfitExecutedQuantity(runtime.credential.getId(), runtime.position);

        QuantPosition before = runtime.position;
        QuantPosition refreshed;
        try {
            refreshed = loadPosition(runtime.credential, runtime.symbol.getSymbol(), executionService.toCredential(runtime.credential));
        } catch (RuntimeException ex) {
            // 保护单状态与持仓事实都无法确认时绝不盲目再发一笔减仓单，等待 REST/UserData 恢复。
            if (runtime.state != null) {
                runtime.state.setRecoveryRequired(true);
                stateService.save(runtime.state);
            }
            return true;
        }
        if (refreshed == null) {
            blockCloseCandle(runtime, context.klineOpenTime());
            runtime.position = null;
            recordPositionDecrease(runtime, before, before.getQuantity(), "持仓关闭；本地同时满足退出条件：" + exit.reason());
            BigDecimal closePrice = context.currentPrice();
            BigDecimal profitRate = leveragedReturnRate(runtime, before.getPositionSide(), closePrice, before.getEntryPrice());
            BigDecimal profitAmount = calculateProfitAmount(before.getPositionSide(), closePrice, before.getEntryPrice(), before.getQuantity());
            notifyCloseSafely(runtime, before.getPositionSide(), before.getQuantity(), before.getEntryPrice(), closePrice, profitAmount, profitRate,
                "EXCHANGE_" + exit.type(), "Binance 条件单已执行并关闭持仓：" + exit.reason());
            if (!cancelStaleProtection(runtime)) {
                return true;
            }
            if (runtime.state != null) {
                stateService.markClosed(runtime.state);
                runtime.state = stateService.findByInstance(runtime.instance.getId());
            }
            return true;
        }
        BigDecimal totalFirst = firstExecuted.add(runtime.state.getFirstTakeProfitLocalExecutedQty() == null ? BigDecimal.ZERO :
runtime.state.getFirstTakeProfitLocalExecutedQty());
        if (runtime.state.getInitialPositionQty() != null && refreshed.getQuantity().compareTo(runtime.state.getInitialPositionQty().subtract(totalFirst).max(BigDecimal.ZERO)) > 0) {
            runtime.state.setRecoveryRequired(true);
            runtime.state.setUnsafeRecovery(true);
            stateService.save(runtime.state);
            return true;
        }
        PositionProtectionState.confirmExchangeFirstProfit(runtime.state, firstExecuted);
        stateService.save(runtime.state);
        if (before.getQuantity() != null && refreshed.getQuantity() != null && refreshed.getQuantity().compareTo(before.getQuantity()) < 0) {
            BigDecimal closedQty = before.getQuantity().subtract(refreshed.getQuantity());
            recordPositionDecrease(runtime, before, closedQty, "保护条件单已确认成交，按剩余目标补平");

            BigDecimal closePrice = context.currentPrice() != null ? context.currentPrice() : (refreshed.getMarkPrice() != null ? refreshed.getMarkPrice() :
before.getEntryPrice());
            BigDecimal profitRate = leveragedReturnRate(runtime, before.getPositionSide(), closePrice, before.getEntryPrice());
            BigDecimal profitAmount = calculateProfitAmount(before.getPositionSide(), closePrice, before.getEntryPrice(), closedQty);

            notifyCloseSafely(runtime, before.getPositionSide(), closedQty, before.getEntryPrice(), closePrice, profitAmount, profitRate, "EXCHANGE_" + exit.type(),
                "Binance 条件单已执行部分减仓：" + exit.reason());
        }
        if (before.getQuantity() != null && refreshed.getQuantity() != null && refreshed.getQuantity().compareTo(before.getQuantity()) > 0) {
            // 出现外部加仓/事实异常时不按旧数量继续 fallback，交给恢复链路处理。
            runtime.position = refreshed;
            if (runtime.state != null) {
                runtime.state.setRecoveryRequired(true);
                stateService.save(runtime.state);
            }
            return true;
        }
        runtime.position = refreshed;
        runtime.position.setProtectionCycleKey(runtime.state.getPositionCycleKey());
        runtime.state.setPositionQty(refreshed.getQuantity());
        if (exit.markFirstTakeProfitTriggered() && Boolean.TRUE.equals(runtime.state.getFirstTakeProfitTriggered())) {
            return true;
        }
        return false;
    }

    /**
     * 构建不暴露基础设施依赖的 StrategyContext 快照。
     */
    private StrategyContext buildContext(ActiveRuntime runtime, BigDecimal price, ExchangeGateway.MarketEvent event) {
        QuantPosition position = runtime.position;
        int leverage = runtime.state != null && runtime.state.getFrozenLeverage() != null ? runtime.state.getFrozenLeverage() : runtime.strategySymbol.getLeverage() == null ?
         runtime.strategy.getDefaultLeverage() : runtime.strategySymbol.getLeverage();
        return new StrategyContext(runtime.symbol.getSymbol(), position == null ? null : position.getPositionSide(), position == null ? null :
 runtime.state != null && runtime.state.getEntryPrice() != null ? runtime.state.getEntryPrice() : position.getEntryPrice(), price, runtime.market.referencePrice(),
runtime.market.previousKlineOpenPrice(), runtime.market.currentKlineOpen(), runtime.market.currentKlineClosed(), event.eventTime() == null ? Instant.now() :
           event.eventTime(), position == null ? BigDecimal.ZERO : position.getQuantity(), runtime.state == null || runtime.state.getInitialPositionQty() == null ?
            (position == null ? BigDecimal.ZERO : position.getQuantity()) : runtime.state.getInitialPositionQty(), position == null ? BigDecimal.ZERO : position.getMargin(),
             leverage, runtime.state == null ? null : runtime.state.getHighestPriceSinceEntry(), runtime.state == null ? null : runtime.state.getLowestPriceSinceEntry(),
            runtime.state != null && Boolean.TRUE.equals(runtime.state.getFirstTakeProfitTriggered()), runtime.state == null || runtime.state.getHighestStep() == null ? 0 :
               runtime.state.getHighestStep());
    }

    /**
     * 读取开仓时冻结的策略参数。
     */
    private Map<String, Object> frozenParams(ActiveRuntime runtime) {
        if (runtime.state == null || runtime.state.getStrategyParamsSnapshot() == null || runtime.state.getStrategyParamsSnapshot().isBlank()) {
            Map<String, Object> legacy = new java.util.LinkedHashMap<>(runtime.params);
            legacy.remove(KlineSlopeRateStrategy.PROTECTION_POLICY_VERSION_KEY);
            return legacy;
        }
        Map<String, Object> frozen = parseParams(runtime.state.getStrategyParamsSnapshot());
        if (runtime.plugin instanceof KlineSlopeRateStrategy && frozen.containsKey(KlineSlopeRateStrategy.PROTECTION_POLICY_VERSION_KEY) && !KlineSlopeRateStrategy.unifiedProtection(frozen)) {
            throw new IllegalStateException("不支持的持仓保护规则版本，保留已有保护并等待核实");
        }
        return frozen;
    }

    /**
     * 在发送开仓执行前持久化待确认动作。
     */
    private QuantStrategyPositionState preparePendingStateForOpen(ActiveRuntime runtime, String direction, BigDecimal quantity, BigDecimal price, Instant klineOpenTime,
     String idempotencyKey) {
        QuantStrategyPositionState state = runtime.state == null ? new QuantStrategyPositionState() : runtime.state;
        state.setInstanceId(runtime.instance.getId());
        invalidateEntryProtectionCleanup(runtime);
        // This state now represents a new opening cycle; detach the previous closed position before snapshot reconciliation.
        state.setPositionId(null);
        PositionProtectionState.startCycle(state, runtime.strategySymbol.getLeverage() == null ? runtime.strategy.getDefaultLeverage() : runtime.strategySymbol.getLeverage(),
            runtime.strategySymbol.getStopLossRatio(), price);
        stateService.resetClosedMaximumReturnRate(runtime.instance.getId());
        runtime.lastProtectionTarget = null;
        runtime.partialOpenProtectionVerified = false;
        state.setStatus("OPEN_PENDING");
        state.setDirection(direction);
        state.setEntryPrice(price);
        state.setPositionQty(quantity);
        state.setInitialPositionQty(quantity);
        state.setMaximumReturnRate(BigDecimal.ZERO);
        runtime.maximumReturnRatePersistencePending = false;
        state.setInitialMargin(price.multiply(quantity).divide(BigDecimal.valueOf(Math.max(1, runtime.strategySymbol.getLeverage() == null ?
         runtime.strategy.getDefaultLeverage() : runtime.strategySymbol.getLeverage())), 16, RoundingMode.HALF_UP));
        Map<String, Object> snapshotParams = new java.util.LinkedHashMap<>(runtime.params);
        snapshotParams.remove(KlineSlopeRateStrategy.PROTECTION_POLICY_VERSION_KEY);
        if (runtime.plugin instanceof KlineSlopeRateStrategy) {
            snapshotParams.put(KlineSlopeRateStrategy.PROTECTION_POLICY_VERSION_KEY, 2);
        }
        state.setStrategyParamsSnapshot(writeParams(snapshotParams));
        state.setFrozenStopLossRatio(runtime.plugin instanceof KlineSlopeRateStrategy
            ? KlineSlopeRateStrategy.localStopLossRate(snapshotParams) : resolveHardStopRatio(runtime));
        state.setPendingAction("OPEN");
        state.setPendingIdempotencyKey(idempotencyKey);
        state.setPendingOrderTime(System.currentTimeMillis());
        state.setPendingNotFoundConfirmations(0);
        state.setRecoveryRequired(false);
        state.setUnsafeRecovery(false);
        stateService.save(state);
        return state;
    }

    /**
     * 在发送退出执行前持久化待确认动作。
     */
    private QuantStrategyPositionState preparePendingStateForExit(ActiveRuntime runtime, String type, BigDecimal quantity, Instant klineOpenTime, String idempotencyKey) {
        QuantStrategyPositionState state = runtime.state == null ? new QuantStrategyPositionState() : runtime.state;
        state.setInstanceId(runtime.instance.getId());
        state.setStatus("EXIT_PENDING");
        state.setPositionId(runtime.position.getId());
        state.setDirection(runtime.position.getPositionSide());
        state.setPositionQty(runtime.position.getQuantity());
        state.setPendingAction(type);
        state.setPendingIdempotencyKey(idempotencyKey);
        state.setPendingOrderTime(System.currentTimeMillis());
        state.setPendingNotFoundConfirmations(0);
        stateService.save(state);
        return state;
    }

    /**
     * 恢复 OPEN/CLOSE 的未知执行状态。订单仍在交易所活动态时绝不能把本地状态强行关闭。
     */
    private boolean reconcilePendingExecution(ActiveRuntime runtime) {
        QuantStrategyPositionState expectedState;
        List<Object> expectedPending;
        String expectedDirection;
        synchronized (runtime) {
            if (runtime.closed || !runtime.hasPendingAction()) {
                return false;
            }
            if ("PAPER".equalsIgnoreCase(runtime.credential.getRunMode())) {
                return reconcilePaperPendingExecution(runtime);
            }
            expectedState = runtime.state;
            expectedPending = pendingIdentity(expectedState);
            expectedDirection = expectedState.getDirection();
        }
        // 网络读取不持有行情锁；只有读取完成且待确认身份未变时才提交恢复结果。
        String key = (String) expectedPending.get(1);
        QuantExecutionRecord execution = key == null || key.isBlank() ? null : executionRecordService.find(runtime.credential.getId(), key);
        Long orderId = (Long) expectedPending.get(2);
        String clientOrderId = (String) expectedPending.get(3);
        if (execution != null) {
            if (orderId == null) {
                orderId = execution.getExchangeOrderId();
            }
            if (clientOrderId == null || clientOrderId.isBlank()) {
                clientOrderId = execution.getClientOrderId();
            }
        }
        var credential = executionService.toCredential(runtime.credential);
        ExchangeGateway.OrderSnapshot order = null;
        String orderQueryFailure = null;
        boolean exactOrderQueryAttempted = orderId != null || clientOrderId != null && !clientOrderId.isBlank();
        if (exactOrderQueryAttempted) {
            try {
                order = gateway.queryOrder(credential, runtime.symbol.getSymbol(), orderId, clientOrderId);
            } catch (RuntimeException ex) {
                // 查询异常不是“订单不存在”；记录诊断并保持 UNKNOWN，不因异常尝试重发交易。
                orderQueryFailure = ex.getClass().getSimpleName();
            }
            // 当前活动订单列表只是精确订单查询的备用路径，未找到时不必每 5 秒重复扫描。
            // 每 3 次精确查询才扫描一次活动订单；历史订单则每 6 次查询一次，以控制 REST 权重。
            int priorNotFound = expectedState.getPendingNotFoundConfirmations() == null ? 0 : expectedState.getPendingNotFoundConfirmations();
            if (order == null && orderQueryFailure == null && (priorNotFound + 1) % 3 == 0) {
                try {
                    order = findPendingOrderInOpenOrders(credential, runtime.symbol.getSymbol(), orderId, clientOrderId);
                } catch (RuntimeException ex) {
                    orderQueryFailure = "openOrders " + ex.getClass().getSimpleName();
                }
            }
            // 当前活动订单只覆盖仍处于活动态的订单。若下单响应丢失而订单已成交/撤销，
            // 仅反复查活动订单会永久停留在 UNKNOWN。每 6 次未找到才查询历史，且只接受
            // 交易对、订单 ID、客户订单 ID 完全匹配的正向事实。
            if (order == null && orderQueryFailure == null && (priorNotFound + 1) % 6 == 0) {
                try {
                    order = findPendingOrderInHistory(credential, runtime.symbol.getSymbol(), orderId, clientOrderId,
                        expectedState.getPendingOrderTime());
                } catch (RuntimeException ex) {
                    orderQueryFailure = "orderHistory " + ex.getClass().getSimpleName();
                }
            }
        }
        List<ExchangeGateway.PositionSnapshot> snapshots = null;
        if (order == null || "NEW".equalsIgnoreCase(order.status()) || "PARTIALLY_FILLED".equalsIgnoreCase(order.status()) || "FILLED".equalsIgnoreCase(order.status())
            || Set.of("CANCELED", "EXPIRED", "REJECTED").contains(order.status() == null ? "" : order.status().toUpperCase(java.util.Locale.ROOT))) {
            // 订单查询之后再读取持仓，防止用成交之前的快照解除待确认保护。
            snapshots = reconciliationService.refreshSymbolPosition(runtime.credential, runtime.symbol.getSymbol(), credential);
        }
        boolean closedOpenProven = false;
        if (order != null && "FILLED".equalsIgnoreCase(order.status()) && "OPEN".equals(expectedPending.get(0)) && snapshots != null && snapshots.stream().allMatch(p -> p.quantity() != null && p.quantity().signum() == 0)) {
            Instant from = expectedPending.get(4) instanceof Long time ? Instant.ofEpochMilli(time) : order.updateTime();
            Instant to = Instant.now();
            // 成交接口窗口有限；缺少完整历史或双向持仓归属时保留 pending，不能仅凭空仓放行。
            ExchangeGateway.PositionMode positionMode = gateway.queryPositionMode(credential);
            if (from != null && !from.isAfter(to) && from.isAfter(to.minusSeconds(6 * 24 * 3600)) && positionMode != null && !positionMode.hedgeMode()) {
                var trades = gateway.queryTrades(credential, runtime.symbol.getSymbol(), new ExchangeGateway.TimeRange(from, to));
                closedOpenProven = closedPendingOpenProven(order, expectedDirection, trades);
                if (closedOpenProven) {
                    // 成交证明读取之后再次确认空仓，避免使用旧的持仓快照提交恢复。
                    snapshots = reconciliationService.refreshSymbolPosition(runtime.credential, runtime.symbol.getSymbol(), credential);
                    closedOpenProven =
snapshots != null && snapshots.stream().allMatch(p -> runtime.symbol.getSymbol().equalsIgnoreCase(p.symbol()) && p.quantity() != null && p.quantity().signum() == 0);
                }
            }
        }
        runtime.lastPendingOrderQueryFailure = orderQueryFailure;
        if (decisionLogs != null && order != null) {
            decisionLogs.order(runtime.credential.getId(), order);
        }
        synchronized (runtime) {
            if (runtime.closed || runtime.state != expectedState || !Objects.equals(expectedPending, pendingIdentity(runtime.state))) {
                runtime.reconcileRequested.set(true);
                return true;
            }
            if (!leaseService.acquireOrRenew(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol())) {
                return true;
            }
            return applyPendingExecution(runtime, execution, order, snapshots, closedOpenProven);
        }
    }

    /**
     * 精确查询返回订单不存在时，扫描当前活动订单作为只读备用发现路径。
     * 只有交易所返回的订单身份与本地待确认身份完全匹配才接受；该路径不会改变 UNKNOWN 的终态判定。
     */
    private ExchangeGateway.OrderSnapshot findPendingOrderInOpenOrders(ExchangeGateway.Credential credential, String symbol,
                                                                        Long orderId, String clientOrderId) {
        if ((orderId == null) && (clientOrderId == null || clientOrderId.isBlank())) {
            return null;
        }
        List<ExchangeGateway.OrderSnapshot> openOrders = gateway.queryOrders(credential, symbol);
        if (openOrders == null || openOrders.isEmpty()) {
            return null;
        }
        List<ExchangeGateway.OrderSnapshot> matches = openOrders.stream()
            .filter(Objects::nonNull)
            .filter(candidate -> candidate.symbol() != null && candidate.symbol().equalsIgnoreCase(symbol))
            .filter(candidate -> candidate.orderId() != null)
            .filter(candidate -> orderId == null || orderId.equals(candidate.orderId()))
            .filter(candidate -> clientOrderId == null || clientOrderId.isBlank()
                || clientOrderId.equals(candidate.clientOrderId()))
            .toList();
        if (matches.size() != 1) {
            if (!matches.isEmpty()) {
                log.warn("待确认订单备用发现结果不唯一，继续保持 UNKNOWN symbol={} orderId={} clientOrderId={} matches={}",
                    symbol, orderId, clientOrderId, matches.size());
            }
            return null;
        }
        ExchangeGateway.OrderSnapshot matched = matches.get(0);
        log.info("待确认订单通过活动订单列表精确发现 symbol={} orderId={} clientOrderId={} status={}",
            symbol, matched.orderId(), matched.clientOrderId(), matched.status());
        return matched;
    }

    /**
     * 在活动订单未发现时，从交易所历史订单中按完整订单身份补偿恢复。
     * 不能把历史接口成功但未匹配到订单当作“订单肯定未发送”；此方法只补充正向事实。
     */
    private ExchangeGateway.OrderSnapshot findPendingOrderInHistory(ExchangeGateway.Credential credential, String symbol,
                                                                      Long orderId, String clientOrderId, Long pendingOrderTime) {
        if (orderId == null && (clientOrderId == null || clientOrderId.isBlank())) {
            return null;
        }
        Instant now = Instant.now();
        Instant from = pendingOrderTime == null
            ? now.minusSeconds(24 * 3600L)
            : Instant.ofEpochMilli(pendingOrderTime).minusSeconds(60L);
        // Binance 历史订单查询窗口有上限；超出可查询范围时不能将“未找到”解释成终态。
        if (from.isBefore(now.minusSeconds(6 * 24 * 3600L))) {
            from = now.minusSeconds(6 * 24 * 3600L);
        }
        List<ExchangeGateway.OrderSnapshot> history = gateway.queryOrderHistory(
            credential, symbol, new ExchangeGateway.TimeRange(from, now), 1000L);
        if (history == null || history.isEmpty()) {
            return null;
        }
        List<ExchangeGateway.OrderSnapshot> matches = history.stream()
            .filter(Objects::nonNull)
            .filter(candidate -> candidate.symbol() != null && candidate.symbol().equalsIgnoreCase(symbol))
            .filter(candidate -> candidate.orderId() != null)
            .filter(candidate -> orderId == null || orderId.equals(candidate.orderId()))
            .filter(candidate -> clientOrderId == null || clientOrderId.isBlank()
                || clientOrderId.equals(candidate.clientOrderId()))
            .toList();
        if (matches.size() != 1) {
            if (!matches.isEmpty()) {
                log.warn("待确认订单历史发现结果不唯一，继续保持 UNKNOWN symbol={} orderId={} clientOrderId={} matches={}",
                    symbol, orderId, clientOrderId, matches.size());
            }
            return null;
        }
        ExchangeGateway.OrderSnapshot matched = matches.get(0);
        log.info("待确认订单通过历史订单精确发现 symbol={} orderId={} clientOrderId={} status={}",
            symbol, matched.orderId(), matched.clientOrderId(), matched.status());
        return matched;
    }

    private List<Object> pendingIdentity(QuantStrategyPositionState state) {
        return Arrays.asList(state.getPendingAction(), state.getPendingIdempotencyKey(), state.getPendingOrderId(), state.getPendingClientOrderId(), state.getPendingOrderTime(),
         state.getStatus(), state.getPositionQty(), state.getPendingNotFoundConfirmations());
    }

    /**
     * 完整单向开平仓成交证明；不接受缺失、截断或其他开仓订单混入的历史。
     */
    private boolean closedPendingOpenProven(ExchangeGateway.OrderSnapshot order, String direction, List<ExchangeGateway.TradeSnapshot> trades) {
        if (order.orderId() == null || order.executedQuantity() == null || order.executedQuantity().signum() <= 0 || !Set.of("LONG", "SHORT").contains(direction == null ? "" :
         direction) || trades == null || trades.isEmpty() || trades.size() >= 1000) {
            return false;
        }
        String openingSide = "LONG".equals(direction) ? "BUY" : "SELL";
        if (!openingSide.equalsIgnoreCase(order.side())) {
            return false;
        }
        Set<Long> seen = new HashSet<>();
        for (var trade : trades) {
            if (trade == null || trade.tradeId() == null || !seen.add(trade.tradeId()) || trade.orderId() == null || trade.time() == null || !order.symbol().equalsIgnoreCase(trade.symbol()) || trade.quantity() == null || trade.quantity().signum() <= 0 || !("BUY".equalsIgnoreCase(trade.side()) || "SELL".equalsIgnoreCase(trade.side()))) {
                return false;
            }
        }
        var ordered = trades.stream().sorted(Comparator.comparing(ExchangeGateway.TradeSnapshot::time).thenComparing(ExchangeGateway.TradeSnapshot::tradeId)).toList();
        BigDecimal opened = BigDecimal.ZERO;
        BigDecimal remaining = BigDecimal.ZERO;
        for (var trade : ordered) {
            if (openingSide.equalsIgnoreCase(trade.side())) {
                if (!order.orderId().equals(trade.orderId())) {
                    return false;
                }
                opened = opened.add(trade.quantity());
                remaining = remaining.add(trade.quantity());
            } else {
                if (order.orderId().equals(trade.orderId())) {
                    return false;
                }
                remaining = remaining.subtract(trade.quantity());
                if (remaining.signum() < 0) {
                    return false;
                }
            }
        }
        return opened.compareTo(order.executedQuantity()) == 0 && remaining.signum() == 0;
    }

    /**
     * 根据已确认的订单与成交推进等待中的策略动作。
     */
    private boolean applyPendingExecution(ActiveRuntime runtime, QuantExecutionRecord execution, ExchangeGateway.OrderSnapshot order,
     List<ExchangeGateway.PositionSnapshot> snapshots) {
        return applyPendingExecution(runtime, execution, order, snapshots, false);
    }

    private boolean applyPendingExecution(ActiveRuntime runtime, QuantExecutionRecord execution, ExchangeGateway.OrderSnapshot order,
                                          List<ExchangeGateway.PositionSnapshot> snapshots, boolean closedOpenProven) {
        if (!runtime.hasPendingAction() || runtime.state == null) {
            return false;
        }
        if ("PAPER".equalsIgnoreCase(runtime.credential.getRunMode())) {
            return reconcilePaperPendingExecution(runtime);
        }
        ExchangeGateway.Credential credential = executionService.toCredential(runtime.credential);
        String pendingKey = runtime.state.getPendingIdempotencyKey();
        if (pendingKey != null && !pendingKey.isBlank()) {
            if (execution != null) {
                boolean changed = false;
                if (runtime.state.getPendingOrderId() == null && execution.getExchangeOrderId() != null) {
                    runtime.state.setPendingOrderId(execution.getExchangeOrderId());
                    changed = true;
                }
                if ((runtime.state.getPendingClientOrderId() == null || runtime.state.getPendingClientOrderId().isBlank()) && execution.getClientOrderId() != null && !execution.getClientOrderId().isBlank()) {
                    runtime.state.setPendingClientOrderId(execution.getClientOrderId());
                    changed = true;
                }
                if (changed) {
                    stateService.save(runtime.state);
                }
            }
        }
        if (runtime.state.getPendingOrderId() == null && (runtime.state.getPendingClientOrderId() == null || runtime.state.getPendingClientOrderId().isBlank())) {
            runtime.position = loadPosition(runtime.credential, runtime.symbol.getSymbol(), credential, snapshots);
            if (runtime.position != null && "OPEN".equalsIgnoreCase(runtime.state.getPendingAction())) {
                // 只有持仓不是本次订单成交证明；缺少订单身份时保留 pending，不猜测初始数量或归属。
                runtime.state.setRecoveryRequired(true);
                runtime.state.setUnsafeRecovery(true);
                stateService.save(runtime.state);
                return true;
            }
            if (execution != null && "FAILED".equalsIgnoreCase(execution.getExecutionStatus())) {
                if ("OPEN".equalsIgnoreCase(runtime.state.getPendingAction())) {
                    releaseFailedOpenEntryGuard(runtime);
                    stateService.markClosed(runtime.state);
                } else {
                    clearPending(runtime.state);
                    runtime.state.setStatus("OPEN");
                    stateService.save(runtime.state);
                }
                return true;
            }
            runtime.state.setRecoveryRequired(true);
            runtime.state.setUnsafeRecovery("OPEN".equalsIgnoreCase(runtime.state.getPendingAction()));
            stateService.save(runtime.state);
            return true;
        }
        if (order == null) {
            if (snapshots != null && !snapshots.isEmpty()) {
                // 查不到订单不代表没有敞口：刷新实际持仓，外层继续挂载硬止损，保留待确认身份。
                runtime.position = loadPosition(runtime.credential, runtime.symbol.getSymbol(), credential, snapshots);
            }
            if (runtime.lastPendingOrderQueryFailure != null) {
                // A timeout, rate limit, or other query error is not evidence that the order
                // was absent. Keep the original pending identity and recovery block; do not
                // increase not-found confirmations based on a failed read.
                runtime.state.setRecoveryRequired(true);
                stateService.save(runtime.state);
                long now = System.currentTimeMillis();
                if (now - runtime.lastPendingOrderQueryFailureLogAt >= 60_000L) {
                    runtime.lastPendingOrderQueryFailureLogAt = now;
                    log.warn("待确认订单查询失败，保留 UNKNOWN 身份且不累计未找到次数 instanceId={} credentialId={} symbol={} runMode={} orderId={} clientOrderId={} queryFailure={}",
                        runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(), runtime.credential.getRunMode(),
                        runtime.state.getPendingOrderId(), runtime.state.getPendingClientOrderId(), runtime.lastPendingOrderQueryFailure);
                }
                return true;
            }
            int confirmations = runtime.state.getPendingNotFoundConfirmations() == null ? 0 : runtime.state.getPendingNotFoundConfirmations();
            confirmations++;
            runtime.state.setPendingNotFoundConfirmations(confirmations);
            if (confirmations < 3) {
                stateService.save(runtime.state);
                return true;
            }
            if ("OPEN".equalsIgnoreCase(runtime.state.getPendingAction()) && execution != null && "FAILED".equalsIgnoreCase(execution.getExecutionStatus()) && snapshots != null && snapshots.isEmpty() && !runtime.protectionSyncing.get() && !runtime.protectionCleanupPending) {
                runtime.position = null;
                releaseFailedOpenEntryGuard(runtime);
                stateService.markClosed(runtime.state);
                log.info("待确认开仓恢复完成：执行已明确失败且交易所空仓 instanceId={} symbol={}", runtime.instance.getId(), runtime.symbol.getSymbol());
                return true;
            }
            runtime.state.setRecoveryRequired(true);
            stateService.save(runtime.state);
            // UNKNOWN 可能是交易所请求已受理但响应丢失，不能仅凭多次未找到就清除保护。
            // 将重复诊断降为每 6 次一次，避免恢复卡住时持续刷 WARN；每次恢复仍会查询订单事实。
            if (confirmations % 6 == 0) {
                log.warn("待确认订单仍未在活动/历史订单中找到，保留交易保护 instanceId={} credentialId={} symbol={} runMode={} pendingAction={} executionStatus={} executionId={} idempotencyKey={} pendingOrderId={} pendingClientOrderId={} positionCount={} orderQueryFailure={} notFoundConfirmations={}",
                    runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(), runtime.credential.getRunMode(),
                    runtime.state.getPendingAction(), execution == null ? "MISSING" : execution.getExecutionStatus(),
                    execution == null ? null : execution.getId(), runtime.state.getPendingIdempotencyKey(),
                    runtime.state.getPendingOrderId(), runtime.state.getPendingClientOrderId(),
                    snapshots == null ? null : snapshots.size(), runtime.lastPendingOrderQueryFailure, confirmations);
            }
            return true;
        }
        String status = order.status();
        if ("NEW".equalsIgnoreCase(status) || "PARTIALLY_FILLED".equalsIgnoreCase(status)) {
            runtime.position = loadPosition(runtime.credential, runtime.symbol.getSymbol(), credential, snapshots);
            runtime.partialOpenProtectionVerified = false;
            if ("OPEN".equals(runtime.state.getPendingAction()) && order.executedQuantity() != null && order.executedQuantity().signum() > 0
                && runtime.position != null && runtime.state.getDirection() != null
                && runtime.state.getDirection().equalsIgnoreCase(runtime.position.getPositionSide())
                && runtime.symbol.getSymbol().equalsIgnoreCase(order.symbol())
                && ("LONG".equalsIgnoreCase(runtime.state.getDirection()) ? "BUY" : "SELL").equalsIgnoreCase(order.side())
                && runtime.position.getQuantity().compareTo(order.executedQuantity()) == 0
                && order.orderId() != null && order.orderId().equals(runtime.state.getPendingOrderId())
                && (runtime.state.getPendingClientOrderId() == null || runtime.state.getPendingClientOrderId().equals(order.clientOrderId()))
                && runtime.position.getEntryPrice() != null && runtime.position.getEntryPrice().signum() > 0
                && runtime.state.getPositionCycleKey() != null && runtime.state.getFrozenLeverage() != null
                && runtime.state.getFrozenLeverage() > 0) {
                runtime.state.setPositionId(runtime.position.getId());
                runtime.state.setPositionQty(runtime.position.getQuantity());
                runtime.state.setEntryPrice(runtime.position.getEntryPrice());
                freezeOrderEntryPrice(runtime.state, order, true);
                runtime.partialOpenProtectionVerified = runtime.state.getEntryPrice() != null && runtime.state.getEntryPrice().signum() > 0;
                runtime.position.setProtectionCycleKey(runtime.state.getPositionCycleKey());
            }
            runtime.state.setPendingNotFoundConfirmations(0);
            stateService.save(runtime.state);
            return true;
        }
        if ("FILLED".equalsIgnoreCase(status)) {
            runtime.state.setPendingNotFoundConfirmations(0);
            runtime.position = loadPosition(runtime.credential, runtime.symbol.getSymbol(), credential, snapshots);
            if (!"OPEN".equalsIgnoreCase(runtime.state.getPendingAction())) {
                blockCloseCandleAtExchangeTime(runtime, order.updateTime());
            }
            if (runtime.position != null) {
                if ("OPEN".equals(runtime.state.getPendingAction())) {
                    runtime.state.setPositionId(runtime.position.getId());
                    runtime.state.setPositionQty(runtime.position.getQuantity());
                    runtime.state.setEntryPrice(runtime.position.getEntryPrice());
                    if (order.executedQuantity() == null || order.executedQuantity().signum() <= 0 || runtime.state.getDirection() == null
                        || !runtime.state.getDirection().equalsIgnoreCase(runtime.position.getPositionSide()) || runtime.position.getQuantity().compareTo(order.executedQuantity()) > 0) {
                        runtime.state.setRecoveryRequired(true);
                        runtime.state.setUnsafeRecovery(true);
                        stateService.save(runtime.state);
                        return true;
                    }
                    runtime.state.setInitialPositionQty(order.executedQuantity());
                    runtime.state.setInitialMargin(runtime.position.getMargin());
                    freezeFilledProtection(runtime);
                    freezeOrderEntryPrice(runtime.state, order, runtime.partialOpenProtectionVerified);
                    runtime.state.setStatus("OPEN");
                    clearPending(runtime.state);
                    runtime.partialOpenProtectionVerified = false;
                    runtime.state.setRecoveryRequired(false);
                    runtime.state.setUnsafeRecovery(false);
                    stateService.save(runtime.state);
                    ensureHardStop(runtime);
                    BigDecimal current = runtime.currentPrice() != null ? runtime.currentPrice() : runtime.position.getMarkPrice();
                    if (current != null && current.signum() > 0) {
                        syncPluginProtections(runtime, buildContext(runtime, current, new ExchangeGateway.MarketEvent("RECOVERY", runtime.symbol.getSymbol(),
                         runtime.strategySymbol.getIntervalCode(), current, null, Instant.now())));
                    }
                    BigDecimal openRatio = runtime.strategySymbol.getOpenRatio() == null ? runtime.strategy.getDefaultOpenRatio() : runtime.strategySymbol.getOpenRatio();
                    int leverage = runtime.strategySymbol.getLeverage() == null ? runtime.strategy.getDefaultLeverage() : runtime.strategySymbol.getLeverage();
                    notificationService.notifyOpen(runtime.strategy, runtime.credential, runtime.symbol.getSymbol(), runtime.position.getPositionSide(),
                runtime.position.getQuantity(), runtime.position.getEntryPrice(), leverage, openRatio, "未知执行恢复后确认开仓成交");
                } else {
                    BigDecimal beforeQty = runtime.state.getPositionQty();
                    BigDecimal executedQty = order.executedQuantity();
                    if (beforeQty == null || executedQty == null || executedQty.signum() <= 0 || runtime.state.getDirection() == null || !runtime.state.getDirection().equalsIgnoreCase(runtime.position.getPositionSide()) || runtime.position.getQuantity().compareTo(beforeQty.subtract(executedQty).max(BigDecimal.ZERO)) > 0) {
                        // FILLED 是订单事实，持仓快照必须同时反映全部成交数量，才能解除退出保护。
                        runtime.state.setRecoveryRequired(true);
                        runtime.state.setUnsafeRecovery(true);
                        stateService.save(runtime.state);
                        return true;
                    }
                    String completedAction = runtime.state.getPendingAction();
                    if ("FIRST_TAKE_PROFIT".equalsIgnoreCase(completedAction)) {
                        PositionProtectionState.confirmLocalFirstProfit(runtime.state, executedQty);
                    }
                    runtime.state.setPositionQty(runtime.position.getQuantity());
                    clearPending(runtime.state);
                    runtime.state.setStatus("OPEN");
                    runtime.state.setRecoveryRequired(false);
                    runtime.state.setUnsafeRecovery(false);
                    stateService.save(runtime.state);
                    ensureHardStop(runtime);
                    BigDecimal current = runtime.currentPrice() != null ? runtime.currentPrice() : runtime.position.getMarkPrice();
                    if (current != null && current.signum() > 0) {
                        syncPluginProtections(runtime, buildContext(runtime, current, new ExchangeGateway.MarketEvent("RECOVERY", runtime.symbol.getSymbol(),
                         runtime.strategySymbol.getIntervalCode(), current, null, Instant.now())));
                    }
                }
                return true;
            }
            if ("OPEN".equals(runtime.state.getPendingAction())) {
                if (closedOpenProven && cancelStaleProtection(runtime)) {
                    blockCloseCandle(runtime, runtime.lastEvaluatedKlineOpenTime);
                    stateService.markClosed(runtime.state);
                    runtime.state = stateService.findByInstance(runtime.instance.getId());
                    log.info("待确认开仓恢复完成：开平仓成交完整且交易所空仓 instanceId={} symbol={} orderId={}", runtime.instance.getId(), runtime.symbol.getSymbol(), order.orderId());
                    return true;
                }
                runtime.state.setRecoveryRequired(true);
                runtime.state.setUnsafeRecovery(true);
                stateService.save(runtime.state);
                return true;
            }
            if (!cancelStaleProtection(runtime)) {
                return true;
            }
            blockCloseCandle(runtime, runtime.lastEvaluatedKlineOpenTime);
            stateService.markClosed(runtime.state);
            runtime.state = stateService.findByInstance(runtime.instance.getId());
            return true;
        }
        if ("CANCELED".equalsIgnoreCase(status) || "EXPIRED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status)) {
            BigDecimal executed = order.executedQuantity();
            if (executed == null) {
                runtime.state.setRecoveryRequired(true);
                runtime.state.setUnsafeRecovery(true);
                stateService.save(runtime.state);
                return true;
            }
            if (executed.signum() > 0) {
                BigDecimal beforeQty = runtime.state.getPositionQty();
                runtime.position = loadPosition(runtime.credential, runtime.symbol.getSymbol(), credential, snapshots);
                if (!"OPEN".equalsIgnoreCase(runtime.state.getPendingAction())) {
                    blockCloseCandleAtExchangeTime(runtime, order.updateTime());
                }
                if ("OPEN".equalsIgnoreCase(runtime.state.getPendingAction())) {
                    if (runtime.position == null || runtime.state.getDirection() == null || !runtime.state.getDirection().equalsIgnoreCase(runtime.position.getPositionSide())
                        || runtime.position.getQuantity().compareTo(executed) != 0) {
                        runtime.state.setRecoveryRequired(true);
                        runtime.state.setUnsafeRecovery(true);
                        stateService.save(runtime.state);
                        return true;
                    }
                    runtime.state.setInitialPositionQty(executed);
                    runtime.state.setPositionQty(executed);
                    freezeFilledProtection(runtime);
                } else if (runtime.position != null && (beforeQty == null || runtime.state.getDirection() == null
                    || !runtime.state.getDirection().equalsIgnoreCase(runtime.position.getPositionSide())
                    || runtime.position.getQuantity().compareTo(beforeQty.subtract(executed).max(BigDecimal.ZERO)) > 0)) {
                    runtime.state.setRecoveryRequired(true);
                    runtime.state.setUnsafeRecovery(true);
                    stateService.save(runtime.state);
                    return true;
                } else if ("FIRST_TAKE_PROFIT".equalsIgnoreCase(runtime.state.getPendingAction())) {
                    PositionProtectionState.confirmLocalFirstProfit(runtime.state, executed);
                }
                if (runtime.position == null && !"OPEN".equalsIgnoreCase(runtime.state.getPendingAction())) {
                    blockCloseCandle(runtime, runtime.lastEvaluatedKlineOpenTime);
                }
            } else {
                runtime.position = loadPosition(runtime.credential, runtime.symbol.getSymbol(), credential, snapshots);
                if (runtime.position != null && ("OPEN".equalsIgnoreCase(runtime.state.getPendingAction())
                    || runtime.state.getDirection() == null || !runtime.state.getDirection().equalsIgnoreCase(runtime.position.getPositionSide())
                    || runtime.state.getPositionQty() == null || runtime.position.getQuantity().compareTo(runtime.state.getPositionQty()) != 0)) {
                    runtime.state.setRecoveryRequired(true);
                    runtime.state.setUnsafeRecovery(true);
                    stateService.save(runtime.state);
                    return true;
                }
            }
            clearPending(runtime.state);
            runtime.partialOpenProtectionVerified = false;
            if (runtime.position != null) {
                runtime.state.setStatus("OPEN");
                runtime.state.setRecoveryRequired(false);
                runtime.state.setUnsafeRecovery(false);
                stateService.save(runtime.state);
            } else {
                if (!cancelStaleProtection(runtime)) {
                    return true;
                }
                stateService.markClosed(runtime.state);
            }
            return true;
        }
        return true;
    }

    /**
     * 只在执行记录明确证明开仓请求未发送时，释放该次请求占用的准确 K 线保护记录。
     */
    private void releaseFailedOpenEntryGuard(ActiveRuntime runtime) {
        if (runtime == null || runtime.state == null || !"OPEN".equalsIgnoreCase(runtime.state.getPendingAction())) {
            return;
        }
        String key = runtime.state.getPendingIdempotencyKey();
        if (key == null) {
            return;
        }
        String[] parts = key.split(":", 5);
        if (parts.length < 4 || !"STRATEGY".equals(parts[0]) || !"OPEN".equals(parts[2]) || !String.valueOf(runtime.instance.getId()).equals(parts[1])) {
            return;
        }
        try {
            Instant candleOpenTime = Instant.ofEpochMilli(Long.parseLong(parts[3]));
            entryGuardService.release(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol(), candleOpenTime);
        } catch (IllegalArgumentException ex) {
            log.warn("未能从明确失败的开仓幂等键解析 K 线时间，保留开仓保护 instanceId={} symbol={}", runtime.instance.getId(), runtime.symbol.getSymbol());
        }
    }

    /**
     * 恢复 PAPER 待确认执行并防止重复结算。
     */
    private boolean reconcilePaperPendingExecution(ActiveRuntime runtime) {
        String key = runtime.state.getPendingIdempotencyKey();
        if (key == null || key.isBlank()) {
            runtime.state.setRecoveryRequired(true);
            runtime.state.setUnsafeRecovery(true);
            stateService.save(runtime.state);
            return true;
        }
        try {
            var execution = executionRecordService.find(runtime.credential.getId(), key);
            String executionStatus = execution == null ? "MISSING" : execution.getExecutionStatus();
            if ("MISSING".equals(executionStatus)) {
                runtime.state.setRecoveryRequired(true);
                runtime.state.setUnsafeRecovery(true);
                stateService.save(runtime.state);
                return true;
            }
            if ("PENDING".equalsIgnoreCase(executionStatus)) {
                // PAPER 执行不存在交易所外部副作用；PENDING 说明本地执行事务尚未确认，不得凭空创建成交。
                if (runtime.state.getPendingOrderTime() != null && System.currentTimeMillis() - runtime.state.getPendingOrderTime() > 30_000L) {
                    if ("OPEN".equalsIgnoreCase(runtime.state.getPendingAction())) {
                        paperTradingService.failPendingOpen(runtime.credential.getId(), key, runtime.state.getInitialMargin(), "PAPER 执行在确认成交前中断，已按无外部副作用安全回滚");
                        clearPending(runtime.state);
                        runtime.state.setStatus("CLOSED");
                        runtime.state.setRecoveryRequired(false);
                        runtime.state.setUnsafeRecovery(false);
                    } else {
                        executionRecordService.markRecoveryAttempt(execution);
                        executionRecordService.failed(execution, "PAPER 平仓在本地结算前中断，未发生外部副作用");
                        clearPending(runtime.state);
                        runtime.state.setStatus("OPEN");
                        runtime.state.setRecoveryRequired(false);
                        runtime.state.setUnsafeRecovery(false);
                    }
                    stateService.save(runtime.state);
                }
                return true;
            }
            if ("OPEN".equalsIgnoreCase(runtime.state.getPendingAction())) {
                QuantPosition position = loadPosition(runtime.credential, runtime.symbol.getSymbol(), null);
                if (position == null) {
                    position = paperTradingService.recordOpenFill(runtime.credential.getId(), key, runtime.symbol.getSymbol(), runtime.state.getDirection(),
                     runtime.state.getPositionQty(), runtime.state.getEntryPrice(), runtime.strategySymbol.getLeverage() == null ? runtime.strategy.getDefaultLeverage() :
                      runtime.strategySymbol.getLeverage());
                }
                runtime.position = position;
                runtime.state.setPositionId(position.getId());
                runtime.state.setPositionQty(position.getQuantity());
                runtime.state.setStatus("OPEN");
                clearPending(runtime.state);
                runtime.state.setRecoveryRequired(false);
                runtime.state.setUnsafeRecovery(false);
                stateService.save(runtime.state);
                return true;
            }
            if (runtime.position == null) {
                runtime.position = loadPosition(runtime.credential, runtime.symbol.getSymbol(), null);
            }
            if (runtime.position == null) {
                runtime.state.setRecoveryRequired(true);
                runtime.state.setUnsafeRecovery(true);
                stateService.save(runtime.state);
                return true;
            }
            BigDecimal closeQty = runtime.state.getPositionQty() == null ? runtime.position.getQuantity() : runtime.state.getPositionQty();
            BigDecimal price = runtime.position.getMarkPrice() == null ? runtime.position.getEntryPrice() : runtime.position.getMarkPrice();
            PaperTradingService.PaperCloseSettlement settlement = paperTradingService.settlePaperExecution(runtime.credential.getId(), key, runtime.position.getId(),
             closeQty.min(runtime.position.getQuantity()), price);
            runtime.position = settlement.position();
            if (runtime.position == null) {
                blockCloseCandle(runtime, runtime.lastEvaluatedKlineOpenTime);
                if (!cancelStaleProtection(runtime)) {
                    return true;
                }
                stateService.markClosed(runtime.state);
                runtime.state = stateService.findByInstance(runtime.instance.getId());
            } else {
                runtime.state.setPositionQty(runtime.position.getQuantity());
                runtime.state.setStatus("OPEN");
                runtime.state.setRecoveryRequired(false);
                runtime.state.setUnsafeRecovery(false);
                clearPending(runtime.state);
                stateService.save(runtime.state);
            }
            return true;
        } catch (RuntimeException ex) {
            runtime.state.setRecoveryRequired(true);
            runtime.state.setUnsafeRecovery(true);
            stateService.save(runtime.state);
            return true;
        }
    }

    /**
     * 查询指定幂等键的执行状态。
     */
    private QuantExecutionRecordStatus findExecutionStatus(Long credentialId, String idempotencyKey) {
        var record = executionRecordService.find(credentialId, idempotencyKey);
        return new QuantExecutionRecordStatus(record == null ? "MISSING" : record.getExecutionStatus());
    }

    /**
     * 释放 PAPER 中已预留但未形成有效持仓的保证金。
     */
    private void releasePaperMargin(Long credentialId, BigDecimal margin) {
        if (margin == null || margin.signum() <= 0) {
            return;
        }
        paperTradingService.refundReservedMargin(credentialId, margin);
    }

    /**
     * 读取当前凭证和交易对的本地持仓事实。
     */
    private QuantPosition loadPosition(QuantCredential credential, String symbol, ExchangeGateway.Credential exchangeCredential) {
        if ("PAPER".equalsIgnoreCase(credential.getRunMode())) {
            return loadPosition(credential, symbol, exchangeCredential, null);
        }
        List<ExchangeGateway.PositionSnapshot> snapshots = reconciliationService.refreshSymbolPosition(credential, symbol, exchangeCredential);
        return loadPosition(credential, symbol, exchangeCredential, snapshots);
    }

    /**
     * 后台对账提交围栏保护的交易对快照，并读取提交后的持仓事实。
     */
    private QuantPosition loadReconciledPosition(ActiveRuntime runtime, List<QuantPosition> baseline, List<ExchangeGateway.PositionSnapshot> snapshots) {
        if ("PAPER".equalsIgnoreCase(runtime.credential.getRunMode())) {
            List<QuantPosition> open = baseline.stream().filter(p -> p.getQuantity() != null && p.getQuantity().signum() > 0).toList();
            if (open.size() > 1) {
                throw new IllegalStateException("PAPER账户存在多个非零方向持仓，拒绝接管");
            }
            return open.isEmpty() ? null : open.get(0);
        }
        List<QuantPosition> open =
        positionMapper.selectList(new LambdaQueryWrapper<QuantPosition>().eq(QuantPosition::getCredentialId, runtime.credential.getId()).eq(QuantPosition::getSymbol,
          runtime.symbol.getSymbol()).gt(QuantPosition::getQuantity, BigDecimal.ZERO));
        if (open.size() > 1) {
            throw new IllegalStateException("同一凭证同一交易对存在多个非零持仓方向，拒绝接管");
        }
        return open.isEmpty() ? null : open.get(0);
    }

    private QuantPosition loadPosition(QuantCredential credential, String symbol, ExchangeGateway.Credential exchangeCredential, List<ExchangeGateway.PositionSnapshot> snapshots) {
        if ("PAPER".equalsIgnoreCase(credential.getRunMode())) {
            List<QuantPosition> positions =
            positionMapper.selectList(new LambdaQueryWrapper<QuantPosition>().eq(QuantPosition::getCredentialId, credential.getId()).eq(QuantPosition::getSymbol, symbol).gt(QuantPosition::getQuantity, BigDecimal.ZERO));
            if (positions.size() > 1) {
                throw new IllegalStateException("PAPER 账户同一交易对存在多个非零方向持仓，拒绝自动接管: " + symbol);
            }
            return positions.isEmpty() ? null : positions.get(0);
        }
        if (snapshots == null) {
            throw new IllegalStateException("Binance symbol position snapshot missing");
        }
        List<QuantPosition> localOpen =
positionMapper.selectList(new LambdaQueryWrapper<QuantPosition>().eq(QuantPosition::getCredentialId, credential.getId()).eq(QuantPosition::getSymbol, symbol).gt(QuantPosition::getQuantity, BigDecimal.ZERO));
        if (localOpen.size() > 1) {
            throw new IllegalStateException("同一凭证同一交易对存在多个非零持仓方向，当前运行时模型拒绝自动接管: " + symbol);
        }
        return localOpen.isEmpty() ? null : localOpen.get(0);
    }

    /**
     * 创建或更新 PAPER 本地持仓。
     */
    private QuantPosition upsertPaperPosition(ActiveRuntime runtime, String direction, BigDecimal quantity, BigDecimal entryPrice, int leverage) {
        QuantPosition position = new QuantPosition();
        position.setCredentialId(runtime.credential.getId());
        position.setSymbol(runtime.symbol.getSymbol());
        position.setPositionSide(direction);
        position.setQuantity(quantity);
        position.setEntryPrice(entryPrice);
        position.setMarkPrice(entryPrice);
        position.setMargin(entryPrice.multiply(quantity).divide(BigDecimal.valueOf(Math.max(1, leverage)), 16, RoundingMode.HALF_UP));
        position.setLeverage(leverage);
        position.setUnrealizedPnl(BigDecimal.ZERO);
        position.setUpdatedAt(LocalDateTime.now());
        positionMapper.insert(position);
        return position;
    }

    /**
     * 用当前持仓事实校准策略运行状态。
     */
    private void syncRuntimeStateWithPosition(ActiveRuntime runtime) {
        // 首止盈的交易所查询由后台对账完成；此入口只提交已有事实。
        syncRuntimeStateWithPosition(runtime, false);
    }

    /**
     * 将运行时策略状态与已核验的账户持仓事实对齐。
     */
    private void syncRuntimeStateWithPosition(ActiveRuntime runtime, boolean firstTpFilled) {
        if (runtime.position == null) {
            return;
        }
        if (runtime.state == null) {
            runtime.state = recoverState(runtime);
        }
        if (runtime.state.getPositionId() == null || !runtime.position.getId().equals(runtime.state.getPositionId())) {
            runtime.state.setPositionId(runtime.position.getId());
        }
        runtime.state.setPositionQty(runtime.position.getQuantity());
        if (firstTpFilled) {
            runtime.state.setFirstTakeProfitTriggered(true);
        }
        runtime.state.setStatus("OPEN");
        clearPending(runtime.state);
        stateService.save(runtime.state);
    }

    /**
     * 根据持仓事实恢复策略运行状态。
     */
    private QuantStrategyPositionState recoverState(ActiveRuntime runtime) {
        QuantStrategyPositionState state = new QuantStrategyPositionState();
        state.setInstanceId(runtime.instance.getId());
        state.setPositionId(runtime.position.getId());
        state.setStatus("OPEN");
        state.setDirection(runtime.position.getPositionSide());
        state.setEntryPrice(runtime.position.getEntryPrice());
        state.setPositionQty(runtime.position.getQuantity());
        state.setInitialPositionQty(runtime.position.getQuantity());
        state.setInitialMargin(runtime.position.getMargin());
        state.setFrozenStopLossRatio(resolveHardStopRatio(runtime));
        Map<String, Object> legacyParams = new java.util.LinkedHashMap<>(runtime.params);
        legacyParams.remove(KlineSlopeRateStrategy.PROTECTION_POLICY_VERSION_KEY);
        state.setStrategyParamsSnapshot(writeParams(legacyParams));
        state.setRecoveryRequired(true);
        state.setUnsafeRecovery(true);
        state.setOpenedAt(null);
        log.error("历史持仓缺少冻结参数和首次止盈事实，保留已有保护并暂停自动接管 instanceId={} symbol={}", runtime.instance.getId(), runtime.symbol.getSymbol());
        stateService.save(state);
        return stateService.findByInstance(runtime.instance.getId());
    }

    /**
     * 确保当前持仓存在正确数量和触发价的固定硬止损。
     */
    private void ensureHardStop(ActiveRuntime runtime) {
        requestProtectionSync(runtime, false);
    }

    /**
     * 只在短锁内捕获持仓快照；所有交易所保护 REST 在锁外运行。
     */
    private void requestProtectionSync(ActiveRuntime runtime, boolean dynamic) {
        synchronized (runtime) {
            if (runtime.closed || runtime.stateRefreshRequired || runtime.position == null || runtime.exitCoordinating.get()
                || !hasFrozenProtectionFacts(runtime) && !(runtime.partialOpenProtectionVerified && runtime.state != null && "OPEN".equals(runtime.state.getPendingAction()))) {
                return;
            }
            runtime.protectionRequested.set(true);
            // 首次同步必须携带 dynamic，确保插件信号（含 localStopLossRate 镜像）与硬止损一次挂上
            if (dynamic || runtime.lastProtectionTarget == null) {
                runtime.dynamicProtectionRequested.set(true);
            }
            if (!runtime.protectionSyncing.compareAndSet(false, true)) {
                return;
            }
        }
        synchronized (runtime) {
            if (runtime.closed) {
                runtime.protectionSyncing.set(false);
                return;
            }
            runtime.protectionTask = recoveryExecutor.submit(() -> {
                Thread heartbeat = null;
                AtomicBoolean renewing = new AtomicBoolean(true);
                AtomicBoolean leaseHeld = new AtomicBoolean(true);
                try {
                    do {
                        runtime.protectionRequested.set(false);
                        ProtectionWork work;
                        synchronized (runtime) {
                            if (runtime.closed || runtime.position == null) {
                                break;
                            }
                            QuantPosition snapshot = new QuantPosition();
                            BeanUtils.copyProperties(runtime.position, snapshot);
                            snapshot.setProtectionCycleKey(runtime.state == null ? null : runtime.state.getPositionCycleKey());
                            snapshot.setProtectionPolicyVersion(runtime.plugin instanceof KlineSlopeRateStrategy && KlineSlopeRateStrategy.unifiedProtection(frozenParams(runtime)) ? 2 : null);
                            if (runtime.state != null && runtime.state.getFrozenLeverage() != null) {
                                snapshot.setLeverage(runtime.state.getFrozenLeverage());
                                snapshot.setEntryPrice(runtime.state.getEntryPrice());
                            }
                            boolean includeDynamic = runtime.dynamicProtectionRequested.getAndSet(false) && !runtime.hasPendingAction();
                            BigDecimal hardStopRatio = resolveHardStopRatio(runtime);
                            if (Integer.valueOf(2).equals(snapshot.getProtectionPolicyVersion()) && hardStopRatio != null
                                && hardStopRatio.signum() > 0 && hardStopRatio.compareTo(BigDecimal.ONE) < 0) {
                                if (runtime.state.getStopLossPrice() == null) {
                                    BigDecimal stop = KlineSlopeRateStrategy.priceForReturn(snapshot.getEntryPrice(), snapshot.getPositionSide(),
                                        hardStopRatio.negate(), snapshot.getLeverage());
                                    runtime.state.setStopLossPrice(runtime.protectionRule == null ? stop
                                        : org.dromara.quant.exchange.QuantityNormalizer.protectionPrice(stop, runtime.protectionRule, snapshot.getPositionSide()));
                                    try {
                                        stateService.save(runtime.state);
                                    } catch (RuntimeException ex) {
                                        // 冻结落库未确认时不能派发，也不能把内存值当成已持久化；先重载以处理提交结果不明。
                                        runtime.state.setStopLossPrice(null);
                                        runtime.stateRefreshRequired = true;
                                        invalidatePositionFacts(runtime);
                                        runtime.nextReconcileAt = 0L;
                                        requestReconciliation(runtime);
                                        throw ex;
                                    }
                                }
                                snapshot.setProtectionStopLossPrice(runtime.state.getStopLossPrice());
                            }
                            BigDecimal current = runtime.currentPrice() != null ? runtime.currentPrice() : snapshot.getMarkPrice();
                            List<ProtectionSignal> signals = List.of();
                            RuntimeException signalFailure = null;
                            try {
                                if (includeDynamic) {
                                    if (current == null || current.signum() <= 0) {
                                        throw new IllegalStateException("缺少有效行情价格，不能确认策略止盈止损目标");
                                    }
                                    signals = protectionSignals(runtime, buildContext(runtime, current, new ExchangeGateway.MarketEvent("PROTECTION_SYNC",
                                     runtime.symbol.getSymbol(), runtime.strategySymbol.getIntervalCode(), current, null, Instant.now())), frozenParams(runtime));
                                }
                            } catch (RuntimeException ex) {
                                // 目标计算失败不能阻断硬止损，也不能以空目标撤掉已有动态保护。
                                signalFailure = ex;
                            }
                            work = new ProtectionWork(runtime.credential.getId(), snapshot, runtime.state, hardStopRatio, runtime.state == null ? snapshot.getQuantity() :
                             runtime.state.getInitialPositionQty(), signals, includeDynamic, !"PAPER".equalsIgnoreCase(runtime.credential.getRunMode()), signalFailure);
                        }
                        if (runtime.closed || !leaseHeld.get()) {
                            break;
                        }
                        if (!leaseService.acquireOrRenew(runtime.instance.getId(), work.credentialId(), work.position().getSymbol())) {
                            runtime.marketHealthy = false;
                            runtime.protectionRequested.set(false);
                            break;
                        }
                        if (heartbeat == null) {
                            heartbeat = Thread.ofVirtual().name("quant-protection-lease").start(() -> {
                                try {
                                    while (renewing.get()) {
                                        Thread.sleep(5000);
                                        if (!renewing.get()) {
                                            break;
                                        }
                                        if (!leaseService.acquireOrRenew(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol())) {
                                            leaseHeld.set(false);
                                            runtime.marketHealthy = false;
                                            break;
                                        }
                                    }
                                } catch (InterruptedException ex) {
                                    Thread.currentThread().interrupt();
                                } catch (RuntimeException ex) {
                                    leaseHeld.set(false);
                                    runtime.marketHealthy = false;
                                }
                            });
                        }
                        RuntimeException failure = null;
                        boolean hardStopConfirmed = false;
                        int confirmedStep = 0;
                        // 硬止损优先并独立确认；止盈等其他目标失败不能跳过硬止损。
                        try {
                            if (work.ratio() == null || work.ratio().signum() <= 0) {
                                throw new IllegalStateException("缺少有效硬止损比例");
                            }
                            if (protectionOrderService.ensureMarginHardStop(work.credentialId(), work.position(), work.ratio(), work.live()) == null) {
                                throw new IllegalStateException("持仓字段不足，硬止损未创建");
                            }
                            hardStopConfirmed = true;
                        } catch (RuntimeException ex) {
                            failure = ex;
                        }
                        try {
                            if (work.signalFailure() != null) {
                                throw work.signalFailure();
                            }
                            if (work.dynamic() && !runtime.closed && leaseHeld.get() && leaseService.acquireOrRenew(runtime.instance.getId(), work.credentialId(),
                             work.position().getSymbol())) {
                                // 硬止损价可能已冻结；跨类型覆盖必须由保护服务精确核实实际委托，不能用重算价格提前过滤。
                                List<ProtectionSignal> exchangeSignals = deduplicateExchangeProtectionSignals(work.signals(), work.position(), work.initialQuantity(),
                                 null, runtime.protectionRule, work.state() != null && work.state().getFrozenLeverage() != null ?
                                  work.state().getFrozenLeverage() : work.position().getLeverage() == null ? 1 : work.position().getLeverage());
                                protectionOrderService.syncStrategyProtections(work.credentialId(), work.position(), work.initialQuantity(), exchangeSignals,
                            work.state() == null ? null : work.state().getFirstTakeProfitTargetQty() == null ? null :
                                  PositionProtectionState.remainingFirstProfit(work.state()),
                                    work.state() == null ? null : work.state().getFirstTakeProfitLocalExecutedQty(), work.live());
                            }
                            if (failure == null) {
                                confirmedStep = protectionOrderService.confirmedStep(work.credentialId(), work.position());
                            }
                        } catch (RuntimeException ex) {
                            if (failure == null) {
                                failure = ex;
                            } else if (failure != ex) {
                                failure.addSuppressed(ex);
                            }
                        }
                        if (!leaseHeld.get() || !leaseService.acquireOrRenew(runtime.instance.getId(), work.credentialId(), work.position().getSymbol())) {
                            runtime.marketHealthy = false;
                            runtime.protectionRequested.set(false);
                            break;
                        }
                        boolean obsolete;
                        synchronized (runtime) {
                            obsolete =
                             runtime.position == null || !Objects.equals(runtime.position.getId(), work.position().getId()) || runtime.state == null || !Objects.equals(runtime.state.getPositionCycleKey(), work.position().getProtectionCycleKey());
                            boolean current =
                             !runtime.closed && !obsolete && runtime.state == work.state() && runtime.position.getQuantity().compareTo(work.position().getQuantity()) == 0;
                            if (current) {
                                runtime.hardStopConfirmed = hardStopConfirmed;
                                if (failure != null) {
                                    markProtectionDegraded(runtime, "交易所保护创建/同步失败", failure);
                                } else {
                                    int previousStep = runtime.state.getConfirmedProtectionStep() == null ? 0 : runtime.state.getConfirmedProtectionStep();
                                    int highestStep = runtime.state.getHighestStep() == null ? 0 : runtime.state.getHighestStep();
                                    if (confirmedStep > previousStep || confirmedStep > highestStep) {
                                        runtime.state.setConfirmedProtectionStep(Math.max(previousStep, confirmedStep));
                                        runtime.state.setHighestStep(Math.max(highestStep, confirmedStep));
                                        stateService.save(runtime.state);
                                    }
                                    if (work.dynamic() && runtime.protectionDegraded && !runtime.hasPendingAction()) {
                                        // 保护恢复只能解除自身降级；未知成交/持仓归属必须由事实恢复链路核实。
                                        if (!Boolean.TRUE.equals(runtime.state.getUnsafeRecovery())) {
                                            runtime.state.setRecoveryRequired(false);
                                        }
                                        stateService.save(runtime.state);
                                        runtime.protectionDegraded = false;
                                        clearProtectionErrorAfterConfirmedSync(runtime);
                                    }
                                }
                            } else if (!runtime.closed && !obsolete) {
                                // 同一持仓数量改变：下一轮先挂正确保护再撤旧，不能清空全部保护。
                                runtime.protectionRequested.set(true);
                            }
                        }
                        if (obsolete) {
                            QuantStrategyPositionState cleanupState;
                            synchronized (runtime) {
                                runtime.protectionCleanupPending = true;
                                cleanupState = runtime.state;
                                if (runtime.position == null && cleanupState != null) {
                                    // 旧保护任务已过期，先阻止开仓并标记待清理；此时不能仅凭本地 position=null 判定交易所空仓。
                                    cleanupState.setStatus("PROTECTION_CLEANUP");
                                    cleanupState.setRecoveryRequired(true);
                                    cleanupState.setUnsafeRecovery(true);
                                    stateService.save(cleanupState);
                                }
                            }
                            // 先确认交易所确实空仓，再撤保护单；本地持仓过期不能作为撤单依据。
                            ProtectionCleanupResult cleanup = cleanupProtectionIfExchangeFlat(runtime, work.position().getId(), work.position().getProtectionCycleKey(),
                             work.live(), "迟到保护清理");
                            boolean exchangeFlat = cleanup.exchangeFlat();
                            boolean cleaned = cleanup.cleaned();

                            synchronized (runtime) {
                                runtime.protectionCleanupPending = !cleaned;
                                // 防止异步期间状态/持仓被另一条执行路径更新：仅处理同一状态对象且仍确认本地无持仓的情况。
                                if (exchangeFlat && !runtime.closed && runtime.position == null && runtime.state == cleanupState && cleanupState != null && !runtime.hasPendingAction()) {
                                    blockCloseCandle(runtime, runtime.lastEvaluatedKlineOpenTime);
                                    stateService.markClosed(cleanupState);
                                    runtime.state = stateService.findByInstance(runtime.instance.getId());
                                    // 保护单撤销失败仍必须阻止新开仓；但已确认空仓的记录不应继续占用最大持仓数。
                                    runtime.protectionCleanupPending = !cleaned;
                                    log.info("迟到保护清理后已确认交易所空仓，释放策略持仓名额 instanceId={} symbol={} protectionCleaned={}", runtime.instance.getId(), runtime.symbol.getSymbol(),
                                     cleaned);
                                }
                            }
                            if (!exchangeFlat) {
                                log.error("交易所持仓未确认空仓，保留保护单并禁止下一次开仓 instanceId={} positionId={}", runtime.instance.getId(), work.position().getId());
                            } else if (!cleaned) {
                                log.error("迟到保护未清理，禁止下一次开仓 instanceId={} positionId={}", runtime.instance.getId(), work.position().getId());
                            }
                            requestReconciliation(runtime);
                        }
                    } while (!runtime.closed && !runtime.exitCoordinating.get() && runtime.protectionRequested.get());
                } catch (RuntimeException ex) {
                    synchronized (runtime) {
                        if (!runtime.closed) {
                            markProtectionDegraded(runtime, "异步保护恢复失败", ex);
                        }
                    }
                } finally {
                    renewing.set(false);
                    if (heartbeat != null) {
                        heartbeat.interrupt();
                        boolean interrupted = false;
                        for (; ; ) {
                            try {
                                heartbeat.join();
                                break;
                            } catch (InterruptedException ex) {
                                interrupted = true;
                            }
                        }
                        if (interrupted) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    runtime.protectionSyncing.set(false);
                    if (!runtime.closed && runtime.protectionRequested.get()) {
                        requestProtectionSync(runtime, runtime.dynamicProtectionRequested.get());
                    }
                }
            });
        }
    }

    /**
     * 标记保护链路降级并阻止后续开仓，等待保护状态恢复确认。
     */
    private void markProtectionDegraded(ActiveRuntime runtime, String reason, RuntimeException ex) {
        if (runtime.state != null) {
            runtime.state.setRecoveryRequired(true);
            // 保护状态未知由目标/派发门禁处理，不能据此把已确认的持仓归属改成不可信，阻断本地安全减仓。
            stateService.save(runtime.state);
        }
        // 保留运行时监管和本地退出能力，仅阻止下一次开仓。
        runtime.protectionDegraded = true;
        if (isProtectionStatePending(ex)) {
            log.warn("{}：保护事实尚待交易所核实，保持开仓阻断 instanceId={} symbol={} detail={}", reason, runtime.instance.getId(), runtime.symbol.getSymbol(), ex.getMessage());
            return;
        }
        runtime.instance.setLastError(reason + "，保留本地止损并等待恢复: " + ex.getMessage());
        instanceMapper.updateById(runtime.instance);
        log.error("{} instanceId={} symbol={}", reason, runtime.instance.getId(), runtime.symbol.getSymbol(), ex);
    }

    /** 仅在整轮保护同步已确认成功后清除本功能写入的旧错误，避免覆盖其他运行错误。 */
    private void clearProtectionErrorAfterConfirmedSync(ActiveRuntime runtime) {
        String lastError = runtime.instance.getLastError();
        if (lastError == null || !lastError.startsWith("交易所保护创建/同步失败")) {
            return;
        }
        runtime.instance.setLastError("");
        if (instanceMapper.updateById(runtime.instance) != 1) {
            runtime.instance.setLastError(lastError);
            log.warn("保护状态已恢复，但策略实例旧错误未能清除 instanceId={} symbol={}", runtime.instance.getId(), runtime.symbol.getSymbol());
        }
    }

    private boolean isProtectionStatePending(RuntimeException ex) {
        return ex instanceof ProtectionOrderService.ProtectionExecutionPendingException || ex instanceof ProtectionTargetCoordinator.PendingTargetException;
    }

    /**
     * 对已确认空仓但保护单仍未清理完成的状态进行重试。
     * <p>该重试不依赖 position 状态仍为 OPEN，避免状态归零后失去后续清理机会。
     */
    private boolean retryProtectionCleanupIfNeeded(ActiveRuntime runtime) {
        final QuantStrategyPositionState expectedState;
        final Long positionId;
        synchronized (runtime) {
            if (runtime.closed || !runtime.protectionCleanupPending || runtime.position != null || runtime.state == null || runtime.state.getPositionId() == null || runtime.hasPendingAction()) {
                return false;
            }
            expectedState = runtime.state;
            positionId = expectedState.getPositionId();
        }
        boolean cleaned;
        try {
            boolean live = !"PAPER".equalsIgnoreCase(runtime.credential.getRunMode());
            ProtectionCleanupResult cleanup = cleanupProtectionIfExchangeFlat(runtime, positionId, expectedState.getPositionCycleKey(), live, "残留保护单重试");
            if (!cleanup.exchangeFlat()) {
                requestReconciliation(runtime);
                return false;
            }
            cleaned = cleanup.cleaned();
        } catch (RuntimeException ex) {
            log.warn("残留保护单重试清理失败，继续阻止开仓 instanceId={} positionId={} error={}", runtime.instance.getId(), positionId, ex.getMessage());
            return false;
        }
        synchronized (runtime) {
            if (runtime.state == expectedState && runtime.position == null && !runtime.closed) {
                runtime.protectionCleanupPending = !cleaned;
                if (cleaned) {
                    runtime.state.setRecoveryRequired(false);
                    runtime.state.setUnsafeRecovery(false);
                    try {
                        stateService.save(runtime.state);
                    } catch (org.dromara.quant.runtime.service.StrategyPositionStateService.StateConflictException ex) {
                        QuantStrategyPositionState latestState = stateService.findByInstance(runtime.instance.getId());
                        if (latestState != null && Objects.equals(latestState.getPositionId(), positionId)
                            && Objects.equals(latestState.getPositionCycleKey(), expectedState.getPositionCycleKey())
                            && "CLOSED".equals(latestState.getStatus())
                            && (latestState.getPositionQty() == null || latestState.getPositionQty().signum() == 0)
                            && latestState.getPendingAction() == null) {
                            runtime.state = latestState;
                            if (!Boolean.TRUE.equals(latestState.getRecoveryRequired()) && !Boolean.TRUE.equals(latestState.getUnsafeRecovery())) {
                                runtime.protectionCleanupPending = false;
                                log.info("残留保护清理期间状态已被并发核实更新，采用最新状态 instanceId={} positionId={}", runtime.instance.getId(), positionId);
                                return false;
                            }
                        } else if (latestState != null) {
                            replaceRuntimeStatePreservingExtrema(runtime, latestState);
                        }
                        runtime.stateRefreshRequired = true;
                        invalidatePositionFacts(runtime);
                        runtime.protectionCleanupPending = true;
                        runtime.nextReconcileAt = System.currentTimeMillis() + 1_000L;
                        runtime.reconcileRequested.set(true);
                        log.warn("残留保护清理状态遇到并发更新，保留开仓阻断并重新核实 instanceId={} positionId={} latestVersion={}",
                            runtime.instance.getId(), positionId, latestState == null ? null : latestState.getVersion());
                        return true;
                    }
                    log.info("残留保护单重试清理成功，解除开仓阻断 instanceId={} positionId={}", runtime.instance.getId(), positionId);
                }
            }
        }
        return false;
    }

    /**
     * 只有交易所持仓确认归零后才清理该周期保护记录。
     */
    private ProtectionCleanupResult cleanupProtectionIfExchangeFlat(ActiveRuntime runtime, Long positionId, String cycleKey, boolean live, String reason) {
        List<ExchangeGateway.PositionSnapshot> snapshots;
        try {
            snapshots = gateway.queryPositions(executionService.toCredential(runtime.credential), runtime.symbol.getSymbol());
        } catch (RuntimeException ex) {
            log.warn("{}时无法确认交易所持仓，保留所有保护单 instanceId={} positionId={} symbol={} error={}", reason, runtime.instance.getId(), positionId, runtime.symbol.getSymbol(),
             ex.getClass().getSimpleName());
            return new ProtectionCleanupResult(false, false);
        }
        boolean exchangeFlat = snapshots != null && snapshots.stream().allMatch(snapshot -> snapshot.quantity() != null && snapshot.quantity().signum() == 0);
        if (!exchangeFlat) {
            log.warn("{}时交易所仍有持仓或持仓结果不完整，保留所有保护单 instanceId={} positionId={} symbol={}", reason, runtime.instance.getId(), positionId, runtime.symbol.getSymbol());
            return new ProtectionCleanupResult(false, false);
        }
        boolean cleaned = protectionOrderService.cancelActiveProtectionOrders(runtime.credential.getId(), positionId, cycleKey, live);
        return new ProtectionCleanupResult(true, cleaned);
    }

    /**
     * 撤销与当前持仓状态不再匹配的过期保护单。
     */
    private boolean cancelStaleProtection(ActiveRuntime runtime) {
        if (runtime == null) {
            return false;
        }
        QuantStrategyPositionState state = runtime.state;
        boolean live = !"PAPER".equalsIgnoreCase(runtime.credential.getRunMode());
        Long positionId = state == null ? null : state.getPositionId();
        boolean success;

        if (positionId != null) {
            success = protectionOrderService.cancelActiveProtectionOrders(runtime.credential.getId(), positionId, live);
        } else {
            // 旧状态缺少 positionId 时，不能直接当作“没有保护单”放行。
            // LIVE/TESTNET 必须先确认交易所该交易对空仓，再按交易对清理所有活动保护单。
            if (live) {
                List<ExchangeGateway.PositionSnapshot> snapshots;
                try {
                    snapshots = gateway.queryPositions(executionService.toCredential(runtime.credential), runtime.symbol.getSymbol());
                } catch (RuntimeException ex) {
                    markProtectionCleanupUnresolved(runtime, state, "缺少 positionId 且无法核实交易所持仓");
                    log.warn("缺少 positionId，无法确认交易所空仓，保留开仓阻断 instanceId={} symbol={} errorType={}",
                        runtime.instance.getId(), runtime.symbol.getSymbol(), ex.getClass().getSimpleName());
                    return false;
                }
                boolean exchangeFlat = snapshots != null && snapshots.stream()
                    .allMatch(snapshot -> snapshot.quantity() != null && snapshot.quantity().signum() == 0);
                if (!exchangeFlat) {
                    markProtectionCleanupUnresolved(runtime, state, "缺少 positionId 且交易所持仓非空或未知");
                    log.warn("缺少 positionId，交易所持仓非空或结果不完整，保留开仓阻断 instanceId={} symbol={}",
                        runtime.instance.getId(), runtime.symbol.getSymbol());
                    return false;
                }
            }
            success = protectionOrderService.cancelActiveProtectionOrdersForSymbol(
                runtime.credential.getId(), runtime.symbol.getSymbol(), live);
        }

        runtime.protectionCleanupPending = !success;
        if (state != null) {
            // 只有保护单清理已确认成功才清除恢复标志；失败时继续 fail-closed。
            state.setStatus("CLOSED");
            state.setPositionQty(BigDecimal.ZERO);
            state.setPendingAction(null);
            state.setPendingClientOrderId(null);
            state.setPendingOrderId(null);
            state.setPendingOrderTime(null);
            state.setPendingIdempotencyKey(null);
            state.setRecoveryRequired(!success);
            state.setUnsafeRecovery(!success);
            stateService.save(state);
        }
        if (!success) {
            log.warn("持仓保护单清理未确认，已保留恢复阻断 instanceId={} positionId={} symbol={}",
                runtime.instance.getId(), positionId, runtime.symbol.getSymbol());
            return false;
        }
        log.info("持仓保护单已确认清理 instanceId={} positionId={} symbol={} live={}",
            runtime.instance.getId(), positionId, runtime.symbol.getSymbol(), live);
        return true;
    }

    private void markProtectionCleanupUnresolved(ActiveRuntime runtime, QuantStrategyPositionState state, String reason) {
        runtime.protectionCleanupPending = true;
        if (state != null) {
            // 只记录“需恢复”，不能在交易所事实未知时伪造 CLOSED 或清除待执行身份。
            state.setRecoveryRequired(true);
            state.setUnsafeRecovery(true);
            stateService.save(state);
        }
        runtime.instance.setLastError(reason + "，已暂停新开仓并等待再次核实");
        instanceMapper.updateById(runtime.instance);
    }

    /**
     * 加载 Runtime 启动所需的初始 K 线和价格快照。
     */
    private MarketRuntime loadInitialMarket(String runMode, String symbol, String interval, Map<String, Object> params, boolean requireSlopeReference) {
        int limit = Math.max(KlineSlopeRateStrategy.SLOPE_LOOKBACK_BARS + 1, positiveInt(params.get("historyLimit"), 50));
        List<ExchangeGateway.Kline> history = "TESTNET".equalsIgnoreCase(runMode) ? gateway.queryKlines(runMode, symbol, interval, limit) : gateway.queryKlines(symbol, interval,
            limit);
        if (requireSlopeReference && (history == null || history.size() < KlineSlopeRateStrategy.SLOPE_LOOKBACK_BARS + 1)) {
            throw new IllegalStateException("策略参考K线不足，等待历史预热: symbol=" + symbol + " interval=" + interval);
        }
        history = history.stream().sorted(java.util.Comparator.comparing(ExchangeGateway.Kline::openTime)).toList();
        MarketRuntime runtime = new MarketRuntime();
        ExchangeGateway.Kline previousClosed = null;
        for (ExchangeGateway.Kline kline : history) {
            runtime.updateKline(kline);
            if (kline.closed()) {
                previousClosed = kline;
            }
        }
        runtime.previousClosedClose = previousClosed == null ? null : previousClosed.close();
        if (requireSlopeReference) {
            BigDecimal reference = runtime.previousKlineOpenPrice();
            if (reference == null || reference.signum() <= 0) {
                throw new IllegalStateException("策略参考K线价格无效，等待历史预热: symbol=" + symbol);
            }
            for (int i = history.size() - KlineSlopeRateStrategy.SLOPE_LOOKBACK_BARS; i < history.size(); i++) {
                if (!history.get(i - 1).closeTime().plusMillis(1).equals(history.get(i).openTime())) {
                    throw new IllegalStateException("策略参考K线不连续，等待历史补偿: symbol=" + symbol);
                }
            }
        }
        return runtime;
    }

    /**
     * 更新开仓后的最高/最低价格。
     */
    private boolean updateRuntimeExtrema(ActiveRuntime runtime, BigDecimal price) {
        if (runtime.state == null || runtime.position == null || price == null || price.signum() <= 0) {
            return !runtime.stateRefreshRequired;
        }
        boolean changed = false;
        boolean priceExtremaChanged = false;
        if ("SHORT".equalsIgnoreCase(runtime.position.getPositionSide())) {
            if (runtime.state.getLowestPriceSinceEntry() == null || price.compareTo(runtime.state.getLowestPriceSinceEntry()) < 0) {
                runtime.state.setLowestPriceSinceEntry(price);
                changed = true;
                priceExtremaChanged = true;
            }
        } else if (runtime.state.getHighestPriceSinceEntry() == null || price.compareTo(runtime.state.getHighestPriceSinceEntry()) > 0) {
            runtime.state.setHighestPriceSinceEntry(price);
            changed = true;
            priceExtremaChanged = true;
        }
        if (runtime.plugin instanceof KlineSlopeRateStrategy && hasFrozenProtectionFacts(runtime)) {
            int reached = KlineSlopeRateStrategy.reachedStep(buildContext(runtime, price, new ExchangeGateway.MarketEvent("EXTREMA", runtime.symbol.getSymbol(),
             runtime.strategySymbol.getIntervalCode(), price, null, Instant.now())), frozenParams(runtime));
            if (runtime.state.getHighestStep() == null || reached > runtime.state.getHighestStep()) {
                runtime.state.setHighestStep(reached);
                changed = true;
            }
        }
        if (changed) {
            runtime.extremaPersistencePending = true;
        }
        runtime.extremaPriceEventPending |= priceExtremaChanged;
        if (runtime.stateRefreshRequired) {
            return false;
        }
        if (runtime.extremaPersistencePending) {
            try {
                stateService.save(runtime.state);
            } catch (org.dromara.quant.runtime.service.StrategyPositionStateService.StateConflictException ex) {
                // 状态竞争不属于行情传输故障：保留本周期极值，暂停决策并由已有恢复线程重新取证。
                runtime.stateRefreshRequired = true;
                invalidatePositionFacts(runtime);
                runtime.nextReconcileAt = 0L;
                log.warn("持仓极值保存遇到状态版本冲突，保留行情连接并等待持仓重核 instanceId={} symbol={} version={}",
                    runtime.instance.getId(), runtime.symbol.getSymbol(), runtime.state.getVersion());
                requestReconciliation(runtime);
                return false;
            }
            runtime.extremaPersistencePending = false;
            if (runtime.extremaPriceEventPending) {
                publishPositionPerformance(runtime);
                runtime.extremaPriceEventPending = false;
            }
        }
        return true;
    }

    /** Persist the peak from the same fresh mark-price return-rate samples shown in the terminal. */
    private void updateRuntimeMaximumReturnRate(ActiveRuntime runtime, BigDecimal markPrice) {
        QuantStrategyPositionState state = runtime.state;
        QuantPosition position = runtime.position;
        if (state == null || position == null || state.getPositionId() == null || state.getPositionCycleKey() == null) {
            return;
        }
        Integer leverage = state.getFrozenLeverage() != null ? state.getFrozenLeverage() : position.getLeverage();
        BigDecimal rate = StrategyPositionReturnRate.calculate(position.getPositionSide(), position.getQuantity(), position.getEntryPrice(), markPrice, leverage);
        if (rate == null) {
            return;
        }
        BigDecimal peak = state.getMaximumReturnRate() == null ? BigDecimal.ZERO.max(rate) : state.getMaximumReturnRate().max(rate);
        boolean newPeak = state.getMaximumReturnRate() == null || peak.compareTo(state.getMaximumReturnRate()) > 0;
        if (!newPeak && !runtime.maximumReturnRatePersistencePending) {
            return;
        }
        state.setMaximumReturnRate(peak);
        try {
            int updated = stateService.updateMaximumReturnRate(state.getInstanceId(), state.getPositionId(), state.getPositionCycleKey(), peak);
            if (updated != 1) {
                runtime.maximumReturnRatePersistencePending = true;
                log.warn("持仓最大回报率未写入当前持仓周期，等待后续标记价重试 instanceId={} positionId={} symbol={}", runtime.instance.getId(), state.getPositionId(), runtime.symbol.getSymbol());
                return;
            }
            runtime.maximumReturnRatePersistencePending = false;
            publishPositionPerformance(runtime);
        } catch (RuntimeException ex) {
            runtime.maximumReturnRatePersistencePending = true;
            log.warn("持仓最大回报率持久化失败，保留内存峰值并等待新标记价重试 instanceId={} positionId={} errorType={}",
                runtime.instance.getId(), state.getPositionId(), ex.getClass().getSimpleName());
        }
    }

    /** 重载完整状态时只合并同一持仓周期的未落库极值，绝不把旧数量、待确认动作或恢复位覆盖回去。 */
    private void replaceRuntimeStatePreservingExtrema(ActiveRuntime runtime, QuantStrategyPositionState latest) {
        if (latest == null) {
            throw new IllegalStateException("策略持仓状态行缺失，禁止按本地旧状态恢复: instanceId=" + runtime.instance.getId());
        }
        QuantStrategyPositionState previous = runtime.state;
        boolean sameCycle = previous != null && previous.getPositionCycleKey() != null
            && previous.getPositionId() != null && Objects.equals(previous.getPositionCycleKey(), latest.getPositionCycleKey())
            && Objects.equals(previous.getPositionId(), latest.getPositionId()) && Objects.equals(previous.getDirection(), latest.getDirection())
            && !"CLOSED".equals(latest.getStatus()) && latest.getPositionQty() != null && latest.getPositionQty().signum() > 0;
        if (runtime.extremaPersistencePending && sameCycle) {
            if (previous.getHighestPriceSinceEntry() != null) {
                latest.setHighestPriceSinceEntry(latest.getHighestPriceSinceEntry() == null ? previous.getHighestPriceSinceEntry()
                    : latest.getHighestPriceSinceEntry().max(previous.getHighestPriceSinceEntry()));
            }
            if (previous.getLowestPriceSinceEntry() != null) {
                latest.setLowestPriceSinceEntry(latest.getLowestPriceSinceEntry() == null ? previous.getLowestPriceSinceEntry()
                    : latest.getLowestPriceSinceEntry().min(previous.getLowestPriceSinceEntry()));
            }
            // 加仓/部分成交可能改变成本和杠杆；旧阶梯只有在收益率口径相同时才能继承。
            if (previous.getHighestStep() != null && previous.getEntryPrice() != null && latest.getEntryPrice() != null
                && previous.getEntryPrice().compareTo(latest.getEntryPrice()) == 0 && Objects.equals(previous.getFrozenLeverage(), latest.getFrozenLeverage())) {
                latest.setHighestStep(Math.max(previous.getHighestStep(), latest.getHighestStep() == null ? 0 : latest.getHighestStep()));
            }
        } else {
            runtime.extremaPersistencePending = false;
            runtime.extremaPriceEventPending = false;
        }
        if (sameCycle && previous.getMaximumReturnRate() != null) {
            latest.setMaximumReturnRate(latest.getMaximumReturnRate() == null ? previous.getMaximumReturnRate()
                : latest.getMaximumReturnRate().max(previous.getMaximumReturnRate()));
        } else if (!sameCycle) {
            runtime.maximumReturnRatePersistencePending = false;
        }
        runtime.state = latest;
        runtime.partialOpenProtectionVerified = false;
    }

    /** 仅在极值状态成功持久化后推送，避免终端显示未落库的峰值。 */
    private void publishPositionPerformance(ActiveRuntime runtime) {
        if (positionPerformanceHub == null || runtime.state == null || runtime.credential == null || runtime.symbol == null) {
            return;
        }
        QuantStrategyPositionState state = runtime.state;
        positionPerformanceHub.publish(new StrategyPositionPerformanceEvent(runtime.credential.getId(), runtime.instance.getId(), state.getPositionId(), state.getPositionCycleKey(),
            runtime.symbol.getSymbol(), state.getDirection(), state.getEntryPrice(), state.getInitialPositionQty(), state.getInitialMargin(), state.getHighestPriceSinceEntry(),
            state.getLowestPriceSinceEntry(), state.getMaximumReturnRate(), state.getUpdatedAt()));
    }

    /**
     * 同步 StrategyPlugin 计算出的动态保护单。
     */
    private void syncPluginProtections(ActiveRuntime runtime, StrategyContext context) {
        if (runtime.position == null || runtime.state == null || runtime.hasPendingAction()) {
            return;
        }
        List<ProtectionSignal> signals = protectionSignals(runtime, context, frozenParams(runtime));
        String target =
         runtime.position.getId() + ":" + runtime.position.getPositionSide() + ":" + runtime.position.getQuantity().stripTrailingZeros().toPlainString() + ":" + signals.stream().map(signal -> signal.type() + ":" + signal.orderType() + ":" + signal.quantityBase() + ":" + (signal.quantityRate() == null ? "1" : signal.quantityRate().stripTrailingZeros().toPlainString()) + ":" + (signal.triggerPrice() == null ? "null" : signal.triggerPrice().stripTrailingZeros().toPlainString())).sorted().collect(java.util.stream.Collectors.joining("|"));
        long now = System.nanoTime();
        // 相同目标合并；定期仍交给后台精确查询，变化的阶梯/数量立即同步。
        if (target.equals(runtime.lastProtectionTarget) && now - runtime.lastProtectionRequestNanos < java.util.concurrent.TimeUnit.SECONDS.toNanos(15)) {
            return;
        }
        runtime.lastProtectionTarget = target;
        runtime.lastProtectionRequestNanos = now;
        requestProtectionSync(runtime, true);
    }

    /**
     * 解析当前持仓应使用的固定硬止损比例。
     */
    private BigDecimal resolveHardStopRatio(ActiveRuntime runtime) {
        // 1. 已冻结的历史止损比例优先（开仓时快照，中途不得改变）
        if (runtime.position != null && runtime.state != null && runtime.state.getFrozenStopLossRatio() != null && runtime.state.getFrozenStopLossRatio().signum() > 0) {
            return runtime.state.getFrozenStopLossRatio();
        }
        // 3. 策略默认止损比例
        if (runtime.strategy.getDefaultStopLossRatio() != null && runtime.strategy.getDefaultStopLossRatio().signum() > 0) {
            return runtime.strategy.getDefaultStopLossRatio();
        }
        // 4. 策略固定硬止损
        if (runtime.strategy.getStopLossRatio() != null && runtime.strategy.getStopLossRatio().signum() > 0) {
            return runtime.strategy.getStopLossRatio();
        }
        // 5. 交易对级保证金亏损比例
        if (runtime.strategySymbol.getHardStopMarginLossRatio() != null && runtime.strategySymbol.getHardStopMarginLossRatio().signum() > 0) {
            return runtime.strategySymbol.getHardStopMarginLossRatio();
        }
        // 6. 策略级保证金亏损比例
        return runtime.strategy.getHardStopMarginLossRatio();
    }

    /**
     * 合并策略默认参数与交易对级参数覆盖。
     */
    private Map<String, Object> mergedParams(QuantStrategy strategy, QuantStrategySymbol symbol) {
        Map<String, Object> result = new HashMap<>(parseParams(strategy.getParamsJson()));
        result.putAll(parseParams(symbol.getParamsJson()));
        return result;
    }

    /**
     * 解析对应业务数据。
     */
    private Map<String, Object> parseParams(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return QuantJsonMapper.readMap(json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("策略参数 JSON 无效", ex);
        }
    }

    /**
     * 序列化并保存策略参数 JSON。
     */
    private String writeParams(Map<String, Object> params) {
        try {
            return QuantJsonMapper.writeMap(params);
        } catch (Exception ex) {
            throw new IllegalArgumentException("策略参数快照序列化失败", ex);
        }
    }

    /**
     * 记录策略实例最近一次运行错误。
     */
    private void recordError(QuantStrategyInstance instance, RuntimeException ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        instance.setLastError(message);
        instanceMapper.updateById(instance);
        if (ex instanceof org.dromara.quant.exchange.ExchangeMarketPort.RequestDeferredException) {
            log.warn("策略对账因本地REST拥堵延后 instanceId={}: {}", instance.getId(), message);
        } else {
            log.error("策略运行时执行失败 instanceId={}: {}", instance.getId(), message, ex);
        }
    }

    /**
     * 停止当前节点的全部策略 Runtime。
     */
    @PreDestroy
    public void destroy() {
        stopAll();
        recoveryExecutor.shutdownNow();
        leaseRenewalExecutor.shutdownNow();
    }

    /**
     * 关闭运行实例并释放订阅、租约和调度资源。
     */
    private void stopAll() {
        activeRuntimes.values().forEach(this::closeRuntime);
        activeRuntimes.clear();
    }

    /**
     * 释放单个 Runtime 的订阅和租约资源。
     */
    private void closeRuntime(ActiveRuntime runtime) {
        if (runtime == null) {
            return;
        }
        preparationService.release(runtime.credential.getId(), runtime.symbol.getSymbol());
        java.util.concurrent.Future<?> protectionTask;
        java.util.concurrent.Future<?> exitTask;
        synchronized (runtime) {
            runtime.closed = true;
            protectionTask = runtime.protectionTask;
            exitTask = runtime.exitTask;
            if (runtime.subscription != null) {
                try {
                    runtime.subscription.close();
                } catch (Exception ignored) {
                }
                runtime.subscription = null;
            }
            if (runtime.userSubscription != null) {
                try {
                    runtime.userSubscription.close();
                } catch (Exception ignored) {
                }
                runtime.userSubscription = null;
            }
        }
        // Drain in-flight protection/exit work without the market lock. Keep renewing the
        // lease while waiting so another node cannot take over a still-running task.
        boolean interrupted = false;
        if (protectionTask != null) {
            try {
                interrupted |= awaitTaskWithLeaseRenewal(runtime, protectionTask);
            } catch (java.util.concurrent.CancellationException ex) {
                log.error("保护任务被提前取消，暂不释放租约 instanceId={}", runtime.instance.getId());
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
        }
        if (exitTask != null) {
            try {
                interrupted |= awaitTaskWithLeaseRenewal(runtime, exitTask);
            } catch (java.util.concurrent.CancellationException ex) {
                log.error("平仓任务被提前取消，暂不释放租约 instanceId={}", runtime.instance.getId());
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                return;
            }
        }
        boolean leaseLockAcquired = false;
        try {
            // Never block shutdown indefinitely behind a hung lease-renewal database call.
            leaseLockAcquired = runtime.leaseOperationLock.tryLock(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            interrupted = true;
        }
        if (!leaseLockAcquired) {
            runtime.leaseValid = false;
            log.error("策略运行租约操作仍在途，停止等待并保留租约直至自然过期 instanceId={} credentialId={} symbol={}",
                runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol());
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            return;
        }
        try {
            leaseService.release(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol());
        } finally {
            runtime.leaseOperationLock.unlock();
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Waits for a trading task while renewing the lease every three seconds. Interruption is
     * remembered but does not abandon an in-flight exchange side effect or release its lease.
     */
    private boolean awaitTaskWithLeaseRenewal(ActiveRuntime runtime, java.util.concurrent.Future<?> task) {
        boolean interrupted = false;
        for (;;) {
            try {
                task.get(3, java.util.concurrent.TimeUnit.SECONDS);
                return interrupted;
            } catch (java.util.concurrent.TimeoutException timeout) {
                if (!"PAPER".equalsIgnoreCase(runtime.credential.getRunMode())) {
                    boolean leaseLockAcquired = false;
                    try {
                        leaseLockAcquired = runtime.leaseOperationLock.tryLock(1, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (InterruptedException ex) {
                        interrupted = true;
                    }
                    if (!leaseLockAcquired) {
                        runtime.leaseValid = false;
                        log.error("关闭运行时等待在途交易期间无法取得租约续期锁，继续等待交易结果且不释放租约 instanceId={} credentialId={} symbol={}",
                            runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol());
                        continue;
                    }
                    try {
                        boolean renewed = leaseService.acquireOrRenew(runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol());
                        if (!renewed) {
                            runtime.leaseValid = false;
                            log.error("关闭运行时等待在途交易期间租约续期失败，保持关闭并禁止接管假设 instanceId={} credentialId={} symbol={}",
                                runtime.instance.getId(), runtime.credential.getId(), runtime.symbol.getSymbol());
                        }
                    } finally {
                        runtime.leaseOperationLock.unlock();
                    }
                }
            } catch (InterruptedException ex) {
                interrupted = true;
            } catch (java.util.concurrent.ExecutionException ex) {
                return interrupted;
            } catch (java.util.concurrent.CancellationException ex) {
                throw ex;
            }
        }
    }

    private record ProtectionWork(Long credentialId, QuantPosition position, QuantStrategyPositionState state, BigDecimal ratio, BigDecimal initialQuantity,
                                  List<ProtectionSignal> signals, boolean dynamic, boolean live, RuntimeException signalFailure) {
    }

    private record ProtectionCleanupResult(boolean exchangeFlat, boolean cleaned) {
    }

    /**
     * 执行恢复流程使用的轻量执行状态投影。
     *
     * @param value 内部状态值
     */
    private record QuantExecutionRecordStatus(String value) {
    }

    /**
     * 单个策略 Runtime 维护的实时市场状态缓存。
     */
    private static final class MarketRuntime {
        /**
         * 当前实时 K 线快照。
         */
        private ExchangeGateway.Kline currentKline;
        /**
         * 上一根已收盘 K 线收盘价。
         */
        private BigDecimal previousClosedClose;
        /**
         * 斜率策略使用的上一根参考 K 线开盘价。
         */
        private BigDecimal previousKlineOpen;
        /**
         * 往前第二根参考 K 线开盘价。
         */
        private BigDecimal secondPreviousKlineOpen;
        /**
         * 往前第三根参考 K 线开盘价。
         */
        private BigDecimal thirdPreviousKlineOpen;
        /**
         * 最近一次收到 K 线事件的时间。
         */
        private Instant lastKlineEventAt;

        /**
         * 更新对应业务状态。
         */
        private void updateKline(ExchangeGateway.Kline kline) {
            if (kline == null) {
                return;
            }
            if (currentKline != null && kline.openTime().isBefore(currentKline.openTime())) {
                return;
            }
            if (currentKline != null && !currentKline.openTime().equals(kline.openTime())) {
                thirdPreviousKlineOpen = secondPreviousKlineOpen;
                secondPreviousKlineOpen = previousKlineOpen;
                previousKlineOpen = currentKline.open();
                if (currentKline.closed()) {
                    previousClosedClose = currentKline.close();
                }
            }
            currentKline = kline;

            if (kline.closed()) {
                previousClosedClose = kline.close();
            }
        }

        /**
         * 返回当前策略参考价格。
         */
        private BigDecimal referencePrice() {
            return previousClosedClose;
        }

        /**
         * 返回斜率计算使用的上一根参考 K 线开盘价。
         */
        private BigDecimal previousKlineOpenPrice() {
            return switch (KlineSlopeRateStrategy.SLOPE_LOOKBACK_BARS) {
                case 2 -> secondPreviousKlineOpen;
                case 3 -> thirdPreviousKlineOpen;
                default -> previousKlineOpen;
            };
        }

        /**
         * 返回当前 K 线开始时间。
         */
        private Instant currentKlineOpen() {
            return currentKline == null ? null : currentKline.openTime();
        }

        /**
         * 判断当前 K 线是否已收盘。
         */
        private boolean currentKlineClosed() {
            return currentKline != null && currentKline.closed();
        }
    }

    /**
     * 单个策略实例在当前 JVM 节点上的活动运行上下文。
     */
    private final class ActiveRuntime {
        private final DecisionLogCursor decisionLogCursor = new DecisionLogCursor();
        /**
         * 交易对，例如 BTCUSDT。
         */
        private final QuantSymbol symbol;
        /**
         * 当前实例对应的 StrategyPlugin。
         */
        private final StrategyPlugin plugin;
        /**
         * 当前策略 Runtime 的实时市场状态。
         */
        private final MarketRuntime market = new MarketRuntime();
        private final AtomicBoolean protectionSyncing = new AtomicBoolean();
        private final AtomicBoolean exitCoordinating = new AtomicBoolean();
        private final AtomicBoolean protectionRequested = new AtomicBoolean();
        private final AtomicBoolean dynamicProtectionRequested = new AtomicBoolean();
        private final AtomicBoolean reconciling = new AtomicBoolean();
        private final AtomicBoolean leaseRenewing = new AtomicBoolean();
        /** Serializes database lease acquisition/renewal with the final release during shutdown. */
        private final ReentrantLock leaseOperationLock = new ReentrantLock();
        private final AtomicBoolean reconcileRequested = new AtomicBoolean();
        /** Next allowed WARN timestamp for repeated pending-action decisions. */
        private volatile long nextPendingActionWarnAt;
        private volatile String lastPendingOrderQueryFailure;
        /** Throttle repeated diagnostics when an UNKNOWN order query keeps failing. */
        private volatile long lastPendingOrderQueryFailureLogAt;
        private final EntryRetryGate entryRetryGate = new EntryRetryGate();
        /** 串行化交易对暂停操作与完整开仓决策/派单流程。 */
        private final Object entryDispatchLock = new Object();
        /** 单个交易对运行开关；关闭时只禁止新开仓，不停止已有持仓管理。 */
        private volatile boolean runtimeEnabled = true;
        /** Updated only by startup/independent lease supervision; false fails closed. */
        private volatile boolean leaseValid;
        /**
         * FIXED: 仅保护模式；策略/凭证/交易对停用但仍有未平仓持仓时为 true。
         */
        private volatile boolean protectionOnly;
        /**
         * 当前策略运行实例定义。
         */
        private QuantStrategyInstance instance;
        /**
         * 当前策略定义。
         */
        private QuantStrategy strategy;
        /**
         * 当前策略交易对配置。
         */
        private QuantStrategySymbol strategySymbol;
        /**
         * 当前运行上下文使用的交易凭证。
         */
        private QuantCredential credential;
        /**
         * 当前实例合并后的策略参数。
         */
        private Map<String, Object> params;
        /**
         * 当前交易对本地持仓事实。
         */
        private volatile QuantPosition position;
        /**
         * 当前策略持仓运行状态。
         */
        private volatile QuantStrategyPositionState state;
        /**
         * 当前 Runtime 持有的行情订阅句柄。
         */
        private volatile AutoCloseable subscription;
        private volatile AutoCloseable userSubscription;
        private volatile java.util.concurrent.Future<?> protectionTask;
        private long nextPreparationNoticeAt;
        private volatile java.util.concurrent.Future<?> exitTask;
        private ExchangeGateway.SymbolRule protectionRule;
        private BigDecimal exitWindowHigh;
        private BigDecimal exitWindowLow;
        private String lastProtectionTarget;
        private long lastProtectionRequestNanos;
        private volatile boolean protectionCleanupPending;
        private boolean extremaPersistencePending;
        private boolean maximumReturnRatePersistencePending;
        private boolean extremaPriceEventPending;
        /** 状态版本竞争后的重载屏障；行情仍接收，核实完成前不得按旧状态决策。 */
        private volatile boolean stateRefreshRequired;
        private volatile boolean positionReconciling;
        private volatile boolean positionFactsReady;
        /** Invalidates an in-flight reconciliation when a newer private-stream or state-conflict fact arrives. */
        private final java.util.concurrent.atomic.AtomicLong positionFactsInvalidationGeneration = new java.util.concurrent.atomic.AtomicLong();
        /** 最近一次未完成的持仓核验阶段，用于解释开仓门控并安排独立重试。 */
        private volatile String positionReconciliationBlockReason = "NOT_STARTED";
        private volatile long positionFactsVerifiedAt;
        private volatile long nextPositionReconcilingLogAt;
        private volatile long nextRecoveryRequiredLogAt;
        private long nextProtectionSyncAt;
        private volatile boolean protectionDegraded;
        private volatile boolean entryProtectionCleanupReady;
        private boolean entryProtectionCleanupRunning;
        private final java.util.concurrent.atomic.AtomicLong entryProtectionCleanupGeneration = new java.util.concurrent.atomic.AtomicLong();
        private long entryProtectionCleanupProofGeneration;
        private long entryProtectionCleanupEpoch;
        private Instant entryProtectionCleanupConfirmedAt;
        private long nextEntryProtectionCleanupAt;
        private java.util.concurrent.Future<?> entryProtectionCleanupTask;
        private boolean hardStopConfirmed;
        private boolean partialOpenProtectionVerified;
        private BigDecimal latestTradePrice;
        private Instant latestTradeEventAt;
        // 健康回调直接关闭决策，不能等待普通行情队列的断线通知。
        private volatile boolean marketStreamReady;
        private volatile long nextReconcileAt;
        /**
         * 最近一次参与开仓判断的 K 线开始时间。
         */
        private volatile Instant lastEvaluatedKlineOpenTime;
        /**
         * 下一次运行租约续期时间。
         */
        /**
         * REST 账户余额查询失败后的下一次允许查询时间。
         */
        private long nextLatencyLogAt;
        private long nextPendingSlopeLogNanos;
        private Instant lastEntryAttemptEventTime;
        private BigDecimal lastEntryAttemptPrice;
        /**
         * 当前行情流是否健康。
         */
        private volatile boolean marketHealthy;
        /**
         * 组件/Runtime 是否已经关闭。
         */
        private volatile boolean closed;

        private long nextSlopeDiagLogAt;
        private long nextMarketUnreadyLogAt;
        private volatile long nextExpiredMarketLogAt;
        private long marketUnreadySinceAt;

        /**
         * 创建单个策略实例的活动 Runtime 上下文。
         */
        private ActiveRuntime(QuantStrategyInstance instance, QuantStrategy strategy, QuantStrategySymbol strategySymbol, QuantCredential credential, QuantSymbol symbol,
StrategyPlugin plugin, Map<String, Object> params) {
            this.instance = instance;
            this.strategy = strategy;
            this.strategySymbol = strategySymbol;
            this.runtimeEnabled = !Boolean.FALSE.equals(strategySymbol.getRuntimeEnabled());
            this.credential = credential;
            this.symbol = symbol;
            this.plugin = plugin;
            this.params = params;
            MarketRuntime initial = loadInitialMarket(credential.getRunMode(), symbol.getSymbol(), strategySymbol.getIntervalCode(), params,
             plugin instanceof KlineSlopeRateStrategy);
            this.market.previousClosedClose = initial.previousClosedClose;
            this.market.previousKlineOpen = initial.previousKlineOpen;
            this.market.secondPreviousKlineOpen = initial.secondPreviousKlineOpen;
            this.market.thirdPreviousKlineOpen = initial.thirdPreviousKlineOpen;
            this.market.currentKline = initial.currentKline;
            this.market.lastKlineEventAt = initial.lastKlineEventAt;
        }

        /**
         * 判断事件是否匹配当前订阅。
         */
        private boolean matches(String pluginCode, String symbolCode, String interval, Long credentialId) {
            return strategy.getPluginCode().equals(pluginCode) && symbol.getSymbol().equalsIgnoreCase(symbolCode) && strategySymbol.getIntervalCode().equals(interval) && credential.getId().equals(credentialId);
        }

        /**
         * 更新对应业务状态。
         */
        private void updateKline(ExchangeGateway.Kline kline) {
            market.updateKline(kline);
        }

        /**
         * 返回策略决策使用的当前实时价格。
         */
        private BigDecimal currentPrice() {
            Instant deadline = Instant.now().minusMillis(Math.max(1, properties.getMarket().getMaxEventAgeMillis()));
            if (latestTradePrice != null && latestTradeEventAt != null && !latestTradeEventAt.isBefore(deadline) && (market.lastKlineEventAt == null || !latestTradeEventAt.isBefore(market.lastKlineEventAt))) {
                return latestTradePrice;
            }
            if (market.currentKline == null) {
                return null;
            }
            return market.currentKline.close();
        }

        /**
         * 判断当前策略持仓是否存在待确认交易动作。
         */
        private boolean hasPendingAction() {
            // An incomplete persisted intent is still unresolved; missing time must not make it look safe to trade.
            return state != null && state.getPendingAction() != null;
        }
    }
}
