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

    public static final int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;
    public static final int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    private final Supplier<M> metadataProvider;

    private final int order;

    public AbstractMetadataJavadocResolver(Supplier<M> metadataProvider) {
        this(metadataProvider, LOWEST_PRECEDENCE);
    }

    public AbstractMetadataJavadocResolver(Supplier<M> metadataProvider, int order) {
        this.metadataProvider = metadataProvider;
        this.order = order;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public String resolve(HandlerMethod handlerMethod, Operation operation) {
        return resolve(handlerMethod, operation, metadataProvider.get());
    }

    /**
     * 执行解析并返回解析到的 Javadoc 内容
     * @param handlerMethod 处理器方法
     * @param operation Swagger Operation实例
     * @param metadata 元信息
     * @return 解析到的 Javadoc 内容
     */
    public abstract String resolve(HandlerMethod handlerMethod, Operation operation, M metadata);

    /**
     * 检查处理器方法所属的类上是否存在注解
     * @param handlerMethod 处理器方法
     * @param annotationClass 注解类
     * @return 是否存在注解
     */
    public boolean hasClassAnnotation(HandlerMethod handlerMethod,Class<? extends Annotation> annotationClass){
        return AnnotationUtil.hasAnnotation(handlerMethod.getBeanType(), annotationClass);
    }

    /**
     * 检查处理器方法所属的类上是否存在注解
     * @param handlerMethod 处理器方法
     * @param annotationTypeName 注解类名称
     * @return 是否存在注解
     */
    public boolean hasClassAnnotation(HandlerMethod handlerMethod, String annotationTypeName){
        return AnnotationUtil.hasAnnotation(handlerMethod.getBeanType(), annotationTypeName);
    }

    /**
     * 检查处理器方法上是否存在注解
     * @param handlerMethod 处理器方法
     * @param annotationClass 注解类
     * @return 是否存在注解
     */
    public boolean hasMethodAnnotation(HandlerMethod handlerMethod,Class<? extends Annotation> annotationClass){
        return AnnotationUtil.hasAnnotation(handlerMethod.getMethod(), annotationClass);
    }

    /**
     * 检查处理器方法上是否存在注解
     * @param handlerMethod 处理器方法
     * @param annotationTypeName 注解类名称
     * @return 是否存在注解
     */
    public boolean hasMethodAnnotation(HandlerMethod handlerMethod, String annotationTypeName){
        return AnnotationUtil.hasAnnotation(handlerMethod.getMethod(), annotationTypeName);
    }

    /**
     * 检查处理器方法上是否存在注解
     * @param handlerMethod 处理器方法
     * @param annotationClass 注解类
     * @return 是否存在注解
     */
    public boolean hasAnnotation(HandlerMethod handlerMethod,Class<? extends Annotation> annotationClass){
        return this.hasClassAnnotation(handlerMethod, annotationClass) || this.hasMethodAnnotation(handlerMethod, annotationClass);
    }

    /**
     * 检查处理器方法上是否存在注解
     * @param handlerMethod 处理器方法
     * @param annotationTypeName 注解类名称
     * @return 是否存在注解
     */
    public boolean hasAnnotation(HandlerMethod handlerMethod, String annotationTypeName){
        return this.hasClassAnnotation(handlerMethod, annotationTypeName) || this.hasMethodAnnotation(handlerMethod, annotationTypeName);
    }

    /**
     * 获取处理器方法所属类上的注解的值
     * @param handlerMethod 处理器方法
     * @param annotationClass 注解类
     * @return 注解的值
     */
    public Map<String, Object> getClassAnnotationValueMap(HandlerMethod handlerMethod, Class<? extends Annotation> annotationClass) {
        return AnnotationUtil.getAnnotationValueMap(handlerMethod.getBeanType(), annotationClass);
    }

    /**
     * 获取处理器方法所属类上的注解的值
     * @param handlerMethod 处理器方法
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
     * @param handlerMethod 处理器方法
     * @param annotationClass 注解类
     * @return 注解的值
     */
    public Map<String, Object> getMethodAnnotationValueMap(HandlerMethod handlerMethod, Class<? extends Annotation> annotationClass) {
        return AnnotationUtil.getAnnotationValueMap(handlerMethod.getMethod(), annotationClass);
    }

    /**
     * 获取处理器方法所属类上的注解的值
     * @param handlerMethod 处理器方法
     * @param annotationClassName 注解类名称
     * @return 注解的值
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMethodAnnotationValueMap(HandlerMethod handlerMethod, String annotationClassName) {
        Class<? extends Annotation> annotationClass = (Class<? extends Annotation>) ClassLoaderUtil.loadClass(annotationClassName, false);
        return AnnotationUtil.getAnnotationValueMap(handlerMethod.getMethod(), annotationClass);
    }

    private Map<String, Object> getAnnotationValueMap(AnnotatedElement annotatedElement, Class<? extends Annotation> annotationClass) {
        return AnnotationUtil.getAnnotationValueMap(annotatedElement, annotationClass);
    }
}
