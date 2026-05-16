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

    private final Set<String> values = new HashSet<>();

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

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        return values.contains(value);
    }

}
