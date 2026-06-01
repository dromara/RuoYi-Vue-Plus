package org.dromara.common.mybatis.aspect;

import org.dromara.common.mybatis.annotation.DataPermission;
import org.springframework.aop.support.StaticMethodMatcherPointcut;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 数据权限匹配切点
 *
 * @author 秋辞未寒
 */
public class DataPermissionPointcut extends StaticMethodMatcherPointcut {

    /**
     * 判断当前方法或目标类型是否命中数据权限切点。
     *
     * @param method      当前执行方法
     * @param targetClass 目标类型
     * @return true 命中数据权限切点 false 未命中
     */
    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        // 优先匹配方法
        // 数据权限注解不对继承生效，所以检查当前方法是否有注解即可，不再往上匹配父类或接口
        if (method.isAnnotationPresent(DataPermission.class)) {
            return true;
        }

        // MyBatis 的 Mapper 就是通过 JDK 动态代理实现的，所以这里需要检查是否匹配 JDK 的动态代理
        Class<?> targetClassRef = resolveTargetClass(targetClass);
        return targetClassRef.isAnnotationPresent(DataPermission.class);
    }

    /**
     * 解析真实目标类型，兼容 MyBatis Mapper 的 JDK 动态代理类。
     *
     * @param targetClass Spring AOP 传入的目标类型
     * @return 真实目标类型或可匹配数据权限注解的接口类型
     */
    private Class<?> resolveTargetClass(Class<?> targetClass) {
        if (!Proxy.isProxyClass(targetClass)) {
            return targetClass;
        }
        for (Class<?> interfaceClass : targetClass.getInterfaces()) {
            // 数据权限注解不对继承生效，但由于 SpringIOC 容器拿到的实际上是 MyBatis 代理过后的 Mapper，而 targetClass.isAnnotationPresent 实际匹配的是 Proxy 类的注解，不会查找代理类。
            // 所以这里不能用 targetClass.isAnnotationPresent，只能用 AnnotatedElementUtils.hasAnnotation 或 targetClass.getInterfaces()[0].isAnnotationPresent 去做匹配，以检查被代理的 MapperClass 是否具有注解
            // 原理：JDK 动态代理本质上就是对接口进行实现然后对具体的接口实现做代理，所以直接通过接口可以拿到实际的 MapperClass
            if (interfaceClass.isAnnotationPresent(DataPermission.class)) {
                return interfaceClass;
            }
        }
        return targetClass;
    }

}
