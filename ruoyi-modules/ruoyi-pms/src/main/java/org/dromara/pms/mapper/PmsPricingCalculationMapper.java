package org.dromara.pms.mapper;

import org.dromara.pms.domain.PmsPricingCalculation;
import org.dromara.pms.domain.vo.PmsPricingCalculationVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 价格计算历史Mapper接口
 *
 * @author PMS
 * @date 2024-12-01
 */
public interface PmsPricingCalculationMapper extends BaseMapperPlus<PmsPricingCalculation, PmsPricingCalculationVo> {

    /**
     * 查询价格趋势数据
     *
     * @param tenantId   租户ID
     * @param deptId     部门ID
     * @param roomTypeId 房型ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 价格趋势数据
     */
    List<Map<String, Object>> selectPriceTrend(@Param("tenantId") String tenantId,
            @Param("deptId") Long deptId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查询规则效果分析数据
     *
     * @param tenantId  租户ID
     * @param deptId    部门ID
     * @param ruleId    规则ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 规则效果分析数据
     */
    List<Map<String, Object>> selectRuleEffectAnalysis(@Param("tenantId") String tenantId,
            @Param("deptId") Long deptId,
            @Param("ruleId") Long ruleId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查询收益分析数据
     *
     * @param tenantId  租户ID
     * @param deptId    部门ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 收益分析数据
     */
    List<Map<String, Object>> selectRevenueAnalysis(@Param("tenantId") String tenantId,
            @Param("deptId") Long deptId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查询平均价格
     *
     * @param tenantId   租户ID
     * @param deptId     部门ID
     * @param roomTypeId 房型ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 平均价格
     */
    Map<String, Object> selectAveragePrice(@Param("tenantId") String tenantId,
            @Param("deptId") Long deptId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查询价格分布统计
     *
     * @param tenantId   租户ID
     * @param deptId     部门ID
     * @param roomTypeId 房型ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 价格分布统计
     */
    List<Map<String, Object>> selectPriceDistribution(@Param("tenantId") String tenantId,
            @Param("deptId") Long deptId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查询渠道价格对比
     *
     * @param tenantId   租户ID
     * @param deptId     部门ID
     * @param roomTypeId 房型ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 渠道价格对比数据
     */
    List<Map<String, Object>> selectChannelPriceComparison(@Param("tenantId") String tenantId,
            @Param("deptId") Long deptId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 删除过期的计算记录
     *
     * @param beforeDate 删除此日期之前的记录
     * @return 删除的记录数
     */
    int deleteExpiredRecords(@Param("beforeDate") LocalDateTime beforeDate);
}
