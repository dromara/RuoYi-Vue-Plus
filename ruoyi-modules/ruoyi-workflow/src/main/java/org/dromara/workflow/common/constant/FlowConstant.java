package org.dromara.workflow.common.constant;


/**
 * 工作流常量
 *
 * @author may
 */
public interface FlowConstant {

    String MESSAGE_CURRENT_TASK_IS_NULL = "当前任务不存在或你不是任务办理人！";

    String MESSAGE_SUSPENDED = "当前任务已挂起不可审批！";

    /**
     * 连线
     */
    String SEQUENCE_FLOW = "sequenceFlow";

    /**
     * 并行网关
     */
    String PARALLEL_GATEWAY = "parallelGateway";

    /**
     * 排它网关
     */
    String EXCLUSIVE_GATEWAY = "exclusiveGateway";

    /**
     * 包含网关
     */
    String INCLUSIVE_GATEWAY = "inclusiveGateway";

    /**
     * 结束节点
     */
    String END_EVENT = "endEvent";


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
     * 业务与流程实例关联对象
     */
    String BUSINESS_INSTANCE_DTO = "businessInstanceDTO";

    /**
     * 流程定义配置
     */
    String WF_DEFINITION_CONFIG_VO = "wfDefinitionConfigVo";

    /**
     * 节点配置
     */
    String WF_NODE_CONFIG_VO = "wfNodeConfigVo";

    /**
     * 流程发起人
     */
    String INITIATOR = "initiator";

    /**
     * 流程实例id
     */
    String PROCESS_INSTANCE_ID = "processInstanceId";

    /**
     * 业务id
     */
    String BUSINESS_KEY = "businessKey";

    /**
     * 流程定义id
     */
    String PROCESS_DEFINITION_ID = "processDefinitionId";

    /**
     * 开启跳过表达式变量
     */
    String FLOWABLE_SKIP_EXPRESSION_ENABLED = "_FLOWABLE_SKIP_EXPRESSION_ENABLED";

    /**
     * 模型标识key命名规范正则表达式
     */
    String MODEL_KEY_PATTERN = "^[a-zA-Z][a-zA-Z0-9_]{0,254}$";

    /**
     * 用户任务
     */
    String USER_TASK = "userTask";

    /**
     * 会签
     */
    String MULTI_INSTANCE = "multiInstance";

    /**
     * 是
     */
    String TRUE = "0";

    /**
     * 否
     */
    String FALSE = "1";
}
