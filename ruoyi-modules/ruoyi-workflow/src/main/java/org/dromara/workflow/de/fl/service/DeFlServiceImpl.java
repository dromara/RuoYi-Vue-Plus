package org.dromara.workflow.de.fl.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.fl.domain.bo.DeFlExecuteBo;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.workflow.mapper.FlwHisTaskMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FL 域服务实现。
 *
 * <p>FL-01 / FL-04 / FL-05 落点：flow_node.ext（varchar 2000）。
 * FL-03 落点：flow_his_task.message（执行轨迹写入）。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeFlServiceImpl implements IDeFlService {

    private final FlwHisTaskMapper flwHisTaskMapper;

    @Override
    public List<Map<String, Object>> catalog(Long nodeId) {
        if (nodeId == null) {
            throw new ServiceException("nodeId 必填");
        }
        // 落点：flow_node.ext；本类仅返回目录占位
        List<Map<String, Object>> plans = new ArrayList<>();
        Map<String, Object> p = new HashMap<>();
        p.put("nodeId", nodeId);
        p.put("planId", 0L);
        p.put("planName", "(占位 —— 节点可执行方案目录)");
        p.put("version", "v0.0.0-placeholder");
        plans.add(p);
        return plans;
    }

    @Override
    public Long execute(DeFlExecuteBo bo) {
        if (bo.getStructure() == null || bo.getStructure().isBlank()) {
            throw new ServiceException("structure 必填");
        }
        // FL-03：执行记录占位 —— 真实副作用留待仓外 sai_agent_conversation_record 锁定（AU-13）
        log.info("[fl.execute] taskId={} structure={}", bo.getTaskId(), bo.getStructure());
        return bo.getTaskId();
    }

    @Override
    public List<Map<String, Object>> versions(Long nodeId) {
        if (nodeId == null) {
            throw new ServiceException("nodeId 必填");
        }
        // FL-04：方案版本占位 —— 真实版本表待 owner 裁
        List<Map<String, Object>> vs = new ArrayList<>();
        Map<String, Object> v = new HashMap<>();
        v.put("nodeId", nodeId);
        v.put("version", "v0.0.0-placeholder");
        v.put("status", "draft");
        v.put("diff", "(占位)");
        vs.add(v);
        return vs;
    }

    @Override
    public Map<String, Object> trace(Long taskId) {
        if (taskId == null) {
            throw new ServiceException("taskId 必填");
        }
        List<FlowHisTask> trail = flwHisTaskMapper.selectList(
            new LambdaQueryWrapper<FlowHisTask>()
                .eq(FlowHisTask::getTaskId, taskId)
                .orderByDesc(FlowHisTask::getCreateTime)
        );
        Map<String, Object> out = new HashMap<>();
        out.put("taskId", taskId);
        out.put("trail", trail);
        out.put("retryCount", trail.size());
        out.put("partialFailure", trail.stream()
            .anyMatch(h -> h.getMessage() != null && h.getMessage().contains("failed")));
        return out;
    }
}