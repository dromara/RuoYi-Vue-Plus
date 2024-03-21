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
 * 表单配置业务对象 wf_form_definition
 *
 * @author gssong
 * @date 2024-03-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = WfFormDefinition.class, reverseConvertGenerate = false)
public class WfFormDefinitionBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 路由地址
     */
    @NotBlank(message = "路由地址不能为空", groups = {AddGroup.class})
    private String path;

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
