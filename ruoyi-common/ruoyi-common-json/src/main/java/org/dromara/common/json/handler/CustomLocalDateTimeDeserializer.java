package org.dromara.common.json.handler;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.LocalDateTime;

/**
 * 自定义 LocalDateTime 类型反序列化处理器
 *
 * @author AprilWind
 */
public class CustomLocalDateTimeDeserializer extends ValueDeserializer<LocalDateTime> {

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
        DateTime parse = DateUtil.parse(text.trim());
        return parse.toLocalDateTime();
    }

}
