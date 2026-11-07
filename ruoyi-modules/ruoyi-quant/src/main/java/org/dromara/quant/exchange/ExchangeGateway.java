package org.dromara.quant.exchange;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 交易所边界。业务模块只能依赖这些内部类型，不能直接引用 Binance SDK。
 */
public interface ExchangeGateway extends ExchangeMarketPort, ExchangeAccountPort, ExchangeTradingPort, ExchangeUserDataPort {
    /**
     * Credential，量化交易模块领域/基础设施对象。
     *
     * @param apiKey        交易所 API Key
     * @param secretKey     交易所 Secret Key；应按系统安全策略加密或保护
     * @param signatureType 交易所请求签名类型
     * @param runMode       运行模式：PAPER、TESTNET 或 LIVE
     */
    record Credential(String apiKey, String secretKey, String signatureType, String runMode) {
    }

    /**
     * ConnectivityResult 操作结果对象。
     *
     * @param success            操作是否成功
     * @param message            结果说明
     * @param canTrade           账户是否具备交易权限
     * @param withdrawalDisabled 兼容字段：账户转出能力是否明确禁用，不代表 API Key 提现权限
     * @param checkedAt          检查时间
     */
    record ConnectivityResult(boolean success, String message, boolean canTrade, boolean withdrawalDisabled, Instant checkedAt) {
        /**
         * 创建简化的交易所连通性检查结果。
         */
        public ConnectivityResult(boolean success, String message) {
            this(success, message, false, false, Instant.now());
        }
    }

    /**
     * ExchangeInfo，量化交易模块领域/基础设施对象。
     *
     * @param symbols 交易规则集合
     */
    record ExchangeInfo(List<SymbolRule> symbols) {
    }

    /**
     * SymbolRule，量化交易模块领域/基础设施对象。
     *
     * @param symbol            交易对，例如 BTCUSDT
     * @param quoteAsset        计价资产，例如 USDT
     * @param pricePrecision    价格精度
     * @param quantityPrecision 数量精度
     * @param priceTickSize     价格最小变动单位
     * @param stepSize          普通订单数量步进
     * @param marketStepSize    市价订单数量步进
     * @param minNotional       最小名义下单金额
     * @param status            当前状态
     */
    record SymbolRule(String symbol, String quoteAsset, int pricePrecision, int quantityPrecision, BigDecimal priceTickSize, BigDecimal stepSize, BigDecimal marketStepSize,
                      BigDecimal minNotional, String status) {
    }

    /**
     * Kline，量化交易模块领域/基础设施对象。
     *
     * @param openTime  K 线开始时间
     * @param closeTime K 线结束时间
     * @param open      K 线开盘价
     * @param high      K 线最高价
     * @param low       K 线最低价
     * @param close     K 线收盘价
     * @param volume    K 线成交量
     * @param closed    K 线是否已收盘
     */
    record Kline(Instant openTime, Instant closeTime, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal volume, boolean closed) {
    }

    /**
     * PriceSnapshot，量化交易模块领域/基础设施对象。
     *
     * @param symbol         交易对，例如 BTCUSDT
     * @param lastTradePrice 最新成交价
     * @param markPrice      交易所标记价格
     * @param observedAt     快照观测时间
     */
    record PriceSnapshot(String symbol, BigDecimal lastTradePrice, BigDecimal markPrice, Instant observedAt) {
    }

    /**
     * MarketEvent，量化交易模块领域/基础设施对象。
     *
     * @param type      信号、事件或订单类型
     * @param symbol    交易对，例如 BTCUSDT
     * @param interval  K 线周期
     * @param price     订单或成交价格
     * @param kline     行情事件携带的 K 线快照
     * @param eventTime 当前行情事件时间
     */
    record MarketEvent(String type, String symbol, String interval, BigDecimal price, Kline kline, Instant eventTime, Long sequenceId, Instant receivedAt, boolean replay) {
        public MarketEvent(String type, String symbol, String interval, BigDecimal price, Kline kline, Instant eventTime) {
            this(type, symbol, interval, price, kline, eventTime, null, Instant.now(), false);
        }
    }

