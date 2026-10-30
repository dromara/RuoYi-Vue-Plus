package org.dromara.workflow.demo;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.entity.EventWait;
import org.dromara.warm.flow.core.enums.EventType;
import org.dromara.warm.flow.core.service.impl.EventRuntimeHandler;
import org.dromara.warm.flow.core.timer.EventTaskHandler;
import org.dromara.warm.flow.core.workflow.context.WorkflowContext;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.springframework.stereotype.Component;

/**
 * 定时启动示例：为流程实例提供可被业务页面解析的数字业务 ID。
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
    public static final String DEMO_FLOW_CODE = "demo_timer_start";

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
            // 这里应该使用业务的id demo使用自动生成id 需要用户自行替换
            String businessId = String.valueOf(IdWorker.getId());
            WorkflowContext context = new WorkflowContext();
            context.setHandler(FlowEngine.getFlowConfig().getSystemHandler());
            FlowEngine.insService().startByTimer(businessId, wait.getDefinitionId(),
                wait.getNodeCode(), null, context);
            log.info("定时启动示例已自动发起审批: businessId={}", businessId);
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
