package org.dromara.workflow.domain.vo;

import org.dromara.workflow.domain.WfFormDefinition;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;



/**
 * 动态单与流程定义关联信息视图对象 wf_form_definition
 *
 * @author may
 * @date 2023-08-31
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
     * 动态表单id
     */
    @ExcelProperty(value = "动态表单id")
    private Long formId;

    /**
     * 流程定义id
     */
    @ExcelProperty(value = "流程定义key")
    private String processDefinitionKey;

    /**
     * 流程定义名称
     */
    @ExcelProperty(value = "流程定义名称")
    private String processDefinitionName;

    /**
     * 流程定义id
     */
    @ExcelProperty(value = "流程定义id")
    private String processDefinitionId;

    /**
     * 流程定义版本
     */
    @ExcelProperty(value = "流程定义版本")
    private Long processDefinitionVersion;


}
