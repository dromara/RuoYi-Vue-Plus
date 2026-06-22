package org.dromara.common.doc.core.customizer;

import cn.hutool.core.io.IoUtil;
import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.oas.annotations.tags.Tags;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springdoc.core.providers.JavadocProvider;
import org.springdoc.core.service.OpenAPIService;
import org.springdoc.core.utils.PropertyResolverUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.method.HandlerMethod;

import java.io.StringReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Controller 类级标签增强。
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
public class ClassTagOperationCustomizer implements GlobalOperationCustomizer, GlobalOpenApiCustomizer {

    /**
     * JavaDoc 提供器。
     */
    private final Optional<JavadocProvider> javadocProvider;

    /**
     * SpringDoc 属性解析工具。
     */
    private final PropertyResolverUtils propertyResolverUtils;

    /**
     * 已解析的 OpenAPI 顶层标签缓存。
     */
    private final Map<String, Tag> tags = new ConcurrentHashMap<>();

    /**
     * 已被类 JavaDoc 替换的自动标签名称集合。
     */
    private final Set<String> replacedAutoTagNames = ConcurrentHashMap.newKeySet();

    /**
     * 自定义接口操作的标签信息。
     *
     * @param operation     OpenAPI 操作对象
     * @param handlerMethod 处理器方法
     * @return 自定义后的 OpenAPI 操作对象
     */
    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        Class<?> beanType = handlerMethod.getBeanType();
        List<io.swagger.v3.oas.annotations.tags.Tag> classTags = getClassTags(beanType);
        if (!CollectionUtils.isEmpty(classTags)) {
            // 优先使用 Controller 类上的 @Tag / @Tags，保持 Swagger 原生注解语义
            addAnnotationTags(operation, classTags);
            return operation;
        }

        String tagName = getClassJavadocTagName(beanType);
        if (StringUtils.isBlank(tagName)) {
            return operation;
        }

        String autoTagName = OpenAPIService.splitCamelCase(beanType.getSimpleName());
        if (!shouldUseClassJavadocTag(operation, autoTagName)) {
            return operation;
        }

        // 无显式 @Tag 时，将 springdoc 自动生成的类名 tag 替换为类 JavaDoc 第一行
        removeOperationTag(operation, autoTagName);
        addOperationTag(operation, tagName);
        replacedAutoTagNames.add(autoTagName);
        tags.putIfAbsent(tagName, new Tag().name(tagName).description(javadocProvider.get().getClassJavadoc(beanType)));
        return operation;
    }

    /**
     * 自定义 OpenAPI 顶层标签集合。
     *
     * @param openApi OpenAPI 文档对象
     */
    @Override
    public void customise(OpenAPI openApi) {
        if (!CollectionUtils.isEmpty(openApi.getTags()) && !CollectionUtils.isEmpty(replacedAutoTagNames)) {
            // 移除已被 JavaDoc tag 替换的默认类名 tag，避免 Swagger UI 出现空分组
            openApi.getTags().removeIf(tag -> replacedAutoTagNames.contains(tag.getName()));
        }
        // 将类级 @Tag 描述或 JavaDoc 描述补充到 OpenAPI 顶层 tags
        tags.values().forEach(tag -> {
            if (openApi.getTags() == null || openApi.getTags().stream().noneMatch(item -> Objects.equals(item.getName(), tag.getName()))) {
                openApi.addTagsItem(tag);
            }
        });
    }

    /**
     * 获取 Controller 类上的 Swagger 标签注解。
     *
     * @param beanType Controller 类型
     * @return 标签注解列表
     */
    private List<io.swagger.v3.oas.annotations.tags.Tag> getClassTags(Class<?> beanType) {
        Set<Tags> tagsSet = AnnotatedElementUtils.findAllMergedAnnotations(beanType, Tags.class);
        Set<io.swagger.v3.oas.annotations.tags.Tag> mergedTags = tagsSet.stream()
            .flatMap(item -> Stream.of(item.value()))
            .collect(Collectors.toSet());
        mergedTags.addAll(AnnotatedElementUtils.findAllMergedAnnotations(beanType, io.swagger.v3.oas.annotations.tags.Tag.class));
        return new ArrayList<>(mergedTags);
    }

    /**
     * 将类级 Swagger 标签追加到接口操作与顶层标签缓存。
     *
     * @param operation OpenAPI 操作对象
     * @param classTags 类级标签注解
     */
    private void addAnnotationTags(Operation operation, List<io.swagger.v3.oas.annotations.tags.Tag> classTags) {
        classTags.stream()
            .map(io.swagger.v3.oas.annotations.tags.Tag::name)
            .map(name -> propertyResolverUtils.resolve(name, Locale.getDefault()))
            .filter(StringUtils::isNotBlank)
            .forEach(name -> addOperationTag(operation, name));

        AnnotationsUtils.getTags(classTags.toArray(new io.swagger.v3.oas.annotations.tags.Tag[0]), true)
            .ifPresent(items -> items.forEach(tag -> {
                tag.name(propertyResolverUtils.resolve(tag.getName(), Locale.getDefault()));
                tag.description(propertyResolverUtils.resolve(tag.getDescription(), Locale.getDefault()));
                if (StringUtils.isNotBlank(tag.getName())) {
                    tags.putIfAbsent(tag.getName(), tag);
                }
            }));
    }

    /**
     * 获取类 JavaDoc 第一行作为标签名称。
     *
     * @param beanType Controller 类型
     * @return 标签名称，未配置 JavaDoc 时返回 null
     */
    private String getClassJavadocTagName(Class<?> beanType) {
        if (javadocProvider.isEmpty()) {
            return null;
        }
        String description = javadocProvider.get().getClassJavadoc(beanType);
        if (StringUtils.isBlank(description)) {
            return null;
        }
        // 与原 OpenApiHandler 保持一致：类 JavaDoc 第一行作为 tag 名，完整 JavaDoc 作为 tag 描述
        List<String> lines = IoUtil.readLines(new StringReader(description), new ArrayList<>());
        return lines.stream().filter(StringUtils::isNotBlank).findFirst().orElse(null);
    }

    /**
     * 判断当前操作是否可以使用类 JavaDoc 标签替换默认标签。
     *
     * @param operation   OpenAPI 操作对象
     * @param autoTagName SpringDoc 自动生成的标签名
     * @return true 可以替换 false 不替换
     */
    private boolean shouldUseClassJavadocTag(Operation operation, String autoTagName) {
        return CollectionUtils.isEmpty(operation.getTags()) || operation.getTags().contains(autoTagName);
    }

    /**
     * 为接口操作追加标签。
     *
     * @param operation OpenAPI 操作对象
     * @param tagName   标签名称
     */
    private void addOperationTag(Operation operation, String tagName) {
        if (operation.getTags() == null) {
            operation.setTags(new ArrayList<>());
        }
        if (!operation.getTags().contains(tagName)) {
            operation.addTagsItem(tagName);
        }
    }

    /**
     * 从接口操作中移除指定标签。
     *
     * @param operation OpenAPI 操作对象
     * @param tagName   标签名称
     */
    private void removeOperationTag(Operation operation, String tagName) {
        if (!CollectionUtils.isEmpty(operation.getTags())) {
            operation.getTags().removeIf(item -> Objects.equals(item, tagName));
        }
    }

}
