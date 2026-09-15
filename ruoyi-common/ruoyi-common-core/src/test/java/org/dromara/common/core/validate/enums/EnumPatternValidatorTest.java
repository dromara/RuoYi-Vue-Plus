package org.dromara.common.core.validate.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EnumPatternValidator 单元测试")
class EnumPatternValidatorTest {

    /**
     * 验证字符串形式的数字能够按枚举字段实际类型转换并完成合法值校验。
     */
    @Test
    @DisplayName("校验枚举字段值")
    void shouldValidateConvertedEnumFieldValues() throws NoSuchFieldException {
        EnumPatternValidator validator = validatorFor("status");

        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid(1, null));
        assertTrue(validator.isValid("2", null));
        assertFalse(validator.isValid("invalid", null));
        assertFalse(validator.isValid(9, null));
    }

    /**
     * 验证未配置枚举字段名时不会错误接受非空输入。
     */
    @Test
    @DisplayName("拒绝缺少枚举字段配置的输入")
    void shouldRejectValueWhenEnumFieldIsNotConfigured() throws NoSuchFieldException {
        EnumPatternValidator validator = validatorFor("unconfigured");

        assertTrue(validator.isValid(null, null));
        assertFalse(validator.isValid(1, null));
    }

    /**
     * 根据测试字段上的真实注解创建并初始化枚举校验器。
     *
     * @param fieldName 测试字段名
     * @return 已初始化的校验器
     */
    private static EnumPatternValidator validatorFor(String fieldName) throws NoSuchFieldException {
        Field field = ValidationTarget.class.getDeclaredField(fieldName);
        EnumPatternValidator validator = new EnumPatternValidator();
        validator.initialize(field.getAnnotation(EnumPattern.class));
        return validator;
    }

    private enum Status {
        ENABLED(1), DISABLED(2);

        private final Integer code;

        /**
         * 创建带校验码的测试状态。
         *
         * @param code 枚举校验码
         */
        Status(Integer code) {
            this.code = code;
        }

        /**
         * 返回供枚举校验器读取的测试状态码。
         *
         * @return 状态码
         */
        public Integer getCode() {
            return code;
        }
    }

    private static class ValidationTarget {
        @EnumPattern(type = Status.class, fieldName = "code")
        private Integer status;

        @EnumPattern(type = Status.class, fieldName = "")
        private Integer unconfigured;
    }
}
