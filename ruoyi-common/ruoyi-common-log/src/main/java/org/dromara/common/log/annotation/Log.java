package org.dromara.common.log.annotation;

import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.log.enums.OperateChannel;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 自定义操作日志记录注解
 *
 * @author ruoyi
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /**
     * 模块【已废弃，请使用 module】
     *
     * @deprecated 请使用 {@link #module()}
     */
    @Deprecated
    String title() default "";

    /**
     * 业务模块名称（必填，如：用户管理、订单管理）
     */
    String module() default "";

    /**
     * 操作功能名称（必填，如：新增用户、导出订单）
     */
    String name() default "";

    /**
     * 操作描述（备注）
     */
    String remark() default "";

    /**
     * 标签（用于检索/分类）
     */
    String[] tags() default {};

    /**
     * 业务类型（查询/新增/修改/删除/导入/导出等）
     */
    BusinessType businessType() default BusinessType.OTHER;

    /**
     * 操作渠道：WEB / APP / MINI_APP / OPEN_API 等
     */
    OperateChannel channel() default OperateChannel.WEB;

    /**
     * 是否保存请求的参数
     */
    boolean isSaveRequestData() default true;

    /**
     * 是否保存响应的参数
     */
    boolean isSaveResponseData() default true;

    /**
     * 排除指定的请求参数
     */
    String[] excludeParamNames() default {};

}
