package org.dromara.workflow.liteflow.instance;

import cn.hutool.core.collection.CollUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.dromara.warm.flow.orm.mapper.FlowTaskMapper;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.domain.context.InstanceDeleteContext;

import java.util.List;
import java.util.Objects;

/**
 * 执行流程实例删除。
 *
 * @author may
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@LiteflowComponent("instanceDeleteExecute")
public class InstanceDeleteExecuteComponent extends NodeComponent {

    private final InsService insService;
    private final FlowHisTaskMapper flowHisTaskMapper;
    private final FlowTaskMapper flowTaskMapper;

    @Override
    public void process() {
        InstanceDeleteContext context = getContextBean(InstanceDeleteContext.class);
        deleteCopyUsers(context.getDeleteInstanceIds());
        if (!context.isHistory()) {
            context.setResult(insService.remove(context.getDeleteInstanceIds()));
            return;
        }
        List<FlowTask> flowTaskList = flowTaskMapper.selectList(QueryBuilder.lambda(FlowTask.class)
            .in(FlowTask::getInstanceId, context.getDeleteInstanceIds())
            .build());
        if (CollUtil.isNotEmpty(flowTaskList)) {
            FlowEngine.userService().deleteByTaskIds(StreamUtils.toList(flowTaskList, FlowTask::getId));
        }
        FlowEngine.taskService().deleteByInsIds(context.getDeleteInstanceIds());
        FlowEngine.hisTaskService().deleteByInsIds(context.getDeleteInstanceIds());
        FlowEngine.insService().removeByIds(context.getDeleteInstanceIds());
        context.setResult(true);
    }

    /**
     * 删除流程实例产生的抄送人员关联。
     *
     * @param instanceIds 流程实例 ID 集合
     */
    private void deleteCopyUsers(List<Long> instanceIds) {
        List<FlowHisTask> copyTasks = flowHisTaskMapper.selectList(QueryBuilder.lambda(FlowHisTask.class)
            .in(FlowHisTask::getInstanceId, instanceIds)
            .eq(FlowHisTask::getFlowStatus, TaskStatusEnum.COPY.getStatus())
            .build());
        if (CollUtil.isEmpty(copyTasks)) {
            return;
        }
        List<Long> copyTaskIds = copyTasks.stream()
            .map(FlowHisTask::getTaskId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (CollUtil.isNotEmpty(copyTaskIds)) {
            FlowEngine.userService().deleteByTaskIds(copyTaskIds);
        }
    }

}
