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
import org.dromara.common.json.utils.JsonUtils;
import tools.jackson.core.type.TypeReference;

import org.dromara.warm.flow.entity.*;

import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.dromara.common.core.utils.StreamUtils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;

import org.dromara.warm.flow.FlowEngine;
import org.dromara.warm.flow.constant.ExceptionCons;
import org.dromara.warm.flow.constant.FlowCons;
import org.dromara.warm.flow.dto.*;
import org.dromara.warm.flow.enums.*;
import org.dromara.warm.flow.listener.ListenerVariable;
import org.dromara.warm.flow.service.WarmServiceImpl;
import org.dromara.warm.flow.utils.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;


import org.dromara.warm.flow.entity.FlowTask;

import org.dromara.common.core.utils.SpringUtils;
import org.springframework.stereotype.Service;
import org.dromara.warm.flow.mapper.FlowTaskMapper;

import java.util.List;


/**
 * 待办任务Service业务层处理
 *
 * @author warm
 * @since 2023-03-29
 */
@Service
public class TaskService extends WarmServiceImpl<FlowTask> {
    public FlowInstance pass(Long taskId, String message, Map<String, Object> variable) {
        return skip(taskId, new FlowParams(SkipType.PASS.getKey(), message, variable));
    }

    public FlowInstance passAtWill(Long taskId, String nodeCode, String message, Map<String, Object> variable) {
        return skip(taskId, new FlowParams(nodeCode, SkipType.PASS.getKey(), message, variable));
    }

    public FlowInstance pass(Long taskId, String message, Map<String, Object> variable, String flowStatus, String hisStatus) {
        return skip(taskId, new FlowParams(SkipType.PASS.getKey(), message, variable, flowStatus, hisStatus));
    }

    public FlowInstance passAtWill(Long taskId, String nodeCode, String message, Map<String, Object> variable
        , String flowStatus, String hisStatus) {
        return skip(taskId, new FlowParams(nodeCode, SkipType.PASS.getKey(), message, variable, flowStatus, hisStatus));
    }


    public FlowInstance reject(Long taskId, String message, Map<String, Object> variable) {
        return skip(taskId, new FlowParams(SkipType.REJECT.getKey(), message, variable));
    }

    public FlowInstance rejectAtWill(Long taskId, String nodeCode, String message, Map<String, Object> variable) {
        return skip(taskId, new FlowParams(nodeCode, SkipType.REJECT.getKey(), message, variable));
    }

    public FlowInstance reject(Long taskId, String message, Map<String, Object> variable, String flowStatus, String hisStatus) {
        return skip(taskId, new FlowParams(SkipType.REJECT.getKey(), message, variable, flowStatus, hisStatus));
    }

    public FlowInstance rejectAtWill(Long taskId, String nodeCode, String message, Map<String, Object> variable
        , String flowStatus, String hisStatus) {
        return skip(taskId, new FlowParams(nodeCode, SkipType.REJECT.getKey(), message, variable, flowStatus, hisStatus));
    }


    public FlowInstance skip(Long taskId, FlowParams flowParams) {
        // 获取待办任务
        FlowTask task = getById(taskId);
        return skip(flowParams, task);
    }

    public FlowInstance skipByInsId(Long instanceId, FlowParams flowParams) {
        return skip(flowParams, getTask(instanceId));
    }

    public FlowInstance rejectLastByInsId(Long instanceId, FlowParams flowParams) {
        return rejectLast(getTask(instanceId), flowParams);
    }

    public FlowInstance rejectLast(Long taskId, FlowParams flowParams) {
        return rejectLast(getById(taskId), flowParams);
    }

    public FlowInstance rejectLast(FlowTask task, FlowParams flowParams) {
        flowParams.skipType(SkipType.REJECT.getKey());
        AssertUtil.isNull(task, ExceptionCons.NOT_FOUNT_TASK);
        // 获取当前任务的前置任务
        List<FlowHisTask> hisTaskList = FlowEngine.hisTaskService().getByInsId(task.getInstanceId());
        // 获取hisTaskList中TargetNodeCod等于task.getNodeCode()的，并且id最大的
        FlowHisTask lastHisTask = hisTaskList.stream()
            .filter(hisTask -> StrUtil.isNotEmpty(hisTask.getTargetNodeCode()))
            .filter(hisTask -> SkipType.isPass(hisTask.getSkipType()))
            .filter(hisTask -> {
                String targetCode = hisTask.getTargetNodeCode();
                if (targetCode.contains(",")) {
                    return Arrays.asList(targetCode.split(",")).contains(task.getNodeCode());
                } else {
                    return targetCode.equals(task.getNodeCode());
                }
            })
            .max(Comparator.comparingLong(FlowHisTask::getId))
            .orElse(null);

        AssertUtil.isNull(lastHisTask, ExceptionCons.NOT_FOUNT_LAST_TASK);
        flowParams.nodeCode(lastHisTask.getNodeCode());
        return skip(flowParams, task);
    }

    public FlowInstance taskBackByInsId(Long instanceId, FlowParams flowParams) {
        // 获取当前任务的前置任务
        FlowHisTask lastHisTask = taskBack(flowParams, instanceId);
        List<FlowNode> suffixNodeList = FlowEngine.nodeService().suffixNodeList(lastHisTask.getDefinitionId()
            , lastHisTask.getNodeCode());

        List<String> suffixNodeCodes = StreamUtils.toList(suffixNodeList, FlowNode::getNodeCode);
        List<FlowTask> taskList = FlowEngine.taskService().getByInsIdAndNodeCodes(instanceId, suffixNodeCodes);
        AssertUtil.isEmpty(taskList, ExceptionCons.NOT_FOUNT_HANDLED_TASK_HANDLER);
        return skip(flowParams, taskList.get(0));
    }

    public FlowInstance taskBack(Long taskId, FlowParams flowParams) {
        FlowTask task = getById(taskId);
        AssertUtil.isNull(task, ExceptionCons.NOT_FOUNT_TASK);
        taskBack(flowParams, task.getInstanceId());
        return skip(flowParams, task);
    }

