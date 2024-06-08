package org.dromara.workflow.flowable.handler;

import org.dromara.common.core.domain.event.ProcessEvent;
import org.dromara.common.core.domain.event.ProcessTaskEvent;
import org.dromara.common.core.utils.SpringUtils;
import org.springframework.stereotype.Component;

/**
 * 流程监听服务
 *
 * @author may
 * @date 2024-06-02
 */
@Component
public class FlowProcessEventHandler {

    /**
     * 总体流程监听(例如: 提交 退回 撤销 终止 作废等)
     *
     * @param key         流程key
     * @param businessKey 业务id
     * @param status      状态
     * @param submit      当为true时为申请人节点办理
     */
    public void processHandler(String key, String businessKey, String status, boolean submit) {
        ProcessEvent processEvent = new ProcessEvent();
        processEvent.setKey(key);
        processEvent.setBusinessKey(businessKey);
        processEvent.setStatus(status);
        processEvent.setSubmit(submit);
        SpringUtils.context().publishEvent(processEvent);
    }

    /**
     * 执行办理任务监听
     *
     * @param key               流程key
     * @param taskDefinitionKey 审批节点key
     * @param taskId            任务id
     * @param businessKey       业务id
     */
    public void processTaskHandler(String key, String taskDefinitionKey, String taskId, String businessKey) {
        ProcessTaskEvent processTaskEvent = new ProcessTaskEvent();
        processTaskEvent.setKey(key);
        processTaskEvent.setTaskDefinitionKey(taskDefinitionKey);
        processTaskEvent.setTaskId(taskId);
        processTaskEvent.setBusinessKey(businessKey);
        SpringUtils.context().publishEvent(processTaskEvent);
    }
}
