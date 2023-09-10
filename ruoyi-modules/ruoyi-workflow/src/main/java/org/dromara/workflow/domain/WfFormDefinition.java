package org.dromara.workflow.domain;

import org.dromara.common.tenant.core.TenantEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 动态单与流程定义关联信息对象 wf_form_definition
 *
 * @author may
 * @date 2023-08-31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_form_definition")
public class WfFormDefinition extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 动态表单id
     */
    private Long formId;

    /**
     * 流程定义key
     */
    private String processDefinitionKey;

    /**
     * 流程定义名称
     */
    private String processDefinitionName;

    /**
     * 流程定义id
     */
    private String processDefinitionId;

    /**
     * 流程定义版本
     */
    private Long processDefinitionVersion;


}
