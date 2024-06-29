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
     * 是否会签,0非会签,1会签
     */
    @ExcelProperty(value = "是否会签,0非会签,1会签")
    private boolean multiple;

    /**
     * 会签保存人员KEY值
     */
    @ExcelProperty(value = "会签保存人员KEY值")
    private boolean multipleColumn;

    /**
     * 是否可退回到当前节点,0不可退回,1可退回
     */
    @ExcelProperty(value = "是否可退回到当前节点,0不可退回,1可退回")
    private boolean back;

    /**
     * 是否可委托,0不可委托,1可委托
     */
    @ExcelProperty(value = "是否可委托,0不可委托,1可委托")
    private boolean delegate;

    /**
     * 是否可转办,0不可转办,1可转办
     */
    @ExcelProperty(value = "是否可转办,0不可转办,1可转办")
    private boolean transfer;

    /**
     * 是否可抄送,0不可抄送,1可抄送
     */
    @ExcelProperty(value = "是否可抄送,0不可抄送,1可抄送")
    private boolean copy;

    /**
     * 是否可加签,0不可加签,1可加签
     */
    @ExcelProperty(value = "是否可加签,0不可加签,1可加签")
    private boolean addMultiInstance;

    /**
     * 是否可加签,0不可减签,1可减签
     */
    @ExcelProperty(value = "是否可加签,0不可减签,1可减签")
    private boolean deleteMultiInstance;

    /**
     * 当前节点是否显示驳回,0不显示驳回按钮,1显示驳回按钮
     */
    @ExcelProperty(value = "当前节点是否显示驳回,0不显示驳回按钮,1显示驳回按钮")
    private boolean showBack;

    /**
     * 表单管理
     */
    private WfFormManageVo wfFormManageVo;


}
