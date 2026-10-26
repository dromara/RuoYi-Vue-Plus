package org.dromara.workflow.api.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 消息启动事件请求。
 */
@Data
public class StartMessageEventDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务主键，调用方必须提供。 */
    private String businessId;
    /** 流程定义编码。 */
    private String flowCode;
    /** 消息启动事件名称。 */
    private String eventName;
    /** 合并到流程变量的消息数据，可为空。 */
    private Map<String, Object> payload;
    /** 操作人，留空时使用系统处理人。 */
    private String handler;
}
