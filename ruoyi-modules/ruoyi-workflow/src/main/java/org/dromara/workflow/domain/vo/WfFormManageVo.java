package org.dromara.workflow.domain.vo;

import org.dromara.workflow.domain.WfFormManage;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 表单管理视图对象 wf_form_manage
 *
 * @author may
 * @date 2024-03-29
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = WfFormManage.class)
public class WfFormManageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 表单名称
     */
    @ExcelProperty(value = "表单名称")
    private String formName;

    /**
     * 表单类型
     */
    @ExcelProperty(value = "表单类型")
    private String formType;

    /**
     * 表单类型名称
     */
    private String formTypeName;

    /**
     * 路由地址/表单ID
     */
    @ExcelProperty(value = "路由地址/表单ID")
    private String router;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;


}
