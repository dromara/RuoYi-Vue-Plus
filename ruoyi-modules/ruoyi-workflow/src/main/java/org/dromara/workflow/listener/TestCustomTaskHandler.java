package org.dromara.workflow.listener;

import lombok.extern.slf4j.Slf4j;
import org.dromara.workflow.annotation.FlowListenerAnnotation;
import org.dromara.workflow.flowable.strategy.FlowTaskEventHandler;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

/**
 * 自定义监听测试
 *
 * @author may
 * @date 2023-12-27
 */
@Slf4j
@Component
@FlowListenerAnnotation(processDefinitionKey = "leave1", taskDefId = "Activity_14633hx")
public class TestCustomTaskHandler implements FlowTaskEventHandler {

    @Override
    public void handleTask(Task task, String businessKey) {
        log.info("任务名称:" + task.getName() + ",业务ID:" + businessKey);
    }
}