    public FlowInstance skip(FlowParams flowParams, FlowTask task) {
        // TODO min 后续考虑并发问题，待办任务和实例表不同步，可给待办任务id加锁，抽取所接口，方便后续兼容分布式锁
        // 流程开启前正确性校验
        R r = getAndCheck(task);
        flowParams.variable(MapUtil.mergeAll(r.instance().getVariableMap(), flowParams.getVariable()));
        // 非第一个记得跳转类型必传
        if (!NodeType.isStart(task.getNodeType())) {
            AssertUtil.isFalse(StrUtil.isNotEmpty(flowParams.getSkipType()), ExceptionCons.NULL_CONDITION_VALUE);
        }
        task.setUserList(FlowEngine.userService().listByAssociatedAndTypes(task.getId()));
        FlowCombine flowCombine = FlowEngine.defService().getFlowCombineNoDef(r.definition().getId());

        // 执行开始监听器
        ListenerUtil.executeStart(new ListenerVariable(r.definition(), r.instance(), r.nowNode(), flowParams.getVariable()
            , task).setFlowParams(flowParams));

        // 如果是受托人在处理任务，需要处理一条委派记录，并且更新委托人，回到计划审批人,然后直接返回流程实例
        if (!flowParams.isIgnoreDepute() && handleDepute(task, flowParams)) {
            return r.instance();
        }

        // 判断当前处理人是否有权限处理
        checkAuth(task, flowParams);

        //或签、会签、票签逻辑处理
        if (!flowParams.isIgnoreCooperate() && cooperate(r.nowNode(), task, flowParams)) {
            return r.instance();
        }

        // 获取后续任务节点结合
        PathWayData pathWayData = new PathWayData().setInsId(task.getInstanceId()).setSkipType(flowParams.getSkipType());
        FlowNode nextNode = FlowEngine.nodeService().getNextNode(r.nowNode(), flowParams.getNodeCode()
            , flowParams.getSkipType(), pathWayData, flowCombine);
        List<FlowNode> nextNodes = FlowEngine.nodeService().getNextByCheckGateway(flowParams.getVariable()
            , nextNode, pathWayData, flowCombine);

        // 判断并行网关和包容网关节点只剩一个前置代办任务，才能生成新的代办任务
        isGenerateNewTask(pathWayData, r.instance(), nextNodes);
        pathWayData.getTargetNodes().addAll(nextNodes);

        // 设置流程图元数据
        r.instance().setDefJson(FlowEngine.chartService().skipMetadata(pathWayData));

        // 构建增待办任务和设置结束任务历史记录
        List<FlowTask> addTasks = StreamUtils.toList(nextNodes, node -> addTask(node, r.instance(), r.definition(), flowParams));

        // 办理人变量替换
        ExpressionUtil.evalVariable(addTasks, flowParams.variable(MapUtil.mergeAll(r.instance().getVariableMap(), flowParams.getVariable())));

        // 执行分派监听器
        ListenerUtil.executeAssignment(new ListenerVariable(r.definition(), r.instance(), r.nowNode(), flowParams.getVariable()
            , task, nextNodes, addTasks).setFlowParams(flowParams));

        // 更新流程信息
        updateFlowInfo(task, r.instance(), addTasks, flowParams, nextNodes);

        // 一票否决（谨慎使用），如果退回，退回指向节点后还存在其他正在执行的待办任务，转历史任务，状态都为失效,重走流程。
        if (CollUtil.isNotEmpty(nextNodes) && SkipType.isReject(flowParams.getSkipType())) {
            oneVoteVeto(task, nextNodes.get(0).getNodeCode(), flowCombine);
        }

        // 处理未完成的任务，当流程完成，还存在待办任务未完成，转历史任务，状态完成。
        handUndoneTask(r.instance());

        // 执行完成和创建监听器
        ListenerUtil.endCreateListener(new ListenerVariable(r.definition(), r.instance(), r.nowNode()
            , flowParams.getVariable(), task, nextNodes, addTasks).setFlowParams(flowParams));

        return r.instance();
    }

