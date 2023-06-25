package org.dromara.workflow.common.constant;


/**
 * 工作流常量
 *
 * @author may
 */
public interface FlowConstant {

    /**
     * 命名空间
     */
    String NAMESPACE = "http://b3mn.org/stencilset/bpmn2.0#";

    String MESSAGE_CURRENT_TASK_IS_NULL = "当前任务不存在或你不是任务办理人！";

    String MESSAGE_SUSPENDED = "当前任务已挂起不可审批！";

    /**
     * 连线
     */
    String SEQUENCE_FLOW = "sequenceFlow";

    /**
     * 流程委派标识
     */
    String PENDING = "PENDING";

    /**
     * 候选人标识
     */
    String CANDIDATE = "candidate";

}
