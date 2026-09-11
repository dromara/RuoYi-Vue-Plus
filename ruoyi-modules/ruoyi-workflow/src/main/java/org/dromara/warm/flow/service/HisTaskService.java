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
import org.dromara.warm.flow.entity.*;

import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.dromara.common.core.utils.StreamUtils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ArrayUtil;

import org.dromara.warm.flow.FlowEngine;
import org.dromara.warm.flow.dto.FlowParams;
import org.dromara.warm.flow.entity.FlowHisTask;
import org.dromara.warm.flow.entity.FlowNode;
import org.dromara.warm.flow.entity.FlowTask;
import org.dromara.warm.flow.entity.FlowUser;
import org.dromara.warm.flow.enums.CooperateType;
import org.dromara.warm.flow.enums.FlowStatus;
import org.dromara.warm.flow.enums.SkipType;
import org.dromara.warm.flow.service.WarmServiceImpl;
import org.dromara.warm.flow.utils.*;

import java.util.ArrayList;
import java.util.List;





import org.dromara.common.core.utils.SpringUtils;
import org.springframework.stereotype.Service;
import org.dromara.warm.flow.mapper.FlowHisTaskMapper;




/**
 * 历史任务记录Service业务层处理
 *
 * @author warm
 * @since 2023-03-29
 */
@Service
public class HisTaskService extends WarmServiceImpl<FlowHisTask> {
    public List<FlowHisTask> listByTaskId(Long taskId) {
        return list(new FlowHisTask().setTaskId(taskId));
    }

    public List<FlowHisTask> listByTaskIdAndCooperateTypes(Long taskId, Integer... cooperateTypes) {
        if (ArrayUtil.isEmpty(cooperateTypes)) {
            return listByTaskId(taskId);
        }
        if (cooperateTypes.length == 1) {
            return list(new FlowHisTask().setTaskId(taskId).setCooperateType(cooperateTypes[0]));
        }
        return getMapper().listByTaskIdAndCooperateTypes(taskId, cooperateTypes);
    }

    public List<FlowHisTask> getByInsAndNodeCodes(Long instanceId, List<String> nodeCodes) {
        return getMapper().getByInsAndNodeCodes(instanceId, nodeCodes);
    }

    public boolean deleteByInsIds(List<Long> instanceIds) {
        return SqlHelper.retBool(getMapper().deleteByInsIds(instanceIds));
    }

    public FlowHisTask setSkipInsHis(FlowTask task, List<FlowNode> nextNodes, FlowParams flowParams) {
        String flowStatus = getFlowStatus(flowParams);
        return setSkipHis(task, nextNodes, flowParams, flowStatus);
    }

    public List<FlowHisTask> setSkipHisList(List<FlowTask> taskList, List<FlowNode> nextNodes, FlowParams flowParams) {
        String flowStatus = getFlowStatus(flowParams);
        List<FlowHisTask> hisTasks = new ArrayList<>();
        for (FlowTask task : taskList) {
            FlowHisTask hisTask = setSkipHis(task, nextNodes, flowParams, flowStatus);
            hisTasks.add(hisTask);
        }
        return hisTasks;
    }

    public FlowHisTask setSkipHisTask(FlowTask task, FlowNode nextNode, FlowParams flowParams) {
        String flowStatus = getFlowStatus(flowParams);
        return setSkipHis(task, CollUtil.toList(nextNode), flowParams, flowStatus);
    }


    public FlowHisTask setCooperateHis(FlowTask task, FlowParams flowParams
        , List<String> collaborators) {
        String flowStatus = getFlowStatus(flowParams);
        FlowHisTask hisTask = new FlowHisTask()
            .setTaskId(task.getId())
            .setInstanceId(task.getInstanceId())
            .setCooperateType(ObjectUtil.defaultIfNull(flowParams.getCooperateType(), CooperateType.APPROVAL.getKey()))
            .setCollaborator(StreamUtils.join(collaborators, c -> c))
            .setNodeCode(task.getNodeCode())
            .setNodeName(task.getNodeName())
            .setNodeType(task.getNodeType())
            .setDefinitionId(task.getDefinitionId())
            .setTargetNodeCode(task.getNodeCode())
            .setTargetNodeName(task.getNodeName())
            .setApprover(flowParams.getHandler())
            .setSkipType(flowParams.getSkipType())
            .setFlowStatus(StrUtil.emptyToDefault(flowStatus, FlowStatus.APPROVAL.getKey()))
            .setFormCustom(task.getFormCustom())
            .setFormPath(task.getFormPath())
            .setMessage(flowParams.getMessage())
            .setVariable(flowParams.getVariableStr())
            //业务详情添加至历史记录
            .setExt(flowParams.getHisTaskExt())
            .setCreateTime(task.getCreateTime());
        FlowEngine.dataFillHandler().idFill(hisTask);
        return hisTask;
    }

    public FlowHisTask notSkip(FlowTask task, FlowParams flowParams) {
        String flowStatus = getFlowStatus(flowParams);
        FlowHisTask hisTask = new FlowHisTask()
            .setTaskId(task.getId())
            .setInstanceId(task.getInstanceId())
            .setCooperateType(ObjectUtil.defaultIfNull(flowParams.getCooperateType(), CooperateType.APPROVAL.getKey()))
            .setNodeCode(task.getNodeCode())
            .setNodeName(task.getNodeName())
            .setNodeType(task.getNodeType())
            .setDefinitionId(task.getDefinitionId())
            .setTargetNodeCode(task.getNodeCode())
            .setTargetNodeName(task.getNodeName())
            .setApprover(flowParams.getHandler())
            .setSkipType(SkipType.NONE.getKey())
            .setFlowStatus(flowStatus)
            .setFormCustom(task.getFormCustom())
            .setFormPath(task.getFormPath())
            .setMessage(flowParams.getMessage())
            .setVariable(flowParams.getVariableStr())
            //业务详情添加至历史记录
            .setExt(flowParams.getHisTaskExt())
            .setCreateTime(task.getCreateTime());
        FlowEngine.dataFillHandler().idFill(hisTask);
        return hisTask;
    }

