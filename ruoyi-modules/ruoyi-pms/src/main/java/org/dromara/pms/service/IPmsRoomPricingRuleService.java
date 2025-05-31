package org.dromara.pms.service;

import org.dromara.pms.domain.PmsRoomPricingRule;
import org.dromara.pms.domain.vo.PmsRoomPricingRuleVo;
import org.dromara.pms.domain.bo.PmsRoomPricingRuleBo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 房间价格规则Service接口
 *
 * @author PMS
 * @date 2024-12-01
 */
public interface IPmsRoomPricingRuleService {

    /**
     * 查询房间价格规则
     */
    PmsRoomPricingRuleVo queryById(Long ruleId);

    /**
     * 查询房间价格规则列表
     */
    TableDataInfo<PmsRoomPricingRuleVo> queryPageList(PmsRoomPricingRuleBo bo, PageQuery pageQuery);

    /**
     * 查询房间价格规则列表
     */
    List<PmsRoomPricingRuleVo> queryList(PmsRoomPricingRuleBo bo);

    /**
     * 新增房间价格规则
     */
    Boolean insertByBo(PmsRoomPricingRuleBo bo);

    /**
     * 修改房间价格规则
     */
    Boolean updateByBo(PmsRoomPricingRuleBo bo);

    /**
     * 校验并批量删除房间价格规则信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

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
    List<PmsRoomPricingRule> getApplicableRules(String tenantId, Long deptId, Long roomTypeId,
            LocalDate checkInDate, LocalDate checkOutDate, String channelCode,
            String memberLevel, Integer guestCount, Integer advanceBookingDays);

    /**
     * 查询有效的价格规则
     *
     * @param tenantId 租户ID
     * @param deptId   部门ID
     * @return 有效的价格规则列表
     */
    List<PmsRoomPricingRule> getActiveRules(String tenantId, Long deptId);

    /**
     * 更新规则使用次数
     *
     * @param ruleId 规则ID
     * @param count  增加的使用次数
     * @return 更新结果
     */
    Boolean updateRuleUsageCount(Long ruleId, Integer count);

    /**
     * 检查规则冲突
     *
     * @param bo 价格规则业务对象
     * @return 是否存在冲突
     */
    Boolean checkRuleConflict(PmsRoomPricingRuleBo bo);

    /**
     * 批量更新规则状态
     *
     * @param ruleIds 规则ID列表
     * @param status  新状态
     * @return 更新结果
     */
    Boolean batchUpdateStatus(List<Long> ruleIds, String status);

    /**
     * 启用规则
     *
     * @param ruleId 规则ID
     * @return 操作结果
     */
    Boolean enableRule(Long ruleId);

    /**
     * 禁用规则
     *
     * @param ruleId 规则ID
     * @return 操作结果
     */
    Boolean disableRule(Long ruleId);

    /**
     * 复制规则
     *
     * @param ruleId  源规则ID
     * @param newName 新规则名称
     * @return 操作结果
     */
    Boolean copyRule(Long ruleId, String newName);

    /**
     * 导出价格规则数据
     *
     * @param bo 查询条件
     * @return 导出数据
     */
    List<PmsRoomPricingRuleVo> exportList(PmsRoomPricingRuleBo bo);
}
