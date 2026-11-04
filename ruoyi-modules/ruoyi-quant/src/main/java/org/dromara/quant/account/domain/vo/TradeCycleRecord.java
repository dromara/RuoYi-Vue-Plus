package org.dromara.quant.account.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One persisted position lifecycle, aggregated from immutable fill allocations. */
@Data
public class TradeCycleRecord {
    private Long id;
    private String cycleKey;
    private Long credentialId;
    private String symbol;
    private String positionSide;
    private String status;
    private String reconstructionSource;
    private String reconciliationStatus;
    private String reconciliationReason;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private BigDecimal openingQuantity;
    private BigDecimal openingNotional;
    private BigDecimal entryPrice;
    private BigDecimal closingQuantity;
    private BigDecimal closingNotional;
    private BigDecimal closePrice;
    private BigDecimal remainingQuantity;
    private BigDecimal realizedPnl;
    private String commissionDisplay;
    private String commissionByAssetJson;
    private Integer fillCount;
}
