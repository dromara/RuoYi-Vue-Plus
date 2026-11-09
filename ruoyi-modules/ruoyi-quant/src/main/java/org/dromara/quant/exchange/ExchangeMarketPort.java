package org.dromara.quant.exchange;

import java.util.List;
import java.util.function.Consumer;

/**
 * 交易所行情端口。
 */
public interface ExchangeMarketPort {
    /**
     * 获取交易所时间相对本机时间的偏移；非 Binance 网关默认无偏移。
     */
    default long exchangeTimeOffsetMillis(String runMode) {
        return 0L;
    }

    private static void requireSupportedMarket(String runMode) {
        if ("TESTNET".equalsIgnoreCase(runMode)) {
            throw new IllegalStateException("网关不支持测试网行情，拒绝回退正式网");
        }
    }

    ExchangeGateway.ExchangeInfo queryExchangeInfo();

    /**
     * 获取订单实际交易环境的规则；不支持环境规则的网关保留调用方规则。
     */
    default ExchangeGateway.SymbolRule queryTradingRule(ExchangeGateway.Credential credential, String symbol) {
        return null;
    }

    List<ExchangeGateway.Kline> queryKlines(String symbol, String interval, int limit);

    /**
     * 从指定开盘时间开始补齐断线期间的K线。
     */
    default List<ExchangeGateway.Kline> queryKlinesFrom(String symbol, String interval, java.time.Instant from, int limit) {
        return queryKlines(symbol, interval, limit);
    }

    /**
     * 按聚合成交序号补偿，不允许将历史成交当作新的交易信号。
     */
    default List<ExchangeGateway.MarketEvent> queryAggregateTradesFrom(String symbol, String interval, long fromId, int limit) {
        return List.of();
    }

    /**
     * 环境感知行情端口；未实现测试网的网关必须拒绝，不能回退正式网。
     */
    default List<ExchangeGateway.Kline> queryKlines(String runMode, String symbol, String interval, int limit) {
        return queryKlinesFrom(runMode, symbol, interval, null, limit);
    }

    default List<ExchangeGateway.Kline> queryKlinesFrom(String runMode, String symbol, String interval, java.time.Instant from, int limit) {
        requireSupportedMarket(runMode);
        return queryKlinesFrom(symbol, interval, from, limit);
    }

    default List<ExchangeGateway.MarketEvent> queryAggregateTradesFrom(String runMode, String symbol, String interval, long fromId, int limit) {
        requireSupportedMarket(runMode);
        return queryAggregateTradesFrom(symbol, interval, fromId, limit);
    }

    default ExchangeGateway.PriceSnapshot queryPrice(String runMode, String symbol) {
        requireSupportedMarket(runMode);
        return queryPrice(symbol);
    }

    /**
     * 查询最新成交价；策略保护兜底不需要额外请求标记价格。
     */
    default ExchangeGateway.PriceSnapshot queryLastTradePrice(String runMode, String symbol) {
        ExchangeGateway.PriceSnapshot snapshot = queryPrice(runMode, symbol);
        return snapshot == null ? null : new ExchangeGateway.PriceSnapshot(snapshot.symbol(), snapshot.lastTradePrice(), null, snapshot.observedAt());
    }

    default ExchangeGateway.MarketStreamSession subscribeMarket(String runMode, String symbol, String interval, Consumer<ExchangeGateway.MarketEvent> listener) {
        requireSupportedMarket(runMode);
        return subscribeMarket(symbol, interval, listener);
    }

    ExchangeGateway.PriceSnapshot queryPrice(String symbol);

    ExchangeGateway.MarketStreamSession subscribeMarket(String symbol, String interval, Consumer<ExchangeGateway.MarketEvent> listener);

    /**
     * 本地请求尚未发出，调用方可以退避后重试只读操作。
     */
    class RequestDeferredException extends IllegalStateException {
        private final long retryAfterMillis;
        private final String reason;
        private final String kind;
        private final int queueSize;
        private final int[] queueSizes;
        private final long waitedMillis;

        public RequestDeferredException() {
            this("DEFERRED", "UNKNOWN", 0, new int[0], 0L, 1_000L);
        }

        public RequestDeferredException(long retryAfterMillis) {
            this("DEFERRED", "UNKNOWN", 0, new int[0], 0L, Math.max(1L, retryAfterMillis));
        }

        public RequestDeferredException(String reason, String kind, int queueSize, int[] queueSizes,
                                        long waitedMillis, long retryAfterMillis) {
            super("REST 请求被本地调度器延后，当前调用尚未发送: reason=" + reason + ", kind=" + kind
                + ", queueSize=" + queueSize + ", waitedMs=" + waitedMillis);
            this.reason = reason;
            this.kind = kind;
            this.queueSize = queueSize;
            this.queueSizes = queueSizes == null ? new int[0] : queueSizes.clone();
            this.waitedMillis = Math.max(0L, waitedMillis);
            this.retryAfterMillis = Math.max(0L, retryAfterMillis);
        }

        public long retryAfterMillis() {
            return retryAfterMillis;
        }

        public String reason() {
            return reason;
        }

        public String kind() {
            return kind;
        }

        public int queueSize() {
            return queueSize;
        }

        public int[] queueSizes() {
            return queueSizes.clone();
        }

        public long waitedMillis() {
            return waitedMillis;
        }
    }

    /** Binance 已返回 418/429；调用方应按 Retry-After 退避，不能重建健康 WebSocket。 */
    class RateLimitException extends ExchangeTradingPort.ExchangeRequestException {
        private final long retryAfterMillis;

        public RateLimitException(String stage, int httpStatus, Integer code, String message, long retryAfterMillis) {
            super(stage, httpStatus, code, message);
            this.retryAfterMillis = Math.max(1L, retryAfterMillis);
        }

        public long retryAfterMillis() {
            return retryAfterMillis;
        }
    }

    /**
     * 交易环境明确拒绝该标的；重建连接不能修复，应等待标的或环境配置变更。
     */
    class UnsupportedMarketSymbolException extends IllegalStateException {
        public UnsupportedMarketSymbolException(String runMode, String symbol) {
            super("交易环境不支持该行情交易对: mode=" + runMode + ", symbol=" + symbol);
        }
    }
}
