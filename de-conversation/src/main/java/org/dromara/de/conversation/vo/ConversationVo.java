package org.dromara.de.conversation.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话响应 VO
 *
 * @author AIP
 * @since 2026-09-22
 */
@Data
public class ConversationVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 对话 ID (UUID) */
    private String conversationId;

    /** 数字员工 ID */
    private Long agentId;

    /** 用户 ID */
    private Long userId;

    /** 对话标题 */
    private String title;

    /** 创建时间 */
    private LocalDateTime createDt;
}