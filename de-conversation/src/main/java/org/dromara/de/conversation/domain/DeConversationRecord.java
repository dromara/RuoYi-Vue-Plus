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
 * 数字员工对话消息记录实体
 *
 * 对应表：sai_agent_conversation_record
 *
 * @author AIP
 * @since 2026-09-22
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sai_agent_conversation_record")
public class DeConversationRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 智能体 ID */
    private Long agentId;

    /** 对话 ID */
    private String conversationId;

    /** 用户 ID */
    private Long userId;

    /** 角色 (user/assistant) */
    private String role;

    /** 消息内容 */
    private String content;

    /** 思考过程 (仅 assistant) */
    private String thinking;

    /** 消息扩展元数据 JSON */
    private String metadata;

    /** 状态 (1=成功,2=失败,3=进行中) */
    private Integer status;

    /** 输入 Token 数 */
    private Integer inputTokens;

    /** 输出 Token 数 */
    private Integer outputTokens;

    /** 缓存命中 Token 数 */
    private Integer cacheTokens;

    /** 创建时间 */
    private LocalDateTime createDt;
}