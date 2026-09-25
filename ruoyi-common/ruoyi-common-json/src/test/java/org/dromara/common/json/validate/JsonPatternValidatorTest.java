package org.dromara.common.json.validate;

import org.dromara.common.json.JsonTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("JsonPatternValidator 单元测试")
class JsonPatternValidatorTest {

    /**
     * 在校验器调用 JsonUtils 前初始化全局 JsonMapper。
     */
    @BeforeAll
    static void initializeJsonMapper() {
        JsonTestContext.initialize();
    }

    /**
     * 验证空值由 NotNull 或 NotBlank 等其他注解负责约束。
     */
    @Test
    @DisplayName("空值交由其他校验注解处理")
    void blankValueShouldBeValid() {
        JsonPatternValidator validator = validator(JsonType.ANY);

        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid(" ", null));
    }

    /**
     * 验证 OBJECT、ARRAY 和 ANY 三种类型约束的分支行为。
     */
    @Test
    @DisplayName("按注解配置校验 JSON 类型")
    void shouldValidateConfiguredJsonType() {
        assertTrue(validator(JsonType.OBJECT).isValid("{\"id\":1}", null));
        assertFalse(validator(JsonType.OBJECT).isValid("[1]", null));
        assertTrue(validator(JsonType.ARRAY).isValid("[1]", null));
        assertFalse(validator(JsonType.ARRAY).isValid("{\"id\":1}", null));
        assertFalse(validator(JsonType.ANY).isValid("1", null));
    }

    /**
     * 根据指定 JSON 类型创建已初始化的校验器。
     *
     * @param type JSON 类型
     * @return 校验器
     */
    private static JsonPatternValidator validator(JsonType type) {
        JsonPattern annotation = mock(JsonPattern.class);
        when(annotation.type()).thenReturn(type);
        JsonPatternValidator validator = new JsonPatternValidator();
        validator.initialize(annotation);
        return validator;
    }

}
