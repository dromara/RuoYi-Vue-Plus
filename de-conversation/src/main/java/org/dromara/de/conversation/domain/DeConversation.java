package org.dromara.de.conversation.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数字员工对话实体
 *
 * 对应表：sai_agent_conversation
 *
 * @author AIP
 * @since 2026-09-22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sai_agent_conversation")
public class DeConversation implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 智能体 ID */
    private Long agentId;

    /** 用户 ID */
    private Long userId;

    /**
     * 锚定流程实例ID（创建时锚定，完成时直用——S-02 正解 D-009，消灭「猜最新实例」）
     */
    private Long instanceId;

    /** 对话 ID (UUID) */
    private String conversationId;

    /** 对话标题 */
    private String title;

    /** 创建时间 */
    private LocalDateTime createDt;

    /** 更新时间 */
    private LocalDateTime updateDt;
}