package org.dromara.pms.service;

import org.dromara.pms.domain.vo.PmsPricingCalculationVo;
import org.dromara.pms.domain.bo.PmsPricingCalculationBo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 价格计算历史Service接口
 *
 * @author PMS
 * @date 2024-12-01
 */
public interface IPmsPricingCalculationService {

    /**
     * 查询价格计算历史
     */
    PmsPricingCalculationVo queryById(Long calculationId);

    /**
     * 查询价格计算历史列表
     */
    TableDataInfo<PmsPricingCalculationVo> queryPageList(PmsPricingCalculationBo bo, PageQuery pageQuery);

    /**
     * 查询价格计算历史列表
     */
    List<PmsPricingCalculationVo> queryList(PmsPricingCalculationBo bo);

    /**
     * 新增价格计算历史
     */
    Boolean insertByBo(PmsPricingCalculationBo bo);

    /**
     * 修改价格计算历史
     */
    Boolean updateByBo(PmsPricingCalculationBo bo);

    /**
     * 校验并批量删除价格计算历史信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 计算房间价格
     *
     * @param tenantId           租户ID
     * @param deptId             部门ID
     * @param roomTypeId         房型ID
     * @param checkInDate        入住日期
     * @param checkOutDate       离店日期
     * @param numAdults          成人数
     * @param numChildren        儿童数
     * @param channelCode        渠道代码
     * @param memberLevel        会员等级
     * @param advanceBookingDays 提前预订天数
     * @param saveHistory        是否保存计算历史
     * @return 价格计算结果
     */
    Map<String, Object> calculatePrice(String tenantId, Long deptId, Long roomTypeId,
            LocalDate checkInDate, LocalDate checkOutDate, Integer numAdults, Integer numChildren,
            String channelCode, String memberLevel, Integer advanceBookingDays, Boolean saveHistory);

    /**
     * 批量计算价格
     *
     * @param calculations 计算参数列表
     * @return 计算结果列表
     */
    List<Map<String, Object>> batchCalculatePrice(List<PmsPricingCalculationBo> calculations);

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
    List<Map<String, Object>> getPriceTrend(String tenantId, Long deptId, Long roomTypeId,
            LocalDate startDate, LocalDate endDate);

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
    List<Map<String, Object>> getRuleEffectAnalysis(String tenantId, Long deptId, Long ruleId,
            LocalDate startDate, LocalDate endDate);

    /**
     * 查询收益分析数据
     *
     * @param tenantId  租户ID
     * @param deptId    部门ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 收益分析数据
     */
    List<Map<String, Object>> getRevenueAnalysis(String tenantId, Long deptId,
            LocalDate startDate, LocalDate endDate);

    /**
     * 查询平均价格
     *
     * @param tenantId   租户ID
     * @param deptId     部门ID
     * @param roomTypeId 房型ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 平均价格信息
     */
    Map<String, Object> getAveragePrice(String tenantId, Long deptId, Long roomTypeId,
            LocalDate startDate, LocalDate endDate);

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
    List<Map<String, Object>> getPriceDistribution(String tenantId, Long deptId, Long roomTypeId,
            LocalDate startDate, LocalDate endDate);

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
    List<Map<String, Object>> getChannelPriceComparison(String tenantId, Long deptId, Long roomTypeId,
            LocalDate startDate, LocalDate endDate);

    /**
     * 清理过期的计算记录
     *
     * @param beforeDate 删除此日期之前的记录
     * @return 删除的记录数
     */
    Integer cleanExpiredRecords(LocalDateTime beforeDate);

    /**
     * 导出价格计算历史数据
     *
     * @param bo 查询条件
     * @return 导出数据
     */
    List<PmsPricingCalculationVo> exportList(PmsPricingCalculationBo bo);
}
