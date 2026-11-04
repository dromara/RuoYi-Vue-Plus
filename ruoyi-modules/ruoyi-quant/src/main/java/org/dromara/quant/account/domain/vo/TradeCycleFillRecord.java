package org.dromara.quant.account.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Portion of one immutable exchange fill assigned to a position cycle. */
@Data
public class TradeCycleFillRecord {
    private Long id;
    private Long cycleId;
    private Long credentialId;
    private String symbol;
    private Long exchangeTradeId;
    private Long exchangeOrderId;
    private Integer allocationSeq;
    private String fillRole;
    private String positionSide;
    private BigDecimal allocatedQuantity;
    private BigDecimal price;
    private BigDecimal commission;
    private String commissionAsset;
    private BigDecimal realizedPnl;
    private LocalDateTime tradeTime;
}
