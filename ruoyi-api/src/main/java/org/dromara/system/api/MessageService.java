package org.dromara.system.api;

import org.dromara.system.api.domain.PushPayloadDTO;

import java.util.List;

/**
 * 通用 消息服务
 *
 * @author Lion Li
 */
public interface MessageService {

    /**
     * 发送指定用户文本消息
     *
     * @param userId  目标用户ID
     * @param message 文本消息内容
     */
    void sendMessage(Long userId, String message);

    /**
     * 全局广播文本消息
     *
     * @param message 文本消息内容
     */
    void sendMessage(String message);

    /**
     * 发送指定用户自定义消息体
     *
     * @param userId  目标用户ID
     * @param payload 消息推送体
     */
    void sendMessage(Long userId, PushPayloadDTO payload);

    /**
     * 全局广播自定义消息体
     *
     * @param payload 消息推送体
     */
    void sendMessage(PushPayloadDTO payload);

    /**
     * 批量发布消息给指定用户列表
     *
     * @param userIds 用户ID集合
     * @param payload 消息推送体
     */
    void publishMessage(List<Long> userIds, PushPayloadDTO payload);

    /**
     * 发布全局广播文本消息
     *
     * @param message 文本消息内容
     */
    void publishAll(String message);

    /**
     * 发布全局广播自定义消息体
     *
     * @param payload 消息推送体
     */
    void publishAll(PushPayloadDTO payload);

}
