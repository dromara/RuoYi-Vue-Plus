package org.dromara.pms.service.cache;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.pms.domain.PmsRoomPricingRule;
import org.dromara.pms.domain.PmsSpecialDatePricing;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 价格管理缓存服务
 *
 * @author ruoyi
 * @date 2024-12-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PricingCacheService {

    private static final String PRICING_RULE_CACHE_KEY = "pms:pricing:rules:{}:{}";
    private static final String SPECIAL_DATE_CACHE_KEY = "pms:pricing:special:{}:{}";
    private static final String BASE_PRICE_CACHE_KEY = "pms:pricing:base:{}:{}:{}";
    private static final String CALCULATION_CACHE_KEY = "pms:pricing:calc:{}";

    private static final Duration CACHE_DURATION = Duration.ofHours(2);
    private static final Duration BASE_PRICE_CACHE_DURATION = Duration.ofMinutes(30);
    private static final Duration CALCULATION_CACHE_DURATION = Duration.ofMinutes(15);

    // 本地缓存，用于频繁访问的数据
    private final Map<String, List<PmsRoomPricingRule>> localRuleCache = new ConcurrentHashMap<>();
    private final Map<String, List<PmsSpecialDatePricing>> localSpecialDateCache = new ConcurrentHashMap<>();

    /**
     * 获取活跃的价格规则（带缓存）
     *
     * @param tenantId 租户ID
     * @param deptId   部门ID
     * @return 价格规则列表
     */
    public List<PmsRoomPricingRule> getActivePricingRules(String tenantId, Long deptId) {
        String cacheKey = StrUtil.format(PRICING_RULE_CACHE_KEY, tenantId, deptId);

        // 先检查本地缓存
        String localKey = tenantId + ":" + deptId;
        if (localRuleCache.containsKey(localKey)) {
            log.debug("从本地缓存获取价格规则: {}", localKey);
            return localRuleCache.get(localKey);
        }

        // 检查Redis缓存
        List<PmsRoomPricingRule> rules = RedisUtils.getCacheObject(cacheKey);
        if (CollUtil.isNotEmpty(rules)) {
            log.debug("从Redis缓存获取价格规则: {}", cacheKey);
            localRuleCache.put(localKey, rules);
            return rules;
        }

        log.debug("价格规则缓存未命中: {}", cacheKey);
        return null;
    }

    /**
     * 缓存活跃的价格规则
     *
     * @param tenantId 租户ID
     * @param deptId   部门ID
     * @param rules    价格规则列表
     */
    public void cacheActivePricingRules(String tenantId, Long deptId, List<PmsRoomPricingRule> rules) {
        if (CollUtil.isEmpty(rules)) {
            return;
        }

        String cacheKey = StrUtil.format(PRICING_RULE_CACHE_KEY, tenantId, deptId);
        String localKey = tenantId + ":" + deptId;

        // 缓存到Redis
        RedisUtils.setCacheObject(cacheKey, rules, CACHE_DURATION);

        // 缓存到本地
        localRuleCache.put(localKey, rules);

        log.debug("缓存价格规则: {}, 数量: {}", cacheKey, rules.size());
    }

    /**
     * 获取特殊日期价格（带缓存）
     *
     * @param tenantId 租户ID
     * @param deptId   部门ID
     * @return 特殊日期价格列表
     */
    public List<PmsSpecialDatePricing> getSpecialDatePricing(String tenantId, Long deptId) {
        String cacheKey = StrUtil.format(SPECIAL_DATE_CACHE_KEY, tenantId, deptId);

        // 先检查本地缓存
        String localKey = tenantId + ":" + deptId;
        if (localSpecialDateCache.containsKey(localKey)) {
            log.debug("从本地缓存获取特殊日期价格: {}", localKey);
            return localSpecialDateCache.get(localKey);
        }

        // 检查Redis缓存
        List<PmsSpecialDatePricing> specialDates = RedisUtils.getCacheObject(cacheKey);
        if (CollUtil.isNotEmpty(specialDates)) {
            log.debug("从Redis缓存获取特殊日期价格: {}", cacheKey);
            localSpecialDateCache.put(localKey, specialDates);
            return specialDates;
        }

        log.debug("特殊日期价格缓存未命中: {}", cacheKey);
        return null;
    }

    /**
     * 缓存特殊日期价格
     *
     * @param tenantId     租户ID
     * @param deptId       部门ID
     * @param specialDates 特殊日期价格列表
     */
    public void cacheSpecialDatePricing(String tenantId, Long deptId, List<PmsSpecialDatePricing> specialDates) {
        if (CollUtil.isEmpty(specialDates)) {
            return;
        }

        String cacheKey = StrUtil.format(SPECIAL_DATE_CACHE_KEY, tenantId, deptId);
        String localKey = tenantId + ":" + deptId;

        // 缓存到Redis
        RedisUtils.setCacheObject(cacheKey, specialDates, CACHE_DURATION);

        // 缓存到本地
        localSpecialDateCache.put(localKey, specialDates);

        log.debug("缓存特殊日期价格: {}, 数量: {}", cacheKey, specialDates.size());
    }

    /**
     * 获取房型基础价格（带缓存）
     *
     * @param tenantId   租户ID
     * @param deptId     部门ID
     * @param roomTypeId 房型ID
     * @return 基础价格
     */
    public BigDecimal getBasePrice(String tenantId, Long deptId, Long roomTypeId) {
        String cacheKey = StrUtil.format(BASE_PRICE_CACHE_KEY, tenantId, deptId, roomTypeId);
        return RedisUtils.getCacheObject(cacheKey);
    }

    /**
     * 缓存房型基础价格
     *
     * @param tenantId   租户ID
     * @param deptId     部门ID
     * @param roomTypeId 房型ID
     * @param basePrice  基础价格
     */
    public void cacheBasePrice(String tenantId, Long deptId, Long roomTypeId, BigDecimal basePrice) {
        if (basePrice == null) {
            return;
        }

        String cacheKey = StrUtil.format(BASE_PRICE_CACHE_KEY, tenantId, deptId, roomTypeId);
        RedisUtils.setCacheObject(cacheKey, basePrice, BASE_PRICE_CACHE_DURATION);

        log.debug("缓存基础价格: {}, 价格: {}", cacheKey, basePrice);
    }

    /**
     * 获取价格计算结果（带缓存）
     *
     * @param calculationKey 计算键值
     * @return 计算结果
     */
    public Map<String, Object> getPricingCalculation(String calculationKey) {
        String cacheKey = StrUtil.format(CALCULATION_CACHE_KEY, calculationKey);
        return RedisUtils.getCacheObject(cacheKey);
    }

    /**
     * 缓存价格计算结果
     *
     * @param calculationKey 计算键值
     * @param result         计算结果
     */
    public void cachePricingCalculation(String calculationKey, Map<String, Object> result) {
        if (result == null || result.isEmpty()) {
            return;
        }

        String cacheKey = StrUtil.format(CALCULATION_CACHE_KEY, calculationKey);
        RedisUtils.setCacheObject(cacheKey, result, CALCULATION_CACHE_DURATION);

        log.debug("缓存价格计算结果: {}", cacheKey);
    }

    /**
     * 生成价格计算缓存键
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
     * @return 缓存键
     */
    public String generateCalculationKey(String tenantId, Long deptId, Long roomTypeId,
            LocalDate checkInDate, LocalDate checkOutDate,
            Integer numAdults, Integer numChildren,
            String channelCode, String memberLevel) {
        return String.format("%s:%s:%s:%s:%s:%s:%s:%s:%s",
                tenantId, deptId, roomTypeId, checkInDate, checkOutDate,
                numAdults, numChildren,
                StrUtil.nullToDefault(channelCode, ""),
                StrUtil.nullToDefault(memberLevel, ""));
    }

    /**
     * 清除价格规则缓存
     *
     * @param tenantId 租户ID
     * @param deptId   部门ID
     */
    public void evictPricingRulesCache(String tenantId, Long deptId) {
        String cacheKey = StrUtil.format(PRICING_RULE_CACHE_KEY, tenantId, deptId);
        String localKey = tenantId + ":" + deptId;

        RedisUtils.deleteObject(cacheKey);
        localRuleCache.remove(localKey);

        log.debug("清除价格规则缓存: {}", cacheKey);
    }

    /**
     * 清除特殊日期价格缓存
     *
     * @param tenantId 租户ID
     * @param deptId   部门ID
     */
    public void evictSpecialDateCache(String tenantId, Long deptId) {
        String cacheKey = StrUtil.format(SPECIAL_DATE_CACHE_KEY, tenantId, deptId);
        String localKey = tenantId + ":" + deptId;

        RedisUtils.deleteObject(cacheKey);
        localSpecialDateCache.remove(localKey);

        log.debug("清除特殊日期价格缓存: {}", cacheKey);
    }

    /**
     * 清除基础价格缓存
     *
     * @param tenantId   租户ID
     * @param deptId     部门ID
     * @param roomTypeId 房型ID
     */
    public void evictBasePriceCache(String tenantId, Long deptId, Long roomTypeId) {
        String cacheKey = StrUtil.format(BASE_PRICE_CACHE_KEY, tenantId, deptId, roomTypeId);
        RedisUtils.deleteObject(cacheKey);

        log.debug("清除基础价格缓存: {}", cacheKey);
    }

    /**
     * 清除所有价格相关缓存
     *
     * @param tenantId 租户ID
     * @param deptId   部门ID
     */
    public void evictAllPricingCache(String tenantId, Long deptId) {
        evictPricingRulesCache(tenantId, deptId);
        evictSpecialDateCache(tenantId, deptId);

        // 清除所有房型的基础价格缓存
        String basePricePattern = StrUtil.format(BASE_PRICE_CACHE_KEY, tenantId, deptId, "*");
        RedisUtils.deleteKeys(basePricePattern);

        // 清除所有计算结果缓存
        String calculationPattern = StrUtil.format(CALCULATION_CACHE_KEY, tenantId + ":" + deptId + ":*");
        RedisUtils.deleteKeys(calculationPattern);

        log.debug("清除所有价格缓存: 租户={}, 部门={}", tenantId, deptId);
    }

    /**
     * 清除活跃规则缓存
     *
     * @param tenantId   租户ID
     * @param deptId     部门ID
     * @param roomTypeId 房型ID
     */
    public void clearActiveRulesCache(String tenantId, Long deptId, Long roomTypeId) {
        // 清除Redis缓存
        String cacheKey = StrUtil.format(PRICING_RULE_CACHE_KEY, tenantId, deptId);
        RedisUtils.deleteObject(cacheKey);

        // 清除本地缓存
        String localKey = tenantId + ":" + deptId;
        localRuleCache.remove(localKey);

        log.debug("已清除活跃规则缓存: 租户={}, 部门={}, 房型={}", tenantId, deptId, roomTypeId);
    }

    /**
     * 清除价格计算结果缓存
     *
     * @param tenantId   租户ID
     * @param deptId     部门ID
     * @param roomTypeId 房型ID
     */
    public void clearCalculationCache(String tenantId, Long deptId, Long roomTypeId) {
        // 清除相关的计算结果缓存
        String pattern = StrUtil.format("pms:pricing:calc:{}:{}:{}:*", tenantId, deptId, roomTypeId);
        RedisUtils.deleteKeys(pattern);

        // 清除基础价格缓存
        evictBasePriceCache(tenantId, deptId, roomTypeId);

        log.debug("已清除价格计算缓存: 租户={}, 部门={}, 房型={}", tenantId, deptId, roomTypeId);
    }

    /**
     * 预热缓存
     *
     * @param tenantId 租户ID
     * @param deptId   部门ID
     */
    public void warmUpCache(String tenantId, Long deptId) {
        log.info("开始预热价格缓存: 租户={}, 部门={}", tenantId, deptId);

        // 这里可以预加载常用的价格规则和特殊日期价格
        // 具体实现可以根据业务需求调整

        log.info("价格缓存预热完成: 租户={}, 部门={}", tenantId, deptId);
    }
}
