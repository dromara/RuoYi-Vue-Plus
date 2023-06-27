package org.dromara.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 流程表单对象 wf_form
 *
 * @author KonBAI
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_form")
public class WfForm extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 表单主键
     */
    @TableId(value = "form_id")
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
