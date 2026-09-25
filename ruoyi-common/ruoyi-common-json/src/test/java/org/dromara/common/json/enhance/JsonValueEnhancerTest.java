package org.dromara.common.json.enhance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JsonValueEnhancer 单元测试")
class JsonValueEnhancerTest {

    /**
     * 验证没有处理器或没有字段命中时直接返回原对象，避免无意义的 JSON 树转换。
     */
    @Test
    @DisplayName("无处理需求时保留原对象")
    void shouldKeepOriginalBodyWhenProcessingIsNotRequired() {
        Payload body = new Payload("secret", List.of(), new Object[0], null);
        JsonValueEnhancer emptyEnhancer = new JsonValueEnhancer(JsonMapper.builder().build(), List.of());
        JsonValueEnhancer unmatchedEnhancer = new JsonValueEnhancer(
            JsonMapper.builder().build(), List.of(new RecordingProcessor(false)));

        assertSame(body, emptyEnhancer.enhance(body));
        assertSame(body, unmatchedEnhancer.enhance(body));
        assertSame(null, emptyEnhancer.enhance(null));
    }

    /**
     * 验证增强器递归处理 Map、集合、数组和 POJO，并且 prepare 在所有字段收集完成后只执行一次。
     */
    @Test
    @DisplayName("递归增强混合对象结构")
    void shouldEnhanceMapIterableArrayAndPojoValues() {
        RecordingProcessor processor = new RecordingProcessor(true);
        JsonValueEnhancer enhancer = new JsonValueEnhancer(JsonMapper.builder().build(), List.of(processor));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payload", new Payload("root", List.of(new Child("list")),
            new Object[]{new Child("array"), 3}, null));
        body.put("plain", "value");

        JsonNode result = (JsonNode) enhancer.enhance(body);

        assertEquals("ROOT", result.get("payload").get("secret").stringValue());
        assertEquals("LIST", result.get("payload").get("children").get(0).get("secret").stringValue());
        assertEquals("ARRAY", result.get("payload").get("values").get(0).get("secret").stringValue());
        assertEquals(3, result.get("payload").get("values").get(1).intValue());
        assertEquals("value", result.get("plain").stringValue());
        assertEquals(3, processor.collectedValues.size());
        assertEquals(1, processor.prepareCalls);
        assertEquals(4, processor.processCalls);
    }

    /**
     * 验证字段被替换为复杂对象后会执行二次增强，使新对象中的目标字段也得到处理。
     */
    @Test
    @DisplayName("二次增强处理器生成的复杂对象")
    void shouldEnhanceComplexValueProducedByProcessor() {
        RecordingProcessor processor = new RecordingProcessor(true);
        JsonValueEnhancer enhancer = new JsonValueEnhancer(JsonMapper.builder().build(), List.of(processor));

        JsonNode result = (JsonNode) enhancer.enhance(
            new Payload("root", List.of(), new Object[0], "translated"));

        assertEquals("ROOT", result.get("secret").stringValue());
        assertEquals("TRANSLATED", result.get("replacement").get("secret").stringValue());
        assertEquals(2, processor.prepareCalls);
    }

    /**
     * 验证已有 JsonNode 不重复处理，并正确过滤字符串和字节数组消息转换器。
     */
    @Test
    @DisplayName("判断响应转换器支持范围")
    void shouldFilterUnsupportedMessageConverters() {
        JsonMapper mapper = JsonMapper.builder().build();
        JsonValueEnhancer enhancer = new JsonValueEnhancer(mapper, List.of(new RecordingProcessor(true)));
        JsonNode tree = mapper.createObjectNode().put("secret", "value");

        assertSame(tree, enhancer.enhance(tree));
        assertTrue(enhancer.supports(JacksonJsonHttpMessageConverter.class));
        assertFalse(enhancer.supports(StringHttpMessageConverter.class));
        assertFalse(enhancer.supports(ByteArrayHttpMessageConverter.class));
        assertFalse(new JsonValueEnhancer(mapper, List.of()).supports(JacksonJsonHttpMessageConverter.class));
    }

    private record Payload(String secret, List<Child> children, Object[] values, String replacement) {
    }

    private record Child(String secret) {
    }

    private static class RecordingProcessor implements JsonFieldProcessor {

        private final boolean enabled;
        private final List<Object> collectedValues = new java.util.ArrayList<>();
        private int prepareCalls;
        private int processCalls;

        /**
         * 创建可控制是否命中字段的记录型处理器。
         *
         * @param enabled 是否处理目标字段
         */
        private RecordingProcessor(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 仅匹配 secret 和 replacement 字段，控制测试覆盖的增强范围。
         *
         * @param fieldContext 字段上下文
         * @return 是否处理当前字段
         */
        @Override
        public boolean supports(JsonFieldContext fieldContext) {
            return enabled && ("secret".equals(fieldContext.propertyName())
                || "replacement".equals(fieldContext.propertyName()));
        }

        /**
         * 记录非空字段值，验证递归收集覆盖了所有目标对象。
         *
         * @param fieldContext 字段上下文
         * @param context      增强上下文
         */
        @Override
        public void collect(JsonFieldContext fieldContext, JsonEnhancementContext context) {
            if (fieldContext.value() != null) {
                collectedValues.add(fieldContext.value());
            }
        }

        /**
         * 记录预处理次数并写入跨阶段属性。
         *
         * @param context 增强上下文
         */
        @Override
        public void prepare(JsonEnhancementContext context) {
            prepareCalls++;
            context.setAttribute("prepared", Boolean.TRUE);
        }

        /**
         * 将 secret 转为大写，并把 replacement 文本转换为待二次增强的对象。
         *
         * @param fieldContext 字段上下文
         * @param value        当前字段值
         * @param context      增强上下文
         * @return 增强后的字段值
         */
        @Override
        public Object process(JsonFieldContext fieldContext, Object value, JsonEnhancementContext context) {
            processCalls++;
            assertTrue(context.containsAttribute("prepared"));
            if ("replacement".equals(fieldContext.propertyName())) {
                return value == null ? null : new Child(String.valueOf(value));
            }
            return value == null ? null : String.valueOf(value).toUpperCase();
        }
    }
}
