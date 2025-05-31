package org.dromara.pms.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 特殊日期状态枚举
 *
 * @author ruoyi
 * @date 2024-12-19
 */
@Getter
@AllArgsConstructor
public enum SpecialDateStatus {

    /**
     * 启用
     */
    ACTIVE("active", "启用"),

    /**
     * 禁用
     */
    INACTIVE("inactive", "禁用");

    /**
     * 数据库存储值
     */
    private final String code;

    /**
     * 显示名称
     */
    private final String description;

    /**
     * 根据代码获取枚举
     *
     * @param code 代码
     * @return 枚举值
     */
    public static SpecialDateStatus fromCode(String code) {
        for (SpecialDateStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的特殊日期状态: " + code);
    }

    /**
     * 检查代码是否有效
     *
     * @param code 代码
     * @return 是否有效
     */
    public static boolean isValidCode(String code) {
        for (SpecialDateStatus status : values()) {
            if (status.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}