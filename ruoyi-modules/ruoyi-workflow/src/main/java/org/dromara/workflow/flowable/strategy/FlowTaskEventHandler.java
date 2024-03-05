package org.dromara.workflow.flowable.strategy;

import org.flowable.task.api.Task;

/**
 * 流程任务监听
 *
 * @author may
 * @date 2023-12-27
 */
public interface FlowTaskEventHandler {

    /**
     * 执行办理任务监听
     *
     * @param task        任务
     * @param businessKey 业务id
     */
    void handleTask(Task task, String businessKey);
}
