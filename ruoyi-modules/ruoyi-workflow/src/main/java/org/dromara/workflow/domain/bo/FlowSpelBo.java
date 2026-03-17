package org.dromara.workflow.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.workflow.domain.FlowSpel;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 流程spel表达式定义业务对象 flow_spel
 *
 * @author Michelle.Chung
 * @date 2025-07-04
 */
@Data
@AutoMapper(target = FlowSpel.class, reverseConvertGenerate = false)
public class FlowSpelBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    private Long id;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 参数
     */
    private String methodParams;

    /**
     * 预览spel值
     */
    @NotBlank(message = "预览spel值不能为空", groups = { AddGroup.class, EditGroup.class })
    private String viewSpel;

    /**
     * 状态（0正常 1停用）
     */
    @NotBlank(message = "状态（0正常 1停用）不能为空", groups = { AddGroup.class, EditGroup.class })
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 请求参数
     */
    private Map<String, Object> params = new HashMap<>();

}
