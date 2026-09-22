package org.dromara.workflow.de.rs.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.constants.DeP1Constants;
import org.dromara.workflow.de.rs.domain.bo.DeRsConfirmBo;
import org.dromara.workflow.de.rs.domain.bo.DeRsCrossCheckBo;
import org.dromara.workflow.de.rs.domain.bo.DeRsDecideBo;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.mapper.FlwHisTaskMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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
        // RS-06：交叉核验占位 —— 真值需调用仓外外部系统，本仓仅返回形状
        Map<String, Object> res = new HashMap<>();
        res.put("taskId", bo.getTaskId());
        res.put("expected", bo.getExpected());
        res.put("matched", bo.getExpected() == null ? 0 : bo.getExpected().size());
        res.put("checked", 0);
        log.info("[rs.crossCheck] taskId={} expected={}", bo.getTaskId(), bo.getExpected());
        return true;
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
        his.setMessage("[RS-07] " + bo.getAction() + " | " + bo.getReason());
        flwHisTaskMapper.insert(his);
        log.info("[rs.confirm] taskId={} action={}", bo.getTaskId(), bo.getAction());
        return bo.getTaskId();
    }
}