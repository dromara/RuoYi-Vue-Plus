package org.dromara.quant.account.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.quant.account.domain.QuantTrade;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 成交记录展示对象。
 */
@Data
@AutoMapper(target = QuantTrade.class)
public class QuantTradeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 主键。
     */
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private Long id;
    /**
     * 交易凭证主键。
     */
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private Long credentialId;
    /**
     * 客户名称。
     */
    private String customerName;
    /**
     * 用户主键。
     */
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private Long userId;
    /**
     * 用户名称。
     */
    @Translation(type = TransConstant.USER_ID_TO_NAME, mapper = "userId")
    private String userName;
    /**
     * 交易对，例如 BTCUSDT。
     */
    private String symbol;
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
     * 已实现盈亏。
     */
    private BigDecimal realizedPnl;
    /**
     * 数据库中的成交时间（UTC）。
     */
    @JsonIgnore
    private LocalDateTime tradeTime;
    /**
     * 交易所成交主键。
     */
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private Long exchangeTradeId;
    /**
     * 交易所订单主键。
     */
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private Long exchangeOrderId;

    /**
     * 返回给前端的成交时间（北京时间）。
     */
    @JsonProperty("tradeTime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    public OffsetDateTime getTradeTimeBeijing() {
        if (tradeTime == null) {
            return null;
        }
        return tradeTime.atOffset(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(8));
    }
}
