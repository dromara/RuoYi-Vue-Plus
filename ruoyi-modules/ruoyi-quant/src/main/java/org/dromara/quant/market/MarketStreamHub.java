package org.dromara.quant.market;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.dromara.quant.config.QuantProperties;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.exchange.ExchangeMarketPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 同一交易对和周期共享上游流，独立隔离每个下游订阅。
 */
@Component
@Slf4j
public class MarketStreamHub {
    private static final long MAX_FUTURE_CLOCK_SKEW_MILLIS = 10_000L;
    private final ExchangeGateway gateway;
    private final ScheduledExecutorService scheduler;
    private final QuantProperties properties;
    private final java.util.concurrent.Semaphore recoveryReads = new java.util.concurrent.Semaphore(1, true);
    private final Map<Key, SharedStream> streams = new ConcurrentHashMap<>();
    private final java.util.concurrent.ExecutorService recoveryExecutor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();

    static long reconnectDelayMillis(int reconnectDelaySeconds, long jitterMillis) {
        long baseDelayMillis = TimeUnit.SECONDS.toMillis(Math.max(1, reconnectDelaySeconds));
        return baseDelayMillis + Math.min(baseDelayMillis, Math.max(0L, jitterMillis));
    }

    public MarketStreamHub(ExchangeGateway gateway, @Qualifier("quantScheduledExecutorService") ScheduledExecutorService scheduler, QuantProperties properties) {
        this.gateway = gateway;
        this.scheduler = scheduler;
        this.properties = properties;
    }

    /**
     * 订阅共享行情流，并返回用于释放本地订阅的句柄。
     */
    public AutoCloseable subscribe(String symbol, String interval, Consumer<ExchangeGateway.MarketEvent> listener) {
        return subscribe(symbol, interval, listener, ignored -> {
        });
    }

    /**
     * 健康回调只更新决策开关，不得阻塞网络线程或执行交易。
     */
    public AutoCloseable subscribe(String symbol, String interval, Consumer<ExchangeGateway.MarketEvent> listener, Consumer<Boolean> health) {
        return subscribe("LIVE", symbol, interval, listener, health);
    }

    /**
     * 按环境隔离上游、参考K线和恢复游标；PAPER沿用正式网公开行情。
     */
    public AutoCloseable subscribe(String runMode, String symbol, String interval, Consumer<ExchangeGateway.MarketEvent> listener, Consumer<Boolean> health) {
        Key key = new Key("TESTNET".equalsIgnoreCase(runMode) ? "TESTNET" : "LIVE", symbol.trim().toUpperCase(java.util.Locale.ROOT), interval.trim());
        UUID id = UUID.randomUUID();
        streams.compute(key, (ignored, current) -> {
            SharedStream stream = current == null ? new SharedStream(key) : current;
            stream.add(id, listener, health);
            return stream;
        });
        return () -> streams.computeIfPresent(key, (ignored, stream) -> {
            stream.remove(id);
            if (stream.listeners.isEmpty()) {
                stream.close();
                return null;
            }
            return stream;
        });
    }

    /**
     * 关闭上游流和补偿任务，释放模块持有的资源。
     */
    @PreDestroy
    public void close() {
        streams.values().forEach(SharedStream::close);
        streams.clear();
        recoveryExecutor.shutdownNow();
    }

    private record Key(String runMode, String symbol, String interval) {
    }

    private record Incoming(long generation, ExchangeGateway.MarketEvent event) {
    }

    private final class SharedStream {
        private final Key key;
        private final Map<UUID, StreamEventConsumer<Incoming>> listeners = new ConcurrentHashMap<>();
        private final Map<UUID, Long> consumedTradeIds = new ConcurrentHashMap<>();
        private final Map<UUID, Instant> consumedKlineOpens = new ConcurrentHashMap<>();
        private final StreamEventConsumer<Incoming> ingress;
        private final Map<String, Instant> lastReceived = new ConcurrentHashMap<>();
        private final Map<String, Instant> lastEventTime = new ConcurrentHashMap<>();
        private final java.util.NavigableMap<Instant, ExchangeGateway.Kline> referenceKlines = new java.util.TreeMap<>();
        /**
         * Holds live events while REST recovery is running without blocking the ingress consumer.
         * Bounded independently; overflow invalidates this stream and triggers the existing safe replay.
         */
        private final java.util.ArrayDeque<Incoming> recoveryBuffer = new java.util.ArrayDeque<>();
        private final Map<UUID, Consumer<Boolean>> healthListeners = new ConcurrentHashMap<>();
        private final java.util.concurrent.atomic.LongAccumulator maxNetworkLag = new java.util.concurrent.atomic.LongAccumulator(Long::max, 0);
        private final java.util.concurrent.atomic.LongAccumulator maxQueueLag = new java.util.concurrent.atomic.LongAccumulator(Long::max, 0);
        private ExchangeGateway.MarketStreamSession upstream;
        private ScheduledFuture<?> watchdog;
        private ScheduledFuture<?> reconnect;
        private Instant lastKlineOpen;
        private boolean lastKlineClosed;
        private Long lastTradeId;
        private volatile long generation;
        private volatile boolean closed;
        private volatile boolean connected;
        private volatile boolean recovering;
        private boolean terminalFailure;
        private Long requiredTradeId;
        private Instant connectionStarted;
        private Instant recoveryStarted;
        // 同一K线周期重复进入补偿时仅首次告警；实际校验及交易门控不受影响。
        private Instant lastRecoveryWarningPeriod;
        private long nextMetricsAt;
        private long nextClockSkewWarningAt;
        private long nextClockSyncWarningAt;
        /**
         * MARK freshness measured at event consumption, not at the watchdog sample.
         */
        private volatile long lastMarkProcessingLagMs = -1;
        private volatile boolean lastMarkFreshAtProcessing;
        private java.util.concurrent.Future<?> recoveryTask;
        private java.util.concurrent.Future<?> quietCheckTask;
        private long nextQuietCheckAt;

