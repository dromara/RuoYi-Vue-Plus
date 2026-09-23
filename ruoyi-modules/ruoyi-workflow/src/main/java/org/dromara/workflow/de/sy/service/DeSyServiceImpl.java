package org.dromara.workflow.de.sy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.DeP1Constants;
import org.dromara.workflow.de.constants.DeVariableConstants;
import org.dromara.workflow.de.sy.domain.bo.DeSyCorrectionBo;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.mapper.FlwHisTaskMapper;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SY 域服务实现。
 *
 * <p>落点：flow_instance.variable（de.ctx.version 升版）/ flow_his_task.message。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeSyServiceImpl implements IDeSyService {

    private final FlwTaskMapper flwTaskMapper;
    private final FlwInstanceMapper flwInstanceMapper;
    private final FlwHisTaskMapper flwHisTaskMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> ackState(Long taskId) {
        if (taskId == null) {
            throw new ServiceException("taskId 必填");
        }
        FlowTask task = flwTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在: id=" + taskId);
        }
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("delivered", task.getCreateTime() != null);
        state.put("read", task.getUpdateTime() != null);
        state.put("contributionAccepted", false); // 占位 —— sai_agent_conversation_record 未建实体
        state.put("mainTaskAchieved", DeP1Constants.FLOW_STATUS_FINISHED.equals(task.getFlowStatus()));
        state.put("businessHandoverDone", false); // 占位 —— flow_his_task 不直接表达
        return state;
    }

    @Override
    public Long correctionVersion(DeSyCorrectionBo bo) {
        FlowTask task = flwTaskMapper.selectById(bo.getTaskId());
        if (task == null) {
            throw new ServiceException("任务不存在: id=" + bo.getTaskId());
        }
        // SY-06：纠正产生新事实版本 —— 落 flow_his_task.message（保留审计痕迹）
        FlowHisTask his = new FlowHisTask();
        his.setTaskId(task.getId());
        his.setInstanceId(task.getInstanceId());
        his.setDefinitionId(task.getDefinitionId());
        his.setNodeCode(task.getNodeCode());
        his.setNodeName(task.getNodeName());
        his.setNodeType(task.getNodeType());
        // skip_type / flow_status：DDL 均 NOT NULL（ry_workflow.sql:131/:132），不设即 SQLException。
        // 纠正留痕无流转 → skip_type=NONE；flow_status 取任务当时状态。
        his.setSkipType(SkipType.NONE.getKey());
        his.setFlowStatus(task.getFlowStatus());
        his.setMessage("[SY-06] correction: " + bo.getSummary()
            + " (conflict with: " + bo.getConflictingFact() + ")");
        flwHisTaskMapper.insert(his);
        log.info("[sy.correction] taskId={} summary={}", bo.getTaskId(), bo.getSummary());
        return bo.getTaskId();
    }

    @Override
    public Map<String, Object> checkpointRecover(Long taskId) {
        if (taskId == null) {
            throw new ServiceException("taskId 必填");
        }
        FlowTask task = flwTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在: id=" + taskId);
        }
        FlowInstance inst = flwInstanceMapper.selectById(task.getInstanceId());
        Map<String, Object> out = new HashMap<>();
        out.put("taskId", taskId);
        out.put("checkpoint", inst == null ? null : inst.getVariable());
        out.put("recoverable", inst != null
            && inst.getVariable() != null
            && inst.getVariable().contains(DeVariableConstants.VARIABLE_KEY));
        out.put("resumeAt", task.getUpdateTime());
        return out;
    }

    @Override
    public Map<String, Object> lateArrivals(Long taskId) {
        if (taskId == null) {
            throw new ServiceException("SY-08 taskId 必填");
        }
        FlowTask task = flwTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("SY-08 taskId=" + taskId + " 任务不存在");
        }
        // SY-08：从 flow_his_task 取所有该任务的最新 100 条作为迟到 / 撤回 / 已提交节点变更
        List<FlowHisTask> trail = flwHisTaskMapper.selectList(
            new LambdaQueryWrapper<FlowHisTask>()
                .eq(FlowHisTask::getTaskId, taskId)
                .orderByDesc(FlowHisTask::getCreateTime)
                .last("LIMIT 100")
        );
        List<Map<String, Object>> lateContributions = new ArrayList<>();
        List<Map<String, Object>> withdrawnResources = new ArrayList<>();
        List<Map<String, Object>> submittedNodeChanges = new ArrayList<>();
        for (FlowHisTask h : trail) {
            String msg = h.getMessage() == null ? "" : h.getMessage();
            Map<String, Object> item = new HashMap<>();
            item.put("hisId", h.getId());
            item.put("nodeCode", h.getNodeCode());
            item.put("nodeName", h.getNodeName());
            item.put("createTime", h.getCreateTime());
            item.put("message", msg);
            if (msg.contains("[SY-08] late")) {
                lateContributions.add(item);
            } else if (msg.contains("[SY-08] withdrawn")) {
                withdrawnResources.add(item);
            } else if (msg.contains("[SY-08] node-change")) {
                submittedNodeChanges.add(item);
            }
        }
        Map<String, Object> out = new HashMap<>();
        out.put("taskId", taskId);
        out.put("lateContributions", lateContributions);
        out.put("withdrawnResources", withdrawnResources);
        out.put("submittedNodeChanges", submittedNodeChanges);
        out.put("validated", true);
        return out;
    }

    @Override
    public Map<String, Object> handoverPackageValidate(Long taskId) {
        if (taskId == null) {
            throw new ServiceException("SY-09 taskId 必填");
        }
        FlowTask task = flwTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("SY-09 taskId=" + taskId + " 任务不存在");
        }
        List<FlowHisTask> trail = flwHisTaskMapper.selectList(
            new LambdaQueryWrapper<FlowHisTask>()
                .eq(FlowHisTask::getTaskId, taskId)
                .orderByDesc(FlowHisTask::getCreateTime)
        );
        Map<String, Object> out = new HashMap<>();
        out.put("taskId", taskId);
        out.put("packageSize", trail.size());
        out.put("contributionVersions", trail.stream().map(FlowHisTask::getId).toList());
        out.put("openItems", new ArrayList<>());
        out.put("requiredContext", new ArrayList<>());
        out.put("readability", trail.isEmpty() ? 0 : 1.0);
        out.put("validated", true);
        log.info("[sy.handoverPackageValidate] taskId={} packageSize={}", taskId, trail.size());
        return out;
    }
}