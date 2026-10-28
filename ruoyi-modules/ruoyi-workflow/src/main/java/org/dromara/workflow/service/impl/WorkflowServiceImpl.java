package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.workflow.context.WorkflowContext;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.workflow.api.WorkflowService;
import org.dromara.workflow.api.domain.CompleteTaskDTO;
import org.dromara.workflow.api.domain.StartMessageEventDTO;
import org.dromara.workflow.api.domain.StartProcessDTO;
import org.dromara.workflow.api.domain.StartProcessReturnDTO;
import org.dromara.workflow.api.domain.StartTimerEventDTO;
import org.dromara.workflow.api.domain.TriggerMessageEventDTO;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.enums.MessageTypeEnum;
import org.dromara.workflow.domain.FlowInstanceBizExt;
import org.dromara.workflow.domain.bo.CompleteTaskBo;
import org.dromara.workflow.domain.bo.StartProcessBo;
import org.dromara.workflow.service.IFlwInstanceService;
import org.dromara.workflow.service.IFlwTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 通用 工作流服务实现
 *
 * @author may
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final IFlwInstanceService flwInstanceService;
    private final IFlwTaskService flwTaskService;

    /**
     * 删除流程实例
     *
     * @param businessIds 业务id
     * @return 结果
     */
    @Override
    public boolean deleteInstance(List<String> businessIds) {
        return flwInstanceService.deleteByBusinessIds(businessIds);
    }

    /**
     * 获取当前流程状态
     *
     * @param taskId 任务id
     * @return 任务关联流程实例的业务状态，未查询到时返回空字符串
     */
    @Override
    public String getBusinessStatusByTaskId(Long taskId) {
        FlowInstance flowInstance = flwInstanceService.selectByTaskId(taskId);
        return ObjectUtil.isNotNull(flowInstance) ? flowInstance.getFlowStatus() : StringUtils.EMPTY;
    }

    /**
     * 获取当前流程状态
     *
     * @param businessId 业务id
     * @return 业务单据对应的流程状态，未查询到时返回空字符串
     */
    @Override
    public String getBusinessStatus(String businessId) {
        FlowInstance flowInstance = flwInstanceService.selectInstByBusinessId(businessId);
        return ObjectUtil.isNotNull(flowInstance) ? flowInstance.getFlowStatus() : StringUtils.EMPTY;
    }

    /**
     * 设置流程变量
     *
     * @param instanceId 流程实例id
     * @param variables  流程变量
     */
    @Override
    public void setVariable(Long instanceId, Map<String, Object> variables) {
        flwInstanceService.setVariable(instanceId, variables);
    }

    /**
     * 获取流程变量
     *
     * @param instanceId 流程实例id
     * @return 实例变量信息
     */
    @Override
    public Map<String, Object> instanceVariable(Long instanceId) {
        return flwInstanceService.instanceVariable(instanceId);
    }

    /**
     * 按照业务id查询流程实例id
     *
     * @param businessId 业务id
     * @return 结果
     */
    @Override
    public Long getInstanceIdByBusinessId(String businessId) {
        FlowInstance flowInstance = flwInstanceService.selectInstByBusinessId(businessId);
        return ObjectUtil.isNotNull(flowInstance) ? flowInstance.getId() : null;
    }

    /**
     * 启动流程
     *
     * @param startProcess 参数
     * @return 启动后的流程实例和首个任务信息
     */
    @Override
    public StartProcessReturnDTO startWorkFlow(StartProcessDTO startProcess) {
        return flwTaskService.startWorkFlow(BeanUtil.toBean(startProcess, StartProcessBo.class));
    }

    /**
     * 办理任务
     * 系统后台发起审批 无用户信息 需要忽略权限
     * completeTask.getVariables().put("ignore", true);
     *
     * @param completeTask 参数
     * @return 办理成功返回 {@code true}
     */
    @Override
    public boolean completeTask(CompleteTaskDTO completeTask) {
        return flwTaskService.completeTask(BeanUtil.toBean(completeTask, CompleteTaskBo.class));
    }

    /**
     * 办理任务
     *
     * @param taskId  任务ID
     * @param message 办理意见
     * @return 办理成功返回 {@code true}
     */
    @Override
    public boolean completeTask(Long taskId, String message) {
        CompleteTaskBo completeTask = new CompleteTaskBo();
        completeTask.setTaskId(taskId);
        completeTask.setMessage(message);
        // 忽略权限(系统后台发起审批 无用户信息 需要忽略权限)
        completeTask.getVariables().put("ignore", true);
        return flwTaskService.completeTask(completeTask);
    }

    /**
     * 启动流程并办理第一个任务
     *
     * @param startProcess 参数
     * @return 首节点办理成功返回 {@code true}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startCompleteTask(StartProcessDTO startProcess) {
        StartProcessBo processBo = new StartProcessBo();
        processBo.setBusinessId(startProcess.getBusinessId());
        processBo.setFlowCode(startProcess.getFlowCode());
        processBo.setVariables(startProcess.getVariables());
        processBo.setHandler(startProcess.getHandler());
        processBo.setBizExt(BeanUtil.toBean(startProcess.getBizExt(), FlowInstanceBizExt.class));

        StartProcessReturnDTO result = flwTaskService.startWorkFlow(processBo);
        CompleteTaskBo taskBo = new CompleteTaskBo();
        taskBo.setTaskId(result.taskId());
        taskBo.setMessageType(Collections.singletonList(MessageTypeEnum.SYSTEM_MESSAGE.getCode()));
        taskBo.setVariables(startProcess.getVariables());
        taskBo.setHandler(startProcess.getHandler());
        return flwTaskService.completeTask(taskBo);
    }

    /**
     * 外部消息按流程编码和事件名称启动新实例。
     *
     * @param request 消息启动参数，业务 ID 由调用方提供
     * @return 新建流程实例 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long startByMessageEvent(StartMessageEventDTO request) {
        if (request == null) {
            throw new ServiceException("消息启动参数不能为空");
        }
        return FlowEngine.insService().startByMessage(request.getBusinessId(), request.getFlowCode(),
            request.getEventName(), request.getPayload(), eventContext(request.getHandler())).getId();
    }

    /**
     * 触发实例中等待的消息捕获或消息边界事件。重复投递时引擎保持实例不变。
     *
     * @param request 消息事件参数
     * @return 流程实例 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long triggerMessageEvent(TriggerMessageEventDTO request) {
        if (request == null) {
            throw new ServiceException("消息事件参数不能为空");
        }
        return FlowEngine.insService().triggerMessage(request.getInstanceId(), request.getEventName(),
            request.getPayload(), eventContext(request.getHandler())).getId();
    }

    /**
     * 手动发起定时启动事件，不改变等待记录及下一次自动触发时间。
     *
     * @param request 定时启动参数，业务 ID 留空时生成数字雪花 ID
     * @return 新建流程实例 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long startByTimerEvent(StartTimerEventDTO request) {
        if (request == null) {
            throw new ServiceException("定时启动参数不能为空");
        }
        String businessId = StringUtils.isNotBlank(request.getBusinessId())
            ? request.getBusinessId().trim() : String.valueOf(IdWorker.getId());
        return FlowEngine.insService().startByTimer(businessId, request.getDefinitionId(),
            request.getNodeCode(), request.getPayload(), eventContext(request.getHandler())).getId();
    }

    /**
     * 从实例级错误捕获节点处理外部错误。
     *
     * @param instanceId   流程实例 ID
     * @param errorCode    错误码，精确匹配优先于通配
     * @param errorMessage 错误描述
     * @return 错误事件已被处理时返回 true
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean throwInstanceErrorEvent(Long instanceId, String errorCode, String errorMessage) {
        return FlowEngine.insService().throwError(instanceId, errorCode, errorMessage);
    }

    /**
     * 从当前待办的错误边界节点处理外部错误。
     *
     * @param taskId       当前待办任务 ID
     * @param errorCode    错误码，精确匹配优先于通配
     * @param errorMessage 错误描述
     * @return 错误事件已被处理时返回 true
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean throwTaskErrorEvent(Long taskId, String errorCode, String errorMessage) {
        return FlowEngine.taskService().throwError(taskId, errorCode, errorMessage);
    }

    /**
     * 构建事件上下文
     *
     * @param handler 操作人
     * @return 事件上下文
     */
    private WorkflowContext eventContext(String handler) {
        WorkflowContext context = new WorkflowContext();
        context.setHandler(StringUtils.isNotBlank(handler) ? handler : FlowEngine.getFlowConfig().getSystemHandler());
        return context;
    }
}
