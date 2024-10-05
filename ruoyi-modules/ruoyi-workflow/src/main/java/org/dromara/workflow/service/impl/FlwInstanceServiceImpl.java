package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.warm.flow.orm.entity.FlowDefinition;
import com.warm.flow.orm.entity.FlowInstance;
import com.warm.flow.orm.mapper.FlowDefinitionMapper;
import com.warm.flow.orm.mapper.FlowInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.workflow.domain.bo.FlowInstanceBo;
import org.dromara.workflow.domain.bo.InstanceBo;
import org.dromara.workflow.domain.vo.FlowInstanceVo;
import org.dromara.workflow.mapper.FlwInstanceMapper;
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
    private final FlwInstanceMapper flwInstanceMapper;
    private final FlowDefinitionMapper flowDefinitionMapper;

    /**
     * 分页查询正在运行的流程实例
     *
     * @param instance  参数
     * @param pageQuery 分页
     */
    @Override
    public TableDataInfo<FlowInstanceVo> getPageByRunning(Instance instance, PageQuery pageQuery) {
        QueryWrapper<FlowInstanceBo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("t.flow_status", FlowStatus.APPROVAL.getKey());
        Page<FlowInstanceVo> page = flwInstanceMapper.page(pageQuery.build(), queryWrapper);
        TableDataInfo<FlowInstanceVo> build = TableDataInfo.build();
        build.setRows(BeanUtil.copyToList(page.getRecords(), FlowInstanceVo.class));
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
    public TableDataInfo<FlowInstanceVo> getPageByFinish(Instance instance, PageQuery pageQuery) {
        QueryWrapper<FlowInstanceBo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("t.flow_status", FlowStatus.FINISHED.getKey());
        Page<FlowInstanceVo> page = flwInstanceMapper.page(pageQuery.build(), queryWrapper);
        TableDataInfo<FlowInstanceVo> build = TableDataInfo.build();
        build.setRows(BeanUtil.copyToList(page.getRecords(), FlowInstanceVo.class));
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
            //List<Node> nextNodes = FlowFactory.taskService().getNextByCheckGateWay(new FlowParams(), getFirstBetween(startNode));
            //Node node = nextNodes.get(0);
           // FlowParams flowParams = FlowParams.build().nodeCode(node.getNodeCode()).skipType(SkipType.PASS.getKey()).permissionFlag(WorkflowUtils.permissionList());
           // taskService.skip(list.get(0).getId(), flowParams);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private Node getFirstBetween(Node startNode) {
        List<Skip> skips = FlowFactory.skipService().list(FlowFactory.newSkip().setDefinitionId(startNode.getDefinitionId()).setNowNodeCode(startNode.getNodeCode()));
        Skip skip = skips.get(0);
        return FlowFactory.nodeService().getOne(FlowFactory.newNode().setDefinitionId(startNode.getDefinitionId()).setNodeCode(skip.getNextNodeCode()));
    }

    /**
     * 获取当前登陆人发起的流程实例
     *
     * @param instanceBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowInstanceVo> getPageByCurrent(InstanceBo instanceBo, PageQuery pageQuery) {
        LambdaQueryWrapper<FlowInstance> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(instanceBo.getFlowCode())) {
            List<FlowDefinition> flowDefinitions = flowDefinitionMapper.selectList(
                new LambdaQueryWrapper<FlowDefinition>().eq(FlowDefinition::getFlowCode, instanceBo.getFlowCode()));
            if (CollUtil.isNotEmpty(flowDefinitions)) {
                List<Long> defIdList = StreamUtils.toList(flowDefinitions, FlowDefinition::getId);
                wrapper.in(FlowInstance::getDefinitionId, defIdList);
            }
        }
        wrapper.eq(FlowInstance::getCreateBy, LoginHelper.getUserId());
        Page<FlowInstance> page = flowInstanceMapper.selectPage(pageQuery.build(), wrapper);
        TableDataInfo<FlowInstanceVo> build = TableDataInfo.build();
        List<FlowInstanceVo> flowInstanceVos = BeanUtil.copyToList(page.getRecords(), FlowInstanceVo.class);
        if (CollUtil.isNotEmpty(flowInstanceVos)) {
            List<Long> definitionIds = StreamUtils.toList(flowInstanceVos, FlowInstanceVo::getDefinitionId);
            List<FlowDefinition> flowDefinitions = flowDefinitionMapper.selectBatchIds(definitionIds);
            for (FlowInstanceVo vo : flowInstanceVos) {
                flowDefinitions.stream().filter(e -> e.getId().toString().equals(vo.getDefinitionId().toString())).findFirst().ifPresent(e -> {
                    vo.setFlowName(e.getFlowName());
                    vo.setFlowCode(e.getFlowCode());
                    vo.setVersion(e.getVersion());
                    vo.setFlowStatusName(FlowStatus.getValueByKey(vo.getFlowStatus()));
                });
            }

        }
        build.setRows(flowInstanceVos);
        build.setTotal(page.getTotal());
        return build;
    }
}
