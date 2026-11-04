package org.dromara.common.json.handler;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JsonSerializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;

import java.math.BigDecimal;

/**
 * 序列化 BigDecimal 时去除小数末尾无意义的零，并始终以普通十进制字符串输出。
 *
 * <p>输出 JSON 字符串而非 JSON 数字，避免 JavaScript Number 解析时丢失交易精度。</p>
 */
@JacksonStdImpl
public class BigDecimalStripSerializer extends JsonSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext context) {
        gen.writeString(value.stripTrailingZeros().toPlainString());
    }

}
