package org.dromara.workflow.event;

import cn.hutool.core.collection.CollUtil;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.workflow.domain.bo.FlowCopyBo;

import java.util.List;

/**
 * 工作流抄送事件。
 *
 * @param task         当前任务
 * @param flowCopyList 抄送人列表
 */
public record WorkflowCopyEvent(Task task, List<FlowCopyBo> flowCopyList) {

    public WorkflowCopyEvent {
        if (CollUtil.isNotEmpty(flowCopyList)) {
            flowCopyList = List.copyOf(flowCopyList);
        }
    }

}
