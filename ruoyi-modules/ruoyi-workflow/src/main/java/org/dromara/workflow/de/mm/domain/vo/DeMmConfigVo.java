package org.dromara.workflow.de.mm.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * MM 域记忆配置读 VO（MM-01：sai_agent.memory_enabled + 载荷覆盖统计）。
 *
 * @author AIP
 */
@Data
public class DeMmConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sai_agent.id */
    private Long agentId;

    /** sai_agent.memory_enabled（0 关 / 1 开） */
    private Boolean memoryEnabled;

    /** 该 Agent 记忆记录总条数 */
    private Integer totalRecords;

    /** 带 metadata 载荷的记录条数（MM-01 记忆范围位 / MM-03 使用回显的覆盖面） */
    private Long recordsWithMetadata;

    /** 去重会话数 */
    private Integer conversationCount;

    /** 去重用户数（MM-07 归属分布） */
    private Integer userCount;
}
