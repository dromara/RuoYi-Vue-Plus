package org.dromara.de.conversation.bo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建对话请求 BO
 *
 * @author AIP
 * @since 2026-09-22
 */
@Data
public class ConversationCreateBo {

    /** 流程实例 ID */
    @NotNull
    private Long instanceId;

    /** 数字员工 ID */
    @NotNull
    private Long agentId;

    /** 对话标题 */
    @Size(max = 255)
    private String title;
}