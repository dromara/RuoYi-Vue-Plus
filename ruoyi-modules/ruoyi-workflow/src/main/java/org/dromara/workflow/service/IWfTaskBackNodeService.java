package org.dromara.workflow.service;


import org.dromara.workflow.domain.WfTaskBackNode;
import org.flowable.task.api.Task;

import java.util.List;

/**
 * 节点驳回记录Service接口
 *
 * @author may
 * @date 2024-03-13
 */
public interface IWfTaskBackNodeService {

    /**
     * 记录审批节点
     *
     * @param task 任务
     */
    void recordExecuteNode(Task task);

    /**
     * 按流程实例id查询
     *
     * @param processInstanceId 流程实例id
     * @return 结果
     */
    List<WfTaskBackNode> getListByInstanceId(String processInstanceId);

    /**
     * 按照流程实例id，节点id查询
     *
     * @param processInstanceId 流程实例id
     * @param nodeId            节点id
     * @return 结果
     */
    WfTaskBackNode getListByInstanceIdAndNodeId(String processInstanceId, String nodeId);

    /**
     * 删除驳回后的节点
     *
     * @param processInstanceId 流程实例id
     * @param targetActivityId  节点id
     * @return 结果
     */
    boolean deleteBackTaskNode(String processInstanceId, String targetActivityId);

    /**
     * 按流程实例id删除
     *
     * @param processInstanceId 流程实例id
     * @return 结果
     */
    boolean deleteByInstanceId(String processInstanceId);

    /**
     * 按流程实例id删除
     *
     * @param processInstanceIds 流程实例id
     * @return 结果
     */
    boolean deleteByInstanceIds(List<String> processInstanceIds);
}
