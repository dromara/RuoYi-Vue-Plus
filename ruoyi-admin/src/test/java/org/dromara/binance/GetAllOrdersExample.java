package org.dromara.binance;

import com.binance.connector.client.common.ApiException;
import com.binance.connector.client.common.ApiResponse;
import com.binance.connector.client.common.configuration.ClientConfiguration;
import com.binance.connector.client.common.configuration.SignatureConfiguration;
import com.binance.connector.client.derivatives_trading_usds_futures.rest.DerivativesTradingUsdsFuturesRestApiUtil;
import com.binance.connector.client.derivatives_trading_usds_futures.rest.api.DerivativesTradingUsdsFuturesRestApi;
import com.binance.connector.client.derivatives_trading_usds_futures.rest.model.AllOrdersResponse;
import com.binance.connector.client.derivatives_trading_usds_futures.rest.model.CancelOrderResponse;
import lombok.Data;
import org.dromara.common.core.utils.DateUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** API examples for TradeApi */
public class GetAllOrdersExample {
    private DerivativesTradingUsdsFuturesRestApi api;

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private static final String TABLE_HEADER_FORMAT =
        "| %-4s | %-16s | %-8s | %-18s | %-4s | %-4s | %-8s | %-12s | %-12s | %-12s | %-12s | %-12s | %-12s | %-4s | %-22s |%n";

    private static final String TABLE_ROW_FORMAT =
        "| %-4d | %-19s | %-10s | %-20s | %-4s | %-6s | %-12s | %-12s | %-14s | %-14s | %-14s | %-14s | %-16s | %-6s | %-22s |%n";

    private static final String TABLE_SEPARATOR =
        "+------+---------------------+------------+--------------+------+--------+--------------+--------------+----------------+----------------+----------------+----------------+------------------+--------+------------------------+";

    private static final List<OrderSymbol> list = new ArrayList<>();

    public DerivativesTradingUsdsFuturesRestApi getApi() {
        if (api == null) {
            ClientConfiguration clientConfiguration = DerivativesTradingUsdsFuturesRestApiUtil.getClientConfiguration();
            SignatureConfiguration signatureConfiguration = new SignatureConfiguration();
            signatureConfiguration.setApiKey("MYo3ipbZULoMbSDQ9hsSkZHaXiF67Id4WlXLTGeh5Azgjj3doJaVgggpTVcWwd0D");
            signatureConfiguration.setSecretKey("mmNAGZXkF9p0EUbyNrZuQ6VOanjL6XxMTdx28vo4aW13bnrnMyU3Lncluevs2vrO");
            clientConfiguration.setSignatureConfiguration(signatureConfiguration);
            api = new DerivativesTradingUsdsFuturesRestApi(clientConfiguration);
        }
        return api;
    }

    /**
     * Cancel Order (TRADE)
     *
     * <p>Cancel an active order. Weight(IP): 1 Security Type: TRADE Notes: - Either
     * &#x60;orderId&#x60; or &#x60;origClientOrderId&#x60; must be sent.
     *
     * @throws ApiException if the Api call fails
     */
    public void cancelOrderExample() throws ApiException, IOException {
        int diff = 60 * 60 * 24 * 1000 * 30;
        String symbol = "BRUSDT";
        symbol = "";
        Long orderId = 0L;
        String origClientOrderId = "myOrder1";
        Long endTime = DateUtils.current();
        Long startTime = DateUtils.date(endTime - diff).getTime();
        System.out.println("endTime:" + DateUtils.date(endTime).toDateStr());
        System.out.println("startTime:" + DateUtils.date(startTime).toDateStr());
//        startTime = DateUtils.parse("2026-11-01 00:00:00").getTime();
//        System.out.println(startTime);
        startTime = null;
        endTime= null;
        Long limit = 100L;
        Long recvWindow = 5000L;
        ApiResponse<AllOrdersResponse> response = getApi().allOrders(symbol, orderId, startTime, endTime, limit, recvWindow);
        AllOrdersResponse allOrders = response.getData();
        // ===== 表格标题 =====
        System.out.println();
        System.out.println("Binance USDS-M Futures 订单列表（GET /fapi/v1/allOrders）");
        System.out.println(TABLE_SEPARATOR);
        System.out.printf(TABLE_HEADER_FORMAT,
            "序号", "时间", "交易对", "订单ID", "方向", "持仓方向",
            "类型", "状态", "价格", "均价", "原始数量", "已成交数量",
            "累计成交额(U)", "只减仓", "客户端订单ID");
        System.out.println(TABLE_SEPARATOR);

        System.out.println(allOrders.get(0).toJson());
        // ===== 数据行 =====
        int index = 1;
        for (var order : allOrders) {
            list.add(new OrderSymbol(order.getOrderId(), order.getSymbol()));
            String time = order.getTime() == null
                ? "-"
                : TIME_FMT.format(Instant.ofEpochMilli(order.getTime()));

            System.out.printf(TABLE_ROW_FORMAT,
                index++,
                time,
                safe(order.getSymbol()),
                safe(order.getOrderId()),
                safe(order.getSide()),
                safe(order.getPositionSide()),
                safe(order.getType()),
                safe(order.getStatus()),
                safe(order.getPrice()),
                safe(order.getAvgPrice()),
                safe(order.getOrigQty()),
                safe(order.getExecutedQty()),
                safe(order.getCumQuote()),
                safe(order.getReduceOnly()),
                safe(order.getClientOrderId()));
        }
        System.out.println(TABLE_SEPARATOR);
        System.out.println("共 " + allOrders.size() + " 条");

    }

    private static String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    public static void main(String[] args) throws Exception {
        // 启动时先同步 Binance 服务器时间
        BinanceTimeSync.sync();

        GetAllOrdersExample order = new GetAllOrdersExample();
        order.getApi();
        order.cancelOrderExample();

        GetAllTradeExample trade = new GetAllTradeExample();
        trade.getApi();
        // ===== 表格输出 =====
        System.out.println();
        System.out.println("成交记录");
        System.out.printf("| %-1s | %-15s | %-8s | %-16s | %-16s | %-4s | %-8s | %-8s | %-8s | %-6s |%n",
            "序号", "成交时间", "交易对", "订单ID", "成交ID", "方向", "价格", "数量", "手续费", "Maker");
        for (OrderSymbol item : list){
            trade.queryTradesByOrderId(item.getOrderId(), item.getSymbol());
        }

    }

    @Data
    class OrderSymbol {
        public Long orderId;

        public String symbol;

        public OrderSymbol(Long orderId, String symbol) {
            this.orderId = orderId;
            this.symbol = symbol;
        }
    }
}
