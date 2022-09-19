package com.ruoyi.common.annotation;

import cn.dev33.satoken.annotation.SaIgnore;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 匿名访问不鉴权注解
 *
 * @author ruoyi
 * @deprecated 将在后续版本使用Sa-Token注解 {@link SaIgnore} 代替
 */
@Deprecated
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Anonymous {
}
