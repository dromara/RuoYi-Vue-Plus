package org.dromara.workflow.api;

import org.dromara.workflow.api.domain.CompleteTaskDTO;
import org.dromara.workflow.api.domain.StartProcessDTO;
import org.dromara.workflow.api.domain.StartProcessReturnDTO;
import org.dromara.workflow.api.domain.StartMessageEventDTO;
import org.dromara.workflow.api.domain.StartTimerEventDTO;
import org.dromara.workflow.api.domain.TriggerMessageEventDTO;

import java.util.List;
import java.util.Map;

/**
 * 通用 工作流服务
 *
 * @author may
 */
public interface WorkflowService {

    /**
     * 运行中的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param businessIds 业务id
     * @return 结果
     */
    boolean deleteInstance(List<String> businessIds);

    /**
     * 获取当前流程状态
     *
     * @param taskId 任务id
     * @return 状态
     */
    String getBusinessStatusByTaskId(Long taskId);

    /**
     * 获取当前流程状态
     *
     * @param businessId 业务id
     * @return 状态
     */
    String getBusinessStatus(String businessId);

    /**
     * 设置流程变量
     *
     * @param instanceId 流程实例id
     * @param variable   流程变量
     */
    void setVariable(Long instanceId, Map<String, Object> variable);

    /**
     * 获取流程变量
     *
     * @param instanceId 流程实例id
     * @return 流程变量详情
     */
    Map<String, Object> instanceVariable(Long instanceId);

    /**
     * 按照业务id查询流程实例id
     *
     * @param businessId 业务id
     * @return 结果
     */
    Long getInstanceIdByBusinessId(String businessId);

    /**
     * 启动流程
     *
     * @param startProcess 参数
     * @return 启动后的流程实例与首任务信息
     */
    StartProcessReturnDTO startWorkFlow(StartProcessDTO startProcess);

    /**
     * 办理任务
     * 系统后台发起审批 无用户信息 需要忽略权限
     * completeTask.getVariables().put("ignore", true);
     *
     * @param completeTask 参数
     * @return 办理成功返回 {@code true}
     */
    boolean completeTask(CompleteTaskDTO completeTask);

    /**
     * 办理任务
     *
     * @param taskId  任务ID
     * @param message 办理意见
     * @return 办理成功返回 {@code true}
     */
    boolean completeTask(Long taskId, String message);

    /**
     * 启动流程并办理第一个任务
     *
     * @param startProcess 参数
     * @return 首节点办理成功返回 {@code true}
     */
    boolean startCompleteTask(StartProcessDTO startProcess);

    /**
     * 外部消息按流程编码和事件名称启动新实例。
     *
     * @param request 消息启动参数，业务 ID 由调用方提供
     * @return 新建流程实例 ID
     */
    Long startByMessageEvent(StartMessageEventDTO request);

    /**
     * 触发实例中等待的消息捕获或消息边界事件。重复投递时引擎保持实例不变。
     *
     * @param request 消息事件参数
     * @return 流程实例 ID
     */
    Long triggerMessageEvent(TriggerMessageEventDTO request);

    /**
     * 手动发起定时启动事件，不改变等待记录及下一次自动触发时间。
     *
     * @param request 定时启动参数，业务 ID 留空时生成数字雪花 ID
     * @return 新建流程实例 ID
     */
    Long startByTimerEvent(StartTimerEventDTO request);

    /**
     * 从实例级错误捕获节点处理外部错误。
     *
     * @param instanceId   流程实例 ID
     * @param errorCode    错误码，精确匹配优先于通配
     * @param errorMessage 错误描述
     * @return 错误事件已被处理时返回 true
     */
    boolean throwInstanceErrorEvent(Long instanceId, String errorCode, String errorMessage);

    /**
     * 从当前待办的错误边界节点处理外部错误。
     *
     * @param taskId       当前待办任务 ID
     * @param errorCode    错误码，精确匹配优先于通配
     * @param errorMessage 错误描述
     * @return 错误事件已被处理时返回 true
     */
    boolean throwTaskErrorEvent(Long taskId, String errorCode, String errorMessage);
}
