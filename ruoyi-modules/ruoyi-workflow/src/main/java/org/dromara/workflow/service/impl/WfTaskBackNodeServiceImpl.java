package org.dromara.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.workflow.domain.WfTaskBackNode;
import org.dromara.workflow.domain.vo.MultiInstanceVo;
import org.dromara.workflow.mapper.WfTaskBackNodeMapper;
import org.dromara.workflow.service.IWfTaskBackNodeService;
import org.dromara.workflow.utils.WorkflowUtils;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.dromara.workflow.common.constant.FlowConstant.MULTI_INSTANCE;
import static org.dromara.workflow.common.constant.FlowConstant.USER_TASK;


/**
 * 节点驳回记录Service业务层处理
 *
 * @author may
 * @date 2024-03-13
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class WfTaskBackNodeServiceImpl implements IWfTaskBackNodeService {

    private final WfTaskBackNodeMapper wfTaskBackNodeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordExecuteNode(Task task) {
        List<WfTaskBackNode> list = getListByInstanceId(task.getProcessInstanceId());
        WfTaskBackNode wfTaskBackNode = new WfTaskBackNode();
        wfTaskBackNode.setNodeId(task.getTaskDefinitionKey());
        wfTaskBackNode.setNodeName(task.getName());
        wfTaskBackNode.setInstanceId(task.getProcessInstanceId());
        wfTaskBackNode.setAssignee(String.valueOf(LoginHelper.getUserId()));
        MultiInstanceVo multiInstance = WorkflowUtils.isMultiInstance(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
        if (ObjectUtil.isNotEmpty(multiInstance)) {
            wfTaskBackNode.setTaskType(MULTI_INSTANCE);
        } else {
            wfTaskBackNode.setTaskType(USER_TASK);
        }
        if (CollUtil.isEmpty(list)) {
            wfTaskBackNode.setOrderNo(0);
            wfTaskBackNodeMapper.insert(wfTaskBackNode);
        } else {
            WfTaskBackNode taskNode = StreamUtils.findFirst(list, e -> e.getNodeId().equals(wfTaskBackNode.getNodeId()) && e.getOrderNo() == 0);
            if (ObjectUtil.isEmpty(taskNode)) {
                wfTaskBackNode.setOrderNo(list.get(0).getOrderNo() + 1);
                WfTaskBackNode node = getListByInstanceIdAndNodeId(wfTaskBackNode.getInstanceId(), wfTaskBackNode.getNodeId());
                if (ObjectUtil.isNotEmpty(node)) {
                    node.setAssignee(node.getAssignee() + StringUtils.SEPARATOR + LoginHelper.getUserId());
                    wfTaskBackNodeMapper.updateById(node);
                } else {
                    wfTaskBackNodeMapper.insert(wfTaskBackNode);
                }
            }
        }
    }

    @Override
    public List<WfTaskBackNode> getListByInstanceId(String processInstanceId) {
        LambdaQueryWrapper<WfTaskBackNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTaskBackNode::getInstanceId, processInstanceId);
        wrapper.orderByDesc(WfTaskBackNode::getOrderNo);
        return wfTaskBackNodeMapper.selectList(wrapper);
    }

    @Override
    public WfTaskBackNode getListByInstanceIdAndNodeId(String processInstanceId, String nodeId) {
        LambdaQueryWrapper<WfTaskBackNode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfTaskBackNode::getInstanceId, processInstanceId);
        queryWrapper.eq(WfTaskBackNode::getNodeId, nodeId);
        return wfTaskBackNodeMapper.selectOne(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBackTaskNode(String processInstanceId, String targetActivityId) {
        try {
            LambdaQueryWrapper<WfTaskBackNode> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WfTaskBackNode::getInstanceId, processInstanceId);
            queryWrapper.eq(WfTaskBackNode::getNodeId, targetActivityId);
            WfTaskBackNode actTaskNode = wfTaskBackNodeMapper.selectOne(queryWrapper);
            if (ObjectUtil.isNotNull(actTaskNode)) {
                Integer orderNo = actTaskNode.getOrderNo();
                List<WfTaskBackNode> taskNodeList = getListByInstanceId(processInstanceId);
                List<Long> ids = new ArrayList<>();
                if (CollUtil.isNotEmpty(taskNodeList)) {
                    for (WfTaskBackNode taskNode : taskNodeList) {
                        if (taskNode.getOrderNo() >= orderNo) {
                            ids.add(taskNode.getId());
                        }
                    }
                }
                if (CollUtil.isNotEmpty(ids)) {
                    wfTaskBackNodeMapper.deleteByIds(ids);
                }
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException("删除失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByInstanceId(String processInstanceId) {
        LambdaQueryWrapper<WfTaskBackNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WfTaskBackNode::getInstanceId, processInstanceId);
        List<WfTaskBackNode> list = wfTaskBackNodeMapper.selectList(wrapper);
        int delete = wfTaskBackNodeMapper.delete(wrapper);
        if (list.size() != delete) {
            throw new ServiceException("删除失败");
        }
        return true;
    }

    @Override
    public boolean deleteByInstanceIds(List<String> processInstanceIds) {
        LambdaQueryWrapper<WfTaskBackNode> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WfTaskBackNode::getInstanceId, processInstanceIds);
        List<WfTaskBackNode> list = wfTaskBackNodeMapper.selectList(wrapper);
        int delete = wfTaskBackNodeMapper.delete(wrapper);
        if (list.size() != delete) {
            throw new ServiceException("删除失败");
        }
        return true;
    }
}
