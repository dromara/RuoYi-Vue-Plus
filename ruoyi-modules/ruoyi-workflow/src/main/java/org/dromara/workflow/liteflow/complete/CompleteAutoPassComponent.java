package org.dromara.workflow.liteflow.complete;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.User;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.mapper.FlowTaskMapper;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.domain.context.CompleteTaskContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.dromara.workflow.common.constant.FlowConstant.*;

/**
 * 执行当前处理人的后续自动审批。
 * <p>
 * 仅当流程实例开启自动审批时生效，用于连续跳过同一登录人负责的后续待办任务。
 *
 * @author may
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@LiteflowComponent("completeAutoPass")
public class CompleteAutoPassComponent extends NodeComponent {

    private final TaskService taskService;
    private final FlowTaskMapper flowTaskMapper;

    @Override
    public void process() {
        CompleteTaskContext context = getContextBean(CompleteTaskContext.class);
        FlowTask flowTask = context.getFlowTask();
        autoPass(flowTask.getId(), context.getFlowParams(), flowTask.getInstanceId());
    }

    /**
     * 递归跳过当前处理人的后续任务。
     *
     * @param taskId     本轮已处理任务 id，用于避免重复跳过同一任务
     * @param flowParams 当前办理参数，会在自动审批时覆盖为自动审批消息和空抄送变量
     * @param instanceId 流程实例 id
     */
    private void autoPass(Long taskId, FlowParams flowParams, Long instanceId) {
        List<FlowTask> flowTaskList = selectByInstId(instanceId);
        if (CollUtil.isEmpty(flowTaskList)) {
            return;
        }
        List<User> userList = FlowEngine.userService()
            .getByAssociateds(StreamUtils.toList(flowTaskList, FlowTask::getId));
        if (CollUtil.isEmpty(userList)) {
            return;
        }
        for (FlowTask task : flowTaskList) {
            if (task.getId().equals(taskId)) {
                continue;
            }
            // 自动审批只处理当前登录人仍是办理人的后续任务，避免替其他候选人或并行分支误审批。
            List<User> users = StreamUtils.filter(userList,
                e -> ObjectUtil.equals(task.getId(), e.getAssociated()) && ObjectUtil.equal(e.getProcessedBy(), LoginHelper.getUserIdStr()));
            if (CollUtil.isEmpty(users)) {
                continue;
            }
            // 自动审批不继承人工办理时的抄送和消息通知，避免重复发送业务消息。
            flowParams
                .message("流程引擎自动审批！")
                .variable(Map.of(
                    SUBMIT, false,
                    FLOW_COPY_LIST, Collections.emptyList(),
                    MESSAGE_NOTICE, StringUtils.EMPTY));
            taskService.skip(task.getId(), flowParams);
            // 跳过后可能生成新的后续任务，继续扫描直到当前登录人不再有可自动办理任务。
            autoPass(task.getId(), flowParams, instanceId);
        }
    }

    private List<FlowTask> selectByInstId(Long instanceId) {
        return flowTaskMapper.selectList(QueryBuilder.lambda(FlowTask.class)
            .eq(FlowTask::getInstanceId, instanceId)
            .build());
    }

}
