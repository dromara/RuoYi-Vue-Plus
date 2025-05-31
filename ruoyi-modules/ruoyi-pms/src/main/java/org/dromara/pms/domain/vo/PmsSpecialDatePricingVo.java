package org.dromara.pms.domain.vo;

import org.dromara.pms.domain.PmsSpecialDatePricing;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * 特殊日期价格视图对象 pms_special_date_pricing
 *
 * @author PMS
 * @date 2024-12-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsSpecialDatePricing.class)
public class PmsSpecialDatePricingVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 特殊日期ID
     */
    @ExcelProperty(value = "特殊日期ID")
    private Long specialDateId;

    /**
     * 租户ID
     */
    @ExcelProperty(value = "租户ID")
    private String tenantId;

    /**
     * 部门ID (门店)
     */
    @ExcelProperty(value = "部门ID")
    private Long deptId;

    /**
     * 房型ID
     */
    @ExcelProperty(value = "房型ID")
    private Long roomTypeId;

    /**
     * 房型名称
     */
    @ExcelProperty(value = "房型名称")
    private String roomTypeName;

    /**
     * 特殊日期
     */
    @ExcelProperty(value = "特殊日期")
    private LocalDate specialDate;

    /**
     * 日期类型
     */
    @ExcelProperty(value = "日期类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "pms_special_date_type")
    private String dateType;

    /**
     * 日期类型文本
     */
    private String dateTypeText;

    /**
     * 日期名称
     */
    @ExcelProperty(value = "日期名称")
    private String dateName;

    /**
     * 价格调整类型
     */
    @ExcelProperty(value = "调整类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "pms_price_adjustment_type")
    private String priceAdjustmentType;

    /**
     * 价格调整类型文本
     */
    private String priceAdjustmentTypeText;

    /**
     * 调整值
     */
    @ExcelProperty(value = "调整值")
    private BigDecimal adjustmentValue;

    /**
     * 优先级
     */
    @ExcelProperty(value = "优先级")
    private Integer priority;

    /**
     * 状态
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "pms_special_date_status")
    private String status;

    /**
     * 状态文本
     */
    private String statusText;

    /**
     * 描述
     */
    @ExcelProperty(value = "描述")
    private String description;

    /**
     * 是否生效
     */
    private Boolean isEffective;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;
}
