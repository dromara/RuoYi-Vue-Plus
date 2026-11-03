package org.dromara.quant.reconcile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.quant.account.domain.QuantAccountSnapshot;
import org.dromara.quant.account.domain.QuantTrade;
import org.dromara.quant.account.mapper.QuantAccountSnapshotMapper;
import org.dromara.quant.account.mapper.QuantTradeMapper;
import org.dromara.quant.credential.domain.QuantCredential;
import org.dromara.quant.decision.TradeDecisionLogService;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.exchange.ExchangeMarketPort;
import org.dromara.quant.execution.StrictBinanceTradeCollector;
import org.dromara.quant.execution.StrategyFillJournalService;
import org.dromara.quant.finance.domain.QuantFinancialEvent;
import org.dromara.quant.finance.domain.vo.QuantFinancialEventVo;
import org.dromara.quant.finance.mapper.QuantFinancialEventMapper;
import org.dromara.quant.order.domain.QuantOrder;
import org.dromara.quant.order.mapper.QuantOrderMapper;
import org.dromara.quant.position.domain.QuantPosition;
import org.dromara.quant.position.mapper.QuantPositionMapper;
import org.dromara.quant.protection.domain.QuantProtectionOrder;
import org.dromara.quant.protection.mapper.QuantProtectionOrderMapper;
import org.dromara.quant.reconcile.domain.QuantReconcileRecord;
import org.dromara.quant.reconcile.mapper.LivePositionSnapshotFenceMapper;
import org.dromara.quant.reconcile.mapper.QuantReconcileRecordMapper;
import org.dromara.quant.reconcile.mapper.StrategyFillSyncCursorMapper;
import org.dromara.quant.reconcile.mapper.FinancialEventSyncCursorMapper;
import org.dromara.quant.risk.mapper.OpenCapacityMapper;
import org.dromara.quant.runtime.service.StrategyPositionStateService;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 交易所事实与本地交易账本的最终一致性入口。
 * <p>订单使用历史订单接口而非仅当前挂单接口；成交按最近窗口持续同步，避免成交后从 openOrders 中消失导致本地状态停在 NEW。
 */
@Service
@Slf4j
public class TradingReconciliationService {
    /**
     * 交易所统一网关。
     */
    private final ExchangeGateway gateway;
    /**
     * 账户快照 Mapper。
     */
    private final QuantAccountSnapshotMapper accountSnapshotMapper;
    /**
     * 持仓 Mapper。
     */
    private final QuantPositionMapper positionMapper;
    /**
     * 订单 Mapper。
     */
    private final QuantOrderMapper orderMapper;
    /**
     * 成交 Mapper。
     */
    private final QuantTradeMapper tradeMapper;
    /**
     * 保护单 Mapper。
     */
    private final QuantProtectionOrderMapper protectionOrderMapper;
    /**
     * 财务事件 Mapper。
     */
    private final QuantFinancialEventMapper financialEventMapper;
    /**
     * 对账记录 Mapper。
     */
    private final QuantReconcileRecordMapper reconcileMapper;
    private final ReconciliationFailureRecorder failureRecorder;
    private final StrategyPositionStateService strategyPositionStateService;
    private final TransactionTemplate transaction;
    private final ConcurrentHashMap<Long, ReentrantLock> positionLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CompletableFuture<List<ExchangeGateway.PositionSnapshot>>> positionRefreshes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CachedPositionSnapshot> runtimePositionSnapshots = new ConcurrentHashMap<>();
    private final Set<Long> balanceSnapshotTasks = ConcurrentHashMap.newKeySet();
    /** Retry read-only snapshot refreshes after REST deferral/rate limits; never retry order mutations. */
    private final ScheduledExecutorService positionRetryExecutor = Executors.newSingleThreadScheduledExecutor(
        Thread.ofPlatform().daemon().name("quant-position-snapshot-retry").factory());
    private final Map<Long, ScheduledFuture<?>> positionRetryTasks = new HashMap<>();
    private final ConcurrentHashMap<Long, java.util.concurrent.atomic.AtomicLong> runtimeSnapshotGenerations = new ConcurrentHashMap<>();
    private static final Duration RUNTIME_POSITION_CACHE_AGE = Duration.ofSeconds(20);
    @Autowired
    private TradeDecisionLogService decisionLogs;
    private final StrictBinanceTradeCollector tradeCollector;
    private final org.dromara.quant.execution.StrictBinanceIncomeCollector incomeCollector;
    private final StrategyFillJournalService fillJournal;
    private final StrategyFillSyncCursorMapper fillSyncCursors;
    private final StrategyFillSyncCursorService fillSyncCursorService;
    private final FinancialEventSyncCursorMapper incomeSyncCursors;
    private final FinancialEventSyncCursorService incomeSyncCursorService;
    @Autowired(required = false)
    private LivePositionFreshness positionFreshness;
    @Autowired
    private LivePositionSnapshotFenceMapper snapshotFence;
    /**
     * Share quant_credential FOR UPDATE with OpenCapacityService.reserve().
     * This serializes position facts and new capacity reservations across nodes.
     */
    @Autowired
    private OpenCapacityMapper openCapacityMapper;
    /**
     * 创建 TradingReconciliationService 实例并初始化其依赖。
     */
    public TradingReconciliationService(ExchangeGateway gateway, QuantAccountSnapshotMapper accountSnapshotMapper, QuantPositionMapper positionMapper,
                                        QuantOrderMapper orderMapper, QuantTradeMapper tradeMapper, QuantProtectionOrderMapper protectionOrderMapper,
                                        QuantFinancialEventMapper financialEventMapper, QuantReconcileRecordMapper reconcileMapper, ReconciliationFailureRecorder failureRecorder
        , StrategyPositionStateService strategyPositionStateService, PlatformTransactionManager transactionManager, StrictBinanceTradeCollector tradeCollector,
                                        org.dromara.quant.execution.StrictBinanceIncomeCollector incomeCollector, StrategyFillJournalService fillJournal, StrategyFillSyncCursorMapper fillSyncCursors, StrategyFillSyncCursorService fillSyncCursorService,
                                        FinancialEventSyncCursorMapper incomeSyncCursors, FinancialEventSyncCursorService incomeSyncCursorService) {
        this.gateway = gateway;
        this.accountSnapshotMapper = accountSnapshotMapper;
        this.positionMapper = positionMapper;
        this.orderMapper = orderMapper;
        this.tradeMapper = tradeMapper;
        this.protectionOrderMapper = protectionOrderMapper;
        this.financialEventMapper = financialEventMapper;
        this.reconcileMapper = reconcileMapper;
        this.failureRecorder = failureRecorder;
        this.strategyPositionStateService = strategyPositionStateService;
        this.tradeCollector = tradeCollector;
        this.incomeCollector = incomeCollector;
        this.fillJournal = fillJournal;
        this.fillSyncCursors = fillSyncCursors;
        this.fillSyncCursorService = fillSyncCursorService;
        this.incomeSyncCursors = incomeSyncCursors;
        this.incomeSyncCursorService = incomeSyncCursorService;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.transaction.setTimeout(30);
    }
    /**
     * Commit an account's verified REST snapshot under a database row lock.
     * Earlier-started requests cannot overwrite later-started snapshots.
     */

    /**
     * 统一保护单持仓方向，兼容单向/双向持仓模式。
     */
    private static String normalizeProtectionSide(String value) {
        if (value == null || value.isBlank() || "BOTH".equalsIgnoreCase(value)) {
            return null;
        }
        if ("LONG".equalsIgnoreCase(value)) {
            return "LONG";
        }
        if ("SHORT".equalsIgnoreCase(value)) {
            return "SHORT";
        }
        return value.toUpperCase();
    }

    /**
     * 判断交易所保护单是否仍处于活动状态。
     */
    private static boolean isOpenProtection(String status) {
        return "NEW".equalsIgnoreCase(status) || "WORKING".equalsIgnoreCase(status) || "ACCEPTED".equalsIgnoreCase(status);
    }

    /**
     * 把非空交易对加入本轮待同步集合。
     */
    private static void addSymbol(Set<String> symbols, String symbol) {
        if (symbol != null && !symbol.isBlank()) {
            symbols.add(symbol);
        }
    }

