package org.dromara.pms.mapper;

import org.dromara.pms.domain.PmsRoomPricingRule;
import org.dromara.pms.domain.vo.PmsRoomPricingRuleVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 房间价格规则Mapper接口
 *
 * @author PMS
 * @date 2024-12-01
 */
public interface PmsRoomPricingRuleMapper extends BaseMapperPlus<PmsRoomPricingRule, PmsRoomPricingRuleVo> {

    /**
     * 查询适用的价格规则
     *
     * @param tenantId           租户ID
     * @param deptId             部门ID
     * @param roomTypeId         房型ID
     * @param checkInDate        入住日期
     * @param checkOutDate       离店日期
     * @param channelCode        渠道代码
     * @param memberLevel        会员等级
     * @param guestCount         客人数量
     * @param advanceBookingDays 提前预订天数
     * @return 适用的价格规则列表
     */
    List<PmsRoomPricingRule> selectApplicableRules(@Param("tenantId") String tenantId,
            @Param("deptId") Long deptId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("channelCode") String channelCode,
            @Param("memberLevel") String memberLevel,
            @Param("guestCount") Integer guestCount,
            @Param("advanceBookingDays") Integer advanceBookingDays);

    /**
     * 查询有效的价格规则
     *
     * @param tenantId 租户ID
     * @param deptId   部门ID
     * @param status   状态
     * @return 有效的价格规则列表
     */
    List<PmsRoomPricingRule> selectActiveRules(@Param("tenantId") String tenantId,
            @Param("deptId") Long deptId,
            @Param("status") String status);

    /**
     * 查询规则使用统计
     *
     * @param ruleId 规则ID
     * @return 使用统计信息
     */
    Integer selectRuleUsageCount(@Param("ruleId") Long ruleId);

    /**
     * 更新规则使用次数
     *
     * @param ruleId 规则ID
     * @param count  增加的使用次数
     * @return 更新结果
     */
    int updateRuleUsageCount(@Param("ruleId") Long ruleId, @Param("count") Integer count);

    /**
     * 查询冲突的价格规则
     *
     * @param tenantId       租户ID
     * @param deptId         部门ID
     * @param roomTypeId     房型ID
     * @param dateRangeStart 开始日期
     * @param dateRangeEnd   结束日期
     * @param excludeRuleId  排除的规则ID
     * @return 冲突的价格规则列表
     */
    List<PmsRoomPricingRule> selectConflictingRules(@Param("tenantId") String tenantId,
            @Param("deptId") Long deptId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("dateRangeStart") LocalDate dateRangeStart,
            @Param("dateRangeEnd") LocalDate dateRangeEnd,
            @Param("excludeRuleId") Long excludeRuleId);

    /**
     * 批量更新规则状态
     *
     * @param ruleIds 规则ID列表
     * @param status  新状态
     * @return 更新结果
     */
    int batchUpdateStatus(@Param("ruleIds") List<Long> ruleIds, @Param("status") String status);
}