    public FlowInstance revoke(Long instanceId, FlowParams flowParams) {
        flowParams.skipType(SkipType.REJECT.getKey());
        // 删除待办任务，保存历史，删除所有代办任务的权限人
        if (StrUtil.isEmpty(flowParams.getFlowStatus())) {
            flowParams.flowStatus(FlowStatus.CANCEL.getKey());
        }

        FlowInstance instance = FlowEngine.insService().getById(instanceId);
        AssertUtil.isNull(instance, ExceptionCons.NOT_FOUNT_INSTANCE);
        FlowDefinition definition = FlowEngine.defService().getById(instance.getDefinitionId());
        AssertUtil.isFalse(judgeActivityStatus(definition, instance), ExceptionCons.NOT_ACTIVITY);
        AssertUtil.isTrue(NodeType.isEnd(instance.getNodeType()), ExceptionCons.FLOW_FINISH);
        flowParams.variable(MapUtil.mergeAll(instance.getVariableMap(), flowParams.getVariable()));

        List<FlowTask> taskList = getByInsId(instanceId);
        FlowCombine flowCombine = FlowEngine.defService().getFlowCombine(definition);
        Map<String, FlowNode> nodeMap = StreamUtils.toMap(flowCombine.getAllNodes(), FlowNode::getNodeCode, node -> node);
        // 执行开始监听器
        taskList.forEach(task -> ListenerUtil.executeStart(new ListenerVariable(definition, instance
                , nodeMap.get(task.getNodeCode()), flowParams.getVariable(), task).setFlowParams(flowParams)));

        // 验证权限是不是当前任务的发起人
        if (!flowParams.isIgnore()) {
            AssertUtil.isFalse(instance.getCreateBy().equals(flowParams.getHandler())
                , ExceptionCons.NOT_DEF_PROMOTER_NOT_CANCEL);
        }

        // 获取开始节点
        FlowNode startNode = StreamUtils.findFirstValue(flowCombine.getAllNodes(), node -> NodeType.isStart(node.getNodeType()));
        // 获取下一个节点，如果是网关节点，则重新获取后续节点
        PathWayData pathWayData = new PathWayData().setInsId(instanceId).setSkipType(flowParams.getSkipType());
        FlowNode nextNode = FlowEngine.nodeService().getNextNode(startNode, null, SkipType.PASS.getKey()
            , null, flowCombine);
        List<FlowNode> nextNodes = FlowEngine.nodeService().getNextByCheckGateway(flowParams.getVariable(), nextNode
            , pathWayData, flowCombine);
        pathWayData.getTargetNodes().addAll(nextNodes);
        // 设置流程图元数据
        instance.setDefJson(FlowEngine.chartService().skipMetadata(pathWayData));

        // 查询任务,如果前一个节点是并行网关，可能任务表有多个任务,增加查询和判断
        List<FlowTask> curTaskList = list(new FlowTask().setInstanceId(instance.getId()));
        AssertUtil.isEmpty(curTaskList, ExceptionCons.NOT_FOUND_FLOW_TASK);

        // 给回退到的那个节点赋权限-给当前处理人权限
        List<FlowTask> addTasks = StreamUtils.toList(nextNodes, node -> addTask(node, instance, definition, flowParams));

        // 办理人变量替换
        ExpressionUtil.evalVariable(addTasks, flowParams.variable(MapUtil.mergeAll(instance.getVariableMap(), flowParams.getVariable())));

        // 执行分派监听器
        taskList.forEach(task -> ListenerUtil.executeAssignment(new ListenerVariable(definition, instance,
            nodeMap.get(task.getNodeCode()), flowParams.getVariable(), task, nextNodes, addTasks)
            .setFlowParams(flowParams)));

        // 设置流程历史任务信息
        List<FlowHisTask> insHisList = FlowEngine.hisTaskService().setSkipHisList(curTaskList, nextNodes, flowParams);
        FlowEngine.hisTaskService().saveBatch(insHisList);
        // 待办任务和处理人
        removeAndUser(curTaskList);
        List<FlowUser> users = FlowEngine.userService().taskAddUsers(addTasks);

        // 设置任务完成后的实例相关信息
        setInsFinishInfo(instance, addTasks, flowParams);
        if (CollUtil.isNotEmpty(addTasks)) {
            saveBatch(addTasks);
        }
        FlowEngine.insService().updateById(instance);
        // 保存下一个待办任务的权限人
        FlowEngine.userService().saveBatch(users);

        // 执行完成和创建监听器
        taskList.forEach(task -> ListenerUtil.endCreateListener(new ListenerVariable(definition, instance,
            nodeMap.get(task.getNodeCode()), flowParams.getVariable(), task, nextNodes, addTasks).setFlowParams(flowParams)));
        return instance;
    }

    public FlowInstance terminationByInsId(Long instanceId, FlowParams flowParams) {
        AssertUtil.isNull(instanceId, ExceptionCons.NULL_INSTANCE_ID);
        // 获取待办任务
        List<FlowTask> taskList = FlowEngine.taskService().getByInsId(instanceId);
        AssertUtil.isEmpty(taskList, ExceptionCons.NOT_FOUNT_TASK);
        FlowTask task = taskList.get(0);
        return termination(task, flowParams);
    }

    public FlowInstance termination(Long taskId, FlowParams flowParams) {
        return termination(getById(taskId), flowParams);
    }

    public FlowInstance termination(FlowTask task, FlowParams flowParams) {
        R r = getAndCheck(task);
        flowParams.skipType(SkipType.PASS.getKey());
        flowParams.variable(MapUtil.mergeAll(r.instance().getVariableMap(), flowParams.getVariable()));
        ListenerUtil.executeStart(new ListenerVariable(r.definition(), r.instance(), r.nowNode(), flowParams.getVariable()
            , task).setFlowParams(flowParams));

        // 判断当前处理人是否有权限处理
        task.setUserList(FlowEngine.userService().listByAssociatedAndTypes(task.getId()));
        checkAuth(task, flowParams);

        // 所有待办转历史
        FlowNode endNode = FlowEngine.nodeService().getEndNode(r.instance().getDefinitionId());

        // 设置流程图元数据
        PathWayData pathWayData = new PathWayData()
            .setInsId(task.getInstanceId())
            .setSkipType(flowParams.getSkipType())
            .setPathWayNodes(Collections.singletonList(r.nowNode()))
            .setTargetNodes(Collections.singletonList(endNode));
        r.instance().setDefJson(FlowEngine.chartService().skipMetadata(pathWayData));

        // 流程实例完成
        r.instance().setNodeType(endNode.getNodeType())
            .setNodeCode(endNode.getNodeCode())
            .setNodeName(endNode.getNodeName())
            .setFlowStatus(StrUtil.emptyToDefault(flowParams.getFlowStatus(), FlowStatus.TERMINATE.getKey()));

        // 待办任务转历史
        flowParams.flowStatus(r.instance().getFlowStatus());
        FlowHisTask insHis = FlowEngine.hisTaskService().setSkipInsHis(task, Collections.singletonList(endNode)
            , flowParams);
        FlowEngine.hisTaskService().save(insHis);
        FlowEngine.insService().updateById(r.instance());

        // 删除流程相关办理人
        FlowEngine.userService().deleteByTaskIds(Collections.singletonList(task.getId()));

        // 处理未完成的任务，当流程完成，还存在待办任务未完成，转历史任务，状态完成。
        handUndoneTask(r.instance());
        // 最后判断是否存在节点监听器，存在执行节点监听器
        ListenerUtil.executeFinish(new ListenerVariable(r.definition(), r.instance(), r.nowNode(), flowParams.getVariable()
            , task).setFlowParams(flowParams));
        return r.instance();
    }

    public boolean deleteByInsIds(List<Long> instanceIds) {
        List<FlowInstance> instanceList = FlowEngine.insService().getByIds(instanceIds);
        FlowDefinition definition;
        for (FlowInstance instance : instanceList) {
            definition = FlowEngine.defService().getById(instance.getDefinitionId());
            AssertUtil.isFalse(judgeActivityStatus(definition, instance), ExceptionCons.NOT_ACTIVITY);
        }
        return SqlHelper.retBool(getMapper().deleteByInsIds(instanceIds));
    }

