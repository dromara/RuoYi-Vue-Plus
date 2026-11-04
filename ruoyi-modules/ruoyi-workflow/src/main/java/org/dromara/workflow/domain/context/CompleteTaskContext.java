package org.dromara.workflow.domain.context;

import lombok.Data;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.domain.bo.CompleteTaskBo;

/**
 * 办理任务 LiteFlow 上下文。
 * <p>
 * 链路内各节点通过该对象传递任务、实例、办理参数等中间结果，避免组件之间重复查询。
 *
 * @author may
 */
@Data
public class CompleteTaskContext {

    /**
     * 办理任务入参。
     */
    private final CompleteTaskBo completeTaskBo;

    /**
     * 当前待办理的运行任务，由校验节点加载。
     */
    private FlowTask flowTask;

    /**
     * 当前任务所属流程实例，由校验节点加载。
     */
    private Instance instance;


}
