package org.dromara.workflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息类型枚举
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

    private final String code;

    private final String desc;
}