    public boolean transfer(Long taskId, FlowParams flowParams) {
        AssertUtil.isNull(taskId, ExceptionCons.NULL_TASK_ID);
        AssertUtil.isNull(flowParams.getHandler(), ExceptionCons.HANDLER_NOT_EMPTY);
        AssertUtil.isNull(flowParams.getAddHandlers(), ExceptionCons.NULL_TRANSFER_HANDLER);
        List<FlowUser> users = FlowEngine.userService().getByProcessedBys(taskId, flowParams.getAddHandlers(), UserType.TRANSFER.getKey());
        AssertUtil.isNotEmpty(users, ExceptionCons.IS_ALREADY_TRANSFER);
        flowParams.cooperateType(CooperateType.TRANSFER.getKey())
            .reductionHandlers(Collections.singletonList(flowParams.getHandler()));

        return updateHandler(taskId, flowParams);
    }

    public boolean depute(Long taskId, FlowParams flowParams) {
        AssertUtil.isNull(taskId, ExceptionCons.NULL_TASK_ID);
        AssertUtil.isNull(flowParams.getHandler(), ExceptionCons.HANDLER_NOT_EMPTY);
        AssertUtil.isNull(flowParams.getAddHandlers(), ExceptionCons.NULL_DEPUTE_HANDLER);
        List<FlowUser> users = FlowEngine.userService().getByProcessedBys(taskId, flowParams.getAddHandlers(), UserType.DEPUTE.getKey());
        AssertUtil.isNotEmpty(users, ExceptionCons.IS_ALREADY_DEPUTE);
        flowParams.cooperateType(CooperateType.DEPUTE.getKey())
            .reductionHandlers(Collections.singletonList(flowParams.getHandler()));

        return updateHandler(taskId, flowParams);
    }

    public boolean addSignature(Long taskId, FlowParams flowParams) {
        AssertUtil.isNull(taskId, ExceptionCons.NULL_TASK_ID);
        AssertUtil.isNull(flowParams.getHandler(), ExceptionCons.HANDLER_NOT_EMPTY);
        AssertUtil.isNull(flowParams.getAddHandlers(), ExceptionCons.NULL_ADD_SIGNATURE_HANDLER);
        List<FlowUser> users = FlowEngine.userService().getByProcessedBys(taskId, flowParams.getAddHandlers(), UserType.APPROVAL.getKey());
        AssertUtil.isNotEmpty(users, ExceptionCons.IS_ALREADY_SIGN);
        flowParams.cooperateType(CooperateType.ADD_SIGNATURE.getKey());

        return updateHandler(taskId, flowParams);
    }

    public boolean reductionSignature(Long taskId, FlowParams flowParams) {
        AssertUtil.isNull(taskId, ExceptionCons.NULL_TASK_ID);
        AssertUtil.isNull(flowParams.getHandler(), ExceptionCons.HANDLER_NOT_EMPTY);
        AssertUtil.isNull(flowParams.getReductionHandlers(), ExceptionCons.NULL_REDUCTION_SIGNATURE_HANDLER);
        List<FlowUser> users = FlowEngine.userService().listByAssociatedAndTypes(taskId
            , UserType.APPROVAL.getKey(), UserType.TRANSFER.getKey());
        AssertUtil.isTrue(CollUtil.isEmpty(users) || users.size() == 1, ExceptionCons.REDUCTION_SIGN_ONE_ERROR);
        flowParams.cooperateType(CooperateType.REDUCTION_SIGNATURE.getKey());

        return updateHandler(taskId, flowParams);
    }

    public boolean updateHandler(Long taskId, FlowParams flowParams) {
        // 获取待办任务
        R r = getAndCheck(taskId);
        flowParams.variable(MapUtil.mergeAll(r.instance().getVariableMap(), flowParams.getVariable()));
        // 执行开始监听器
        ListenerUtil.executeStart(new ListenerVariable(r.definition(), r.instance(), r.nowNode(), null, r.task()));

        // 获取给谁的权限
        if (!flowParams.isIgnore()) {
            // 判断当前处理人是否有权限，获取当前办理人的权限
            List<String> permissions = flowParams.getPermissionFlag();
            // 获取任务权限人
            List<String> taskPermissions = FlowEngine.userService().getPermission(taskId
                , UserType.APPROVAL.getKey(), UserType.TRANSFER.getKey(), UserType.DEPUTE.getKey());
            AssertUtil.isTrue(CollUtil.isNotEmpty(taskPermissions) && (CollUtil.isEmpty(permissions)
                || !CollUtil.containsAny(permissions, taskPermissions)), ExceptionCons.NOT_AUTHORITY);
        }
        // 留存历史记录
        flowParams.skipType(SkipType.NONE.getKey());
        FlowHisTask hisTask = null;
        // 删除对应的操作人
        if (CollUtil.isNotEmpty(flowParams.getReductionHandlers())) {
            for (String reductionHandler : flowParams.getReductionHandlers()) {
                FlowEngine.userService().remove(new FlowUser().setAssociated(taskId)
                    .setProcessedBy(reductionHandler));
            }
            hisTask = FlowEngine.hisTaskService().setCooperateHis(r.task(), flowParams, flowParams.getReductionHandlers());
        }

        // 新增权限人
        if (CollUtil.isNotEmpty(flowParams.getAddHandlers())) {
            String type;
            if (CooperateType.TRANSFER.getKey().equals(flowParams.getCooperateType())) {
                type = UserType.TRANSFER.getKey();
            } else if (CooperateType.DEPUTE.getKey().equals(flowParams.getCooperateType())) {
                type = UserType.DEPUTE.getKey();
            } else {
                type = UserType.APPROVAL.getKey();
            }
            FlowEngine.userService().saveBatch(StreamUtils.toList(flowParams.getAddHandlers(), permission ->
                FlowEngine.userService().structureUser(taskId, permission
                    , type, flowParams.getHandler())));
            hisTask = FlowEngine.hisTaskService().setCooperateHis(r.task(), flowParams, flowParams.getAddHandlers());
        }
        if (ObjectUtil.isNotNull(hisTask)) {
            FlowEngine.hisTaskService().save(hisTask);
        }
        // 最后判断是否存在节点监听器，存在执行节点监听器
        ListenerUtil.executeFinish(new ListenerVariable(r.definition(), r.instance(), r.nowNode(), flowParams.getVariable()
            , r.task()));
        return true;
    }

