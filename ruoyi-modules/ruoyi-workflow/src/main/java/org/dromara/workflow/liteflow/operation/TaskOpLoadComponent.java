package org.dromara.workflow.liteflow.operation;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.CooperateType;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.dromara.warm.flow.orm.mapper.FlowNodeMapper;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.enums.TaskOperationEnum;
import org.dromara.workflow.domain.context.TaskOperationContext;

/**
 * 加载任务节点并校验操作约束。
 *
 * @author may
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@LiteflowComponent("taskOpLoad")
public class TaskOpLoadComponent extends NodeComponent {

    private final TaskService taskService;
    private final FlowNodeMapper flowNodeMapper;

    @Override
    public void process() {
        TaskOperationContext context = getRequestData();
        Task task = taskService.getById(context.getTaskOperationBo().getTaskId());
        if (ObjectUtil.isNull(task)) {
            throw new ServiceException("任务不存在！");
        }
        FlowNode flowNode = flowNodeMapper.selectOne(QueryBuilder.lambda(FlowNode.class)
            .eq(FlowNode::getNodeCode, task.getNodeCode())
            .eq(FlowNode::getDefinitionId, task.getDefinitionId())
            .build());
        if (ObjectUtil.isNull(flowNode)) {
            throw new ServiceException("流程节点不存在");
        }
        if ((context.getOperation() == TaskOperationEnum.ADD_SIGNATURE || context.getOperation() == TaskOperationEnum.REDUCTION_SIGNATURE)
            && CooperateType.isOrSign(flowNode.getNodeRatio())) {
            throw new ServiceException(task.getNodeName() + "不是会签或票签节点！");
        }
        context.setTask(task);
        context.setFlowNode(flowNode);
    }

}
