package org.dromara.system.api.domain;

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
public class PushPayloadDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息记录ID
     */
    private Long messageId;

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

    /**
     * 构建推送消息体，缺省消息类型与来源时使用系统默认值。
     *
     * @param type 消息类型
     * @param source 消息来源
     * @param message 文本消息
     * @param data 扩展数据
     * @return 推送消息体
     */
    public static PushPayloadDTO of(String type, String source, String message, Object data) {
        PushPayloadDTO payload = new PushPayloadDTO();
        payload.setType(StringUtils.defaultIfBlank(type, PushTypeEnum.MESSAGE.getType()));
        payload.setSource(StringUtils.defaultIfBlank(source, PushSourceEnum.BACKEND.getSource()));
        payload.setMessage(message);
        payload.setData(data);
        payload.setTimestamp(System.currentTimeMillis());
        return payload;
    }

    /**
     * 通过枚举值构建推送消息体。
     *
     * @param type 消息类型枚举
     * @param source 消息来源枚举
     * @param message 文本消息
     * @param data 扩展数据
     * @return 推送消息体
     */
    public static PushPayloadDTO of(PushTypeEnum type, PushSourceEnum source, String message, Object data) {
        return of(
            type == null ? null : type.getType(),
            source == null ? null : source.getSource(),
            message,
            data
        );
    }

    /**
     * 构建带前端跳转路径的推送消息体。
     *
     * @param type 消息类型枚举
     * @param source 消息来源枚举
     * @param message 文本消息
     * @param data 扩展数据
     * @param path 前端跳转路径
     * @return 推送消息体
     */
    public static PushPayloadDTO of(PushTypeEnum type, PushSourceEnum source, String message, Object data, String path) {
        PushPayloadDTO payload = of(type, source, message, data);
        payload.setPath(path);
        return payload;
    }
}
