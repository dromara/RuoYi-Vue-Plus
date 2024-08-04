package org.dromara.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.warm.flow.core.FlowFactory;
import com.warm.flow.core.constant.ExceptionCons;
import com.warm.flow.core.dto.FlowParams;
import com.warm.flow.core.entity.*;
import com.warm.flow.core.enums.FlowStatus;
import com.warm.flow.core.enums.NodeType;
import com.warm.flow.core.enums.SkipType;
import com.warm.flow.core.service.DefService;
import com.warm.flow.core.service.InsService;
import com.warm.flow.core.service.NodeService;
import com.warm.flow.core.service.TaskService;
import com.warm.flow.core.utils.AssertUtil;
import com.warm.flow.core.utils.page.Page;
import com.warm.flow.orm.entity.FlowInstance;
import com.warm.flow.orm.mapper.FlowInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.service.IFlwInstanceService;
import org.dromara.workflow.utils.WorkflowUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 流程实例 服务层实现
 *
 * @author may
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwInstanceServiceImpl implements IFlwInstanceService {

    private final InsService insService;
    private final NodeService nodeService;
    private final DefService defService;
    private final TaskService taskService;
    private final FlowInstanceMapper flowInstanceMapper;

    /**
     * 分页查询正在运行的流程实例
     *
     * @param instance  参数
     * @param pageQuery 分页
     */
    @Override
    public TableDataInfo<Instance> getPageByRunning(Instance instance, PageQuery pageQuery) {
        Page<Instance> page = Page.pageOf(pageQuery.getPageNum(), pageQuery.getPageSize());
        instance.setFlowStatus(FlowStatus.APPROVAL.getKey());
        page = insService.orderByCreateTime().desc().page(instance, page);
        TableDataInfo<Instance> build = TableDataInfo.build();
        build.setRows(page.getList());
        build.setTotal(page.getTotal());
        return build;
    }

    /**
     * 分页查询已结束的流程实例
     *
     * @param instance  参数
     * @param pageQuery 分页
     */
    @Override
    public TableDataInfo<Instance> getPageByFinish(Instance instance, PageQuery pageQuery) {
        Page<Instance> page = Page.pageOf(pageQuery.getPageNum(), pageQuery.getPageSize());
        instance.setFlowStatus(FlowStatus.FINISHED.getKey());
        page = insService.orderByCreateTime().desc().page(instance, page);
        TableDataInfo<Instance> build = TableDataInfo.build();
        build.setRows(page.getList());
        build.setTotal(page.getTotal());
        return build;
    }

    /**
     * 根据业务id查询流程实例
     *
     * @param businessId 业务id
     */
    @Override
    public FlowInstance instanceByBusinessId(String businessId) {
        return flowInstanceMapper.selectOne(new LambdaQueryWrapper<FlowInstance>().eq(FlowInstance::getBusinessId, businessId));
    }

    /**
     * 按照业务id删除流程实例
     *
     * @param businessIds 业务id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByBusinessIds(List<Long> businessIds) {
        List<FlowInstance> flowInstances = flowInstanceMapper.selectList(new LambdaQueryWrapper<FlowInstance>().in(FlowInstance::getBusinessId, businessIds));
        if (CollUtil.isEmpty(flowInstances)) {
            return false;
        }
        return insService.remove(StreamUtils.toList(flowInstances, FlowInstance::getId));
    }

    /**
     * 按照实例id删除流程实例
     *
     * @param instanceIds 实例id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByInstanceIds(List<Long> instanceIds) {
        return insService.remove(instanceIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelProcessApply(String businessId) {
        FlowInstance flowInstance = instanceByBusinessId(businessId);
        if (ObjectUtil.isNull(flowInstance)) {
            return false;
        }
        try {
            Definition definition = defService.getById(flowInstance.getDefinitionId());
            List<Task> list = taskService.list(FlowFactory.newTask().setInstanceId(flowInstance.getId()));
            // 获取已发布的流程节点
            List<Node> nodes = nodeService.getByFlowCode(definition.getFlowCode());
            AssertUtil.isTrue(CollUtil.isEmpty(nodes), ExceptionCons.NOT_PUBLISH_NODE);
            // 获取开始节点
            Node startNode = nodes.stream().filter(t -> NodeType.isStart(t.getNodeType())).findFirst().orElse(null);
            AssertUtil.isNull(startNode, ExceptionCons.LOST_START_NODE);

            // 获取下一个节点，如果是网关节点，则重新获取后续节点
            List<Node> nextNodes = FlowFactory.taskService().getNextByCheckGateWay(new FlowParams(), getFirstBetween(startNode));
            Node node = nextNodes.get(0);
            FlowParams flowParams = FlowParams.build()
                .nodeCode(node.getNodeCode())
                .skipType(SkipType.PASS.getKey())
                .permissionFlag(WorkflowUtils.permissionList());
            taskService.skip(list.get(0).getId(), flowParams);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private Node getFirstBetween(Node startNode) {
        List<Skip> skips = FlowFactory.skipService().list(FlowFactory.newSkip()
            .setDefinitionId(startNode.getDefinitionId()).setNowNodeCode(startNode.getNodeCode()));
        Skip skip = skips.get(0);
        return FlowFactory.nodeService().getOne(FlowFactory.newNode().setDefinitionId(startNode.getDefinitionId())
            .setNodeCode(skip.getNextNodeCode()));
    }
}
