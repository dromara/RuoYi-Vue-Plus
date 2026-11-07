package org.dromara.quant.execution;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable exchange fill facts plus the mutable attribution processing state.
 */
@Data
public class StrategyFillJournalRecord {
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
    private String attributionStatus;
    private String statusReason;
    private Integer attemptCount;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
