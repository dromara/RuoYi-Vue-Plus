package org.dromara.workflow.de.br.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * BR-03 岗位配置包导入 / 导出 / 模板复制 / 受控分享请求。
 *
 * <p>落点：{@code sai_agent.config}（TEXT）。
 */
@Data
public class DeAgentConfigBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sai_agent.id */
    @NotNull(message = "agentId 不能为空")
    private Long agentId;

    /** 配置包 JSON 字符串（写入 sai_agent.config 或从中导出） */
    @NotBlank(message = "payload 不能为空")
    private String payload;

    /** 操作类型 */
    @NotBlank(message = "operation 不能为空")
    private String operation; // IMPORT | EXPORT | CLONE | SHARE
}
