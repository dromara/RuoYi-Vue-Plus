package org.dromara.pms.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.pms.domain.PmsRoomPricingRule;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 房间价格规则业务对象 pms_room_pricing_rules
 *
 * @author PMS
 * @date 2024-12-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsRoomPricingRule.class, reverseConvertGenerate = false)
public class PmsRoomPricingRuleBo extends BaseEntity {

    /**
     * 规则ID
     */
    private Long ruleId;

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
     * 规则名称
     */
    @NotBlank(message = "规则名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 100, message = "规则名称长度不能超过100个字符")
    private String name;

    /**
     * 规则描述
     */
    @Size(max = 500, message = "规则描述长度不能超过500个字符")
    private String description;

    /**
     * 适用房型ID (NULL表示全部房型)
     */
    private Long roomTypeId;

    /**
     * 适用日期范围开始
     */
    private LocalDate dateRangeStart;

    /**
     * 适用日期范围结束
     */
    private LocalDate dateRangeEnd;

    /**
     * 适用星期列表
     */
    private List<Integer> daysOfWeek;

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
     * 最小提前预订天数
     */
    @Min(value = 0, message = "最小提前预订天数不能小于0")
    private Integer advanceBookingDaysMin;

    /**
     * 最大提前预订天数
     */
    @Min(value = 0, message = "最大提前预订天数不能小于0")
    private Integer advanceBookingDaysMax;

    /**
     * 渠道限制列表
     */
    private List<String> channelRestrictions;

    /**
     * 最小客人数
     */
    @Min(value = 1, message = "最小客人数不能小于1")
    private Integer guestCountMin;

    /**
     * 最大客人数
     */
    @Min(value = 1, message = "最大客人数不能小于1")
    private Integer guestCountMax;

    /**
     * 会员等级限制列表
     */
    private List<String> memberLevelRestrictions;

    /**
     * 优先级 (数字越大优先级越高)
     */
    @NotNull(message = "优先级不能为空", groups = { AddGroup.class, EditGroup.class })
    @Min(value = 0, message = "优先级不能小于0")
    private Integer priority;

    /**
     * 规则状态
     */
    @NotBlank(message = "规则状态不能为空", groups = { AddGroup.class, EditGroup.class })
    private String status;

    /**
     * 规则生效开始日期
     */
    private LocalDate effectiveStartDate;

    /**
     * 规则生效结束日期
     */
    private LocalDate effectiveEndDate;

    /**
     * 最大折扣金额
     */
    @DecimalMin(value = "0.00", message = "最大折扣金额不能小于0")
    @Digits(integer = 8, fraction = 2, message = "最大折扣金额格式不正确")
    private BigDecimal maxDiscountAmount;

    /**
     * 最低最终价格
     */
    @DecimalMin(value = "0.00", message = "最低最终价格不能小于0")
    @Digits(integer = 8, fraction = 2, message = "最低最终价格格式不正确")
    private BigDecimal minFinalPrice;

    /**
     * 是否可与其他规则组合
     */
    private Boolean isCombinable;

    /**
     * 使用次数限制
     */
    @Min(value = 1, message = "使用次数限制不能小于1")
    private Integer usageLimit;

    /**
     * 已使用次数
     */
    private Integer usedCount;
}
