package org.dromara.workflow.api.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 手动发起定时启动事件请求。
 */
@Data
public class StartTimerEventDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务主键，留空时生成数字雪花 ID。 */
    private String businessId;
    /** 已发布的流程定义 ID。 */
    private Long definitionId;
    /** 定时启动事件节点编码。 */
    private String nodeCode;
    /** 合并到流程变量的业务数据，可为空。 */
    private Map<String, Object> payload;
    /** 操作人，留空时使用系统处理人。 */
    private String handler;
}
