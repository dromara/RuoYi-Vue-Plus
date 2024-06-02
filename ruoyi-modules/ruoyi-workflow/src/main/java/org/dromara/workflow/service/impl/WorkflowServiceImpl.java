package org.dromara.workflow.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.service.WorkflowService;
import org.dromara.workflow.service.IActProcessInstanceService;
import org.dromara.workflow.utils.WorkflowUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通用 工作流服务实现
 *
 * @author may
 */
@RequiredArgsConstructor
@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final IActProcessInstanceService iActProcessInstanceService;

    /**
     * 运行中的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param businessKeys 业务id
     * @return 结果
     */
    @Override
    public boolean deleteRunAndHisInstance(List<String> businessKeys) {
        return iActProcessInstanceService.deleteRunAndHisInstance(businessKeys);
    }

    /**
     * 获取当前流程状态
     *
     * @param taskId 任务id
     */
    @Override
    public String getBusinessStatusByTaskId(String taskId) {
        return WorkflowUtils.getBusinessStatusByTaskId(taskId);
    }

    /**
     * 获取当前流程状态
     *
     * @param businessKey 业务id
     */
    @Override
    public String getBusinessStatus(String businessKey) {
        return WorkflowUtils.getBusinessStatus(businessKey);
    }

    /**
     * 设置流程实例对象
     *
     * @param obj         业务对象
     * @param businessKey 业务id
     */
    @Override
    public void setBusinessInstanceDTO(Object obj, String businessKey) {
        WorkflowUtils.setBusinessInstanceDTO(obj, businessKey);
    }

    /**
     * 设置流程实例对象
     *
     * @param obj       业务对象
     * @param idList    业务id
     * @param fieldName 主键属性名称
     */
    @Override
    public void setBusinessInstanceListDTO(Object obj, List<String> idList, String fieldName) {
        WorkflowUtils.setBusinessInstanceListDTO(obj, idList, fieldName);
    }
}
