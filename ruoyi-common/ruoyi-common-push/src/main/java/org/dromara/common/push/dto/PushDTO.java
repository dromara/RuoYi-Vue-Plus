package org.dromara.common.push.dto;

import lombok.Data;
import org.dromara.system.api.domain.PushPayloadDTO;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 统一推送 DTO。
 *
 * @author Lion Li
 */
@Data
public class PushDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 目标用户 ID 列表，为空表示广播。
     */
    private List<Long> userIds;

    /**
     * 推送消息体。
     */
    private PushPayloadDTO payload;

    /**
     * 构建指定用户推送消息。
     *
     * @param userIds 目标用户 ID 列表
     * @param payload 推送消息体
     * @return 推送 DTO
     */
    public static PushDTO of(List<Long> userIds, PushPayloadDTO payload) {
        PushDTO dto = new PushDTO();
        dto.setUserIds(userIds);
        dto.setPayload(payload);
        return dto;
    }

    /**
     * 构建广播推送消息。
     *
     * @param payload 推送消息体
     * @return 推送 DTO
     */
    public static PushDTO broadcast(PushPayloadDTO payload) {
        return of(null, payload);
    }
}
