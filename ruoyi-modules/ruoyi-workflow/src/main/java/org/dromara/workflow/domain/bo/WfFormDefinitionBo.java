package org.dromara.workflow.domain.bo;

import org.dromara.workflow.domain.WfFormDefinition;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;

/**
 * 动态单与流程定义关联信息业务对象 wf_form_definition
 *
 * @author may
 * @date 2023-08-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = WfFormDefinition.class, reverseConvertGenerate = false)
public class WfFormDefinitionBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 动态表单id
     */
    @NotNull(message = "动态表单id不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long formId;

    /**
     * 流程定义id
     */
    @NotBlank(message = "流程定义key不能为空", groups = { AddGroup.class, EditGroup.class })
    private String processDefinitionKey;

    /**
     * 流程定义名称
     */
    @NotBlank(message = "流程定义名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String processDefinitionName;

    /**
     * 流程定义id
     */
    @NotBlank(message = "流程定义id不能为空", groups = { AddGroup.class, EditGroup.class })
    private String processDefinitionId;

    /**
     * 流程定义版本
     */
    @NotNull(message = "流程定义版本不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long processDefinitionVersion;


}
