package org.dromara.quant.exchange;

import java.util.List;

/**
 * 交易所订单/成交端口。
 */
public interface ExchangeTradingPort {
    List<ExchangeGateway.OrderSnapshot> queryOrders(ExchangeGateway.Credential credential, String symbol);

    ExchangeGateway.OrderSnapshot queryOrder(ExchangeGateway.Credential credential, String symbol, Long orderId, String clientOrderId);

    List<ExchangeGateway.OrderSnapshot> queryOrderHistory(ExchangeGateway.Credential credential, String symbol, ExchangeGateway.TimeRange range, Long limit);

    List<ExchangeGateway.TradeSnapshot> queryTrades(ExchangeGateway.Credential credential, String symbol, ExchangeGateway.TimeRange range);

    List<ExchangeGateway.TradeSnapshot> queryTradesByOrder(ExchangeGateway.Credential credential, String symbol, Long orderId, ExchangeGateway.TimeRange range, Long limit);

    ExchangeGateway.OrderResult placeOrder(ExchangeGateway.OrderRequest request);

    /**
     * 撤销交易所普通订单。
     */
    void cancelOrder(ExchangeGateway.CancelOrderRequest request);

    ExchangeGateway.ProtectionOrderResult placeProtectionOrder(ExchangeGateway.ProtectionOrderRequest request);

    /**
     * 撤销交易所条件/保护订单。
     */
    void cancelProtectionOrder(ExchangeGateway.CancelProtectionOrderRequest request);

    List<ExchangeGateway.ProtectionOrderSnapshot> queryProtectionOrders(ExchangeGateway.Credential credential, String symbol);

    /**
     * 按持久化标识精确确认条件单，默认兼容其他网关。
     */
    default ExchangeGateway.ProtectionOrderSnapshot queryProtectionOrder(ExchangeGateway.Credential credential, Long orderId, String clientOrderId, String symbol) {
        return queryProtectionOrders(credential, symbol).stream().filter(item -> orderId != null && orderId.equals(item.algoId()) || clientOrderId != null && clientOrderId.equals(item.clientAlgoId())).findFirst().orElse(null);
    }

    /**
     * 可安全展示的执行诊断，不保留 SDK 原始请求和异常。
     */
    class ExchangeRequestException extends IllegalStateException {
        private final Integer httpStatus;
        private final Integer code;

        public ExchangeRequestException(String stage, Integer httpStatus, Integer code, String message) {
            super("stage=" + stage + " httpStatus=" + httpStatus + " binanceCode=" + code + " msg=" + sanitize(message));
            this.httpStatus = httpStatus;
            this.code = code;
        }

        public Integer httpStatus() {
            return httpStatus;
        }

        public Integer code() {
            return code;
        }

        private static String sanitize(String message) {
            if (message == null) {
                return "unknown";
            }
            String safe =
                message.replaceAll("[\\r\\n]", " ").replaceAll("(?i)https?://\\S+", "[REDACTED_URL]").replaceAll("(?i)bearer\\s+[^\\s,;]+", "Bearer [REDACTED]").replaceAll("(?i)" +
                    "(signature|token|secret|api[-_]?key|authorization)\\s*[:=]\\s*[^\\s,;]+", "$1=[REDACTED]");
            return safe.substring(0, Math.min(safe.length(), 256));
        }
    }

    /**
     * 仅用于交易所明确拒绝订单；超时、断网和未知响应不能使用。
     */
    class OrderRejectedException extends ExchangeRequestException {
        public OrderRejectedException(int code, String message) {
            super("PLACE_ORDER", 400, code, message);
        }
    }

    /**
     * 网关已确认尚未调用下单HTTP，允许按FAILED释放；请求发出后不得使用。
     */
    class OrderNotSentException extends ExchangeRequestException {
        public OrderNotSentException(String reason) {
            super("BEFORE_PLACE_ORDER", null, null, reason);
        }
    }

    /**
     * 仅交易所明确返回订单不存在时使用，网络异常不得转换成该结果。
     */
    class ProtectionOrderAbsentException extends IllegalStateException {
        public ProtectionOrderAbsentException() {
            super("Binance 明确确认保护单不存在");
        }
    }
}
