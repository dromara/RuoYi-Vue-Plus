package org.dromara.workflow.de.nc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.nc.domain.bo.DeNcDiffBo;
import org.dromara.workflow.de.nc.domain.bo.DeNcExecModeBo;
import org.dromara.workflow.de.nc.domain.bo.DeNcScopeBo;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.dromara.warm.flow.orm.mapper.FlowNodeMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * NC 域服务实现。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeNcServiceImpl implements IDeNcService {

    private final FlowNodeMapper flowNodeMapper;

    @Override
    public Long saveScope(DeNcScopeBo bo) {
        FlowNode node = flowNodeMapper.selectById(bo.getNodeId());
        if (node == null) {
            throw new ServiceException("节点不存在: id=" + bo.getNodeId());
        }
        node.setExt(bo.getConfig());
        flowNodeMapper.updateById(node);
        log.info("[nc.scope] nodeId={} scope={}", bo.getNodeId(), bo.getScope());
        return node.getId();
    }

    @Override
    public Long setExecMode(DeNcExecModeBo bo) {
        FlowNode node = flowNodeMapper.selectById(bo.getNodeId());
        if (node == null) {
            throw new ServiceException("节点不存在: id=" + bo.getNodeId());
        }
        // 复用 listener_type 列（底座已有）：存 manual/assist/auto
        node.setListenerType(bo.getExecMode());
        flowNodeMapper.updateById(node);
        log.info("[nc.execMode] nodeId={} mode={}", bo.getNodeId(), bo.getExecMode());
        return node.getId();
    }

    @Override
    public Object showDiff(DeNcDiffBo bo) {
        FlowNode node = flowNodeMapper.selectById(bo.getNodeId());
        if (node == null) {
            throw new ServiceException("节点不存在: id=" + bo.getNodeId());
        }
        Map<String, Object> diff = new HashMap<>();
        diff.put("nodeId", node.getId());
        diff.put("currentListenerType", node.getListenerType());
        diff.put("currentExt", node.getExt());
        diff.put("newAssigneeId", bo.getNewAssigneeId());
        diff.put("newResourceId", bo.getNewResourceId());
        diff.put("runningConstraint", "运行中节点切换执行者须先确认 in-flight task 已收尾");
        return diff;
    }
}