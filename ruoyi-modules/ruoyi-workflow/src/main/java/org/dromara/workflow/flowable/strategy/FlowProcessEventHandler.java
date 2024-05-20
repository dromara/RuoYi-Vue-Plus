package org.dromara.workflow.flowable.strategy;


/**
 * 流程监听
 *
 * @author may
 * @date 2023-12-27
 */
public interface FlowProcessEventHandler {

    /**
     * 执行办理任务监听
     *
     * @param businessKey 业务id
     * @param status      状态
     * @param submit      当为true时为申请人节点办理
     */
    void handleProcess(String businessKey, String status, boolean submit);
}
