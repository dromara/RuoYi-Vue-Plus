/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.dromara.warm.flow.service;
import org.dromara.warm.flow.json.JsonUtil;

import org.dromara.warm.flow.entity.*;

import org.dromara.common.core.utils.StreamUtils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import org.dromara.warm.flow.FlowEngine;
import org.dromara.warm.flow.constant.ExceptionCons;
import org.dromara.warm.flow.dto.FlowCombine;
import org.dromara.warm.flow.dto.FlowParams;
import org.dromara.warm.flow.dto.PathWayData;
import org.dromara.warm.flow.enums.ActivityStatus;
import org.dromara.warm.flow.enums.FlowStatus;
import org.dromara.warm.flow.enums.NodeType;
import org.dromara.warm.flow.enums.SkipType;
import org.dromara.warm.flow.listener.ListenerVariable;
import org.dromara.warm.flow.service.WarmServiceImpl;
import org.dromara.warm.flow.utils.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import org.dromara.warm.flow.entity.FlowInstance;

import org.dromara.common.core.utils.SpringUtils;
import org.springframework.stereotype.Service;
import org.dromara.warm.flow.mapper.FlowInstanceMapper;



/**
 * 流程实例Service业务层处理
 *
 * @author warm
 * @since 2023-03-29
 */
@Service
public class InsService extends WarmServiceImpl<FlowInstance> {
    public FlowInstance start(String businessId, FlowParams flowParams) {
        AssertUtil.isNull(flowParams.getFlowCode(), ExceptionCons.NULL_FLOW_CODE);
        AssertUtil.isEmpty(businessId, ExceptionCons.NULL_BUSINESS_ID);
        // 获取已发布的流程节点
        FlowDefinition definition = FlowEngine.defService().getPublishByFlowCode(flowParams.getFlowCode());
        AssertUtil.isNull(definition, ExceptionCons.NOT_FOUNT_DEF);
        FlowCombine flowCombine = FlowEngine.defService().getFlowCombine(definition);
        // 获取开始节点
        FlowNode startNode = StreamUtils.findFirstValue(flowCombine.getAllNodes(), t -> NodeType.isStart(t.getNodeType()));
        AssertUtil.isNull(startNode, ExceptionCons.LOST_START_NODE);

        // 判断流程定义是否激活状态
        AssertUtil.isTrue(definition.getActivityStatus().equals(ActivityStatus.SUSPENDED.getKey())
            , ExceptionCons.NOT_DEFINITION_ACTIVITY);
        flowParams.skipType(SkipType.PASS.getKey());

        // 执行开始监听器
        ListenerUtil.executeStart(new ListenerVariable(definition, null, startNode, flowParams.getVariable())
            .setFlowParams(flowParams));


        // 获取下一个节点，如果是网关节点，则重新获取后续节点
        PathWayData pathWayData = new PathWayData().setDefId(startNode.getDefinitionId()).setSkipType(flowParams.getSkipType());
        List<FlowNode> nextNodes = FlowEngine.nodeService().getNextNodeList(startNode, null, flowParams.getSkipType(),
            flowParams.getVariable(), pathWayData, flowCombine);

        // 设置流程实例对象
        FlowInstance instance = setStartInstance(nextNodes.get(0), businessId, flowParams);

        // 设置历史任务
        FlowHisTask hisTask = setHisTask(nextNodes, flowParams, startNode, instance.getId());

        List<FlowTask> addTasks = StreamUtils.toList(nextNodes, node -> FlowEngine.taskService()
            .addTask(node, instance, definition, flowParams));

        // 办理人变量替换
        if (CollUtil.isNotEmpty(addTasks)) {
            ExpressionUtil.evalVariable(addTasks, flowParams);
        }

        // 设置流程图元数据
        pathWayData.getTargetNodes().addAll(nextNodes);
        instance.setDefJson(FlowEngine.chartService().startMetadata(pathWayData));

        // 执行分派监听器
        ListenerUtil.executeAssignment(new ListenerVariable(definition, instance, startNode, flowParams.getVariable()
            , null, nextNodes, addTasks).setFlowParams(flowParams));

        // 开启流程，保存流程信息
        saveFlowInfo(instance, addTasks, hisTask, flowParams);

        // 执行完成和创建监听器
        ListenerUtil.endCreateListener(new ListenerVariable(definition, instance, startNode, flowParams.getVariable()
            , null, nextNodes, addTasks).setFlowParams(flowParams));

        return instance;
    }

    public List<FlowInstance> listByDefIds(List<Long> defIds) {
        return getMapper().getByDefIds(defIds);
    }

    public boolean remove(List<Long> instanceIds) {
        return toRemoveTask(instanceIds);
    }

    public List<FlowInstance> getByDefId(Long definitionId) {
        return list(new FlowInstance().setDefinitionId(definitionId));
    }

