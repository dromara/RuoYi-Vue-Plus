package org.dromara.de.conversation.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息响应 VO
 *
 * @author AIP
 * @since 2026-09-22
 */
@Data
public class MessageVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息记录 ID */
    private Long recordId;

    /** 对话 ID */
    private String conversationId;

    /** 角色 */
    private String role;

    /** 创建时间 */
    private LocalDateTime createDt;
}