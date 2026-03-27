package org.dromara.workflow.common.constant;


/**
 * 工作流常量
 *
 * @author may
 */
public interface FlowConstant {

    /**
     * 流程发起人
     */
    String INITIATOR = "initiator";

    /**
     * 业务id
     */
    String BUSINESS_ID = "businessId";

    /**
     * 部门id
     */
    String INITIATOR_DEPT_ID = "initiatorDeptId";

    /**
     * 流程分类 ID 转名称的翻译标识。
     */
    String CATEGORY_ID_TO_NAME = "category_id_to_name";

    /**
     * 流程分类名称缓存键。
     */
    String FLOW_CATEGORY_NAME = "flow_category_name#30d";

    /**
     * 默认租户 OA 申请分类 ID。
     */
    Long FLOW_CATEGORY_ID = 100L;

    /**
     * 申请人提交动作标识。
     */
    String SUBMIT = "submit";

    /**
     * 抄送列表流程变量名。
     */
    String FLOW_COPY_LIST = "flowCopyList";

    /**
     * 消息类型流程变量名。
     */
    String MESSAGE_TYPE = "messageType";

    /**
     * 消息通知内容流程变量名。
     */
    String MESSAGE_NOTICE = "messageNotice";

    /**
     * 我的发起页面路径。
     */
    String PATH_MY_DOCUMENT = "/task/myDocument";

    /**
     * 我的待办页面路径。
     */
    String PATH_TASK_WAITING = "/task/taskWaiting";

    /**
     * 我的已办页面路径。
     */
    String PATH_TASK_FINISH = "/task/taskFinish";

    /**
     * 我的抄送页面路径。
     */
    String PATH_TASK_COPY = "/task/taskCopyList";

    /**
     * 任务状态字典类型编码。
     */
    String WF_TASK_STATUS = "wf_task_status";

    /**
     * 自动通过变量标识。
     */
    String AUTO_PASS = "autoPass";

    /**
     * 业务编码流程变量名。
     */
    String BUSINESS_CODE = "businessCode";

    /**
     * 忽略-办理权限校验（true：忽略，false：不忽略）
     */
    String VAR_IGNORE = "ignore";

    /**
     * 忽略-委派处理（true：忽略，false：不忽略）
     */
    String VAR_IGNORE_DEPUTE = "ignoreDepute";

    /**
     * 忽略-会签票签处理（true：忽略，false：不忽略）
     */
    String VAR_IGNORE_COOPERATE = "ignoreCooperate";

}
