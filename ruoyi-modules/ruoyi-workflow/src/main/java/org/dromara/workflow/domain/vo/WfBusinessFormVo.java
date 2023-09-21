package org.dromara.workflow.domain.vo;

import org.dromara.workflow.domain.WfBusinessForm;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 发起流程视图对象 wf_business_form
 *
 * @author may
 * @date 2023-09-16
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = WfBusinessForm.class)
public class WfBusinessFormVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 申请编码
     */
    @ExcelProperty(value = "申请编码")
    private String applyCode;

    /**
     * 表单id
     */
    @ExcelProperty(value = "表单id")
    private Long formId;

    /**
     * 表单名称
     */
    @ExcelProperty(value = "表单名称")
    private String formName;

    /**
     * 表单内容
     */
    @ExcelProperty(value = "表单内容")
    private String content;

    /**
     * 表单值
     */
    @ExcelProperty(value = "表单值")
    private String contentValue;

    /**
     * 动态单与流程定义关联信息业务对象
     */
    private WfFormDefinitionVo wfFormDefinitionVo;

    /**
     * 流程实例对象
     */
    private ProcessInstanceVo processInstanceVo;

}
