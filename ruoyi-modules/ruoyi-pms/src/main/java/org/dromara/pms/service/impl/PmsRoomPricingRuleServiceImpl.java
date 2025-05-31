package org.dromara.pms.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
import org.dromara.pms.domain.PmsRoomPricingRule;
import org.dromara.pms.domain.bo.PmsRoomPricingRuleBo;
import org.dromara.pms.domain.vo.PmsRoomPricingRuleVo;
import org.dromara.pms.mapper.PmsRoomPricingRuleMapper;
import org.dromara.pms.service.IPmsRoomPricingRuleService;
import org.dromara.pms.service.cache.PricingCacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 房间价格规则Service业务层处理
 *
 * @author PMS
 * @date 2024-12-01
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PmsRoomPricingRuleServiceImpl implements IPmsRoomPricingRuleService {

    private final PmsRoomPricingRuleMapper baseMapper;
    private final PricingCacheService cacheService;

    /**
     * 查询房间价格规则
     */
    @Override
    public PmsRoomPricingRuleVo queryById(Long ruleId) {
        return baseMapper.selectVoById(ruleId);
    }

    /**
     * 查询房间价格规则列表
     */
    @Override
    public TableDataInfo<PmsRoomPricingRuleVo> queryPageList(PmsRoomPricingRuleBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsRoomPricingRule> lqw = buildQueryWrapper(bo);
        Page<PmsRoomPricingRuleVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询房间价格规则列表
     */
    @Override
    public List<PmsRoomPricingRuleVo> queryList(PmsRoomPricingRuleBo bo) {
        LambdaQueryWrapper<PmsRoomPricingRule> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<PmsRoomPricingRule> buildQueryWrapper(PmsRoomPricingRuleBo bo) {
        LambdaQueryWrapper<PmsRoomPricingRule> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), PmsRoomPricingRule::getTenantId, bo.getTenantId());
        lqw.eq(bo.getDeptId() != null, PmsRoomPricingRule::getDeptId, bo.getDeptId());
        lqw.like(StringUtils.isNotBlank(bo.getName()), PmsRoomPricingRule::getName, bo.getName());
        lqw.eq(bo.getRoomTypeId() != null, PmsRoomPricingRule::getRoomTypeId, bo.getRoomTypeId());
        lqw.eq(StringUtils.isNotBlank(bo.getPriceAdjustmentType()), PmsRoomPricingRule::getPriceAdjustmentType,
                bo.getPriceAdjustmentType());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), PmsRoomPricingRule::getStatus, bo.getStatus());
        lqw.ge(bo.getDateRangeStart() != null, PmsRoomPricingRule::getDateRangeStart, bo.getDateRangeStart());
        lqw.le(bo.getDateRangeEnd() != null, PmsRoomPricingRule::getDateRangeEnd, bo.getDateRangeEnd());
        lqw.ge(bo.getEffectiveStartDate() != null, PmsRoomPricingRule::getEffectiveStartDate,
                bo.getEffectiveStartDate());
        lqw.le(bo.getEffectiveEndDate() != null, PmsRoomPricingRule::getEffectiveEndDate, bo.getEffectiveEndDate());
        lqw.orderByDesc(PmsRoomPricingRule::getPriority);
        lqw.orderByDesc(PmsRoomPricingRule::getCreateTime);
        return lqw;
    }

    /**
     * 新增房间价格规则
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(PmsRoomPricingRuleBo bo) {
        PmsRoomPricingRule add = MapstructUtils.convert(bo, PmsRoomPricingRule.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setRuleId(add.getRuleId());
            // 清除相关缓存
            clearRelatedCache(add);
            log.info("新增价格规则成功，已清除相关缓存: 规则ID={}", add.getRuleId());
        }
        return flag;
    }

    /**
     * 修改房间价格规则
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(PmsRoomPricingRuleBo bo) {
        PmsRoomPricingRule update = MapstructUtils.convert(bo, PmsRoomPricingRule.class);
        validEntityBeforeSave(update);
        boolean flag = baseMapper.updateById(update) > 0;
        if (flag) {
            // 清除相关缓存
            clearRelatedCache(update);
            log.info("修改价格规则成功，已清除相关缓存: 规则ID={}", update.getRuleId());
        }
        return flag;
    }

    /**
     * 清除相关缓存
     */
    private void clearRelatedCache(PmsRoomPricingRule rule) {
        try {
            // 清除活跃规则缓存
            cacheService.clearActiveRulesCache(rule.getTenantId(), rule.getDeptId(), rule.getRoomTypeId());

            // 清除价格计算结果缓存
            cacheService.clearCalculationCache(rule.getTenantId(), rule.getDeptId(), rule.getRoomTypeId());

            log.debug("已清除价格规则相关缓存: 租户={}, 部门={}, 房型={}",
                    rule.getTenantId(), rule.getDeptId(), rule.getRoomTypeId());
        } catch (Exception e) {
            log.warn("清除价格规则缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(PmsRoomPricingRule entity) {
        // 校验规则名称唯一性
        if (StringUtils.isNotBlank(entity.getName())) {
            LambdaQueryWrapper<PmsRoomPricingRule> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(PmsRoomPricingRule::getTenantId, entity.getTenantId());
            wrapper.eq(PmsRoomPricingRule::getDeptId, entity.getDeptId());
            wrapper.eq(PmsRoomPricingRule::getName, entity.getName());
            wrapper.ne(entity.getRuleId() != null, PmsRoomPricingRule::getRuleId, entity.getRuleId());
            if (baseMapper.exists(wrapper)) {
                throw new ServiceException("规则名称已存在");
            }
        }

        // 校验日期范围
        if (entity.getDateRangeStart() != null && entity.getDateRangeEnd() != null) {
            if (entity.getDateRangeStart().isAfter(entity.getDateRangeEnd())) {
                throw new ServiceException("开始日期不能晚于结束日期");
            }
        }

        // 校验生效日期范围
        if (entity.getEffectiveStartDate() != null && entity.getEffectiveEndDate() != null) {
            if (entity.getEffectiveStartDate().isAfter(entity.getEffectiveEndDate())) {
                throw new ServiceException("生效开始日期不能晚于生效结束日期");
            }
        }

        // 校验客人数范围
        if (entity.getGuestCountMin() != null && entity.getGuestCountMax() != null) {
            if (entity.getGuestCountMin() > entity.getGuestCountMax()) {
                throw new ServiceException("最小客人数不能大于最大客人数");
            }
        }

        // 校验入住天数范围
        if (entity.getMinLengthOfStay() != null && entity.getMaxLengthOfStay() != null) {
            if (entity.getMinLengthOfStay() > entity.getMaxLengthOfStay()) {
                throw new ServiceException("最小入住天数不能大于最大入住天数");
            }
        }

        // 校验提前预订天数范围
        if (entity.getAdvanceBookingDaysMin() != null && entity.getAdvanceBookingDaysMax() != null) {
            if (entity.getAdvanceBookingDaysMin() > entity.getAdvanceBookingDaysMax()) {
                throw new ServiceException("最小提前预订天数不能大于最大提前预订天数");
            }
        }
    }

    /**
     * 批量删除房间价格规则
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 校验是否有关联的计算记录
            // TODO: 添加关联校验逻辑
        }

        // 获取要删除的规则信息，用于清除缓存
        List<PmsRoomPricingRule> rulesToDelete = baseMapper.selectBatchIds(ids);

        boolean flag = baseMapper.deleteByIds(ids) > 0;

        if (flag) {
            // 清除相关缓存
            for (PmsRoomPricingRule rule : rulesToDelete) {
                clearRelatedCache(rule);
            }
            log.info("批量删除价格规则成功，已清除相关缓存: 规则数量={}", ids.size());
        }

        return flag;
    }

    /**
     * 查询适用的价格规则
     */
    @Override
    public List<PmsRoomPricingRule> getApplicableRules(String tenantId, Long deptId, Long roomTypeId,
            LocalDate checkInDate, LocalDate checkOutDate, String channelCode,
            String memberLevel, Integer guestCount, Integer advanceBookingDays) {
        return baseMapper.selectApplicableRules(tenantId, deptId, roomTypeId, checkInDate, checkOutDate,
                channelCode, memberLevel, guestCount, advanceBookingDays);
    }

    /**
     * 查询有效的价格规则
     */
    @Override
    public List<PmsRoomPricingRule> getActiveRules(String tenantId, Long deptId) {
        return baseMapper.selectActiveRules(tenantId, deptId, "active");
    }

    /**
     * 更新规则使用次数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateRuleUsageCount(Long ruleId, Integer count) {
        return baseMapper.updateRuleUsageCount(ruleId, count) > 0;
    }

    /**
     * 检查规则冲突
     */
    @Override
    public Boolean checkRuleConflict(PmsRoomPricingRuleBo bo) {
        List<PmsRoomPricingRule> conflictingRules = baseMapper.selectConflictingRules(
                bo.getTenantId(), bo.getDeptId(), bo.getRoomTypeId(),
                bo.getDateRangeStart(), bo.getDateRangeEnd(), bo.getRuleId());
        return !conflictingRules.isEmpty();
    }

    /**
     * 批量更新规则状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchUpdateStatus(List<Long> ruleIds, String status) {
        // 获取要更新的规则信息，用于清除缓存
        List<PmsRoomPricingRule> rulesToUpdate = baseMapper.selectBatchIds(ruleIds);

        boolean flag = baseMapper.batchUpdateStatus(ruleIds, status) > 0;

        if (flag) {
            // 清除相关缓存
            for (PmsRoomPricingRule rule : rulesToUpdate) {
                clearRelatedCache(rule);
            }
            log.info("批量更新价格规则状态成功，已清除相关缓存: 规则数量={}, 状态={}", ruleIds.size(), status);
        }

        return flag;
    }

    /**
     * 启用规则
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean enableRule(Long ruleId) {
        PmsRoomPricingRule rule = baseMapper.selectById(ruleId);
        if (rule == null) {
            throw new ServiceException("规则不存在");
        }

        rule.setStatus("active");
        boolean flag = baseMapper.updateById(rule) > 0;

        if (flag) {
            clearRelatedCache(rule);
            log.info("启用价格规则成功，已清除相关缓存: 规则ID={}", ruleId);
        }

        return flag;
    }

    /**
     * 禁用规则
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disableRule(Long ruleId) {
        PmsRoomPricingRule rule = baseMapper.selectById(ruleId);
        if (rule == null) {
            throw new ServiceException("规则不存在");
        }

        rule.setStatus("inactive");
        boolean flag = baseMapper.updateById(rule) > 0;

        if (flag) {
            clearRelatedCache(rule);
            log.info("禁用价格规则成功，已清除相关缓存: 规则ID={}", ruleId);
        }

        return flag;
    }

    /**
     * 复制规则
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean copyRule(Long ruleId, String newName) {
        PmsRoomPricingRule original = baseMapper.selectById(ruleId);
        if (original == null) {
            throw new ServiceException("原规则不存在");
        }

        PmsRoomPricingRule copy = BeanUtil.copyProperties(original, PmsRoomPricingRule.class);
        copy.setRuleId(null);
        copy.setName(newName);
        copy.setStatus("draft"); // 复制的规则默认为草稿状态
        copy.setUsedCount(0);

        validEntityBeforeSave(copy);
        boolean flag = baseMapper.insert(copy) > 0;

        if (flag) {
            clearRelatedCache(copy);
            log.info("复制价格规则成功: 原规则ID={}, 新规则ID={}", ruleId, copy.getRuleId());
        }

        return flag;
    }

    /**
     * 导出数据
     */
    @Override
    public List<PmsRoomPricingRuleVo> exportList(PmsRoomPricingRuleBo bo) {
        LambdaQueryWrapper<PmsRoomPricingRule> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }
}