    public FlowHisTask setDeputeHisTask(FlowTask task, FlowParams flowParams, FlowUser entrustedUser) {
        String flowStatus = getFlowStatus(flowParams);
        FlowHisTask hisTask = new FlowHisTask()
            .setTaskId(task.getId())
            .setInstanceId(task.getInstanceId())
            .setCooperateType(CooperateType.DEPUTE.getKey())
            .setNodeCode(task.getNodeCode())
            .setNodeName(task.getNodeName())
            .setNodeType(task.getNodeType())
            .setDefinitionId(task.getDefinitionId())
            .setTargetNodeCode(task.getNodeCode())
            .setTargetNodeName(task.getNodeName())
            .setApprover(flowParams.getHandler())
            .setCollaborator(entrustedUser.getCreateBy())
            .setSkipType(flowParams.getSkipType())
            .setFlowStatus(StrUtil.isNotEmpty(flowStatus)
                ? flowStatus : SkipType.isReject(flowParams.getSkipType())
                ? FlowStatus.REJECT.getKey() : FlowStatus.PASS.getKey())
            .setFormCustom(task.getFormCustom())
            .setFormPath(task.getFormPath())
            .setMessage(flowParams.getMessage())
            .setVariable(flowParams.getVariableStr())
            //业务详情添加至历史记录
            .setExt(flowParams.getHisTaskExt())
            .setCreateTime(task.getCreateTime());
        FlowEngine.dataFillHandler().idFill(hisTask);
        return hisTask;
    }

    public FlowHisTask setSignHisTask(FlowTask task, FlowParams flowParams, String nodeRatio, boolean isPass) {
        String flowStatus = getFlowStatus(flowParams);
        FlowHisTask hisTask = new FlowHisTask()
            .setTaskId(task.getId())
            .setInstanceId(task.getInstanceId())
            .setCooperateType(CooperateType.isCountersign(nodeRatio)
                ? CooperateType.COUNTERSIGN.getKey() : CooperateType.VOTE.getKey())
            .setNodeCode(task.getNodeCode())
            .setNodeName(task.getNodeName())
            .setNodeType(task.getNodeType())
            .setDefinitionId(task.getDefinitionId())
            .setApprover(flowParams.getHandler())
            .setSkipType(isPass ? SkipType.PASS.getKey() : SkipType.REJECT.getKey())
            .setFlowStatus(StrUtil.isNotEmpty(flowStatus)
                ? flowStatus : isPass
                ? FlowStatus.PASS.getKey() : FlowStatus.REJECT.getKey())
            .setFormCustom(task.getFormCustom())
            .setFormPath(task.getFormPath())
            .setMessage(flowParams.getMessage())
            .setVariable(flowParams.getVariableStr())
            //业务详情添加至历史记录
            .setExt(flowParams.getHisTaskExt())
            .setCreateTime(task.getCreateTime());
        FlowEngine.dataFillHandler().idFill(hisTask);
        return hisTask;
    }

    public List<FlowHisTask> getByInsId(Long instanceId) {
        return FlowEngine.hisTaskService().list(new FlowHisTask().setInstanceId(instanceId));
    }

    private FlowHisTask setSkipHis(FlowTask task, List<FlowNode> nextNodes, FlowParams flowParams, String flowStatus) {
        FlowHisTask hisTask = new FlowHisTask()
            .setTaskId(task.getId())
            .setInstanceId(task.getInstanceId())
            .setCooperateType(ObjectUtil.defaultIfNull(flowParams.getCooperateType(), CooperateType.APPROVAL.getKey()))
            .setNodeCode(task.getNodeCode())
            .setNodeName(task.getNodeName())
            .setNodeType(task.getNodeType())
            .setDefinitionId(task.getDefinitionId())
            .setTargetNodeCode(StreamUtils.join(nextNodes, FlowNode::getNodeCode))
            .setTargetNodeName(StreamUtils.join(nextNodes, FlowNode::getNodeName))
            .setApprover(flowParams.getHandler())
            .setSkipType(flowParams.getSkipType())
            .setFlowStatus(StrUtil.isNotEmpty(flowStatus)
                ? flowStatus : SkipType.isReject(flowParams.getSkipType())
                ? FlowStatus.REJECT.getKey() : FlowStatus.PASS.getKey())
            .setFormCustom(task.getFormCustom())
            .setFormPath(task.getFormPath())
            .setMessage(flowParams.getMessage())
            .setVariable(flowParams.getVariableStr())
            //业务详情添加至历史记录
            .setExt(flowParams.getHisTaskExt())
            .setCreateTime(task.getCreateTime());
        FlowEngine.dataFillHandler().idFill(hisTask);
        return hisTask;
    }

    private String getFlowStatus(FlowParams flowParams) {
        return StrUtil.emptyToDefault(flowParams.getHisStatus(), flowParams.getFlowStatus());
    }

    @Override
    public FlowHisTaskMapper getMapper() {
        return SpringUtils.getBean(FlowHisTaskMapper.class);
    }
}