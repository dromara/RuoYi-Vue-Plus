package org.dromara.pms.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 价格规则状态枚举
 *
 * @author ruoyi
 * @date 2024-12-19
 */
@Getter
@AllArgsConstructor
public enum PricingRuleStatus {

    /**
     * 草稿状态
     */
    DRAFT("draft", "草稿"),

    /**
     * 启用状态
     */
    ACTIVE("active", "启用"),

    /**
     * 禁用状态
     */
    INACTIVE("inactive", "禁用"),

    /**
     * 已过期
     */
    EXPIRED("expired", "已过期"),

    /**
     * 已删除
     */
    DELETED("deleted", "已删除");

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
    public static PricingRuleStatus fromCode(String code) {
        for (PricingRuleStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的价格规则状态: " + code);
    }

    /**
     * 检查代码是否有效
     *
     * @param code 代码
     * @return 是否有效
     */
    public static boolean isValidCode(String code) {
        for (PricingRuleStatus status : values()) {
            if (status.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查是否为活跃状态
     *
     * @return 是否活跃
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * 检查是否可以编辑
     *
     * @return 是否可编辑
     */
    public boolean isEditable() {
        return this == DRAFT || this == INACTIVE;
    }
}
