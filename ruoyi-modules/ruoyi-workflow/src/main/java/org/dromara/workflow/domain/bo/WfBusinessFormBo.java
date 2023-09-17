package org.dromara.workflow.domain.bo;

import org.dromara.workflow.domain.WfBusinessForm;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;

/**
 * 发起流程业务对象 wf_business_form
 *
 * @author may
 * @date 2023-09-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = WfBusinessForm.class, reverseConvertGenerate = false)
public class WfBusinessFormBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 申请编码
     */
    private String applyCode;

    /**
     * 表单id
     */
    @NotNull(message = "表单id不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long formId;

    /**
     * 表单名称
     */
    @NotBlank(message = "表单名称不能为空", groups = { AddGroup.class, EditGroup.class })
    private String formName;

    /**
     * 表单内容
     */
    @NotBlank(message = "表单内容不能为空", groups = { AddGroup.class, EditGroup.class })
    private String content;

    /**
     * 表单值
     */
    private String contentValue;


}
