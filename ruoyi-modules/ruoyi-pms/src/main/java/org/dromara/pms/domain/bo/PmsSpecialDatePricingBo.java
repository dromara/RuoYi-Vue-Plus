package org.dromara.pms.domain.bo;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.pms.domain.PmsSpecialDatePricing;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 特殊日期价格业务对象 pms_special_date_pricing
 *
 * @author PMS
 * @date 2024-12-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsSpecialDatePricing.class, reverseConvertGenerate = false)
public class PmsSpecialDatePricingBo extends BaseEntity {

    /**
     * 特殊日期ID
     */
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
    @NotNull(message = "特殊日期不能为空", groups = { AddGroup.class, EditGroup.class })
    private LocalDate specialDate;

    /**
     * 日期类型
     */
    @NotBlank(message = "日期类型不能为空", groups = { AddGroup.class, EditGroup.class })
    private String dateType;

    /**
     * 日期名称
     */
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
}
