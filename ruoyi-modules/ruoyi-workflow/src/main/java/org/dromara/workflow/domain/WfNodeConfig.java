package org.dromara.workflow.domain;

import org.dromara.common.tenant.core.TenantEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 节点配置对象 wf_node_config
 *
 * @author may
 * @date 2024-03-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_node_config")
public class WfNodeConfig extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 表单id
     */
    private Long formId;

    /**
     * 表单类型
     */
    private String formType;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点id
     */
    private String nodeId;

    /**
     * 流程定义id
     */
    private String definitionId;

    /**
     * 是否为申请人节点 （0是 1否）
     */
    private String applyUserTask;

    /**
     * 是否会签,0非会签,1会签
     */
    private boolean multiple;

    /**
     * 会签保存人员KEY值
     */
    private boolean multipleColumn;

    /**
     * 是否可退回到当前节点,0不可退回,1可退回
     */
    private boolean back;

    /**
     * 是否可委托,0不可委托,1可委托
     */
    private boolean delegate;

    /**
     * 是否可转办,0不可转办,1可转办
     */
    private boolean transfer;

    /**
     * 是否可抄送,0不可抄送,1可抄送
     */
    private boolean copy;

    /**
     * 是否可加签,0不可加签,1可加签
     */
    private boolean addMultiInstance;

    /**
     * 是否可加签,0不可减签,1可减签
     */
    private boolean deleteMultiInstance;

    /**
     * 当前节点是否显示驳回,0不显示驳回按钮,1显示驳回按钮
     */
    private boolean showBack;


}
