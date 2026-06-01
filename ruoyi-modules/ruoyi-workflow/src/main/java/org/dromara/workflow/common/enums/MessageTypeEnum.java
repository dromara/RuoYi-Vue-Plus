package org.dromara.workflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 消息类型枚举，定义流程通知支持的消息通道。
 *
 * @author may
 */
@Getter
@AllArgsConstructor
public enum MessageTypeEnum {

    /**
     * 站内信
     */
    SYSTEM_MESSAGE("1", "站内信"),

    /**
     * 邮箱
     */
    EMAIL_MESSAGE("2", "邮箱"),

    /**
     * 短信
     */
    SMS_MESSAGE("3", "短信");

    /**
     * 消息类型编码。
     */
    private final String code;

    /**
     * 消息类型描述。
     */
    private final String desc;

    /**
     * 消息类型编码缓存。
     */
    private static final Map<String, MessageTypeEnum> MESSAGE_TYPE_ENUM_MAP = Arrays.stream(values())
        .collect(Collectors.toConcurrentMap(MessageTypeEnum::getCode, Function.identity()));

    /**
     * 根据消息类型编码获取枚举实例。
     *
     * @param code 消息类型code
     * @return MessageTypeEnum
     */
    public static MessageTypeEnum getByCode(String code) {
        return MESSAGE_TYPE_ENUM_MAP.getOrDefault(code, null);
    }

}
