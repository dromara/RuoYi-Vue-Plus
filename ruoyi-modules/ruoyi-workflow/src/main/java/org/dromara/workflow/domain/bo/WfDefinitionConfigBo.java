package org.dromara.workflow.domain.bo;

import org.dromara.workflow.domain.WfDefinitionConfig;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;

/**
 * 流程定义配置业务对象 wf_form_definition
 *
 * @author may
 * @date 2024-03-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = WfDefinitionConfig.class, reverseConvertGenerate = false)
public class WfDefinitionConfigBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 表单ID
     */
    @NotNull(message = "表单ID不能为空", groups = {AddGroup.class})
    private Long formId;

    /**
     * 流程定义ID
     */
    @NotBlank(message = "流程定义ID不能为空", groups = {AddGroup.class})
    private String definitionId;

    /**
     * 流程KEY
     */
    @NotBlank(message = "流程KEY不能为空", groups = {AddGroup.class})
    private String processKey;

    /**
     * 备注
     */
    private String remark;


}
