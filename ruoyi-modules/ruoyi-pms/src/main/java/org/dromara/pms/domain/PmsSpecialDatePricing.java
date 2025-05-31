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

/**
 * 特殊日期价格对象 pms_special_date_pricing
 *
 * @author PMS
 * @date 2024-12-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_special_date_pricing")
public class PmsSpecialDatePricing extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 特殊日期ID
     */
    @TableId(value = "special_date_id", type = IdType.AUTO)
    private Long specialDateId;

    /**
     * 租户ID
     */
    @NotBlank(message = "租户ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private String tenantId;

    /**
     * 部门ID (门店)
     */
    private Long deptId;

    /**
     * 房型ID (NULL表示全部房型)
     */
    private Long roomTypeId;

    /**
     * 特殊日期
     */
    @TableField("specific_date")
    @NotNull(message = "特殊日期不能为空", groups = { AddGroup.class, EditGroup.class })
    private LocalDate specialDate;

    /**
     * 日期范围开始
     */
    private LocalDate dateRangeStart;

    /**
     * 日期范围结束
     */
    private LocalDate dateRangeEnd;

    /**
     * 日期类型
     */
    @NotBlank(message = "日期类型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String dateType;

    /**
     * 日期名称
     */
    @TableField("name")
    @NotBlank(message = "日期名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 100, message = "日期名称长度不能超过100个字符")
    private String dateName;

    /**
     * 价格调整类型
     */
    @NotBlank(message = "价格调整类型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String priceAdjustmentType;

    /**
     * 调整值
     */
    @NotNull(message = "调整值不能为空", groups = { AddGroup.class, EditGroup.class })
    @DecimalMin(value = "0.00", message = "调整值不能小于0")
    @Digits(integer = 8, fraction = 2, message = "调整值格式不正确")
    private BigDecimal adjustmentValue;

    /**
     * 固定价格
     */
    @DecimalMin(value = "0.00", message = "固定价格不能小于0")
    @Digits(integer = 8, fraction = 2, message = "固定价格格式不正确")
    private BigDecimal fixedPrice;

    /**
     * 优先级
     */
    @NotNull(message = "优先级不能为空", groups = { AddGroup.class, EditGroup.class })
    @Min(value = 0, message = "优先级不能小于0")
    private Integer priority;

    /**
     * 状态
     */
    @NotBlank(message = "状态不能为空", groups = { AddGroup.class, EditGroup.class })
    private String status;

    /**
     * 描述
     */
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;

    /**
     * 是否每年重复
     */
    private Boolean isRecurringYearly;

    /**
     * 最小入住天数
     */
    @Min(value = 1, message = "最小入住天数不能小于1")
    private Integer minLengthOfStay;

    /**
     * 最大入住天数
     */
    @Min(value = 1, message = "最大入住天数不能小于1")
    private Integer maxLengthOfStay;

    /**
     * 渠道限制 (JSON数组)
     */
    private String channelRestrictionsJson;

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
     * 日期类型文本
     */
    @TableField(exist = false)
    private String dateTypeText;

    /**
     * 价格调整类型文本
     */
    @TableField(exist = false)
    private String priceAdjustmentTypeText;

    /**
     * 状态文本
     */
    @TableField(exist = false)
    private String statusText;

    /**
     * 是否生效
     */
    @TableField(exist = false)
    private Boolean isEffective;
}
