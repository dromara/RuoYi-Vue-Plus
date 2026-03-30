package org.dromara.common.push.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 消息推送传输方式。
 *
 * @author Lion Li
 */
@Getter
@AllArgsConstructor
public enum MessageTransportEnum {

    /**
     * SSE 传输方式
     * 服务端推送事件，单向轻量传输
     */
    SSE("sse"),

    /**
     * WebSocket 传输方式
     * 全双工长连接，支持双向实时通信
     */
    WEBSOCKET("websocket");

    /**
     * 传输类型编码
     */
    private final String code;

    /**
     * 判断传输方式是否匹配
     *
     * @param transport 传输方式字符串
     * @return 是否匹配
     */
    public boolean matches(String transport) {
        return code.equalsIgnoreCase(transport);
    }

    /**
     * 根据传输类型字符串获取枚举
     * 找不到则默认返回 SSE
     *
     * @param transport 传输方式字符串
     * @return 对应的消息传输枚举
     */
    public static MessageTransportEnum of(String transport) {
        return Arrays.stream(values())
            .filter(item -> item.matches(transport))
            .findFirst()
            .orElse(SSE);
    }
}
