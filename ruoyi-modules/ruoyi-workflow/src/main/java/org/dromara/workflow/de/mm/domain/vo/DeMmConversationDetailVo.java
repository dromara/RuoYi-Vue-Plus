package org.dromara.workflow.de.mm.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * MM 域单会话上下文回溯 VO（MM-03 / MM-04：全量历史 + 摘要）。
 *
 * @author AIP
 */
@Data
public class DeMmConversationDetailVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 会话 ID（sai_agent_conversation_record.conversation_id） */
    private String conversationId;

    /** 归属 Agent（首条记录的 agent_id） */
    private Long agentId;

    /** 归属用户（首条记录的 user_id） */
    private Long userId;

    /** 记录总条数 */
    private Integer recordCount;

    /** 角色分布：role → 条数 */
    private Map<String, Long> roleDistribution;

    /** 状态分布：status(1/2/3) → 条数 */
    private Map<Integer, Long> statusDistribution;

    /** 带 metadata 载荷的记录条数 */
    private Long recordsWithMetadata;

    /** 首条内容预览（≤200 字符） */
    private String firstContent;

    /** 末条内容预览（≤200 字符） */
    private String lastContent;

    /** 末条角色 */
    private String lastRole;

    /** 全量历史（按 id 升序） */
    private List<DeMmMemoryVo> records;
}
