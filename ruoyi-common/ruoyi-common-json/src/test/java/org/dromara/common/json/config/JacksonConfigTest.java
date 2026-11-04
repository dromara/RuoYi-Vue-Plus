package org.dromara.common.json.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JacksonConfig 单元测试")
class JacksonConfigTest {

    /**
     * 验证安全范围内整数保持数字类型，超出 JavaScript 安全范围的整数和 BigDecimal 输出字符串。
     */
    @Test
    @DisplayName("安全序列化大数字")
    void shouldSerializeNumbersWithoutJavaScriptPrecisionLoss() {
        JsonMapper mapper = configuredMapper();

        assertEquals("9007199254740991", mapper.writeValueAsString(9_007_199_254_740_991L));
        assertEquals("\"9007199254740992\"", mapper.writeValueAsString(9_007_199_254_740_992L));
        assertEquals("\"1234567890.123456789\"",
            mapper.writeValueAsString(new BigDecimal("1234567890.123456789")));
        assertEquals("\"23.0546822307066408\"",
            mapper.writeValueAsString(new BigDecimal("23.0546822307066408000")));
    }

    /**
     * 验证 BigDecimal 去除无意义尾零时仍以普通十进制字符串输出，避免前端浮点精度损失。
     */
    @Test
    @DisplayName("BigDecimal 去尾零并保留精度")
    void shouldStripBigDecimalTrailingZerosAndKeepStringPrecision() {
        JsonMapper mapper = configuredMapper();

        assertEquals("\"1.5\"", mapper.writeValueAsString(new BigDecimal("1.50000000")));
        assertEquals("\"1000\"", mapper.writeValueAsString(new BigDecimal("1000.00000000")));
        assertEquals("\"0\"", mapper.writeValueAsString(new BigDecimal("0.00000000")));
        assertEquals("\"0.000001\"", mapper.writeValueAsString(new BigDecimal("0.00000100")));
        assertEquals("\"12345678901234567890.12345678\"",
            mapper.writeValueAsString(new BigDecimal("12345678901234567890.1234567800")));
    }

    /**
     * 验证 LocalDateTime 使用统一格式序列化，并支持带空白的日期和时间字符串反序列化。
     */
    @Test
    @DisplayName("序列化和反序列化日期时间")
    void shouldSerializeAndDeserializeTemporalValues() {
        JsonMapper mapper = configuredMapper();
        LocalDateTime value = LocalDateTime.of(2026, 9, 15, 10, 20, 30);

        assertEquals("\"2026-09-15 10:20:30\"", mapper.writeValueAsString(value));
        assertEquals(value, mapper.readValue("\" 2026-09-15 10:20:30 \"", LocalDateTime.class));
        Date date = mapper.readValue("\"2026-09-15 10:20:30\"", Date.class);
        assertTrue(date.getTime() > 0);
    }

    /**
     * 创建注册项目 Jackson 模块的独立 JsonMapper，避免依赖完整 Spring 容器。
     *
     * @return 配置完成的 JsonMapper
     */
    private static JsonMapper configuredMapper() {
        return JsonMapper.builder()
            .addModule(new JacksonConfig().registerJavaTimeModule())
            .build();
    }
}
