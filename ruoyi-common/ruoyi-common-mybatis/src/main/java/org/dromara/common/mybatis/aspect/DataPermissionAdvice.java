package org.dromara.common.mybatis.aspect;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.helper.DataPermissionHelper;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 数据权限注解Advice
 *
 * @author 秋辞未寒
 */
public class DataPermissionAdvice implements MethodInterceptor {

    /**
     * 拦截带有数据权限注解的方法调用，设置当前线程的数据权限上下文。
     *
     * @param invocation 方法调用上下文
     * @return 代理方法执行结果
     * @throws Throwable 代理方法执行异常
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object target = invocation.getThis();
        Method method = invocation.getMethod();
        // 设置权限注解
        DataPermissionHelper.setPermission(getDataPermissionAnnotation(target, method));
        try {
            // 执行代理方法
            return invocation.proceed();
        } finally {
            // 清除权限注解
            DataPermissionHelper.removePermission();
        }
    }

    /**
     * 获取数据权限注解
     *
     * @param target 目标对象
     * @param method 当前执行方法
     * @return 数据权限注解，未配置时返回 null
     */
    private DataPermission getDataPermissionAnnotation(Object target, Method method) {
        DataPermission dataPermission = method.getAnnotation(DataPermission.class);
        // 优先获取方法上的注解
        if (dataPermission != null) {
            return dataPermission;
        }
        // 方法上没有注解，则获取类上的注解
        Class<?> targetClass = target.getClass();
        // 如果是 JDK 动态代理，则获取真实的Class实例
        if (Proxy.isProxyClass(targetClass)) {
            return getProxyClassDataPermission(targetClass);
        }
        return targetClass.getAnnotation(DataPermission.class);
    }

    /**
     * 从 JDK 动态代理接口上获取数据权限注解。
     *
     * @param targetClass 代理类
     * @return 数据权限注解，未配置时返回 null
     */
    private DataPermission getProxyClassDataPermission(Class<?> targetClass) {
        for (Class<?> interfaceClass : targetClass.getInterfaces()) {
            DataPermission dataPermission = interfaceClass.getAnnotation(DataPermission.class);
            if (dataPermission != null) {
                return dataPermission;
            }
        }
        return null;
    }
}
