package org.dromara.binance;

import com.binance.connector.client.common.ApiException;
import com.binance.connector.client.common.ApiResponse;
import com.binance.connector.client.common.configuration.ClientConfiguration;
import com.binance.connector.client.common.configuration.SignatureConfiguration;
import com.binance.connector.client.derivatives_trading_usds_futures.rest.DerivativesTradingUsdsFuturesRestApiUtil;
import com.binance.connector.client.derivatives_trading_usds_futures.rest.api.DerivativesTradingUsdsFuturesRestApi;
import com.binance.connector.client.derivatives_trading_usds_futures.rest.model.CurrentAllOpenOrdersResponse;
import java.io.IOException;

/** API examples for TradeApi */
public class CurrentAllOpenOrdersExample {
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
     * Current All Open Orders (USER_DATA)
     *
     * <p>Get all open orders on a symbol. Weight: **1** for a single symbol; **40** when the symbol
     * parameter is omitted **Careful** when accessing this with no symbol. Security Type: USER_DATA
     * Notes: - If the symbol is not sent, orders for all symbols will be returned in an array.
     *
     * @throws ApiException if the Api call fails
     */
    public void currentAllOpenOrdersExample() throws ApiException, IOException {
        String symbol = "BRUSDT";
        Long recvWindow = 5000L;
        ApiResponse<CurrentAllOpenOrdersResponse> response = getApi().currentAllOpenOrders(symbol, recvWindow);
        System.out.println(response.getData());
    }

    public static void main() throws IOException {
        CurrentAllOpenOrdersExample example = new CurrentAllOpenOrdersExample();
        example.getApi();
        example.currentAllOpenOrdersExample();
    }
}
