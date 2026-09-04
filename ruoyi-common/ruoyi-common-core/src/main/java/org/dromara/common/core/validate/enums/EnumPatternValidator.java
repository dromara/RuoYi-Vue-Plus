package org.dromara.common.core.validate.enums;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.BooleanUtil;
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
public class EnumPatternValidator implements ConstraintValidator<EnumPattern, Object> {

    /**
     * 枚举允许值集合。
     */
    private final Set<Object> values = new HashSet<>();

    /**
     * 枚举字段值类型。
     */
    private Class<?> valueType;

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
                if (valueType == null) {
                    valueType = fieldValue.getClass();
                }
                values.add(fieldValue);
            }
        }
    }

    /**
     * 校验值是否在枚举允许值集合内。
     *
     * <p>以枚举字段的实际类型转换输入值后进行比较，可兼容字符串、数字、布尔值等字段类型，
     * 同时避免将无法转换的值视为合法值。字符串类型的空白值按未填写处理，其他类型仅 null
     * 按未填写处理。</p>
     *
     * @param value                      待校验值
     * @param constraintValidatorContext 校验上下文
     * @return true 校验通过 false 校验失败
     */
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null) {
            return true;
        }
        if (valueType == null) {
            return false;
        }
        if (value instanceof CharSequence && StringUtils.isBlank(value.toString())
            && CharSequence.class.isAssignableFrom(valueType)) {
            return true;
        }
        if ((valueType == Boolean.class || valueType == boolean.class) && value instanceof CharSequence
            && BooleanUtil.toBooleanObject(value.toString()) == null) {
            return false;
        }
        Object convertedValue = Convert.convertWithCheck(valueType, value, null, true);
        return convertedValue != null && values.contains(convertedValue);
    }

}
