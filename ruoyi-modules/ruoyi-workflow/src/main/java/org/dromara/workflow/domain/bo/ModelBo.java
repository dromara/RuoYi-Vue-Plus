package org.dromara.workflow.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.workflow.common.constant.FlowConstant;

import java.io.Serial;
import java.io.Serializable;

/**
 * 模型请求对象
 *
 * @author may
 */
@Data
public class ModelBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模型id
     */
    @NotBlank(message = "模型ID不能为空", groups = {EditGroup.class})
    private String id;

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空", groups = {AddGroup.class})
    private String name;

    /**
     * 模型标识key
     */
    @NotBlank(message = "模型标识key不能为空", groups = {AddGroup.class})
    @Pattern(regexp = FlowConstant.MODEL_KEY_PATTERN, message = "模型标识key只能字符或者下划线开头", groups = {AddGroup.class})
    private String key;

    /**
     * 模型分类
     */
    @NotBlank(message = "模型分类不能为空", groups = {AddGroup.class})
    private String categoryCode;

    /**
     * 模型XML
     */
    @NotBlank(message = "模型XML不能为空", groups = {AddGroup.class})
    private String xml;

    /**
     * 模型SVG图片
     */
    @NotBlank(message = "模型SVG不能为空", groups = {EditGroup.class})
    private String svg;

    /**
     * 备注
     */
    private String description;

}
