package org.dromara.workflow.de.rs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.system.api.OssService;
import org.dromara.system.api.domain.OssDTO;
import org.dromara.workflow.de.constants.DeP1Constants;
import org.dromara.workflow.de.rs.domain.bo.DeRsConfirmBo;
import org.dromara.workflow.de.rs.domain.bo.DeRsCrossCheckBo;
import org.dromara.workflow.de.rs.domain.bo.DeRsDecideBo;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.mapper.FlwHisTaskMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RS 域服务实现。
 *
 * <p>RS-05 / RS-07 落点：flow_his_task.message（审核者决定与确认结果）。
 * RS-06 落点：flow_his_task.ext（交叉核验结果 JSON）。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeRsServiceImpl implements IDeRsService {

    private final FlwTaskMapper flwTaskMapper;
    private final FlwHisTaskMapper flwHisTaskMapper;
    private final OssService ossService;

    @Override
    public Long decide(DeRsDecideBo bo) {
        FlowTask task = flwTaskMapper.selectById(bo.getTaskId());
        if (task == null) {
            throw new ServiceException("任务不存在: id=" + bo.getTaskId());
        }
        // 写 flow_his_task.message —— 仅在 RS 决议时第一次写入历史（结束才留）
        FlowHisTask his = new FlowHisTask();
        his.setTaskId(task.getId());
        his.setInstanceId(task.getInstanceId());
        his.setDefinitionId(task.getDefinitionId());
        his.setNodeCode(task.getNodeCode());
        his.setNodeName(task.getNodeName());
        his.setNodeType(task.getNodeType());
        his.setFlowStatus(DeP1Constants.FLOW_STATUS_FINISHED);
        // skip_type：DDL NOT NULL 无默认（ry_workflow.sql:131），不设即 SQLException
        his.setSkipType(mapDecisionSkipType(bo.getDecision()));
        his.setMessage("[RS-05] " + bo.getDecision() + " | " + bo.getComment());
        flwHisTaskMapper.insert(his);
        log.info("[rs.decide] taskId={} decision={}", bo.getTaskId(), bo.getDecision());
        return bo.getTaskId();
    }

    @Override
    public Boolean crossCheck(DeRsCrossCheckBo bo) {
        FlowTask task = flwTaskMapper.selectById(bo.getTaskId());
        if (task == null) {
            throw new ServiceException("任务不存在: id=" + bo.getTaskId());
        }
        // RS-06（29 号 :593）：回执必须指向真实外部对象及当前状态；
        // 失败 / 不确定 / 部分成功均可表达。逐个期望 ossId 回查 sys_oss（OssService），
        // 全部解析为真实对象（非空 URL）才通过；任一缺失 = 部分成功/失败 → false；
        // expected 为空 = 无提交产物可核验（不确定）→ false。
        if (bo.getExpected() == null || bo.getExpected().isEmpty()) {
            log.info("[rs.crossCheck] taskId={} 无期望产物，核验不确定 → false", bo.getTaskId());
            return false;
        }
        String joined = String.join(",", bo.getExpected().keySet());
        List<OssDTO> actual = ossService.selectByIds(joined);
        Set<Long> resolved = new HashSet<>();
        if (actual != null) {
            for (OssDTO dto : actual) {
                if (dto.getOssId() != null && dto.getUrl() != null && !dto.getUrl().isBlank()) {
                    resolved.add(dto.getOssId());
                }
            }
        }
        int matched = 0;
        for (String ossId : bo.getExpected().keySet()) {
            try {
                if (resolved.contains(Long.parseLong(ossId))) {
                    matched++;
                }
            } catch (NumberFormatException e) {
                // 非数字 ossId 恒不可解析，计入未命中
            }
        }
        boolean passed = matched == bo.getExpected().size();
        log.info("[rs.crossCheck] taskId={} expected={} matched={} passed={}",
            bo.getTaskId(), bo.getExpected().size(), matched, passed);
        return passed;
    }

    @Override
    public Long confirm(DeRsConfirmBo bo) {
        FlowTask task = flwTaskMapper.selectById(bo.getTaskId());
        if (task == null) {
            throw new ServiceException("任务不存在: id=" + bo.getTaskId());
        }
        FlowHisTask his = new FlowHisTask();
        his.setTaskId(task.getId());
        his.setInstanceId(task.getInstanceId());
        his.setDefinitionId(task.getDefinitionId());
        his.setNodeCode(task.getNodeCode());
        his.setNodeName(task.getNodeName());
        his.setNodeType(task.getNodeType());
        his.setFlowStatus(DeP1Constants.FLOW_STATUS_FINISHED);
        // skip_type：DDL NOT NULL 无默认（ry_workflow.sql:131），不设即 SQLException
        his.setSkipType(mapActionSkipType(bo.getAction()));
        his.setMessage("[RS-07] " + bo.getAction() + " | " + bo.getReason());
        flwHisTaskMapper.insert(his);
        log.info("[rs.confirm] taskId={} action={}", bo.getTaskId(), bo.getAction());
        return bo.getTaskId();
    }

    /**
     * RS-05 decision → flow_his_task.skip_type（DDL 值域 PASS/REJECT/NONE，ry_workflow.sql:131）：
     * accept→PASS · reject→REJECT · require-more / 未知→NONE（无动作）。
     */
    private String mapDecisionSkipType(String decision) {
        if ("accept".equals(decision)) {
            return SkipType.PASS.getKey();
        }
        if ("reject".equals(decision)) {
            return SkipType.REJECT.getKey();
        }
        return SkipType.NONE.getKey();
    }

    /**
     * RS-07 action → flow_his_task.skip_type（DDL 值域 PASS/REJECT/NONE）：
     * confirm→PASS · rollback/compensate→REJECT · permission-denied/cancel / 未知→NONE（无动作）。
     */
    private String mapActionSkipType(String action) {
        if ("confirm".equals(action)) {
            return SkipType.PASS.getKey();
        }
        if ("rollback".equals(action) || "compensate".equals(action)) {
            return SkipType.REJECT.getKey();
        }
        return SkipType.NONE.getKey();
    }
}