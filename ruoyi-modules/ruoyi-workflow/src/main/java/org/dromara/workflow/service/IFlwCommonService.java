package org.dromara.workflow.service;

import org.dromara.warm.flow.core.entity.User;
import org.dromara.warm.flow.core.service.UserService;

import java.util.List;
import java.util.Set;

/**
 * 通用 工作流服务
 *
 * @author LionLi
 */
public interface IFlwCommonService {

    /**
     * 获取工作流用户service
     *
     * @return 工作流用户service
     */
    UserService getFlowUserService();

    /**
     * 构建工作流用户
     *
     * @param userList 办理用户
     * @param taskId   任务ID
     * @return 用户
     */
    Set<User> buildUser(List<User> userList, Long taskId);

    /**
     * 构建工作流用户
     *
     * @param userIdList 办理用户
     * @param taskId     任务ID
     * @return 用户
     */
    Set<User> buildFlowUser(List<String> userIdList, Long taskId);

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
     * 驳回
     *
     * @param message        审批意见
     * @param instanceId     流程实例id
     * @param targetNodeCode 目标节点
     * @param flowStatus     流程状态
     * @param flowHisStatus  节点操作状态
     */
    void backTask(String message, Long instanceId, String targetNodeCode, String flowStatus, String flowHisStatus);

    /**
     * 申请人节点编码
     *
     * @param definitionId 流程定义id
     * @return 申请人节点编码
     */
    String applyNodeCode(Long definitionId);

    /**
     * 删除运行中的任务
     *
     * @param taskIds 任务id
     */
    void deleteRunTask(List<Long> taskIds);
}