    /**
     * 值为空时返回默认值。
     */
    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private ReentrantLock positionLock(Long credentialId) {
        return positionLocks.computeIfAbsent(credentialId, ignored -> new ReentrantLock());
    }

    /**
     * 仅在刷新代次未变化且快照完整时提交账户持仓事实。
     */
    private void commitPositionSnapshot(QuantCredential credential, ExchangeGateway.AccountSnapshot account, List<ExchangeGateway.PositionSnapshot> positions,
                                        LocalDateTime fetchStartedAt, long eventGeneration, long runtimeGeneration, String trigger) {
        var accountLock = positionLock(credential.getId());
        accountLock.lock();
        try {
            if (positionFreshness != null && positionFreshness.generation(credential.getId()) != eventGeneration) {
                throw new SnapshotSupersededException();
            }
            if (runtimeSnapshotGeneration(credential.getId()) != runtimeGeneration) {
                throw new SnapshotSupersededException("Account position snapshot superseded by a private-stream event");
            }
            Runnable commitSnapshot = () -> transaction.executeWithoutResult(status -> {
                if (openCapacityMapper != null && openCapacityMapper.lockCredential(credential.getId()).isEmpty()) {
                    throw new IllegalStateException("Cannot lock credential for position snapshot reconciliation");
                }
                if (snapshotFence != null) {
                    snapshotFence.ensure(credential.getId());
                    LocalDateTime previous = snapshotFence.lockAndRead(credential.getId());
                    if (previous != null && previous.isAfter(fetchStartedAt)) {
                        throw new IllegalStateException("Binance position snapshot superseded by another node");
                    }
                }
                if (positionFreshness != null && positionFreshness.generation(credential.getId()) != eventGeneration) {
                    throw new SnapshotSupersededException();
                }
                syncAccount(credential, account);
                syncPositions(credential, positions);
                if (snapshotFence != null && snapshotFence.advance(credential.getId(), fetchStartedAt) != 1) {
                    throw new IllegalStateException("Position snapshot fence advance failed");
                }
            });
            if (positionFreshness != null) {
                if (!positionFreshness.runIfGenerationUnchanged(credential.getId(), eventGeneration, commitSnapshot)) {
                    throw new SnapshotSupersededException("Position snapshot superseded before atomic commit");
                }
            } else {
                commitSnapshot.run();
            }
            if (!markPositionVerified(credential.getId(), eventGeneration)) {
                // A private-stream event can race the final commit/verification boundary.
                // The database may contain this older snapshot, but it must never become
                // a fresh runtime cache entry or release the LIVE entry gate.
                throw new SnapshotSupersededException("Position snapshot changed before freshness verification");
            }
            runtimePositionSnapshots.put(credential.getId(), new CachedPositionSnapshot(eventGeneration, runtimeGeneration, Instant.now(), positions));
        } finally {
            accountLock.unlock();
        }
        long openPositionCount = positions.stream().filter(p -> p.quantity() != null && p.quantity().signum() != 0).count();
        log.info("账户级持仓快照已提交 credentialId={} runMode={} generation={} trigger={} snapshotCount={} openPositionCount={} elapsedMs={}", credential.getId(), credential.getRunMode(),
            eventGeneration, trigger, positions.size(), openPositionCount, java.time.Duration.between(fetchStartedAt, LocalDateTime.now()).toMillis());
    }

    private long positionGeneration(Long credentialId) {
        return positionFreshness == null ? 0L : positionFreshness.generation(credentialId);
    }

    private long runtimeSnapshotGeneration(Long credentialId) {
        return runtimeSnapshotGenerations.computeIfAbsent(credentialId, ignored -> new java.util.concurrent.atomic.AtomicLong()).get();
    }

    private boolean markPositionVerified(Long credentialId, long generation) {
        return positionFreshness == null || positionFreshness.verifiedIfUnchanged(credentialId, generation);
    }

    /**
     * Startup/triggered position-only refresh. No historical orders or fills
     * are required to persist an authenticated full account position snapshot.
     * Never treat an unavailable exchange response as an empty account.
     */
    public void refreshPositionsOnly(QuantCredential credential) {
        refreshAccountPositionSnapshot(credential, "scheduled-or-event");
    }

    /**
     * Return a recent, complete account snapshot for runtime recovery, fetching and committing one
     * shared snapshot when the cache is missing, stale, or invalidated by a private-stream event.
     */
    public List<ExchangeGateway.PositionSnapshot> runtimePositionSnapshot(QuantCredential credential) {
        if (credential == null || credential.getId() == null) {
            throw new IllegalArgumentException("Credential is required for a runtime position snapshot");
        }
        CachedPositionSnapshot cached = runtimePositionSnapshots.get(credential.getId());
        long generation = positionGeneration(credential.getId());
        long runtimeGeneration = runtimeSnapshotGeneration(credential.getId());
        if (cached != null && cached.positionGeneration() == generation && cached.runtimeGeneration() == runtimeGeneration && Duration.between(cached.fetchedAt(), Instant.now()).compareTo(RUNTIME_POSITION_CACHE_AGE) <= 0
            && (!"LIVE".equalsIgnoreCase(credential.getRunMode()) || positionFreshness == null || positionFreshness.fresh(credential.getId()))) {
            log.debug("运行时复用账户级持仓快照 credentialId={} generation={} ageMs={}", credential.getId(), generation,
                Duration.between(cached.fetchedAt(), Instant.now()).toMillis());
            return cached.positions();
        }
        return refreshAccountPositionSnapshot(credential, "runtime-reconcile");
    }

    /** Invalidate the in-memory account snapshot after a private-stream event. */
    public void invalidateRuntimePositionSnapshot(Long credentialId) {
        if (credentialId != null) {
            var accountLock = positionLock(credentialId);
            accountLock.lock();
            try {
                runtimeSnapshotGenerations.computeIfAbsent(credentialId, ignored -> new java.util.concurrent.atomic.AtomicLong()).incrementAndGet();
                runtimePositionSnapshots.remove(credentialId);
            } finally {
                accountLock.unlock();
            }
        }
    }

