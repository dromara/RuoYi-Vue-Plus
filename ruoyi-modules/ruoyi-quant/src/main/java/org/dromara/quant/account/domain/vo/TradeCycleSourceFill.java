package org.dromara.quant.account.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Verified exchange fill facts enriched with immutable order-role sources. */
@Data
public class TradeCycleSourceFill {
    private Long id;
    private Long credentialId;
    private String symbol;
    private Long exchangeTradeId;
    private Long exchangeOrderId;
    private String side;
    private String positionSide;
    private String exchangePositionSide;
    private String fillRole;
    private String positionCycleKey;
    private Boolean reduceOnly;
    private Boolean closePosition;
    private BigDecimal orderExecutedQuantity;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal commission;
    private String commissionAsset;
    private BigDecimal realizedPnl;
    private LocalDateTime tradeTime;
    private Boolean sourceConflict;
    private Long strategyInstanceId;
}
