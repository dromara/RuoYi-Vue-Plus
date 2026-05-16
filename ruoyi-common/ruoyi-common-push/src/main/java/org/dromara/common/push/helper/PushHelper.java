package org.dromara.common.push.helper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.enums.PushSourceEnum;
import org.dromara.common.core.enums.PushTypeEnum;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.push.core.PushSessionManager;
import org.dromara.common.push.dto.PushDTO;
import org.dromara.system.api.domain.PushPayloadDTO;

import java.util.List;

/**
 * 统一消息推送工具。
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PushHelper {

    /**
     * 发送指定用户文本消息
     *
     * @param userId  目标用户ID
     * @param message 文本消息内容
     */
    public static void sendMessage(Long userId, String message) {
        sendMessage(userId, buildMessage(message));
    }

    /**
     * 全局广播文本消息
     *
     * @param message 文本消息内容
     */
    public static void sendMessage(String message) {
        sendMessage(buildMessage(message));
    }

    /**
     * 发送指定用户自定义消息体
     *
     * @param userId  目标用户ID
     * @param payload 消息推送体
     */
    public static void sendMessage(Long userId, PushPayloadDTO payload) {
        if (!isEnabled()) {
            return;
        }
        getSessionManager().sendMessage(userId, payload);
    }

    /**
     * 全局广播自定义消息体
     *
     * @param payload 消息推送体
     */
    public static void sendMessage(PushPayloadDTO payload) {
        if (!isEnabled()) {
            return;
        }
        getSessionManager().sendMessage(payload);
    }

    /**
     * 批量发布消息给指定用户列表
     *
     * @param userIds 用户ID集合
     * @param payload 消息推送体
     */
    public static void publishMessage(List<Long> userIds, PushPayloadDTO payload) {
        publishMessage(PushDTO.of(userIds, payload));
    }

    /**
     * 批量发布消息（使用完整推送DTO）
     *
     * @param dto 推送参数封装对象
     */
    public static void publishMessage(PushDTO dto) {
        if (!isEnabled() || dto == null || dto.getPayload() == null) {
            return;
        }
        getSessionManager().publishMessage(dto);
    }

    /**
     * 发布全局广播文本消息
     *
     * @param message 文本消息内容
     */
    public static void publishAll(String message) {
        publishAll(buildMessage(message));
    }

    /**
     * 发布全局广播自定义消息体
     *
     * @param payload 消息推送体
     */
    public static void publishAll(PushPayloadDTO payload) {
        if (!isEnabled()) {
            return;
        }
        getSessionManager().publishAll(payload);
    }

    /**
     * 判断消息推送功能是否开启
     * 读取配置：message.enabled
     *
     * @return 是否开启推送
     */
    public static boolean isEnabled() {
        return Boolean.TRUE.equals(SpringUtils.getProperty("message.enabled", Boolean.class, Boolean.TRUE));
    }

    /**
     * 获取推送会话管理器Bean
     *
     * @return PushSessionManager 实例
     */
    private static PushSessionManager getSessionManager() {
        return SpringUtils.getBean(PushSessionManager.class);
    }

    /**
     * 构建默认格式的消息推送体
     *
     * @param message 消息内容
     * @return 封装好的 PushPayloadDTO
     */
    private static PushPayloadDTO buildMessage(String message) {
        return PushPayloadDTO.of(PushTypeEnum.MESSAGE, PushSourceEnum.BACKEND, message, null);
    }

}
