package org.dromara.workflow.liteflow.complete;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.mapper.FlowTaskMapper;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.domain.bo.CompleteTaskBo;
import org.dromara.workflow.domain.context.CompleteTaskContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.dromara.workflow.common.constant.FlowConstant.*;

/**
 * 准备任务办理上下文。
 * <p>
 * 负责加载任务实例、补齐流程变量，并解析弹窗选择的后续办理人。
 *
 * @author may
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@LiteflowComponent("completePrepare")
public class CompletePrepareComponent extends NodeComponent {

    private final FlowTaskMapper flowTaskMapper;
    private final InsService insService;

    @Override
    public void process() {
        CompleteTaskContext context = getContextBean(CompleteTaskContext.class);
        CompleteTaskBo completeTaskBo = context.getCompleteTaskBo();
        FlowTask flowTask = flowTaskMapper.selectById(completeTaskBo.getTaskId());
        if (ObjectUtil.isNull(flowTask)) {
            throw new ServiceException("流程任务不存在或任务已审批！");
        }
        Instance instance = insService.getById(flowTask.getInstanceId());
        if (ObjectUtil.isNull(instance)) {
            throw new ServiceException("流程实例不存在");
        }
        context.setFlowTask(flowTask);
        context.setInstance(instance);

        Map<String, Object> variables = completeTaskBo.getVariables();
        variables.put(FLOW_COPY_LIST, completeTaskBo.getFlowCopyList());
        variables.put(MESSAGE_TYPE, completeTaskBo.getMessageType());
        variables.put(MESSAGE_NOTICE, completeTaskBo.getNotice());
        if (BusinessStatusEnum.isDraftOrCancelOrBack(instance.getFlowStatus())) {
            // 草稿、撤销、驳回状态再次办理时等同于重新提交，条件表达式通过 SUBMIT 识别提交分支。
            variables.put(SUBMIT, true);
        }
        Map<String, Object> assigneeMap = setPopAssigneeMap(completeTaskBo.getAssigneeMap(), instance.getVariableMap());
        if (CollUtil.isNotEmpty(assigneeMap)) {
            variables.putAll(assigneeMap);
        }
    }

    /**
     * 将弹窗处理人映射成通过/退回状态下的办理人变量。
     */
    private Map<String, Object> setPopAssigneeMap(Map<String, Object> assigneeMap, Map<String, Object> variablesMap) {
        Map<String, Object> map = new HashMap<>();
        if (CollUtil.isEmpty(assigneeMap)) {
            return map;
        }
        for (Map.Entry<String, Object> entry : assigneeMap.entrySet()) {
            if (variablesMap.containsKey(entry.getKey())) {
                String userIds = variablesMap.get(entry.getKey()).toString();
                if (StringUtils.isNotBlank(userIds)) {
                    Set<String> hashSet = CollUtil.newHashSet();
                    List<String> popUserIds = StringUtils.str2List(entry.getValue().toString(), StringUtils.SEPARATOR, true, true);
                    List<String> variableUserIds = StringUtils.str2List(userIds, StringUtils.SEPARATOR, true, true);
                    hashSet.addAll(popUserIds);
                    hashSet.addAll(variableUserIds);
                    // Warm-Flow 会按“状态:变量名”读取办理人，正向和退回都写入同一批候选人。
                    map.put(TaskStatusEnum.PASS.getStatus() + StringUtils.COLON + entry.getKey(), StringUtils.joinComma(hashSet));
                    map.put(TaskStatusEnum.BACK.getStatus() + StringUtils.COLON + entry.getKey(), StringUtils.joinComma(hashSet));
                }
            } else {
                map.put(TaskStatusEnum.PASS.getStatus() + StringUtils.COLON + entry.getKey(), entry.getValue());
                map.put(TaskStatusEnum.BACK.getStatus() + StringUtils.COLON + entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

}
