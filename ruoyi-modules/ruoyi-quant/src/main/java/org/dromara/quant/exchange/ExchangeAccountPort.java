package org.dromara.quant.exchange;

import java.util.List;

/**
 * 交易所账户/持仓查询端口。
 */
public interface ExchangeAccountPort {
    ExchangeGateway.ConnectivityResult testConnectivity();

    ExchangeGateway.ConnectivityResult validateCredential(ExchangeGateway.Credential credential);

    ExchangeGateway.AccountSnapshot queryAccount(ExchangeGateway.Credential credential);

    /**
     * 余额资金覆盖校准必须从本次调用之后发起，不能加入此前的共享请求。
     */
    default ExchangeGateway.AccountSnapshot queryAccountFresh(ExchangeGateway.Credential credential) {
        return queryAccount(credential);
    }

    /**
     * Account facts fetched as part of a position verification receive higher REST scheduling priority
     * than informational balance refreshes.
     */
    default ExchangeGateway.AccountSnapshot queryAccountForPosition(ExchangeGateway.Credential credential) {
        return queryAccountFresh(credential);
    }

    List<ExchangeGateway.PositionSnapshot> queryPositions(ExchangeGateway.Credential credential, String symbol);

    List<ExchangeGateway.IncomeSnapshot> queryIncomeHistory(ExchangeGateway.Credential credential, String symbol, String incomeType, ExchangeGateway.TimeRange range, Long limit);

    ExchangeGateway.PositionMode queryPositionMode(ExchangeGateway.Credential credential);

    /**
     * 设置账户持仓模式。
     */
    void setPositionMode(ExchangeGateway.Credential credential, boolean hedgeMode);

    /**
     * 设置交易对保证金模式。
     */
    void setMarginType(ExchangeGateway.Credential credential, String symbol, String marginType);

    default SymbolTradingSettings querySymbolTradingSettings(ExchangeGateway.Credential credential, String symbol) {
        throw new UnsupportedOperationException("交易所未提供可验证的账户交易对杠杆/保证金模式回查");
    }

    /**
     * 设置交易对杠杆。
     */
    void setLeverage(ExchangeGateway.Credential credential, String symbol, int leverage);

    /**
     * Fresh read-after-write of actual exchange margin mode and leverage.
     * The base implementation fails closed until an adapter explicitly supports
     * verified results even when account position quantity is zero.
     */
    record SymbolTradingSettings(String symbol, String marginMode, int leverage) {
    }
}