    /**
     * 设置历史任务
     *
     * @param nextNodes  下一节点集合
     * @param flowParams 流程参数
     * @param startNode  开始节点
     * @param instanceId 流程实例id
     */
    private FlowHisTask setHisTask(List<FlowNode> nextNodes, FlowParams flowParams, FlowNode startNode, Long instanceId) {
        FlowTask startTask = new FlowTask()
            .setInstanceId(instanceId)
            .setDefinitionId(startNode.getDefinitionId())
            .setNodeCode(startNode.getNodeCode())
            .setNodeName(startNode.getNodeName())
            .setNodeType(startNode.getNodeType());
        FlowEngine.dataFillHandler().idFill(startTask);
        // 开始任务转历史任务
        return FlowEngine.hisTaskService().setSkipInsHis(startTask, nextNodes, flowParams);
    }

    /**
     * 开启流程，保存流程信息
     *
     * @param instance 流程实例
     * @param addTasks 新增任务
     * @param hisTask  历史任务
     */
    private void saveFlowInfo(FlowInstance instance, List<FlowTask> addTasks, FlowHisTask hisTask, FlowParams flowParams) {
        FlowEngine.taskService().setInsFinishInfo(instance, addTasks, flowParams);
        FlowEngine.hisTaskService().save(hisTask);
        // 待办任务设置处理人
        if (CollUtil.isNotEmpty(addTasks)) {
            List<FlowUser> users = FlowEngine.userService().taskAddUsers(addTasks);
            FlowEngine.taskService().saveBatch(addTasks);
            FlowEngine.userService().saveBatch(users);
        }
        save(instance);
    }

    /**
     * 设置流程实例对象
     *
     * @param firstBetweenNode 第一个中间节点
     * @param businessId       业务id
     * @return FlowInstance
     */
    private FlowInstance setStartInstance(FlowNode firstBetweenNode, String businessId
        , FlowParams flowParams) {
        FlowInstance instance = new FlowInstance();
        Date now = new Date();
        FlowEngine.dataFillHandler().idFill(instance);
        // 关联业务id,其实后面可以不用到业务id,传业务id目前来看只是为了批量创建流程的时候能创建出有区别化的流程,也是为了后期需要用到businessId。
        instance.setDefinitionId(firstBetweenNode.getDefinitionId())
            .setBusinessId(businessId)
            .setNodeType(firstBetweenNode.getNodeType())
            .setNodeCode(firstBetweenNode.getNodeCode())
            .setNodeName(firstBetweenNode.getNodeName())
            .setFlowStatus(StrUtil.emptyToDefault(flowParams.getFlowStatus(), FlowStatus.TOBESUBMIT.getKey()))
            .setActivityStatus(ActivityStatus.ACTIVITY.getKey())
            .setVariable(JsonUtil.objToStr(flowParams.getVariable()))
            .setCreateTime(now)
            .setUpdateTime(now)
            .setCreateBy(flowParams.getHandler())
            .setUpdateBy(flowParams.getHandler())
            .setExt(flowParams.getExt());
        return instance;
    }

    private boolean toRemoveTask(List<Long> instanceIds) {
        AssertUtil.isEmpty(instanceIds, ExceptionCons.NULL_INSTANCE_ID);

        List<Long> taskIds = new ArrayList<>();
        instanceIds.forEach(instanceId -> taskIds.addAll(
            FlowEngine.taskService()
                .list(new FlowTask().setInstanceId(instanceId))
                .stream()
                .map(FlowTask::getId)
                .collect(Collectors.toList())));

        if (CollUtil.isNotEmpty(taskIds)) {
            FlowEngine.userService().deleteByTaskIds(taskIds);
        }

        FlowEngine.taskService().deleteByInsIds(instanceIds);
        FlowEngine.hisTaskService().deleteByInsIds(instanceIds);
        return FlowEngine.insService().removeByIds(instanceIds);
    }

    public boolean active(Long id) {
        FlowInstance instance = getById(id);
        AssertUtil.isNull(instance, ExceptionCons.NOT_FOUNT_INSTANCE);
        AssertUtil.isTrue(ActivityStatus.isActivity(instance.getActivityStatus()), ExceptionCons.INSTANCE_ALREADY_ACTIVITY);
        instance.setActivityStatus(ActivityStatus.ACTIVITY.getKey());
        return updateById(instance);
    }

    public boolean unActive(Long id) {
        FlowInstance instance = getById(id);
        AssertUtil.isNull(instance, ExceptionCons.NOT_FOUNT_INSTANCE);
        AssertUtil.isTrue(ActivityStatus.isSuspended(instance.getActivityStatus()), ExceptionCons.INSTANCE_ALREADY_SUSPENDED);
        instance.setActivityStatus(ActivityStatus.SUSPENDED.getKey());
        return updateById(instance);
    }

    public void removeVariables(Long instanceId, String... keys) {
        FlowInstance instance = FlowEngine.insService().getById(instanceId);
        if (instance != null) {
            Map<String, Object> variableMap = instance.getVariableMap();
            for (String key : keys) {
                variableMap.remove(key);
            }
            instance.setVariable(JsonUtil.objToStr(variableMap));
            FlowEngine.insService().updateById(instance);
        }
    }

    @Override
    public FlowInstanceMapper getMapper() {
        return SpringUtils.getBean(FlowInstanceMapper.class);
    }
}