package org.dromara.quant.protection.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 持仓保护单本地账本。
 */
@Data
@TableName("quant_protection_order")
public class QuantProtectionOrder {
    /**
     * 主键。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 交易凭证主键。
     */
    private Long credentialId;
    /** 创建保护意图时冻结的策略来源；未知的历史来源保持为空。 */
    private Long strategyId;
    /** 创建保护意图时冻结的策略实例，不能按当前交易对反推旧周期归属。 */
    private Long strategyInstanceId;
    /**
     * 本地持仓主键。
     */
    private Long positionId;
    /**
     * 所属持仓周期，防止同一持仓行复用时认领旧订单。
     */
    private String positionCycleKey;
    /**
     * 所属保护目标版本。
     */
    private Long targetVersion;
    /** 本地保护意图首次持久化时间，Unix epoch 毫秒；历史数据为空时不得推断订单不存在。 */
    @TableField("created_at_epoch_ms")
    private Long createdAtEpochMillis;
    /**
     * 保护意图覆盖的最高台阶，非阶梯保护为0。
     */
    private Integer stepIndex;
    /**
     * 该条件单已精确确认的累计成交量，不因重复查询累加。
     */
    private BigDecimal executedQuantity;
    /**
     * 交易对，例如 BTCUSDT。
     */
    private String symbol;
    /**
     * 持仓方向，例如 LONG/SHORT/BOTH。
     */
    private String positionSide;
    /**
     * 交易所订单主键。
     */
    private Long exchangeOrderId;
    /**
     * 客户端订单号。
     */
    private String clientOrderId;
    /**
     * 保护类型，例如 HARD_STOP、LOCAL_STOP、FIRST_TP、STEP_TP。
     */
    private String protectionType;
    /**
     * 订单类型。
     */
    private String orderType;
    /**
     * 数量。
     */
    private BigDecimal quantity;
    /**
     * 条件单触发价格。
     */
    private BigDecimal stopPrice;
    /**
     * 当前状态。
     */
    private String status;
}
