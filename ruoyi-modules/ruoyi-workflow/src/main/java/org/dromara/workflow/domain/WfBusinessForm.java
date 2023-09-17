package org.dromara.workflow.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.dromara.common.tenant.core.TenantEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Map;

/**
 * 发起流程对象 wf_business_form
 *
 * @author may
 * @date 2023-09-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_business_form")
public class WfBusinessForm extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 申请编码
     */
    private String applyCode;

    /**
     * 表单id
     */
    private Long formId;

    /**
     * 表单名称
     */
    private String formName;

    /**
     * 表单内容
     */
    private String content;

    /**
     * 表单值
     */
    private String contentValue;

    /**
     * 流程变量
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String,Object> variable;

}
