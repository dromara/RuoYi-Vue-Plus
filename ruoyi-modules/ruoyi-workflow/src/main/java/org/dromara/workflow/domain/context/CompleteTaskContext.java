package org.dromara.workflow.domain.context;

import lombok.Data;
import org.dromara.warm.flow.core.dto.FlowParams;
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

    /**
     * Warm-Flow 办理参数，由参数构建节点生成并供执行、自动审批节点复用。
     */
    private FlowParams flowParams;

    /**
     * 流程实例是否开启自动审批，由参数构建节点从实例变量中解析。
     */
    private Boolean autoPass;

}
