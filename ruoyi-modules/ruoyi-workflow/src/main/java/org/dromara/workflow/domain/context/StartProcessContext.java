package org.dromara.workflow.domain.context;

import lombok.Data;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.workflow.api.domain.StartProcessReturnDTO;
import org.dromara.workflow.domain.FlowInstanceBizExt;
import org.dromara.workflow.domain.bo.StartProcessBo;

import java.util.List;
import java.util.Map;

/**
 * 启动流程 LiteFlow 上下文。
 * <p>
 * 链路内保存启动参数、流程定义、流程实例和首个任务等中间结果。
 *
 * @author may
 */
@Data
public class StartProcessContext {

    /**
     * 启动流程入参。
     */
    private final StartProcessBo startProcessBo;

    /**
     * 业务唯一标识。
     */
    private String businessId;

    /**
     * 流程变量。
     */
    private Map<String, Object> variables;

    /**
     * 流程实例业务扩展信息。
     */
    private FlowInstanceBizExt bizExt;

    /**
     * 已存在的流程实例，非空时走续提交逻辑。
     */
    private FlowInstance existingInstance;

    /**
     * 已发布的流程定义。
     */
    private Definition definition;

    /**
     * 新启动的流程实例。
     */
    private Instance instance;

    /**
     * 当前流程实例下生成的任务列表。
     */
    private List<Task> taskList;

    /**
     * 启动流程返回结果。
     */
    private StartProcessReturnDTO startProcessReturn;

}
