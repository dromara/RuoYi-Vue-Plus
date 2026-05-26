package org.dromara.common.push.condition;

import org.dromara.common.push.annotation.ConditionalOnMessageTransport;
import org.dromara.common.push.enums.MessageTransportEnum;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * 消息推送传输方式条件判断。
 *
 * @author Lion Li
 */
public class MessageTransportCondition implements Condition {

    /**
     * 判断当前消息推送配置是否匹配注解声明的传输方式。
     *
     * @param context 条件上下文
     * @param metadata 注解元数据
     * @return 是否匹配
     */
    @Override
    public boolean matches(@NonNull ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnMessageTransport.class.getName());
        if (attributes == null) {
            return true;
        }

        Boolean enabled = context.getEnvironment().getProperty("message.enabled", Boolean.class, true);
        String transport = context.getEnvironment().getProperty("message.transport", MessageTransportEnum.SSE.getCode());
        String expected = (String) attributes.get("value");
        return enabled && expected.equalsIgnoreCase(transport);
    }

}
