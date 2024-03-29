package org.dromara.workflow.flowable.strategy;


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
     * @param taskId      任务ID
     * @param businessKey 业务id
     */
    void handleTask(String taskId, String businessKey);
}
