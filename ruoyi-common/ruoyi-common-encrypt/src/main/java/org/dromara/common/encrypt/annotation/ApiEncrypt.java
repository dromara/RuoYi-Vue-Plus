package org.dromara.common.encrypt.annotation;

import java.lang.annotation.*;

/**
 * 强制加密注解
 *
 * @author Michelle.Chung
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiEncrypt {

    /**
     * 响应加密忽略，默认加密，为 false 时不加密
     */
    boolean response() default true;

}
