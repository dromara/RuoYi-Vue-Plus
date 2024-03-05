package org.dromara.workflow.flowable.cmd;

import org.dromara.common.core.utils.StreamUtils;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;

import java.io.Serializable;
import java.util.List;

/**
 * 获取并行网关执行后保留的执行实例数据
 *
 * @author may
 */
public class ExecutionChildByExecutionIdCmd implements Command<List<ExecutionEntity>>, Serializable {

    /**
     * 当前任务执行实例id
     */
    private final String executionId;

    public ExecutionChildByExecutionIdCmd(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public List<ExecutionEntity> execute(CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
        // 获取当前执行数据
        ExecutionEntity executionEntity = executionEntityManager.findById(executionId);
        // 通过当前执行数据的父执行，查询所有子执行数据
        List<ExecutionEntity> allChildrenExecution =
            executionEntityManager.collectChildren(executionEntity.getParent());
        return StreamUtils.filter(allChildrenExecution, e -> !e.isActive());
    }
}
