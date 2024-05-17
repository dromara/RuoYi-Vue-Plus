package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.common.enums.BusinessStatusEnum;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.domain.ActHiProcinst;
import org.dromara.workflow.domain.bo.ProcessInstanceBo;
import org.dromara.workflow.domain.bo.ProcessInvalidBo;
import org.dromara.workflow.domain.bo.TaskUrgingBo;
import org.dromara.workflow.domain.vo.*;
import org.dromara.workflow.flowable.CustomDefaultProcessDiagramGenerator;
import org.dromara.workflow.flowable.cmd.DeleteExecutionCmd;
import org.dromara.workflow.flowable.cmd.ExecutionChildByExecutionIdCmd;
import org.dromara.workflow.flowable.strategy.FlowEventStrategy;
import org.dromara.workflow.flowable.strategy.FlowProcessEventHandler;
import org.dromara.workflow.service.IActHiProcinstService;
import org.dromara.workflow.service.IActProcessInstanceService;
import org.dromara.workflow.service.IWfNodeConfigService;
import org.dromara.workflow.service.IWfTaskBackNodeService;
import org.dromara.workflow.utils.QueryUtils;
import org.dromara.workflow.utils.WorkflowUtils;
import org.flowable.bpmn.model.*;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.engine.task.Attachment;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程实例 服务层实现
 *
 * @author may
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ActProcessInstanceServiceImpl implements IActProcessInstanceService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final TaskService taskService;
    private final IActHiProcinstService actHiProcinstService;
    private final ManagementService managementService;
    private final FlowEventStrategy flowEventStrategy;
    private final IWfTaskBackNodeService wfTaskBackNodeService;
    private final IWfNodeConfigService wfNodeConfigService;

    @Value("${flowable.activity-font-name}")
    private String activityFontName;

    @Value("${flowable.label-font-name}")
    private String labelFontName;

    @Value("${flowable.annotation-font-name}")
    private String annotationFontName;

    /**
     * 分页查询正在运行的流程实例
     *
     * @param bo 参数
     */
    @Override
    public TableDataInfo<ProcessInstanceVo> getPageByRunning(ProcessInstanceBo bo, PageQuery pageQuery) {
        List<ProcessInstanceVo> list = new ArrayList<>();
        ProcessInstanceQuery query = QueryUtils.instanceQuery();
        if (StringUtils.isNotBlank(bo.getName())) {
            query.processInstanceNameLikeIgnoreCase("%" + bo.getName() + "%");
        }
        if (StringUtils.isNotBlank(bo.getKey())) {
            query.processDefinitionKey(bo.getKey());
        }
        if (StringUtils.isNotBlank(bo.getStartUserId())) {
            query.startedBy(bo.getStartUserId());
        }
        if (StringUtils.isNotBlank(bo.getBusinessKey())) {
            query.processInstanceBusinessKey(bo.getBusinessKey());
        }
        if (StringUtils.isNotBlank(bo.getCategoryCode())) {
            query.processDefinitionCategory(bo.getCategoryCode());
        }
        query.orderByStartTime().desc();
        List<ProcessInstance> processInstances = query.listPage(pageQuery.getFirstNum(), pageQuery.getPageSize());
        for (ProcessInstance processInstance : processInstances) {
            ProcessInstanceVo processInstanceVo = BeanUtil.toBean(processInstance, ProcessInstanceVo.class);
            processInstanceVo.setIsSuspended(processInstance.isSuspended());
            processInstanceVo.setBusinessStatusName(BusinessStatusEnum.findByStatus(processInstance.getBusinessStatus()));
            list.add(processInstanceVo);
        }
        if (CollUtil.isNotEmpty(list)) {
            List<String> processDefinitionIds = StreamUtils.toList(list, ProcessInstanceVo::getProcessDefinitionId);
            List<WfNodeConfigVo> wfNodeConfigVoList = wfNodeConfigService.selectByDefIds(processDefinitionIds);
            for (ProcessInstanceVo processInstanceVo : list) {
                if (CollUtil.isNotEmpty(wfNodeConfigVoList)) {
                    wfNodeConfigVoList.stream().filter(e -> e.getDefinitionId().equals(processInstanceVo.getProcessDefinitionId()) && FlowConstant.TRUE.equals(e.getApplyUserTask())).findFirst().ifPresent(processInstanceVo::setWfNodeConfigVo);
                }
            }
        }
        long count = query.count();
        TableDataInfo<ProcessInstanceVo> build = TableDataInfo.build();
        build.setRows(list);
        build.setTotal(count);
        return build;
    }

    /**
     * 分页查询已结束的流程实例
     *
     * @param bo 参数
     */
    @Override
    public TableDataInfo<ProcessInstanceVo> getPageByFinish(ProcessInstanceBo bo, PageQuery pageQuery) {
        List<ProcessInstanceVo> list = new ArrayList<>();
        HistoricProcessInstanceQuery query = QueryUtils.hisInstanceQuery()
            .finished().orderByProcessInstanceEndTime().desc();
        if (StringUtils.isNotEmpty(bo.getName())) {
            query.processInstanceNameLikeIgnoreCase("%" + bo.getName() + "%");
        }
        if (StringUtils.isNotBlank(bo.getKey())) {
            query.processDefinitionKey(bo.getKey());
        }
        if (StringUtils.isNotEmpty(bo.getStartUserId())) {
            query.startedBy(bo.getStartUserId());
        }
        if (StringUtils.isNotBlank(bo.getBusinessKey())) {
            query.processInstanceBusinessKey(bo.getBusinessKey());
        }
        if (StringUtils.isNotBlank(bo.getCategoryCode())) {
            query.processDefinitionCategory(bo.getCategoryCode());
        }
        List<HistoricProcessInstance> historicProcessInstances = query.listPage(pageQuery.getFirstNum(), pageQuery.getPageSize());
        for (HistoricProcessInstance historicProcessInstance : historicProcessInstances) {
            ProcessInstanceVo processInstanceVo = BeanUtil.toBean(historicProcessInstance, ProcessInstanceVo.class);
            processInstanceVo.setBusinessStatusName(BusinessStatusEnum.findByStatus(historicProcessInstance.getBusinessStatus()));
            list.add(processInstanceVo);
        }
        if (CollUtil.isNotEmpty(list)) {
            List<String> processDefinitionIds = StreamUtils.toList(list, ProcessInstanceVo::getProcessDefinitionId);
            List<WfNodeConfigVo> wfNodeConfigVoList = wfNodeConfigService.selectByDefIds(processDefinitionIds);
            for (ProcessInstanceVo processInstanceVo : list) {
                if (CollUtil.isNotEmpty(wfNodeConfigVoList)) {
                    wfNodeConfigVoList.stream().filter(e -> e.getDefinitionId().equals(processInstanceVo.getProcessDefinitionId()) && FlowConstant.TRUE.equals(e.getApplyUserTask())).findFirst().ifPresent(processInstanceVo::setWfNodeConfigVo);
                }
            }
        }
        long count = query.count();
        TableDataInfo<ProcessInstanceVo> build = TableDataInfo.build();
        build.setRows(list);
        build.setTotal(count);
        return build;
    }

    /**
     * 通过流程实例id获取历史流程图
     *
     * @param processInstanceId 流程实例id
     */
    @SneakyThrows
    @Override
    public String getHistoryImage(String processInstanceId) {
        String processDefinitionId;
        // 获取当前的流程实例
        ProcessInstance processInstance = QueryUtils.instanceQuery(processInstanceId).singleResult();
        // 如果流程已经结束，则得到结束节点
        if (Objects.isNull(processInstance)) {
            HistoricProcessInstance pi = QueryUtils.hisInstanceQuery(processInstanceId).singleResult();
            processDefinitionId = pi.getProcessDefinitionId();
        } else {
            // 根据流程实例ID获得当前处于活动状态的ActivityId合集
            ProcessInstance pi = QueryUtils.instanceQuery(processInstanceId).singleResult();
            processDefinitionId = pi.getProcessDefinitionId();
        }

        // 获得活动的节点
        List<HistoricActivityInstance> highLightedFlowList = QueryUtils.hisActivityInstanceQuery(processInstanceId).orderByHistoricActivityInstanceStartTime().asc().list();

        List<String> highLightedFlows = new ArrayList<>();
        List<String> highLightedNodes = new ArrayList<>();
        //高亮
        for (HistoricActivityInstance tempActivity : highLightedFlowList) {
            if (FlowConstant.SEQUENCE_FLOW.equals(tempActivity.getActivityType())) {
                //高亮线
                highLightedFlows.add(tempActivity.getActivityId());
            } else {
                //高亮节点
                if (tempActivity.getEndTime() == null) {
                    highLightedNodes.add(Color.RED.toString() + tempActivity.getActivityId());
                } else {
                    highLightedNodes.add(tempActivity.getActivityId());
                }
            }
        }
        List<String> highLightedNodeList = new ArrayList<>();
        //运行中的节点
        List<String> redNodeCollect = StreamUtils.filter(highLightedNodes, e -> e.contains(Color.RED.toString()));
        //排除与运行中相同的节点
        for (String nodeId : highLightedNodes) {
            if (!nodeId.contains(Color.RED.toString()) && !redNodeCollect.contains(Color.RED + nodeId)) {
                highLightedNodeList.add(nodeId);
            }
        }
        highLightedNodeList.addAll(redNodeCollect);
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
        CustomDefaultProcessDiagramGenerator diagramGenerator = new CustomDefaultProcessDiagramGenerator();
        InputStream inputStream = diagramGenerator.generateDiagram(bpmnModel, "png", highLightedNodeList, highLightedFlows, activityFontName, labelFontName, annotationFontName, null, 1.0, true);
        return Base64.encode(IoUtil.readBytes(inputStream));
    }

    /**
     * 通过流程实例id获取历史流程图运行中，历史等节点
     *
     * @param processInstanceId 流程实例id
     */
    @Override
    public Map<String, Object> getHistoryList(String processInstanceId) {
        Map<String, Object> map = new HashMap<>();
        List<Map<String, Object>> taskList = new ArrayList<>();
        HistoricProcessInstance historicProcessInstance = QueryUtils.hisInstanceQuery(processInstanceId).singleResult();
        StringBuilder xml = new StringBuilder();
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(historicProcessInstance.getProcessDefinitionId());
        // 获取节点
        List<HistoricActivityInstance> highLightedFlowList = QueryUtils.hisActivityInstanceQuery(processInstanceId).orderByHistoricActivityInstanceStartTime().asc().list();
        for (HistoricActivityInstance tempActivity : highLightedFlowList) {
            Map<String, Object> task = new HashMap<>();
            if (!FlowConstant.SEQUENCE_FLOW.equals(tempActivity.getActivityType()) &&
                !FlowConstant.PARALLEL_GATEWAY.equals(tempActivity.getActivityType()) &&
                !FlowConstant.EXCLUSIVE_GATEWAY.equals(tempActivity.getActivityType()) &&
                !FlowConstant.INCLUSIVE_GATEWAY.equals(tempActivity.getActivityType())
            ) {
                task.put("key", tempActivity.getActivityId());
                task.put("completed", tempActivity.getEndTime() != null);
                task.put("activityType", tempActivity.getActivityType());
                taskList.add(task);
            }
        }
        ProcessInstance processInstance = QueryUtils.instanceQuery(processInstanceId).singleResult();
        if (processInstance != null) {
            taskList = taskList.stream().filter(e -> !e.get("activityType").equals(FlowConstant.END_EVENT)).collect(Collectors.toList());
        }
        //查询出运行中节点
        List<Map<String, Object>> runtimeNodeList = taskList.stream().filter(e -> !(Boolean) e.get("completed")).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(runtimeNodeList)) {
            Iterator<Map<String, Object>> iterator = taskList.iterator();
            while (iterator.hasNext()) {
                Map<String, Object> next = iterator.next();
                runtimeNodeList.stream().filter(t -> t.get("key").equals(next.get("key")) && (Boolean) next.get("completed")).findFirst().ifPresent(t -> iterator.remove());
            }
        }
        map.put("taskList", taskList);
        List<ActHistoryInfoVo> historyTaskList = getHistoryTaskList(processInstanceId);
        map.put("historyList", historyTaskList);
        InputStream inputStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), processDefinition.getResourceName());
        xml.append(IoUtil.read(inputStream, StandardCharsets.UTF_8));
        map.put("xml", xml.toString());
        return map;
    }

    /**
     * 获取历史任务节点信息
     *
     * @param processInstanceId 流程实例id
     */
    private List<ActHistoryInfoVo> getHistoryTaskList(String processInstanceId) {
        //查询任务办理记录
        List<HistoricTaskInstance> list = QueryUtils.hisTaskInstanceQuery(processInstanceId).orderByHistoricTaskInstanceEndTime().desc().list();
        list = StreamUtils.sorted(list, Comparator.comparing(HistoricTaskInstance::getEndTime, Comparator.nullsFirst(Date::compareTo)).reversed());
        List<ActHistoryInfoVo> actHistoryInfoVoList = new ArrayList<>();
        for (HistoricTaskInstance historicTaskInstance : list) {
            ActHistoryInfoVo actHistoryInfoVo = new ActHistoryInfoVo();
            BeanUtils.copyProperties(historicTaskInstance, actHistoryInfoVo);
            actHistoryInfoVo.setStatus(actHistoryInfoVo.getEndTime() == null ? "待处理" : "已处理");
            if (ObjectUtil.isNotEmpty(historicTaskInstance.getDurationInMillis())) {
                actHistoryInfoVo.setRunDuration(getDuration(historicTaskInstance.getDurationInMillis()));
            }
            actHistoryInfoVoList.add(actHistoryInfoVo);
        }
        List<ActHistoryInfoVo> historyInfoVoList = new ArrayList<>();
        Map<String, List<ActHistoryInfoVo>> groupByKey = StreamUtils.groupByKey(actHistoryInfoVoList, ActHistoryInfoVo::getTaskDefinitionKey);
        for (Map.Entry<String, List<ActHistoryInfoVo>> entry : groupByKey.entrySet()) {
            ActHistoryInfoVo historyInfoVo = new ActHistoryInfoVo();
            BeanUtils.copyProperties(entry.getValue().get(0), historyInfoVo);
            actHistoryInfoVoList.stream().filter(e -> e.getTaskDefinitionKey().equals(entry.getKey()) && e.getEndTime() == null).findFirst()
                .ifPresent(e -> {
                    historyInfoVo.setStatus("待处理");
                    historyInfoVo.setStartTime(e.getStartTime());
                    historyInfoVo.setEndTime(null);
                    historyInfoVo.setRunDuration(null);
                    if (ObjectUtil.isEmpty(e.getAssignee())) {
                        ParticipantVo participantVo = WorkflowUtils.getCurrentTaskParticipant(e.getId());
                        if (ObjectUtil.isNotEmpty(participantVo) && CollUtil.isNotEmpty(participantVo.getCandidate())) {
                            historyInfoVo.setAssignee(StreamUtils.join(participantVo.getCandidate(), Convert::toStr));
                        }
                    }
                });
            historyInfoVoList.add(historyInfoVo);
        }
        return historyInfoVoList;
    }

    /**
     * 获取审批记录
     *
     * @param processInstanceId 流程实例id
     */
    @Override
    public List<ActHistoryInfoVo> getHistoryRecord(String processInstanceId) {
        // 查询任务办理记录
        List<HistoricTaskInstance> list = QueryUtils.hisTaskInstanceQuery(processInstanceId).orderByHistoricTaskInstanceEndTime().desc().list();
        list = StreamUtils.sorted(list, Comparator.comparing(HistoricTaskInstance::getEndTime, Comparator.nullsFirst(Date::compareTo)).reversed());
        List<ActHistoryInfoVo> actHistoryInfoVoList = new ArrayList<>();
        List<Comment> processInstanceComments = taskService.getProcessInstanceComments(processInstanceId);
        //附件
        List<Attachment> attachmentList = taskService.getProcessInstanceAttachments(processInstanceId);
        for (HistoricTaskInstance historicTaskInstance : list) {
            ActHistoryInfoVo actHistoryInfoVo = new ActHistoryInfoVo();
            BeanUtils.copyProperties(historicTaskInstance, actHistoryInfoVo);
            if (actHistoryInfoVo.getEndTime() == null) {
                actHistoryInfoVo.setStatus(TaskStatusEnum.WAITING.getStatus());
                actHistoryInfoVo.setStatusName(TaskStatusEnum.WAITING.getDesc());
            }
            if (CollUtil.isNotEmpty(processInstanceComments)) {
                processInstanceComments.stream().filter(e -> e.getTaskId().equals(historicTaskInstance.getId())).findFirst().ifPresent(e -> {
                    actHistoryInfoVo.setComment(e.getFullMessage());
                    actHistoryInfoVo.setStatus(e.getType());
                    actHistoryInfoVo.setStatusName(TaskStatusEnum.findByStatus(e.getType()));
                });
            }
            if (ObjectUtil.isNotEmpty(historicTaskInstance.getDurationInMillis())) {
                actHistoryInfoVo.setRunDuration(getDuration(historicTaskInstance.getDurationInMillis()));
            }
            //附件
            if (CollUtil.isNotEmpty(attachmentList)) {
                List<Attachment> attachments = attachmentList.stream().filter(e -> e.getTaskId().equals(historicTaskInstance.getId())).collect(Collectors.toList());
                if (CollUtil.isNotEmpty(attachments)) {
                    actHistoryInfoVo.setAttachmentList(attachments);
                }
            }
            //设置人员id
            if (ObjectUtil.isEmpty(historicTaskInstance.getAssignee())) {
                ParticipantVo participantVo = WorkflowUtils.getCurrentTaskParticipant(historicTaskInstance.getId());
                if (ObjectUtil.isNotEmpty(participantVo) && CollUtil.isNotEmpty(participantVo.getCandidate())) {
                    actHistoryInfoVo.setAssignee(StreamUtils.join(participantVo.getCandidate(), Convert::toStr));
                }
            }
            actHistoryInfoVoList.add(actHistoryInfoVo);
        }
        // 审批记录
        Map<String, List<ActHistoryInfoVo>> groupByKey = StreamUtils.groupByKey(actHistoryInfoVoList, ActHistoryInfoVo::getTaskDefinitionKey);
        for (Map.Entry<String, List<ActHistoryInfoVo>> entry : groupByKey.entrySet()) {
            ActHistoryInfoVo actHistoryInfoVo = BeanUtil.toBean(entry.getValue().get(0), ActHistoryInfoVo.class);
            actHistoryInfoVoList.stream().filter(e -> e.getTaskDefinitionKey().equals(entry.getKey()) && e.getEndTime() != null).findFirst()
                .ifPresent(e -> {
                    actHistoryInfoVo.setStatus("已处理");
                    actHistoryInfoVo.setStartTime(e.getStartTime());
                });
            actHistoryInfoVoList.stream().filter(e -> e.getTaskDefinitionKey().equals(entry.getKey()) && e.getEndTime() == null).findFirst()
                .ifPresent(e -> {
                    actHistoryInfoVo.setStatus("待处理");
                    actHistoryInfoVo.setStartTime(e.getStartTime());
                    actHistoryInfoVo.setEndTime(null);
                    actHistoryInfoVo.setRunDuration(null);
                });
        }
        List<ActHistoryInfoVo> recordList = new ArrayList<>();
        // 待办理
        recordList.addAll(StreamUtils.filter(actHistoryInfoVoList, e -> e.getEndTime() == null));
        // 已办理
        recordList.addAll(StreamUtils.filter(actHistoryInfoVoList, e -> e.getEndTime() != null));

        return recordList;
    }

    /**
     * 任务完成时间处理
     *
     * @param time 时间
     */
    private String getDuration(long time) {

        long day = time / (24 * 60 * 60 * 1000);
        long hour = (time / (60 * 60 * 1000) - day * 24);
        long minute = ((time / (60 * 1000)) - day * 24 * 60 - hour * 60);
        long second = (time / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - minute * 60);

        if (day > 0) {
            return day + "天" + hour + "小时" + minute + "分钟";
        }
        if (hour > 0) {
            return hour + "小时" + minute + "分钟";
        }
        if (minute > 0) {
            return minute + "分钟";
        }
        if (second > 0) {
            return second + "秒";
        } else {
            return 0 + "秒";
        }
    }

    /**
     * 作废流程实例，不会删除历史记录(删除运行中的实例)
     *
     * @param processInvalidBo 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRunInstance(ProcessInvalidBo processInvalidBo) {
        try {
            List<Task> list = QueryUtils.taskQuery(processInvalidBo.getProcessInstanceId()).list();
            List<Task> subTasks = StreamUtils.filter(list, e -> StringUtils.isNotBlank(e.getParentTaskId()));
            if (CollUtil.isNotEmpty(subTasks)) {
                subTasks.forEach(e -> taskService.deleteTask(e.getId()));
            }
            String deleteReason = LoginHelper.getLoginUser().getNickname() + "作废了当前申请！";
            if (StringUtils.isNotBlank(processInvalidBo.getDeleteReason())) {
                deleteReason = LoginHelper.getLoginUser().getNickname() + "作废理由:" + processInvalidBo.getDeleteReason();
            }
            for (Task task : StreamUtils.filter(list, e -> StringUtils.isBlank(e.getParentTaskId()))) {
                taskService.addComment(task.getId(), task.getProcessInstanceId(), TaskStatusEnum.INVALID.getStatus(), deleteReason);
            }
            HistoricProcessInstance historicProcessInstance = QueryUtils.hisInstanceQuery(processInvalidBo.getProcessInstanceId()).singleResult();
            BusinessStatusEnum.checkInvalidStatus(historicProcessInstance.getBusinessStatus());
            runtimeService.updateBusinessStatus(processInvalidBo.getProcessInstanceId(), BusinessStatusEnum.INVALID.getStatus());
            runtimeService.deleteProcessInstance(processInvalidBo.getProcessInstanceId(), deleteReason);
            FlowProcessEventHandler processHandler = flowEventStrategy.getProcessHandler(historicProcessInstance.getProcessDefinitionKey());
            if (processHandler != null) {
                processHandler.handleProcess(historicProcessInstance.getBusinessKey(), BusinessStatusEnum.INVALID.getStatus(), false);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 运行中的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param processInstanceIds 流程实例id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRunAndHisInstance(List<String> processInstanceIds) {
        try {
            // 1.删除运行中流程实例
            List<Task> list = QueryUtils.taskQuery(processInstanceIds).list();
            List<Task> subTasks = StreamUtils.filter(list, e -> StringUtils.isNotBlank(e.getParentTaskId()));
            if (CollUtil.isNotEmpty(subTasks)) {
                subTasks.forEach(e -> taskService.deleteTask(e.getId()));
            }
            runtimeService.bulkDeleteProcessInstances(processInstanceIds, LoginHelper.getUserId() + "删除了当前流程申请");
            // 2.删除历史记录
            List<HistoricProcessInstance> historicProcessInstanceList = QueryUtils.hisInstanceQuery(new HashSet<>(processInstanceIds)).list();
            if (ObjectUtil.isNotEmpty(historicProcessInstanceList)) {
                historyService.bulkDeleteHistoricProcessInstances(processInstanceIds);
            }
            wfTaskBackNodeService.deleteByInstanceIds(processInstanceIds);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 按照业务id删除 运行中的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param businessKeys 业务id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRunAndHisInstanceByBusinessKeys(List<String> businessKeys) {
        try {
            // 1.删除运行中流程实例
            List<ActHiProcinst> actHiProcinsts = actHiProcinstService.selectByBusinessKeyIn(businessKeys);
            if (CollUtil.isEmpty(actHiProcinsts)) {
                log.warn("当前业务ID:{}查询到流程实例为空！", businessKeys);
                return false;
            }
            List<String> processInstanceIds = StreamUtils.toList(actHiProcinsts, ActHiProcinst::getId);
            List<Task> list = QueryUtils.taskQuery(processInstanceIds).list();
            List<Task> subTasks = StreamUtils.filter(list, e -> StringUtils.isNotBlank(e.getParentTaskId()));
            if (CollUtil.isNotEmpty(subTasks)) {
                subTasks.forEach(e -> taskService.deleteTask(e.getId()));
            }
            runtimeService.bulkDeleteProcessInstances(processInstanceIds, LoginHelper.getUserId() + "删除了当前流程申请");
            // 2.删除历史记录
            List<HistoricProcessInstance> historicProcessInstanceList = QueryUtils.hisInstanceQuery(new HashSet<>(processInstanceIds)).list();
            if (ObjectUtil.isNotEmpty(historicProcessInstanceList)) {
                historyService.bulkDeleteHistoricProcessInstances(processInstanceIds);
            }
            wfTaskBackNodeService.deleteByInstanceIds(processInstanceIds);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 已完成的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param processInstanceIds 流程实例id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFinishAndHisInstance(List<String> processInstanceIds) {
        try {
            historyService.bulkDeleteHistoricProcessInstances(processInstanceIds);
            wfTaskBackNodeService.deleteByInstanceIds(processInstanceIds);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 撤销流程申请
     *
     * @param processInstanceId 流程实例id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelProcessApply(String processInstanceId) {
        try {
            ProcessInstance processInstance = QueryUtils.instanceQuery(processInstanceId)
                .startedBy(String.valueOf(LoginHelper.getUserId())).singleResult();
            if (ObjectUtil.isNull(processInstance)) {
                throw new ServiceException("您不是流程发起人,撤销失败!");
            }
            if (processInstance.isSuspended()) {
                throw new ServiceException(FlowConstant.MESSAGE_SUSPENDED);
            }
            BusinessStatusEnum.checkCancelStatus(processInstance.getBusinessStatus());
            List<Task> taskList = QueryUtils.taskQuery(processInstanceId).list();
            for (Task task : taskList) {
                taskService.setAssignee(task.getId(), null);
                taskService.addComment(task.getId(), processInstanceId, TaskStatusEnum.CANCEL.getStatus(), LoginHelper.getLoginUser().getNickname() + "：撤销申请");
            }
            HistoricTaskInstance historicTaskInstance = QueryUtils.hisTaskInstanceQuery().finished().orderByHistoricTaskInstanceEndTime().asc().list().get(0);
            List<String> nodeIds = StreamUtils.toList(taskList, Task::getTaskDefinitionKey);
            runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdsToSingleActivityId(nodeIds, historicTaskInstance.getTaskDefinitionKey()).changeState();
            Task task = QueryUtils.taskQuery(processInstanceId).list().get(0);
            taskService.setAssignee(task.getId(), historicTaskInstance.getAssignee());
            //获取并行网关执行后保留的执行实例数据
            ExecutionChildByExecutionIdCmd childByExecutionIdCmd = new ExecutionChildByExecutionIdCmd(task.getExecutionId());
            List<ExecutionEntity> executionEntities = managementService.executeCommand(childByExecutionIdCmd);
            //删除流程实例垃圾数据
            for (ExecutionEntity executionEntity : executionEntities) {
                DeleteExecutionCmd deleteExecutionCmd = new DeleteExecutionCmd(executionEntity.getId());
                managementService.executeCommand(deleteExecutionCmd);
            }
            runtimeService.updateBusinessStatus(processInstanceId, BusinessStatusEnum.CANCEL.getStatus());
            FlowProcessEventHandler processHandler = flowEventStrategy.getProcessHandler(processInstance.getProcessDefinitionKey());
            if (processHandler != null) {
                processHandler.handleProcess(processInstance.getBusinessKey(), BusinessStatusEnum.CANCEL.getStatus(), false);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException("撤销失败:" + e.getMessage());
        }
    }

    /**
     * 分页查询当前登录人单据
     *
     * @param bo 参数
     */
    @Override
    public TableDataInfo<ProcessInstanceVo> getPageByCurrent(ProcessInstanceBo bo, PageQuery pageQuery) {
        List<ProcessInstanceVo> list = new ArrayList<>();
        HistoricProcessInstanceQuery query = QueryUtils.hisInstanceQuery();
        query.startedBy(String.valueOf(LoginHelper.getUserId()));
        if (StringUtils.isNotBlank(bo.getName())) {
            query.processInstanceNameLikeIgnoreCase("%" + bo.getName() + "%");
        }
        if (StringUtils.isNotBlank(bo.getKey())) {
            query.processDefinitionKey(bo.getKey());
        }
        if (StringUtils.isNotBlank(bo.getBusinessKey())) {
            query.processInstanceBusinessKey(bo.getBusinessKey());
        }
        if (StringUtils.isNotBlank(bo.getCategoryCode())) {
            query.processDefinitionCategory(bo.getCategoryCode());
        }
        query.orderByProcessInstanceStartTime().desc();
        List<HistoricProcessInstance> historicProcessInstanceList = query.listPage(pageQuery.getFirstNum(), pageQuery.getPageSize());
        List<TaskVo> taskVoList = new ArrayList<>();
        if (CollUtil.isNotEmpty(historicProcessInstanceList)) {
            List<String> processInstanceIds = StreamUtils.toList(historicProcessInstanceList, HistoricProcessInstance::getId);
            List<Task> taskList = QueryUtils.taskQuery(processInstanceIds).list();
            for (Task task : taskList) {
                taskVoList.add(BeanUtil.toBean(task, TaskVo.class));
            }
        }
        for (HistoricProcessInstance processInstance : historicProcessInstanceList) {
            ProcessInstanceVo processInstanceVo = BeanUtil.toBean(processInstance, ProcessInstanceVo.class);
            processInstanceVo.setBusinessStatusName(BusinessStatusEnum.findByStatus(processInstance.getBusinessStatus()));
            if (CollUtil.isNotEmpty(taskVoList)) {
                List<TaskVo> collect = StreamUtils.filter(taskVoList, e -> e.getProcessInstanceId().equals(processInstance.getId()));
                processInstanceVo.setTaskVoList(CollUtil.isNotEmpty(collect) ? collect : Collections.emptyList());
            }
            list.add(processInstanceVo);
        }
        if (CollUtil.isNotEmpty(list)) {
            List<String> processDefinitionIds = StreamUtils.toList(list, ProcessInstanceVo::getProcessDefinitionId);
            List<WfNodeConfigVo> wfNodeConfigVoList = wfNodeConfigService.selectByDefIds(processDefinitionIds);
            for (ProcessInstanceVo processInstanceVo : list) {
                if (CollUtil.isNotEmpty(wfNodeConfigVoList)) {
                    wfNodeConfigVoList.stream().filter(e -> e.getDefinitionId().equals(processInstanceVo.getProcessDefinitionId()) && FlowConstant.TRUE.equals(e.getApplyUserTask())).findFirst().ifPresent(processInstanceVo::setWfNodeConfigVo);
                }
            }
        }
        long count = query.count();
        TableDataInfo<ProcessInstanceVo> build = TableDataInfo.build();
        build.setRows(list);
        build.setTotal(count);
        return build;
    }

    /**
     * 任务催办(给当前任务办理人发送站内信，邮件，短信等)
     *
     * @param taskUrgingBo 任务催办
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean taskUrging(TaskUrgingBo taskUrgingBo) {
        try {
            ProcessInstance processInstance = QueryUtils.instanceQuery(taskUrgingBo.getProcessInstanceId()).singleResult();
            if (processInstance == null) {
                throw new ServiceException("任务已结束！");
            }
            String message = taskUrgingBo.getMessage();
            if (StringUtils.isBlank(message)) {
                message = "您的【" + processInstance.getName() + "】单据还未审批，请您及时处理。";
            }
            List<Task> list = QueryUtils.taskQuery(taskUrgingBo.getProcessInstanceId()).list();
            WorkflowUtils.sendMessage(list, processInstance.getName(), taskUrgingBo.getMessageType(), message);
        } catch (ServiceException e) {
            throw new ServiceException(e.getMessage());
        }
        return true;
    }
}
