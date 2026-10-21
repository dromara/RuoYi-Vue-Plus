package org.dromara.workflow.service.impl;

import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.constant.EventCons;
import org.dromara.warm.flow.core.dto.EventWaitQuery;
import org.dromara.warm.flow.core.entity.EventWait;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.enums.EventType;
import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.warm.flow.core.utils.page.Page;
import org.dromara.warm.flow.core.workflow.context.WorkflowContext;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.domain.bo.FlowEventWaitBo;
import org.dromara.workflow.domain.bo.FlowTimerTriggerBo;
import org.dromara.workflow.domain.vo.FlowEventWaitVo;
import org.dromara.workflow.service.IFlwEventWaitService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务（事件等待记录）服务实现。
 * <p>条件解析与合法性校验由引擎 {@link EventWaitQuery} 与门面层完成，本类只负责参数透传、
 * 引擎实体到视图对象的装配（流程名 / 业务主键）与状态文案。手动补发复用引擎
 * {@code InsService#startByTimer}，不读写等待记录，定时任务下次触发时间不受影响。</p>
 *
 * @author may
 * @since 2026/10/21
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@Slf4j
@Service
public class FlwEventWaitServiceImpl implements IFlwEventWaitService {

    /**
     * 事件类型持久化值 → 页面文案。
     */
    private static final Map<String, String> EVENT_TYPE_NAMES = new LinkedHashMap<>();

    /**
     * 等待状态持久化值 → 页面文案。
     */
    private static final Map<String, String> STATUS_NAMES = new LinkedHashMap<>();

    static {
        EVENT_TYPE_NAMES.put(EventType.MESSAGE_START.getValue(), "消息启动");
        EVENT_TYPE_NAMES.put(EventType.TIMER_START.getValue(), "定时启动");
        EVENT_TYPE_NAMES.put(EventType.TIMER_CATCH.getValue(), "定时捕获");
        EVENT_TYPE_NAMES.put(EventType.MESSAGE_CATCH.getValue(), "消息捕获");
        EVENT_TYPE_NAMES.put(EventType.MESSAGE_THROW.getValue(), "消息抛出");
        EVENT_TYPE_NAMES.put(EventType.TIMER_BOUNDARY.getValue(), "定时边界");
        EVENT_TYPE_NAMES.put(EventType.MESSAGE_BOUNDARY.getValue(), "消息边界");
        EVENT_TYPE_NAMES.put(EventType.TERMINATE_END.getValue(), "终止结束");
        EVENT_TYPE_NAMES.put(EventType.MESSAGE_END.getValue(), "消息结束");
        EVENT_TYPE_NAMES.put(EventType.ERROR_CATCH.getValue(), "错误捕获");
        EVENT_TYPE_NAMES.put(EventType.ERROR_BOUNDARY.getValue(), "错误边界");

        STATUS_NAMES.put(EventCons.STATUS_WAITING, "待触发");
        STATUS_NAMES.put(EventCons.STATUS_RETRY, "重试中");
        STATUS_NAMES.put(EventCons.STATUS_RUNNING, "执行中");
        STATUS_NAMES.put(EventCons.STATUS_CANCELLED, "已取消");
        STATUS_NAMES.put(EventCons.STATUS_DEAD, "已死");
        STATUS_NAMES.put(EventCons.STATUS_SUCCESS, "成功");
    }

    /**
     * 分页查询事件等待记录。
     *
     * @param bo 查询条件
     * @return 分页等待记录
     */
    @Override
    public PageResult<FlowEventWaitVo> queryPageList(FlowEventWaitBo bo) {
        Page<EventWait> page = FlowEngine.workflow().pageEventWaits(bo.toQuery());
        List<EventWait> items = page.getList() == null ? List.of() : page.getList();
        boolean timerEnabled = FlowEngine.getFlowConfig().isTimerEnabled();
        List<FlowEventWaitVo> rows = new ArrayList<>(items.size());
        for (EventWait wait : items) {
            FlowEventWaitVo vo = toVo(wait);
            vo.setTimerEnabled(timerEnabled);
            rows.add(vo);
        }
        return PageResult.build(rows, page.getTotal());
    }

    /**
     * 按同一组筛选条件统计各状态等待记录数量。
     *
     * @param bo 查询条件
     * @return 各状态计数
     */
    @Override
    public Map<String, Long> summary(FlowEventWaitBo bo) {
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("waiting", countByStatus(bo, EventCons.STATUS_WAITING));
        summary.put("retry", countByStatus(bo, EventCons.STATUS_RETRY));
        summary.put("running", countByStatus(bo, EventCons.STATUS_RUNNING));
        summary.put("dead", countByStatus(bo, EventCons.STATUS_DEAD));
        summary.put("cancelled", countByStatus(bo, EventCons.STATUS_CANCELLED));
        return summary;
    }

    /**
     * 查询单条等待记录详情。
     *
     * @param id 事件等待记录主键
     * @return 等待记录详情，不存在时返回 null
     */
    @Override
    public FlowEventWaitVo queryById(Long id) {
        EventWait wait = FlowEngine.eventWaitService().getById(id);
        if (wait == null) {
            return null;
        }
        FlowEventWaitVo vo = toVo(wait);
        vo.setTimerEnabled(FlowEngine.getFlowConfig().isTimerEnabled());
        return vo;
    }

    /**
     * 手动补发一次定时启动任务（运维应急）。
     * <p>
     * 只做「按定义 + 事件节点直接发起」，<b>不读写等待记录</b>：定时任务的下次触发时间保持原样，
     * 提前补发不会吞掉当天的正常触发。业务主键优先取调用方传入值，留空时用雪花 long 型 id
     * （与宿主其它业务表主键同型，便于下游按 long 解析），天然不会与定时触发的
     * {@code T{waitId}_{计划时间}} 撞号。加分布式锁防止同机/多机双击连发。
     *
     * @param bo 补发参数
     * @return 新建实例主键
     */
    @Override
    @Lock4j(keys = {"#bo.waitId"})
    public Long manualTrigger(FlowTimerTriggerBo bo) {
        if (bo == null || bo.getWaitId() == null) {
            throw new ServiceException("事件等待记录主键不能为空");
        }
        EventWait wait = FlowEngine.eventWaitService().getById(bo.getWaitId());
        if (wait == null) {
            throw new ServiceException("事件等待记录不存在: " + bo.getWaitId());
        }
        if (!EventType.TIMER_START.getValue().equals(wait.getEventType())) {
            throw new ServiceException("只有定时启动任务支持手动触发，当前事件类型为 " + wait.getEventType());
        }
        String finalBusinessId = StringUtils.isNotBlank(bo.getBusinessId())
            ? bo.getBusinessId().trim()
            : String.valueOf(IdWorker.getId());
        WorkflowContext context = new WorkflowContext();
        context.setHandler(LoginHelper.getUserIdStr());
        try {
            Instance instance = FlowEngine.insService().startByTimer(finalBusinessId, wait.getDefinitionId()
                , wait.getNodeCode(), bo.getVariables(), context);
            return instance.getId();
        } catch (FlowException e) {
            log.warn("定时启动任务手动补发失败 waitId={} 原因={}", bo.getWaitId(), e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 统计某一状态下的等待记录数量。
     *
     * @param bo     查询参数（其中状态条件被覆盖）
     * @param status 等待状态
     * @return 记录数量
     */
    private long countByStatus(FlowEventWaitBo bo, String status) {
        EventWaitQuery query = bo.toQuery();
        query.setStatuses(List.of(status));
        return FlowEngine.workflow().countEventWaits(query);
    }

    /**
     * 引擎等待记录 → 视图对象。
     *
     * @param wait 引擎等待记录
     * @return 视图对象
     */
    private FlowEventWaitVo toVo(EventWait wait) {
        FlowEventWaitVo vo = new FlowEventWaitVo();
        vo.setId(wait.getId());
        vo.setDefinitionId(wait.getDefinitionId());
        vo.setInstanceId(wait.getInstanceId());
        vo.setNodeCode(wait.getNodeCode());
        vo.setEventType(wait.getEventType());
        vo.setEventName(wait.getEventName());
        vo.setAttachedTaskId(wait.getAttachedTaskId());
        vo.setTriggerType(wait.getTriggerType());
        vo.setTriggerInterval(wait.getTriggerInterval());
        vo.setNextTriggerAt(wait.getNextTriggerAt());
        vo.setTimezone(wait.getTimezone());
        vo.setRepeatCount(wait.getRepeatCount());
        vo.setMaxRepeatCount(wait.getMaxRepeatCount());
        vo.setStatus(wait.getStatus());
        vo.setRetryCount(wait.getRetryCount());
        vo.setMaxRetryCount(wait.getMaxRetryCount());
        vo.setRetryAt(wait.getRetryAt());
        vo.setPayload(wait.getPayload());
        vo.setLastError(wait.getLastError());
        vo.setCreateTime(wait.getCreateTime());
        vo.setUpdateTime(wait.getUpdateTime());
        vo.setEventTypeName(EVENT_TYPE_NAMES.getOrDefault(wait.getEventType(), wait.getEventType()));
        vo.setStatusName(STATUS_NAMES.getOrDefault(wait.getStatus(), wait.getStatus()));
        if (wait.getDefinitionId() != null) {
            var definition = FlowEngine.defService().getById(wait.getDefinitionId());
            vo.setFlowName(definition == null ? null : definition.getFlowName());
        }
        if (wait.getInstanceId() != null) {
            var instance = FlowEngine.insService().getById(wait.getInstanceId());
            vo.setBusinessId(instance == null ? null : instance.getBusinessId());
        }
        return vo;
    }
}