    /**
     * MarketStreamSession，量化交易模块领域/基础设施对象。
     *
     * @param closeAction 关闭底层连接的回调
     */
    record MarketStreamSession(Runnable closeAction) implements AutoCloseable {
        /**
         * 关闭公开行情流会话并释放底层连接资源。
         */
        @Override
        public void close() {
            closeAction.run();
        }
    }

    /**
     * AccountSnapshot，量化交易模块领域/基础设施对象。
     *
     * @param walletBalance    钱包余额
     * @param availableBalance 可用余额
     * @param marginBalance    保证金余额
     * @param unrealizedPnl    未实现盈亏
     */
    record AccountSnapshot(BigDecimal walletBalance, BigDecimal availableBalance, BigDecimal marginBalance, BigDecimal unrealizedPnl) {
    }

    /**
     * PositionSnapshot，量化交易模块领域/基础设施对象。
     *
     * @param symbol           交易对，例如 BTCUSDT
     * @param side             订单买卖方向，例如 BUY/SELL
     * @param quantity         数量
     * @param entryPrice       平均开仓价
     * @param markPrice        交易所标记价格
     * @param unrealizedPnl    未实现盈亏
     * @param liquidationPrice 预估强平价格
     * @param margin           持仓占用保证金
     * @param leverage         杠杆倍数
     * @param takeProfitPrice  交易所持仓返回的止盈参考价
     * @param updateTime       交易所持仓更新时间，用于过滤晚到的旧账户事件
     * @param stopLossPrice    交易所持仓返回的止损参考价
     */
    record PositionSnapshot(String symbol, String side, BigDecimal quantity, BigDecimal entryPrice, BigDecimal markPrice, BigDecimal unrealizedPnl, BigDecimal liquidationPrice,
                            BigDecimal margin, Integer leverage, BigDecimal takeProfitPrice, BigDecimal stopLossPrice, Instant updateTime) {
        public PositionSnapshot(String symbol, String side, BigDecimal quantity, BigDecimal entryPrice, BigDecimal markPrice, BigDecimal unrealizedPnl,
                                BigDecimal liquidationPrice, BigDecimal margin, Integer leverage, BigDecimal takeProfitPrice, BigDecimal stopLossPrice) {
            this(symbol, side, quantity, entryPrice, markPrice, unrealizedPnl, liquidationPrice, margin, leverage, takeProfitPrice, stopLossPrice, null);
        }
    }

