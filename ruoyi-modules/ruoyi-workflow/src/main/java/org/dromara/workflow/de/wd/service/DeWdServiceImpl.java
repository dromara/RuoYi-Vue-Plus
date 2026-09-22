package org.dromara.workflow.de.wd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.DeP1Constants;
import org.dromara.workflow.de.constants.DeVariableConstants;
import org.dromara.workflow.de.domain.SaiAgent;
import org.dromara.workflow.de.domain.SaiUserAgent;
import org.dromara.workflow.de.mapper.SaiAgentMapper;
import org.dromara.workflow.de.mapper.SaiUserAgentMapper;
import org.dromara.workflow.de.wd.domain.bo.DeWdTaskFilterBo;
import org.dromara.workflow.de.wd.domain.vo.DeWdTaskListVo;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.workflow.mapper.FlwUserMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 工作台 WD 域服务实现。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeWdServiceImpl implements IDeWdService {

    private final FlwTaskMapper flwTaskMapper;
    private final FlwInstanceMapper flwInstanceMapper;
    private final FlwUserMapper flwUserMapper;
    private final SaiAgentMapper saiAgentMapper;
    private final SaiUserAgentMapper saiUserAgentMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<DeWdTaskListVo> listInProgress(Long userId) {
        if (userId == null) {
            throw new ServiceException("userId 必填");
        }
        List<FlowTask> tasks = flwTaskMapper.selectList(
            new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getNodeType, DeP1Constants.NODE_TYPE_BETWEEN)
                .orderByDesc(FlowTask::getUpdateTime)
        );
        return filterAndProject(tasks, userId, false);
    }

    @Override
    public List<DeWdTaskListVo> filterTasks(DeWdTaskFilterBo bo) {
        if (bo.getUserId() == null) {
            throw new ServiceException("userId 必填");
        }
        LambdaQueryWrapper<FlowTask> qw = new LambdaQueryWrapper<FlowTask>()
            .eq(FlowTask::getNodeType, DeP1Constants.NODE_TYPE_BETWEEN);
        if (bo.getFlowIds() != null && !bo.getFlowIds().isEmpty()) {
            qw.in(FlowTask::getDefinitionId, bo.getFlowIds());
        }
        if (bo.getDueBefore() != null && !bo.getDueBefore().isBlank()) {
            // 仅作粗筛（避免改底座 task 表结构）：create_time 早于 dueBefore 的全部展示
            qw.le(FlowTask::getCreateTime, new java.util.Date());
        }
        List<FlowTask> tasks = flwTaskMapper.selectList(qw.orderByDesc(FlowTask::getUpdateTime));
        List<DeWdTaskListVo> base = filterAndProject(tasks, bo.getUserId(), false);

        if (bo.getProjectKey() != null && !bo.getProjectKey().isBlank()) {
            base.removeIf(vo -> vo.getInstanceId() == null
                || !String.valueOf(vo.getInstanceId()).contains(bo.getProjectKey()));
        }
        if (bo.getAgentId() != null) {
            base.removeIf(vo -> vo.getAgentStep() == null
                || !vo.getAgentStep().equalsIgnoreCase(bo.getAgentId().toString()));
        }
        if (bo.getAssigneeId() != null && !bo.getAssigneeId().equals(bo.getUserId())) {
            base.clear(); // 当前用户不是责任人的不展示
        }
        return base;
    }

    @Override
    public List<DeWdTaskListVo> listActionRequired(Long userId) {
        if (userId == null) {
            throw new ServiceException("userId 必填");
        }
        List<FlowTask> tasks = flwTaskMapper.selectList(
            new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getNodeType, DeP1Constants.NODE_TYPE_BETWEEN)
                .orderByDesc(FlowTask::getUpdateTime)
        );
        List<DeWdTaskListVo> all = filterAndProject(tasks, userId, true);
        return all;
    }

    /**
     * 通用：把 flow_task 列表投射为 VO；根据 userId 找 flow_user(type='1') 的 processed_by
     * 是否匹配，并解析 flow_instance.variable 中的 de.ctx.task.step。
     */
    private List<DeWdTaskListVo> filterAndProject(List<FlowTask> tasks, Long userId, boolean actionOnly) {
        if (tasks == null || tasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<DeWdTaskListVo> out = new ArrayList<>();
        for (FlowTask task : tasks) {
            FlowInstance inst = flwInstanceMapper.selectById(task.getInstanceId());
            if (inst == null) {
                continue;
            }
            // 责任归属：flow_user(type='1', associated=taskId)
            FlowUser u = flwUserMapper.selectOne(
                new LambdaQueryWrapper<FlowUser>()
                    .eq(FlowUser::getAssociated, task.getId())
                    .eq(FlowUser::getType, DeP1Constants.USER_TYPE_APPROVER)
            );
            if (u == null || !String.valueOf(userId).equals(u.getProcessedBy())) {
                if (!actionOnly) {
                    continue;
                }
            }
            // 解析 de.ctx.task.step
            String agentStep = null;
            if (inst.getVariable() != null && !inst.getVariable().isBlank()) {
                try {
                    JsonNode root = objectMapper.readTree(inst.getVariable());
                    JsonNode deCtx = root.get(DeVariableConstants.VARIABLE_KEY);
                    if (deCtx != null) {
                        JsonNode step = deCtx.get(DeVariableConstants.KEY_TASK_STEP);
                        if (step != null) {
                            agentStep = step.asText();
                        }
                    }
                } catch (Exception e) {
                    // 2026-09-19 H1 P1 #11: WD 读路径 JSON 解析失败仅 warn ⇒ agentStep=null 后
                    // 走 categorize 归类为 'exception'，掩盖了真实失败。
                    // 改为显式抛错，与 updateVariable 入口校验策略对齐（写时拒收 + 读时也拒收）。
                    log.error("[wd] de.ctx 解析失败 instanceId={}: {}", inst.getId(), e.getMessage());
                    throw new ServiceException(
                        "instance.variable 解析失败 instanceId=" + inst.getId() + ": " + e.getMessage());
                }
            }
            DeWdTaskListVo vo = new DeWdTaskListVo();
            vo.setTaskId(task.getId());
            vo.setInstanceId(inst.getId());
            vo.setNodeName(task.getNodeName());
            vo.setFlowName(inst.getFlowName());
            vo.setAgentStep(agentStep);
            vo.setFlowStatus(task.getFlowStatus());
            vo.setCreateTime(task.getCreateTime());
            vo.setActionCategory(categorize(task, agentStep));
            out.add(vo);
        }
        return out;
    }

    /**
     * 异常 / 求助 / 审核 / 资源失效 归类（简易版；底座无对应列，逻辑层做映射）。
     */
    private String categorize(FlowTask task, String agentStep) {
        if (agentStep != null && DeVariableConstants.STEP_FAILED.equals(agentStep)) {
            return "exception";
        }
        if (task.getFlowStatus() != null && task.getFlowStatus().startsWith("reject")) {
            return "help";
        }
        if (task.getFormPath() == null || task.getFormPath().isBlank()) {
            return "review";
        }
        return "normal";
    }
}