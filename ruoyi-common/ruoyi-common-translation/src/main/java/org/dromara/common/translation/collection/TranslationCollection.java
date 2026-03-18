package org.dromara.common.translation.collection;

import java.lang.annotation.*;

/**
 * 集合翻译（声明到需要翻译的实体上）
 *
 * @author 秋辞未寒
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface TranslationCollection {

    /**
     * 类型
     */
    String type();

    /**
     * 其他条件 例如: 字典type(sys_user_gender)
     */
    String other() default "";

}
