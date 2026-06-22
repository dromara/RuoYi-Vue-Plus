package org.dromara.common.doc.core.customizer;

import io.swagger.v3.oas.models.Operation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.dromara.common.doc.core.resolver.JavadocResolver;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springdoc.core.providers.JavadocProvider;
import org.springframework.util.CollectionUtils;
import org.springframework.web.method.HandlerMethod;

import java.util.List;
import java.util.Optional;

/**
 * 方法 JavaDoc 与扩展描述增强。
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
public class JavadocOperationCustomizer implements GlobalOperationCustomizer {

    /**
     * JavaDoc 提供器。
     */
    private final Optional<JavadocProvider> javadocProvider;

    /**
     * JavaDoc 扩展解析器列表。
     */
    private final List<JavadocResolver> javadocResolvers;

    /**
     * 自定义接口操作的 JavaDoc 描述。
     *
     * @param operation     OpenAPI 操作对象
     * @param handlerMethod 处理器方法
     * @return 自定义后的 OpenAPI 操作对象
     */
    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        javadocProvider.ifPresent(provider -> {
            String description = provider.getMethodJavadocDescription(handlerMethod.getMethod());
            if (StringUtils.isNotBlank(description)) {
                // 使用方法 JavaDoc 首句作为接口摘要，完整 JavaDoc 作为接口描述
                operation.setSummary(provider.getFirstSentence(description));
                operation.setDescription(description);
            }
        });

        if (CollectionUtils.isEmpty(javadocResolvers)) {
            return operation;
        }

        StringBuilder description = new StringBuilder(Optional.ofNullable(operation.getDescription()).orElse(""));
        List<String> resolvedDescriptions = javadocResolvers.stream()
            .sorted()
            // 只执行支持当前 HandlerMethod 的扩展解析器，避免无注解接口被追加权限说明
            .filter(resolver -> resolver.supports(handlerMethod))
            .map(resolver -> resolver.resolve(handlerMethod, operation))
            .filter(StringUtils::isNotBlank)
            .toList();
        if (!resolvedDescriptions.isEmpty()) {
            // 在原方法 JavaDoc 后追加权限等扩展描述，保持现有 resolver 扩展点
            resolvedDescriptions.forEach(description::append);
            operation.setDescription(description.toString());
        }
        return operation;
    }

}
