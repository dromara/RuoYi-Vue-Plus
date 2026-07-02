package org.dromara.workflow.domain.context;

import lombok.Data;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.dromara.workflow.common.enums.TaskOperationEnum;
import org.dromara.workflow.domain.bo.TaskOperationBo;

/**
 * 任务操作 LiteFlow 上下文。
 * <p>
 * 用于在委派、转办、加签、减签链路中传递操作类型、任务、节点和办理参数。
 *
 * @author may
 */
@Data
public class TaskOperationContext {

    /**
     * 任务操作入参。
     */
    private final TaskOperationBo taskOperationBo;

    /**
     * 操作类型编码。
     */
    private final String taskOperation;

    /**
     * 解析后的操作枚举。
     */
    private TaskOperationEnum operation;

    /**
     * Warm-Flow 操作参数。
     */
    private FlowParams flowParams;

    /**
     * 当前任务。
     */
    private Task task;

    /**
     * 当前任务对应的流程节点。
     */
    private FlowNode flowNode;

    /**
     * 操作执行结果。
     */
    private boolean result;

}
