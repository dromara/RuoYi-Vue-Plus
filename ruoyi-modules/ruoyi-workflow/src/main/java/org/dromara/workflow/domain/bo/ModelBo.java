package org.dromara.workflow.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.workflow.common.PageEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 模型请求对象
 *
 * @author may
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModelBo extends PageEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空", groups = {AddGroup.class})
    private String name;

    /**
     * 模型标识key
     */
    @NotBlank(message = "模型标识key不能为空", groups = {AddGroup.class})
    private String key;

    /**
     * 备注
     */
    private String description;

}
