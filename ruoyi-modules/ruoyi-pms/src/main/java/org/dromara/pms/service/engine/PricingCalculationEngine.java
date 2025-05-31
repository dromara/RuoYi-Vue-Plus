package org.dromara.pms.service.engine;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.pms.domain.PmsRoomPricingRule;
import org.dromara.pms.domain.PmsSpecialDatePricing;
import org.dromara.pms.domain.enums.PriceAdjustmentType;
import org.dromara.pms.service.cache.PricingCacheService;
import org.dromara.pms.service.IPmsRoomPricingRuleService;
import org.dromara.pms.service.IPmsSpecialDatePricingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * 价格计算引擎
 *
 * 提供高性能的价格计算服务，支持：
 * - 多规则叠加计算
 * - 并行计算优化
 * - 缓存机制
 * - 规则优先级处理
 * - 特殊日期价格处理
 *
 * @author ruoyi
 * @date 2024-12-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PricingCalculationEngine {

    private final PricingCacheService cacheService;
    private final IPmsRoomPricingRuleService pricingRuleService;
    private final IPmsSpecialDatePricingService specialDatePricingService;

    /**
     * 默认基础价格
     */
    private static final BigDecimal DEFAULT_BASE_PRICE = BigDecimal.valueOf(200.00);

    /**
     * 价格精度（小数点后2位）
     */
    private static final int PRICE_SCALE = 2;

    /**
     * 获取框架统一的异步执行器
     */
    private Executor getAsyncExecutor() {
        try {
            // 优先使用框架配置的线程池
            return SpringUtils.getBean("scheduledExecutorService");
        } catch (Exception e) {
            log.warn("获取框架线程池失败，使用默认线程池: {}", e.getMessage());
            return CompletableFuture.delayedExecutor(0, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 计算房间价格
     *
     * @param tenantId     租户ID
     * @param deptId       部门ID
     * @param roomTypeId   房型ID
     * @param checkInDate  入住日期
     * @param checkOutDate 退房日期
     * @param numAdults    成人数
     * @param numChildren  儿童数
     * @param channelCode  渠道代码
     * @param memberLevel  会员等级
     * @return 计算结果
     */
    public PricingCalculationResult calculatePrice(String tenantId, Long deptId, Long roomTypeId,
            LocalDate checkInDate, LocalDate checkOutDate,
            Integer numAdults, Integer numChildren,
            String channelCode, String memberLevel) {

        long startTime = System.currentTimeMillis();

        try {
            // 1. 生成缓存键并尝试从缓存获取结果
            String calculationKey = cacheService.generateCalculationKey(
                    tenantId, deptId, roomTypeId, checkInDate, checkOutDate,
                    numAdults, numChildren, channelCode, memberLevel);

            Map<String, Object> cachedResult = cacheService.getPricingCalculation(calculationKey);
            if (cachedResult != null) {
                log.debug("使用缓存的价格计算结果: {}", calculationKey);
                return buildResultFromCache(cachedResult, startTime);
            }

            // 2. 获取基础价格
            BigDecimal basePrice = getBasePrice(tenantId, deptId, roomTypeId);

            // 3. 获取适用的价格规则
            List<PmsRoomPricingRule> applicableRules = getApplicableRules(
                    tenantId, deptId, roomTypeId, checkInDate, checkOutDate,
                    channelCode, memberLevel, numAdults, numChildren);

            // 4. 获取特殊日期价格
            List<PmsSpecialDatePricing> specialDates = getSpecialDatePricing(
                    tenantId, deptId, roomTypeId, checkInDate, checkOutDate);

            // 5. 计算住宿天数
            long stayDays = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            if (stayDays <= 0) {
                stayDays = 1; // 至少1天
            }

            // 6. 执行价格计算
            PricingCalculationResult result;
            if (stayDays > 7) {
                // 长期住宿使用并行计算
                result = calculatePriceParallel(basePrice, applicableRules, specialDates, checkInDate, checkOutDate);
            } else {
                // 短期住宿使用串行计算
                result = calculatePriceSerial(basePrice, applicableRules, specialDates, checkInDate, checkOutDate);
            }

            // 7. 设置计算时间和基础信息
            result.setBasePrice(basePrice);
            result.setStayDays((int) stayDays);
            result.setCalculationTime(System.currentTimeMillis() - startTime);
            result.setFromCache(false);

            // 8. 缓存计算结果
            Map<String, Object> resultMap = buildCacheResult(result);
            cacheService.cachePricingCalculation(calculationKey, resultMap);

            log.info("价格计算完成: 房型={}, 入住={}, 退房={}, 总价={}, 耗时={}ms",
                    roomTypeId, checkInDate, checkOutDate, result.getTotalPrice(), result.getCalculationTime());

            return result;

        } catch (Exception e) {
            log.error("价格计算失败: 房型={}, 入住={}, 退房={}", roomTypeId, checkInDate, checkOutDate, e);

            // 返回基础价格作为兜底
            BigDecimal basePrice = getBasePrice(tenantId, deptId, roomTypeId);
            long stayDays = ChronoUnit.DAYS.between(checkInDate, checkOutDate);

            return PricingCalculationResult.builder()
                    .totalPrice(basePrice.multiply(BigDecimal.valueOf(Math.max(stayDays, 1))))
                    .basePrice(basePrice)
                    .stayDays((int) Math.max(stayDays, 1))
                    .calculationTime(System.currentTimeMillis() - startTime)
                    .fromCache(false)
                    .hasError(true)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 获取基础价格（带缓存）
     */
    private BigDecimal getBasePrice(String tenantId, Long deptId, Long roomTypeId) {
        BigDecimal basePrice = cacheService.getBasePrice(tenantId, deptId, roomTypeId);
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            // 这里应该从房型表查询基础价格，暂时使用默认值
            basePrice = DEFAULT_BASE_PRICE;
            cacheService.cacheBasePrice(tenantId, deptId, roomTypeId, basePrice);
        }
        return basePrice;
    }

    /**
     * 获取适用的价格规则（带缓存）
     */
    private List<PmsRoomPricingRule> getApplicableRules(String tenantId, Long deptId, Long roomTypeId,
            LocalDate checkInDate, LocalDate checkOutDate,
            String channelCode, String memberLevel, Integer numAdults, Integer numChildren) {

        // 先从缓存获取活跃规则
        List<PmsRoomPricingRule> cachedRules = cacheService.getActivePricingRules(tenantId, deptId);
        if (cachedRules == null) {
            // 从数据库查询并缓存
            cachedRules = pricingRuleService.getActiveRules(tenantId, deptId);
            if (CollUtil.isNotEmpty(cachedRules)) {
                cacheService.cacheActivePricingRules(tenantId, deptId, cachedRules);
            }
        }

        if (CollUtil.isEmpty(cachedRules)) {
            return List.of();
        }

        // 过滤出适用的规则
        return cachedRules.stream()
                .filter(rule -> isRuleApplicable(rule, roomTypeId, checkInDate, checkOutDate,
                        channelCode, memberLevel, numAdults, numChildren))
                .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority())) // 高优先级优先
                .collect(Collectors.toList());
    }

    /**
     * 获取特殊日期价格（带缓存）
     */
    private List<PmsSpecialDatePricing> getSpecialDatePricing(String tenantId, Long deptId, Long roomTypeId,
            LocalDate checkInDate, LocalDate checkOutDate) {

        // 先从缓存获取特殊日期价格
        List<PmsSpecialDatePricing> cachedSpecialDates = cacheService.getSpecialDatePricing(tenantId, deptId);
        if (cachedSpecialDates == null) {
            // 从数据库查询并缓存
            cachedSpecialDates = specialDatePricingService.getActiveSpecialDates(tenantId, deptId);
            if (CollUtil.isNotEmpty(cachedSpecialDates)) {
                cacheService.cacheSpecialDatePricing(tenantId, deptId, cachedSpecialDates);
            }
        }

        if (CollUtil.isEmpty(cachedSpecialDates)) {
            return List.of();
        }

        // 过滤出日期范围内的特殊价格
        return cachedSpecialDates.stream()
                .filter(special -> {
                    LocalDate specialDate = special.getSpecialDate();
                    return (specialDate.isEqual(checkInDate) || specialDate.isAfter(checkInDate)) &&
                            specialDate.isBefore(checkOutDate) &&
                            (special.getRoomTypeId() == null || special.getRoomTypeId().equals(roomTypeId)) &&
                            "active".equals(special.getStatus());
                })
                .sorted((s1, s2) -> Integer.compare(s2.getPriority(), s1.getPriority())) // 高优先级优先
                .collect(Collectors.toList());
    }

    /**
     * 串行价格计算（适用于短期住宿）
     */
    private PricingCalculationResult calculatePriceSerial(BigDecimal basePrice,
            List<PmsRoomPricingRule> rules,
            List<PmsSpecialDatePricing> specialDates,
            LocalDate checkInDate, LocalDate checkOutDate) {

        BigDecimal totalPrice = BigDecimal.ZERO;
        PricingCalculationResult.PricingCalculationResultBuilder resultBuilder = PricingCalculationResult.builder();

        // 按日期逐日计算
        LocalDate currentDate = checkInDate;
        while (currentDate.isBefore(checkOutDate)) {
            BigDecimal dailyPrice = calculateDailyPrice(basePrice, rules, specialDates, currentDate);
            totalPrice = totalPrice.add(dailyPrice);
            currentDate = currentDate.plusDays(1);
        }

        return resultBuilder
                .totalPrice(totalPrice.setScale(PRICE_SCALE, RoundingMode.HALF_UP))
                .appliedRulesCount(rules.size())
                .appliedSpecialDatesCount(specialDates.size())
                .build();
    }

    /**
     * 并行价格计算（适用于长期住宿）
     */
    private PricingCalculationResult calculatePriceParallel(BigDecimal basePrice,
            List<PmsRoomPricingRule> rules,
            List<PmsSpecialDatePricing> specialDates,
            LocalDate checkInDate, LocalDate checkOutDate) {

        long stayDays = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        Executor executor = getAsyncExecutor();

        // 将日期范围分割为多个批次并行计算
        int parallelism = Runtime.getRuntime().availableProcessors();
        int batchSize = Math.max(1, (int) stayDays / parallelism);

        List<CompletableFuture<BigDecimal>> futures = LongStream.range(0, stayDays)
                .filter(i -> i % batchSize == 0)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    BigDecimal batchTotal = BigDecimal.ZERO;
                    long endIndex = Math.min(i + batchSize, stayDays);

                    for (long j = i; j < endIndex; j++) {
                        LocalDate currentDate = checkInDate.plusDays(j);
                        BigDecimal dailyPrice = calculateDailyPrice(basePrice, rules, specialDates, currentDate);
                        batchTotal = batchTotal.add(dailyPrice);
                    }

                    return batchTotal;
                }, executor))
                .collect(Collectors.toList());

        // 等待所有批次计算完成并汇总结果
        BigDecimal totalPrice = futures.stream()
                .map(CompletableFuture::join)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PricingCalculationResult.builder()
                .totalPrice(totalPrice.setScale(PRICE_SCALE, RoundingMode.HALF_UP))
                .appliedRulesCount(rules.size())
                .appliedSpecialDatesCount(specialDates.size())
                .build();
    }

    /**
     * 计算单日价格
     */
    private BigDecimal calculateDailyPrice(BigDecimal basePrice, List<PmsRoomPricingRule> rules,
            List<PmsSpecialDatePricing> specialDates, LocalDate date) {
        BigDecimal dailyPrice = basePrice;

        // 1. 先应用特殊日期价格（优先级最高）
        PmsSpecialDatePricing applicableSpecialDate = specialDates.stream()
                .filter(special -> special.getSpecialDate().isEqual(date))
                .findFirst()
                .orElse(null);

        if (applicableSpecialDate != null) {
            dailyPrice = applySpecialDatePricing(dailyPrice, applicableSpecialDate);
        }

        // 2. 再应用价格规则
        List<PmsRoomPricingRule> applicableRules = rules.stream()
                .filter(rule -> isRuleApplicableForDate(rule, date))
                .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority())) // 高优先级优先
                .collect(Collectors.toList());

        for (PmsRoomPricingRule rule : applicableRules) {
            dailyPrice = applyPricingRule(dailyPrice, rule);
        }

        return dailyPrice;
    }

    /**
     * 应用特殊日期价格
     */
    private BigDecimal applySpecialDatePricing(BigDecimal currentPrice, PmsSpecialDatePricing specialDate) {
        if (specialDate.getAdjustmentValue() == null) {
            return currentPrice;
        }

        PriceAdjustmentType adjustmentType = PriceAdjustmentType.fromCode(specialDate.getPriceAdjustmentType());
        BigDecimal adjustmentValue = specialDate.getAdjustmentValue();

        switch (adjustmentType) {
            case FIXED_AMOUNT:
                return currentPrice.add(adjustmentValue);
            case PERCENTAGE:
                BigDecimal percentage = adjustmentValue.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                BigDecimal adjustment = currentPrice.multiply(percentage);
                return currentPrice.add(adjustment);
            case FIXED_PRICE:
                return adjustmentValue;
            default:
                log.warn("未知的特殊日期价格调整类型: {}", specialDate.getPriceAdjustmentType());
                return currentPrice;
        }
    }

    /**
     * 应用价格规则
     */
    private BigDecimal applyPricingRule(BigDecimal currentPrice, PmsRoomPricingRule rule) {
        if (rule.getAdjustmentValue() == null) {
            return currentPrice;
        }

        PriceAdjustmentType adjustmentType = PriceAdjustmentType.fromCode(rule.getPriceAdjustmentType());
        BigDecimal adjustmentValue = rule.getAdjustmentValue();

        switch (adjustmentType) {
            case FIXED_AMOUNT:
                return currentPrice.add(adjustmentValue);
            case PERCENTAGE:
                BigDecimal percentage = adjustmentValue.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                BigDecimal adjustment = currentPrice.multiply(percentage);
                return currentPrice.add(adjustment);
            case FIXED_PRICE:
                return adjustmentValue;
            default:
                log.warn("未知的价格调整类型: {}", rule.getPriceAdjustmentType());
                return currentPrice;
        }
    }

    /**
     * 检查规则是否适用于指定日期
     */
    private boolean isRuleApplicableForDate(PmsRoomPricingRule rule, LocalDate date) {
        // 检查日期范围
        if (rule.getDateRangeStart() != null && date.isBefore(rule.getDateRangeStart())) {
            return false;
        }
        if (rule.getDateRangeEnd() != null && date.isAfter(rule.getDateRangeEnd())) {
            return false;
        }

        // 检查生效日期
        if (rule.getEffectiveStartDate() != null && date.isBefore(rule.getEffectiveStartDate())) {
            return false;
        }
        if (rule.getEffectiveEndDate() != null && date.isAfter(rule.getEffectiveEndDate())) {
            return false;
        }

        // 检查星期限制
        // TODO: 实现星期限制检查

        return true;
    }

    /**
     * 检查规则是否适用
     */
    private boolean isRuleApplicable(PmsRoomPricingRule rule, Long roomTypeId,
            LocalDate checkInDate, LocalDate checkOutDate,
            String channelCode, String memberLevel, Integer numAdults, Integer numChildren) {

        // 检查房型
        if (rule.getRoomTypeId() != null && !rule.getRoomTypeId().equals(roomTypeId)) {
            return false;
        }

        // 检查日期范围
        if (rule.getDateRangeStart() != null && checkOutDate.isBefore(rule.getDateRangeStart())) {
            return false;
        }
        if (rule.getDateRangeEnd() != null && checkInDate.isAfter(rule.getDateRangeEnd())) {
            return false;
        }

        // 检查生效日期
        if (rule.getEffectiveStartDate() != null && checkInDate.isBefore(rule.getEffectiveStartDate())) {
            return false;
        }
        if (rule.getEffectiveEndDate() != null && checkInDate.isAfter(rule.getEffectiveEndDate())) {
            return false;
        }

        // 检查客人数量
        Integer totalGuests = (numAdults != null ? numAdults : 0) + (numChildren != null ? numChildren : 0);
        if (rule.getGuestCountMin() != null && totalGuests < rule.getGuestCountMin()) {
            return false;
        }
        if (rule.getGuestCountMax() != null && totalGuests > rule.getGuestCountMax()) {
            return false;
        }

        // 检查入住天数
        long stayDays = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (rule.getMinLengthOfStay() != null && stayDays < rule.getMinLengthOfStay()) {
            return false;
        }
        if (rule.getMaxLengthOfStay() != null && stayDays > rule.getMaxLengthOfStay()) {
            return false;
        }

        // TODO: 检查渠道限制、会员等级限制、提前预订天数等

        return true;
    }

    /**
     * 从缓存结果构建返回对象
     */
    private PricingCalculationResult buildResultFromCache(Map<String, Object> cachedResult, long startTime) {
        return PricingCalculationResult.builder()
                .totalPrice((BigDecimal) cachedResult.get("totalPrice"))
                .basePrice((BigDecimal) cachedResult.get("basePrice"))
                .stayDays((Integer) cachedResult.get("stayDays"))
                .appliedRulesCount((Integer) cachedResult.get("appliedRulesCount"))
                .appliedSpecialDatesCount((Integer) cachedResult.get("appliedSpecialDatesCount"))
                .calculationTime(System.currentTimeMillis() - startTime)
                .fromCache(true)
                .build();
    }

    /**
     * 构建缓存结果
     */
    private Map<String, Object> buildCacheResult(PricingCalculationResult result) {
        Map<String, Object> cacheResult = new HashMap<>();
        cacheResult.put("totalPrice", result.getTotalPrice());
        cacheResult.put("basePrice", result.getBasePrice());
        cacheResult.put("stayDays", result.getStayDays());
        cacheResult.put("appliedRulesCount", result.getAppliedRulesCount());
        cacheResult.put("appliedSpecialDatesCount", result.getAppliedSpecialDatesCount());
        return cacheResult;
    }

    /**
     * 价格计算结果
     */
    @lombok.Data
    @lombok.Builder
    public static class PricingCalculationResult {
        /**
         * 总价格
         */
        private BigDecimal totalPrice;

        /**
         * 基础价格
         */
        private BigDecimal basePrice;

        /**
         * 住宿天数
         */
        private Integer stayDays;

        /**
         * 应用的规则数量
         */
        private Integer appliedRulesCount;

        /**
         * 应用的特殊日期数量
         */
        private Integer appliedSpecialDatesCount;

        /**
         * 计算耗时（毫秒）
         */
        private Long calculationTime;

        /**
         * 是否来自缓存
         */
        private Boolean fromCache;

        /**
         * 是否有错误
         */
        private Boolean hasError;

        /**
         * 错误信息
         */
        private String errorMessage;

        /**
         * 获取平均每日价格
         */
        public BigDecimal getAverageDailyPrice() {
            if (stayDays == null || stayDays <= 0 || totalPrice == null) {
                return BigDecimal.ZERO;
            }
            return totalPrice.divide(BigDecimal.valueOf(stayDays), PRICE_SCALE, RoundingMode.HALF_UP);
        }
    }
}
