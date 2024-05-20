package org.dromara.workflow.domain.vo;

import org.dromara.workflow.domain.WfNodeConfig;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * 节点配置视图对象 wf_node_config
 *
 * @author may
 * @date 2024-03-30
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = WfNodeConfig.class)
public class WfNodeConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    private Long id;

    /**
     * 表单id
     */
    @ExcelProperty(value = "表单id")
    private Long formId;

    /**
     * 表单类型
     */
    @ExcelProperty(value = "表单类型")
    private String formType;

    /**
     * 节点名称
     */
    @ExcelProperty(value = "节点名称")
    private String nodeName;

    /**
     * 节点id
     */
    @ExcelProperty(value = "节点id")
    private String nodeId;

    /**
     * 流程定义id
     */
    @ExcelProperty(value = "流程定义id")
    private String definitionId;

    /**
     * 是否为申请人节点 （0是 1否）
     */
    @ExcelProperty(value = "是否为申请人节点 （0是 1否）")
    private String applyUserTask;

    /**
     * 表单管理
     */
    private WfFormManageVo wfFormManageVo;


}
