package org.dromara.workflow.common.enums;

/**
 * 节点扩展属性枚举通用接口，约束扩展选项的展示名、值和默认勾选状态。
 *
 * @author AprilWind
 */
public interface NodeExtEnum {

    /**
     * 获取选项展示名称。
     *
     * @return 选项label
     */
    String getLabel();

    /**
     * 获取选项值。
     *
     * @return 选项值
     */
    String getValue();

    /**
     * 是否默认选中。
     *
     * @return 是否默认选中
     */
    boolean isSelected();

}

