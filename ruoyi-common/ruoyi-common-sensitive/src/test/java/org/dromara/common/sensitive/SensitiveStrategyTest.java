package org.dromara.common.sensitive;

import org.dromara.common.sensitive.core.SensitiveStrategy;
import org.dromara.common.sensitive.annotation.Sensitive;
import org.dromara.common.sensitive.core.SensitiveService;
import org.dromara.common.sensitive.handler.SensitiveJsonFieldProcessor;
import org.dromara.common.json.enhance.JsonEnhancementContext;
import org.dromara.common.json.enhance.JsonFieldContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("common-sensitive 功能单元测试")
class SensitiveStrategyTest {

    /**
     * 验证手机号、邮箱和中文名称使用预期的标准脱敏规则。
     */
    @Test
    @DisplayName("执行常用身份信息脱敏")
    void shouldDesensitizeCommonIdentityValues() {
        assertEquals("138****8000", SensitiveStrategy.PHONE.desensitizer().apply("13812348000"));
        assertEquals("a****@example.com", SensitiveStrategy.EMAIL.desensitizer().apply("admin@example.com"));
        assertEquals("张**", SensitiveStrategy.CHINESE_NAME.desensitizer().apply("张三丰"));
    }

    /**
     * 验证通用掩码和高安全掩码保留配置要求的首尾字符。
     */
    @Test
    @DisplayName("执行字符串安全掩码")
    void shouldMaskStringWithConfiguredVisibility() {
        assertEquals("abcd****mnop", SensitiveStrategy.STRING_MASK.desensitizer().apply("abcdefghijklmnop"));
        String highSecurity = SensitiveStrategy.MASK_HIGH_SECURITY.desensitizer().apply("abcdefghijklmnop");
        assertTrue(highSecurity.startsWith("ab"));
        assertTrue(highSecurity.endsWith("op"));
        assertEquals(16, highSecurity.length());
    }

    /**
     * 验证清空策略分别返回空字符串和 null。
     */
    @Test
    @DisplayName("执行清空脱敏策略")
    void shouldClearSensitiveValue() {
        assertEquals("", SensitiveStrategy.CLEAR.desensitizer().apply("secret"));
        assertNull(SensitiveStrategy.CLEAR_TO_NULL.desensitizer().apply("secret"));
    }

    /**
     * 验证响应字段处理器只在注解和权限服务同时允许时脱敏，并对非字符串值保持透传。
     */
    @Test
    @DisplayName("按字段注解和当前权限执行脱敏")
    void shouldProcessSensitiveFieldOnlyWhenAuthorized() {
        Sensitive annotation = mock(Sensitive.class);
        when(annotation.strategy()).thenReturn(SensitiveStrategy.PHONE);
        when(annotation.roleKey()).thenReturn(new String[]{"admin"});
        when(annotation.perms()).thenReturn(new String[]{"system:user:list"});
        JsonFieldContext fieldContext = mock(JsonFieldContext.class);
        when(fieldContext.getAnnotation(Sensitive.class)).thenReturn(annotation);
        SensitiveService service = mock(SensitiveService.class);
        SensitiveJsonFieldProcessor processor = new SensitiveJsonFieldProcessor();
        ReflectionTestUtils.setField(processor, "sensitiveService", service);

        when(service.isSensitive(annotation.roleKey(), annotation.perms())).thenReturn(true);
        assertTrue(processor.supports(fieldContext));
        assertEquals("138****8000", processor.process(fieldContext, "13812348000",
            new JsonEnhancementContext(null)));

        when(service.isSensitive(annotation.roleKey(), annotation.perms())).thenReturn(false);
        assertEquals("13812348000", processor.process(fieldContext, "13812348000",
            new JsonEnhancementContext(null)));
        assertEquals(100L, processor.process(fieldContext, 100L, new JsonEnhancementContext(null)));
    }

    /**
     * 验证缺少脱敏注解或权限服务时保持原值，避免响应增强误处理普通字段。
     */
    @Test
    @DisplayName("未命中脱敏条件时保留原值")
    void shouldKeepOriginalValueWithoutAnnotationOrService() {
        JsonFieldContext plainField = mock(JsonFieldContext.class);
        SensitiveJsonFieldProcessor processor = new SensitiveJsonFieldProcessor();

        assertFalse(processor.supports(plainField));
        assertEquals("plain", processor.process(plainField, "plain", new JsonEnhancementContext(null)));
    }
}
