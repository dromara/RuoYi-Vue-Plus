package org.dromara.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 推送消息来源枚举
 *
 * @author Lion Li
 */
@Getter
@AllArgsConstructor
public enum PushSourceEnum {

    /**
     * 后端系统消息
     */
    BACKEND("backend"),

    /**
     * 通知公告
     */
    NOTICE("notice"),

    /**
     * 工作流
     */
    WORKFLOW("workflow"),

    /**
     * 大模型
     */
    LLM("llm"),

    /**
     * 客户端消息
     */
    CLIENT("client");

    private final String source;
}
