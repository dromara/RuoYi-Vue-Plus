package org.dromara.workflow.flowable.cmd;

import cn.hutool.core.collection.CollUtil;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.dromara.workflow.common.constant.FlowConstant.NUMBER_OF_INSTANCES;

/**
 * 串行加签
 *
 * @author may
 */
public class AddSequenceMultiInstanceCmd implements Command<Void> {

    /**
     * 执行id
     */
    private final String executionId;

    /**
     * 会签人员集合KEY
     */
    private final String assigneeList;

    /**
     * 加签人员
     */
    private final List<Long> assignees;

    public AddSequenceMultiInstanceCmd(String executionId, String assigneeList, List<Long> assignees) {
        this.executionId = executionId;
        this.assigneeList = assigneeList;
        this.assignees = assignees;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
        ExecutionEntity entity = executionEntityManager.findById(executionId);
        // 多实例任务总数加 assignees.size()
        if (entity.getVariable(NUMBER_OF_INSTANCES) instanceof Integer nrOfInstances) {
            entity.setVariable(NUMBER_OF_INSTANCES, nrOfInstances + assignees.size());
        }
        // 设置流程变量
        if (entity.getVariable(assigneeList) instanceof List<?> userIds) {
            CollUtil.addAll(userIds, assignees);
            Map<String, Object> variables = new HashMap<>(16);
            variables.put(assigneeList, userIds);
            entity.setVariables(variables);
        }
        return null;
    }
}
