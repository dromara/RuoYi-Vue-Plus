package org.dromara.workflow.domain.bo;

import org.dromara.workflow.domain.WfNodeConfig;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;

/**
 * 节点配置业务对象 wf_node_config
 *
 * @author may
 * @date 2024-03-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = WfNodeConfig.class, reverseConvertGenerate = false)
public class WfNodeConfigBo extends BaseEntity {

    /**
     * 主键
     */
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
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
    @NotBlank(message = "节点名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String nodeName;

    /**
     * 节点id
     */
    @NotBlank(message = "节点id不能为空", groups = {AddGroup.class, EditGroup.class})
    private String nodeId;

    /**
     * 流程定义id
     */
    @NotBlank(message = "流程定义id不能为空", groups = {AddGroup.class, EditGroup.class})
    private String definitionId;

    /**
     * 是否为申请人节点 （0是 1否）
     */
    @NotBlank(message = "是否为申请人节点不能为空", groups = {AddGroup.class, EditGroup.class})
    private String applyUserTask;

    /**
     * 是否会签,0非会签,1会签
     */
    @NotNull(message = "是否会签不能为空", groups = {AddGroup.class, EditGroup.class})
    private boolean multiple;

    /**
     * 会签保存人员KEY值
     */
    private boolean multipleColumn;

    /**
     * 是否可退回到当前节点,0不可退回,1可退回
     */
    @NotNull(message = "是否可退回到当前节点不能为空", groups = {AddGroup.class, EditGroup.class})
    private boolean back;

    /**
     * 是否可委托,0不可委托,1可委托
     */
    @NotNull(message = "是否可委托不能为空", groups = {AddGroup.class, EditGroup.class})
    private boolean delegate;

    /**
     * 是否可转办,0不可转办,1可转办
     */
    @NotNull(message = "是否可转办不能为空", groups = {AddGroup.class, EditGroup.class})
    private boolean transfer;

    /**
     * 是否可抄送,0不可抄送,1可抄送
     */
    @NotNull(message = "是否可抄送不能为空", groups = {AddGroup.class, EditGroup.class})
    private boolean copy;

    /**
     * 是否可加签,0不可加签,1可加签
     */
    @NotNull(message = "是否可加签不能为空", groups = {AddGroup.class, EditGroup.class})
    private boolean addMultiInstance;

    /**
     * 是否可减签,0不可减签,1可减签
     */
    @NotNull(message = "是否可减签不能为空", groups = {AddGroup.class, EditGroup.class})
    private boolean deleteMultiInstance;

    /**
     * 当前节点是否显示驳回,0不显示驳回按钮,1显示驳回按钮
     */
    @NotNull(message = "当前节点是否显示驳回不能为空", groups = {AddGroup.class, EditGroup.class})
    private boolean showBack;

}
