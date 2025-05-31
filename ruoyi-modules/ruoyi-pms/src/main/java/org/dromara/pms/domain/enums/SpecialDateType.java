package org.dromara.pms.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 特殊日期类型枚举
 *
 * @author ruoyi
 * @date 2024-12-19
 */
@Getter
@AllArgsConstructor
public enum SpecialDateType {

    /**
     * 法定节假日
     */
    LEGAL_HOLIDAY("legal_holiday", "法定节假日"),

    /**
     * 传统节日
     */
    TRADITIONAL_FESTIVAL("traditional_festival", "传统节日"),

    /**
     * 周末
     */
    WEEKEND("weekend", "周末"),

    /**
     * 促销日
     */
    PROMOTION_DAY("promotion_day", "促销日"),

    /**
     * 特殊活动日
     */
    SPECIAL_EVENT("special_event", "特殊活动日"),

    /**
     * 淡季
     */
    LOW_SEASON("low_season", "淡季"),

    /**
     * 旺季
     */
    HIGH_SEASON("high_season", "旺季");

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
    public static SpecialDateType fromCode(String code) {
        for (SpecialDateType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的特殊日期类型: " + code);
    }

    /**
     * 检查代码是否有效
     *
     * @param code 代码
     * @return 是否有效
     */
    public static boolean isValidCode(String code) {
        for (SpecialDateType type : values()) {
            if (type.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}
