package org.dromara.workflow.de.fl.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

/**
 * FL-03 执行方案 BO。
 *
 * @author AIP
 */
@Data
public class DeFlExecuteBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    private Long taskId;

    /** 串行 / 并行 / 条件分支 / 汇总 / 人工确认 */
    @NotBlank
    private String structure;

    /** 输入 JSON 字符串 */
    private String inputJson;
}