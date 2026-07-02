package org.dromara.workflow.liteflow.instance;

import cn.hutool.core.collection.CollUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.mapper.FlowTaskMapper;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.domain.context.InstanceDeleteContext;

import java.util.List;

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
    private final FlowTaskMapper flowTaskMapper;

    @Override
    public void process() {
        InstanceDeleteContext context = getRequestData();
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

}