        private SharedStream(Key key) {
            this.key = key;
            ingress = new StreamEventConsumer<>(properties.getMarket().getQueueCapacity(), incoming -> onEvent(incoming.generation(), incoming.event()), ex -> disconnect(ex),
                () -> scheduleDisconnect("上游消费队列溢出，行情存在缺口"));
        }

        private synchronized void add(UUID id, Consumer<ExchangeGateway.MarketEvent> listener, Consumer<Boolean> health) {
            healthListeners.put(id, health);
            health.accept(false);
            StreamEventConsumer<Incoming> consumer = new StreamEventConsumer<>(properties.getMarket().getQueueCapacity(), delivery -> {
                if (closed || delivery.generation() != generation) {
                    return;
                }
                ExchangeGateway.MarketEvent event = delivery.event();
                if ("CONNECTED".equals(event.type())) {
                    synchronized (this) {
                        if (closed || delivery.generation() != generation || !connected) {
                            return;
                        }
                        health.accept(true);
                    }
                }
                try {
                    listener.accept(event);
                } catch (RuntimeException ex) {
                    if (delivery.generation() != generation) {
                        return;
                    }
                    throw ex;
                }
                if (delivery.generation() != generation) {
                    return;
                }
                if ("TRADE".equals(event.type()) && event.sequenceId() != null) {
                    consumedTradeIds.computeIfPresent(id, (ignored, consumed) -> event.sequenceId() == consumed + 1 ? event.sequenceId() : consumed);
                }
                if (event.kline() != null) {
                    consumedKlineOpens.computeIfPresent(id, (ignored, consumed) -> event.kline().openTime().isAfter(consumed) ? event.kline().openTime() : consumed);
                }
            }, ex -> {
                log.error("行情消费者失败，暂停决策并从成功游标恢复: symbol={}, interval={}, listenerId={}", key.symbol(), key.interval(), id, ex);
                scheduleDisconnect("行情消费者处理失败");
            }, () -> scheduleDisconnect("下游消费队列溢出，暂停决策并恢复行情"));
            if (lastTradeId != null) {
                consumedTradeIds.put(id, lastTradeId);
            }
            if (lastKlineOpen != null) {
                consumedKlineOpens.put(id, lastKlineOpen);
            }
            listeners.put(id, consumer);
            if (listeners.size() == 1) {
                start();
                watchdog = scheduler.scheduleAtFixedRate(this::checkHealth, 5, 5, TimeUnit.SECONDS);
            } else {
                // 持有共享流锁，保证参考回放先于该订阅者的实时事件和就绪通知入队。
                for (ExchangeGateway.Kline kline : referenceKlines.values()) {
                    consumer.accept(new Incoming(generation, new ExchangeGateway.MarketEvent("KLINE", key.symbol(), key.interval(), null, kline, Instant.now(), null,
                        Instant.now(), true)));
                }
                consumer.accept(new Incoming(generation, status(terminalFailure ? "ERROR" : connected ? "CONNECTED" : "CONNECTING")));
            }
        }

        private List<ExchangeGateway.Kline> history(Instant from, int limit) {
            if ("TESTNET".equals(key.runMode())) {
                return from == null ? gateway.queryKlines(key.runMode(), key.symbol(), key.interval(), limit) : gateway.queryKlinesFrom(key.runMode(), key.symbol(),
                    key.interval(), from, limit);
            }
            return from == null ? gateway.queryKlines(key.symbol(), key.interval(), limit) : gateway.queryKlinesFrom(key.symbol(), key.interval(), from, limit);
        }

        private Instant exchangeNow() {
            long offsetMillis = gateway.exchangeTimeOffsetMillis(key.runMode());
            return Instant.now().plusMillis(offsetMillis);
        }

        private void remove(UUID id) {
            StreamEventConsumer<Incoming> listener = listeners.remove(id);
            if (listener != null) {
                listener.close();
            }
            healthListeners.remove(id);
            consumedTradeIds.remove(id);
            consumedKlineOpens.remove(id);
        }

