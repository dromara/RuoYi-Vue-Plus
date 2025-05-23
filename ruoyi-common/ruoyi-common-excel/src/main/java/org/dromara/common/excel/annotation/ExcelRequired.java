package org.dromara.common.excel.annotation;

import org.apache.poi.ss.usermodel.IndexedColors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 是否必填
 * @author guzhouyanyu
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelRequired {

    /**
     * col index
     */
    int index() default -1;
    /**
     * 字体颜色
     */
    IndexedColors fontColor() default IndexedColors.RED;
}
