package org.dromara.common.json.handler;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 自定义 LocalDateTime 类型反序列化处理器（支持多种格式，无第三方依赖）
 *
 * @author AprilWind
 */
public class CustomLocalDateTimeDeserializer extends ValueDeserializer<LocalDateTime> {

    /** 支持时间的格式列表（直接解析为 LocalDateTime） */
    private static final List<DateTimeFormatter> DATETIME_FORMATTERS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
        DateTimeFormatter.ofPattern("yyyyMMdd HHmmss"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    /** 仅日期的格式列表（解析为 LocalDate，再补零时转 LocalDateTime） */
    private static final List<DateTimeFormatter> DATE_ONLY_FORMATTERS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.BASIC_ISO_DATE
    );

    /**
     * 反序列化逻辑：将字符串转换为 LocalDateTime 对象
     *
     * @param p    JSON 解析器，用于获取字符串值
     * @param ctxt 上下文环境（可用于获取更多配置）
     * @return 转换后的 LocalDateTime 对象，若为空字符串返回 null
     */
    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
        String text = p.getString();
        if (text == null || text.isBlank()) {
            return null;
        }
        text = text.trim();

        // 纯数字：按时间戳处理（毫秒）
        if (text.chars().allMatch(Character::isDigit)) {
            return Instant.ofEpochMilli(Long.parseLong(text))
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        }

        // 尝试带时间的格式
        for (DateTimeFormatter formatter : DATETIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        // 尝试仅日期的格式，补零时
        for (DateTimeFormatter formatter : DATE_ONLY_FORMATTERS) {
            try {
                return LocalDate.parse(text, formatter).atStartOfDay();
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
    }

}
