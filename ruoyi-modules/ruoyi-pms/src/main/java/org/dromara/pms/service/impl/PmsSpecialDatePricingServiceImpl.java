package org.dromara.pms.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
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
import org.dromara.pms.domain.PmsSpecialDatePricing;
import org.dromara.pms.domain.bo.PmsSpecialDatePricingBo;
import org.dromara.pms.domain.vo.PmsSpecialDatePricingVo;
import org.dromara.pms.mapper.PmsSpecialDatePricingMapper;
import org.dromara.pms.service.IPmsSpecialDatePricingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 特殊日期价格Service业务层处理
 *
 * @author PMS
 * @date 2024-12-01
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class PmsSpecialDatePricingServiceImpl implements IPmsSpecialDatePricingService {

    private final PmsSpecialDatePricingMapper baseMapper;

    /**
     * 查询特殊日期价格
     */
    @Override
    public PmsSpecialDatePricingVo queryById(Long specialDateId) {
        return baseMapper.selectVoById(specialDateId);
    }

    /**
     * 查询特殊日期价格列表
     */
    @Override
    public TableDataInfo<PmsSpecialDatePricingVo> queryPageList(PmsSpecialDatePricingBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PmsSpecialDatePricing> lqw = buildQueryWrapper(bo);
        Page<PmsSpecialDatePricingVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询特殊日期价格列表
     */
    @Override
    public List<PmsSpecialDatePricingVo> queryList(PmsSpecialDatePricingBo bo) {
        LambdaQueryWrapper<PmsSpecialDatePricing> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<PmsSpecialDatePricing> buildQueryWrapper(PmsSpecialDatePricingBo bo) {
        LambdaQueryWrapper<PmsSpecialDatePricing> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getTenantId()), PmsSpecialDatePricing::getTenantId, bo.getTenantId());
        lqw.eq(bo.getDeptId() != null, PmsSpecialDatePricing::getDeptId, bo.getDeptId());
        lqw.eq(bo.getRoomTypeId() != null, PmsSpecialDatePricing::getRoomTypeId, bo.getRoomTypeId());
        lqw.eq(bo.getSpecialDate() != null, PmsSpecialDatePricing::getSpecialDate, bo.getSpecialDate());
        lqw.eq(StringUtils.isNotBlank(bo.getDateType()), PmsSpecialDatePricing::getDateType, bo.getDateType());
        lqw.like(StringUtils.isNotBlank(bo.getDateName()), PmsSpecialDatePricing::getDateName, bo.getDateName());
        lqw.eq(StringUtils.isNotBlank(bo.getPriceAdjustmentType()), PmsSpecialDatePricing::getPriceAdjustmentType,
                bo.getPriceAdjustmentType());
        lqw.eq(StringUtils.isNotBlank(bo.getStatus()), PmsSpecialDatePricing::getStatus, bo.getStatus());
        lqw.orderByDesc(PmsSpecialDatePricing::getPriority);
        lqw.orderByDesc(PmsSpecialDatePricing::getSpecialDate);
        return lqw;
    }

    /**
     * 新增特殊日期价格
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(PmsSpecialDatePricingBo bo) {
        PmsSpecialDatePricing add = MapstructUtils.convert(bo, PmsSpecialDatePricing.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setSpecialDateId(add.getSpecialDateId());
        }
        return flag;
    }

    /**
     * 修改特殊日期价格
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(PmsSpecialDatePricingBo bo) {
        PmsSpecialDatePricing update = MapstructUtils.convert(bo, PmsSpecialDatePricing.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(PmsSpecialDatePricing entity) {
        // 校验特殊日期唯一性
        if (entity.getSpecialDate() != null) {
            LambdaQueryWrapper<PmsSpecialDatePricing> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(PmsSpecialDatePricing::getTenantId, entity.getTenantId());
            wrapper.eq(PmsSpecialDatePricing::getDeptId, entity.getDeptId());
            wrapper.eq(entity.getRoomTypeId() != null, PmsSpecialDatePricing::getRoomTypeId, entity.getRoomTypeId());
            wrapper.eq(PmsSpecialDatePricing::getSpecialDate, entity.getSpecialDate());
            wrapper.ne(entity.getSpecialDateId() != null, PmsSpecialDatePricing::getSpecialDateId,
                    entity.getSpecialDateId());
            if (baseMapper.exists(wrapper)) {
                throw new ServiceException("该日期已设置特殊价格");
            }
        }

        // 校验特殊日期不能是过去的日期
        if (entity.getSpecialDate() != null && entity.getSpecialDate().isBefore(LocalDate.now())) {
            throw new ServiceException("特殊日期不能是过去的日期");
        }
    }

    /**
     * 批量删除特殊日期价格
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if (isValid) {
            // 校验是否有关联的计算记录
            // TODO: 添加关联校验逻辑
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /**
     * 查询指定日期范围内的特殊日期价格
     */
    @Override
    public List<PmsSpecialDatePricing> getSpecialDatesByRange(String tenantId, Long deptId, Long roomTypeId,
            LocalDate startDate, LocalDate endDate) {
        return baseMapper.selectByDateRange(tenantId, deptId, roomTypeId, startDate, endDate);
    }

    /**
     * 查询指定日期的特殊价格
     */
    @Override
    public PmsSpecialDatePricing getSpecialDateByDate(String tenantId, Long deptId, Long roomTypeId,
            LocalDate specialDate) {
        return baseMapper.selectByDate(tenantId, deptId, roomTypeId, specialDate);
    }

    /**
     * 查询有效的特殊日期价格
     */
    @Override
    public List<PmsSpecialDatePricing> getActiveSpecialDates(String tenantId, Long deptId) {
        return baseMapper.selectActiveSpecialDates(tenantId, deptId, "active");
    }

    /**
     * 检查特殊日期冲突
     */
    @Override
    public Boolean checkDateConflict(PmsSpecialDatePricingBo bo) {
        List<PmsSpecialDatePricing> conflictingDates = baseMapper.selectConflictingDates(
                bo.getTenantId(), bo.getDeptId(), bo.getRoomTypeId(),
                bo.getSpecialDate(), bo.getSpecialDateId());
        return !conflictingDates.isEmpty();
    }

    /**
     * 批量设置特殊日期价格
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchInsertSpecialDates(List<PmsSpecialDatePricingBo> specialDates) {
        if (CollUtil.isEmpty(specialDates)) {
            return false;
        }

        List<PmsSpecialDatePricing> entities = specialDates.stream()
                .map(bo -> MapstructUtils.convert(bo, PmsSpecialDatePricing.class))
                .collect(Collectors.toList());

        // 批量校验
        for (PmsSpecialDatePricing entity : entities) {
            validEntityBeforeSave(entity);
        }

        return baseMapper.batchInsert(entities) > 0;
    }

    /**
     * 批量更新特殊日期价格状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchUpdateStatus(List<Long> dateIds, String status) {
        return baseMapper.batchUpdateStatus(dateIds, status) > 0;
    }

    /**
     * 启用特殊日期价格
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean enableSpecialDate(Long specialDateId) {
        PmsSpecialDatePricing specialDate = baseMapper.selectById(specialDateId);
        if (specialDate == null) {
            throw new ServiceException("特殊日期价格不存在");
        }
        specialDate.setStatus("active");
        return baseMapper.updateById(specialDate) > 0;
    }

    /**
     * 禁用特殊日期价格
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disableSpecialDate(Long specialDateId) {
        PmsSpecialDatePricing specialDate = baseMapper.selectById(specialDateId);
        if (specialDate == null) {
            throw new ServiceException("特殊日期价格不存在");
        }
        specialDate.setStatus("inactive");
        return baseMapper.updateById(specialDate) > 0;
    }

    /**
     * 复制特殊日期价格到其他日期
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean copySpecialDate(Long sourceId, LocalDate targetDate) {
        PmsSpecialDatePricing sourceDate = baseMapper.selectById(sourceId);
        if (sourceDate == null) {
            throw new ServiceException("源特殊日期价格不存在");
        }

        PmsSpecialDatePricing newDate = BeanUtil.copyProperties(sourceDate, PmsSpecialDatePricing.class);
        newDate.setSpecialDateId(null);
        newDate.setSpecialDate(targetDate);
        newDate.setDateName(sourceDate.getDateName() + " (复制)");
        newDate.setStatus("inactive");
        newDate.setCreateTime(null);
        newDate.setUpdateTime(null);

        return baseMapper.insert(newDate) > 0;
    }

    /**
     * 查询按日期类型分组的统计信息
     */
    @Override
    public List<Map<String, Object>> getStatsByDateType(String tenantId, Long deptId,
            LocalDate startDate, LocalDate endDate) {
        return baseMapper.selectStatsByDateType(tenantId, deptId, startDate, endDate);
    }

    /**
     * 清理过期的特殊日期价格
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer cleanExpiredDates(LocalDate beforeDate) {
        return baseMapper.deleteExpiredDates(beforeDate);
    }

    /**
     * 导出特殊日期价格数据
     */
    @Override
    public List<PmsSpecialDatePricingVo> exportList(PmsSpecialDatePricingBo bo) {
        LambdaQueryWrapper<PmsSpecialDatePricing> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }
}
