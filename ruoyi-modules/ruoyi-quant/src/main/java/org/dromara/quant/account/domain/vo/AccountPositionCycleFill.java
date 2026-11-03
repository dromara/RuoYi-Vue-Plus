package org.dromara.quant.account.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成交周期投影使用的本地成交事实及其精确订单快照。
 */
@Data
public class AccountPositionCycleFill {
    private Long id;
    private Long credentialId;
    private String symbol;
    private Long exchangeTradeId;
    private Long exchangeOrderId;
    private String side;
    private String positionSide;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal commission;
    private String commissionAsset;
    private BigDecimal realizedPnl;
    private LocalDateTime tradeTime;
    private Integer leverageSnapshot;
    private BigDecimal priceTickSizeSnapshot;
    private BigDecimal postFillWalletBalance;
    private LocalDateTime postFillBalanceAt;
}