        /**
         * 建立上游订阅并初始化新鲜度、事件游标和补偿状态。
         */
        private synchronized void start() {
            if (closed || terminalFailure || listeners.isEmpty()) {
                return;
            }
            long attempt = ++generation;
            connected = false;
            recovering = true;
            lastEventTime.clear();
            lastMarkProcessingLagMs = -1;
            lastMarkFreshAtProcessing = false;
            connectionStarted = Instant.now();
            recoveryStarted = connectionStarted;
            lastReceived.put("KLINE", connectionStarted);
            lastReceived.put("MARK", connectionStarted);
            signalUnhealthy();
            publish(status("CONNECTING"));
            try {
                Consumer<ExchangeGateway.MarketEvent> listener = event -> {
                    if ("DISCONNECTED".equals(event.type()) || "ERROR".equals(event.type())) {
                        if (attempt == generation) {
                            scheduleDisconnect("上游行情流断开");
                        }
                    } else {
                        ingress.accept(new Incoming(attempt, event));
                    }
                };
                upstream = "TESTNET".equals(key.runMode()) ? gateway.subscribeMarket(key.runMode(), key.symbol(), key.interval(), listener) :
                    gateway.subscribeMarket(key.symbol(), key.interval(), listener);
                recoveryTask = recoveryExecutor.submit(() -> restoreHistory(attempt));
            } catch (RuntimeException ex) {
                disconnect(ex);
            }
        }

        /**
         * REST 在独立线程执行；实时事件由 ingress 消费后进入有界恢复缓冲区，避免阻塞上游入口。
         */
        private void restoreHistory(long attempt) {
            try {
                Instant cursor;
                synchronized (this) {
                    if (closed || attempt != generation) {
                        return;
                    }
                    cursor = lastKlineOpen;
                }
                do {
                    Instant from = cursor;
                    List<ExchangeGateway.Kline> history = recoveryRead(attempt, () -> from == null ? history(null, 200) : history(from, 1000));
                    if (history == null || history.isEmpty()) {
                        throw new IllegalStateException("K线历史为空，不能确认行情已就绪");
                    }
                    if (cursor == null && history.size() < 2) {
                        throw new IllegalStateException("K线历史不足两根，无法建立策略参考价格");
                    }
                    for (ExchangeGateway.Kline kline : history) {
                        synchronized (this) {
                            if (closed || attempt != generation) {
                                return;
                            }
                            validateKline(kline);
                            if (lastKlineOpen != null && kline.openTime().isBefore(lastKlineOpen)) {
                                continue;
                            }
                            if (lastKlineOpen != null && kline.openTime().isAfter(nextOpen(lastKlineOpen))) {
                                throw new IllegalStateException("REST K线仍存在时间缺口");
                            }
                            if (lastKlineOpen != null && kline.openTime().equals(lastKlineOpen) && lastKlineClosed && !kline.closed()) {
                                continue;
                            }
                            if (lastKlineOpen != null && kline.openTime().isAfter(lastKlineOpen) && !lastKlineClosed) {
                                throw new IllegalStateException("REST未确认上一根K线最终收盘");
                            }
                            rememberReference(kline);
                            lastKlineOpen = kline.openTime();
                            lastKlineClosed = kline.closed();
                        }
                        publishReplay(attempt, new ExchangeGateway.MarketEvent("KLINE", key.symbol(), key.interval(), null, kline, Instant.now(), null, Instant.now(), true));
                    }
                    if (history.size() < 1000) {
                        break;
                    }
                    Instant next = nextOpen(history.get(history.size() - 1).openTime());
                    if (cursor != null && !next.isAfter(cursor)) {
                        throw new IllegalStateException("K线补偿游标未前进");
                    }
                    cursor = next;
                } while (!closed && attempt == generation);
                synchronized (this) {
                    if (closed || attempt != generation) {
                        return;
                    }
                    // REST可能在周期切换瞬间只返回刚收盘的一根；新周期由已缓冲的WS补齐。
                    if (lastKlineOpen == null || nextOpen(nextOpen(lastKlineOpen)).isBefore(exchangeNow())) {
                        throw new IllegalStateException("REST K线未补齐到当前周期");
                    }
                }
                recoverTrades(attempt);
                synchronized (this) {
                    if (closed || attempt != generation) {
                        return;
                    }
                    if (requiredTradeId != null && (lastTradeId == null || lastTradeId < requiredTradeId)) {
                        throw new IllegalStateException("REST成交补偿尚未覆盖检测到的缺口");
                    }
                    requiredTradeId = null;
                    recovering = false;
                    // REST只确认K线状态，不能刷新WS的MARK心跳。
                    lastReceived.put("KLINE", Instant.now());
                    notifyAll();
                    // Replay buffered live events in arrival order after REST has restored the baseline.
                    // Keep the lock while draining so newer ingress events cannot overtake the buffer.
                    while (!recoveryBuffer.isEmpty() && !closed && attempt == generation && !recovering) {
                        Incoming buffered = recoveryBuffer.removeFirst();
                        onEvent(buffered.generation(), buffered.event());
                    }
                }
                log.info("行情历史恢复完成，等待当前周期K线有效和新鲜MARK: symbol={}, interval={}, lastOpen={}", key.symbol(), key.interval(), lastKlineOpen);
            } catch (RuntimeException ex) {
                synchronized (this) {
                    if (!closed && attempt == generation) {
                        if (unsupportedSymbol(ex)) {
                            failPermanently(ex);
                        } else if (networkFailure(ex)) {
                            // REST不稳定不能连带拆掉健康WS；保持缓冲和决策暂停，继续精确补偿。
                            log.warn("行情REST补偿网络失败，保留WS并退避重试: symbol={}, interval={}, errorType={}", key.symbol(), key.interval(), ex.getClass().getSimpleName());
                            long retryDelayMillis = recoveryRetryDelayMillis(ex);
                            recoveryTask = scheduler.schedule(() -> {
                                synchronized (this) {
                                    if (!closed && attempt == generation) {
                                        recoveryTask = recoveryExecutor.submit(() -> restoreHistory(attempt));
                                    }
                                }
                            }, retryDelayMillis, TimeUnit.MILLISECONDS);
                        } else {
                            disconnect(ex);
                        }
                    }
                }
            }
        }

