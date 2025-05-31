package org.dromara.pms.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 价格调整类型枚举
 *
 * @author ruoyi
 * @date 2024-12-19
 */
@Getter
@AllArgsConstructor
public enum PriceAdjustmentType {

    /**
     * 固定金额调整
     */
    FIXED_AMOUNT("fixed_amount", "固定金额"),

    /**
     * 百分比调整
     */
    PERCENTAGE("percentage", "百分比"),

    /**
     * 固定价格
     */
    FIXED_PRICE("fixed_price", "固定价格");

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
    public static PriceAdjustmentType fromCode(String code) {
        for (PriceAdjustmentType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的价格调整类型: " + code);
    }

    /**
     * 检查代码是否有效
     *
     * @param code 代码
     * @return 是否有效
     */
    public static boolean isValidCode(String code) {
        for (PriceAdjustmentType type : values()) {
            if (type.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
