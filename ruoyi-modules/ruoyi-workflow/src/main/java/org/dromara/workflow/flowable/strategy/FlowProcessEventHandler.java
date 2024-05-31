package org.dromara.workflow.flowable.strategy;


/**
 * 总体流程监听(例如: 撤销 提交 退回 等)
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
