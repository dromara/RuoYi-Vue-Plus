package com.ruoyi.common.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ruoyi.common.jackson.DictDataJsonSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字典数据映射注解
 *
 * @author itino
 * @deprecated 建议使用通用翻译注解
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@JacksonAnnotationsInside
@JsonSerialize(using = DictDataJsonSerializer.class)
public @interface DictDataMapper {

    /**
     * 设置字典的type值 (如: sys_user_sex)
     */
    String dictType() default "";
}
