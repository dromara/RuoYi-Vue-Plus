package org.dromara.common.doc.core.resolver;

import io.swagger.v3.oas.models.Operation;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.Ordered;
import org.springframework.web.method.HandlerMethod;

/**
 * Javadoc解析器接口
 *
 * @author echo
 * @author 秋辞未寒
 */
public interface JavadocResolver extends Comparable<JavadocResolver>, Ordered {

    /**
     * 检查解析器是否支持解析 HandlerMethod
     *
     * @param handlerMethod 处理器方法
     * @return 是否支持解析
     */
    boolean supports(HandlerMethod handlerMethod);

    /**
     * 执行解析并返回解析到的 Javadoc 内容
     *
     * @param handlerMethod 处理器方法
     * @param operation     Swagger Operation实例
     * @return 解析到的 Javadoc 内容
     */
    String resolve(HandlerMethod handlerMethod, Operation operation);

    /**
     * 获取解析器优先级。
     *
     * @return 优先级
     */
    default int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * 获取解析器的名称
     *
     * @return 解析器名称
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 比较解析器执行顺序。
     *
     * @param o 其他解析器
     * @return 比较结果
     */
    @Override
    default int compareTo(@NotNull JavadocResolver o) {
        return Integer.compare(getOrder(), o.getOrder());
    }
}
