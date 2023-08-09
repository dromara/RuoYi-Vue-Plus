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

    /**
     * 会签任务总数
     */
    String NUMBER_OF_INSTANCES = "nrOfInstances";

    /**
     * 正在执行的会签总数
     */
    String NUMBER_OF_ACTIVE_INSTANCES = "nrOfActiveInstances";

    /**
     * 已完成的会签任务总数
     */
    String NUMBER_OF_COMPLETED_INSTANCES = "nrOfCompletedInstances";

    /**
     * 循环的索引值，可以使用elementIndexVariable属性修改loopCounter的变量名
     */
    String LOOP_COUNTER = "loopCounter";

    String ZIP = "ZIP";

    /**
     * 流程实例对象
     */
    String PROCESS_INSTANCE_VO = "processInstanceVo";

    /**
     * 流程发起人
     */
    String INITIATOR = "INITIATOR";

    /**
     * 开启跳过表达式变量
     */
    String FLOWABLE_SKIP_EXPRESSION_ENABLED = "_FLOWABLE_SKIP_EXPRESSION_ENABLED";

}
