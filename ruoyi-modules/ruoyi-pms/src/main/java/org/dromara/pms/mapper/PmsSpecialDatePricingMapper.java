package org.dromara.pms.mapper;

import org.dromara.pms.domain.PmsSpecialDatePricing;
import org.dromara.pms.domain.vo.PmsSpecialDatePricingVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 特殊日期价格Mapper接口
 *
 * @author PMS
 * @date 2024-12-01
 */
public interface PmsSpecialDatePricingMapper extends BaseMapperPlus<PmsSpecialDatePricing, PmsSpecialDatePricingVo> {

    /**
     * 查询指定日期范围内的特殊日期价格
     *
     * @param tenantId   租户ID
     * @param deptId     部门ID
     * @param roomTypeId 房型ID (NULL表示全部房型)
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 特殊日期价格列表
     */
    List<PmsSpecialDatePricing> selectByDateRange(@Param("tenantId") String tenantId,
            @Param("deptId") Long deptId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查询指定日期的特殊价格
     *
     * @param tenantId    租户ID
     * @param deptId      部门ID
     * @param roomTypeId  房型ID
     * @param specialDate 特殊日期
     * @return 特殊日期价格
     */
    PmsSpecialDatePricing selectByDate(@Param("tenantId") String tenantId,
            @Param("deptId") Long deptId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("specialDate") LocalDate specialDate);

    /**
     * 查询有效的特殊日期价格
     *
     * @param tenantId 租户ID
     * @param deptId   部门ID
     * @param status   状态
     * @return 有效的特殊日期价格列表
     */
    List<PmsSpecialDatePricing> selectActiveSpecialDates(@Param("tenantId") String tenantId,
            @Param("deptId") Long deptId,
            @Param("status") String status);

    /**
     * 查询冲突的特殊日期价格
     *
     * @param tenantId      租户ID
     * @param deptId        部门ID
     * @param roomTypeId    房型ID
     * @param specialDate   特殊日期
     * @param excludeDateId 排除的特殊日期ID
     * @return 冲突的特殊日期价格列表
     */
    List<PmsSpecialDatePricing> selectConflictingDates(@Param("tenantId") String tenantId,
            @Param("deptId") Long deptId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("specialDate") LocalDate specialDate,
            @Param("excludeDateId") Long excludeDateId);

    /**
     * 批量插入特殊日期价格
     *
     * @param specialDatePricings 特殊日期价格列表
     * @return 插入结果
     */
    int batchInsert(@Param("list") List<PmsSpecialDatePricing> specialDatePricings);

    /**
     * 批量更新特殊日期价格状态
     *
     * @param dateIds 特殊日期ID列表
     * @param status  新状态
     * @return 更新结果
     */
    int batchUpdateStatus(@Param("dateIds") List<Long> dateIds, @Param("status") String status);

    /**
     * 查询按日期类型分组的统计信息
     *
     * @param tenantId  租户ID
     * @param deptId    部门ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 统计信息
     */
    List<java.util.Map<String, Object>> selectStatsByDateType(@Param("tenantId") String tenantId,
            @Param("deptId") Long deptId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 删除过期的特殊日期价格
     *
     * @param beforeDate 删除此日期之前的记录
     * @return 删除的记录数
     */
    int deleteExpiredDates(@Param("beforeDate") LocalDate beforeDate);
}
