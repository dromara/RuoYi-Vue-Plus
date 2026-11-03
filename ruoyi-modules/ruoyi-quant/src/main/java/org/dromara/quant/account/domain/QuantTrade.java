package org.dromara.quant.account.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 本地成交账本实体，保存交易所成交、手续费、盈亏和策略来源归因。
 */
@Data
@TableName("quant_trade")
public class QuantTrade {
    /**
     * 主键。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 交易凭证主键。
     */
    private Long credentialId;
    /**
     * 策略定义主键；为空表示非策略来源。
     */
    private Long strategyId;
    /**
     * 策略运行实例主键；为空表示非策略来源。
     */
    private Long strategyInstanceId;
    /**
     * 交易对，例如 BTCUSDT。
     */
    private String symbol;
    /**
     * 交易所成交主键。
     */
    private Long exchangeTradeId;
    /**
     * 交易所订单主键。
     */
    private Long exchangeOrderId;
    /**
     * 订单买卖方向，例如 BUY/SELL。
     */
    private String side;
    /**
     * 数量。
     */
    private BigDecimal quantity;
    /**
     * 订单或成交价格。
     */
    private BigDecimal price;
    /**
     * 交易手续费。
     */
    private BigDecimal commission;
    /**
     * 资金费字段；完整资金费事实以 quant_financial_event 为准。
     */
    private BigDecimal fundingFee;
    /**
     * 已实现盈亏。
     */
    private BigDecimal realizedPnl;
    /**
     * 成交时间。
     */
    private LocalDateTime tradeTime;
}
