package org.dromara.workflow.api.domain;

/**
 * 启动流程后的返回结果对象。
 *
 * @param processInstanceId 流程实例 ID
 * @param taskId            首个任务 ID
 * @author Lion Li
 */
public record StartProcessReturnDTO(
    Long processInstanceId,
    Long taskId
) {
}
