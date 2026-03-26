package org.dromara.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 推送消息类型枚举
 *
 * @author Lion Li
 */
@Getter
@AllArgsConstructor
public enum PushTypeEnum {

    /**
     * 通用消息
     */
    MESSAGE("message"),

    /**
     * 通知公告
     */
    NOTICE("notice"),

    /**
     * 大模型消息
     */
    LLM("llm"),

    /**
     * 自定义消息
     */
    CUSTOM("custom");

    private final String type;
}
