package org.dromara.workflow.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.reflect.ReflectUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.common.enums.BusinessStatusEnum;
import org.dromara.workflow.domain.ActHiProcinst;
import org.dromara.workflow.domain.vo.MultiInstanceVo;
import org.dromara.workflow.domain.vo.ParticipantVo;
import org.dromara.workflow.domain.vo.ProcessInstanceVo;
import org.dromara.workflow.flowable.cmd.UpdateHiTaskInstCmd;
import org.dromara.workflow.service.IActHiProcinstService;
import org.dromara.workflow.service.IWorkflowUserService;
import org.dromara.workflow.service.impl.WorkflowUserServiceImpl;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.engine.impl.bpmn.behavior.SequentialMultiInstanceBehavior;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.rmi.ServerException;
import java.util.*;
import java.util.stream.Collectors;

import static org.dromara.workflow.common.constant.FlowConstant.PROCESS_INSTANCE_VO;

/**
 * 工作流工具
 *
 * @author may
 */
public class WorkflowUtils {

    public WorkflowUtils() {
    }

    private static final ProcessEngine PROCESS_ENGINE = SpringUtils.getBean(ProcessEngine.class);
    private static final IWorkflowUserService I_WORK_FLOW_USER_SERVICE = SpringUtils.getBean(WorkflowUserServiceImpl.class);
    private static final IActHiProcinstService I_ACT_HI_PROCINST_SERVICE = SpringUtils.getBean(IActHiProcinstService.class);

    /**
     * bpmnModel转为xml
     *
     * @param jsonBytes json
     */
    public static byte[] bpmnJsonToXmlBytes(byte[] jsonBytes) throws IOException {
        if (jsonBytes == null) {
            return new byte[0];
        }
        //1. json字节码转成 BpmnModel 对象
        ObjectMapper objectMapper = JsonUtils.getObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonBytes);
        BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(jsonNode);

