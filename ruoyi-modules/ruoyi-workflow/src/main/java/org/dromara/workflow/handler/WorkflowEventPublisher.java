package org.dromara.workflow.handler;

import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.service.event.EventMessage;
import org.dromara.warm.flow.core.service.event.EventPublisher;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.springframework.stereotype.Component;

/**
 * 流程消息事件的宿主发布器
 * <p>
 * 引擎在 {@code MESSAGE_THROW} / {@code MESSAGE_END} 时经 {@code FlowEngine.eventPublisher()}
 * （即容器里唯一的 {@link EventPublisher} Bean）把消息交出来。没有这个 Bean 时引擎只会告警并跳过，
 * 消息事件看起来“配了但没生效”。
 * <p>
 * 当前实现只落日志，够演示与排查用；接真实中间件时在这里改成发 MQ / 调用外部接口，
 * 注意本方法与流程状态更新在同一事务边界内，需要可靠投递请自行引入 Outbox。
 *
 * @author may
 */
@ConditionalOnEnable
@Slf4j
@Component
public class WorkflowEventPublisher implements EventPublisher {

    @Override
    public void publish(EventMessage message) {
        log.info("收到流程消息事件: eventName={}, eventType={}, definitionId={}, instanceId={}, nodeCode={}, payload={}",
            message.getEventName(), message.getEventType(), message.getDefinitionId(), message.getInstanceId(),
            message.getNodeCode(), message.getPayload());
    }

}
