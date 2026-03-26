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

    SSE("sse"),
    WEBSOCKET("websocket");

    private final String code;

    public boolean matches(String transport) {
        return code.equalsIgnoreCase(transport);
    }

    public static MessageTransportEnum of(String transport) {
        return Arrays.stream(values())
            .filter(item -> item.matches(transport))
            .findFirst()
            .orElse(SSE);
    }
}
