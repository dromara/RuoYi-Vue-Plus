package org.dromara.workflow.annotation;


import java.lang.annotation.*;

/**
 * 流程任务监听注解
 *
 * @author may
 * @date 2023-12-27
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface FlowListenerAnnotation {

    /**
     * 流程定义key
     */
    String processDefinitionKey();

    /**
     * 节点id
     */
    String taskDefId() default "";
}
