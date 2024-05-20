package org.dromara.workflow.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 模型视图对象
 *
 * @author may
 */
@Data
public class ModelVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模型id
     */
    private String id;

    /**
     * 模型名称
     */
    private String name;

    /**
     * 模型标识key
     */
    private String key;

    /**
     * 模型分类
     */
    private String categoryCode;

    /**
     * 模型XML
     */
    private String xml;

    /**
     * 备注
     */
    private String description;
}
