package org.dromara.common.sse.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 消息的dto
 *
 * @author zendwang
 */
@Data
public class SseMessageDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 需要推送到的session key 列表
     */
    private List<Long> userIds;

    /**
     * 需要发送的消息
     */
    private String message;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息来源
     */
    private String messageSource;

    /**
     * 扩展数据
     */
    private Object data;

    /**
     * 前端跳转路径
     */
    private String path;

    /**
     * 前端跳转参数
     */
    private Map<String, Object> query;
}
