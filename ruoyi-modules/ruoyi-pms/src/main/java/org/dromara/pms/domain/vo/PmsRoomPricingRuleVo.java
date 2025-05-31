package org.dromara.pms.domain.vo;

import org.dromara.pms.domain.PmsRoomPricingRule;
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
import java.util.List;

/**
 * 房间价格规则视图对象 pms_room_pricing_rules
 *
 * @author PMS
 * @date 2024-12-01
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = PmsRoomPricingRule.class)
public class PmsRoomPricingRuleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规则ID
     */
    @ExcelProperty(value = "规则ID")
    private Long ruleId;

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
     * 规则名称
     */
    @ExcelProperty(value = "规则名称")
    private String name;

    /**
     * 规则描述
     */
    @ExcelProperty(value = "规则描述")
    private String description;

    /**
     * 适用房型ID
     */
    @ExcelProperty(value = "适用房型ID")
    private Long roomTypeId;

    /**
     * 房型名称
     */
    @ExcelProperty(value = "房型名称")
    private String roomTypeName;

    /**
     * 适用日期范围开始
     */
    @ExcelProperty(value = "适用开始日期")
    private LocalDate dateRangeStart;

    /**
     * 适用日期范围结束
     */
    @ExcelProperty(value = "适用结束日期")
    private LocalDate dateRangeEnd;

    /**
     * 适用星期列表
     */
    private List<Integer> daysOfWeek;

    /**
     * 适用星期显示文本
     */
    @ExcelProperty(value = "适用星期")
    private String daysOfWeekText;

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
     * 最小入住天数
     */
    @ExcelProperty(value = "最小入住天数")
    private Integer minLengthOfStay;

    /**
     * 最大入住天数
     */
    @ExcelProperty(value = "最大入住天数")
    private Integer maxLengthOfStay;

    /**
     * 最小提前预订天数
     */
    @ExcelProperty(value = "最小提前预订天数")
    private Integer advanceBookingDaysMin;

    /**
     * 最大提前预订天数
     */
    @ExcelProperty(value = "最大提前预订天数")
    private Integer advanceBookingDaysMax;

    /**
     * 渠道限制列表
     */
    private List<String> channelRestrictions;

    /**
     * 渠道限制显示文本
     */
    @ExcelProperty(value = "渠道限制")
    private String channelRestrictionsText;

    /**
     * 最小客人数
     */
    @ExcelProperty(value = "最小客人数")
    private Integer guestCountMin;

    /**
     * 最大客人数
     */
    @ExcelProperty(value = "最大客人数")
    private Integer guestCountMax;

    /**
     * 会员等级限制列表
     */
    private List<String> memberLevelRestrictions;

    /**
     * 会员等级限制显示文本
     */
    @ExcelProperty(value = "会员等级限制")
    private String memberLevelRestrictionsText;

    /**
     * 优先级
     */
    @ExcelProperty(value = "优先级")
    private Integer priority;

    /**
     * 规则状态
     */
    @ExcelProperty(value = "规则状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "pms_rule_status")
    private String status;

    /**
     * 规则状态文本
     */
    private String statusText;

    /**
     * 规则生效开始日期
     */
    @ExcelProperty(value = "生效开始日期")
    private LocalDate effectiveStartDate;

    /**
     * 规则生效结束日期
     */
    @ExcelProperty(value = "生效结束日期")
    private LocalDate effectiveEndDate;

    /**
     * 最大折扣金额
     */
    @ExcelProperty(value = "最大折扣金额")
    private BigDecimal maxDiscountAmount;

    /**
     * 最低最终价格
     */
    @ExcelProperty(value = "最低最终价格")
    private BigDecimal minFinalPrice;

    /**
     * 是否可与其他规则组合
     */
    @ExcelProperty(value = "可组合")
    private Boolean isCombinable;

    /**
     * 使用次数限制
     */
    @ExcelProperty(value = "使用次数限制")
    private Integer usageLimit;

    /**
     * 已使用次数
     */
    @ExcelProperty(value = "已使用次数")
    private Integer usedCount;

    /**
     * 剩余使用次数
     */
    @ExcelProperty(value = "剩余使用次数")
    private Integer remainingUsage;

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
