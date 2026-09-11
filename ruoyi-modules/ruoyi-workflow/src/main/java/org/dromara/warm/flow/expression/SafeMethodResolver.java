package org.dromara.warm.flow.expression;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.spel.support.DataBindingMethodResolver;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 安全的方法解析器（限制可调用的方法）
 *
 * @author warm
 * @since 2026/3/31
 */
public class SafeMethodResolver implements MethodResolver {

        private static final Set<String> DANGEROUS_METHODS = new HashSet<>(Arrays.asList(
            "getRuntime",
            "exec",
            "forName",
            "loadClass",
            "getClassLoader",
            "setAccessible",
            "newInstance",
            "invoke",
            "getField",
            "getDeclaredField",
            "getMethod",
            "getDeclaredMethod"
        ));


    @Nullable
    @Override
    public MethodExecutor resolve(@NonNull EvaluationContext context, @NonNull Object targetObject
        , @NonNull String name, @NonNull List<TypeDescriptor> argumentTypes) throws AccessException {
        if (DANGEROUS_METHODS.contains(name)) {
            throw new AccessException("不允许调用方法：" + name);
        }

        // 委托给默认的方法解析器
        return DataBindingMethodResolver.forInstanceMethodInvocation()
            .resolve(context, targetObject, name, argumentTypes);
    }
}
