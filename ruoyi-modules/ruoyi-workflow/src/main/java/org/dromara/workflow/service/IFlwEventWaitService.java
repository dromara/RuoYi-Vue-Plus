package org.dromara.workflow.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.workflow.domain.bo.FlowEventWaitBo;
import org.dromara.workflow.domain.bo.FlowTimerTriggerBo;
import org.dromara.workflow.domain.vo.FlowEventWaitVo;

import java.util.Map;

/**
 * 定时任务（事件等待记录）服务接口。
 * <p>数据来自引擎 flow_event_wait，页面提供查询、状态统计与定时启动任务的手动补发。
 * 手动补发只创建一次实例，不读取也不推进等待记录，定时任务的下次触发时间不受影响。</p>
 *
 * @author may
 * @since 2026/10/21
 */
public interface IFlwEventWaitService {

    /**
     * 分页查询事件等待记录，默认只查定时类事件、按下次触发时间升序。
     *
     * @param bo 查询条件
     * @return 等待记录分页数据
     */
    PageResult<FlowEventWaitVo> queryPageList(FlowEventWaitBo bo);

    /**
     * 按同一组筛选条件统计各状态等待记录数量。
     *
     * @param bo 查询条件（其中状态条件被逐一分组覆盖）
     * @return 各状态计数，键为 waiting / retry / running / dead / cancelled
     */
    Map<String, Long> summary(FlowEventWaitBo bo);

    /**
     * 查询单条等待记录详情。
     *
     * @param id 事件等待记录主键
     * @return 等待记录详情，不存在时返回 {@code null}
     */
    FlowEventWaitVo queryById(Long id);

    /**
     * 手动补发一次定时启动任务（运维应急）。
     * <p>实例发起人记为当前登录用户；等待记录保持不变，「每天 9 点的任务今天提前补发一次」
     * 不会吞掉当天 9 点的正常触发。</p>
     *
     * @param bo 补发参数（等待记录主键、可选业务主键与变量）
     * @return 新建流程实例主键
     */
    Long manualTrigger(FlowTimerTriggerBo bo);
}
