package org.dromara.common.translation.collection;

import java.lang.annotation.*;


/**
 * 集合翻译类型
 *
 * @author 秋辞未寒
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface TranslationCollectionType {

    /**
     * 类型
     */
    String type();

}
