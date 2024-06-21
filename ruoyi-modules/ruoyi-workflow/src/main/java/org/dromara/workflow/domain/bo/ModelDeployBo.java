package org.dromara.workflow.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程定义请求对象
 *
 * @author may
 */
@Data
public class ModelDeployBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "模型ID不能为空")
    private String modelId;

    private String tenantId;
}
