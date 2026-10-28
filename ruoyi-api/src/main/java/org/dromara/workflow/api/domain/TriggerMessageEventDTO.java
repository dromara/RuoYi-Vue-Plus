package org.dromara.workflow.api.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 触发实例中等待的消息事件请求。
 */
@Data
public class TriggerMessageEventDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 已存在的流程实例 ID。
     */
    private Long instanceId;

    /**
     * 等待中的消息捕获或消息边界事件名称。
     */
    private String eventName;

    /**
     * 合并到流程变量的消息数据，可为空。
     */
    private Map<String, Object> payload;

    /**
     * 操作人，留空时使用系统处理人。
     */
    private String handler;
}