    public FlowInstance pendingByInsId(Long instanceId, FlowParams flowParams) {
        return pending(getTask(instanceId), flowParams);
    }

    public FlowInstance pending(Long taskId, FlowParams flowParams) {
        // 获取待办任务
        FlowTask task = getById(taskId);
        return pending(task, flowParams);
    }

    public FlowInstance pending(FlowTask task, FlowParams flowParams) {
        // TODO min 后续考虑并发问题，待办任务和实例表不同步，可给待办任务id加锁，抽取所接口，方便后续兼容分布式锁
        // 流程开启前正确性校验
        R r = getAndCheck(task);
        flowParams.flowStatus(StrUtil.emptyToDefault(flowParams.getFlowStatus(), FlowStatus.PENDING.getKey()));
        // 执行开始监听器
        ListenerUtil.executeStart(new ListenerVariable(r.definition(), r.instance(), r.nowNode(), flowParams.getVariable()
            , r.task()).setFlowParams(flowParams));

        // 判断当前处理人是否有权限处理
        checkAuth(r.task(), flowParams);

        // 设置流程历史任务信息
        FlowHisTask insHis = FlowEngine.hisTaskService().notSkip(r.task(), flowParams);
        FlowEngine.hisTaskService().save(insHis);

        FlowEngine.insService().updateById(r.instance().setFlowStatus(flowParams.getFlowStatus()));

        // 执行任务完成监听器
        ListenerUtil.executeFinish(new ListenerVariable(r.definition(), r.instance(), r.nowNode()
            , flowParams.getVariable(), r.task()));

        return r.instance();
    }

    public FlowTask addTask(FlowNode node, FlowInstance instance, FlowDefinition definition, FlowParams flowParams) {
        FlowTask addTask = new FlowTask();
        Date now = new Date();
        FlowEngine.dataFillHandler().idFill(addTask);
        addTask.setDefinitionId(instance.getDefinitionId())
            .setInstanceId(instance.getId())
            .setNodeCode(node.getNodeCode())
            .setNodeName(node.getNodeName())
            .setNodeType(node.getNodeType())
            .setFlowStatus(StrUtil.emptyToDefault(flowParams.getFlowStatus(),
                setFlowStatus(node.getNodeType(), flowParams.getSkipType())))
            .setCreateTime(now)
            .setPermissionList(StrUtil.splitTrim(node.getPermissionFlag(), FlowCons.SPLIT_AT));

        if (StrUtil.isNotEmpty(node.getFormCustom()) && StrUtil.isNotEmpty(node.getFormPath())) {
            // 节点有自定义表单则使用
            addTask.setFormCustom(node.getFormCustom()).setFormPath(node.getFormPath());
        } else {
            addTask.setFormCustom(definition.getFormCustom()).setFormPath(definition.getFormPath());
        }

        return addTask;
    }

    public List<FlowTask> getByInsId(Long instanceId) {
        return list(new FlowTask().setInstanceId(instanceId));
    }

    public List<FlowTask> getByInsIdAndNodeCodes(Long instanceId, List<String> nodeCodes) {
        return getMapper().getByInsIdAndNodeCodes(instanceId, nodeCodes);
    }

    public void setInsFinishInfo(FlowInstance instance, List<FlowTask> addTasks, FlowParams flowParams) {
        instance.setUpdateTime(new Date());
        // 合并流程变量到实例对象
        mergeVariable(instance, flowParams.getVariable());
        if (CollUtil.isNotEmpty(addTasks)) {
            // 终结节点任务不算待办任务，取其中最后一个作为实例最终信息
            List<FlowTask> endTasks = StreamUtils.filter(addTasks, addTask -> NodeType.isEnd(addTask.getNodeType()));
            addTasks.removeAll(endTasks);
            FlowTask finallyTask = CollUtil.getLast(endTasks);
            if (finallyTask == null) {
                finallyTask = getNextTask(addTasks);
            }
            instance.setNodeType(finallyTask.getNodeType())
                .setNodeCode(finallyTask.getNodeCode())
                .setNodeName(finallyTask.getNodeName())
                .setFlowStatus(finallyTask.getFlowStatus());
        }
    }

    public void mergeVariable(FlowInstance instance, Map<String, Object> variable) {
        if (MapUtil.isNotEmpty(variable)) {
            String variableStr = instance.getVariable();
            Map<String, Object> deserialize = Optional.ofNullable(JsonUtils.parseObject(variableStr, new TypeReference<Map<String, Object>>() {
            })).orElseGet(HashMap::new);
            deserialize.putAll(variable);
            instance.setVariable(JsonUtils.toJsonString(deserialize));
        }
    }

    /**
     * 根据流程实例id获取操作人最近的已办历史任务
     *
     * @param flowParams 包含流程相关参数的对象
     * @param instanceId 流程实例id
     * @return 最近的已办历史任务
     */
    private FlowHisTask taskBack(FlowParams flowParams, Long instanceId) {
        flowParams.skipType(SkipType.REJECT.getKey())
            .ignore(true)
            .ignoreDepute(true)
            .ignoreCooperate(true)
            .flowStatus(StrUtil.emptyToDefault(flowParams.getFlowStatus(), FlowStatus.TASK_BACK.getKey()));
        // 获取当前任务的前置任务
        List<FlowHisTask> hisTaskList = FlowEngine.hisTaskService().getByInsId(instanceId);
        // 获取hisTaskList中TargetNodeCod等于task.getNodeCode()的，并且id最大的
        FlowHisTask lastHisTask = hisTaskList.stream()
            .filter(hisTask -> StrUtil.isNotEmpty(hisTask.getApprover()))
            .filter(hisTask -> SkipType.isPass(hisTask.getSkipType()))
            .filter(hisTask -> hisTask.getApprover().equals(flowParams.getHandler()))
            .max(Comparator.comparingLong(FlowHisTask::getId))
            .orElse(null);
        AssertUtil.isNull(lastHisTask, ExceptionCons.NOT_FOUNT_HANDLED_TASK);
        flowParams.nodeCode(lastHisTask.getNodeCode());
        return lastHisTask;
    }

