package org.dromara.workflow.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.workflow.common.PageEntity;

import java.io.Serial;
import java.io.Serializable;

/**
 * 流程定义请求对象
 *
 * @author may
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessDefinitionBo extends PageEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 流程定义名称key
     */
    private String key;

    /**
     * 流程定义名称
     */
    private String name;

    /**
     * 模型分类
     */
    private String categoryCode;

}
