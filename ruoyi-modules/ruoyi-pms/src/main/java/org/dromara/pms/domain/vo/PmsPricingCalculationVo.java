package org.dromara.pms.domain.vo;

import org.dromara.pms.domain.PmsPricingCalculation;
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
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 价格计算历史视图对象 pms_pricing_calculations
 *
 * @author PMS
 * @date 2024-12-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsPricingCalculation.class)
public class PmsPricingCalculationVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 计算记录ID
     */
    @ExcelProperty(value = "计算记录ID")
    private Long calculationId;

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
     * 入住日期
     */
    @ExcelProperty(value = "入住日期")
    private LocalDate checkInDate;

    /**
     * 离店日期
     */
    @ExcelProperty(value = "离店日期")
    private LocalDate checkOutDate;

    /**
     * 入住天数
     */
    @ExcelProperty(value = "入住天数")
    private Integer stayDays;

    /**
     * 成人数
     */
    @ExcelProperty(value = "成人数")
    private Integer numAdults;

    /**
     * 儿童数
     */
    @ExcelProperty(value = "儿童数")
    private Integer numChildren;

    /**
     * 渠道代码
     */
    @ExcelProperty(value = "渠道代码")
    private String channelCode;

    /**
     * 渠道名称
     */
    @ExcelProperty(value = "渠道名称")
    private String channelName;

    /**
     * 会员等级
     */
    @ExcelProperty(value = "会员等级", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "pms_member_level")
    private String memberLevel;

    /**
     * 提前预订天数
     */
    @ExcelProperty(value = "提前预订天数")
    private Integer advanceBookingDays;

    /**
     * 基础价格
     */
    @ExcelProperty(value = "基础价格")
    private BigDecimal basePrice;

    /**
     * 最终价格
     */
    @ExcelProperty(value = "最终价格")
    private BigDecimal finalPrice;

    /**
     * 总折扣金额
     */
    @ExcelProperty(value = "总折扣金额")
    private BigDecimal totalDiscount;

    /**
     * 折扣率
     */
    @ExcelProperty(value = "折扣率")
    private BigDecimal discountRate;

    /**
     * 价格变化金额
     */
    @ExcelProperty(value = "价格变化金额")
    private BigDecimal priceChange;

    /**
     * 价格变化率
     */
    @ExcelProperty(value = "价格变化率")
    private BigDecimal priceChangeRate;

    /**
     * 应用的规则列表
     */
    private List<Map<String, Object>> appliedRules;

    /**
     * 规则数量
     */
    @ExcelProperty(value = "应用规则数量")
    private Integer ruleCount;

    /**
     * 计算上下文
     */
    private Map<String, Object> calculationContext;

    /**
     * 计算时间
     */
    @ExcelProperty(value = "计算时间")
    private LocalDateTime calculationTime;

    /**
     * 计算来源
     */
    @ExcelProperty(value = "计算来源", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "pms_calculation_source")
    private String calculationSource;

    /**
     * 计算来源文本
     */
    private String calculationSourceText;

    /**
     * 关联订单ID
     */
    @ExcelProperty(value = "关联订单ID")
    private Long orderId;

    /**
     * 订单号
     */
    @ExcelProperty(value = "订单号")
    private String orderNumber;

    /**
     * 是否为最终预订
     */
    @ExcelProperty(value = "是否最终预订")
    private Boolean isFinalBooking;

    /**
     * 是否有折扣
     */
    private Boolean hasDiscount;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;
}
