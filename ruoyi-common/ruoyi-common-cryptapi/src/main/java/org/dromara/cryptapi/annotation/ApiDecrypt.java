package org.dromara.cryptapi.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 当标有当前注解的接口，接口穿参为加密字符串，进行解密后为dto对象， 不影响后续参数校验。
 * @author wdhcr
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ApiDecrypt {
}
