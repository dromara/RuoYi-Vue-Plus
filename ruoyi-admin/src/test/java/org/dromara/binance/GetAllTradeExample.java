package org.dromara.binance;

import com.binance.connector.client.common.ApiException;
import com.binance.connector.client.common.ApiResponse;
import com.binance.connector.client.common.configuration.ClientConfiguration;
import com.binance.connector.client.common.configuration.SignatureConfiguration;
import com.binance.connector.client.derivatives_trading_usds_futures.rest.DerivativesTradingUsdsFuturesRestApiUtil;
import com.binance.connector.client.derivatives_trading_usds_futures.rest.api.DerivativesTradingUsdsFuturesRestApi;
import com.binance.connector.client.derivatives_trading_usds_futures.rest.model.AccountTradeListResponse;
import org.dromara.common.core.utils.DateUtils;

import java.io.IOException;

/** API examples for TradeApi */
public class GetAllTradeExample {
    private DerivativesTradingUsdsFuturesRestApi api;

    public DerivativesTradingUsdsFuturesRestApi getApi() {
        if (api == null) {
            ClientConfiguration clientConfiguration =
                    DerivativesTradingUsdsFuturesRestApiUtil.getClientConfiguration();
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
    public void cancelTradeExample() throws ApiException, IOException {
        String symbol = "BTCUSDT";
//        symbol = "";
        Long orderId = 1121238596337L;
        int diff = 60 * 60 * 24 * 1000 * 7;
        Long endTime = DateUtils.current();
        Long startTime = DateUtils.date(endTime - diff).getTime();
        System.out.println("endTime:" + DateUtils.date(endTime).toDateStr());
        System.out.println("startTime:" + DateUtils.date(startTime).toDateStr());
        startTime = null;
        endTime= null;
        Long fromId = 1L;
        Long limit = 500L;
        Long recvWindow = 5000L;
        ApiResponse<AccountTradeListResponse> response = getApi().accountTradeList(symbol, orderId, startTime, endTime, fromId, limit, recvWindow);
//        System.out.println(response.getData());
        AccountTradeListResponse allOrders = response.getData();
        allOrders.forEach(order -> {
            System.out.println(DateUtils.date(order.getTime()) + "  " + order.getSymbol());
        });

    }

    public void queryTradesByOrderId(Long orderId, String symbol) throws ApiException, IOException {
        if(orderId == null){
            orderId = 1119296222739L;
        }
        if(symbol.isBlank()){
            symbol = "BTCUSDT";
        }

        // 只传 symbol + orderId，其余全部 null
        Long startTime = null;
        Long endTime = null;
        Long fromId = null;   // ⚠️ 关键：不要传 fromId
        Long limit = 500L;
        Long recvWindow = 5000L;

        ApiResponse<AccountTradeListResponse> response = getApi().accountTradeList(symbol, orderId, startTime, endTime, fromId, limit, recvWindow);

        AccountTradeListResponse trades = response.getData();

        System.out.println(trades.get(0).toJson());
        int index = 1;
        for (var trade : trades) {
            String time = trade.getTime() == null ? "-" :
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date(trade.getTime()));

            System.out.printf("| %-2d | %-19s | %-10s | %-18s | %-18s | %-4s | %-10s | %-10s | %-10s | %-6s |%n",
                index++,
                time,
                safe(trade.getSymbol()),
                safe(trade.getOrderId()),
                safe(trade.getId()),          // 成交ID（exchangeTradeId）
                safe(trade.getSide()),
                safe(trade.getPrice()),
                safe(trade.getQty()),
                safe(trade.getCommission()),
                safe(trade.getMaker()));
        }
//        System.out.println("+----+---------------------+------------+--------------+--------------+------+------------+------------+------------+--------+");
//        System.out.println("共 " + trades.size() + " 笔成交");
    }

    private static String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    public static void main(String[] args) throws Exception {
        // 启动时先同步 Binance 服务器时间
        BinanceTimeSync.sync();

        GetAllTradeExample example = new GetAllTradeExample();
        example.getApi();
        example.cancelTradeExample();
        // ===== 表格输出 =====
        System.out.println();
//        System.out.println("成交记录（symbol=" + symbol + ", orderId=" + orderId + "）");
        System.out.printf("| %-1s | %-15s | %-8s | %-16s | %-16s | %-4s | %-8s | %-8s | %-8s | %-6s |%n",
            "序号", "成交时间", "交易对", "订单ID", "成交ID", "方向", "价格", "数量", "手续费", "Maker");
        example.queryTradesByOrderId(null, null);
    }
}
