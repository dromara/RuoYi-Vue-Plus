package org.dromara.workflow.domain;

import org.dromara.common.tenant.core.TenantEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 表单管理对象 wf_form_manage
 *
 * @author may
 * @date 2024-03-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_form_manage")
public class WfFormManage extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 表单名称
     */
    private String formName;

    /**
     * 表单类型
     */
    private String formType;

    /**
     * 路由地址/表单ID
     */
    private String router;

    /**
     * 备注
     */
    private String remark;


}
