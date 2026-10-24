package org.dromara.workflow.demo;

import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.EventWait;
import org.dromara.warm.flow.core.enums.EventType;
import org.dromara.warm.flow.core.service.impl.EventRuntimeHandler;
import org.dromara.warm.flow.core.timer.EventTaskHandler;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.springframework.stereotype.Component;

/**
 * 定时启动 + 业务绑定示例：每日 09:00 自动创建巡检工单，并以工单主键发起审批。
 *
 * <p>接入要点：实现 {@link EventTaskHandler} 后引擎内置默认处理器（EventRuntimeHandler）
 * 自动让位，本类成为全部事件的处理入口——非本演示流程的事件必须委托默认处理器，
 * 否则会覆盖引擎原生的定时启动与边界事件行为；等待记录的完成由默认三参编排
 * （handle 后执行 completion），实现里无需自行调用。</p>
 *
 * <p>使用方式：发布含「定时启动」节点的定义（流程编码 {@link #DEMO_FLOW_CODE}），
 * 开启 {@code warm-flow.timer-enabled}。</p>
 *
 * @author may
 */
@Slf4j
@Component
@ConditionalOnEnable
public class DemoTimerInspectHandler implements EventTaskHandler {

    /**
     * 演示流程编码：对应设计器发布的含「定时启动」节点的定义
     */
    public static final String DEMO_FLOW_CODE = "demo_timer_inspect";

    /**
     * 引擎内置默认处理器：兜底所有非本演示流程的事件
     */
    private final EventTaskHandler defaultHandler = new EventRuntimeHandler();

    /**
     * 定时调度入口（无上下文）：统一处理。
     */
    @Override
    public void handle(EventWait wait) {
        if (isInspectTimerStart(wait)) {
            // 业务先行：创建巡检工单，以工单主键作为 businessId 发起审批
            long l = System.currentTimeMillis();
            FlowEngine.insService().startByTimer(String.valueOf(l), wait.getDefinitionId(),
                wait.getNodeCode(), null, null);
            log.info("巡检工单 {} 已自动发起审批: businessId={}", l, l);
            return;
        }
        // 非本流程的事件：委托默认处理器，保持定时启动与边界事件原生行为
        defaultHandler.handle(wait);
    }

    /**
     * 只拦截本演示流程的定时启动事件。
     */
    private boolean isInspectTimerStart(EventWait wait) {
        return EventType.TIMER_START.getValue().equals(wait.getEventType())
            && DEMO_FLOW_CODE.equals(FlowEngine.defService()
                .getById(wait.getDefinitionId()).getFlowCode());
    }
}
