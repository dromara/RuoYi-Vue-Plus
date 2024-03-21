package org.dromara.workflow.domain;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 表单配置对象 wf_form_definition
 *
 * @author gssong
 * @date 2024-03-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_form_definition")
public class WfFormDefinition extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 流程定义ID
     */
    private String definitionId;

    /**
     * 流程KEY
     */
    private String processKey;

    /**
     * 备注
     */
    private String remark;


}
