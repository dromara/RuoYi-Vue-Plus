package org.dromara.pms.service;

import org.dromara.pms.domain.PmsSpecialDatePricing;
import org.dromara.pms.domain.vo.PmsSpecialDatePricingVo;
import org.dromara.pms.domain.bo.PmsSpecialDatePricingBo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 特殊日期价格Service接口
 *
 * @author PMS
 * @date 2024-12-01
 */
public interface IPmsSpecialDatePricingService {

    /**
     * 查询特殊日期价格
     */
    PmsSpecialDatePricingVo queryById(Long specialDateId);

    /**
     * 查询特殊日期价格列表
     */
    TableDataInfo<PmsSpecialDatePricingVo> queryPageList(PmsSpecialDatePricingBo bo, PageQuery pageQuery);

    /**
     * 查询特殊日期价格列表
     */
    List<PmsSpecialDatePricingVo> queryList(PmsSpecialDatePricingBo bo);

    /**
     * 新增特殊日期价格
     */
    Boolean insertByBo(PmsSpecialDatePricingBo bo);

    /**
     * 修改特殊日期价格
     */
    Boolean updateByBo(PmsSpecialDatePricingBo bo);

    /**
     * 校验并批量删除特殊日期价格信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 查询指定日期范围内的特殊日期价格
     *
     * @param tenantId   租户ID
     * @param deptId     部门ID
     * @param roomTypeId 房型ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 特殊日期价格列表
     */
    List<PmsSpecialDatePricing> getSpecialDatesByRange(String tenantId, Long deptId, Long roomTypeId,
            LocalDate startDate, LocalDate endDate);

    /**
     * 查询指定日期的特殊价格
     *
     * @param tenantId    租户ID
     * @param deptId      部门ID
     * @param roomTypeId  房型ID
     * @param specialDate 特殊日期
     * @return 特殊日期价格
     */
    PmsSpecialDatePricing getSpecialDateByDate(String tenantId, Long deptId, Long roomTypeId,
            LocalDate specialDate);

    /**
     * 查询有效的特殊日期价格
     *
     * @param tenantId 租户ID
     * @param deptId   部门ID
     * @return 有效的特殊日期价格列表
     */
    List<PmsSpecialDatePricing> getActiveSpecialDates(String tenantId, Long deptId);

    /**
     * 检查特殊日期冲突
     *
     * @param bo 特殊日期价格业务对象
     * @return 是否存在冲突
     */
    Boolean checkDateConflict(PmsSpecialDatePricingBo bo);

    /**
     * 批量设置特殊日期价格
     *
     * @param specialDates 特殊日期价格列表
     * @return 操作结果
     */
    Boolean batchInsertSpecialDates(List<PmsSpecialDatePricingBo> specialDates);

    /**
     * 批量更新特殊日期价格状态
     *
     * @param dateIds 特殊日期ID列表
     * @param status  新状态
     * @return 更新结果
     */
    Boolean batchUpdateStatus(List<Long> dateIds, String status);

    /**
     * 启用特殊日期价格
     *
     * @param specialDateId 特殊日期ID
     * @return 操作结果
     */
    Boolean enableSpecialDate(Long specialDateId);

    /**
     * 禁用特殊日期价格
     *
     * @param specialDateId 特殊日期ID
     * @return 操作结果
     */
    Boolean disableSpecialDate(Long specialDateId);

    /**
     * 复制特殊日期价格到其他日期
     *
     * @param sourceId   源特殊日期ID
     * @param targetDate 目标日期
     * @return 操作结果
     */
    Boolean copySpecialDate(Long sourceId, LocalDate targetDate);

    /**
     * 查询按日期类型分组的统计信息
     *
     * @param tenantId  租户ID
     * @param deptId    部门ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 统计信息
     */
    List<Map<String, Object>> getStatsByDateType(String tenantId, Long deptId,
            LocalDate startDate, LocalDate endDate);

    /**
     * 清理过期的特殊日期价格
     *
     * @param beforeDate 删除此日期之前的记录
     * @return 删除的记录数
     */
    Integer cleanExpiredDates(LocalDate beforeDate);

    /**
     * 导出特殊日期价格数据
     *
     * @param bo 查询条件
     * @return 导出数据
     */
    List<PmsSpecialDatePricingVo> exportList(PmsSpecialDatePricingBo bo);
}
