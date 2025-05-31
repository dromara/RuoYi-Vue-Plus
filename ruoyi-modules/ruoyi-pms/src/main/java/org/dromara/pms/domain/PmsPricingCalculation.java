package org.dromara.pms.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 价格计算历史对象 pms_pricing_calculations
 *
 * @author PMS
 * @date 2024-12-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_pricing_calculations")
public class PmsPricingCalculation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 计算记录ID
     */
    @TableId(value = "calculation_id", type = IdType.AUTO)
    private Long calculationId;

    /**
     * 租户ID
     */
    @NotBlank(message = "租户ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String tenantId;

    /**
     * 部门ID (门店)
     */
    @NotNull(message = "部门ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long deptId;

    /**
     * 房型ID
     */
    @NotNull(message = "房型ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long roomTypeId;

    /**
     * 入住日期
     */
    @NotNull(message = "入住日期不能为空", groups = { AddGroup.class, EditGroup.class })
    private LocalDate checkInDate;

    /**
     * 离店日期
     */
    @NotNull(message = "离店日期不能为空", groups = { AddGroup.class, EditGroup.class })
    private LocalDate checkOutDate;

    /**
     * 成人数
     */
    @NotNull(message = "成人数不能为空", groups = { AddGroup.class, EditGroup.class })
    @Min(value = 1, message = "成人数不能小于1")
    private Integer numAdults;

    /**
     * 儿童数
     */
    @Min(value = 0, message = "儿童数不能小于0")
    private Integer numChildren;

    /**
     * 渠道代码
     */
    private String channelCode;

    /**
     * 会员等级
     */
    private String memberLevel;

    /**
     * 提前预订天数
     */
    @Min(value = 0, message = "提前预订天数不能小于0")
    private Integer advanceBookingDays;

    /**
     * 基础价格
     */
    @NotNull(message = "基础价格不能为空", groups = { AddGroup.class, EditGroup.class })
    @DecimalMin(value = "0.00", message = "基础价格不能小于0")
    @Digits(integer = 10, fraction = 2, message = "基础价格格式不正确")
    private BigDecimal basePrice;

    /**
     * 最终价格
     */
    @NotNull(message = "最终价格不能为空", groups = { AddGroup.class, EditGroup.class })
    @DecimalMin(value = "0.00", message = "最终价格不能小于0")
    @Digits(integer = 10, fraction = 2, message = "最终价格格式不正确")
    private BigDecimal finalPrice;

    /**
     * 总折扣金额
     */
    @DecimalMin(value = "0.00", message = "总折扣金额不能小于0")
    @Digits(integer = 10, fraction = 2, message = "总折扣金额格式不正确")
    private BigDecimal totalDiscount;

    /**
     * 应用的规则详情 (JSON)
     */
    private String appliedRulesJson;

    /**
     * 计算上下文 (JSON)
     */
    private String calculationContextJson;

    /**
     * 计算时间
     */
    @NotNull(message = "计算时间不能为空", groups = { AddGroup.class, EditGroup.class })
    private LocalDateTime calculationTime;

    /**
     * 计算来源
     */
    private String calculationSource;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 是否为最终预订
     */
    private Boolean isFinalBooking;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    // 非数据库字段，用于前端展示和查询

    /**
     * 房型名称
     */
    @TableField(exist = false)
    private String roomTypeName;

    /**
     * 渠道名称
     */
    @TableField(exist = false)
    private String channelName;

    /**
     * 订单号
     */
    @TableField(exist = false)
    private String orderNumber;

    /**
     * 入住天数
     */
    @TableField(exist = false)
    private Integer stayDays;

    /**
     * 折扣率
     */
    @TableField(exist = false)
    private BigDecimal discountRate;

    /**
     * 应用的规则列表 (解析JSON后的数据)
     */
    @TableField(exist = false)
    private List<Map<String, Object>> appliedRules;

    /**
     * 计算上下文 (解析JSON后的数据)
     */
    @TableField(exist = false)
    private Map<String, Object> calculationContext;

    /**
     * 计算来源文本
     */
    @TableField(exist = false)
    private String calculationSourceText;

    /**
     * 价格变化金额
     */
    @TableField(exist = false)
    private BigDecimal priceChange;

    /**
     * 价格变化率
     */
    @TableField(exist = false)
    private BigDecimal priceChangeRate;

    /**
     * 是否有折扣
     */
    @TableField(exist = false)
    private Boolean hasDiscount;

    /**
     * 规则数量
     */
    @TableField(exist = false)
    private Integer ruleCount;
}
