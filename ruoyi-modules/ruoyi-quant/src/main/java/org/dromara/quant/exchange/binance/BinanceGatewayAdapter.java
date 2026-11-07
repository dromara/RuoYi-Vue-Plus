package org.dromara.quant.exchange.binance;

import com.binance.connector.client.common.ApiClient;
import com.binance.connector.client.common.ApiResponse;
import com.binance.connector.client.common.Pair;
import com.binance.connector.client.common.auth.Authentication;
import com.binance.connector.client.common.auth.BinanceAuthenticationFactory;
import com.binance.connector.client.common.auth.BinanceBaseAuthentication;
import com.binance.connector.client.common.auth.SignatureGeneratorFactory;
import com.binance.connector.client.common.configuration.ClientConfiguration;
import com.binance.connector.client.common.configuration.SignatureConfiguration;
import com.binance.connector.client.common.sign.SignatureGenerator;
import com.binance.connector.client.derivatives_trading_usds_futures.rest.api.DerivativesTradingUsdsFuturesRestApi;
import com.binance.connector.client.derivatives_trading_usds_futures.rest.model.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.CryptoException;
import org.dromara.quant.config.QuantProperties;
import org.dromara.quant.exchange.ExchangeGateway;
import org.dromara.quant.exchange.ExchangeMarketPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Binance USDⓈ-M Futures SDK 12.x 适配器。SDK 类型只允许出现在本类。
 */
@Component
@Slf4j
public class BinanceGatewayAdapter implements ExchangeGateway {
    /**
     * Binance 服务器时间偏移刷新周期，单位毫秒。
     */
    private static final long CLOCK_OFFSET_REFRESH_MILLIS = 30_000L;
    private static final long MAX_STALE_MARKET_CLOCK_OFFSET_MILLIS = 5 * 60_000L;
    /**
     * Binance 签名请求允许的时间窗口，覆盖代理和网络抖动。
     */
    private static final long RECV_WINDOW_MILLIS = 60_000L;
    private static final long MARKET_REST_CONNECT_TIMEOUT_SECONDS = 5L;
    private static final long MARKET_REST_READ_TIMEOUT_SECONDS = 15L;
    private static final long MARKET_REST_CALL_TIMEOUT_SECONDS = 20L;
    private static final long ACCOUNT_REST_CONNECT_TIMEOUT_SECONDS = 5L;
    private static final long ACCOUNT_REST_READ_TIMEOUT_SECONDS = 15L;
    private static final long ACCOUNT_REST_CALL_TIMEOUT_SECONDS = 20L;
    private static final int ALGO_HISTORY_PAGE_SIZE = 1000;
    private static final int ALGO_HISTORY_MAX_QUERIES = 4096;
    /**
     * 量化模块配置。
     */
    private final QuantProperties properties;
    private final okhttp3.ConnectionPool restPool = new okhttp3.ConnectionPool();
    private final okhttp3.Dispatcher restDispatcher = new okhttp3.Dispatcher();
    /**
     * Binance 服务器时间偏移缓存。
     */
    private final Map<String, ClockOffset> clockOffsets = new ConcurrentHashMap<>();
    /**
     * 仅在当前线程执行服务端时间同步请求时记录真正进入 HTTP 链路的时间点。
     * 该时间点在 REST 调度器放行后记录，避免把本地排队耗时计入网络往返时间。
     */
    private final ThreadLocal<ClockSyncTiming> clockSyncTiming = new ThreadLocal<>();
    /**
     * 同一环境只允许一个服务器时间刷新，其他线程复用结果。
     */
    private final Map<String, Object> clockOffsetRefreshLocks = new ConcurrentHashMap<>();
    private final Map<String, Long> clockOffsetWarningTimes = new ConcurrentHashMap<>();
    private final Map<String, TradingRules> tradingRules = new ConcurrentHashMap<>();
    /**
     * 进程内共享的 REST 请求节流时间点，避免同一出口 IP 触发 Binance 权重限制。
     */
    private final java.util.concurrent.ConcurrentHashMap<Credential, java.util.concurrent.CompletableFuture<AccountSnapshot>> accountQueries =
        new java.util.concurrent.ConcurrentHashMap<>();
    private final RestRequestScheduler restScheduler = new RestRequestScheduler(250);
    /**
     * Binance 事件解析使用的 JSON 工具。
     */
    private final Gson gson = new Gson();
    private final java.util.concurrent.atomic.AtomicReference<String> currentWebsocketTransport = new java.util.concurrent.atomic.AtomicReference<>();
    private final java.util.Set<StreamHandle> activeWebsocketStreams = ConcurrentHashMap.newKeySet();
    /**
     * 创建 BinanceGatewayAdapter 实例并初始化其依赖。
     */
    public BinanceGatewayAdapter(QuantProperties properties) {
        this.properties = properties;
        currentWebsocketTransport.set(validateWebsocketTransport(properties.getBinance().getWebsocketTransport()));
    }

    /**
     * 从 Binance 返回对象中安全读取字符串。
     */
    private static String string(Object bean, String getter) {
        try {
            Object value = bean.getClass().getMethod(getter).invoke(bean);
            return value == null ? null : String.valueOf(value);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Boolean nullableBoolean(Object bean, String getter) {
        String value = string(bean, getter);
        return value == null || value.isBlank() ? null : Boolean.valueOf(value);
    }

    /**
     * Normalize status-token casing and whitespace at the Binance boundary.
     * Unknown values remain unknown to the protection lifecycle and fail closed.
     */
    static String normalizeAlgoStatus(String status) {
        return status == null || status.isBlank() ? status : status.trim().toUpperCase(Locale.ROOT);
    }

    static BigDecimal protectionExecutedQuantity(String status, BigDecimal actualQuantity) {
        if (actualQuantity != null) {
            return actualQuantity;
        }
        return status != null && Set.of("CANCELED", "EXPIRED", "REJECTED").contains(normalizeAlgoStatus(status)) ? BigDecimal.ZERO : null;
    }

    /**
     * 从 Binance 返回对象中安全读取数值。
     */
    private static Long number(Object bean, String getter) {
        String value = string(bean, getter);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * 按主参数名、兼容别名和默认值读取 BigDecimal 参数。
     */
    private static BigDecimal decimal(String value) {
        return value == null || value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value);
    }

    /**
     * 把交易所时间戳转换为 Instant。
     */
    private static Instant instant(Long value) {
        return value == null ? Instant.EPOCH : Instant.ofEpochMilli(value);
    }

    /**
     * 读取必需数据，不满足条件时抛出异常。
     */
    private static void requireCredential(Credential credential) {
        if (credential == null || credential.apiKey() == null || credential.secretKey() == null || credential.apiKey().isBlank() || credential.secretKey().isBlank()) {
            throw new IllegalArgumentException("认证交易请求缺少有效凭证");
        }
    }

    /**
     * 私有账户交易能力校验。canWithdraw 表示合约账户资金转出能力，不能用于判断 API Key 提现权限。
     * 兼容字段 withdrawalDisabled 只记录 canWithdraw 是否明确为 false，不作为凭证或交易准入条件。
     */
    static ConnectivityResult tradingPermissions(Boolean canTrade, Boolean canWithdraw) {
        boolean tradeAllowed = Boolean.TRUE.equals(canTrade);
        boolean withdrawalDisabled = Boolean.FALSE.equals(canWithdraw);
        if (!tradeAllowed) {
            return new ConnectivityResult(false, "账户没有 Futures Trade 权限", false, withdrawalDisabled, Instant.now());
        }
        return new ConnectivityResult(true, "OK", true, withdrawalDisabled, Instant.now());
    }

    private static BigDecimal jsonDecimal(JsonObject data, String field) {
        if (!data.has(field) || data.get(field).isJsonNull()) {
            throw new IllegalStateException("Binance 公开行情缺少字段: " + field);
        }
        return decimal(data.get(field).getAsString());
    }

    private static String validateWebsocketTransport(String input) {
        String value = input == null ? "" : input.trim().toUpperCase(Locale.ROOT);
        if (!"RAW_OKHTTP".equals(value) && !"BINANCE_SDK".equals(value)) {
            throw new IllegalArgumentException("WebSocket transport must be RAW_OKHTTP or BINANCE_SDK");
        }
        return value;
    }

    /**
     * 设置交易对保证金模式。
     */
    private static com.binance.connector.client.common.ApiException apiError(RuntimeException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof com.binance.connector.client.common.ApiException error) {
                return error;
            }
            current = current.getCause();
        }
        return null;
    }