        if (bpmnModel.getProcesses().isEmpty()) {
            return new byte[0];
        }
        //2.将bpmnModel转为xml
        return new BpmnXMLConverter().convertToXML(bpmnModel);
    }

    /**
     * xml转为bpmnModel
     *
     * @param xmlBytes xml
     */
    public static BpmnModel xmlToBpmnModel(byte[] xmlBytes) throws XMLStreamException {
        ByteArrayInputStream byteArrayInputStream = IoUtil.toStream(xmlBytes);
        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLStreamReader xtr = xif.createXMLStreamReader(byteArrayInputStream);
        return new BpmnXMLConverter().convertToBpmnModel(xtr);
    }

    /**
     * 校验模型
     *
     * @param bpmnModel bpmn模型
     */
    public static void checkBpmnModel(BpmnModel bpmnModel) throws ServerException {
        Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();

        checkBpmnNode(flowElements, false);

        List<SubProcess> subProcessList = flowElements.stream().filter(SubProcess.class::isInstance).map(SubProcess.class::cast).collect(Collectors.toList());
        if (!CollUtil.isEmpty(subProcessList)) {
            for (SubProcess subProcess : subProcessList) {
                Collection<FlowElement> subProcessFlowElements = subProcess.getFlowElements();
                checkBpmnNode(subProcessFlowElements, true);
            }
        }
    }

    /**
     * 校验bpmn节点是否合法
     *
     * @param flowElements 节点集合
     * @param subtask      是否子流程
     */
    private static void checkBpmnNode(Collection<FlowElement> flowElements, boolean subtask) throws ServerException {

        if (CollUtil.isEmpty(flowElements)) {
            throw new ServerException(subtask ? "子流程必须存在节点" : "" + "必须存在节点！");
        }

        List<StartEvent> startEventList = flowElements.stream().filter(StartEvent.class::isInstance).map(StartEvent.class::cast).collect(Collectors.toList());
        if (CollUtil.isEmpty(startEventList)) {
            throw new ServerException(subtask ? "子流程必须存在开始节点" : "" + "必须存在开始节点！");
        }

        if (startEventList.size() > 1) {
            throw new ServerException(subtask ? "子流程只能存在一个开始节点" : "" + "只能存在一个开始节点！");
        }

        StartEvent startEvent = startEventList.get(0);
        List<SequenceFlow> outgoingFlows = startEvent.getOutgoingFlows();
        if (CollUtil.isEmpty(outgoingFlows)) {
            throw new ServerException(subtask ? "子流程流程节点为空，请至少设计一条主线流程！" : "" + "流程节点为空，请至少设计一条主线流程！");
        }

        FlowElement targetFlowElement = outgoingFlows.get(0).getTargetFlowElement();
        if (!(targetFlowElement instanceof UserTask)) {
            throw new ServerException(subtask ? "子流程开始节点后第一个节点必须是用户任务！" : "" + "开始节点后第一个节点必须是用户任务！");
        }

        List<EndEvent> endEventList = flowElements.stream().filter(EndEvent.class::isInstance).map(EndEvent.class::cast).collect(Collectors.toList());
        if (CollUtil.isEmpty(endEventList)) {
            throw new ServerException(subtask ? "子流程必须存在结束节点！" : "" + "必须存在结束节点！");
        }
    }

    /**
     * 创建一个新任务
     *
     * @param currentTask 参数
     */
    public static TaskEntity createNewTask(Task currentTask) {
        TaskEntity task = null;
        if (ObjectUtil.isNotEmpty(currentTask)) {
            task = (TaskEntity) PROCESS_ENGINE.getTaskService().newTask();
            task.setCategory(currentTask.getCategory());
            task.setDescription(currentTask.getDescription());
            task.setTenantId(currentTask.getTenantId());
            task.setAssignee(currentTask.getAssignee());
            task.setName(currentTask.getName());
            task.setProcessDefinitionId(currentTask.getProcessDefinitionId());
            task.setProcessInstanceId(currentTask.getProcessInstanceId());
            task.setTaskDefinitionKey(currentTask.getTaskDefinitionKey());
            task.setPriority(currentTask.getPriority());
            task.setCreateTime(new Date());
            task.setTenantId(TenantHelper.getTenantId());
            PROCESS_ENGINE.getTaskService().saveTask(task);
        }
        if (ObjectUtil.isNotNull(task)) {
            UpdateHiTaskInstCmd updateHiTaskInstCmd = new UpdateHiTaskInstCmd(Collections.singletonList(task.getId()), task.getProcessDefinitionId(), task.getProcessInstanceId());
            PROCESS_ENGINE.getManagementService().executeCommand(updateHiTaskInstCmd);
        }
        return task;
    }

    /**
     * 获取当前任务参与者
     *
     * @param taskId 任务id
     */
    public static ParticipantVo getCurrentTaskParticipant(String taskId) {
        ParticipantVo participantVo = new ParticipantVo();
        List<HistoricIdentityLink> linksForTask = PROCESS_ENGINE.getHistoryService().getHistoricIdentityLinksForTask(taskId);
        Task task = PROCESS_ENGINE.getTaskService().createTaskQuery().taskTenantId(TenantHelper.getTenantId()).taskId(taskId).singleResult();
        if (task != null && CollUtil.isNotEmpty(linksForTask)) {
            List<HistoricIdentityLink> groupList = StreamUtils.filter(linksForTask, e -> StringUtils.isNotBlank(e.getGroupId()));
            if (CollUtil.isNotEmpty(groupList)) {
                List<Long> groupIds = StreamUtils.toList(groupList, e -> Long.valueOf(e.getGroupId()));
                List<SysUserRole> sysUserRoles = I_WORK_FLOW_USER_SERVICE.getUserRoleListByRoleIds(groupIds);
                if (CollUtil.isNotEmpty(sysUserRoles)) {
                    participantVo.setGroupIds(groupIds);
                    List<Long> userIdList = StreamUtils.toList(sysUserRoles, SysUserRole::getUserId);
                    List<SysUserVo> sysUsers = I_WORK_FLOW_USER_SERVICE.getUserListByIds(userIdList);
                    if (CollUtil.isNotEmpty(sysUsers)) {
                        List<Long> userIds = StreamUtils.toList(sysUsers, SysUserVo::getUserId);
                        List<String> nickNames = StreamUtils.toList(sysUsers, SysUserVo::getNickName);
                        participantVo.setCandidate(userIds);
                        participantVo.setCandidateName(nickNames);
                        participantVo.setClaim(!StringUtils.isBlank(task.getAssignee()));
                    }
                }
            } else {
                List<HistoricIdentityLink> candidateList = StreamUtils.filter(linksForTask, e -> FlowConstant.CANDIDATE.equals(e.getType()));
                List<Long> userIdList = new ArrayList<>();
                for (HistoricIdentityLink historicIdentityLink : linksForTask) {
                    try {
                        userIdList.add(Long.valueOf(historicIdentityLink.getUserId()));
                    } catch (NumberFormatException ignored) {

                    }
                }
                List<SysUserVo> sysUsers = I_WORK_FLOW_USER_SERVICE.getUserListByIds(userIdList);
                if (CollUtil.isNotEmpty(sysUsers)) {
                    List<Long> userIds = StreamUtils.toList(sysUsers, SysUserVo::getUserId);
                    List<String> nickNames = StreamUtils.toList(sysUsers, SysUserVo::getNickName);
                    participantVo.setCandidate(userIds);
                    participantVo.setCandidateName(nickNames);
                    if (StringUtils.isBlank(task.getAssignee()) && CollUtil.isNotEmpty(candidateList)) {
                        participantVo.setClaim(false);
                    }
                    if (!StringUtils.isBlank(task.getAssignee()) && CollUtil.isNotEmpty(candidateList)) {
                        participantVo.setClaim(true);
                    }
                }
            }
        }
        return participantVo;
    }

    /**
     * 判断当前节点是否为会签节点
     *
     * @param processDefinitionId 流程定义id
     * @param taskDefinitionKey   流程定义id
     */
    public static MultiInstanceVo isMultiInstance(String processDefinitionId, String taskDefinitionKey) {
        BpmnModel bpmnModel = PROCESS_ENGINE.getRepositoryService().getBpmnModel(processDefinitionId);
        FlowNode flowNode = (FlowNode) bpmnModel.getFlowElement(taskDefinitionKey);
        MultiInstanceVo multiInstanceVo = new MultiInstanceVo();
        //判断是否为并行会签节点
        if (flowNode.getBehavior() instanceof ParallelMultiInstanceBehavior behavior && behavior.getCollectionExpression() != null) {
            Expression collectionExpression = behavior.getCollectionExpression();
            String assigneeList = collectionExpression.getExpressionText();
            String assignee = behavior.getCollectionElementVariable();
            multiInstanceVo.setType(behavior);
            multiInstanceVo.setAssignee(assignee);
            multiInstanceVo.setAssigneeList(assigneeList);
            return multiInstanceVo;
            //判断是否为串行会签节点
        } else if (flowNode.getBehavior() instanceof SequentialMultiInstanceBehavior behavior && behavior.getCollectionExpression() != null) {
            Expression collectionExpression = behavior.getCollectionExpression();
            String assigneeList = collectionExpression.getExpressionText();
            String assignee = behavior.getCollectionElementVariable();
            multiInstanceVo.setType(behavior);
            multiInstanceVo.setAssignee(assignee);
            multiInstanceVo.setAssigneeList(assigneeList);
            return multiInstanceVo;
        }
        return null;
    }

    /**
     * 获取当前流程状态
     *
     * @param taskId 任务id
     */
    public static String getBusinessStatus(String taskId) {
        HistoricTaskInstance historicTaskInstance = PROCESS_ENGINE.getHistoryService().createHistoricTaskInstanceQuery().taskId(taskId).taskTenantId(TenantHelper.getTenantId()).singleResult();
        HistoricProcessInstance historicProcessInstance = PROCESS_ENGINE.getHistoryService().createHistoricProcessInstanceQuery()
            .processInstanceId(historicTaskInstance.getProcessInstanceId()).processInstanceTenantId(TenantHelper.getTenantId()).singleResult();
        return historicProcessInstance.getBusinessStatus();
    }

    /**
     * 设置流程实例对象
     *
     * @param obj         业务对象
     * @param businessKey 业务id
     */
    public static void setProcessInstanceVo(Object obj, String businessKey) {
        ActHiProcinst actHiProcinst = I_ACT_HI_PROCINST_SERVICE.selectByBusinessKey(businessKey);
        if (actHiProcinst == null) {
            ProcessInstanceVo processInstanceVo = new ProcessInstanceVo();
            processInstanceVo.setBusinessStatus(BusinessStatusEnum.DRAFT.getStatus());
            ReflectUtils.invokeSetter(obj, PROCESS_INSTANCE_VO, processInstanceVo);
            return;
        }
        ProcessInstanceVo processInstanceVo = BeanUtil.toBean(actHiProcinst, ProcessInstanceVo.class);
        processInstanceVo.setBusinessStatusName(BusinessStatusEnum.getEumByStatus(processInstanceVo.getBusinessStatus()));
        ReflectUtils.invokeSetter(obj, PROCESS_INSTANCE_VO, processInstanceVo);
    }

    /**
     * 设置流程实例对象
     *
     * @param obj       业务对象
     * @param idList    业务id
     * @param fieldName 主键属性名称
     */
    public static void setProcessInstanceListVo(Object obj, List<String> idList, String fieldName) {
        if (CollUtil.isEmpty(idList)) {
            return;
        }
        List<ActHiProcinst> actHiProcinstList = I_ACT_HI_PROCINST_SERVICE.selectByBusinessKeyIn(idList);
        if (obj instanceof Collection<?> collection) {
            for (Object o : collection) {
                String fieldValue = ReflectUtils.invokeGetter(o, fieldName).toString();
                if (CollUtil.isEmpty(actHiProcinstList)) {
                    ProcessInstanceVo processInstanceVo = new ProcessInstanceVo();
                    processInstanceVo.setBusinessStatus(BusinessStatusEnum.DRAFT.getStatus());
                    processInstanceVo.setBusinessStatusName(BusinessStatusEnum.getEumByStatus(processInstanceVo.getBusinessStatus()));
                    ReflectUtils.invokeSetter(o, PROCESS_INSTANCE_VO, processInstanceVo);
                } else {
                    ActHiProcinst actHiProcinst = actHiProcinstList.stream().filter(e -> e.getBusinessKey().equals(fieldValue)).findFirst().orElse(null);
                    if (ObjectUtil.isNotEmpty(actHiProcinst)) {
                        ProcessInstanceVo processInstanceVo = BeanUtil.toBean(actHiProcinst, ProcessInstanceVo.class);
                        processInstanceVo.setBusinessStatusName(BusinessStatusEnum.getEumByStatus(processInstanceVo.getBusinessStatus()));
                        ReflectUtils.invokeSetter(o, PROCESS_INSTANCE_VO, processInstanceVo);
                    } else {
                        ProcessInstanceVo processInstanceVo = new ProcessInstanceVo();
                        processInstanceVo.setBusinessStatus(BusinessStatusEnum.DRAFT.getStatus());
                        processInstanceVo.setBusinessStatusName(BusinessStatusEnum.getEumByStatus(processInstanceVo.getBusinessStatus()));
                        ReflectUtils.invokeSetter(o, PROCESS_INSTANCE_VO, processInstanceVo);
                    }
                }
            }
        }
    }
}
