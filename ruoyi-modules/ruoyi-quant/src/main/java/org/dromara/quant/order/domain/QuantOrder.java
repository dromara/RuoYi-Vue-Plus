package org.dromara.quant.order.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 本地订单账本。
 */
@Data
@TableName("quant_order")
public class QuantOrder {
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
     * 交易所订单主键。
     */
    private Long exchangeOrderId;
    /**
     * 客户端订单号。
     */
    private String clientOrderId;
    /**
     * 订单买卖方向，例如 BUY/SELL。
     */
    private String side;
    /**
     * 订单类型。
     */
    private String orderType;
    /**
     * 当前状态。
     */
    private String status;
    /**
     * 持仓方向，例如 LONG/SHORT/BOTH。
     */
    private String positionSide;
    /**
     * 交易所委托价格；市价单实际成交均价单独存储。
     */
    private BigDecimal price;
    /** 实际累计成交均价；未成交或交易所未提供时为空。 */
    private BigDecimal averagePrice;
    /**
     * 订单原始数量。
     */
    private BigDecimal originalQuantity;
    /**
     * 订单已成交数量。
     */
    private BigDecimal executedQuantity;
    /**
     * 是否为只减仓订单。
     */
    private Boolean reduceOnly;
    /** Binance 原始订单事实来源。 */
    private String reduceOnlySource;
    /** 是否触发后平掉该方向全部持仓。 */
    private Boolean closePosition;
    /** 开仓订单受理时冻结的杠杆。 */
    private Integer leverageSnapshot;
    /** 开仓订单受理时冻结的价格最小变动单位。 */
    private BigDecimal priceTickSizeSnapshot;
    /** 成交后核实到账户钱包余额；不代表该笔交易单独造成的余额变化。 */
    private BigDecimal postFillWalletBalance;
    /** 成交后余额核实时间（UTC）。 */
    private LocalDateTime postFillBalanceAt;
    /** 最近一次余额核实尝试时间，用于限频。 */
    private LocalDateTime postFillBalanceRequestedAt;
    /**
     * 执行幂等键，用于阻止重复交易请求。
     */
    private String idempotencyKey;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 最后更新时间。
     */
    private LocalDateTime updatedAt;
}
