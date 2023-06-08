package org.dromara.workflow.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 流程实例 服务层
 *
 * @author may
 */
public interface IActProcessInstanceService {
    /**
     * 通过流程实例id获取历史流程图
     *
     * @param processInstanceId 流程实例id
     * @param response          响应
     */
    void getHistoryProcessImage(String processInstanceId, HttpServletResponse response);
}
