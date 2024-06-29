package org.dromara.common.mybatis.annotation;

import java.lang.annotation.*;

/**
 * 数据权限组注解，用于标记数据权限配置数组
 *
 * @author Lion Li
 * @version 3.5.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 数据权限配置数组，用于指定数据权限的占位符关键字和替换值
     *
     * @return 数据权限配置数组
     */
    DataColumn[] value();

}