    private static Integer errorCode(com.binance.connector.client.common.ApiException error) {
        try {
            return com.google.gson.JsonParser.parseString(error.getResponseBody()).getAsJsonObject().get("code").getAsInt();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    static boolean isInvalidSymbolError(RuntimeException ex) {
        var error = apiError(ex);
        return error != null && Integer.valueOf(-1121).equals(errorCode(error));
    }

    /**
     * 将 SDK 异常映射为带阶段信息的安全错误，不输出认证数据。
     */
    private RuntimeException requestFailure(String stage, RuntimeException ex) {
        if (ex instanceof org.dromara.quant.exchange.ExchangeMarketPort.RequestDeferredException deferred) {
            return deferred;
        }
        if (ex instanceof ExchangeRequestException safe) {
            return safe;
        }
        var error = apiError(ex);
        String message = ex.getClass().getSimpleName();
        if (error != null) {
            try {
                message = com.google.gson.JsonParser.parseString(error.getResponseBody()).getAsJsonObject().get("msg").getAsString();
            } catch (RuntimeException ignored) {
            }
        }
        // 不附带原始异常，避免 SDK 的 URL、签名和请求头进入日志。
        if (error != null && (error.getCode() == 418 || error.getCode() == 429)) {
            long retryAfterMillis = retryAfterMillis(error);
            if (error.getCode() == 418 && retryAfterMillis < 0L) {
                // Binance did not provide a parseable ban expiry. Do not guess that the ban
                // expires after 60 seconds: require an operator-controlled process restart.
                restScheduler.blockUntilManualReset();
                log.error("Binance REST HTTP 418 has no trustworthy expiry; REST is latched closed until operator restart. stage={}", stage);
                return new ExchangeMarketPort.RateLimitException(stage, error.getCode(), errorCode(error), message, 86_400_000L);
            }
            restScheduler.deferFor(retryAfterMillis);
            return new ExchangeMarketPort.RateLimitException(stage, error.getCode(), errorCode(error), message, retryAfterMillis);
        }
        return new ExchangeRequestException(stage, error == null ? null : error.getCode(), error == null ? null : errorCode(error), message);
    }

    private static long retryAfterMillis(com.binance.connector.client.common.ApiException error) {
        String retryAfter = null;
        if (error.getResponseHeaders() != null) {
            for (var header : error.getResponseHeaders().entrySet()) {
                if ("retry-after".equalsIgnoreCase(header.getKey()) && header.getValue() != null && !header.getValue().isEmpty()) {
                    retryAfter = header.getValue().getFirst();
                    break;
                }
            }
        }
        return retryAfterMillis(error.getCode(), retryAfter, error.getResponseBody());
    }

    private static long retryAfterMillis(int statusCode, String retryAfter, String body) {
        if (retryAfter != null && !retryAfter.isBlank()) {
            String value = retryAfter.trim();
            try {
                return Math.max(1L, Math.multiplyExact(Long.parseLong(value), 1_000L));
            } catch (RuntimeException ignored) {
                try {
                    return Math.max(1L, java.time.Duration.between(java.time.Instant.now(), java.time.ZonedDateTime.parse(value,
                        java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).toMillis());
                } catch (RuntimeException ignoredDate) {
                    // Fall through to Binance's ban-until timestamp.
                }
            }
        }
        if (body != null) {
            java.util.regex.Matcher bannedUntil = java.util.regex.Pattern.compile("(?i)banned until\\s+(\\d+)").matcher(body);
            if (bannedUntil.find()) {
                try {
                    return Math.max(1L, Long.parseLong(bannedUntil.group(1)) - System.currentTimeMillis());
                } catch (NumberFormatException ignored) {
                    // An unparseable expiry is not safe to treat as a short ban.
                }
            }
        }
        // Do not guess an expiry for HTTP 418: Binance IP bans can outlast a short fallback
        // and may escalate. A negative sentinel triggers a process-local manual-reset latch.
        if (statusCode == 418) {
            return -1L;
        }
        // A 429 without a usable Retry-After header still represents a rate-limit response.
        return 60_000L;
    }

    /**
     * Package-private for SDK DTO/hedge-row contract tests.
     */
    static org.dromara.quant.exchange.ExchangeAccountPort.SymbolTradingSettings parseSymbolTradingSettings(String expectedSymbol, java.util.List<?> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new IllegalStateException("Binance 未返回有效交易对配置");
        }
        String actualMode = null;
        Integer actualLeverage = null;
        for (var row : rows) {
            String actualSymbol = string(row, "getSymbol");
            if (!expectedSymbol.equalsIgnoreCase(actualSymbol)) {
                throw new IllegalStateException("Binance 配置回查返回了不匹配的交易对");
            }
            String isolated = string(row, "getIsolated");
            String mode = null;
            if ("true".equalsIgnoreCase(isolated)) {
                mode = "ISOLATED";
            } else if ("false".equalsIgnoreCase(isolated)) {
                mode = "CROSSED";
            }
            if (mode == null) {
                String raw = string(row, "getMarginType");
                if ("ISOLATED".equalsIgnoreCase(raw)) {
                    mode = "ISOLATED";
                } else if ("CROSS".equalsIgnoreCase(raw) || "CROSSED".equalsIgnoreCase(raw)) {
                    mode = "CROSSED";
                }
            }
            String rawLeverage = string(row, "getLeverage");
            if (mode == null || rawLeverage == null) {
                throw new IllegalStateException("Binance 配置回查缺少保证金模式或杠杆");
            }
            int leverage;
            try {
                leverage = Integer.parseInt(rawLeverage);
            } catch (NumberFormatException ex) {
                throw new IllegalStateException("Binance 返回的杠杆值无效");
            }
            if (leverage < 1 || leverage > 125 || actualMode != null && !actualMode.equals(mode) || actualLeverage != null && actualLeverage != leverage) {
                throw new IllegalStateException("Binance 不同持仓方向的保证金/杠杆配置不一致");
            }
            actualMode = mode;
            actualLeverage = leverage;
        }
        return new org.dromara.quant.exchange.ExchangeAccountPort.SymbolTradingSettings(expectedSymbol, actualMode, actualLeverage);
    }

    private void requireOrderDeadline(OrderRequest request) {
        if (request.validUntil() == null) {
            return;
        }
        String runMode = request.credential() == null ? "UNKNOWN" : request.credential().runMode();
        long exchangeOffsetMillis = request.credential() == null ? 0L : exchangeTimeOffsetMillis(runMode);
        Instant localNow = Instant.now();
        Instant exchangeNow = localNow.plusMillis(exchangeOffsetMillis);
        if (isOrderDeadlineExpired(request.validUntil(), localNow, exchangeOffsetMillis)) {
            log.warn("开仓信号过期，拒绝发送订单 runMode={} symbol={} localNow={} exchangeNow={} exchangeOffsetMillis={} signalDeadline={} remainingSignalMillis={}", runMode,
                request.symbol(), localNow, exchangeNow, exchangeOffsetMillis, request.validUntil(), java.time.Duration.between(exchangeNow, request.validUntil()).toMillis());
            throw new OrderNotSentException("开仓信号在交易请求准备期间过期，订单未发送，等待新的行情");
        }
    }

    static boolean isOrderDeadlineExpired(Instant validUntil, Instant localNow, long exchangeOffsetMillis) {
        return validUntil != null && !localNow.plusMillis(exchangeOffsetMillis).isBefore(validUntil);
    }

    static boolean hedgePositionSide(String positionSide) {
        return "LONG".equalsIgnoreCase(positionSide) || "SHORT".equalsIgnoreCase(positionSide);
    }

    /**
     * Mirrors the request-body classification used by the official SDK's HTTP
     * transport without depending on OkHttp's non-public internal classes.
     * Keep this rule aligned with connector updates to prevent signature drift.
     */
    static boolean requiresRequestBody(String method) {
        if (method == null) {
            return false;
        }
        return switch (method.toUpperCase(java.util.Locale.ROOT)) {
            case "POST", "PUT", "PATCH", "PROPPATCH", "REPORT" -> true;
            default -> false;
        };
    }

    /**
     * 验证交易所连通性。
     */
    @Override
    public ConnectivityResult testConnectivity() {
        try {
            api(null).testConnectivity();
            return new ConnectivityResult(true, "Binance 网络连接正常");
        } catch (RuntimeException ex) {
            return new ConnectivityResult(false, "Binance 网络连接失败");
        }
    }

    /**
     * 校验交易凭证与权限。
     */
    @Override
    public ConnectivityResult validateCredential(Credential credential) {
        try {
            if (credential == null) {
                return new ConnectivityResult(false, "缺少账户凭证");
            }
            ApiResponse<AccountInformationV3Response> account = signedRead(credential, RestRequestScheduler.Kind.ACCOUNT,
                client -> client.accountInformationV3(RECV_WINDOW_MILLIS));
            ApiResponse<FuturesAccountConfigurationResponse> configuration = signedRead(credential, RestRequestScheduler.Kind.ACCOUNT,
                client -> client.futuresAccountConfiguration(RECV_WINDOW_MILLIS));
            if (account.getData() == null || configuration.getData() == null) {
                return new ConnectivityResult(false, "Binance 凭证校验无响应");
            }
            return tradingPermissions(configuration.getData().getCanTrade(), configuration.getData().getCanWithdraw());
        } catch (RuntimeException ex) {
            return new ConnectivityResult(false, "Binance 连接失败");
        }
    }

    /**
     * 查询交易所交易规则和交易对元数据。
     */
    @Override
    public ExchangeInfo queryExchangeInfo() {
        return queryExchangeInfo(null);
    }

    /**
     * 查询交易对的精度、过滤器和下单规则。
     */
    @Override
    public synchronized SymbolRule queryTradingRule(Credential credential, String symbol) {
        String environment = credential != null && "TESTNET".equalsIgnoreCase(credential.runMode()) ? "TESTNET" : "LIVE";
        TradingRules cached = tradingRules.get(environment);
        if (cached == null || cached.expiresAt() <= System.currentTimeMillis()) {
            cached = new TradingRules(System.currentTimeMillis() + 60_000L, queryExchangeInfo(credential));
            tradingRules.put(environment, cached);
        }
        return cached.info().symbols().stream().filter(rule -> rule.symbol().equalsIgnoreCase(symbol)).findFirst().orElseThrow(() -> new IllegalArgumentException(
            "实际交易环境中没有有效交易规则: " + symbol));
    }

    private ExchangeInfo queryExchangeInfo(Credential credential) {
        try {
            ApiResponse<ExchangeInformationResponse> response = api(credential).exchangeInformation();
            List<SymbolRule> rules = new ArrayList<>();
            for (ExchangeInformationResponseSymbolsInner symbol : response.getData().getSymbols()) {
                if (!"TRADING".equals(symbol.getStatus())) {
                    continue;
                }
                BigDecimal priceTick = BigDecimal.ZERO;
                BigDecimal step = BigDecimal.ZERO;
                BigDecimal marketStep = BigDecimal.ZERO;
                BigDecimal minNotional = BigDecimal.ZERO;
                if (symbol.getFilters() == null) {
                    continue;
                }
                for (var filter : symbol.getFilters()) {
                    if ("PRICE_FILTER".equals(filter.getFilterType())) {
                        priceTick = decimal(filter.getTickSize());
                    }
                    if ("LOT_SIZE".equals(filter.getFilterType())) {
                        step = decimal(filter.getStepSize());
                    }
                    if ("MARKET_LOT_SIZE".equals(filter.getFilterType())) {
                        marketStep = decimal(filter.getStepSize());
                    }
                    if ("MIN_NOTIONAL".equals(filter.getFilterType()) || "NOTIONAL".equals(filter.getFilterType())) {
                        minNotional = decimal(filter.getNotional());
                    }
                }
                if (marketStep.signum() <= 0) {
                    marketStep = step;
                }
                if (priceTick.signum() <= 0 || step.signum() <= 0 || marketStep.signum() <= 0 || minNotional.signum() <= 0) {
                    continue;
                }
                rules.add(new SymbolRule(symbol.getSymbol(), symbol.getQuoteAsset(), symbol.getPricePrecision().intValue(), symbol.getQuantityPrecision().intValue(), priceTick,
                    step, marketStep, minNotional, symbol.getStatus()));
            }
            return new ExchangeInfo(rules);
        } catch (RuntimeException ex) {
            throw requestFailure("QUERY_TRADING_RULES", ex);
        }
    }

    /**
     * 查询历史 K 线。
     */
    @Override
    public List<Kline> queryKlines(String symbol, String interval, int limit) {
        return queryKlinesFrom(symbol, interval, null, limit);
    }

    /**
     * 从开盘时间补取 LIVE 环境的历史 K 线。
     */
    @Override
    public List<Kline> queryKlinesFrom(String symbol, String interval, Instant from, int limit) {
        return queryKlinesFrom("LIVE", symbol, interval, from, limit);
    }

    /**
     * 按指定交易环境补取历史 K 线，不跨环境回退。
     */
    @Override
    public List<Kline> queryKlinesFrom(String runMode, String symbol, String interval, Instant from, int limit) {
        try {
            var sdkInterval = Interval.fromValue(interval);
            ApiResponse<KlineCandlestickDataResponse> response = marketApi(runMode).klineCandlestickData(symbol.trim(), sdkInterval, from == null ? null : from.toEpochMilli(),
                null, (long) Math.min(1000, limit));
            List<Kline> result = new ArrayList<>(response.getData().size());
            long now = System.currentTimeMillis();
            for (var row : response.getData()) {
                Instant openTime = Instant.ofEpochMilli(Long.parseLong(row.get(0)));
                Instant closeTime = Instant.ofEpochMilli(Long.parseLong(row.get(6)));
                result.add(new Kline(openTime, closeTime, decimal(row.get(1)), decimal(row.get(2)), decimal(row.get(3)), decimal(row.get(4)), decimal(row.get(5)),
                    closeTime.toEpochMilli() <= now));
            }
            return result;
        } catch (RuntimeException ex) {
            if (isInvalidSymbolError(ex)) {
                throw new ExchangeMarketPort.UnsupportedMarketSymbolException(runMode, symbol.trim().toUpperCase(Locale.ROOT));
            }
            throw requestFailure("QUERY_KLINES", ex);
        }
    }

    /**
     * 按聚合成交 ID 补取 LIVE 环境的行情事件。
     */
    @Override
    public List<MarketEvent> queryAggregateTradesFrom(String symbol, String interval, long fromId, int limit) {
        return queryAggregateTradesFrom("LIVE", symbol, interval, fromId, limit);
    }

    /** Validate proxy before switching any active LIVE streams. */

    /**
     * 在指定环境按成交 ID 补偿历史行情缺口。
     */
    @Override
    public List<MarketEvent> queryAggregateTradesFrom(String runMode, String symbol, String interval, long fromId, int limit) {
        var data = marketApi(runMode).compressedAggregateTradesList(symbol, fromId, null, null, (long) Math.min(1000, limit)).getData();
        if (data == null) {
            return List.of();
        }
        return data.stream().map(item -> new MarketEvent("TRADE", symbol, interval, decimal(item.getpLowerCase()), null, instant(item.getT()), item.getaLowerCase(),
            Instant.now(), true)).toList();
    }

    /**
     * 查询交易对最新价格。
     */
    @Override
    public PriceSnapshot queryPrice(String symbol) {
        return queryPrice("LIVE", symbol);
    }

    /**
     * 查询指定环境的最新标记价格，并返回带来源时间的快照。
     */
    @Override
    public PriceSnapshot queryPrice(String runMode, String symbol) {
        try {
            String normalizedSymbol = symbol.trim();
            JsonObject trade = publicJson(runMode, "/fapi/v2/ticker/price", normalizedSymbol);
            JsonObject mark = publicJson(runMode, "/fapi/v1/premiumIndex", normalizedSymbol);
            return new PriceSnapshot(normalizedSymbol, jsonDecimal(trade, "price"), jsonDecimal(mark, "markPrice"), Instant.now());
        } catch (RuntimeException ex) {
            throw new IllegalStateException("查询 Binance 实时价格失败", ex);
        }
    }

    /**
     * SDK 12.1.1 的 ticker/mark oneOf 解码会返回 actualInstance=null；保留 SDK 的请求、代理和超时配置，按原始 JSON 读取。
     */
    private JsonObject publicJson(String runMode, String path, String symbol) {
        ClientConfiguration config = restConfiguration(marketBaseUrl(runMode));
        ApiClient client = new ApiClient(config);
        List<Pair> query = new ArrayList<>(client.parameterToPair("symbol", symbol));
        okhttp3.Call call = client.buildCall(null, path, "GET", query, new ArrayList<>(), null, new HashMap<>(Map.of("Accept", "application/json")), new HashMap<>(),
            new HashMap<>(), new HashSet<>());
        ApiResponse<JsonObject> response = client.execute(call, JsonObject.class);
        JsonObject data = response.getData();
        if (data == null) {
            throw new IllegalStateException("Binance 公开行情响应为空: " + path);
        }
        return data;
    }

    /**
     * 订阅交易所公开行情流。
     */
    @Override
    public MarketStreamSession subscribeMarket(String symbol, String interval, Consumer<MarketEvent> listener) {
        return subscribeMarket("LIVE", symbol, interval, listener);
    }

    /**
     * 订阅指定环境的公开行情，并封装 SDK 流生命周期。
     */
    @Override
    public MarketStreamSession subscribeMarket(String runMode, String symbol, String interval, Consumer<MarketEvent> listener) {
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        String streamSymbol = normalized.toLowerCase(Locale.ROOT);
        String base = "TESTNET".equalsIgnoreCase(runMode) ? properties.getBinance().getTestnetMarketStreamUrl() : properties.getBinance().getMarketStreamUrl();
        Consumer<String> events = text -> {
            MarketEvent event = parseMarketEvent(text, normalized, interval);
            if (event != null) {
                listener.accept(event);
            }
        };
        Runnable disconnected = () -> listener.accept(new MarketEvent("DISCONNECTED", normalized, interval, null, null, Instant.now()));
        if (sdkStreamsEnabled()) {
            requireSdkStreamsCanConnect();
            try {
                BinanceSdkStreamTransport sdk = BinanceSdkStreamTransport.market(base, streamSymbol, interval, events, disconnected,
                    properties.getBinance().getSdkStreamBacklogLimit(), properties.getProxy());
                StreamHandle managed = trackWebsocket(sdk::close, disconnected);
                return new MarketStreamSession(managed::close);
            } catch (RuntimeException sdkFailure) {
                // A broken SDK/Jetty listener must not trap every market stream in an endless retry loop.
                // Downgrade this node to the independently implemented OkHttp transport and let hubs replay.
                fallbackToRawWebsocketTransport(sdkFailure);
            }
        }
        String url = base + "?streams=" + streamSymbol + "@aggTrade/" + streamSymbol + "@markPrice@1s/" + streamSymbol + "@kline_" + interval;
        SocketConnection socket = new SocketConnection(url, events, disconnected);
        StreamHandle managed = trackWebsocket(socket::close, disconnected);
        return new MarketStreamSession(managed::close);
    }

    /**
     * New subscriptions use this transport; existing subscriptions change only after safe reconnect.
     */
    boolean sdkStreamsEnabled() {
        return "BINANCE_SDK".equals(currentWebsocketTransport.get());
    }

    /**
     * 启动 SDK WebSocket 前检查代理和网络配置是否满足连接条件。
     */
    private void requireSdkStreamsCanConnect() {
        QuantProperties.Proxy proxy = properties.getProxy();
        if (proxy == null) {
            return;
        }
        boolean hasHost = proxy.getHost() != null && !proxy.getHost().isBlank();
        if (hasHost && (proxy.getPort() == null || proxy.getPort() < 1 || proxy.getPort() > 65535)) {
            throw new IllegalArgumentException("Binance SDK Streams HTTP proxy port is invalid");
        }
        if (!hasHost && proxy.getPort() != null) {
            throw new IllegalArgumentException("Binance SDK Streams HTTP proxy host is missing");
        }
    }

    /**
     * Node-local current mode; cluster deployments must switch every trading node.
     */
    public String websocketTransport() {
        return currentWebsocketTransport.get();
    }

    /**
     * Atomically choose the transport for future subscriptions and force old sessions to reconnect.
     * The market and account stream hubs receive DISCONNECTED and perform existing recovery/replay.
     * This is NOT a zero-gap migration; operators must verify LIVE protections and user-data health.
     *
     * @return number of sessions instructed to reconnect
     */
    public synchronized int switchWebsocketTransport(String target) {
        String normalized = validateWebsocketTransport(target);
        // Reject an unsupported SDK/proxy combination BEFORE switching the node
        // transport or notifying existing sockets. Otherwise all LIVE market and
        // private streams disconnect and enter a permanent reconnect failure loop.
        if ("BINANCE_SDK".equals(normalized)) {
            requireSdkStreamsCanConnect();
        }
        if (normalized.equals(currentWebsocketTransport.get())) {
            return 0;
        }
        currentWebsocketTransport.set(normalized);
        int count = 0;
        for (StreamHandle stream : java.util.List.copyOf(activeWebsocketStreams)) {
            stream.reconnect();
            count++;
        }
        log.warn("Binance WebSocket transport changed to {} on this node, reconnecting {} streams", normalized, count);
        return count;
    }

    /**
     * Fail over the node-wide stream transport after an SDK connection/bootstrap failure.
     * Existing hubs are notified through their normal reconnect callback; the failing subscription
     * continues below through RAW_OKHTTP. This is a transport failover, not permission to trade
     * on stale data: MarketStreamHub still owns freshness checks and REST replay.
     */
    private synchronized void fallbackToRawWebsocketTransport(RuntimeException cause) {
        if (!"BINANCE_SDK".equals(currentWebsocketTransport.get())) {
            return;
        }
        currentWebsocketTransport.set("RAW_OKHTTP");
        int reconnecting = 0;
        for (StreamHandle stream : java.util.List.copyOf(activeWebsocketStreams)) {
            stream.reconnect();
            reconnecting++;
        }
        log.error("Binance SDK WebSocket bootstrap failed; switched this node to RAW_OKHTTP and requested reconnect for {} existing streams; errorType={}",
            reconnecting, cause.getClass().getSimpleName());
    }

    private StreamHandle trackWebsocket(Runnable close, Runnable notifyDisconnected) {
        StreamHandle handle = new StreamHandle(close, notifyDisconnected);
        activeWebsocketStreams.add(handle);
        return handle;
    }

    /**
     * Original protocol transport: deterministic fail/replay path when SDK stream backlog is unsuitable.
     */
    private MarketEvent parseMarketEvent(String text, String symbol, String interval) {
        JsonObject root = gson.fromJson(text, JsonObject.class);
        JsonObject data = root.has("data") ? root.getAsJsonObject("data") : root;
        if (!data.has("e")) {
            return null;
        }
        if (!data.has("s") || !symbol.equalsIgnoreCase(data.get("s").getAsString())) {
            throw new IllegalStateException("Binance行情交易对与订阅不一致");
        }
        Instant time = Instant.ofEpochMilli(data.get("E").getAsLong());
        return switch (data.get("e").getAsString()) {
            case "aggTrade" -> new MarketEvent("TRADE", symbol, interval, data.get("p").getAsBigDecimal(), null, time, data.get("a").getAsLong(), Instant.now(), false);
            case "markPriceUpdate" -> new MarketEvent("MARK", symbol, interval, data.get("p").getAsBigDecimal(), null, time);
            case "kline" -> {
                JsonObject k = data.getAsJsonObject("k");
                if (!interval.equals(k.get("i").getAsString()) || !symbol.equalsIgnoreCase(k.get("s").getAsString())) {
                    throw new IllegalStateException("Binance K线交易对或周期与订阅不一致");
                }
                Kline candle = new Kline(Instant.ofEpochMilli(k.get("t").getAsLong()), Instant.ofEpochMilli(k.get("T").getAsLong()), k.get("o").getAsBigDecimal(),
                    k.get("h").getAsBigDecimal(), k.get("l").getAsBigDecimal(), k.get("c").getAsBigDecimal(), k.get("v").getAsBigDecimal(), k.get("x").getAsBoolean());
                yield new MarketEvent("KLINE", symbol, interval, null, candle, time);
            }
            default -> null;
        };
    }

    /**
     * 查询交易账户资产信息。
     */
    @Override
    public AccountSnapshot queryAccount(Credential credential) {
        var result = new java.util.concurrent.CompletableFuture<AccountSnapshot>();
        var running = accountQueries.putIfAbsent(credential, result);
        if (running != null) {
            try {
                return running.get(15, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new org.dromara.quant.exchange.ExchangeMarketPort.RequestDeferredException();
            } catch (java.util.concurrent.TimeoutException ex) {
                throw new org.dromara.quant.exchange.ExchangeMarketPort.RequestDeferredException();
            } catch (java.util.concurrent.ExecutionException ex) {
                if (ex.getCause() instanceof RuntimeException error) {
                    throw error;
                }
                throw new IllegalStateException("账户查询失败");
            }
        }
        try {
            AccountSnapshot snapshot = queryAccountFresh(credential);
            result.complete(snapshot);
            return snapshot;
        } catch (RuntimeException ex) {
            result.completeExceptionally(ex);
            throw ex;
        } finally {
            accountQueries.remove(credential, result);
        }
    }

    /**
     * 直接从交易所刷新账户余额与持仓事实，不使用本地缓存。
     */
    @Override
    public AccountSnapshot queryAccountFresh(Credential credential) {
        try {
            AccountInformationV3Response data = signedRead(credential, RestRequestScheduler.Kind.ACCOUNT,
                client -> client.accountInformationV3(RECV_WINDOW_MILLIS).getData());
            return new AccountSnapshot(decimal(data.getTotalWalletBalance()), decimal(data.getAvailableBalance()), decimal(data.getTotalMarginBalance()),
                decimal(data.getTotalUnrealizedProfit()));
        } catch (RuntimeException ex) {
            throw requestFailure("QUERY_ACCOUNT", ex);
        }
    }

    /**
     * 查询交易所收入/财务流水。
     */
    @Override
    public List<IncomeSnapshot> queryIncomeHistory(Credential credential, String symbol, String incomeType, TimeRange range, Long limit) {
        requireCredential(credential);
        Long start = range == null || range.from() == null ? null : range.from().toEpochMilli();
        Long end = range == null || range.to() == null ? null : range.to().toEpochMilli();
        IncomeType type = incomeType == null || incomeType.isBlank() ? null : IncomeType.fromValue(incomeType);
        GetIncomeHistoryResponse data = signedRead(credential, RestRequestScheduler.Kind.BACKGROUND,
            client -> client.getIncomeHistory(symbol, type, start, end, null, limit == null ? 1000L : limit, RECV_WINDOW_MILLIS).getData());
        if (data == null) {
            throw new IllegalStateException("Binance income-history response has no data payload; completeness cannot be proven");
        }
        List<IncomeSnapshot> result = new ArrayList<>();
        for (GetIncomeHistoryResponseInner item : data) {
            if (item == null) {
                // Preserve a malformed/null row for the strict collector to reject with diagnostics.
                result.add(null);
                continue;
            }
            // Income history is financial evidence: do not turn absent fields into plausible defaults.
            // decimal() maps null to zero and instant() maps null to epoch for other API paths,
            // so this mapping must explicitly preserve nulls for strict reconciliation.
            BigDecimal amount = item.getIncome() == null ? null : decimal(item.getIncome());
            Instant eventTime = item.getTime() == null ? null : Instant.ofEpochMilli(item.getTime());
            result.add(new IncomeSnapshot(item.getSymbol(), string(item, "getIncomeType"), amount, item.getAsset(), item.getTranId(), item.getInfo(),
                eventTime));
        }
        return result;
    }

    /**
     * 查询账户持仓模式。
     */
    @Override
    public PositionMode queryPositionMode(Credential credential) {
        requireCredential(credential);
        var data = signedRead(credential, RestRequestScheduler.Kind.ACCOUNT,
            client -> client.getCurrentPositionMode(RECV_WINDOW_MILLIS).getData());
        return new PositionMode(Boolean.TRUE.equals(data.getDualSidePosition()));
    }

    /**
     * 设置账户持仓模式。
     */
    @Override
    public void setPositionMode(Credential credential, boolean hedgeMode) {
        requireCredential(credential);
        ChangePositionModeRequest request = new ChangePositionModeRequest();
        request.setDualSidePosition(hedgeMode ? "TRUE" : "FALSE");
        try {
            api(credential, true).changePositionMode(request);
        } catch (RuntimeException ex) {
            String message = ex.getMessage();
            if (message != null && (message.contains("No need to change") || message.contains("-4059"))) {
                return;
            }
            throw requestFailure("SET_POSITION_MODE", ex);
        }
    }

    /**
     * 按请求设置合约保证金模式。
     */
    @Override
    public void setMarginType(Credential credential, String symbol, String marginType) {
        requireCredential(credential);
        ChangeMarginTypeRequest request = new ChangeMarginTypeRequest();
        request.setSymbol(symbol);
        request.setMarginType("ISOLATED".equalsIgnoreCase(marginType) ? MarginType.ISOLATED : MarginType.CROSSED);
        try {
            api(credential, true).changeMarginType(request);
        } catch (RuntimeException ex) {
            com.binance.connector.client.common.ApiException apiError = apiError(ex);
            if (apiError != null && apiError.getCode() == 400 && Integer.valueOf(-4046).equals(errorCode(apiError))) {
                return;
            }
            throw requestFailure("SET_MARGIN_TYPE", ex);
        }
    }

    /**
     * 设置交易对杠杆。
     */
    @Override
    public void setLeverage(Credential credential, String symbol, int leverage) {
        requireCredential(credential);
        ChangeInitialLeverageRequest request = new ChangeInitialLeverageRequest();
        request.setSymbol(symbol);
        request.setLeverage((long) leverage);
        try {
            api(credential, true).changeInitialLeverage(request);
        } catch (RuntimeException ex) {
            throw requestFailure("SET_LEVERAGE", ex);
        }
    }

    /**
     * Read Binance positionRisk V2 without filtering out zero-quantity rows.
     * A single zero-quantity row can carry active account-symbol leverage and
     * isolated/cross mode; account aggregates or cached open positions cannot.
     * <p>
     * Every returned LONG/SHORT/BOTH row must agree. Missing getters, empty
     * data or conflicting facts fail closed rather than acknowledging settings.
     */
    @Override
    public org.dromara.quant.exchange.ExchangeAccountPort.SymbolTradingSettings querySymbolTradingSettings(Credential credential, String symbol) {
        requireCredential(credential);
        if (symbol == null || !symbol.matches("[a-zA-Z0-9]{2,32}")) {
            throw new IllegalArgumentException("Binance 交易设置回查缺少有效交易对");
        }
        var rows = signedRead(credential, RestRequestScheduler.Kind.ACCOUNT,
            client -> client.positionInformationV2(symbol.trim().toUpperCase(Locale.ROOT), RECV_WINDOW_MILLIS).getData());
        if (rows == null || rows.isEmpty()) {
            throw new IllegalStateException("Binance 未返回可核实的交易对保证金/杠杆配置");
        }
        String expectedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
        return parseSymbolTradingSettings(expectedSymbol, rows);
    }

    /**
     * 查询交易所当前持仓。
     */
    @Override
    public List<PositionSnapshot> queryPositions(Credential credential, String symbol) {
        return signedRead(credential, RestRequestScheduler.Kind.ACCOUNT, client -> queryPositions(client, symbol));
    }

    private List<PositionSnapshot> queryPositions(DerivativesTradingUsdsFuturesRestApi client, String symbol) {
        // A successful positionInformationV3 response is a complete exchange fact. This adapter
        // intentionally filters zero-quantity rows, so an empty list means the requested symbol
        // (or the whole account when symbol == null) was successfully verified flat. Transport,
        // rate-limit, decoding, and API errors must throw instead of being converted to empty.
        var response = client.positionInformationV3(symbol == null ? null : symbol.trim(), RECV_WINDOW_MILLIS).getData();
        var positions =
            response.stream().filter(item -> symbol == null || symbol.equalsIgnoreCase(item.getSymbol())).filter(item -> decimal(item.getPositionAmt()).signum() != 0).collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        if (positions.isEmpty()) {
            return List.of();
        }
        var leverageByPosition =
            client.positionInformationV2(symbol == null ? null : symbol.trim(), RECV_WINDOW_MILLIS).getData().stream().collect(java.util.stream.Collectors.toMap(item -> item.getSymbol() + ":" + item.getPositionSide(), item -> Integer.valueOf(item.getLeverage()), (first, second) -> first));
        return positions.stream().map(item -> new PositionSnapshot(item.getSymbol(), item.getPositionSide(), decimal(item.getPositionAmt()), decimal(item.getEntryPrice()),
            decimal(item.getMarkPrice()), decimal(item.getUnRealizedProfit()), decimal(item.getLiquidationPrice()), decimal(item.getPositionInitialMargin()),
            leverageByPosition.get(item.getSymbol() + ":" + item.getPositionSide()), null, null,
            item.getUpdateTime() == null ? null : Instant.ofEpochMilli(item.getUpdateTime()))).toList();
    }

    /**
     * 对只读签名请求遇到 Binance 时间戳拒绝时，刷新服务端偏移并最多重试一次。
     * 写操作不经过此方法，避免请求结果未知时重复提交交易或撤单。
     */
    private <T> T signedRead(Credential credential, RestRequestScheduler.Kind kind, Function<DerivativesTradingUsdsFuturesRestApi, T> request) {
        DerivativesTradingUsdsFuturesRestApi client = api(credential, kind);
        String runMode = credential == null ? "LIVE" : credential.runMode();
        String baseUrl = "TESTNET".equalsIgnoreCase(runMode) ? properties.getBinance().getTestnetUrl() : properties.getBinance().getBaseUrl();
        ClockOffset usedOffset = clockOffsets.get(baseUrl);
        long usedOffsetUpdatedAt = usedOffset == null ? Long.MIN_VALUE : usedOffset.updatedAt();
        try {
            return request.apply(client);
        } catch (RuntimeException ex) {
            var error = apiError(ex);
            if (error == null || !Integer.valueOf(-1021).equals(errorCode(error))) {
                throw ex;
            }
            refreshClockOffset(baseUrl, usedOffsetUpdatedAt);
            log.warn("Binance 只读请求因时间戳被拒绝，已刷新服务器时间并重试一次: kind={}, runMode={}, binanceCode=-1021", kind, runMode);
            return request.apply(api(credential, kind));
        }
    }

    /**
     * 查询活动订单。
     */
    @Override
    public List<OrderSnapshot> queryOrders(Credential credential, String symbol) {
        return signedRead(credential, RestRequestScheduler.Kind.ACCOUNT, client -> {
            ApiResponse<CurrentAllOpenOrdersResponse> response =
                client.currentAllOpenOrders(symbol == null ? null : symbol.trim(), RECV_WINDOW_MILLIS);
            if (response == null || response.getData() == null) {
                throw new IllegalStateException("Binance 活动订单响应缺少数据载荷，无法确认查询完整性");
            }
            return response.getData().stream().map(item -> new OrderSnapshot(item.getSymbol(), item.getOrderId(), item.getClientOrderId(), item.getSide(), item.getType(),
                item.getStatus(), decimal(item.getPrice()), decimal(item.getOrigQty()), decimal(item.getExecutedQty()), instant(item.getUpdateTime()), decimal(string(item,
                "getAvgPrice")), string(item, "getPositionSide"), nullableBoolean(item, "getReduceOnly"))).toList();
        });
    }

    /**
     * 查询指定订单。
     */
    @Override
    public OrderSnapshot queryOrder(Credential credential, String symbol, Long orderId, String clientOrderId) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("查询订单必须指定交易对");
        }
        if (orderId == null && (clientOrderId == null || clientOrderId.isBlank())) {
            throw new IllegalArgumentException("查询订单必须指定 orderId 或 clientOrderId");
        }
        if ("PAPER".equalsIgnoreCase(credential.runMode())) {
            return null;
        }
        Object data;
        try {
            data = signedRead(credential, RestRequestScheduler.Kind.ACCOUNT,
                client -> client.queryOrder(symbol.trim(), orderId, clientOrderId, RECV_WINDOW_MILLIS).getData());
        } catch (RuntimeException ex) {
            var error = apiError(ex);
            if (error != null && error.getCode() == 400 && Integer.valueOf(-2013).equals(errorCode(error))) {
                return null;
            }
            throw requestFailure("QUERY_ORDER", ex);
        }
        if (data == null) {
            return null;
        }
        // Do not synthesize exchange identity from query arguments. Recovery and
        // dispatch-guard release must be based on identity fields actually returned
        // by Binance, not on the fact that a query was issued with those identifiers.
        Long actualOrderId = number(data, "getOrderId");
        String actualClientOrderId = string(data, "getClientOrderId");
        String actualSymbol = string(data, "getSymbol");
        if (!hasExactOrderIdentity(symbol, orderId, clientOrderId, actualOrderId, actualClientOrderId, actualSymbol)) {
            throw new IllegalStateException("Binance 精确订单查询缺少或返回不匹配的订单身份，保持派发恢复阻断");
        }
        String status = string(data, "getStatus");
        if (status == null || status.isBlank()) {
            status = "UNKNOWN";
        }
        String type = string(data, "getType");
        if (type == null || type.isBlank()) {
            type = "UNKNOWN";
        }
        return new OrderSnapshot(actualSymbol, actualOrderId, actualClientOrderId, string(data, "getSide"), type, status, decimal(string(data, "getPrice")), decimal(string(data,
            "getOrigQty")), decimal(string(data, "getExecutedQty")), instant(number(data, "getUpdateTime")), decimal(string(data, "getAvgPrice")), string(data, "getPositionSide"),
            nullableBoolean(data, "getReduceOnly"), instant(number(data, "getTime")));
    }

    /**
     * 分页查询账户成交。
     */
    @Override
    public List<TradeSnapshot> queryTrades(Credential credential, String symbol, TimeRange range) {
//        if (symbol == null || symbol.isBlank()) {
//            throw new IllegalArgumentException("查询成交记录必须指定交易对");
//        }
        Long from = range == null || range.from() == null ? null : range.from().toEpochMilli();
        Long to = range == null || range.to() == null ? null : range.to().toEpochMilli();
        return signedRead(credential, RestRequestScheduler.Kind.ACCOUNT, client -> {
            ApiResponse<AccountTradeListResponse> response = client.accountTradeList(symbol.trim(), null, from, to, null, 1000L, RECV_WINDOW_MILLIS);
            if (response == null || response.getData() == null) {
                throw new IllegalStateException("Binance 历史成交响应缺少数据载荷，无法确认查询完整性");
            }
            return response.getData().stream().map(item -> new TradeSnapshot(item.getSymbol(), item.getId(), item.getOrderId(), item.getSide(), decimal(item.getQty()),
                decimal(item.getPrice()), decimal(item.getCommission()), decimal(item.getRealizedPnl()), instant(item.getTime()), string(item, "getCommissionAsset"))).toList();
        });
    }

    /**
     * 按交易对、订单 ID 和订单生命周期时间范围查询成交。
     */
    @Override
    public List<TradeSnapshot> queryTradesByOrder(Credential credential, String symbol, Long orderId, TimeRange range, Long limit) {
        requireCredential(credential);
        if (symbol == null || symbol.isBlank() || orderId == null || orderId <= 0
            || range == null || range.from() == null || range.to() == null || range.from().isAfter(range.to())) {
            throw new IllegalArgumentException("按订单查询成交必须指定交易对、订单 ID 和有效时间范围");
        }
        return signedRead(credential, RestRequestScheduler.Kind.ACCOUNT, client -> {
            ApiResponse<AccountTradeListResponse> response = client.accountTradeList(
                symbol.trim(), orderId, range.from().toEpochMilli(), range.to().toEpochMilli(), null,
                limit == null ? 1000L : limit, RECV_WINDOW_MILLIS);
            if (response == null || response.getData() == null) {
                throw new IllegalStateException("Binance 指定订单成交响应缺少数据载荷，无法确认查询完整性");
            }
            return response.getData().stream().map(item -> new TradeSnapshot(item.getSymbol(), item.getId(), item.getOrderId(), item.getSide(),
                decimal(item.getQty()), decimal(item.getPrice()), decimal(item.getCommission()), decimal(item.getRealizedPnl()),
                instant(item.getTime()), string(item, "getCommissionAsset"))).toList();
        });
    }

    /**
     * 查询历史订单。
     */
    @Override
    public List<OrderSnapshot> queryOrderHistory(Credential credential, String symbol, TimeRange range, Long limit) {
        requireCredential(credential);
        System.out.println(symbol + "   "  + range.from() + "   ***********************   " + range.to());
        Long start = range == null || range.from() == null ? null : range.from().toEpochMilli();
        Long end = range == null || range.to() == null ? null : range.to().toEpochMilli();
        String querySymbol = symbol == null ? "" : symbol.trim();
        Long orderId = querySymbol.isEmpty() ? 0L : null;
        var data = signedRead(credential, RestRequestScheduler.Kind.BACKGROUND,
            client -> client.allOrders(querySymbol, orderId, start, end, limit == null ? 1000L : limit, RECV_WINDOW_MILLIS).getData());
        if (data == null) {
            throw new IllegalStateException("Binance 历史订单响应缺少数据载荷，无法确认查询完整性");
        }
        return data.stream().map(item -> new OrderSnapshot(item.getSymbol(), item.getOrderId(), item.getClientOrderId(), item.getSide(), item.getType(), item.getStatus(),
            decimal(item.getPrice()), decimal(item.getOrigQty()), decimal(item.getExecutedQty()), instant(item.getUpdateTime()), decimal(string(item, "getAvgPrice")),
            string(item, "getPositionSide"), nullableBoolean(item, "getReduceOnly"), instant(number(item, "getTime")))).toList();
    }

    /**
     * 查询交易所条件/保护订单。
     */
    @Override
    public List<ProtectionOrderSnapshot> queryProtectionOrders(Credential credential, String symbol) {
        requireCredential(credential);
        var data = signedRead(credential, RestRequestScheduler.Kind.ACCOUNT,
            client -> client.currentAllAlgoOpenOrders("CONDITIONAL", symbol, null, RECV_WINDOW_MILLIS).getData());
        if (data == null) {
            return List.of();
        }
        return data.stream().map(item -> new ProtectionOrderSnapshot(string(item, "getSymbol"), number(item, "getAlgoId"), string(item, "getClientAlgoId"), normalizeAlgoStatus(string(item,
            "getAlgoStatus")), string(item, "getSide"), string(item, "getPositionSide"), string(item, "getOrderType"), decimal(string(item, "getQuantity")), decimal(string(item,
            "getTriggerPrice")), instant(number(item, "getUpdateTime")), null, Boolean.valueOf(string(item, "getReduceOnly")), Boolean.valueOf(string(item, "getClosePosition")))).filter(item -> symbol == null || symbol.equalsIgnoreCase(item.symbol())).toList();
    }

    /**
     * Verify order identity using only fields returned by the exchange; never
     * fill missing response fields from the query arguments.
     */
    static boolean hasExactOrderIdentity(String requestedSymbol, Long requestedOrderId, String requestedClientOrderId,
                                         Long actualOrderId, String actualClientOrderId, String actualSymbol) {
        return requestedSymbol != null && !requestedSymbol.isBlank()
            && actualOrderId != null && actualOrderId > 0
            && actualClientOrderId != null && !actualClientOrderId.isBlank()
            && actualSymbol != null && !actualSymbol.isBlank()
            && requestedSymbol.trim().equalsIgnoreCase(actualSymbol)
            && (requestedOrderId == null || requestedOrderId.equals(actualOrderId))
            && (requestedClientOrderId == null || requestedClientOrderId.equals(actualClientOrderId));
    }

    /**
     * 按交易所订单 ID 或客户端 ID 精确查询保护单状态。
     */
    @Override
    public ProtectionOrderSnapshot queryProtectionOrder(Credential credential, Long orderId, String clientOrderId, String symbol) {
        requireCredential(credential);
        com.binance.connector.client.derivatives_trading_usds_futures.rest.model.QueryAlgoOrderResponse data;
        try {
            data = signedRead(credential, RestRequestScheduler.Kind.ACCOUNT,
                client -> client.queryAlgoOrder(orderId, clientOrderId, RECV_WINDOW_MILLIS).getData());
        } catch (com.binance.connector.client.common.ApiException ex) {
            if (!isProtectionOrderAbsent(ex)) {
                throw ex;
            }
            // Binance 的精确查询可能已不返回已结束的 Algo 单；仅在明确 -2013 时查近 7 天历史，
            // 必须按 clientAlgoId 和 symbol 精确匹配。历史查询失败或未匹配仍返回未知，由上层保持阻断。
            return queryProtectionOrderHistory(credential, orderId, clientOrderId, symbol);
        }
        if (data == null) {
            // 成功响应也可能没有 data；先查有限时间范围的算法单历史，再由上层决定是否仍未知。
            return queryProtectionOrderHistory(credential, orderId, clientOrderId, symbol);
        }
        String status = normalizeAlgoStatus(string(data, "getAlgoStatus"));
        return new ProtectionOrderSnapshot(string(data, "getSymbol"), number(data, "getAlgoId"), string(data, "getClientAlgoId"), status, string(data, "getSide"), string(data,
            "getPositionSide"), string(data, "getOrderType"), decimal(string(data, "getQuantity")), decimal(string(data, "getTriggerPrice")),
            instant(number(data, "getUpdateTime")), protectionExecutedQuantity(status, decimal(string(data, "getActualQty"))), null, null, number(data, "getActualOrderId"),
            decimal(string(data, "getActualPrice")));
    }

    private ProtectionOrderSnapshot queryProtectionOrderHistory(Credential credential, Long orderId, String clientOrderId, String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        long endTime = System.currentTimeMillis();
        long startTime = endTime - java.time.Duration.ofDays(7).toMillis() + 1;
        if (orderId != null && orderId > 0) {
            var response = signedRead(credential, RestRequestScheduler.Kind.ACCOUNT,
                client -> client.queryAllAlgoOrders(symbol.trim().toUpperCase(Locale.ROOT), orderId, startTime, endTime,
                    (long) ALGO_HISTORY_PAGE_SIZE, RECV_WINDOW_MILLIS));
            var data = response == null ? null : response.getData();
            ProtectionOrderSnapshot exact = findExactAlgoHistory(data, symbol, orderId, clientOrderId);
            if (exact != null) {
                return exact;
            }
        }
        AlgoHistoryResult result = collectCompleteAlgoHistory(startTime, endTime, (from, to) -> {
            var response = signedRead(credential, RestRequestScheduler.Kind.ACCOUNT,
                client -> client.queryAllAlgoOrders(symbol.trim().toUpperCase(Locale.ROOT), null, from, to,
                    (long) ALGO_HISTORY_PAGE_SIZE, RECV_WINDOW_MILLIS));
            return response == null ? null : response.getData();
        });
        // A full page at one millisecond resolution may still be truncated.
        // In that case absence and uniqueness are unknowable, so stay fail closed.
        return result.complete() ? findExactAlgoHistory(result.orders(), symbol, orderId, clientOrderId) : null;
    }

    static AlgoHistoryResult collectCompleteAlgoHistory(long startTime, long endTime,
                                                         java.util.function.BiFunction<Long, Long,
                                                             List<com.binance.connector.client.derivatives_trading_usds_futures.rest.model.QueryAllAlgoOrdersResponseInner>> query) {
        if (startTime < 0 || endTime < startTime || query == null) {
            return new AlgoHistoryResult(List.of(), false);
        }
        List<com.binance.connector.client.derivatives_trading_usds_futures.rest.model.QueryAllAlgoOrdersResponseInner> orders = new ArrayList<>();
        int[] queryCount = {0};
        boolean complete = collectAlgoHistoryRange(startTime, endTime, query, orders, queryCount);
        return new AlgoHistoryResult(complete ? List.copyOf(orders) : List.of(), complete);
    }

    private static boolean collectAlgoHistoryRange(long startTime, long endTime,
                                                    java.util.function.BiFunction<Long, Long,
                                                        List<com.binance.connector.client.derivatives_trading_usds_futures.rest.model.QueryAllAlgoOrdersResponseInner>> query,
                                                    List<com.binance.connector.client.derivatives_trading_usds_futures.rest.model.QueryAllAlgoOrdersResponseInner> orders,
                                                    int[] queryCount) {
        if (++queryCount[0] > ALGO_HISTORY_MAX_QUERIES) {
            return false;
        }
        List<com.binance.connector.client.derivatives_trading_usds_futures.rest.model.QueryAllAlgoOrdersResponseInner> page = query.apply(startTime, endTime);
        if (page == null) {
            return false;
        }
        if (page.size() < ALGO_HISTORY_PAGE_SIZE) {
            orders.addAll(page);
            return true;
        }
        if (startTime == endTime) {
            return false;
        }
        long middle = startTime + (endTime - startTime) / 2;
        return collectAlgoHistoryRange(startTime, middle, query, orders, queryCount)
            && collectAlgoHistoryRange(middle + 1, endTime, query, orders, queryCount);
    }

    record AlgoHistoryResult(List<com.binance.connector.client.derivatives_trading_usds_futures.rest.model.QueryAllAlgoOrdersResponseInner> orders, boolean complete) {
    }

    static ProtectionOrderSnapshot findExactAlgoHistory(
        List<com.binance.connector.client.derivatives_trading_usds_futures.rest.model.QueryAllAlgoOrdersResponseInner> data, String symbol, Long orderId, String clientOrderId) {
        if (data == null || symbol == null || symbol.isBlank() || orderId == null && (clientOrderId == null || clientOrderId.isBlank())) {
            return null;
        }
        var matches = data.stream().filter(item -> item != null && symbol.equalsIgnoreCase(item.getSymbol())
            && (orderId == null || orderId.equals(item.getAlgoId()))
            && (clientOrderId == null || clientOrderId.equals(item.getClientAlgoId()))).toList();
        if (matches.size() != 1) {
            return null;
        }
        var item = matches.getFirst();
        String status = normalizeAlgoStatus(item.getAlgoStatus());
        BigDecimal executedQuantity = status != null && Set.of("CANCELED", "EXPIRED", "REJECTED").contains(status) ? BigDecimal.ZERO : null;
        Long updateTime = item.getUpdateTime() == null ? item.getCreateTime() : item.getUpdateTime();
        return new ProtectionOrderSnapshot(item.getSymbol(), item.getAlgoId(), item.getClientAlgoId(), status, item.getSide(), item.getPositionSide(), item.getOrderType(),
            decimal(item.getQuantity()), decimal(item.getTriggerPrice()), instant(updateTime), executedQuantity, item.getReduceOnly(), item.getClosePosition(),
            number(item, "getActualOrderId"), decimal(item.getActualPrice()));
    }

    private static boolean isProtectionOrderAbsent(com.binance.connector.client.common.ApiException exception) {
        try {
            JsonObject error = com.google.gson.JsonParser.parseString(exception.getResponseBody()).getAsJsonObject();
            return error.has("code") && error.get("code").getAsInt() == -2013;
        } catch (RuntimeException ignored) {
            // 无法解析的错误不能降级为“订单不存在”。
            return false;
        }
    }

    /**
     * 向交易所提交普通订单。
     */
    @Override
    public OrderResult placeOrder(OrderRequest request) {
        if (!request.live()) {
            return new OrderResult(null, request.clientOrderId(), "FILLED");
        }
        requireOrderDeadline(request);
        requireCredential(request.credential());
        Side side = Side.fromValue(request.side().toUpperCase());
        OrderType type = OrderType.fromValue(request.type().toUpperCase());
        NewOrderRequest order =
            new NewOrderRequest().symbol(request.symbol()).side(side).type(type).quantity(request.quantity().doubleValue()).newClientOrderId(request.clientOrderId()).newOrderRespType(NewOrderRespType.RESULT);
        if (request.positionSide() != null && !request.positionSide().isBlank()) {
            order.positionSide(request.positionSide().toUpperCase());
        }
        if (request.price() != null) {
            order.price(request.price().doubleValue());
        }
        if (request.reduceOnly() && !hedgePositionSide(request.positionSide())) {
            order.reduceOnly(ReduceOnly.TRUE);
        }
        // api() 内含限流排队和时间同步；准备结束后仍必须检查原始信号截止时间。
        DerivativesTradingUsdsFuturesRestApi client;
        try {
            client = api(request.credential());
        } catch (RuntimeException ex) {
            throw new OrderNotSentException("交易请求准备失败，订单未发送");
        }
        requireOrderDeadline(request);
        ApiResponse<NewOrderResponse> response;
        try {
            response = client.newOrder(order);
        } catch (com.binance.connector.client.common.ApiException ex) {
            Integer code = null;
            String message = null;
            try {
                JsonObject error = com.google.gson.JsonParser.parseString(ex.getResponseBody()).getAsJsonObject();
                code = error.get("code").getAsInt();
                message = error.get("msg").getAsString();
            } catch (RuntimeException ignored) {
            }
            // 只分类已知参数拒绝；503、网络超时及不明确响应仍交给UNKNOWN恢复。
            if (ex.getCode() == 400 && code != null && Set.of(-4023, -4004, -4005, -1111, -4164).contains(code)) {
                tradingRules.remove("TESTNET".equalsIgnoreCase(request.credential().runMode()) ? "TESTNET" : "LIVE");
                throw new OrderRejectedException(code, message);
            }
            throw requestFailure("PLACE_ORDER", ex);
        } catch (RuntimeException ex) {
            // The shared interceptor throws RequestDeferredException before chain.proceed(),
            // so this order HTTP request was definitely not sent. All other runtime failures
            // remain ambiguous because the SDK may already have written the request.
            throw classifyOrderDispatchFailure(ex);
        }
        NewOrderResponse data = response.getData();
        return new OrderResult(data.getOrderId(), data.getClientOrderId(), data.getStatus());
    }

    static RuntimeException classifyOrderDispatchFailure(RuntimeException ex) {
        if (ex instanceof org.dromara.quant.exchange.ExchangeMarketPort.RequestDeferredException deferred) {
            return new OrderNotSentException("REST 调度器在下单 HTTP 请求发送前拒绝派发，订单未发送；请退避 " + deferred.retryAfterMillis() + "ms");
        }
        return ex;
    }

    /**
     * 撤销交易所普通订单。
     */
    @Override
    public void cancelOrder(CancelOrderRequest request) {
        if (!request.live()) {
            return;
        }
        requireCredential(request.credential());
        api(request.credential(), true).cancelOrder(request.symbol(), request.orderId(), request.clientOrderId(), RECV_WINDOW_MILLIS);
    }

    /**
     * 提交交易所条件/保护订单。
     */
    @Override
    public ProtectionOrderResult placeProtectionOrder(ProtectionOrderRequest request) {
        if (!request.live()) {
            return new ProtectionOrderResult(null, request.clientOrderId(), "PAPER_ACCEPTED");
        }
        requireCredential(request.credential());
        NewAlgoOrderRequest algo =
            new NewAlgoOrderRequest().algoType(AlgoType.CONDITIONAL).symbol(request.symbol()).side(Side.fromValue(request.side().toUpperCase())).type(OrderType.fromValue((request.orderType() == null ? "STOP_MARKET" : request.orderType()).toUpperCase())).quantity(request.quantity().doubleValue()).triggerPrice(request.stopPrice().doubleValue()).workingType(WorkingType.CONTRACT_PRICE).clientAlgoId(request.clientOrderId());
        if (request.positionSide() != null && !request.positionSide().isBlank()) {
            algo.positionSide(request.positionSide().toUpperCase());
        }
        if (!hedgePositionSide(request.positionSide())) {
            algo.reduceOnly(ReduceOnly.TRUE);
        }
        ApiResponse<NewAlgoOrderResponse> response;
        try {
            response = api(request.credential(), true).newAlgoOrder(algo);
        } catch (RuntimeException ex) {
            var error = apiError(ex);
            Integer code = error == null ? null : errorCode(error);
            if (error != null && error.getCode() == 400 && code != null && Set.of(-4023, -4004, -4005, -1111, -4164, -4014, -2021).contains(code)) {
                tradingRules.remove("TESTNET".equalsIgnoreCase(request.credential().runMode()) ? "TESTNET" : "LIVE");
                throw new OrderRejectedException(code, "保护单参数被交易所明确拒绝");
            }
            throw requestFailure("PLACE_PROTECTION_ORDER", ex);
        }
        NewAlgoOrderResponse data = response.getData();
        return new ProtectionOrderResult(data.getAlgoId(), data.getClientAlgoId(), normalizeAlgoStatus(data.getAlgoStatus()));
    }

    /**
     * 撤销交易所条件/保护订单。
     */
    @Override
    public void cancelProtectionOrder(CancelProtectionOrderRequest request) {
        if (!request.live()) {
            return;
        }
        if (request.orderId() == null && (request.clientOrderId() == null || request.clientOrderId().isBlank())) {
            throw new IllegalArgumentException("撤销保护单必须指定 orderId 或 clientOrderId");
        }
        requireCredential(request.credential());
        api(request.credential(), true).cancelAlgoOrder(request.orderId(), request.clientOrderId(), RECV_WINDOW_MILLIS);
    }

    /**
     * 订阅账户私有事件流。
     */
    @Override
    public UserDataStreamSession subscribeUserData(Credential credential, Consumer<UserDataEvent> listener) {
        requireCredential(credential);
        String listenKey;
        try {
            listenKey = api(credential, RestRequestScheduler.Kind.ACCOUNT).startUserDataStream().getData().getListenKey();
        } catch (RuntimeException ex) {
            throw requestFailure("START_USER_DATA_STREAM", ex);
        }
        if (listenKey == null || listenKey.isBlank()) {
            throw new IllegalStateException("Binance 用户数据流未返回 listenKey");
        }
        boolean testnet = "TESTNET".equalsIgnoreCase(credential.runMode());
        String base = testnet ? properties.getBinance().getTestnetUserDataStreamUrl() : "wss://fstream.binance.com/private/ws/";
        Consumer<String> events = text -> listener.accept(parseUserDataEvent(text));
        Runnable disconnected = () -> listener.accept(new UserDataEvent("DISCONNECTED", Instant.now(), null));
        if (sdkStreamsEnabled()) {
            try {
                requireSdkStreamsCanConnect();
                // SDK userData(listenKey) subscribes via /private/stream (not the legacy /private/ws/{key} endpoint).
                String sdkBase = base.endsWith("/private/ws/") ? base.substring(0, base.length() - "/private/ws/".length()) + "/stream" : base;
                BinanceSdkStreamTransport sdk = BinanceSdkStreamTransport.user(sdkBase, listenKey, events, disconnected, properties.getBinance().getSdkStreamBacklogLimit(),
                    properties.getProxy());
                StreamHandle managed = trackWebsocket(() -> {
                    sdk.close();
                    try {
                        api(credential).closeUserDataStream();
                    } catch (RuntimeException ignored) {
                    }
                }, disconnected);
                return new UserDataStreamSession(listenKey, Instant.now(), () -> keepAliveUserDataStream(credential), managed::close, sdk::connected);
            } catch (RuntimeException ex) {
                // The listenKey is still valid: SDK bootstrap failed before a usable session was returned.
                // Keep it for the raw socket, and downgrade the node so market and private streams agree.
                fallbackToRawWebsocketTransport(ex);
                log.error("Binance SDK user stream bootstrap failed; falling back to RAW_OKHTTP for this credential; errorType={}",
                    ex.getClass().getSimpleName());
            }
        }
        SocketConnection socket = new SocketConnection(base + listenKey, events, disconnected);
        StreamHandle managed = trackWebsocket(() -> {
            socket.close();
            try {
                api(credential).closeUserDataStream();
            } catch (RuntimeException ignored) {
            }
        }, disconnected);
        return new UserDataStreamSession(listenKey, Instant.now(), () -> keepAliveUserDataStream(credential), managed::close, socket.connected::get);
    }

    private void keepAliveUserDataStream(Credential credential) {
        try {
            api(credential, RestRequestScheduler.Kind.ACCOUNT).keepaliveUserDataStream();
        } catch (RuntimeException ex) {
            throw requestFailure("KEEPALIVE_USER_DATA_STREAM", ex);
        }
    }

    private UserDataEvent parseUserDataEvent(String text) {
        JsonObject json = gson.fromJson(text, JsonObject.class);
        String type = json.has("e") ? json.get("e").getAsString() : "UNKNOWN";
        Instant time = json.has("E") ? Instant.ofEpochMilli(json.get("E").getAsLong()) : Instant.now();
        return new UserDataEvent(type, time, text);
    }

    /**
     * 公开行情仅选择环境，不携带账户签名或密钥。
     */
    private DerivativesTradingUsdsFuturesRestApi marketApi(String runMode) {
        return new DerivativesTradingUsdsFuturesRestApi(marketRestClient(new ApiClient(restConfiguration(marketBaseUrl(runMode)))));
    }

    okhttp3.OkHttpClient marketHttpClient(okhttp3.OkHttpClient client) {
        return restHttpClient(client, MARKET_REST_CONNECT_TIMEOUT_SECONDS, MARKET_REST_READ_TIMEOUT_SECONDS, MARKET_REST_CALL_TIMEOUT_SECONDS);
    }

    ApiClient marketRestClient(ApiClient client) {
        client.setHttpClient(restHttpClient(client.getHttpClient(), MARKET_REST_CONNECT_TIMEOUT_SECONDS, MARKET_REST_READ_TIMEOUT_SECONDS,
            MARKET_REST_CALL_TIMEOUT_SECONDS, RestRequestScheduler.Kind.BACKGROUND));
        return client;
    }

    ApiClient accountRestClient(ApiClient client) {
        client.setHttpClient(restHttpClient(client.getHttpClient(), ACCOUNT_REST_CONNECT_TIMEOUT_SECONDS, ACCOUNT_REST_READ_TIMEOUT_SECONDS,
            ACCOUNT_REST_CALL_TIMEOUT_SECONDS, RestRequestScheduler.Kind.ACCOUNT));
        return client;
    }

    private okhttp3.OkHttpClient restHttpClient(okhttp3.OkHttpClient client, long connectSeconds, long readSeconds, long callSeconds) {
        return restHttpClient(client, connectSeconds, readSeconds, callSeconds, RestRequestScheduler.Kind.BACKGROUND);
    }

    private okhttp3.OkHttpClient restHttpClient(okhttp3.OkHttpClient client, long connectSeconds, long readSeconds, long callSeconds,
                                                RestRequestScheduler.Kind kind) {
        return client.newBuilder().connectionPool(restPool).dispatcher(restDispatcher)
            .connectTimeout(connectSeconds, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(readSeconds, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(callSeconds, java.util.concurrent.TimeUnit.SECONDS)
            // Acquire a slot for every actual HTTP request, not merely when an SDK client is constructed.
            // Some SDK API objects issue multiple requests, so client-construction throttling is insufficient.
            .addInterceptor(chain -> {
                long queueStarted = System.nanoTime();
                restScheduler.acquire(kind);
                long queueMillis = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - queueStarted);
                ClockSyncTiming syncTiming = clockSyncTiming.get();
                log.debug("Binance REST 获准执行 kind={} queueMillis={} clockSync={}", kind, queueMillis, syncTiming != null);
                // Keep diagnostics out of the measured HTTP round trip.
                long requestStartedAt = System.currentTimeMillis();
                if (syncTiming != null) {
                    syncTiming.markRequestStarted(requestStartedAt, queueMillis);
                }
                okhttp3.Response response = chain.proceed(chain.request());
                if (syncTiming != null) {
                    syncTiming.markResponseReceived(System.currentTimeMillis());
                }
                // Apply HTTP 418 cooldown/latch centrally so even SDK methods without a local
                // ApiException wrapper cannot leave the shared REST scheduler open.
                if (response.code() == 418 || response.code() == 429) {
                    String body = null;
                    if (response.code() == 418) {
                        try {
                            body = response.peekBody(64L * 1024L).string();
                        } catch (java.io.IOException inspectionFailure) {
                            // If the expiry cannot be inspected, fail closed instead of guessing.
                            restScheduler.blockUntilManualReset();
                            log.error("Binance REST HTTP 418 expiry could not be inspected; REST is latched closed until operator restart.");
                        }
                    }
                    if (!restScheduler.isManuallyBlocked()) {
                        long retryAfterMillis = retryAfterMillis(response.code(), response.header("Retry-After"), body);
                        if (retryAfterMillis < 0L) {
                            restScheduler.blockUntilManualReset();
                            log.error("Binance REST HTTP 418 has no trustworthy expiry; REST is latched closed until operator restart.");
                        } else {
                            restScheduler.deferFor(retryAfterMillis);
                        }
                    }
                }
                // Capture Binance's actual rolling weight when exposed by the endpoint. This is
                // diagnostic telemetry for validating budgets; never infer a universal limit from it.
                String usedWeight = response.header("X-MBX-USED-WEIGHT-1M");
                if (usedWeight != null || response.code() == 418 || response.code() == 429) {
                    log.debug("Binance REST 响应 status={} kind={} usedWeight1m={} retryAfter={}", response.code(), kind, usedWeight,
                        response.header("Retry-After"));
                }
                return response;
            })
            .retryOnConnectionFailure(false).build();
    }

    /**
     * 根据凭证运行模式创建 Binance API 客户端。
     */

    /**
     * 按运行环境选择行情 REST 主机，不允许跨环境回退。
     */
    private String marketBaseUrl(String runMode) {
        return "TESTNET".equalsIgnoreCase(runMode) ? properties.getBinance().getTestnetUrl() : properties.getBinance().getBaseUrl();
    }

    /**
     * 将 Binance 的服务端时钟偏移应用于公开行情时间判定。
     */
    @Override
    public long exchangeTimeOffsetMillis(String runMode) {
        String baseUrl = marketBaseUrl(runMode);
        try {
            return clockOffset(baseUrl);
        } catch (RuntimeException ex) {
            if (!(ex instanceof ExchangeMarketPort.RequestDeferredException ||
                ex instanceof ExchangeMarketPort.RateLimitException)) {
                throw ex;
            }
            ClockOffset cached = clockOffsets.get(baseUrl);
            long now = System.currentTimeMillis();
            if (cached == null || now - cached.updatedAt() > MAX_STALE_MARKET_CLOCK_OFFSET_MILLIS) {
                throw ex;
            }
            java.util.concurrent.atomic.AtomicBoolean shouldWarn = new java.util.concurrent.atomic.AtomicBoolean();
            clockOffsetWarningTimes.compute(baseUrl, (ignored, lastWarning) -> {
                if (lastWarning == null || now - lastWarning >= 30_000L) {
                    shouldWarn.set(true);
                    return now;
                }
                return lastWarning;
            });
            if (shouldWarn.get()) {
                log.warn("Binance时间同步受限，暂用最近一次有效偏移并等待恢复: baseUrl={}, cacheAgeMs={}", baseUrl, now - cached.updatedAt());
            }
            return cached.value();
        }
    }

    private DerivativesTradingUsdsFuturesRestApi api(Credential credential) {
        return api(credential, RestRequestScheduler.Kind.BACKGROUND);
    }

    private DerivativesTradingUsdsFuturesRestApi api(Credential credential, boolean urgent) {
        return api(credential, urgent ? RestRequestScheduler.Kind.TRADE : RestRequestScheduler.Kind.BACKGROUND);
    }

    private DerivativesTradingUsdsFuturesRestApi api(Credential credential, RestRequestScheduler.Kind kind) {
        boolean testnet = credential != null && "TESTNET".equalsIgnoreCase(credential.runMode());
        ClientConfiguration config = restConfiguration(testnet ? properties.getBinance().getTestnetUrl() : properties.getBinance().getBaseUrl(), kind);
        if (credential != null) {
            SignatureConfiguration signature = new SignatureConfiguration();
            signature.setApiKey(credential.apiKey());
            signature.setSecretKey(credential.secretKey());
            config.setSignatureConfiguration(signature);
            long offset = clockOffset(config.getUrl());
            ApiClient client = new ApiClient(config, new OffsetAuthenticationFactory(offset));
            return new DerivativesTradingUsdsFuturesRestApi(kind == RestRequestScheduler.Kind.ACCOUNT ? accountRestClient(client) : restClient(client, kind));
        }
        ApiClient client = new ApiClient(config);
        return new DerivativesTradingUsdsFuturesRestApi(kind == RestRequestScheduler.Kind.ACCOUNT ? accountRestClient(client) : restClient(client, kind));
    }

    ApiClient restClient(ApiClient client) {
        return restClient(client, RestRequestScheduler.Kind.BACKGROUND);
    }

    private ApiClient restClient(ApiClient client, RestRequestScheduler.Kind kind) {
        // 共享连接池，仍保留 SDK 的每请求签名与代理配置；有限超时避免阻塞本地风控。
        client.setHttpClient(restHttpClient(client.getHttpClient(), 3L, 3L, 5L, kind));
        return client;
    }

    /**
     * REST、公开行情和服务器时间请求使用相同的可选 HTTP 代理；未配置时由 SDK 直连。
     */
    ClientConfiguration restConfiguration(String baseUrl) {
        return restConfiguration(baseUrl, RestRequestScheduler.Kind.BACKGROUND);
    }

    private ClientConfiguration restConfiguration(String baseUrl, RestRequestScheduler.Kind kind) {
        ClientConfiguration config = new ClientConfiguration();
        config.setUrl(baseUrl);
        // SDK 默认会自动重试 429，容易放大 IP 权重；由上层低频调度和退避处理。
        config.setRetries(0);
        QuantProperties.Proxy proxy = properties.getProxy();
        if (proxy == null || (proxy.getHost() == null || proxy.getHost().isBlank()) && proxy.getPort() == null) {
            log.debug("Binance REST 请求未配置代理，使用直连: baseUrl={}", baseUrl);
            return config;
        }
        if (proxy.getHost() == null || proxy.getHost().isBlank() || proxy.getPort() == null || proxy.getPort() < 1 || proxy.getPort() > 65535) {
            throw new IllegalStateException("量化代理配置需要有效的 host 和 port");
        }
        config.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost().trim(), proxy.getPort())));
        //        log.info("Binance REST 请求启用 HTTP 代理: baseUrl={}, proxyHost={}, proxyPort={}", baseUrl, proxy.getHost().trim(), proxy.getPort());
        return config;
    }

    /**
     * 获取 Binance 服务器时间偏移。
     *
     * 估算必须使用 REST 调度器放行后、真正进入 HTTP 调用链的起始时间；
     * 若使用 SDK 调用前的时间，本地队列等待会被错误地当作网络 RTT，
     * 使中点偏向过去，并可能导致签名时间戳仍然超前于 Binance 服务器。
     */
    private long clockOffset(String baseUrl) {
        long now = System.currentTimeMillis();
        ClockOffset cached = clockOffsets.get(baseUrl);
        if (cached != null && now - cached.updatedAt() < CLOCK_OFFSET_REFRESH_MILLIS) {
            return cached.value();
        }
        synchronized (clockOffsetRefreshLocks.computeIfAbsent(baseUrl, ignored -> new Object())) {
            // 等待中的线程必须再次检查缓存，不能重复发起同一轮刷新。
            cached = clockOffsets.get(baseUrl);
            if (cached != null && System.currentTimeMillis() - cached.updatedAt() < CLOCK_OFFSET_REFRESH_MILLIS) {
                return cached.value();
            }
            long refreshStartedAt = System.currentTimeMillis();
            ClockSyncResult sync = synchronizeClock(baseUrl);
            clockOffsets.put(baseUrl, new ClockOffset(sync.offsetMillis(), sync.responseReceivedAtMillis()));
            log.info("Binance 服务端时间偏移已刷新: baseUrl={}, offsetMillis={}, recvWindowMillis={}, syncElapsedMillis={}, schedulerQueueMillis={}, requestElapsedMillis={}",
                baseUrl, sync.offsetMillis(), RECV_WINDOW_MILLIS, sync.responseReceivedAtMillis() - refreshStartedAt,
                sync.schedulerQueueMillis(), sync.responseReceivedAtMillis() - sync.requestStartedAtMillis());
            return sync.offsetMillis();
        }
    }

    /** 强制刷新偏移；若其他并发请求已在本次请求后完成刷新，则复用其结果。 */
    private void refreshClockOffset(String baseUrl, long observedUpdatedAt) {
        synchronized (clockOffsetRefreshLocks.computeIfAbsent(baseUrl, ignored -> new Object())) {
            ClockOffset cached = clockOffsets.get(baseUrl);
            if (cached != null && cached.updatedAt() > observedUpdatedAt) {
                return;
            }
            long refreshStartedAt = System.currentTimeMillis();
            ClockSyncResult sync = synchronizeClock(baseUrl);
            clockOffsets.put(baseUrl, new ClockOffset(sync.offsetMillis(), sync.responseReceivedAtMillis()));
            log.info("Binance 服务端时间偏移已强制刷新: baseUrl={}, offsetMillis={}, syncElapsedMillis={}, schedulerQueueMillis={}, requestElapsedMillis={}",
                baseUrl, sync.offsetMillis(), sync.responseReceivedAtMillis() - refreshStartedAt,
                sync.schedulerQueueMillis(), sync.responseReceivedAtMillis() - sync.requestStartedAtMillis());
        }
    }

    /**
     * 发起一次带调度的 Binance 公共服务器时间请求，并从 HTTP 拦截器取得实际往返时间。
     */
    private ClockSyncResult synchronizeClock(String baseUrl) {
        ClientConfiguration config = restConfiguration(baseUrl);
        ClockSyncTiming timing = new ClockSyncTiming();
        clockSyncTiming.set(timing);
        ApiResponse<CheckServerTimeResponse> response;
        try {
            response = new DerivativesTradingUsdsFuturesRestApi(restClient(new ApiClient(config))).checkServerTime();
        } catch (RuntimeException ex) {
            throw requestFailure("CHECK_SERVER_TIME", ex);
        } finally {
            // 不让时间同步上下文泄漏到同线程后续的其他 REST 请求。
            clockSyncTiming.remove();
        }
        CheckServerTimeResponse data = response.getData();
        if (data == null || data.getServerTime() == null) {
            throw new IllegalStateException("无法获取 Binance 服务端时间");
        }
        long requestStartedAt = timing.requestStartedAtMillis();
        long responseReceivedAt = timing.responseReceivedAtMillis();
        if (requestStartedAt <= 0L || responseReceivedAt < requestStartedAt) {
            // 测量缺失时不回退到 SDK 调用前的时间，否则会重新引入排队偏差。
            throw new IllegalStateException("无法准确测量 Binance 服务器时间请求的 HTTP 往返时间");
        }
        long offset = estimateClockOffset(data.getServerTime(), requestStartedAt, responseReceivedAt);
        return new ClockSyncResult(offset, requestStartedAt, responseReceivedAt, timing.schedulerQueueMillis());
    }

    /**
     * 使用 HTTP 请求真正开始到收到响应之间的中点估算时钟偏移。
     * 包含 SDK 调用前的本地排队时间会产生偏差，因此调用方不能传入排队前的时间戳。
     */
    static long estimateClockOffset(long serverTimeMillis, long requestStartedAtMillis, long responseReceivedAtMillis) {
        if (requestStartedAtMillis <= 0L || responseReceivedAtMillis < requestStartedAtMillis) {
            throw new IllegalArgumentException("服务器时间采样的 HTTP 时间区间无效");
        }
        long localMidpoint = requestStartedAtMillis + (responseReceivedAtMillis - requestStartedAtMillis) / 2L;
        return serverTimeMillis - localMidpoint;
    }

    /**
     * 在同一进程内限制 REST 请求启动速率。
     */

    private record TradingRules(long expiresAt, ExchangeInfo info) {
    }

    /**
     * Binance 服务器时间偏移缓存。
     *
     * @param value     内部状态值
     * @param updatedAt 最后更新时间
     */
    private record ClockOffset(long value, long updatedAt) {
    }

    /** 单次服务器时间同步的 HTTP 时序采样。 */
    private record ClockSyncResult(long offsetMillis, long requestStartedAtMillis, long responseReceivedAtMillis,
                                   long schedulerQueueMillis) {
    }

    /** 由 REST 拦截器在调度放行及响应到达时填充。 */
    private static final class ClockSyncTiming {
        private volatile long requestStartedAtMillis;
        private volatile long responseReceivedAtMillis;
        private volatile long schedulerQueueMillis;

        private void markRequestStarted(long timestampMillis, long queueMillis) {
            requestStartedAtMillis = timestampMillis;
            schedulerQueueMillis = queueMillis;
        }

        private void markResponseReceived(long timestampMillis) {
            responseReceivedAtMillis = timestampMillis;
        }

        private long requestStartedAtMillis() {
            return requestStartedAtMillis;
        }

        private long responseReceivedAtMillis() {
            return responseReceivedAtMillis;
        }

        private long schedulerQueueMillis() {
            return schedulerQueueMillis;
        }
    }

    /**
     * 支持服务器时间偏移的 Binance 鉴权工厂。
     */
    private static final class OffsetAuthenticationFactory extends BinanceAuthenticationFactory {
        /**
         * Binance 服务器时间偏移。
         */
        private final long offset;

        /**
         * 创建带服务器时间偏移支持的 Binance 鉴权工厂。
         */
        private OffsetAuthenticationFactory(long offset) {
            this.offset = offset;
        }

        /**
         * 创建带服务器时间偏移的 Binance 鉴权实现。
         */
        @Override
        public Authentication getAuthentication(SignatureConfiguration configuration) {
            SignatureGenerator generator = new SignatureGeneratorFactory().getSignatureGenerator(configuration);
            return generator == null ? null : new OffsetSignatureAuthentication(configuration.getApiKey(), generator, offset);
        }
    }

    /**
     * 在 Binance 签名参数中应用服务器时间偏移的鉴权实现。
     */
    private static final class OffsetSignatureAuthentication extends BinanceBaseAuthentication {
        /**
         * 交易所 API Key。
         */
        private final String apiKey;
        /**
         * Binance 请求签名生成器。
         */
        private final SignatureGenerator generator;
        /**
         * Binance 服务器时间偏移。
         */
        private final long offset;

        /**
         * 创建应用服务器时间偏移的 Binance 签名鉴权器。
         */
        private OffsetSignatureAuthentication(String apiKey, SignatureGenerator generator, long offset) {
            this.apiKey = apiKey;
            this.generator = generator;
            this.offset = offset;
        }

        /**
         * 在签名参数中应用服务器时间偏移。
         */
        @Override
        public void applyToParams(List<Pair> queryParams, Map<String, String> headerParams, Map<String, String> cookieParams, String payload, String method, URI uri) {
            headerParams.put("X-MBX-APIKEY", apiKey);
            queryParams.add(new Pair("timestamp", String.valueOf(System.currentTimeMillis() + offset)));
            String input = joinQueryParameters(queryParams);
            if (requiresRequestBody(method) && payload != null && !payload.isEmpty()) {
                input += payload;
            }
            try {
                queryParams.add(new Pair("signature", generator.signAsString(input)));
            } catch (CryptoException ex) {
                throw new IllegalStateException("Binance 请求签名失败", ex);
            }
        }
    }

    private final class StreamHandle {
        private final Runnable closeTransport;
        private final Runnable notifyDisconnected;
        private final AtomicBoolean stopped = new AtomicBoolean();

        StreamHandle(Runnable closeTransport, Runnable notifyDisconnected) {
            this.closeTransport = closeTransport;
            this.notifyDisconnected = notifyDisconnected;
        }

        void close() {
            if (stopped.compareAndSet(false, true)) {
                activeWebsocketStreams.remove(this);
                try {
                    closeTransport.run();
                } catch (RuntimeException ex) {
                    log.warn("Binance WebSocket close failed: {}", ex.getClass().getSimpleName());
                }
            }
        }

        void reconnect() {
            if (stopped.get()) {
                return;
            }
            close();
            try {
                notifyDisconnected.run();
            } catch (RuntimeException ex) {
                log.warn("Binance WebSocket reconnect notification failed");
            }
        }
    }

    /**
     * OkHttp 负责 ping/pong；重连、补偿及消费隔离统一由 Hub 管理。
     */
    private final class SocketConnection extends okhttp3.WebSocketListener implements AutoCloseable {
        private final AtomicBoolean closed = new AtomicBoolean();
        private final AtomicBoolean connected = new AtomicBoolean();
        private final Consumer<String> messages;
        private final Runnable disconnected;
        private final okhttp3.WebSocket socket;

        private SocketConnection(String url, Consumer<String> messages, Runnable disconnected) {
            this.messages = messages;
            this.disconnected = disconnected;
            var builder =
                new okhttp3.OkHttpClient.Builder().connectionPool(restPool).dispatcher(restDispatcher).connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS).readTimeout(0,
                    java.util.concurrent.TimeUnit.SECONDS).pingInterval(20, java.util.concurrent.TimeUnit.SECONDS).retryOnConnectionFailure(false);
            QuantProperties.Proxy proxy = properties.getProxy();
            if (proxy != null && proxy.getHost() != null && !proxy.getHost().isBlank() && proxy.getPort() != null) {
                builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxy.getHost(), proxy.getPort())));
            }
            socket = builder.build().newWebSocket(new okhttp3.Request.Builder().url(url).build(), this);
        }

        @Override
        public void onOpen(okhttp3.WebSocket socket, okhttp3.Response response) {
            connected.set(!closed.get());
        }

        @Override
        public void onMessage(okhttp3.WebSocket socket, String text) {
            if (closed.get()) {
                return;
            }
            try {
                messages.accept(text);
            } catch (RuntimeException ex) {
                // 不输出私有事件正文/URL，防止 listenKey 或账户信息进入错误日志。
                log.warn("Binance WebSocket 事件处理失败: {}", ex.getClass().getSimpleName());
                fail();
                socket.cancel();
            }
        }

        @Override
        public void onFailure(okhttp3.WebSocket socket, Throwable failure, okhttp3.Response response) {
            // socket.cancel() 会异步触发 onFailure；主动关闭的旧连接不应当产生误导性 WARN。
            // 非预期失败必须继续向 Hub 发送断线事件，以触发重连、REST 补偿和交易门控。
            if (closed.get()) {
                return;
            }
            log.warn("Binance WebSocket传输失败: errorType={}, httpStatus={}, wasConnected={}", failure.getClass().getSimpleName(), response == null ? null : response.code(),
                connected.get());
            fail();
        }

        @Override
        public void onClosing(okhttp3.WebSocket socket, int code, String reason) {
            socket.close(code, null);
            fail();
        }

        private void fail() {
            connected.set(false);
            if (closed.compareAndSet(false, true)) {
                disconnected.run();
            }
        }

        @Override
        public void close() {
            closed.set(true);
            connected.set(false);
            socket.cancel();
        }
    }
}

