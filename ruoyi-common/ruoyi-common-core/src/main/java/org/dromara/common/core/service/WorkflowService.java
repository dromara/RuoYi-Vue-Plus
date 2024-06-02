package org.dromara.common.core.service;

import java.util.List;

/**
 * 通用 工作流服务
 *
 * @author may
 */
public interface WorkflowService {

    /**
     * 运行中的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param businessKeys 业务id
     * @return 结果
     */
    boolean deleteRunAndHisInstance(List<String> businessKeys);

    /**
     * 获取当前流程状态
     *
     * @param taskId 任务id
     */
    String getBusinessStatusByTaskId(String taskId);

    /**
     * 获取当前流程状态
     *
     * @param businessKey 业务id
     */
    String getBusinessStatus(String businessKey);

    /**
     * 设置流程实例对象
     *
     * @param obj         业务对象
     * @param businessKey 业务id
     */
    void setBusinessInstanceDTO(Object obj, String businessKey);

    /**
     * 设置流程实例对象
     *
     * @param obj       业务对象
     * @param idList    业务id
     * @param fieldName 主键属性名称
     */
    void setBusinessInstanceListDTO(Object obj, List<String> idList, String fieldName);
}
