package org.dromara.workflow.de.br.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 11 条 BR 校验的通用 ack 响应。
 *
 * <p>每条 BR 的额外字段通过 {@code extras} 透传（如 BR-05 的 affectedAgentCount、
 * BR-08 的 conflicts / rollbackHint、BR-18 的 messagePrefix 等），
 * 避免生成 11 个独立 VO；owner 接 sys_menu 后若有强类型需求可拆分。
 */
@Data
public class BrComplianceAckVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** BR 编号（BR-01 / BR-03 / ... / BR-18） */
    private String brId;

    /** true = 校验通过 / 状态机合法；false = 不通过（caller 应阻止落库） */
    private Boolean validated;

    /** 简短描述（人类可读） */
    private String description;

    /** BR 专用附加字段（affectedAgentCount / resolvedNodeType / messagePrefix 等） */
    private Map<String, Object> extras = new HashMap<>();
}
