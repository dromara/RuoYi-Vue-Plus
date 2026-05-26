package org.dromara.common.push.annotation;

import org.dromara.common.push.condition.MessageTransportCondition;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 按消息推送传输方式启用组件。
 *
 * @author Lion Li
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(MessageTransportCondition.class)
public @interface ConditionalOnMessageTransport {

    /**
     * 传输方式：sse / websocket。
     */
    String value();

}
