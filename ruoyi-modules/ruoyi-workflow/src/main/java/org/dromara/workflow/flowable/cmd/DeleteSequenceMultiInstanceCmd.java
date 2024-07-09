package org.dromara.workflow.flowable.cmd;

import cn.hutool.core.util.ObjectUtil;
import lombok.AllArgsConstructor;
import org.dromara.common.core.utils.StreamUtils;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.dromara.workflow.common.constant.FlowConstant.LOOP_COUNTER;
import static org.dromara.workflow.common.constant.FlowConstant.NUMBER_OF_INSTANCES;


/**
 * 串行减签
 *
 * @author may
 */
@AllArgsConstructor
public class DeleteSequenceMultiInstanceCmd implements Command<Void> {

    /**
     * 当前节点审批人员id
     */
    private final String currentUserId;

    /**
     * 执行id
     */
    private final String executionId;

    /**
     * 会签人员集合KEY
     */
    private final String assigneeList;

    /**
     * 减签人员
     */
    private final List<Long> assignees;


    @Override
    @SuppressWarnings("unchecked")
    public Void execute(CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
        ExecutionEntity entity = executionEntityManager.findById(executionId);
        // 设置流程变量
        List<Long> userIds = new ArrayList<>();
        List<Object> variable = (List<Object>) entity.getVariable(assigneeList);
        for (Object o : variable) {
            userIds.add(Long.valueOf(o.toString()));
        }
        List<Long> userIdList = new ArrayList<>();
        userIds.forEach(e -> {
            Long userId = StreamUtils.findFirst(assignees, id -> ObjectUtil.equals(id, e));
            if (userId == null) {
                userIdList.add(e);
            }
        });
        // 当前任务执行位置
        int loopCounterIndex = -1;
        for (int i = 0; i < userIdList.size(); i++) {
            Long userId = userIdList.get(i);
            if (currentUserId.equals(userId.toString())) {
                loopCounterIndex = i;
            }
        }
        Map<String, Object> variables = new HashMap<>(16);
        variables.put(NUMBER_OF_INSTANCES, userIdList.size());
        variables.put(assigneeList, userIdList);
        variables.put(LOOP_COUNTER, loopCounterIndex);
        entity.setVariables(variables);
        return null;
    }
}
