package com.ruoyi.common.translation.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ruoyi.common.translation.core.handler.TranslationHandler;

import java.lang.annotation.*;

/**
 * 通用翻译注解
 *
 * @author Lion Li
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = TranslationHandler.class)
public @interface Translation {

    /**
     * 类型 (需与实现类上的 {@link com.ruoyi.common.translation.annotation.TranslationType} 注解type对应)
     */
    String type();

    /**
     * 通用Key 如果为空则取被标注的字段值 例如: 字典type(sys_user_sex)
     */
    String key() default "";

}
