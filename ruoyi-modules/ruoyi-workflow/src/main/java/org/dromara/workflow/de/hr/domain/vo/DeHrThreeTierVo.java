package org.dromara.workflow.de.hr.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * HR-04 三分位落点响应 VO（2026-09-23）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
public class DeHrThreeTierVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 实例 ID */
    private Long instanceId;

    /** 任务 ID */
    private Long taskId;

    /** 三分位 JSON 字符串（解析自 flow_instance.variable.de.ctx.hr.three_tier） */
    private String threeTierJson;

    /** 审批人（type=1） */
    private String approver;

    /** 转办人（type=2） */
    private String transferTo;

    /** 委托人（type=3） */
    private String delegateTo;
}