package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.domain.bo.ProcessInstanceBo;
import org.dromara.workflow.domain.bo.ProcessInvalidBo;
import org.dromara.workflow.domain.vo.ActHistoryInfoVo;
import org.dromara.workflow.domain.vo.GraphicInfoVo;
import org.dromara.workflow.domain.vo.ProcessInstanceVo;
import org.dromara.workflow.flowable.CustomDefaultProcessDiagramGenerator;
import org.dromara.workflow.service.IActProcessInstanceService;
import org.flowable.bpmn.model.*;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程实例 服务层实现
 *
 * @author may
 */

@RequiredArgsConstructor
@Service
public class ActProcessInstanceServiceImpl implements IActProcessInstanceService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final ISysUserService iSysUserService;
    private final TaskService taskService;

    @Value("${flowable.activity-font-name}")
    private String activityFontName;

    @Value("${flowable.label-font-name}")
    private String labelFontName;

    @Value("${flowable.annotation-font-name}")
    private String annotationFontName;

    /**
     * 分页查询正在运行的流程实例
     *
     * @param processInstanceBo 参数
     */
    @Override
    public TableDataInfo<ProcessInstanceVo> getProcessInstanceRunningByPage(ProcessInstanceBo processInstanceBo) {
        List<ProcessInstanceVo> list = new ArrayList<>();
        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
        query.processInstanceTenantId(TenantHelper.getTenantId());
        if (StringUtils.isNotBlank(processInstanceBo.getName())) {
            query.processInstanceNameLikeIgnoreCase("%" + processInstanceBo.getName() + "%");
        }
        if (StringUtils.isNotBlank(processInstanceBo.getStartUserId())) {
            query.startedBy(processInstanceBo.getStartUserId());
        }
        if (StringUtils.isNotBlank(processInstanceBo.getBusinessKey())) {
            query.processInstanceBusinessKey(processInstanceBo.getBusinessKey());
        }
        List<ProcessInstance> processInstances = query.listPage(processInstanceBo.getPageNum(), processInstanceBo.getPageSize());
        for (ProcessInstance processInstance : processInstances) {
            list.add(BeanUtil.toBean(processInstance, ProcessInstanceVo.class));
        }
        long count = query.count();
        return new TableDataInfo<>(list, count);
    }

    /**
     * 分页查询已结束的流程实例
     *
     * @param processInstanceBo 参数
     */
    @Override
    public TableDataInfo<ProcessInstanceVo> getProcessInstanceFinishByPage(ProcessInstanceBo processInstanceBo) {
        List<ProcessInstanceVo> list = new ArrayList<>();
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().finished()
            .orderByProcessInstanceEndTime().desc();
        query.processInstanceTenantId(TenantHelper.getTenantId());
        if (StringUtils.isNotEmpty(processInstanceBo.getName())) {
            query.processInstanceNameLikeIgnoreCase("%" + processInstanceBo.getName() + "%");
        }
        if (StringUtils.isNotEmpty(processInstanceBo.getStartUserId())) {
            query.startedBy(processInstanceBo.getStartUserId());
        }
        if (StringUtils.isNotBlank(processInstanceBo.getBusinessKey())) {
            query.processInstanceBusinessKey(processInstanceBo.getBusinessKey());
        }
        List<HistoricProcessInstance> historicProcessInstances = query.listPage(processInstanceBo.getPageNum(), processInstanceBo.getPageSize());
        for (HistoricProcessInstance historicProcessInstance : historicProcessInstances) {
            list.add(BeanUtil.toBean(historicProcessInstance, ProcessInstanceVo.class));
        }
        long count = query.count();
        return new TableDataInfo<>(list, count);
    }

    /**
     * 通过流程实例id获取历史流程图
     *
     * @param processInstanceId 流程实例id
     * @param response          响应
     */
    @Override
    public void getHistoryProcessImage(String processInstanceId, HttpServletResponse response) {
        // 设置页面不缓存
        response.setHeader("Pragma", "no-cache");
        response.addHeader("Cache-Control", "must-revalidate");
        response.addHeader("Cache-Control", "no-cache");
        response.addHeader("Cache-Control", "no-store");
        response.setDateHeader("Expires", 0);
        InputStream inputStream = null;
        try {
            String processDefinitionId;
            // 获取当前的流程实例
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            // 如果流程已经结束，则得到结束节点
            if (Objects.isNull(processInstance)) {
                HistoricProcessInstance pi = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
                processDefinitionId = pi.getProcessDefinitionId();
            } else {
                // 根据流程实例ID获得当前处于活动状态的ActivityId合集
                ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
                processDefinitionId = pi.getProcessDefinitionId();
            }

            // 获得活动的节点
            List<HistoricActivityInstance> highLightedFlowList = historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).orderByHistoricActivityInstanceStartTime().asc().list();

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
            inputStream = diagramGenerator.generateDiagram(bpmnModel, "png", highLightedNodeList, highLightedFlows, activityFontName, labelFontName, annotationFontName, null, 1.0, true);
            // 响应相关图片
            response.setContentType("image/png");

            byte[] bytes = IOUtils.toByteArray(inputStream);
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取审批记录
     *
     * @param processInstanceId 流程实例id
     */
    @Override
    public Map<String, Object> getHistoryRecord(String processInstanceId) {
        Map<String, Object> map = new HashMap<>();
        //查询任务办理记录
        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
            .processInstanceId(processInstanceId).taskTenantId(TenantHelper.getTenantId()).orderByHistoricTaskInstanceEndTime().desc().list();
        list = StreamUtils.sorted(list, Comparator.comparing(HistoricTaskInstance::getEndTime, Comparator.nullsFirst(Date::compareTo)).reversed());
        List<ActHistoryInfoVo> actHistoryInfoVoList = new ArrayList<>();
        for (HistoricTaskInstance historicTaskInstance : list) {
            ActHistoryInfoVo actHistoryInfoVo = new ActHistoryInfoVo();
            BeanUtils.copyProperties(historicTaskInstance, actHistoryInfoVo);
            actHistoryInfoVo.setStatus(actHistoryInfoVo.getEndTime() == null ? "待处理" : "已处理");
            List<Comment> taskComments = taskService.getTaskComments(historicTaskInstance.getId());
            if (CollUtil.isNotEmpty(taskComments)) {
                actHistoryInfoVo.setCommentId(taskComments.get(0).getId());
                String message = taskComments.stream().map(Comment::getFullMessage).collect(Collectors.joining("。"));
                if (StringUtils.isNotBlank(message)) {
                    actHistoryInfoVo.setComment(message);
                }
            }
            if (ObjectUtil.isNotEmpty(historicTaskInstance.getDurationInMillis())) {
                actHistoryInfoVo.setRunDuration(getDuration(historicTaskInstance.getDurationInMillis()));
            }
            actHistoryInfoVoList.add(actHistoryInfoVo);
        }
        //翻译人员名称
        if (CollUtil.isNotEmpty(actHistoryInfoVoList)) {
            actHistoryInfoVoList.forEach(e -> {
                SysUserVo sysUserVo = iSysUserService.selectUserById(Long.valueOf(e.getAssignee()));
                e.setNickName(ObjectUtil.isNotEmpty(sysUserVo) ? sysUserVo.getNickName() : "");
            });
        }
        List<ActHistoryInfoVo> nodeInfoList = new ArrayList<>();
        Map<String, List<ActHistoryInfoVo>> groupByKey = StreamUtils.groupByKey(actHistoryInfoVoList, ActHistoryInfoVo::getTaskDefinitionKey);
        for (Map.Entry<String, List<ActHistoryInfoVo>> entry : groupByKey.entrySet()) {
            ActHistoryInfoVo actHistoryInfoVo = BeanUtil.toBean(entry.getValue().get(0), ActHistoryInfoVo.class);
            String nickName = entry.getValue().stream().filter(e -> StringUtils.isNotBlank(e.getNickName()) && e.getEndTime() == null).map(ActHistoryInfoVo::getNickName).toList().stream().distinct().collect(Collectors.joining(StringUtils.SEPARATOR));
            if (StringUtils.isNotBlank(nickName)) {
                actHistoryInfoVo.setNickName(nickName);
            }
            actHistoryInfoVoList.stream().filter(e -> e.getTaskDefinitionKey().equals(entry.getKey()) && e.getEndTime() == null).findFirst()
                .ifPresent(e -> {
                    actHistoryInfoVo.setStatus("待处理");
                    actHistoryInfoVo.setStartTime(e.getStartTime());
                    actHistoryInfoVo.setEndTime(null);
                    actHistoryInfoVo.setRunDuration(null);
                });
            nodeInfoList.add(actHistoryInfoVo);
        }
        //节点信息
        map.put("nodeListInfo", nodeInfoList);
        List<ActHistoryInfoVo> collect = new ArrayList<>();
        //待办理
        List<ActHistoryInfoVo> waitingTask = StreamUtils.filter(actHistoryInfoVoList, e -> e.getEndTime() == null);
        //已办理
        List<ActHistoryInfoVo> finishTask = StreamUtils.filter(actHistoryInfoVoList, e -> e.getEndTime() != null);
        collect.addAll(waitingTask);
        collect.addAll(finishTask);
        //审批记录
        map.put("historyRecordList", collect);
        BpmnModel bpmnModel = repositoryService.getBpmnModel(list.get(0).getProcessDefinitionId());
        List<GraphicInfoVo> graphicInfoVos = new ArrayList<>();
        Collection<FlowElement> flowElements = bpmnModel.getMainProcess().getFlowElements();
        for (FlowElement flowElement : flowElements) {
            if (flowElement instanceof UserTask) {
                GraphicInfo graphicInfo = bpmnModel.getGraphicInfo(flowElement.getId());
                GraphicInfoVo graphicInfoVo = BeanUtil.toBean(graphicInfo, GraphicInfoVo.class);
                graphicInfoVo.setNodeId(flowElement.getId());
                graphicInfoVo.setNodeName(flowElement.getName());
                graphicInfoVos.add(graphicInfoVo);
            }
        }
        //节点图形信息
        map.put("graphicInfoVos", graphicInfoVos);
        return map;
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
    public boolean deleteRuntimeProcessInst(ProcessInvalidBo processInvalidBo) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
            .processInstanceId(processInvalidBo.getProcessInstId()).processInstanceTenantId(TenantHelper.getTenantId()).singleResult();
        try {
            List<Task> list = taskService.createTaskQuery().processInstanceId(processInvalidBo.getProcessInstId())
                .taskTenantId(TenantHelper.getTenantId()).list();
            List<Task> subTasks = list.stream().filter(e -> StringUtils.isNotBlank(e.getParentTaskId())).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(subTasks)) {
                subTasks.forEach(e -> taskService.deleteTask(e.getId()));
            }
            String deleteReason = LoginHelper.getUsername() + "作废了当前申请！";
            if (StringUtils.isNotBlank(processInvalidBo.getDeleteReason())) {
                deleteReason = LoginHelper.getUsername() + "作废理由:" + processInvalidBo.getDeleteReason();
            }
            runtimeService.deleteProcessInstance(processInvalidBo.getProcessInstId(), deleteReason);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 运行中的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param processInstanceId 流程实例id
     * @return
     */
    @Override
    public boolean deleteRuntimeProcessAndHisInst(String processInstanceId) {
        try {
            //1.删除运行中流程实例
            List<Task> list = taskService.createTaskQuery().processInstanceId(processInstanceId)
                .taskTenantId(TenantHelper.getTenantId()).list();
            List<Task> subTasks = list.stream().filter(e -> StringUtils.isNotBlank(e.getParentTaskId())).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(subTasks)) {
                subTasks.forEach(e -> taskService.deleteTask(e.getId()));
            }
            runtimeService.deleteProcessInstance(processInstanceId, LoginHelper.getUserId() + "删除了当前流程申请");
            //2.删除历史记录
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceTenantId(TenantHelper.getTenantId()).processInstanceId(processInstanceId).singleResult();
            if (ObjectUtil.isNotEmpty(historicProcessInstance)) {
                historyService.deleteHistoricProcessInstance(processInstanceId);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 已完成的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param processInstanceId 流程实例id
     */
    @Override
    public boolean deleteFinishProcessAndHisInst(String processInstanceId) {
        try {
            historyService.deleteHistoricProcessInstance(processInstanceId);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServiceException(e.getMessage());
        }
    }
}
