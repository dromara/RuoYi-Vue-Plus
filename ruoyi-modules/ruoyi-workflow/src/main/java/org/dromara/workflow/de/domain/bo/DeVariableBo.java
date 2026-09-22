package org.dromara.workflow.de.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新实例运行态请求 BO（对应 PUT /de/binding/variable）。
 *
 * <p>value 字段是完整 de.ctx JSON 字符串（不是顶层裸键）；service 内做形状校验。
 *
 * @author AIP
 */
@Data
public class DeVariableBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** flow_instance.id（必填） */
    @NotNull(message = "实例 ID 不能为空")
    private Long instanceId;

    /** de.ctx JSON 字符串（必填；由 service 校验形状） */
    @NotBlank(message = "variable 值不能为空")
    private String value;
}