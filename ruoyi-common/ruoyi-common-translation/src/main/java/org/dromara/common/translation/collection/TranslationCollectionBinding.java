package org.dromara.common.translation.collection;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import tools.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.*;

/**
 * 序列化器绑定注解
 *
 * @author 秋辞未寒
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@JacksonAnnotationsInside
@JsonSerialize(using = TranslationCollectionSerializer.class)
public @interface TranslationCollectionBinding {
}
