package org.dromara.pms.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 配置类型枚举
 *
 * @author PMS
 * @date 2024-12-01
 */
@Getter
@AllArgsConstructor
public enum SettingType {

    /**
     * 字符串类型
     */
    STRING("string", "字符串"),

    /**
     * 数字类型
     */
    NUMBER("number", "数字"),

    /**
     * 布尔类型
     */
    BOOLEAN("boolean", "布尔值"),

    /**
     * JSON类型
     */
    JSON("json", "JSON对象"),

    /**
     * 数组类型
     */
    ARRAY("array", "数组"),

    /**
     * 日期类型
     */
    DATE("date", "日期"),

    /**
     * 时间类型
     */
    DATETIME("datetime", "日期时间"),

    /**
     * 文件路径类型
     */
    FILE_PATH("file_path", "文件路径"),

    /**
     * URL类型
     */
    URL("url", "URL地址"),

    /**
     * 邮箱类型
     */
    EMAIL("email", "邮箱地址"),

    /**
     * 电话类型
     */
    PHONE("phone", "电话号码"),

    /**
     * 密码类型
     */
    PASSWORD("password", "密码"),

    /**
     * 颜色类型
     */
    COLOR("color", "颜色值"),

    /**
     * 枚举类型
     */
    ENUM("enum", "枚举值");

    /**
     * 类型代码
     */
    private final String code;

    /**
     * 类型描述
     */
    private final String description;

    /**
     * 根据代码获取枚举
     */
    public static SettingType getByCode(String code) {
        for (SettingType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return STRING; // 默认返回字符串类型
    }

    /**
     * 验证配置值格式是否正确
     */
    public boolean validateValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true; // 空值由业务层校验
        }

        try {
            switch (this) {
                case NUMBER:
                    Double.parseDouble(value);
                    return true;
                case BOOLEAN:
                    return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
                case JSON:
                    // 简单的JSON格式检查
                    return value.trim().startsWith("{") && value.trim().endsWith("}") ||
                            value.trim().startsWith("[") && value.trim().endsWith("]");
                case EMAIL:
                    return value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
                case PHONE:
                    return value.matches("^1[3-9]\\d{9}$");
                case URL:
                    return value.matches("^https?://.*");
                case COLOR:
                    return value.matches("^#[0-9A-Fa-f]{6}$");
                default:
                    return true; // 其他类型暂不校验
            }
        } catch (Exception e) {
            return false;
        }
    }
}
