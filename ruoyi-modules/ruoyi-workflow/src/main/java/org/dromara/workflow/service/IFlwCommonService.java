package org.dromara.workflow.service;

import java.util.List;

/**
 * 通用 工作流服务
 *
 * @author LionLi
 */
public interface IFlwCommonService {

    /**
     * 构建工作流用户
     *
     * @param permissionList 办理用户
     * @return 用户
     */
    List<String> buildUser(List<String> permissionList);

    /**
     * 发送消息
     *
     * @param flowName    流程定义名称
     * @param instId      实例id
     * @param messageType 消息类型
     * @param message     消息内容，为空则发送默认配置的消息内容
     */
    void sendMessage(String flowName, Long instId, List<String> messageType, String message);

    /**
     * 申请人节点编码
     *
     * @param definitionId 流程定义id
     * @return 申请人节点编码
     */
    String applyNodeCode(Long definitionId);
}
