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

    void subscribeMessage(Consumer<PushDTO> consumer);

    void sendMessage(Long userId, PushPayloadDTO payload);

    void sendMessage(PushPayloadDTO payload);

    void publishMessage(PushDTO pushDTO);

    void publishAll(PushPayloadDTO payload);
}
