package org.dromara.common.core.domain.dto;

import lombok.Data;
import org.dromara.common.core.enums.PushSourceEnum;
import org.dromara.common.core.enums.PushTypeEnum;
import org.dromara.common.core.utils.StringUtils;

import java.io.Serial;
import java.io.Serializable;

/**
 * 推送给前端的统一消息体
 *
 * @author Lion Li
 */
@Data
public class PushPayload implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型
     */
    private String type;

    /**
     * 消息来源
     */
    private String source;

    /**
     * 文本消息
     */
    private String message;

    /**
     * 扩展数据
     */
    private Object data;

    /**
     * 前端跳转路径
     */
    private String path;

    /**
     * 时间戳
     */
    private Long timestamp;

    public static PushPayload of(String type, String source, String message, Object data) {
        PushPayload payload = new PushPayload();
        payload.setType(StringUtils.defaultIfBlank(type, PushTypeEnum.MESSAGE.getType()));
        payload.setSource(StringUtils.defaultIfBlank(source, PushSourceEnum.BACKEND.getSource()));
        payload.setMessage(message);
        payload.setData(data);
        payload.setTimestamp(System.currentTimeMillis());
        return payload;
    }

    public static PushPayload of(PushTypeEnum type, PushSourceEnum source, String message, Object data) {
        return of(
            type == null ? null : type.getType(),
            source == null ? null : source.getSource(),
            message,
            data
        );
    }

    public static PushPayload of(PushTypeEnum type, PushSourceEnum source, String message, Object data, String path) {
        PushPayload payload = of(type, source, message, data);
        payload.setPath(path);
        return payload;
    }
}
