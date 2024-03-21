package org.dromara.workflow.domain.vo;

import org.dromara.workflow.domain.WfFormDefinition;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;



/**
 * 表单配置视图对象 wf_form_definition
 *
 * @author gssong
 * @date 2024-03-18
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = WfFormDefinition.class)
public class WfFormDefinitionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 路由地址
     */
    @ExcelProperty(value = "路由地址")
    private String path;

    /**
     * 流程定义ID
     */
    @ExcelProperty(value = "流程定义ID")
    private String definitionId;

    /**
     * 流程KEY
     */
    @ExcelProperty(value = "流程KEY")
    private String processKey;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
