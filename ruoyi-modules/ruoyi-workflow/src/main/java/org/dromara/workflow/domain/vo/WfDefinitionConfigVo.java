package org.dromara.workflow.domain.vo;

import org.dromara.workflow.domain.WfDefinitionConfig;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 流程定义配置视图对象 wf_definition_config
 *
 * @author may
 * @date 2024-03-18
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = WfDefinitionConfig.class)
public class WfDefinitionConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 表名
     */
    @ExcelProperty(value = "表名")
    private String tableName;

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
     * 流程版本
     */
    @ExcelProperty(value = "流程版本")
    private Integer version;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 表单管理
     */
    private WfFormManageVo wfFormManageVo;


}
