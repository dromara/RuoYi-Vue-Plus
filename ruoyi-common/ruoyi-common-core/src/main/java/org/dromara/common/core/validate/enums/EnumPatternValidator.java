package org.dromara.common.core.validate.enums;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.reflect.ReflectUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * 自定义枚举校验注解实现
 *
 * @author 秋辞未寒
 * @date 2024-12-09
 */
public class EnumPatternValidator implements ConstraintValidator<EnumPattern, String> {

    /**
     * 枚举允许值集合。
     */
    private final Set<String> values = new HashSet<>();

    /**
     * 初始化枚举允许值集合。
     *
     * @param annotation 枚举校验注解
     */
    @Override
    public void initialize(EnumPattern annotation) {
        ConstraintValidator.super.initialize(annotation);
        String fieldName = annotation.fieldName();
        if (StringUtils.isBlank(fieldName)) {
            return;
        }
        for (Object e : annotation.type().getEnumConstants()) {
            Object fieldValue = ReflectUtils.invokeGetter(e, fieldName);
            if (fieldValue != null) {
                values.add(String.valueOf(fieldValue));
            }
        }
    }

    /**
     * 校验字符串是否在枚举允许值集合内。
     *
     * @param value                      待校验值
     * @param constraintValidatorContext 校验上下文
     * @return true 校验通过 false 校验失败
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        return values.contains(value);
    }

}