    /**
     * 获取待办任务
     *
     * @param instanceId 实例id
     * @return 待办任务
     */
    private FlowTask getTask(Long instanceId) {
        List<FlowTask> taskList = getByInsId(instanceId);
        AssertUtil.isEmpty(taskList, ExceptionCons.NOT_FOUNT_TASK);
        AssertUtil.isTrue(taskList.size() > 1, ExceptionCons.TASK_NOT_ONE);
        return taskList.get(0);
    }

    private String setFlowStatus(Integer nodeType, String skipType) {
        // 根据审批动作确定流程状态
        if (NodeType.isStart(nodeType)) {
            return FlowStatus.TOBESUBMIT.getKey();
        } else if (NodeType.isEnd(nodeType)) {
            return FlowStatus.FINISHED.getKey();
        } else if (SkipType.isReject(skipType)) {
            return FlowStatus.REJECT.getKey();
        } else {
            return FlowStatus.APPROVAL.getKey();
        }
    }

    private FlowTask getNextTask(List<FlowTask> tasks) {
        if (tasks.size() == 1) {
            return tasks.get(0);
        }
        return tasks.stream().max(Comparator.comparingLong(FlowTask::getId)).orElse(null);
    }

    private void removeAndUser(List<FlowTask> taskList) {
        removeByIds(StreamUtils.toList(taskList, FlowTask::getId));
        FlowEngine.userService().deleteByTaskIds(StreamUtils.toList(taskList, FlowTask::getId));
    }

    private R getAndCheck(Long taskId) {
        AssertUtil.isNull(taskId, ExceptionCons.NULL_TASK_ID);
        return getAndCheck(getById(taskId));
    }

    private R getAndCheck(FlowTask task) {
        AssertUtil.isNull(task, ExceptionCons.NOT_FOUNT_TASK);
        FlowInstance instance = FlowEngine.insService().getById(task.getInstanceId());
        AssertUtil.isNull(instance, ExceptionCons.NOT_FOUNT_INSTANCE);
        FlowDefinition definition = FlowEngine.defService().getById(instance.getDefinitionId());
        AssertUtil.isFalse(judgeActivityStatus(definition, instance), ExceptionCons.NOT_ACTIVITY);
        AssertUtil.isTrue(NodeType.isEnd(instance.getNodeType()), ExceptionCons.FLOW_FINISH);
        FlowNode nowNode = FlowEngine.nodeService().getByDefIdAndNodeCode(task.getDefinitionId(), task.getNodeCode());
        AssertUtil.isNull(nowNode, ExceptionCons.LOST_CUR_NODE);
        return new R(instance, definition, nowNode, task);
    }

    /**
     * 办理校验后的上下文：实例、定义、当前节点、当前任务
     */
    private record R(FlowInstance instance, FlowDefinition definition, FlowNode nowNode, FlowTask task) {
    }

    private boolean handleDepute(FlowTask task, FlowParams flowParams) {
        // 获取受托人
        List<FlowUser> entrustedUserList = StreamUtils.filter(task.getUserList(),
            user -> UserType.DEPUTE.getKey().equals(user.getType())
                && Objects.equals(flowParams.getHandler(), user.getProcessedBy()));
        if (CollUtil.isEmpty(entrustedUserList)) {
            return false;
        }

        // 记录受托人处理任务记录
        FlowUser entrustedUser = entrustedUserList.get(0);
        FlowHisTask hisTask = FlowEngine.hisTaskService().setDeputeHisTask(task, flowParams, entrustedUser);
        FlowEngine.hisTaskService().save(hisTask);
        FlowEngine.userService().removeById(entrustedUser.getId());

        // 查询委托人，如果在flow_user不存在，则给委托人新增待办记录
        FlowUser deputeUser = FlowEngine.userService().getOne(new FlowUser().setAssociated(task.getId())
            .setProcessedBy(entrustedUser.getCreateBy()).setType(UserType.APPROVAL.getKey()));
        if (ObjectUtil.isNull(deputeUser)) {
            FlowUser newUser = FlowEngine.userService().structureUser(entrustedUser.getAssociated()
                , entrustedUser.getCreateBy()
                , UserType.APPROVAL.getKey(), entrustedUser.getProcessedBy());
            FlowEngine.userService().save(newUser);
        }

        return true;
    }

