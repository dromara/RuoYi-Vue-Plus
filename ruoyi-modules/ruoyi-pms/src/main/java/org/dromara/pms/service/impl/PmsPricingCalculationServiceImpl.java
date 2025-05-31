package org.dromara.pms.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.pms.domain.PmsPricingCalculation;
import org.dromara.pms.domain.PmsRoomPricingRule;
import org.dromara.pms.domain.PmsRoomType;
import org.dromara.pms.domain.bo.PmsPricingCalculationBo;
import org.dromara.pms.domain.vo.PmsPricingCalculationVo;
import org.dromara.pms.mapper.PmsPricingCalculationMapper;
import org.dromara.pms.mapper.PmsRoomTypeMapper;
import org.dromara.pms.service.IPmsPricingCalculationService;
import org.dromara.pms.service.IPmsRoomPricingRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 价格计算历史Service业务层处理
 *
 * @author PMS
 * @date 2024-12-01
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PmsPricingCalculationServiceImpl implements IPmsPricingCalculationService {

    private final PmsPricingCalculationMapper baseMapper;
    private final IPmsRoomPricingRuleService pricingRuleService;
    private final PmsRoomTypeMapper roomTypeMapper;

    /**
     * 查询价格计算历史
     */
    @Override
    public PmsPricingCalculationVo queryById(Long calculationId) {
        return baseMapper.selectVoById(calculationId);
    }

    /**
     * 查询价格计算历史列表
     */
    @Override
    public TableDataInfo<PmsPricingCalculationVo> queryPageList(PmsPricingCalculationBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsPricingCalculation> lqw = buildQueryWrapper(bo);
        Page<PmsPricingCalculationVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询价格计算历史列表
     */
    @Override
    public List<PmsPricingCalculationVo> queryList(PmsPricingCalculationBo bo) {
        LambdaQueryWrapper<PmsPricingCalculation> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<PmsPricingCalculation> buildQueryWrapper(PmsPricingCalculationBo bo) {
        LambdaQueryWrapper<PmsPricingCalculation> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), PmsPricingCalculation::getTenantId, bo.getTenantId());
        lqw.eq(bo.getDeptId() != null, PmsPricingCalculation::getDeptId, bo.getDeptId());
        lqw.eq(bo.getRoomTypeId() != null, PmsPricingCalculation::getRoomTypeId, bo.getRoomTypeId());
        lqw.ge(bo.getCheckInDate() != null, PmsPricingCalculation::getCheckInDate, bo.getCheckInDate());
        lqw.le(bo.getCheckOutDate() != null, PmsPricingCalculation::getCheckOutDate, bo.getCheckOutDate());
        lqw.eq(StringUtils.isNotBlank(bo.getChannelCode()), PmsPricingCalculation::getChannelCode, bo.getChannelCode());
        lqw.eq(StringUtils.isNotBlank(bo.getMemberLevel()), PmsPricingCalculation::getMemberLevel, bo.getMemberLevel());
        lqw.eq(bo.getOrderId() != null, PmsPricingCalculation::getOrderId, bo.getOrderId());
        lqw.eq(bo.getIsFinalBooking() != null, PmsPricingCalculation::getIsFinalBooking, bo.getIsFinalBooking());
        lqw.orderByDesc(PmsPricingCalculation::getCalculationTime);
        return lqw;
    }

    /**
     * 新增价格计算历史
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(PmsPricingCalculationBo bo) {
        PmsPricingCalculation add = MapstructUtils.convert(bo, PmsPricingCalculation.class);
        return baseMapper.insert(add) > 0;
    }

    /**
     * 修改价格计算历史
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(PmsPricingCalculationBo bo) {
        PmsPricingCalculation update = MapstructUtils.convert(bo, PmsPricingCalculation.class);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 批量删除价格计算历史
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 计算房间价格
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> calculatePrice(String tenantId, Long deptId, Long roomTypeId,
            LocalDate checkInDate, LocalDate checkOutDate, Integer numAdults, Integer numChildren,
            String channelCode, String memberLevel, Integer advanceBookingDays, Boolean saveHistory) {

        // 获取房型基础价格
        PmsRoomType roomType = roomTypeMapper.selectById(roomTypeId);
        if (roomType == null) {
            throw new ServiceException("房型不存在");
        }

        BigDecimal basePrice = roomType.getDefaultPrice();
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("房型基础价格未设置");
        }

        // 计算入住天数
        long stayDays = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (stayDays <= 0) {
            throw new ServiceException("入住天数必须大于0");
        }

        // 获取适用的价格规则
        List<PmsRoomPricingRule> applicableRules = pricingRuleService.getApplicableRules(
                tenantId, deptId, roomTypeId, checkInDate, checkOutDate,
                channelCode, memberLevel, numAdults + (numChildren != null ? numChildren : 0), advanceBookingDays);

        // 按优先级排序规则
        applicableRules.sort((r1, r2) -> r2.getPriority().compareTo(r1.getPriority()));

        // 计算最终价格
        BigDecimal finalPrice = basePrice;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<Map<String, Object>> appliedRules = new ArrayList<>();

        for (PmsRoomPricingRule rule : applicableRules) {
            // 检查规则是否可以组合
            if (!appliedRules.isEmpty() && !rule.getIsCombinable()) {
                continue;
            }

            BigDecimal adjustment = calculateRuleAdjustment(rule, basePrice, finalPrice);
            if (adjustment.compareTo(BigDecimal.ZERO) != 0) {
                // 应用规则
                BigDecimal newPrice = applyPriceAdjustment(finalPrice, rule.getPriceAdjustmentType(), adjustment);

                // 检查最低价格限制
                if (rule.getMinFinalPrice() != null && newPrice.compareTo(rule.getMinFinalPrice()) < 0) {
                    newPrice = rule.getMinFinalPrice();
                }

                // 检查最大折扣限制
                BigDecimal discount = finalPrice.subtract(newPrice);
                if (rule.getMaxDiscountAmount() != null && discount.compareTo(rule.getMaxDiscountAmount()) > 0) {
                    newPrice = finalPrice.subtract(rule.getMaxDiscountAmount());
                    discount = rule.getMaxDiscountAmount();
                }

                // 记录应用的规则
                Map<String, Object> appliedRule = new HashMap<>();
                appliedRule.put("ruleId", rule.getRuleId());
                appliedRule.put("ruleName", rule.getName());
                appliedRule.put("adjustmentType", rule.getPriceAdjustmentType());
                appliedRule.put("adjustmentValue", rule.getAdjustmentValue());
                appliedRule.put("originalPrice", finalPrice);
                appliedRule.put("adjustedPrice", newPrice);
                appliedRule.put("discount", discount);
                appliedRules.add(appliedRule);

                finalPrice = newPrice;
                totalDiscount = totalDiscount.add(discount);

                // 更新规则使用次数
                pricingRuleService.updateRuleUsageCount(rule.getRuleId(), 1);
            }
        }

        // 计算总价格（按天数）
        BigDecimal totalPrice = finalPrice.multiply(BigDecimal.valueOf(stayDays));
        BigDecimal totalDiscountAmount = totalDiscount.multiply(BigDecimal.valueOf(stayDays));

        // 构建计算上下文
        Map<String, Object> calculationContext = new HashMap<>();
        calculationContext.put("stayDays", stayDays);
        calculationContext.put("numAdults", numAdults);
        calculationContext.put("numChildren", numChildren);
        calculationContext.put("advanceBookingDays", advanceBookingDays);
        calculationContext.put("roomTypeName", roomType.getTypeName());

        // 保存计算历史
        if (saveHistory) {
            PmsPricingCalculation calculation = new PmsPricingCalculation();
            calculation.setTenantId(tenantId);
            calculation.setDeptId(deptId);
            calculation.setRoomTypeId(roomTypeId);
            calculation.setCheckInDate(checkInDate);
            calculation.setCheckOutDate(checkOutDate);
            calculation.setNumAdults(numAdults);
            calculation.setNumChildren(numChildren);
            calculation.setChannelCode(channelCode);
            calculation.setMemberLevel(memberLevel);
            calculation.setAdvanceBookingDays(advanceBookingDays);
            calculation.setBasePrice(basePrice);
            calculation.setFinalPrice(finalPrice);
            calculation.setTotalDiscount(totalDiscount);
            calculation.setAppliedRules(appliedRules);
            calculation.setCalculationContext(calculationContext);
            calculation.setCalculationTime(LocalDateTime.now());
            calculation.setCalculationSource("manual");
            calculation.setIsFinalBooking(false);

            baseMapper.insert(calculation);
        }

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("basePrice", basePrice);
        result.put("finalPrice", finalPrice);
        result.put("totalPrice", totalPrice);
        result.put("totalDiscount", totalDiscountAmount);
        result.put("stayDays", stayDays);
        result.put("appliedRules", appliedRules);
        result.put("calculationContext", calculationContext);

        return result;
    }

    /**
     * 计算规则调整值
     */
    private BigDecimal calculateRuleAdjustment(PmsRoomPricingRule rule, BigDecimal basePrice, BigDecimal currentPrice) {
        return rule.getAdjustmentValue();
    }

    /**
     * 应用价格调整
     */
    private BigDecimal applyPriceAdjustment(BigDecimal currentPrice, String adjustmentType,
            BigDecimal adjustmentValue) {
        switch (adjustmentType) {
            case "fixed_amount":
                return currentPrice.add(adjustmentValue);
            case "percentage":
                BigDecimal percentage = adjustmentValue.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                return currentPrice.multiply(BigDecimal.ONE.add(percentage));
            case "fixed_price":
                return adjustmentValue;
            case "discount_amount":
                return currentPrice.subtract(adjustmentValue);
            case "discount_percentage":
                BigDecimal discountPercentage = adjustmentValue.divide(BigDecimal.valueOf(100), 4,
                        RoundingMode.HALF_UP);
                return currentPrice.multiply(BigDecimal.ONE.subtract(discountPercentage));
            default:
                return currentPrice;
        }
    }

    /**
     * 批量计算价格
     */
    @Override
    public List<Map<String, Object>> batchCalculatePrice(List<PmsPricingCalculationBo> calculations) {
        return calculations.stream()
                .map(calc -> calculatePrice(calc.getTenantId(), calc.getDeptId(), calc.getRoomTypeId(),
                        calc.getCheckInDate(), calc.getCheckOutDate(), calc.getNumAdults(), calc.getNumChildren(),
                        calc.getChannelCode(), calc.getMemberLevel(), calc.getAdvanceBookingDays(), false))
                .collect(Collectors.toList());
    }

    /**
     * 查询价格趋势数据
     */
    @Override
    public List<Map<String, Object>> getPriceTrend(String tenantId, Long deptId, Long roomTypeId,
            LocalDate startDate, LocalDate endDate) {
        return baseMapper.selectPriceTrend(tenantId, deptId, roomTypeId, startDate, endDate);
    }

    /**
     * 查询规则效果分析数据
     */
    @Override
    public List<Map<String, Object>> getRuleEffectAnalysis(String tenantId, Long deptId, Long ruleId,
            LocalDate startDate, LocalDate endDate) {
        return baseMapper.selectRuleEffectAnalysis(tenantId, deptId, ruleId, startDate, endDate);
    }

    /**
     * 查询收益分析数据
     */
    @Override
    public List<Map<String, Object>> getRevenueAnalysis(String tenantId, Long deptId,
            LocalDate startDate, LocalDate endDate) {
        return baseMapper.selectRevenueAnalysis(tenantId, deptId, startDate, endDate);
    }

    /**
     * 查询平均价格
     */
    @Override
    public Map<String, Object> getAveragePrice(String tenantId, Long deptId, Long roomTypeId,
            LocalDate startDate, LocalDate endDate) {
        return baseMapper.selectAveragePrice(tenantId, deptId, roomTypeId, startDate, endDate);
    }

    /**
     * 查询价格分布统计
     */
    @Override
    public List<Map<String, Object>> getPriceDistribution(String tenantId, Long deptId, Long roomTypeId,
            LocalDate startDate, LocalDate endDate) {
        return baseMapper.selectPriceDistribution(tenantId, deptId, roomTypeId, startDate, endDate);
    }

    /**
     * 查询渠道价格对比
     */
    @Override
    public List<Map<String, Object>> getChannelPriceComparison(String tenantId, Long deptId, Long roomTypeId,
            LocalDate startDate, LocalDate endDate) {
        return baseMapper.selectChannelPriceComparison(tenantId, deptId, roomTypeId, startDate, endDate);
    }

    /**
     * 清理过期的计算记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer cleanExpiredRecords(LocalDateTime beforeDate) {
        return baseMapper.deleteExpiredRecords(beforeDate);
    }

    /**
     * 导出价格计算历史数据
     */
    @Override
    public List<PmsPricingCalculationVo> exportList(PmsPricingCalculationBo bo) {
        LambdaQueryWrapper<PmsPricingCalculation> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }
}