    /**
     * OrderSnapshot，量化交易模块领域/基础设施对象。
     *
     * @param symbol           交易对，例如 BTCUSDT
     * @param orderId          交易所订单主键
     * @param clientOrderId    客户端订单号
     * @param side             订单买卖方向，例如 BUY/SELL
     * @param type             信号、事件或订单类型
     * @param status           当前状态
     * @param price            订单或成交价格
     * @param originalQuantity 订单原始数量
     * @param executedQuantity 订单已成交数量
     * @param updateTime       交易所事实更新时间
     * @param averagePrice     实际累计成交均价，未知时为空，不能用委托价格代替
     * @param positionSide     币安返回的持仓方向，例如 LONG/SHORT/BOTH
     * @param reduceOnly       币安返回的只减仓事实；未知时为空
     */
    record OrderSnapshot(String symbol,
                         @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class) Long orderId,
                         String clientOrderId, String side, String type, String status, BigDecimal price, BigDecimal originalQuantity, BigDecimal executedQuantity,
                         Instant updateTime, BigDecimal averagePrice, String positionSide, Boolean reduceOnly, Instant createTime,
                         Boolean closePosition) {
        public OrderSnapshot(String symbol, Long orderId, String clientOrderId, String side, String type, String status, BigDecimal price, BigDecimal originalQuantity,
                             BigDecimal executedQuantity, Instant updateTime, BigDecimal averagePrice, String positionSide, Boolean reduceOnly, Instant createTime) {
            this(symbol, orderId, clientOrderId, side, type, status, price, originalQuantity, executedQuantity, updateTime,
                averagePrice, positionSide, reduceOnly, createTime, null);
        }

        public OrderSnapshot(String symbol, Long orderId, String clientOrderId, String side, String type, String status, BigDecimal price, BigDecimal originalQuantity,
                             BigDecimal executedQuantity, Instant updateTime, BigDecimal averagePrice, String positionSide, Boolean reduceOnly) {
            this(symbol, orderId, clientOrderId, side, type, status, price, originalQuantity, executedQuantity, updateTime,
                averagePrice, positionSide, reduceOnly, null, null);
        }

        public OrderSnapshot(String symbol, Long orderId, String clientOrderId, String side, String type, String status, BigDecimal price, BigDecimal originalQuantity,
                             BigDecimal executedQuantity, Instant updateTime, BigDecimal averagePrice) {
            this(symbol, orderId, clientOrderId, side, type, status, price, originalQuantity, executedQuantity, updateTime, averagePrice, null, null, null, null);
        }

        public OrderSnapshot(String symbol, Long orderId, String clientOrderId, String side, String type, String status, BigDecimal price, BigDecimal originalQuantity,
                             BigDecimal executedQuantity, Instant updateTime) {
            this(symbol, orderId, clientOrderId, side, type, status, price, originalQuantity, executedQuantity, updateTime, null, null, null, null, null);
        }
    }

    /**
     * TradeSnapshot，量化交易模块领域/基础设施对象。
     *
     * @param symbol      交易对，例如 BTCUSDT
     * @param tradeId     交易所成交主键
     * @param orderId     交易所订单主键
     * @param side        订单买卖方向，例如 BUY/SELL
     * @param quantity    数量
     * @param price       订单或成交价格
     * @param commission  交易手续费
     * @param realizedPnl 已实现盈亏
     * @param time         交易所事实发生时间
     * @param commissionAsset 手续费资产
     * @param positionSide 币安返回的持仓方向，例如 LONG/SHORT/BOTH
     */
    record TradeSnapshot(String symbol,
                         @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class) Long tradeId,
                         @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class) Long orderId,
                         String side, BigDecimal quantity, BigDecimal price, BigDecimal commission, BigDecimal realizedPnl, Instant time, String commissionAsset,
                         String positionSide) {
        /**
         * Legacy callers have unknown fee asset and position side.
         */
        public TradeSnapshot(String symbol, Long tradeId, Long orderId, String side, BigDecimal quantity, BigDecimal price, BigDecimal commission,
                             BigDecimal realizedPnl, Instant time, String commissionAsset) {
            this(symbol, tradeId, orderId, side, quantity, price, commission, realizedPnl, time, commissionAsset, null);
        }

        /**
         * Legacy callers have unknown fee asset and position side.
         */
        public TradeSnapshot(String symbol, Long tradeId, Long orderId, String side, BigDecimal quantity, BigDecimal price, BigDecimal commission,
                             BigDecimal realizedPnl, Instant time) {
            this(symbol, tradeId, orderId, side, quantity, price, commission, realizedPnl, time, null, null);
        }
    }

    /**
     * TimeRange，量化交易模块领域/基础设施对象。
     *
     * @param from 查询起始时间
     * @param to   查询结束时间
     */
    record TimeRange(Instant from, Instant to) {
    }

    /**
     * IncomeSnapshot，量化交易模块领域/基础设施对象。
     *
     * @param symbol        交易对，例如 BTCUSDT
     * @param incomeType    交易所收入类型
     * @param amount        财务事件金额；正负方向遵循交易所语义
     * @param asset         资产代码，例如 USDT
     * @param transactionId 交易所财务流水主键
     * @param info          交易所财务流水补充信息
     * @param time          交易所事实发生时间
     */
    record IncomeSnapshot(String symbol, String incomeType, BigDecimal amount, String asset, Long transactionId, String info, Instant time) {
    }

    /**
     * OrderRequest API 请求对象。
     *
     * @param credential    当前运行上下文使用的交易凭证
     * @param symbol        交易对，例如 BTCUSDT
     * @param side          订单买卖方向，例如 BUY/SELL
     * @param type          信号、事件或订单类型
     * @param quantity      数量
     * @param price         订单或成交价格
     * @param reduceOnly    是否为只减仓订单
     * @param positionSide  持仓方向，例如 LONG/SHORT/BOTH
     * @param clientOrderId 客户端订单号
     * @param live          是否真正向交易所发送有交易副作用的请求
     */
    record OrderRequest(Credential credential, String symbol, String side, String type, BigDecimal quantity, BigDecimal price, boolean reduceOnly, String positionSide,
                        String clientOrderId, boolean live, Instant validUntil) {
        public OrderRequest(Credential credential, String symbol, String side, String type, BigDecimal quantity, BigDecimal price, boolean reduceOnly, String positionSide,
                            String clientOrderId, boolean live) {
            this(credential, symbol, side, type, quantity, price, reduceOnly, positionSide, clientOrderId, live, null);
        }
    }

    /**
     * OrderResult 操作结果对象。
     *
     * @param orderId       交易所订单主键
     * @param clientOrderId 客户端订单号
     * @param status        当前状态
     */
    record OrderResult(Long orderId, String clientOrderId, String status) {
    }

    /**
     * 人工撤单 API 请求对象。
     *
     * @param credential    当前运行上下文使用的交易凭证
     * @param symbol        交易对，例如 BTCUSDT
     * @param orderId       交易所订单主键
     * @param clientOrderId 客户端订单号
     * @param live          是否真正向交易所发送有交易副作用的请求
     */
    record CancelOrderRequest(Credential credential, String symbol, Long orderId, String clientOrderId, boolean live) {
    }

    /**
     * ProtectionOrderRequest API 请求对象。
     *
     * @param credential    当前运行上下文使用的交易凭证
     * @param symbol        交易对，例如 BTCUSDT
     * @param side          订单买卖方向，例如 BUY/SELL
     * @param positionSide  持仓方向，例如 LONG/SHORT/BOTH
     * @param orderType     订单类型
     * @param quantity      数量
     * @param stopPrice     条件单触发价格
     * @param clientOrderId 客户端订单号
     * @param live          是否真正向交易所发送有交易副作用的请求
     */
    record ProtectionOrderRequest(Credential credential, String symbol, String side, String positionSide, String orderType, BigDecimal quantity, BigDecimal stopPrice,
                                  String clientOrderId, boolean live) {
    }

    /**
     * ProtectionOrderResult 操作结果对象。
     *
     * @param orderId       交易所订单主键
     * @param clientOrderId 客户端订单号
     * @param status        当前状态
     */
    record ProtectionOrderResult(Long orderId, String clientOrderId, String status) {
    }

    /**
     * CancelProtectionOrderRequest API 请求对象。
     *
     * @param credential    当前运行上下文使用的交易凭证
     * @param orderId       交易所订单主键
     * @param clientOrderId 客户端订单号
     * @param live          是否真正向交易所发送有交易副作用的请求
     */
    record CancelProtectionOrderRequest(Credential credential, Long orderId, String clientOrderId, boolean live) {
    }

    /**
     * PositionMode，量化交易模块领域/基础设施对象。
     *
     * @param hedgeMode 是否启用双向持仓模式
     */
    record PositionMode(boolean hedgeMode) {
    }

    /**
     * ProtectionOrderSnapshot，量化交易模块领域/基础设施对象。
     *
     * @param symbol        交易对，例如 BTCUSDT
     * @param algoId        交易所条件/算法单主键
     * @param clientAlgoId  客户端条件单标识
     * @param status        当前状态
     * @param side          订单买卖方向，例如 BUY/SELL
     * @param positionSide  持仓方向，例如 LONG/SHORT/BOTH
     * @param type          信号、事件或订单类型
     * @param quantity      数量
     * @param triggerPrice  条件单触发价格
     * @param reduceOnly    是否仅减仓，用于区分单向持仓保护与开仓条件单
     * @param closePosition 是否触发后平掉全部持仓
     * @param updateTime    交易所事实更新时间
     */
    record ProtectionOrderSnapshot(String symbol,
                                   @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class) Long algoId,
                                   String clientAlgoId, String status, String side, String positionSide, String type, BigDecimal quantity, BigDecimal triggerPrice,
                                   Instant updateTime, BigDecimal executedQuantity, Boolean reduceOnly, Boolean closePosition,
                                   @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class) Long actualOrderId,
                                   BigDecimal actualPrice) {
        public ProtectionOrderSnapshot(String symbol, Long algoId, String clientAlgoId, String status, String side, String positionSide, String type, BigDecimal quantity,
                                       BigDecimal triggerPrice, Instant updateTime, BigDecimal executedQuantity, Boolean reduceOnly, Boolean closePosition) {
            this(symbol, algoId, clientAlgoId, status, side, positionSide, type, quantity, triggerPrice, updateTime, executedQuantity, reduceOnly, closePosition, null, null);
        }

        public ProtectionOrderSnapshot(String symbol, Long algoId, String clientAlgoId, String status, String side, String positionSide, String type, BigDecimal quantity,
                                       BigDecimal triggerPrice, Instant updateTime, BigDecimal executedQuantity) {
            this(symbol, algoId, clientAlgoId, status, side, positionSide, type, quantity, triggerPrice, updateTime, executedQuantity, null, null);
        }

        public ProtectionOrderSnapshot(String symbol, Long algoId, String clientAlgoId, String status, String side, String positionSide, String type, BigDecimal quantity,
                                       BigDecimal triggerPrice, Instant updateTime) {
            this(symbol, algoId, clientAlgoId, status, side, positionSide, type, quantity, triggerPrice, updateTime, null);
        }
    }

    /**
     * UserDataEvent，量化交易模块领域/基础设施对象。
     *
     * @param eventType 财务事件类型，例如 FUNDING_FEE、COMMISSION、REALIZED_PNL
     * @param eventTime 当前行情事件时间
     * @param rawJson   交易所私有事件原始 JSON
     */
    record UserDataEvent(String eventType, Instant eventTime, String rawJson) {
    }

    /**
     * UserDataStreamSession，量化交易模块领域/基础设施对象。
     *
     * @param listenKey       用户数据流 listenKey
     * @param startedAt       用户数据流启动时间
     * @param keepAliveAction listenKey 保活回调
     * @param closeAction     关闭底层连接的回调
     */
    record UserDataStreamSession(String listenKey, Instant startedAt, Runnable keepAliveAction, Runnable closeAction,
                                 java.util.function.BooleanSupplier connected) implements AutoCloseable {
        public UserDataStreamSession(String listenKey, Instant startedAt, Runnable keepAliveAction, Runnable closeAction) {
            this(listenKey, startedAt, keepAliveAction, closeAction, () -> true);
        }

        /**
         * 返回当前上游连接的健康状态。
         */
        public boolean isConnected() {
            return connected.getAsBoolean();
        }

        /**
         * 续期 UserData listenKey。
         */
        public void keepAlive() {
            keepAliveAction.run();
        }

        /**
         * 关闭账户用户数据流会话并释放 listenKey/连接资源。
         */
        @Override
        public void close() {
            closeAction.run();
        }
    }
}
