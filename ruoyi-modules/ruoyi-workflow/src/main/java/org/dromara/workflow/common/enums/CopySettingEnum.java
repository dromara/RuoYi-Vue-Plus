package org.dromara.workflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 抄送设置枚举
 *
 * @author AprilWind
 */
@Getter
@AllArgsConstructor
public enum CopySettingEnum implements NodeExtEnum {
    ;

    /**
     * 展示名称。
     */
    private final String label;

    /**
     * 配置值。
     */
    private final String value;

    /**
     * 是否默认选中。
     */
    private final boolean selected;

}

