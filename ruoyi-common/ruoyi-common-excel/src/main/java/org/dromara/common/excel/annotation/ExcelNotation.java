package org.dromara.common.excel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 批注
 * @author guzhouyanyu
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelNotation {

    /**
     * col index
     */
    int index() default -1;
    /**
     * 批注内容
     */
    String value() default "";
}
