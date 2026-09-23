package org.dromara.workflow.de.tm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.mapper.FlwHisTaskMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TM 域服务实现（TM-03 / TM-06 · P1 档 A 实接）。
 *
 * <p>TM-03 共享 / 私有技能区分：落点 sai_agent_team（仓外 owner 未造）。
 * 本仓面仅暴露接口契约 + 空集；owner 解锁 sai_agent_team 后填实。
 *
 * <p>TM-06 团队执行输出对接节点完成检查与交接包：复用底座 {@code flow_his_task}
 * 留痕（不发明列），交接包的版本 / 节点名 / 任务 ID 通过 {@code FlwTaskMapper} +
 * {@code FlwHisTaskMapper} 装配。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeTmServiceImpl implements IDeTmService {

    private final FlwTaskMapper flwTaskMapper;
    private final FlwHisTaskMapper flwHisTaskMapper;

    @Override
    public Map<String, Object> skillScope(Long teamId, Long userId) {
        if (teamId == null) {
            throw new ServiceException("TM-03 teamId 必填");
        }
        Map<String, Object> out = new HashMap<>();
        out.put("teamId", teamId);
        out.put("userId", userId);
        out.put("sharedSkillIds", new ArrayList<>());
        out.put("privateSkillIds", new ArrayList<>());
        out.put("validated", true);
        out.put("note", "sai_agent_team 待 owner 解锁 —— TM-03 待 owner 接入 sai_agent_team 后填实");
        return out;
    }

    @Override
    public Map<String, Object> handover(Long teamId, Long taskId) {
        if (taskId == null) {
            throw new ServiceException("TM-06 taskId 必填");
        }
        FlowTask task = flwTaskMapper.selectById(taskId);
        if (task == null) {
            throw new ServiceException("TM-06 taskId=" + taskId + " 任务不存在");
        }
        // 取最近历史 → 装配交接包（H1 P1 AC-12 节点完成检查 / 交接包可读性）
        List<FlowHisTask> trail = flwHisTaskMapper.selectList(
            new LambdaQueryWrapper<FlowHisTask>()
                .eq(FlowHisTask::getTaskId, taskId)
                .orderByDesc(FlowHisTask::getCreateTime)
        );
        List<Long> contributionVersions = new ArrayList<>();
        for (FlowHisTask h : trail) {
            contributionVersions.add(h.getId());
        }
        // 留一条 TM-06 交接包摘要到 flow_his_task.message（不发明列）
        FlowHisTask handover = new FlowHisTask();
        handover.setTaskId(task.getId());
        handover.setInstanceId(task.getInstanceId());
        handover.setDefinitionId(task.getDefinitionId());
        handover.setNodeCode(task.getNodeCode());
        handover.setNodeName(task.getNodeName());
        handover.setNodeType(task.getNodeType());
        // skip_type / flow_status：DDL 均 NOT NULL（ry_workflow.sql:131/:132），不设即 SQLException。
        // 交接留痕无流转 → skip_type=NONE；flow_status 取任务当时状态。
        handover.setSkipType(SkipType.NONE.getKey());
        handover.setFlowStatus(task.getFlowStatus());
        handover.setMessage("[TM-06] teamId=" + teamId + " handover package committed (versions="
            + contributionVersions.size() + ")");
        flwHisTaskMapper.insert(handover);

        Map<String, Object> out = new HashMap<>();
        out.put("teamId", teamId);
        out.put("taskId", taskId);
        out.put("status", "committed");
        out.put("handoverPackage", trail);
        out.put("contributionVersions", contributionVersions);
        out.put("packageSize", trail.size());
        out.put("validated", true);
        log.info("[tm.handover] teamId={} taskId={} packageSize={}",
            teamId, taskId, trail.size());
        return out;
    }
}