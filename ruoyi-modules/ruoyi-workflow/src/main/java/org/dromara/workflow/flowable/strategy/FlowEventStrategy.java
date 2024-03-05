package org.dromara.workflow.flowable.strategy;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.workflow.annotation.FlowListenerAnnotation;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 流程任务监听策略
 *
 * @author may
 * @date 2023-12-27
 */
@Component
public class FlowEventStrategy implements BeanPostProcessor {

    private final Map<String, FlowTaskEventHandler> flowTaskEventHandlers = new HashMap<>();
    private final Map<String, FlowProcessEventHandler> flowProcessEventHandlers = new HashMap<>();

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof FlowTaskEventHandler) {
            FlowListenerAnnotation annotation = bean.getClass().getAnnotation(FlowListenerAnnotation.class);
            if (null != annotation) {
                if (StringUtils.isNotBlank(annotation.processDefinitionKey()) && StringUtils.isNotBlank(annotation.taskDefId())) {
                    String id = annotation.processDefinitionKey() + "_" + annotation.taskDefId();
                    if (!flowTaskEventHandlers.containsKey(id)) {
                        flowTaskEventHandlers.put(id, (FlowTaskEventHandler) bean);
                    }
                }
            }
        }
        if (bean instanceof FlowProcessEventHandler) {
            FlowListenerAnnotation annotation = bean.getClass().getAnnotation(FlowListenerAnnotation.class);
            if (null != annotation) {
                if (StringUtils.isNotBlank(annotation.processDefinitionKey()) && StringUtils.isBlank(annotation.taskDefId())) {
                    if (!flowProcessEventHandlers.containsKey(annotation.processDefinitionKey())) {
                        flowProcessEventHandlers.put(annotation.processDefinitionKey(), (FlowProcessEventHandler) bean);
                    }
                }
            }
        }
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }

    /**
     * 获取可执行bean
     *
     * @param key key
     */
    public FlowTaskEventHandler getTaskHandler(String key) {
        if (!flowTaskEventHandlers.containsKey(key)) {
            return null;
        }
        return flowTaskEventHandlers.get(key);
    }

    /**
     * 获取可执行bean
     *
     * @param key key
     */
    public FlowProcessEventHandler getProcessHandler(String key) {
        if (!flowProcessEventHandlers.containsKey(key)) {
            return null;
        }
        return flowProcessEventHandlers.get(key);
    }
}
