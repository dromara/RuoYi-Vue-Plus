package org.dromara.common.core.utils.reflect;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.exceptions.UtilException;
import cn.hutool.core.lang.Dict;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Map;

/**
 * 注解工具类
 *
 * @author 秋辞未寒
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnnotationUtils extends AnnotationUtil {

    /**
     * 获取指定注解
     *
     * @param annotationEle  {@link AnnotatedElement}，可以是Class、Method、Field、Constructor、ReflectPermission
     * @param annotationTypeName 注解类型名称
     * @return 注解对象
     */
    @SuppressWarnings("unchecked")
    public static Annotation getAnnotation(AnnotatedElement annotationEle, String annotationTypeName) {
        try {
            return AnnotationUtil.getAnnotation(annotationEle, (Class<? extends Annotation>) Class.forName(annotationTypeName));
        } catch (final ClassNotFoundException | ClassCastException e) {
            // ignore
            log.error("AnnotationUtils.getAnnotation(AnnotatedElement, String) error.", e);
            return  null;
        }
    }

    /**
     * 获取指定注解中所有属性值<br>
     * 如果无指定的属性方法返回null
     *
     * @param annotationEle  {@link AnnotatedElement}，可以是Class、Method、Field、Constructor、ReflectPermission
     * @param annotationTypeName 注解类型名称
     * @return 注解对象所有属性键值
     * @throws UtilException 调用注解中的方法时执行异常
     */
    @SuppressWarnings("unchecked")
    public static Dict getAnnotationValueMap(AnnotatedElement annotationEle, String annotationTypeName) throws UtilException {
        try {
            Map<String, Object> annotationValueMap = AnnotationUtil.getAnnotationValueMap(annotationEle, (Class<? extends Annotation>) Class.forName(annotationTypeName));
            return new Dict(annotationValueMap);
        } catch (final ClassNotFoundException | ClassCastException e) {
            // ignore
            log.error("AnnotationUtils.getAnnotationValueMap(AnnotatedElement, String) error.", e);
            return  null;
        }
    }


}