    /**
     * 会签，票签，协作处理，返回true；或签或者会签、票签结束返回false
     *
     * @param nowNode    当前节点
     * @param task       任务
     * @param flowParams 流程参数
     * @return boolean
     */
    private boolean cooperate(FlowNode nowNode, FlowTask task, FlowParams flowParams) {
        if (flowParams.isIgnore()) {
            return false;
        }
        String nodeRatio = nowNode.getNodeRatio();
        // 或签，直接返回
        if (CooperateType.isOrSign(nodeRatio)) {
            return false;
        }

        // 办理人和转办人列表
        List<FlowUser> todoList = FlowEngine.userService().listByAssociatedAndTypes(task.getId()
            , UserType.APPROVAL.getKey(), UserType.TRANSFER.getKey(), UserType.DEPUTE.getKey());

        // 判断办理人是否有办理权限
        AssertUtil.isEmpty(flowParams.getHandler(), ExceptionCons.SIGN_NULL_HANDLER);
        FlowUser todoUser = CollUtil.getFirst(StreamUtils.filter(todoList, u -> Objects.equals(u.getProcessedBy(), flowParams.getHandler())));
        AssertUtil.isNull(todoUser, ExceptionCons.NOT_AUTHORITY);

        // 除当前办理人外剩余办理人列表
        List<FlowUser> restList = StreamUtils.filter(todoList, u -> !Objects.equals(u.getProcessedBy(), flowParams.getHandler()));

        // 会签并且当前人退回直接返回
        if (CooperateType.isCountersign(nodeRatio) && SkipType.isReject(flowParams.getSkipType())) {
            return removeRestList(restList);
        }

        // 查询会签票签已办列表
        List<FlowHisTask> doneList = FlowEngine.hisTaskService().listByTaskId(task.getId());
        doneList = CollUtil.isEmpty(doneList) ? new ArrayList<>() : doneList;

        // 总人数
        int allNum = todoList.size() + doneList.size();

        // 通过历史记录
        List<FlowHisTask> donePassList = StreamUtils.filter(doneList
            , hisTask -> Objects.equals(hisTask.getSkipType(), SkipType.PASS.getKey()));

        // 驳回历史记录
        List<FlowHisTask> doneRejectList = StreamUtils.filter(doneList
            , hisTask -> Objects.equals(hisTask.getSkipType(), SkipType.REJECT.getKey()));

        boolean isPass = SkipType.isPass(flowParams.getSkipType());
        // 如果是票签默认或者spel表达式策略，则执行表达式
        if (CooperateType.isVoteSignDefault(nodeRatio) || CooperateType.isVoteSignRejectSpel(nodeRatio)) {
            Map<String, Object> variable = MapUtil.clone(flowParams.getVariable());
            variable.put("skipType", flowParams.getSkipType());
            variable.put("passNum", donePassList.size());
            variable.put("rejectNum", doneRejectList.size());
            variable.put("todoNum", todoList.size());
            variable.put("allNum", allNum);
            variable.put("passList", donePassList);
            variable.put("rejectList", doneRejectList);
            variable.put("todoList", todoList);
            if (ExpressionUtil.evalVoteSign(nodeRatio, variable)) {
                return removeRestList(restList);
            }
        } else {
            // 计算通过率
            BigDecimal passRatio = (isPass ? BigDecimal.ONE : BigDecimal.ZERO)
                .add(BigDecimal.valueOf(donePassList.size()))
                .divide(BigDecimal.valueOf(allNum), 4, RoundingMode.HALF_UP).multiply(MathUtil.ONE_HUNDRED);
            // 计算驳回率
            BigDecimal rejectRatio = (isPass ? BigDecimal.ZERO : BigDecimal.ONE)
                .add(BigDecimal.valueOf(doneRejectList.size()))
                .divide(BigDecimal.valueOf(allNum), 4, RoundingMode.HALF_UP).multiply(MathUtil.ONE_HUNDRED);

            // 判断是否是票签中的固定通过人数，如果是则判断是否达到该人数
            if (CooperateType.isVoteSignPassCount(nodeRatio)) {
                String passCount = StrUtil.subSuf(nodeRatio, nodeRatio.indexOf("=") + 1);
                if ((isPass && donePassList.size() + 1 >= Integer.parseInt(passCount))
                    || (!isPass && doneRejectList.size() + 1 > allNum - Integer.parseInt(passCount))) {
                    return removeRestList(restList);
                }
            } else if (CooperateType.isVoteSignRejectCount(nodeRatio)) {
                // 判断是否是票签中的固定驳回人数，如果是则判断是否达到该人数
                String rejectCount = StrUtil.subSuf(nodeRatio, nodeRatio.indexOf("=") + 1);
                if ((!isPass && doneRejectList.size() + 1 >= Integer.parseInt(rejectCount))
                || (isPass && donePassList.size() + 1 > allNum - Integer.parseInt(rejectCount))) {
                    return removeRestList(restList);
                }
            } else if ((!isPass && rejectRatio.compareTo(MathUtil.ONE_HUNDRED.subtract(new BigDecimal(nodeRatio))) > 0)
                || (isPass && passRatio.compareTo(new BigDecimal(nodeRatio)) >= 0)) {
                // 提前不满足通过率或者满足通过率，删除剩余办理人，流程正常流程流转
                return removeRestList(restList);
            }
        }

        // 当只剩一位待办用户时，由当前用户决定走向
        if (todoList.size() == 1) {
            return false;
        }

        // 添加历史任务
        FlowHisTask hisTask = FlowEngine.hisTaskService().setSignHisTask(task, flowParams, nodeRatio, isPass);
        FlowEngine.hisTaskService().save(hisTask);

        // 删掉待办用户
        FlowEngine.userService().removeById(todoUser.getId());
        return true;
    }

    /**
     * 删除剩余办理人
     * @param restList 待办用户列表
     * @return  boolean
     */
    private static boolean removeRestList(List<FlowUser> restList) {
        if (CollUtil.isNotEmpty(restList)) {
            FlowEngine.userService().removeByIds(StreamUtils.toList(restList, FlowUser::getId));
        }
        return false;
    }

    /**
     * 判断并行网关和包容网关节点只剩一个前置代办任务，才能生成新的代办任务
     *
     * @param pathWayData 办理过程中途径数据
     * @param instance    实例
     */
    private void isGenerateNewTask(PathWayData pathWayData, FlowInstance instance, List<FlowNode> nextNodes) {
        if (SkipType.isReject(pathWayData.getSkipType())) {
            return;
        }

        DefJson defJson = JsonUtils.parseObject(instance.getDefJson(), DefJson.class);
        Map<String, NodeJson> nodeJsonMap = StreamUtils.toMap(defJson.getNodeList(), NodeJson::getNodeCode, node -> node);
        // 途径节点中的并行/包容网关，只剩一个前置待办任务时才能生成新的代办任务
        List<FlowNode> parallelOrInclusiveList = StreamUtils.filter(pathWayData.getPathWayNodes(),
            t -> NodeType.isGateWayParallel(t.getNodeType()) || NodeType.isGateWayInclusive(t.getNodeType()));
        if (CollUtil.isEmpty(parallelOrInclusiveList)) {
            return;
        }

        List<FlowNode> previousNodeList = FlowEngine.nodeService().previousNodeList(instance.getDefinitionId()
            , parallelOrInclusiveList.get(parallelOrInclusiveList.size() - 1).getNodeCode());
        // 前置节点中处于待办状态的数量
        long toDoCount = previousNodeList.stream()
            .map(FlowNode::getNodeCode)
            .map(nodeJsonMap::get)
            .filter(Objects::nonNull)
            .filter(nodeJson -> ChartStatus.isToDo(nodeJson.getStatus()))
            .count();
        // 并行网关和包容网关还有多个前置待办任务，不可生成新的代办任务
        if (toDoCount <= 1) {
            return;
        }

        nextNodes.clear();
        String gatewayCode = parallelOrInclusiveList.get(0).getNodeCode();
        // 途径节点保留首个网关自身，其后的全部移除
        List<FlowNode> pathWayNodes = pathWayData.getPathWayNodes();
        for (int i = 0; i < pathWayNodes.size(); i++) {
            if (pathWayNodes.get(i).getNodeCode().equals(gatewayCode)) {
                pathWayNodes.subList(i + 1, pathWayNodes.size()).clear();
                break;
            }
        }
        // 途径连线从首个网关出发的（含）全部移除
        List<FlowSkip> pathWaySkips = pathWayData.getPathWaySkips();
        for (int i = 0; i < pathWaySkips.size(); i++) {
            if (pathWaySkips.get(i).getNowNodeCode().equals(gatewayCode)) {
                pathWaySkips.subList(i, pathWaySkips.size()).clear();
                break;
            }
        }
    }

