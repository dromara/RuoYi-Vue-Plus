package org.dromara.workflow.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.workflow.domain.WfForm;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程分类视图对象
 *
 * @author KonBAI
 */
@Data
@AutoMapper(target = WfForm.class)
public class WfFormVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 表单主键
     */
    private Long formId;

    /**
     * 表单名称
     */
    private String formName;

    /**
     * 表单配置
     */
    private String formConfig;

    /**
     * 表单内容
     */
    private String content;

    /**
     * 备注
     */
    private String remark;
}