    private List<ExchangeGateway.PositionSnapshot> refreshAccountPositionSnapshot(QuantCredential credential, String trigger) {
        if (credential == null || credential.getId() == null) {
            throw new IllegalArgumentException("Credential is required for an account position snapshot");
        }
        long credentialId = credential.getId();
        CompletableFuture<List<ExchangeGateway.PositionSnapshot>> created = new CompletableFuture<>();
        CompletableFuture<List<ExchangeGateway.PositionSnapshot>> existing = positionRefreshes.putIfAbsent(credentialId, created);
        if (existing != null) {
            long waitStartedAt = System.nanoTime();
            List<ExchangeGateway.PositionSnapshot> snapshots = joinPositionRefresh(existing);
            log.debug("运行时等待共享账户持仓快照 credentialId={} trigger={} waitMs={}", credentialId, trigger,
                java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - waitStartedAt));
            return snapshots;
        }
        try {
            long generation = positionGeneration(credentialId);
            long runtimeGeneration = runtimeSnapshotGeneration(credentialId);
            LocalDateTime fetchStartedAt = LocalDateTime.now();
            ExchangeGateway.Credential exchangeCredential = new ExchangeGateway.Credential(credential.getApiKey(), credential.getSecretKey(), credential.getSignatureType(),
                credential.getRunMode());
            // Network I/O stays outside the account write lock and database transaction.
            var account = java.util.Objects.requireNonNull(gateway.queryAccount(exchangeCredential), "Binance account snapshot missing");
            var snapshots = List.copyOf(java.util.Objects.requireNonNull(gateway.queryPositions(exchangeCredential, null), "Binance full position snapshot missing"));
            commitPositionSnapshot(credential, account, snapshots, fetchStartedAt, generation, runtimeGeneration, trigger);
            created.complete(snapshots);
            return snapshots;
        } catch (SnapshotSupersededException ex) {
            // 私有流事件/其他更新已使本次快照过期：不能提交旧快照，也不能把它当成交易所故障。
            // 同时安排一个按 credential 合并的只读刷新，避免仅依赖策略行情事件触发恢复，
            // 否则低流动性交易对可能长期停留在 POSITION_FACTS_UNVERIFIED。
            schedulePositionSnapshotRetry(credential, trigger + ":superseded", 1_000L);
            created.completeExceptionally(ex);
            throw ex;
        } catch (RuntimeException ex) {
            long retryAfterMillis = snapshotRetryAfterMillis(ex);
            if (retryAfterMillis > 0) {
                schedulePositionSnapshotRetry(credential, trigger, retryAfterMillis);
            }
            failureRecorder.record(credentialId, ex.getMessage());
            created.completeExceptionally(ex);
            throw ex;
        } finally {
            positionRefreshes.remove(credentialId, created);
        }
    }

    private long snapshotRetryAfterMillis(RuntimeException failure) {
        if (failure instanceof ExchangeMarketPort.RequestDeferredException deferred) {
            return deferred.retryAfterMillis();
        }
        if (failure instanceof ExchangeMarketPort.RateLimitException rateLimited) {
            return rateLimited.retryAfterMillis();
        }
        return 0L;
    }

    /** Coalesce a delayed, read-only account snapshot retry for each credential. */
    private void schedulePositionSnapshotRetry(QuantCredential credential, String trigger, long retryAfterMillis) {
        if (credential == null || credential.getId() == null || "PAPER".equalsIgnoreCase(credential.getRunMode())) {
            return;
        }
        long credentialId = credential.getId();
        long delay = Math.max(1_000L, retryAfterMillis);
        synchronized (positionRetryTasks) {
            ScheduledFuture<?> current = positionRetryTasks.get(credentialId);
            if (current != null && !current.isDone()) {
                return;
            }
            ScheduledFuture<?> scheduled = positionRetryExecutor.schedule(() -> {
                synchronized (positionRetryTasks) {
                    positionRetryTasks.remove(credentialId);
                }
                try {
                    refreshAccountPositionSnapshot(credential, "retry:" + trigger);
                } catch (RuntimeException retryFailure) {
                    log.warn("账户持仓快照延迟重试仍未成功 credentialId={} trigger={} errorType={}",
                        credentialId, trigger, retryFailure.getClass().getSimpleName());
                }
            }, delay, TimeUnit.MILLISECONDS);
            positionRetryTasks.put(credentialId, scheduled);
            log.info("已安排账户持仓快照延迟重试 credentialId={} trigger={} retryAfterMs={}", credentialId, trigger, delay);
        }
    }

    @PreDestroy
    public void shutdownPositionSnapshotRetries() {
        positionRetryExecutor.shutdownNow();
        synchronized (positionRetryTasks) {
            positionRetryTasks.values().forEach(task -> task.cancel(false));
            positionRetryTasks.clear();
        }
    }

    private List<ExchangeGateway.PositionSnapshot> joinPositionRefresh(CompletableFuture<List<ExchangeGateway.PositionSnapshot>> refresh) {
        try {
            return refresh.join();
        } catch (CompletionException ex) {
            if (ex.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw ex;
        }
    }

    /**
     * Persist one symbol snapshot through the same account lock and cross-node fence as a full snapshot.
     * A symbol snapshot only changes that symbol; it must never be treated as a complete account snapshot.
     */
    public void syncSymbolPositionSnapshot(QuantCredential credential, String symbol, List<ExchangeGateway.PositionSnapshot> snapshots, LocalDateTime fetchStartedAt,
                                           long eventGeneration) {
        if (credential == null || credential.getId() == null) {
            throw new IllegalArgumentException("Credential is required for a symbol position snapshot");
        }
        syncSymbolPositionSnapshot(credential, symbol, snapshots, fetchStartedAt, eventGeneration, runtimeSnapshotGeneration(credential.getId()));
    }

    private void syncSymbolPositionSnapshot(QuantCredential credential, String symbol, List<ExchangeGateway.PositionSnapshot> snapshots, LocalDateTime fetchStartedAt,
                                            long eventGeneration, long runtimeGeneration) {
        if (credential == null || credential.getId() == null || symbol == null || symbol.isBlank() || snapshots == null || fetchStartedAt == null) {
            throw new IllegalArgumentException("Complete symbol position snapshot metadata is required");
        }
        long generation = eventGeneration;
        var accountLock = positionLock(credential.getId());
        accountLock.lock();
        try {
            if (positionFreshness != null && positionFreshness.generation(credential.getId()) != generation) {
                throw new SnapshotSupersededException();
            }
            if (runtimeSnapshotGeneration(credential.getId()) != runtimeGeneration) {
                throw new SnapshotSupersededException("Binance symbol position snapshot superseded by a private-stream event");
            }
            transaction.executeWithoutResult(status -> {
                if (openCapacityMapper != null && openCapacityMapper.lockCredential(credential.getId()).isEmpty()) {
                    throw new IllegalStateException("Cannot lock credential for symbol position snapshot");
                }
                if (snapshotFence != null) {
                    snapshotFence.ensure(credential.getId());
                    LocalDateTime previous = snapshotFence.lockAndRead(credential.getId());
                    if (previous != null && previous.isAfter(fetchStartedAt)) {
                        throw new SnapshotSupersededException("Binance symbol position snapshot superseded by another node");
                    }
                }
                if (positionFreshness != null && positionFreshness.generation(credential.getId()) != generation) {
                    throw new SnapshotSupersededException();
                }
                syncSymbolPosition(credential, symbol, snapshots);
                if (snapshotFence != null && snapshotFence.advance(credential.getId(), fetchStartedAt) != 1) {
                    throw new IllegalStateException("Symbol position snapshot fence advance failed");
                }
            });
            runtimeSnapshotGenerations.computeIfAbsent(credential.getId(), ignored -> new java.util.concurrent.atomic.AtomicLong()).incrementAndGet();
            runtimePositionSnapshots.remove(credential.getId());
        } finally {
            accountLock.unlock();
        }
    }

    /**
     * Fetch and persist a symbol snapshot while serializing same-process requests for this account.
     */
    public List<ExchangeGateway.PositionSnapshot> refreshSymbolPosition(QuantCredential credential, String symbol, ExchangeGateway.Credential exchangeCredential) {
        if (credential == null || credential.getId() == null || symbol == null || symbol.isBlank() || exchangeCredential == null) {
            throw new IllegalArgumentException("Credential, symbol and exchange credentials are required");
        }
        LocalDateTime fetchStartedAt = LocalDateTime.now();
        long generation = positionGeneration(credential.getId());
        long runtimeGeneration = runtimeSnapshotGeneration(credential.getId());
        final List<ExchangeGateway.PositionSnapshot> snapshots;
        try {
            snapshots = List.copyOf(java.util.Objects.requireNonNull(gateway.queryPositions(exchangeCredential, symbol),
                "Binance symbol position snapshot missing"));
        } catch (SnapshotSupersededException ex) {
            // A newer private-stream fact won the race. Schedule a fresh read-only snapshot
            // rather than leaving the strategy blocked until another market event arrives.
            schedulePositionSnapshotRetry(credential, "symbol:" + symbol + ":superseded", 1_000L);
            throw ex;
        } catch (RuntimeException ex) {
            long retryAfterMillis = snapshotRetryAfterMillis(ex);
            if (retryAfterMillis > 0) {
                schedulePositionSnapshotRetry(credential, "symbol:" + symbol, retryAfterMillis);
            }
            throw ex;
        }
        try {
            syncSymbolPositionSnapshot(credential, symbol, snapshots, fetchStartedAt, generation, runtimeGeneration);
        } catch (SnapshotSupersededException ex) {
            // Supersession is detected at commit time, after the REST read. Retry the whole
            // read/commit cycle so the latest facts can converge without waiting for market data.
            schedulePositionSnapshotRetry(credential, "symbol:" + symbol + ":superseded", 1_000L);
            throw ex;
        }
        return snapshots;
    }

    /**
     * 对账指定交易对持仓，保留无法唯一归属的原始策略份额。
     */
    private void syncSymbolPosition(QuantCredential credential, String symbol, List<ExchangeGateway.PositionSnapshot> snapshots) {
        if (snapshots.stream().anyMatch(snapshot -> !symbol.equalsIgnoreCase(snapshot.symbol()))) {
            throw new IllegalStateException("Binance returned a position outside the requested symbol: " + symbol);
        }
        List<ExchangeGateway.PositionSnapshot> symbolFacts = snapshots.stream().filter(snapshot -> symbol.equalsIgnoreCase(snapshot.symbol())).toList();
        long nonZeroCount = symbolFacts.stream().filter(snapshot -> snapshot.quantity() != null && snapshot.quantity().signum() != 0).count();
        if (nonZeroCount > 1) {
            throw new IllegalStateException("同一凭证同一交易对存在多个非零持仓方向，拒绝自动接管: " + symbol);
        }
        syncPositions(credential, symbolFacts, symbol);
    }

    /**
     * Query Binance outside of a transaction; missing facts must not partially overwrite
     * account positions, orders or protections.
     */
    public void reconcileCredential(QuantCredential credential, int historyHours) {
        try {
            long positionGeneration = positionGeneration(credential.getId());
            long runtimeGeneration = runtimeSnapshotGeneration(credential.getId());
            LocalDateTime fetchStartedAt = LocalDateTime.now();
            ExchangeGateway.Credential exchangeCredential = new ExchangeGateway.Credential(credential.getApiKey(), credential.getSecretKey(), credential.getSignatureType(),
                credential.getRunMode());
            // Collect a complete account position snapshot before any DB writes.
            // Commit it independently BEFORE querying order/fill/income history:
            // a failing history endpoint must not keep already-closed Binance
            // positions counted as open indefinitely.
            var account = java.util.Objects.requireNonNull(gateway.queryAccount(exchangeCredential), "Binance account snapshot missing");
            var positions = List.copyOf(java.util.Objects.requireNonNull(gateway.queryPositions(exchangeCredential, null), "Binance full position snapshot missing"));
            commitPositionSnapshot(credential, account, positions, fetchStartedAt, positionGeneration, runtimeGeneration, "full-reconcile");
            ExchangeFacts facts = collectExchangeFacts(credential, historyHours, exchangeCredential, account, positions);
            transaction.executeWithoutResult(status -> applyExchangeFacts(credential, facts));
        } catch (SnapshotSupersededException ex) {
            // The account snapshot lost the generation fence to a newer private-stream fact.
            // Schedule a fresh read-only full reconciliation so this credential can converge
            // even if no subsequent market event arrives for the affected symbols.
            schedulePositionSnapshotRetry(credential, "full-reconcile:superseded", 1_000L);
            throw ex;
        } catch (RuntimeException ex) {
            failureRecorder.record(credential.getId(), ex.getMessage());
            throw ex;
        }
    }

    /**
     * 对单个交易凭证执行完整事实对账。
     */

    /**
     * Binance income history is retained for up to six months. A missing cursor is a
     * bootstrap, not proof that the recent scheduler window is the full history.
     */
    static Instant initialRetainedHistoryStart(Instant now, LocalDateTime storedCursor) {
        if (now == null) {
            throw new IllegalArgumentException("Exchange reconciliation time is required");
        }
        return storedCursor == null ? now.minus(Duration.ofDays(180)) : storedCursor.toInstant(ZoneOffset.UTC);
    }

    /**
     * 收集并校验账户、持仓和成交事实，缺项时不写入部分同步结果。
     */
    private ExchangeFacts collectExchangeFacts(QuantCredential credential, int historyHours, ExchangeGateway.Credential c, ExchangeGateway.AccountSnapshot account,
                                               List<ExchangeGateway.PositionSnapshot> positions) {
        Instant now = Instant.now().plusMillis(gateway.exchangeTimeOffsetMillis(credential.getRunMode()));
        ExchangeGateway.TimeRange range = new ExchangeGateway.TimeRange(now.minus(Duration.ofHours(Math.max(1, historyHours))), now);
        var openOrders = List.copyOf(gateway.queryOrders(c, null));
        var history = List.copyOf(gateway.queryOrderHistory(c, null, range, 1000L));
        Set<String> symbols = new HashSet<>();
        positions.forEach(p -> addSymbol(symbols, p.symbol()));
        history.forEach(o -> addSymbol(symbols, o.symbol()));
        openOrders.forEach(o -> addSymbol(symbols, o.symbol()));
        symbols.addAll(fillSyncCursors.selectSymbols(credential.getId()));
        symbols.addAll(orderMapper.selectHistoricalSymbolsByCredential(credential.getId()));
        Map<String, List<ExchangeGateway.TradeSnapshot>> trades = new HashMap<>();
        Map<String, Instant> fillCursorEnds = new HashMap<>();
        Map<String, LocalDateTime> futureFillCursors = new HashMap<>();
        Set<String> fillHistoryBacklogSymbols = new HashSet<>();
        Map<String, List<ExchangeGateway.ProtectionOrderSnapshot>> protections = new HashMap<>();
        for (String symbol : symbols) {
            LocalDateTime storedCursor = fillSyncCursors.selectLastCompleteTo(credential.getId(), symbol);
            Instant from = initialRetainedHistoryStart(now, storedCursor);
            Instant to = range.to();
            if (from.isAfter(to)) {
                // 旧版把本地时间按 UTC 写入游标。先完整重采窗口，事务内再以原值 CAS 修复，不能直接跳到现在。
                futureFillCursors.put(symbol, storedCursor);
                from = range.from().minus(Duration.between(to, from));
            }
            if (Duration.between(from, to).compareTo(Duration.ofDays(180)) > 0) {
                to = from.plus(Duration.ofDays(180));
            }
            if (to.isBefore(range.to())) {
                fillHistoryBacklogSymbols.add(symbol);
            }
            if (from.isBefore(to)) {
                trades.put(symbol, tradeCollector.collect(c, symbol, from, to));
                fillCursorEnds.put(symbol, to);
            } else {
                trades.put(symbol, List.of());
            }
            protections.put(symbol, List.copyOf(gateway.queryProtectionOrders(c, symbol)));
        }
        LocalDateTime storedIncomeCursor = incomeSyncCursors.selectLastCompleteTo(credential.getId());
        Instant incomeFrom = initialRetainedHistoryStart(now, storedIncomeCursor);
        Instant incomeTo = range.to();
        if (incomeFrom.isAfter(incomeTo)) {
            throw new IllegalStateException("Binance income-history cursor is in the future; refusing to skip financial events: credentialId="
                + credential.getId() + ", cursor=" + storedIncomeCursor);
        }
        boolean incomeHistoryBacklog = Duration.between(incomeFrom, incomeTo).compareTo(Duration.ofDays(180)) > 0;
        if (incomeHistoryBacklog) {
            incomeTo = incomeFrom.plus(Duration.ofDays(180));
        }
        List<ExchangeGateway.IncomeSnapshot> income = incomeFrom.isBefore(incomeTo)
            ? incomeCollector.collect(c, new ExchangeGateway.TimeRange(incomeFrom, incomeTo))
            : List.of();
        LocalDateTime incomeCursorEnd = LocalDateTime.ofInstant(incomeTo, ZoneOffset.UTC);
        return new ExchangeFacts(account, positions, openOrders, history, trades, fillCursorEnds, futureFillCursors, fillHistoryBacklogSymbols,
            protections, income, incomeCursorEnd, incomeHistoryBacklog);
    }

    /**
     * Only database writes are permitted inside this short transaction.
     */
    private void applyExchangeFacts(QuantCredential credential, ExchangeFacts facts) {
        syncOrders(credential, facts.history());
        syncOrders(credential, facts.openOrders());
        if ("LIVE".equalsIgnoreCase(credential.getRunMode())) {
            fillJournal.retryPending(credential.getId());
        }
        for (var entry : facts.trades().entrySet()) {
            syncTrades(credential, entry.getValue());
        }
        for (var entry : facts.fillCursorEnds().entrySet()) {
            LocalDateTime cursorEnd = LocalDateTime.ofInstant(entry.getValue(), ZoneOffset.UTC);
            LocalDateTime futureCursor = facts.futureFillCursors().get(entry.getKey());
            if (futureCursor == null) {
                fillSyncCursorService.advance(credential.getId(), entry.getKey(), cursorEnd);
            } else {
                fillSyncCursorService.repairFutureAfterCollection(credential.getId(), entry.getKey(), futureCursor, cursorEnd);
            }
        }
        long unattributedFills = "LIVE".equalsIgnoreCase(credential.getRunMode()) ? fillJournal.countUnresolved(credential.getId()) : 0L;
        int unmatchedProtections = 0;
        for (var entry : facts.protections().entrySet()) {
            unmatchedProtections += syncProtectionOrders(credential, entry.getKey(), facts.positions(), entry.getValue());
        }
        boolean financialLedgerConflict = false;
        try {
            syncFinancialEvents(credential, facts.income());
            incomeSyncCursorService.advance(credential.getId(), facts.incomeCursorEnd());
        } catch (IllegalStateException ex) {
            // An immutable financial-ledger conflict must not roll back verified order/fill facts
            // written earlier in this transaction. Do not advance the income cursor: the
            // conflicting event must be retried/reviewed, while already inserted income events
            // remain idempotently replayable on the next reconciliation.
            if (ex.getMessage() == null || !ex.getMessage().startsWith("Binance financial event conflicts with persisted ledger:")) {
                throw ex;
            }
            financialLedgerConflict = true;
            log.error("Binance 财务事件账本冲突；已保留本轮成交同步结果且不推进财务游标 credentialId={} cursorEnd={}",
                credential.getId(), facts.incomeCursorEnd(), ex);
        }
        int fillHistoryBacklogSymbols = facts.fillHistoryBacklogSymbols().size();
        if (unmatchedProtections > 0 || unattributedFills > 0 || fillHistoryBacklogSymbols > 0 || facts.incomeHistoryBacklog() || financialLedgerConflict) {
            // Unmatched protections, unattributed fills, financial conflicts, or a catch-up cursor
            // behind the current interval may not be reported as complete.
            reconcileMapper.insert(record(credential.getId(), "DEGRADED",
                "{\"unmatchedLiveProtections\":" + unmatchedProtections + ",\"unattributedFills\":" + unattributedFills
                    + ",\"fillHistoryBacklogSymbols\":" + fillHistoryBacklogSymbols + ",\"incomeHistoryBacklog\":" + facts.incomeHistoryBacklog()
                    + ",\"financialLedgerConflict\":" + financialLedgerConflict + "}"));
        } else {
            reconcileMapper.insert(record(credential.getId(), "SUCCESS", "{}"));
        }
    }

    /**
     * 同步账户资产快照。
     */
    private void syncAccount(QuantCredential credential, ExchangeGateway.AccountSnapshot snapshot) {
        LocalDateTime observedAt = LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai")).withNano(0);
        QuantAccountSnapshot entity = accountSnapshotMapper.selectOne(new LambdaQueryWrapper<QuantAccountSnapshot>().eq(QuantAccountSnapshot::getCredentialId,
            credential.getId()).eq(QuantAccountSnapshot::getObservedAt, observedAt));
        if (entity == null) {
            entity = new QuantAccountSnapshot();
            entity.setCredentialId(credential.getId());
            entity.setObservedAt(observedAt);
        }
        entity.setWalletBalance(nvl(snapshot.walletBalance()));
        entity.setAvailableBalance(nvl(snapshot.availableBalance()));
        entity.setMarginBalance(nvl(snapshot.marginBalance()));
        entity.setUnrealizedPnl(nvl(snapshot.unrealizedPnl()));
        entity.setRealizedPnl(BigDecimal.ZERO);
        entity.setFundingFee(BigDecimal.ZERO);
        if (entity.getId() == null) {
            accountSnapshotMapper.insert(entity);
        } else {
            accountSnapshotMapper.updateById(entity);
        }
    }

    /**
     * 同步交易所持仓事实。
     */
    private void syncPositions(QuantCredential credential, List<ExchangeGateway.PositionSnapshot> snapshots) {
        syncPositions(credential, snapshots, null);
    }

    /**
     * 按已验证的交易所快照同步持仓，不将单个交易对查询误作全账户事实。
     */
    private void syncPositions(QuantCredential credential, List<ExchangeGateway.PositionSnapshot> snapshots, String scopedSymbol) {
        Set<String> activeKeys = new HashSet<>();
        for (ExchangeGateway.PositionSnapshot s : snapshots) {
            if (s.quantity() == null || s.quantity().signum() == 0) {
                continue;
            }
            String direction = "BOTH".equalsIgnoreCase(s.side()) ? (s.quantity().signum() >= 0 ? "LONG" : "SHORT") : s.side().toUpperCase();
            String key = s.symbol().toUpperCase() + ":" + direction;
            activeKeys.add(key);
            QuantPosition entity =
                positionMapper.selectOne(new LambdaQueryWrapper<QuantPosition>().eq(QuantPosition::getCredentialId, credential.getId()).eq(QuantPosition::getSymbol, s.symbol()).eq(QuantPosition::getPositionSide, direction));
            if (entity == null) {
                entity = new QuantPosition();
                entity.setCredentialId(credential.getId());
                entity.setSymbol(s.symbol());
                entity.setPositionSide(direction);
            }
            entity.setQuantity(s.quantity().abs());
            entity.setEntryPrice(nvl(s.entryPrice()));
            entity.setMarkPrice(nvl(s.markPrice()));
            entity.setUnrealizedPnl(nvl(s.unrealizedPnl()));
            entity.setLiquidationPrice(s.liquidationPrice());
            entity.setMargin(s.margin());
            entity.setLeverage(s.leverage());
            entity.setUpdatedAt(LocalDateTime.now());
            if (entity.getId() == null) {
                positionMapper.insert(entity);
            } else {
                positionMapper.updateById(entity);
            }
        }

        var localQuery = new LambdaQueryWrapper<QuantPosition>().eq(QuantPosition::getCredentialId, credential.getId()).and(query -> query.gt(QuantPosition::getQuantity,
            BigDecimal.ZERO).or().inSql(QuantPosition::getId, "SELECT position_id FROM quant_strategy_position_state WHERE status <> 'CLOSED' " + "AND (position_qty > 0 OR " +
            "pending_action IS NOT NULL OR status='PROTECTION_CLEANUP')"));
        if (scopedSymbol != null) {
            localQuery.eq(QuantPosition::getSymbol, scopedSymbol);
        }
        List<QuantPosition> locals = positionMapper.selectList(localQuery);
        for (QuantPosition local : locals) {
            String key = local.getSymbol().toUpperCase() + ":" + local.getPositionSide().toUpperCase();
            if (!activeKeys.contains(key)) {
                BigDecimal previousQuantity = local.getQuantity();
                if (previousQuantity != null && previousQuantity.signum() > 0) {
                    local.setQuantity(BigDecimal.ZERO);
                    local.setUnrealizedPnl(BigDecimal.ZERO);
                    local.setUpdatedAt(LocalDateTime.now());
                    positionMapper.updateById(local);
                }
                strategyPositionStateService.markClosedByPositionId(local.getId());
                if (previousQuantity != null && previousQuantity.signum() > 0) {
                    log.info("Binance持仓快照确认本地仓位归零 credentialId={} positionId={} symbol={} side={} previousQuantity={}", credential.getId(), local.getId(), local.getSymbol(),
                        local.getPositionSide(), previousQuantity);
                }
            }
        }
    }

    /**
     * 同步交易所订单事实。
     */
    private void syncOrders(QuantCredential credential, List<ExchangeGateway.OrderSnapshot> snapshots) {
        for (ExchangeGateway.OrderSnapshot s : snapshots) {
            String clientOrderId = s.clientOrderId() == null ? "EXCHANGE:" + s.orderId() : s.clientOrderId();
            QuantOrder entity = s.orderId() == null ? null :
                orderMapper.selectOne(new LambdaQueryWrapper<QuantOrder>().eq(QuantOrder::getCredentialId, credential.getId()).eq(QuantOrder::getSymbol, s.symbol())
                    .eq(QuantOrder::getExchangeOrderId, s.orderId()));
            if (entity == null) {
                entity = orderMapper.selectOne(new LambdaQueryWrapper<QuantOrder>().eq(QuantOrder::getCredentialId, credential.getId()).eq(QuantOrder::getClientOrderId,
                    clientOrderId).eq(QuantOrder::getSymbol, s.symbol()));
            }
            if (entity == null) {
                entity = new QuantOrder();
                entity.setCredentialId(credential.getId());
                entity.setClientOrderId(clientOrderId);
                entity.setCreatedAt(LocalDateTime.now());
                entity.setIdempotencyKey(clientOrderId);
            }
            entity.setSymbol(s.symbol());
            entity.setExchangeOrderId(s.orderId());
            entity.setSide(s.side());
            entity.setOrderType(s.type());
            entity.setStatus(s.status());
            entity.setPrice(s.price());
            entity.setOriginalQuantity(nvl(s.originalQuantity()));
            entity.setExecutedQuantity(nvl(s.executedQuantity()));
            entity.setUpdatedAt(s.updateTime() == null ? LocalDateTime.now() : LocalDateTime.ofInstant(s.updateTime(), ZoneOffset.UTC));
            if (entity.getId() == null) {
                orderMapper.insert(entity);
            } else {
                // 订单历史与活动订单列表可能返回较旧快照。更新必须走 SQL 原子单调合并，
                // 不能用 updateById 把已成交数量或终态回滚；返回 0 表示事实冲突/过期，保留现有账本。
                orderMapper.mergeExchangeFactIfMonotonic(entity);
            }
            // 历史/当前订单快照同样可补齐受理后的成交事实；价格、数量、时间均沿用交易所返回值。
            if (decisionLogs != null) {
                decisionLogs.order(credential.getId(), s);
            }
        }
    }

    /**
     * 同步交易所成交事实。已落库的交易所事实不可被重放覆盖；仅允许补齐此前缺失且可证明的策略归属。
     */
    private void syncTrades(QuantCredential credential, List<ExchangeGateway.TradeSnapshot> snapshots) {
        Map<Long, QuantOrder> filledOrders = new LinkedHashMap<>();
        boolean live = "LIVE".equalsIgnoreCase(credential.getRunMode());
        for (ExchangeGateway.TradeSnapshot s : snapshots) {
            // Pagination identity is mandatory, but incomplete money facts must still reach the
            // LIVE raw-fill journal so the account's history watermark does not erase the evidence.
            if (s == null || s.symbol() == null || s.symbol().isBlank() || s.tradeId() == null || s.tradeId() <= 0 || s.time() == null) {
                throw new IllegalStateException("Binance fill lacks stable symbol/trade/time identity; refusing to advance history");
            }
            QuantOrder sourceOrder = s.orderId() == null ? null : orderMapper.selectOne(new LambdaQueryWrapper<QuantOrder>()
                .eq(QuantOrder::getCredentialId, credential.getId())
                .eq(QuantOrder::getSymbol, s.symbol())
                .eq(QuantOrder::getExchangeOrderId, s.orderId()));

            boolean completeFacts = s.orderId() != null && s.orderId() > 0
                && java.util.Set.of("BUY", "SELL").contains(s.side() == null ? "" : s.side().toUpperCase(Locale.ROOT))
                && s.quantity() != null && s.quantity().signum() > 0
                && s.price() != null && s.price().signum() > 0
                && s.commission() != null && s.commission().signum() >= 0
                && s.realizedPnl() != null
                && s.commissionAsset() != null && s.commissionAsset().matches("[A-Za-z0-9]{2,20}");
            if (!completeFacts) {
                if (live) {
                    String status = fillJournal.attribute(s, sourceOrder, credential.getId());
                    if ("CONFLICT".equals(status)) {
                        log.error("Incomplete LIVE fill conflicts with persisted raw fact credentialId={} symbol={} exchangeTradeId={}",
                            credential.getId(), s.symbol(), s.tradeId());
                    } else {
                        log.warn("LIVE Binance fill retained for attribution review; local numeric trade row was not fabricated credentialId={} symbol={} exchangeTradeId={} orderId={}",
                            credential.getId(), s.symbol(), s.tradeId(), s.orderId());
                    }
                    continue;
                }
                throw new IllegalStateException("Binance fill lacks valid immutable facts; refusing to persist partial trade");
            }

            // First persist/check the raw exchange fact WITHOUT allocating it. Only after the local
            // trade identity is verified may strategy attribution mutate the allocation ledger.
            if (live) {
                String rawStatus = fillJournal.attribute(s, null, credential.getId());
                if ("CONFLICT".equals(rawStatus)) {
                    log.error("LIVE fill journal quarantined conflicting raw fact; local trade ledger was not modified credentialId={} symbol={} exchangeTradeId={}",
                        credential.getId(), s.symbol(), s.tradeId());
                    continue;
                }
            }
            if (decisionLogs != null) {
                decisionLogs.trade(credential.getId(), s);
            }

            QuantTrade entity = tradeMapper.selectOne(new LambdaQueryWrapper<QuantTrade>()
                .eq(QuantTrade::getCredentialId, credential.getId())
                .eq(QuantTrade::getSymbol, s.symbol())
                .eq(QuantTrade::getExchangeTradeId, s.tradeId()));

            if (entity == null) {
                entity = new QuantTrade();
                entity.setCredentialId(credential.getId());
                entity.setSymbol(s.symbol());
                entity.setExchangeTradeId(s.tradeId());
                entity.setExchangeOrderId(s.orderId());
                entity.setSide(s.side());
                entity.setQuantity(s.quantity());
                entity.setPrice(s.price());
                entity.setCommission(s.commission());
                entity.setRealizedPnl(s.realizedPnl());
                // Funding income is stored separately in quant_financial_event. Zero is only
                // an initial display default; a replay must never erase a previously stored value.
                entity.setFundingFee(BigDecimal.ZERO);
                entity.setTradeTime(LocalDateTime.ofInstant(s.time(), ZoneOffset.UTC));
                mergeStrategyAttribution(entity, sourceOrder);
                try {
                    if (tradeMapper.insert(entity) != 1) {
                        throw new IllegalStateException("Binance成交事实未写入本地成交账本");
                    }
                } catch (DuplicateKeyException concurrentInsert) {
                    QuantTrade raced = tradeMapper.selectOne(new LambdaQueryWrapper<QuantTrade>()
                        .eq(QuantTrade::getCredentialId, credential.getId())
                        .eq(QuantTrade::getSymbol, s.symbol())
                        .eq(QuantTrade::getExchangeTradeId, s.tradeId()));
                    if (raced == null) {
                        throw concurrentInsert;
                    }
                    final boolean attributionChanged;
                    try {
                        assertImmutableTradeFacts(raced, s);
                        attributionChanged = mergeStrategyAttribution(raced, sourceOrder);
                    } catch (IllegalStateException conflict) {
                        if (!live) {
                            throw conflict;
                        }
                        fillJournal.quarantineConflict(credential.getId(), s.symbol(), s.tradeId(), conflict.getMessage());
                        log.error("Concurrent local trade conflicts with Binance fill; quarantined raw fact credentialId={} symbol={} exchangeTradeId={}",
                            credential.getId(), s.symbol(), s.tradeId());
                        continue;
                    }
                    if (attributionChanged && tradeMapper.updateById(raced) != 1) {
                        throw new IllegalStateException("Concurrent Binance fill attribution could not be enriched safely", concurrentInsert);
                    }
                    log.debug("并发对账已写入相同成交事实，跳过重复插入 credentialId={} symbol={} exchangeTradeId={}",
                        credential.getId(), s.symbol(), s.tradeId());
                }
            } else {
                final boolean attributionChanged;
                try {
                    assertImmutableTradeFacts(entity, s);
                    attributionChanged = mergeStrategyAttribution(entity, sourceOrder);
                } catch (IllegalStateException conflict) {
                    if (!live) {
                        throw conflict;
                    }
                    fillJournal.quarantineConflict(credential.getId(), s.symbol(), s.tradeId(), conflict.getMessage());
                    log.error("Persisted local trade conflicts with Binance fill; quarantined raw fact credentialId={} symbol={} exchangeTradeId={}",
                        credential.getId(), s.symbol(), s.tradeId());
                    continue;
                }
                if (attributionChanged && tradeMapper.updateById(entity) != 1) {
                    throw new IllegalStateException("Binance fill attribution could not be enriched safely: credentialId="
                        + credential.getId() + ", symbol=" + s.symbol() + ", exchangeTradeId=" + s.tradeId());
                }
            }

            if (live && sourceOrder != null && sourceOrder.getStrategyId() != null && sourceOrder.getStrategyInstanceId() != null) {
                String status = fillJournal.attribute(s, sourceOrder, credential.getId());
                if ("CONFLICT".equals(status)) {
                    log.error("LIVE fill attribution became conflicting after local fact verification credentialId={} symbol={} exchangeTradeId={}",
                        credential.getId(), s.symbol(), s.tradeId());
                }
            }
            if (sourceOrder != null && sourceOrder.getId() != null) {
                filledOrders.put(sourceOrder.getId(), sourceOrder);
            }
        }
        filledOrders.values().forEach(order -> capturePostFillBalance(credential, order));
    }

    private static void assertImmutableTradeFacts(QuantTrade stored, ExchangeGateway.TradeSnapshot incoming) {
        if (!Objects.equals(stored.getExchangeOrderId(), incoming.orderId())
            || !sameText(stored.getSymbol(), incoming.symbol())
            || !sameText(stored.getSide(), incoming.side())
            || !sameDecimal(stored.getQuantity(), incoming.quantity())
            || !sameDecimal(stored.getPrice(), incoming.price())
            || !sameDecimal(stored.getCommission(), incoming.commission())
            || !sameDecimal(stored.getRealizedPnl(), incoming.realizedPnl())
            || !sameTradeTime(stored.getTradeTime(), incoming.time())) {
            throw new IllegalStateException("Persisted local trade conflicts with immutable Binance fill facts; refusing overwrite: symbol="
                + incoming.symbol() + ", exchangeTradeId=" + incoming.tradeId() + ", exchangeOrderId=" + incoming.orderId());
        }
    }

    /**
     * Enrich absent strategy ownership only from the exact local order. Existing non-null ownership is immutable.
     */
    private static boolean mergeStrategyAttribution(QuantTrade trade, QuantOrder sourceOrder) {
        if (sourceOrder == null || sourceOrder.getStrategyId() == null || sourceOrder.getStrategyInstanceId() == null) {
            return false;
        }
        if (trade.getStrategyId() != null && !trade.getStrategyId().equals(sourceOrder.getStrategyId())
            || trade.getStrategyInstanceId() != null && !trade.getStrategyInstanceId().equals(sourceOrder.getStrategyInstanceId())) {
            throw new IllegalStateException("Binance fill strategy attribution conflicts with persisted ownership: exchangeOrderId="
                + sourceOrder.getExchangeOrderId());
        }
        boolean changed = false;
        if (trade.getStrategyId() == null) {
            trade.setStrategyId(sourceOrder.getStrategyId());
            changed = true;
        }
        if (trade.getStrategyInstanceId() == null) {
            trade.setStrategyInstanceId(sourceOrder.getStrategyInstanceId());
            changed = true;
        }
        return changed;
    }

    private static boolean sameText(String left, String right) {
        return left == null ? right == null : right != null && left.equalsIgnoreCase(right);
    }

    private static boolean sameDecimal(BigDecimal left, BigDecimal right) {
        return left == null ? right == null : right != null && left.compareTo(right) == 0;
    }

    private static boolean sameTradeTime(LocalDateTime stored, Instant incoming) {
        if (stored == null || incoming == null) {
            return stored == null && incoming == null;
        }
        // quant_trade.trade_time is DATETIME without fractional seconds in the supported schema.
        return stored.truncatedTo(java.time.temporal.ChronoUnit.SECONDS).equals(
            LocalDateTime.ofInstant(incoming, ZoneOffset.UTC).truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
    }

    /** Capture a best-effort account wallet balance after reconciled exchange fills. */
    private void capturePostFillBalance(QuantCredential credential, QuantOrder order) {
        if (order.getId() == null || !balanceSnapshotTasks.add(order.getId())) {
            return;
        }
        LocalDateTime requestedAt = LocalDateTime.now(ZoneOffset.UTC);
        if (orderMapper.claimPostFillBalanceSnapshot(order.getId(), requestedAt) != 1) {
            balanceSnapshotTasks.remove(order.getId());
            return;
        }
        Thread.startVirtualThread(() -> {
            try {
                ExchangeGateway.Credential exchangeCredential = new ExchangeGateway.Credential(credential.getApiKey(), credential.getSecretKey(),
                    credential.getSignatureType(), credential.getRunMode());
                ExchangeGateway.AccountSnapshot snapshot = gateway.queryAccountFresh(exchangeCredential);
                if (snapshot == null || snapshot.walletBalance() == null) {
                    return;
                }
                orderMapper.savePostFillBalanceSnapshot(order.getId(), snapshot.walletBalance(), LocalDateTime.now(ZoneOffset.UTC));
            } catch (RuntimeException ex) {
                // Balance is informational; an unavailable snapshot must never affect order or position handling.
                log.debug("成交后账户余额核实失败 credentialId={} orderId={} errorType={}", credential.getId(), order.getId(), ex.getClass().getSimpleName());
            } finally {
                balanceSnapshotTasks.remove(order.getId());
            }
        });
    }

    /** Share the collector's canonical identity so collection and persistence deduplicate identically. */
    static String financialEventIdentityKey(ExchangeGateway.IncomeSnapshot event) {
        return org.dromara.quant.execution.StrictBinanceIncomeCollector.identityKey(event);
    }

    /**
     * 同步交易所财务流水到独立财务事件账本。
     */
    private void syncFinancialEvents(QuantCredential credential, List<ExchangeGateway.IncomeSnapshot> snapshots) {
        for (ExchangeGateway.IncomeSnapshot s : snapshots) {
            if (s.transactionId() == null) {
                continue;
            }
            // Missing financial facts are unknown, not zero/current-time defaults.
            if (s.incomeType() == null || s.incomeType().isBlank() || s.amount() == null || s.asset() == null || s.asset().isBlank() || s.time() == null) {
                throw new IllegalStateException("Binance financial event is missing required facts: credentialId="
                    + credential.getId() + ", transactionId=" + s.transactionId());
            }
            LocalDateTime eventTime = LocalDateTime.ofInstant(s.time(), ZoneOffset.UTC);
            // Binance tranId is only unique within the same incomeType. Different
            // financial event types may legally share a transaction ID, so the
            // idempotency key must include eventType as well as credential and tranId.
            String identityKey = financialEventIdentityKey(s);
            QuantFinancialEventVo vo = financialEventMapper.selectVoOne(new LambdaQueryWrapper<QuantFinancialEvent>()
                .eq(QuantFinancialEvent::getCredentialId, credential.getId())
                .eq(QuantFinancialEvent::getEventIdentityKey, identityKey));
            QuantFinancialEvent entity = MapstructUtils.convert(vo, QuantFinancialEvent.class);
            if (entity != null) {
                // Treat the exchange ledger as immutable. A replay may confirm the
                // original event, but must not silently rewrite a conflicting fact.
                boolean sameTime = entity.getEventTime() != null
                    && entity.getEventTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
                        .equals(eventTime.truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
                List<String> conflictingFields = new ArrayList<>();
                if (!Objects.equals(entity.getSymbol(), s.symbol())) conflictingFields.add("symbol");
                if (!Objects.equals(entity.getEventType(), s.incomeType())) conflictingFields.add("eventType");
                if (entity.getAmount() == null || entity.getAmount().compareTo(s.amount()) != 0) conflictingFields.add("amount");
                if (!Objects.equals(entity.getAsset(), s.asset())) conflictingFields.add("asset");
                if (!Objects.equals(entity.getInfo(), s.info())) conflictingFields.add("info");
                if (!sameTime) conflictingFields.add("eventTime");
                if (!conflictingFields.isEmpty()) {
                    /*
                     * Narrow compatibility repair for rows written by the legacy Beijing-local-time
                     * representation: the immutable exchange identity and every non-time fact must
                     * match, and the stored time must equal the incoming UTC time plus exactly 8h.
                     * Never normalize arbitrary timestamp conflicts or overwrite other financial facts.
                     */
                    boolean legacyBeijingTime = conflictingFields.equals(List.of("eventTime"))
                        && entity.getEventTime() != null
                        && entity.getEventTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
                            .equals(eventTime.plusHours(8).truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
                    if (legacyBeijingTime && entity.getId() != null) {
                        entity.setEventTime(eventTime);
                        if (financialEventMapper.updateById(entity) != 1) {
                            throw new IllegalStateException("Legacy Beijing-time financial event could not be normalized safely: credentialId="
                                + credential.getId() + ", transactionId=" + s.transactionId());
                        }
                        log.warn("Normalized legacy Beijing-local eventTime to UTC for matching Binance financial event credentialId={} transactionId={}",
                            credential.getId(), s.transactionId());
                        continue;
                    }
                    // Any other mismatch remains a hard reconciliation failure; do not advance the cursor.
                    throw new IllegalStateException("Binance financial event conflicts with persisted ledger: credentialId="
                        + credential.getId() + ", transactionId=" + s.transactionId() + ", fields=" + String.join(",", conflictingFields));
                }
                continue;
            }
            entity = new QuantFinancialEvent();
            entity.setCredentialId(credential.getId());
            entity.setExchangeTransactionId(s.transactionId());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setSymbol(s.symbol());
            entity.setEventType(s.incomeType());
            entity.setEventIdentityKey(identityKey);
            entity.setAmount(s.amount());
            entity.setAsset(s.asset());
            entity.setInfo(s.info());
            entity.setEventTime(eventTime);
            if (financialEventMapper.insert(entity) != 1) {
                throw new IllegalStateException("Binance financial event was not persisted: credentialId="
                    + credential.getId() + ", transactionId=" + s.transactionId());
            }
        }
    }

    /**
     * 同步交易所保护单状态。
     */
    private int syncProtectionOrders(QuantCredential credential, String symbol, List<ExchangeGateway.PositionSnapshot> positions,
                                     List<ExchangeGateway.ProtectionOrderSnapshot> snapshots) {
        int unmatchedLiveProtections = 0;
        for (ExchangeGateway.ProtectionOrderSnapshot s : snapshots) {
            QuantPosition position = findProtectionPosition(credential.getId(), symbol, positions, s.positionSide(), s.side());
            QuantProtectionOrder entity = protectionOrderMapper.selectOne(new LambdaQueryWrapper<QuantProtectionOrder>().eq(QuantProtectionOrder::getCredentialId,
                credential.getId()).eq(QuantProtectionOrder::getExchangeOrderId, s.algoId()));
            if (entity == null && s.clientAlgoId() != null) {
                entity =
                    protectionOrderMapper.selectOne(new LambdaQueryWrapper<QuantProtectionOrder>().eq(QuantProtectionOrder::getCredentialId, credential.getId()).eq(QuantProtectionOrder::getClientOrderId, s.clientAlgoId()));
            }
            if (entity == null) {
                entity = new QuantProtectionOrder();
                entity.setCredentialId(credential.getId());
                entity.setClientOrderId(s.clientAlgoId());
                entity.setProtectionType("EXCHANGE_CONDITIONAL");
            }
            // 活动列表是较早采集的账户视图，不能覆盖本系统保护意图、持仓周期或精确确认的终态。
            if (entity.getId() == null || "EXCHANGE_CONDITIONAL".equals(entity.getProtectionType())) {
                entity.setPositionId(position == null ? null : position.getId());
                entity.setSymbol(symbol);
                entity.setPositionSide(position == null ? normalizeProtectionSide(s.positionSide()) : position.getPositionSide());
                entity.setExchangeOrderId(s.algoId());
                entity.setOrderType(s.type());
                entity.setQuantity(s.quantity());
                entity.setStatus(s.status());
                entity.setStopPrice(s.triggerPrice());
                if (entity.getId() == null) {
                    protectionOrderMapper.insert(entity);
                } else {
                    protectionOrderMapper.updateById(entity);
                }
            }
            // Missing local position mapping is not proof of an absent exchange position.
            // Reconciliation must not cancel a possibly live protective order.
            if (position == null && isOpenProtection(s.status())) {
                unmatchedLiveProtections++;
            }
        }

        // 列表未见不是不存在/撤单/成交证明。终态只由 ProtectionOrderService 精确查询原标识后落库。
        return unmatchedLiveProtections;
    }

    /**
     * 根据保护单定位对应本地持仓。
     */
    private QuantPosition findProtectionPosition(Long credentialId, String symbol, List<ExchangeGateway.PositionSnapshot> snapshots, String exchangePositionSide,
                                                 String orderSide) {
        String wanted = normalizeProtectionSide(exchangePositionSide);
        if (wanted == null && orderSide != null) {
            if ("SELL".equalsIgnoreCase(orderSide)) {
                wanted = "LONG";
            }
            if ("BUY".equalsIgnoreCase(orderSide)) {
                wanted = "SHORT";
            }
        }
        final String matchedDirection = wanted;
        return positionMapper.selectList(new LambdaQueryWrapper<QuantPosition>().eq(QuantPosition::getCredentialId, credentialId).eq(QuantPosition::getSymbol, symbol).gt(QuantPosition::getQuantity, BigDecimal.ZERO)).stream().filter(p -> matchedDirection == null || matchedDirection.equalsIgnoreCase(p.getPositionSide())).findFirst().orElseGet(() -> {
            if (matchedDirection != null) {
                return null;
            }
            for (ExchangeGateway.PositionSnapshot snapshot : snapshots) {
                if (symbol.equalsIgnoreCase(snapshot.symbol()) && snapshot.quantity() != null && snapshot.quantity().signum() != 0) {
                    String direction = "BOTH".equalsIgnoreCase(snapshot.side()) ? (snapshot.quantity().signum() >= 0 ? "LONG" : "SHORT") : normalizeProtectionSide(snapshot.side());
                    return positionMapper.selectOne(new LambdaQueryWrapper<QuantPosition>().eq(QuantPosition::getCredentialId, credentialId).eq(QuantPosition::getSymbol, symbol).eq(QuantPosition::getPositionSide, direction));
                }
            }
            return null;
        });
    }

    private QuantReconcileRecord record(Long credentialId, String result, String detail) {
        QuantReconcileRecord record = new QuantReconcileRecord();
        record.setCredentialId(credentialId);
        record.setReconcileType("FULL");
        record.setResult(result);
        record.setDetailJson(detail);
        record.setReconciledAt(LocalDateTime.now());
        return record;
    }

    /**
     * A fresh account event invalidated an in-flight REST snapshot; safe to fetch again.
     */
    public static final class SnapshotSupersededException extends IllegalStateException {
        public SnapshotSupersededException() {
            this("Binance position snapshot superseded by newer account event");
        }

        public SnapshotSupersededException(String message) {
            super(message);
        }
    }

    private record ExchangeFacts(ExchangeGateway.AccountSnapshot account, List<ExchangeGateway.PositionSnapshot> positions, List<ExchangeGateway.OrderSnapshot> openOrders,
                                 List<ExchangeGateway.OrderSnapshot> history, Map<String, List<ExchangeGateway.TradeSnapshot>> trades,
                                 Map<String, Instant> fillCursorEnds, Map<String, LocalDateTime> futureFillCursors, Set<String> fillHistoryBacklogSymbols,
                                 Map<String, List<ExchangeGateway.ProtectionOrderSnapshot>> protections,
                                 List<ExchangeGateway.IncomeSnapshot> income, LocalDateTime incomeCursorEnd, boolean incomeHistoryBacklog) {
    }

    private record CachedPositionSnapshot(long positionGeneration, long runtimeGeneration, Instant fetchedAt, List<ExchangeGateway.PositionSnapshot> positions) {
    }
}
