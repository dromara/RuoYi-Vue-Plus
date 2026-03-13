package org.dromara.workflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 节点扩展变量枚举，占位用于后续补充可配置变量定义。
 *
 * @author AprilWind
 */
@Getter
@AllArgsConstructor
public enum VariablesEnum implements NodeExtEnum {
    ;

    /**
     * 展示名称。
     */
    private final String label;

    /**
     * 变量值。
     */
    private final String value;

    /**
     * 是否选中。
     */
    private final boolean selected;

}

