package org.dromara.common.push.core;

import org.dromara.common.core.domain.dto.PushPayloadDTO;
import org.dromara.common.push.dto.PushDTO;

import java.util.function.Consumer;

/**
 * 统一推送会话管理器。
 *
 * @author Lion Li
 */
public interface PushSessionManager {

    /**
     * 订阅消息通道
     * 注册消息消费者，用于监听并处理消息推送事件
     *
     * @param consumer 消息消费逻辑
     */
    void subscribeMessage(Consumer<PushDTO> consumer);

    /**
     * 发送消息给指定用户
     *
     * @param userId  目标用户ID
     * @param payload 消息体
     */
    void sendMessage(Long userId, PushPayloadDTO payload);

    /**
     * 全局广播消息（所有在线用户）
     *
     * @param payload 消息体
     */
    void sendMessage(PushPayloadDTO payload);

    /**
     * 批量发布消息给指定用户列表
     *
     * @param pushDTO 推送参数封装对象
     */
    void publishMessage(PushDTO pushDTO);

    /**
     * 全局广播消息（所有用户）
     *
     * @param payload 消息体
     */
    void publishAll(PushPayloadDTO payload);
}
