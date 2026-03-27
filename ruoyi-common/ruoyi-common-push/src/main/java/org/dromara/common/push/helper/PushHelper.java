package org.dromara.common.push.helper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.domain.dto.PushPayloadDTO;
import org.dromara.common.core.enums.PushSourceEnum;
import org.dromara.common.core.enums.PushTypeEnum;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.push.core.PushSessionManager;
import org.dromara.common.push.dto.PushDTO;

import java.util.List;

/**
 * 统一消息推送工具。
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PushHelper {

    public static void sendMessage(Long userId, String message) {
        sendMessage(userId, buildMessage(message));
    }

    public static void sendMessage(String message) {
        sendMessage(buildMessage(message));
    }

    public static void sendMessage(Long userId, PushPayloadDTO payload) {
        if (!isEnabled()) {
            return;
        }
        getSessionManager().sendMessage(userId, payload);
    }

    public static void sendMessage(PushPayloadDTO payload) {
        if (!isEnabled()) {
            return;
        }
        getSessionManager().sendMessage(payload);
    }

    public static void publishMessage(List<Long> userIds, PushPayloadDTO payload) {
        PushDTO dto = new PushDTO();
        dto.setUserIds(userIds);
        dto.setPayload(payload);
        publishMessage(dto);
    }

    public static void publishMessage(PushDTO dto) {
        if (!isEnabled() || dto == null || dto.getPayload() == null) {
            return;
        }
        getSessionManager().publishMessage(dto);
    }

    public static void publishAll(String message) {
        publishAll(buildMessage(message));
    }

    public static void publishAll(PushPayloadDTO payload) {
        if (!isEnabled()) {
            return;
        }
        getSessionManager().publishAll(payload);
    }

    public static boolean isEnabled() {
        return Boolean.TRUE.equals(SpringUtils.getProperty("message.enabled", Boolean.class, Boolean.TRUE));
    }

    private static PushSessionManager getSessionManager() {
        return SpringUtils.getBean(PushSessionManager.class);
    }

    private static PushPayloadDTO buildMessage(String message) {
        return PushPayloadDTO.of(PushTypeEnum.MESSAGE, PushSourceEnum.BACKEND, message, null);
    }
}