    /**
     * 判断当前处理人是否有权限处理
     *
     * @param task                   当前任务（任务id）
     * @param flowParams:包含流程相关参数的对象
     */
    private void checkAuth(FlowTask task, FlowParams flowParams) {
        if (flowParams.isIgnore()) {
            return;
        }
        // 查询审批人和转办人
        List<String> permissions = StreamUtils.toList(task.getUserList(), FlowUser::getProcessedBy);
        // 当前办理人拥有的权限和设计时候填的权限集合是否有交集，有说明有权限办理
        AssertUtil.isTrue(CollUtil.isNotEmpty(permissions) && (CollUtil.isEmpty(flowParams.getPermissionFlag())
            || !CollUtil.containsAny(flowParams.getPermissionFlag(), permissions)), ExceptionCons.NULL_ROLE_NODE);
    }


    /**
     * 一票否决（谨慎使用），如果退回，退回指向节点后还存在其他正在执行的待办任务，转历史任务，状态都为退回,重走流程。
     *
     * @param task         当前任务
     * @param nextNodeCode 下一个节点编码
     * @param flowCombine  流程数据集合
     */
    private void oneVoteVeto(FlowTask task, String nextNodeCode, FlowCombine flowCombine) {
        // 一票否决（谨慎使用），如果退回，退回指向节点后还存在其他正在执行的待办任务，转历史任务，状态失效,重走流程。
        List<FlowTask> tasks = list(new FlowTask().setInstanceId(task.getInstanceId()));
        // 属于退回指向节点的后置未完成的任务
        List<FlowTask> noDoneTasks = new ArrayList<>();
        List<FlowNode> suffixNodeList = FlowEngine.nodeService().suffixNodeList(nextNodeCode, flowCombine);
        List<String> suffixCodes = StreamUtils.toList(suffixNodeList, FlowNode::getNodeCode);
        for (FlowTask flowTask : tasks) {
            if (suffixCodes.contains(flowTask.getNodeCode())) {
                noDoneTasks.add(flowTask);
            }
        }
        if (CollUtil.isNotEmpty(noDoneTasks)) {
            removeAndUser(noDoneTasks);
        }
    }


    /**
     * 处理未完成的任务，当流程完成，还存在待办任务未完成，转历史任务，状态完成。
     *
     * @param instance 流程实例
     */
    private void handUndoneTask(FlowInstance instance) {
        if (NodeType.isEnd(instance.getNodeType())) {
            List<FlowTask> taskList = list(new FlowTask().setInstanceId(instance.getId()));
            if (CollUtil.isNotEmpty(taskList)) {
                removeAndUser(taskList);
            }
        }
    }

    /**
     * 更新流程信息
     *
     * @param task       当前任务
     * @param instance   流程实例
     * @param addTasks   新增待办任务
     * @param flowParams 包含流程相关参数的对象
     * @param nextNodes  下一个节点集合
     */
    private void updateFlowInfo(FlowTask task, FlowInstance instance, List<FlowTask> addTasks, FlowParams flowParams
        , List<FlowNode> nextNodes) {
        // 设置流程历史任务信息
        FlowHisTask insHis = FlowEngine.hisTaskService().setSkipInsHis(task, nextNodes, flowParams);
        FlowEngine.hisTaskService().save(insHis);
        removeAndUser(Collections.singletonList(task));
        // 待办任务设置处理人
        List<FlowUser> users = FlowEngine.userService().taskAddUsers(addTasks);

        // 设置任务完成后的实例相关信息
        setInsFinishInfo(instance, addTasks, flowParams);
        if (CollUtil.isNotEmpty(addTasks)) {
            saveBatch(addTasks);
        }
        FlowEngine.insService().updateById(instance);
        // 保存下一个待办任务的权限人
        FlowEngine.userService().saveBatch(users);
    }

    private boolean judgeActivityStatus(FlowDefinition definition, FlowInstance instance) {
        return ActivityStatus.isActivity(definition.getActivityStatus())
            && ActivityStatus.isActivity(instance.getActivityStatus());
    }


    public FlowDto load(Long taskId, FlowParams flowParams) {
        R r = getAndCheck(taskId);

        FlowDto flowDto = new FlowDto();
        flowDto.setData(r.instance().getVariableMap().get(FlowCons.FORM_DATA));

        return flowDto;
    }

    public FlowDto hisLoad(Long hisTaskId, FlowParams flowParams) {
        FlowHisTask hisTask = FlowEngine.hisTaskService().getById(hisTaskId);
        AssertUtil.isNull(hisTask, ExceptionCons.NOT_FOUND_FLOW_TASK);

        FlowDefinition definition = FlowEngine.defService().getById(hisTask.getDefinitionId());
        AssertUtil.isNull(definition, ExceptionCons.NOT_FOUNT_DEF);

        FlowNode nowNode = CollUtil.getFirst(FlowEngine.nodeService()
            .getMapper().getByNodeCodes(Collections.singletonList(hisTask.getNodeCode()), hisTask.getDefinitionId()));
        AssertUtil.isNull(nowNode, ExceptionCons.LOST_CUR_NODE);

        FlowDto flowDto = new FlowDto();
        flowDto.setData(hisTask.getVariableMap().get(FlowCons.FORM_DATA));

        return flowDto;
    }

    @Override
    public FlowTaskMapper getMapper() {
        return SpringUtils.getBean(FlowTaskMapper.class);
    }
}
