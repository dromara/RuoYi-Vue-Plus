package org.dromara.common.mybatis.aspect;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;

/**
 * 数据权限注解切面定义
 *
 * @author 秋辞未寒
 */
@SuppressWarnings("all")
public class DataPermissionPointcutAdvisor extends AbstractPointcutAdvisor {

    /**
     * 数据权限通知逻辑。
     */
    private final Advice advice;

    /**
     * 数据权限切点匹配器。
     */
    private final Pointcut pointcut;

    /**
     * 构造数据权限切面定义。
     */
    public DataPermissionPointcutAdvisor() {
        this.advice = new DataPermissionAdvice();
        this.pointcut =  new DataPermissionPointcut();
    }

    /**
     * 获取数据权限切点。
     *
     * @return 数据权限切点
     */
    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    /**
     * 获取数据权限通知。
     *
     * @return 数据权限通知
     */
    @Override
    public Advice getAdvice() {
        return this.advice;
    }

}
