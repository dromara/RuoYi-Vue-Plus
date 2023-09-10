package org.dromara.workflow.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.workflow.domain.WfForm;

import java.io.Serial;

/**
 * 流程表单业务对象
 *
 * @author KonBAI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = WfForm.class, reverseConvertGenerate = false)
public class WfFormBo extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 表单主键
     */
    @NotNull(message = "表单ID不能为空", groups = { EditGroup.class })
    private Long formId;

    /**
     * 表单名称
     */
    @NotBlank(message = "表单名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String formName;

    /**
     * 表单配置
     */
    @NotBlank(message = "表单配置不能为空", groups = { AddGroup.class, EditGroup.class })
    private String formConfig;

    /**
     * 表单内容
     */
    @NotBlank(message = "表单内容不能为空", groups = { AddGroup.class, EditGroup.class })
    private String content;

    /**
     * 状态（0正常 1停用）
     */
    @NotBlank(message = "状态不能为空", groups = { AddGroup.class, EditGroup.class })
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 动态单与流程定义关联信息业务对象
     */
    private WfFormDefinitionBo wfFormDefinitionBo;
}
