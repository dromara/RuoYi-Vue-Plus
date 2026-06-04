package org.dromara.common.doc.core.resolver;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ClassLoaderUtil;
import io.swagger.v3.oas.models.Operation;
import org.springframework.web.method.HandlerMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 抽象元数据 Javadoc 解析器
 *
 * @param <M> 元数据类型
 * @author 秋辞未寒
 */
public abstract class AbstractMetadataJavadocResolver<M> implements JavadocResolver {

    /**
     * 最高优先级。
     */
    public static final int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

    /**
     * 最低优先级。
     */
    public static final int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    /**
     * 元数据提供者。
     */
    private final Supplier<M> metadataProvider;

    /**
     * 解析器排序值。
     */
    private final int order;

    /**
     * 构造元数据 Javadoc 解析器。
     *
     * @param metadataProvider 元数据提供者
     */
    public AbstractMetadataJavadocResolver(Supplier<M> metadataProvider) {
        this(metadataProvider, LOWEST_PRECEDENCE);
    }

    /**
     * 构造带排序值的元数据 Javadoc 解析器。
     *
     * @param metadataProvider 元数据提供者
     * @param order            排序值
     */
    public AbstractMetadataJavadocResolver(Supplier<M> metadataProvider, int order) {
        this.metadataProvider = metadataProvider;
        this.order = order;
    }

    /**
     * 获取解析器排序值。
     *
     * @return 排序值
     */
    @Override
    public int getOrder() {
        return order;
    }

    /**
     * 使用当前元数据解析接口文档描述。
     *
     * @param handlerMethod 处理器方法
     * @param operation     Swagger Operation 实例
     * @return 解析后的 Javadoc 内容
     */
    @Override
    public String resolve(HandlerMethod handlerMethod, Operation operation) {
        return resolve(handlerMethod, operation, metadataProvider.get());
    }

    /**
     * 执行解析并返回解析到的 Javadoc 内容
     *
     * @param handlerMethod 处理器方法
     * @param operation     Swagger Operation实例
     * @param metadata      元信息
     * @return 解析到的 Javadoc 内容
     */
    public abstract String resolve(HandlerMethod handlerMethod, Operation operation, M metadata);

    /**
     * 检查处理器方法所属的类上是否存在注解
     *
     * @param handlerMethod   处理器方法
     * @param annotationClass 注解类
     * @return 是否存在注解
     */
    public boolean hasClassAnnotation(HandlerMethod handlerMethod, Class<? extends Annotation> annotationClass) {
        return AnnotationUtil.hasAnnotation(handlerMethod.getBeanType(), annotationClass);
    }

    /**
     * 检查处理器方法所属的类上是否存在注解
     *
     * @param handlerMethod      处理器方法
     * @param annotationTypeName 注解类名称
     * @return 是否存在注解
     */
    public boolean hasClassAnnotation(HandlerMethod handlerMethod, String annotationTypeName) {
        return AnnotationUtil.hasAnnotation(handlerMethod.getBeanType(), annotationTypeName);
    }

    /**
     * 检查处理器方法上是否存在注解
     *
     * @param handlerMethod   处理器方法
     * @param annotationClass 注解类
     * @return 是否存在注解
     */
    public boolean hasMethodAnnotation(HandlerMethod handlerMethod, Class<? extends Annotation> annotationClass) {
        return AnnotationUtil.hasAnnotation(handlerMethod.getMethod(), annotationClass);
    }

    /**
     * 检查处理器方法上是否存在注解
     *
     * @param handlerMethod      处理器方法
     * @param annotationTypeName 注解类名称
     * @return 是否存在注解
     */
    public boolean hasMethodAnnotation(HandlerMethod handlerMethod, String annotationTypeName) {
        return AnnotationUtil.hasAnnotation(handlerMethod.getMethod(), annotationTypeName);
    }

    /**
     * 检查处理器方法上是否存在注解
     *
     * @param handlerMethod   处理器方法
     * @param annotationClass 注解类
     * @return 是否存在注解
     */
    public boolean hasAnnotation(HandlerMethod handlerMethod, Class<? extends Annotation> annotationClass) {
        return this.hasClassAnnotation(handlerMethod, annotationClass) || this.hasMethodAnnotation(handlerMethod, annotationClass);
    }

    /**
     * 检查处理器方法上是否存在注解
     *
     * @param handlerMethod      处理器方法
     * @param annotationTypeName 注解类名称
     * @return 是否存在注解
     */
    public boolean hasAnnotation(HandlerMethod handlerMethod, String annotationTypeName) {
        return this.hasClassAnnotation(handlerMethod, annotationTypeName) || this.hasMethodAnnotation(handlerMethod, annotationTypeName);
    }

    /**
     * 获取处理器方法所属类上的注解的值
     *
     * @param handlerMethod   处理器方法
     * @param annotationClass 注解类
     * @return 注解的值
     */
    public Map<String, Object> getClassAnnotationValueMap(HandlerMethod handlerMethod, Class<? extends Annotation> annotationClass) {
        return AnnotationUtil.getAnnotationValueMap(handlerMethod.getBeanType(), annotationClass);
    }

    /**
     * 获取处理器方法所属类上的注解的值
     *
     * @param handlerMethod       处理器方法
     * @param annotationClassName 注解类名称
     * @return 注解的值
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getClassAnnotationValueMap(HandlerMethod handlerMethod, String annotationClassName) {
        Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) ClassLoaderUtil.loadClass(annotationClassName, false);
        return AnnotationUtil.getAnnotationValueMap(handlerMethod.getBeanType(), annotationClass);
    }

    /**
     * 获取处理器方法上的注解的值
     *
     * @param handlerMethod   处理器方法
     * @param annotationClass 注解类
     * @return 注解的值
     */
    public Map<String, Object> getMethodAnnotationValueMap(HandlerMethod handlerMethod, Class<? extends Annotation> annotationClass) {
        return AnnotationUtil.getAnnotationValueMap(handlerMethod.getMethod(), annotationClass);
    }

    /**
     * 获取处理器方法所属类上的注解的值
     *
     * @param handlerMethod       处理器方法
     * @param annotationClassName 注解类名称
     * @return 注解的值
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMethodAnnotationValueMap(HandlerMethod handlerMethod, String annotationClassName) {
        Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) ClassLoaderUtil.loadClass(annotationClassName, false);
        return AnnotationUtil.getAnnotationValueMap(handlerMethod.getMethod(), annotationClass);
    }

    /**
     * 获取指定元素上的注解属性映射。
     *
     * @param annotatedElement 被注解元素
     * @param annotationClass  注解类型
     * @return 注解属性映射
     */
    private Map<String, Object> getAnnotationValueMap(AnnotatedElement annotatedElement, Class<? extends Annotation> annotationClass) {
        return AnnotationUtil.getAnnotationValueMap(annotatedElement, annotationClass);
    }
}