        /**
         * 识别可通过延迟重连恢复的网络故障，不吞掉协议或数据错误。
         */
        private boolean networkFailure(Throwable error) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (cause instanceof java.io.IOException || cause instanceof ExchangeMarketPort.RequestDeferredException ||
                    cause instanceof ExchangeMarketPort.RateLimitException) {
                    return true;
                }
            }
            return false;
        }

        private long recoveryRetryDelayMillis(Throwable error) {
            long delay = TimeUnit.SECONDS.toMillis(Math.max(1, properties.getMarket().getReconnectDelaySeconds()));
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (cause instanceof ExchangeMarketPort.RequestDeferredException deferred) {
                    delay = Math.max(delay, deferred.retryAfterMillis());
                    delay += java.util.concurrent.ThreadLocalRandom.current().nextLong(0, 1_001L);
                } else if (cause instanceof ExchangeMarketPort.RateLimitException rateLimit) {
                    delay = Math.max(delay, rateLimit.retryAfterMillis());
                    delay += java.util.concurrent.ThreadLocalRandom.current().nextLong(0, 1_001L);
                }
            }
            return delay;
        }

        private boolean rateLimited(Throwable error) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (cause instanceof ExchangeMarketPort.RateLimitException) {
                    return true;
                }
            }
            return false;
        }

        private boolean requestDeferred(Throwable error) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (cause instanceof ExchangeMarketPort.RequestDeferredException) {
                    return true;
                }
            }
            return false;
        }

        private boolean clockUnavailable(Throwable error) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (cause instanceof ExchangeMarketPort.RequestDeferredException ||
                    cause instanceof ExchangeMarketPort.RateLimitException) {
                    return true;
                }
            }
            return false;
        }

        private boolean unsupportedSymbol(Throwable error) {
            for (Throwable cause = error; cause != null; cause = cause.getCause()) {
                if (cause instanceof ExchangeMarketPort.UnsupportedMarketSymbolException) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 仅重试公共只读查询，绝不重试交易副作用。
         */
        private <T> T recoveryRead(long attempt, java.util.function.Supplier<T> query) {
            for (int retry = 0; ; retry++) {
                if (closed || attempt != generation) {
                    throw new java.util.concurrent.CancellationException("行情恢复代次失效");
                }
                try {
                    recoveryReads.acquire();
                    try {
                        if (closed || attempt != generation) {
                            throw new java.util.concurrent.CancellationException("行情恢复代次失效");
                        }
                        return query.get();
                    } finally {
                        recoveryReads.release();
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new java.util.concurrent.CancellationException("行情恢复已取消");
                } catch (RuntimeException ex) {
                    if (retry >= 2 || !networkFailure(ex) || rateLimited(ex) || requestDeferred(ex)) {
                        throw ex;
                    }
                    log.warn("行情REST只读重试: symbol={}, interval={}, attempt={}, errorType={}", key.symbol(), key.interval(), retry + 1, ex.getClass().getSimpleName());
                    try {
                        Thread.sleep(500L << retry);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new java.util.concurrent.CancellationException("行情恢复已取消");
                    }
                }
            }
        }

        /**
         * 发布历史补偿事件并忽略过期恢复轮次产生的结果。
         */
        private void publishReplay(long attempt, ExchangeGateway.MarketEvent event) {
            if (closed || attempt != generation) {
                return;
            }
            listeners.forEach((id, consumer) -> {
                if (event.kline() != null) {
                    consumedKlineOpens.putIfAbsent(id, event.kline().openTime());
                }
                if (event.sequenceId() != null) {
                    consumedTradeIds.putIfAbsent(id, event.sequenceId() - 1);
                }
                consumer.acceptReplay(new Incoming(attempt, event));
            });
        }

        /**
         * 最多保留当前及前三根K线，不另发REST，也不回放交易信号。
         */
        private void rememberReference(ExchangeGateway.Kline kline) {
            referenceKlines.put(kline.openTime(), kline);
            while (referenceKlines.size() > 4) {
                referenceKlines.pollFirstEntry();
            }
        }

        /**
         * 校验补偿 K 线时间边界和连续性；不完整时保持流未就绪。
         */
        private void validateKline(ExchangeGateway.Kline kline) {
            if (kline == null || kline.openTime() == null || kline.closeTime() == null || kline.open() == null || kline.high() == null || kline.low() == null || kline.close() == null || kline.volume() == null || kline.open().signum() <= 0 || kline.low().signum() <= 0 || kline.close().signum() <= 0 || kline.volume().signum() < 0 || kline.high().compareTo(kline.open().max(kline.close())) < 0 || kline.low().compareTo(kline.open().min(kline.close())) > 0 || kline.closeTime().isBefore(kline.openTime())) {
                throw new IllegalStateException("K线OHLCV或时间字段无效");
            }
        }

        /**
         * 按聚合成交游标补偿断线区间并验证补偿完整性。
         */
        private void recoverTrades(long attempt) {
            if (lastTradeId == null) {
                return;
            }
            for (int page = 0; page < 100 && !closed && attempt == generation; page++) {
                long fromId = lastTradeId + 1;
                List<ExchangeGateway.MarketEvent> trades = recoveryRead(attempt, () -> "TESTNET".equals(key.runMode()) ? gateway.queryAggregateTradesFrom(key.runMode(),
                    key.symbol(), key.interval(), fromId, 1000) : gateway.queryAggregateTradesFrom(key.symbol(), key.interval(), fromId, 1000));
                if (trades == null || trades.isEmpty()) {
                    return;
                }
                for (ExchangeGateway.MarketEvent trade : trades) {
                    if (trade.sequenceId() == null || trade.sequenceId() != lastTradeId + 1) {
                        throw new IllegalStateException("REST 成交补偿仍存在序号缺口");
                    }
                    synchronized (this) {
                        if (closed || attempt != generation) {
                            return;
                        }
                        lastTradeId = trade.sequenceId();
                    }
                    publishReplay(attempt, trade);
                }
                if (trades.size() < 1000) {
                    return;
                }
            }
            throw new IllegalStateException("成交补偿超过100页，继续保持断线状态，禁止静默跳过历史");
        }

        private Instant nextOpen(Instant open) {
            String interval = key.interval();
            int count = Integer.parseInt(interval.substring(0, interval.length() - 1));
            return switch (interval.charAt(interval.length() - 1)) {
                case 'm' -> open.plusSeconds(count * 60L);
                case 'h' -> open.plusSeconds(count * 3600L);
                case 'd' -> open.plusSeconds(count * 86400L);
                case 'w' -> open.plusSeconds(count * 604800L);
                case 'M' -> open.atZone(ZoneOffset.UTC).plusMonths(count).toInstant();
                default -> throw new IllegalArgumentException("不支持的K线周期: " + interval);
            };
        }

        /**
         * 接收上游事件，过滤旧代次消息并更新连续性与新鲜度状态。
         */
        private synchronized void onEvent(long attempt, ExchangeGateway.MarketEvent event) {
            if (closed || attempt != generation || event == null) {
                return;
            }
            if (recovering) {
                int capacity = Math.max(1, properties.getMarket().getQueueCapacity());
                if (recoveryBuffer.size() >= capacity) {
                    scheduleDisconnect("行情恢复期间实时事件缓冲区溢出，必须重新补偿");
                    return;
                }
                recoveryBuffer.addLast(new Incoming(attempt, event));
                return;
            }
            if ("DISCONNECTED".equals(event.type()) || "ERROR".equals(event.type())) {
                disconnect(new IllegalStateException("上游行情流断开"));
                return;
            }
            if (!List.of("TRADE", "MARK", "KLINE").contains(event.type())) {
                return;
            }
            long exchangeOffsetMillis;
            try {
                exchangeOffsetMillis = gateway.exchangeTimeOffsetMillis(key.runMode());
            } catch (RuntimeException ex) {
                if (!clockUnavailable(ex)) {
                    disconnect(ex);
                    return;
                }
                connected = false;
                signalUnhealthy();
                warnClockUnavailable(ex);
                return;
            }
            Instant now = Instant.now();
            Instant exchangeNow = now.plusMillis(exchangeOffsetMillis);
            if (event.eventTime() == null) {
                disconnect(new IllegalStateException("Binance事件时间缺失: type=" + event.type() + ", symbol=" + key.symbol()));
                return;
            }
            long futureSkewMillis = java.time.Duration.between(exchangeNow, event.eventTime()).toMillis();
            if (futureSkewMillis > MAX_FUTURE_CLOCK_SKEW_MILLIS) {
                disconnect(new IllegalStateException("Binance事件时间超前过大: symbol=" + key.symbol() + ", type=" + event.type() + ", futureSkewMs=" + futureSkewMillis + ", eventTime=" + event.eventTime() + ", exchangeNow=" + exchangeNow));
                return;
            }
            Instant receivedAt = event.receivedAt() == null ? now : event.receivedAt();
            Instant processingEventTime = futureSkewMillis > 0 ? receivedAt.plusMillis(exchangeOffsetMillis) : event.eventTime();
            if (futureSkewMillis > 1_000L && System.currentTimeMillis() >= nextClockSkewWarningAt) {
                nextClockSkewWarningAt = System.currentTimeMillis() + 30_000L;
                log.warn("Binance行情时间领先服务端校准时钟，按校准后的接收时间处理: mode={} symbol={} type={} futureSkewMs={} eventTime={} receivedAt={}", key.runMode(), key.symbol(), event.type(),
                    futureSkewMillis, event.eventTime(), receivedAt);
            }
            long networkLag = Math.max(0, java.time.Duration.between(event.eventTime(), receivedAt.plusMillis(exchangeOffsetMillis)).toMillis());
            long queueLag = Math.max(0, java.time.Duration.between(receivedAt, now).toMillis());
            maxNetworkLag.accumulate(networkLag);
            maxQueueLag.accumulate(queueLag);
            lastReceived.put(event.type(), receivedAt);
            long maxEventAgeMillis = Math.max(1, properties.getMarket().getMaxEventAgeMillis());
            boolean fresh = MarketEventFreshness.isFresh(processingEventTime, receivedAt, now, exchangeNow, maxEventAgeMillis);
            if ("MARK".equals(event.type())) {
                lastMarkProcessingLagMs = java.time.Duration.between(processingEventTime, exchangeNow).toMillis();
                lastMarkFreshAtProcessing = fresh;
            }
            Instant previous = lastEventTime.get(event.type());
            if (previous != null && processingEventTime.isBefore(previous)) {
                return;
            }
            if (event.kline() != null) {
                ExchangeGateway.Kline kline = event.kline();
                validateKline(kline);
                if (lastKlineOpen != null && kline.openTime().isBefore(lastKlineOpen)) {
                    return;
                }
                if (lastKlineOpen != null && kline.openTime().equals(lastKlineOpen) && lastKlineClosed && !kline.closed()) {
                    return;
                }
                if (lastKlineOpen != null && kline.openTime().isAfter(nextOpen(lastKlineOpen))) {
                    beginRecovery("检测到K线时间缺口", null);
                    return;
                }
                if (lastKlineOpen != null && kline.openTime().isAfter(lastKlineOpen) && !lastKlineClosed) {
                    beginRecovery("上一根K线收盘状态未确认", null);
                    return;
                }
                if (!kline.closed() && !isEventTimeWithinKlinePeriod(event.eventTime(), kline.openTime(), nextOpen(kline.openTime()))) {
                    beginRecovery("形成中K线与事件周期不一致，校验当前周期", null);
                    return;
                }
                rememberReference(kline);
                lastKlineOpen = kline.openTime();
                lastKlineClosed = kline.closed();
            }
            event = new ExchangeGateway.MarketEvent(event.type(), event.symbol(), event.interval(), event.price(), event.kline(), processingEventTime, event.sequenceId(),
                receivedAt, event.replay() || !fresh);
            lastEventTime.put(event.type(), processingEventTime);
            if (event.sequenceId() != null && "TRADE".equals(event.type())) {
                if (lastTradeId != null && event.sequenceId() <= lastTradeId) {
                    return;
                }
                if (lastTradeId != null && event.sequenceId() > lastTradeId + 1) {
                    log.warn("成交事件序号缺口: symbol={}, previous={}, current={}，保留WS并精确补偿", key.symbol(), lastTradeId, event.sequenceId());
                    beginRecovery("成交事件序号缺口", event.sequenceId());
                    return;
                }
                if (!currentKlineValid(exchangeNow)) {
                    if (lastTradeId == null) {
                        lastTradeId = event.sequenceId() - 1;
                    }
                    beginRecovery("成交到达新周期，先确认K线状态", event.sequenceId());
                    return;
                }
                lastTradeId = event.sequenceId();
            }
            if (!connected && currentKlineValid(exchangeNow) && lastEventTime.get("MARK") != null && !lastEventTime.get("MARK").isBefore(exchangeNow.minusMillis(Math.max(1,
                properties.getMarket().getMaxEventAgeMillis())))) {
                connected = true;
                log.info("行情已就绪: symbol={}, interval={}, klineOpen={}, networkLagMs={}, queueLagMs={}", key.symbol(), key.interval(), lastKlineOpen, networkLag, queueLag);
                publish(status("CONNECTED"));
            }
            publish(event);
        }

        static boolean isEventTimeWithinKlinePeriod(Instant eventTime, Instant openTime, Instant nextOpen) {
            return eventTime != null && openTime != null && nextOpen != null
                && !eventTime.isBefore(openTime) && eventTime.isBefore(nextOpen);
        }

        /**
         * K线可以由REST或WS确认；不把低活跃交易对的静默当成断线。
         */
        private boolean currentKlineValid(Instant exchangeNow) {
            return lastKlineOpen != null && !lastKlineClosed && !lastKlineOpen.isAfter(exchangeNow) && nextOpen(lastKlineOpen).isAfter(exchangeNow);
        }

        /**
         * 暂停决策并保留上游连接；补偿事件只恢复状态，不触发回溯交易。
         */
        private void beginRecovery(String reason, Long tradeId) {
            if (closed || recovering) {
                return;
            }
            connected = false;
            recovering = true;
            requiredTradeId = tradeId;
            recoveryStarted = Instant.now();
            signalUnhealthy();
            // 同一周期可能先后由watchdog、TRADE和KLINE触发校验；避免刷屏误报为多次断线。
            Instant warningPeriod = lastKlineOpen == null ? null : nextOpen(lastKlineOpen);
            if (warningPeriod == null || !warningPeriod.equals(lastRecoveryWarningPeriod)) {
                lastRecoveryWarningPeriod = warningPeriod;
                log.warn("行情暂停决策并校验补偿，保留WS: symbol={}, interval={}, reason={}", key.symbol(), key.interval(), reason);
            } else {
                log.debug("同周期再次触发K线REST校验: symbol={}, interval={}, reason={}", key.symbol(), key.interval(), reason);
            }
            publish(status("CONNECTING"));
            long attempt = generation;
            recoveryTask = recoveryExecutor.submit(() -> restoreHistory(attempt));
        }

        /**
         * 构造携带当前连接代次与健康状态的内部流事件。
         */
        private ExchangeGateway.MarketEvent status(String type) {
            return new ExchangeGateway.MarketEvent(type, key.symbol(), key.interval(), null, null, Instant.now());
        }

        /**
         * 向订阅者分发事件，并隔离单个监听器抛出的异常。
         */
        private void publish(ExchangeGateway.MarketEvent event) {
            listeners.forEach((id, listener) -> {
                if ("TRADE".equals(event.type()) && event.sequenceId() != null) {
                    consumedTradeIds.putIfAbsent(id, event.sequenceId() - 1);
                }
                if (event.kline() != null) {
                    consumedKlineOpens.putIfAbsent(id, event.kline().openTime());
                }
                listener.accept(new Incoming(generation, event));
            });
        }

        /**
         * 标记行情暂不可用并触发异步重连与缺口恢复。
         */
        private synchronized void scheduleDisconnect(String message) {
            connected = false;
            signalUnhealthy();
            if (!closed) {
                scheduler.execute(() -> disconnect(new IllegalStateException(message)));
            }
        }

        /**
         * 通知订阅者暂停决策，直到流恢复并完成补偿。
         */
        private void signalUnhealthy() {
            healthListeners.values().forEach(health -> health.accept(false));
        }

        /**
         * 定时检查流事件年龄、补偿状态和下游健康回调。
         */
        private synchronized void checkHealth() {
            if (closed || terminalFailure || reconnect != null) {
                return;
            }
            long exchangeOffsetMillis;
            try {
                exchangeOffsetMillis = gateway.exchangeTimeOffsetMillis(key.runMode());
            } catch (RuntimeException ex) {
                if (clockUnavailable(ex)) {
                    connected = false;
                    signalUnhealthy();
                    warnClockUnavailable(ex);
                    return;
                }
                disconnect(ex);
                return;
            }
            Instant localNow = Instant.now();
            Instant exchangeNow = localNow.plusMillis(exchangeOffsetMillis);
            if (!connected && System.currentTimeMillis() >= nextMetricsAt) {
                nextMetricsAt = System.currentTimeMillis() + 60_000L;
                Instant now = localNow;
                Instant markTime = lastEventTime.get("MARK");
                Instant markReceived = lastReceived.get("MARK");
                long maxAge = Math.max(1, properties.getMarket().getMaxEventAgeMillis());
                log.warn("行情未就绪诊断 mode={} symbol={} interval={} recovering={} wsMarkSeen={} markAgeMs={} markReceiveAgeMs={} maxEventAgeMs={} markFresh={} markProcessLagMs={} " +
                    "markFreshAtProcessing={} klineOpen={} klineClosed={} currentKlineValid={} listeners={} ingress={}", key.runMode(), key.symbol(), key.interval(), recovering,
                    markTime != null, markTime == null ? null : java.time.Duration.between(markTime, exchangeNow).toMillis(), markReceived == null ? null :
                        java.time.Duration.between(markReceived, now).toMillis(), maxAge, markTime != null && !markTime.isBefore(exchangeNow.minusMillis(maxAge)),
                    lastMarkProcessingLagMs, lastMarkFreshAtProcessing, lastKlineOpen, lastKlineClosed, currentKlineValid(exchangeNow), listeners.size(), ingress.statistics());
            }
            if (connectionStarted != null && connectionStarted.isBefore(Instant.now().minusSeconds(23 * 3600 + 55 * 60))) {
                disconnect(new IllegalStateException("连接接近24小时有效期，主动轮换并补偿"));
                return;
            }
            if (recovering) {
                if ((recoveryTask == null || recoveryTask.isDone()) && recoveryStarted != null && recoveryStarted.isBefore(Instant.now().minusSeconds(120))) {
                    disconnect(new IllegalStateException("行情历史恢复超时"));
                }
                return;
            }
            Instant deadline = Instant.now().minusSeconds(Math.max(1, properties.getMarket().getIdleTimeoutSeconds()));
            // MARK按固定频率推送，用于检测连接存活；KLINE是数据更新流。
            if (lastReceived.getOrDefault("MARK", Instant.EPOCH).isBefore(deadline)) {
                disconnect(new IllegalStateException("MARK流超时未收到事件"));
                return;
            }
            if (!currentKlineValid(exchangeNow)) {
                beginRecovery("K线周期已切换，REST校验当前状态", null);
            } else if (lastReceived.getOrDefault("KLINE", Instant.EPOCH).isBefore(deadline)) {
                checkQuietKline();
            }
        }

        private void warnClockUnavailable(RuntimeException error) {
            long now = System.currentTimeMillis();
            if (now >= nextClockSyncWarningAt) {
                nextClockSyncWarningAt = now + 30_000L;
                log.warn("Binance时间校准受限，保留WebSocket但暂停行情决策: symbol={} interval={} errorType={}", key.symbol(), key.interval(), error.getClass().getSimpleName());
            }
        }

        /**
         * 周期仍有效且 MARK 存活时，静默只触发合并的后台校验，不暂停健康行情。
         */
        private synchronized void checkQuietKline() {
            if (System.currentTimeMillis() < nextQuietCheckAt || quietCheckTask != null && !quietCheckTask.isDone()) {
                return;
            }
            long attempt = generation;
            Instant expectedOpen = lastKlineOpen;
            Instant expectedReceived = lastReceived.get("KLINE");
            nextQuietCheckAt = System.currentTimeMillis() + 60_000L;
            quietCheckTask = recoveryExecutor.submit(() -> {
                try {
                    List<ExchangeGateway.Kline> history = recoveryRead(attempt, () -> history(expectedOpen, 1000));
                    synchronized (this) {
                        if (closed || attempt != generation || recovering || !expectedOpen.equals(lastKlineOpen) || !java.util.Objects.equals(expectedReceived, lastReceived.get(
                            "KLINE"))) {
                            return;
                        }
                        if (history == null || history.isEmpty()) {
                            throw new IllegalStateException("静默校验K线为空");
                        }
                        for (ExchangeGateway.Kline kline : history) {
                            validateKline(kline);
                        }
                        ExchangeGateway.Kline latest = history.get(history.size() - 1);
                        if (!latest.openTime().equals(expectedOpen) || latest.closed() || !currentKlineValid(exchangeNow())) {
                            beginRecovery("静默校验发现K线周期状态变化", null);
                            return;
                        }
                        // 不刷新 MARK 心跳；REST 事件只恢复K线状态，不生成回溯交易。
                        rememberReference(latest);
                        publishReplay(attempt, new ExchangeGateway.MarketEvent("KLINE", key.symbol(), key.interval(), null, latest, Instant.now(), null, Instant.now(), true));
                    }
                } catch (RuntimeException ex) {
                    synchronized (this) {
                        if (!closed && attempt == generation && !recovering) {
                            beginRecovery("静默校验未能确认K线状态", null);
                        }
                    }
                }
            });
        }

        /**
         * 使当前连接代次失效并安排受控重连和历史补偿。
         */
        private synchronized void disconnect(Throwable cause) {
            if (closed || terminalFailure || reconnect != null) {
                return;
            }
            log.warn("行情中断，暂停决策并重建: symbol={}, interval={}", key.symbol(), key.interval(), cause);
            signalUnhealthy();
            ++generation;
            connected = false;
            recovering = false;
            notifyAll();
            if (recoveryTask != null) {
                recoveryTask.cancel(true);
            }
            if (quietCheckTask != null) {
                quietCheckTask.cancel(true);
            }
            nextQuietCheckAt = 0L;
            log.warn("行情恢复队列统计 symbol={}, interval={}, {}", key.symbol(), key.interval(), ingress.statistics());
            // 收到并不等于处理完成；从最慢消费者的提交游标补偿被清空的事件。
            lastTradeId = consumedTradeIds.values().stream().min(Long::compareTo).orElse(lastTradeId);
            lastKlineOpen = consumedKlineOpens.values().stream().min(Instant::compareTo).orElse(lastKlineOpen);
            lastKlineClosed = false;
            ingress.clear();
            recoveryBuffer.clear();
            listeners.values().forEach(StreamEventConsumer::clear);
            publish(status("DISCONNECTED"));
            ExchangeGateway.MarketStreamSession old = upstream;
            upstream = null;
            int baseReconnectDelay = Math.max(1, properties.getMarket().getReconnectDelaySeconds());
            long jitterMillis = ThreadLocalRandom.current().nextLong(0, TimeUnit.SECONDS.toMillis(baseReconnectDelay) + 1L);
            long reconnectDelay = reconnectDelayMillis(baseReconnectDelay, jitterMillis);
            reconnect = scheduler.schedule(() -> {
                synchronized (this) {
                    reconnect = null;
                    start();
                }
            }, reconnectDelay, TimeUnit.MILLISECONDS);
            if (old != null) {
                try {
                    old.close();
                } catch (RuntimeException ex) {
                    log.warn("关闭旧行情连接失败", ex);
                }
            }
        }

        private synchronized void close() {
            closed = true;
            signalUnhealthy();
            ++generation;
            recovering = false;
            notifyAll();
            if (recoveryTask != null) {
                recoveryTask.cancel(true);
            }
            if (quietCheckTask != null) {
                quietCheckTask.cancel(true);
            }
            nextQuietCheckAt = 0L;
            if (watchdog != null) {
                watchdog.cancel(false);
            }
            if (reconnect != null) {
                reconnect.cancel(false);
            }
            if (upstream != null) {
                upstream.close();
                upstream = null;
            }
            ingress.close();
            recoveryBuffer.clear();
            listeners.values().forEach(StreamEventConsumer::close);
            listeners.clear();
        }

        /**
         * 将交易环境明确拒绝的标的隔离为终态，避免循环重建同一行情连接。
         */
        private synchronized void failPermanently(Throwable cause) {
            if (closed || terminalFailure) {
                return;
            }
            terminalFailure = true;
            signalUnhealthy();
            ++generation;
            connected = false;
            recovering = false;
            notifyAll();
            if (recoveryTask != null) {
                recoveryTask.cancel(true);
            }
            if (quietCheckTask != null) {
                quietCheckTask.cancel(true);
            }
            nextQuietCheckAt = 0L;
            ingress.clear();
            recoveryBuffer.clear();
            listeners.values().forEach(StreamEventConsumer::clear);
            log.error("交易环境拒绝该行情交易对，已暂停该标的并停止自动重连: mode={} symbol={} interval={}", key.runMode(), key.symbol(), key.interval(), cause);
            publish(status("ERROR"));
            ExchangeGateway.MarketStreamSession old = upstream;
            upstream = null;
            if (old != null) {
                try {
                    old.close();
                } catch (RuntimeException ex) {
                    log.warn("关闭不支持标的的行情连接失败: mode={} symbol={}", key.runMode(), key.symbol(), ex);
                }
            }
        }
    }
}
