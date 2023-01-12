package com.ruoyi.common.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.SimpleCache;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.cglib.beans.BeanCopier;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.cglib.core.Converter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * bean深拷贝工具(基于 cglib 性能优异)
 * <p>
 * 重点 cglib 不支持 拷贝到链式对象
 * 例如: 源对象 拷贝到 目标(链式对象)
 * 请区分好`浅拷贝`和`深拷贝`再做使用
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BeanCopyUtils {

    /**
     * 单对象基于class创建拷贝
     *
     * @param source 数据来源实体
     * @param desc   描述对象 转换后的对象
     * @return desc
     */
    public static <T, V> V copy(T source, Class<V> desc) {
        if (ObjectUtil.isNull(source)) {
            return null;
        }
        if (ObjectUtil.isNull(desc)) {
            return null;
        }
        final V target = ReflectUtil.newInstanceIfPossible(desc);
        return copy(source, target);
    }

    /**
     * 单对象基于对象创建拷贝
     *
     * @param source 数据来源实体
     * @param desc   转换后的对象
     * @return desc
     */
    public static <T, V> V copy(T source, V desc) {
        if (ObjectUtil.isNull(source)) {
            return null;
        }
        if (ObjectUtil.isNull(desc)) {
            return null;
        }
        BeanCopier beanCopier = BeanCopierCache.INSTANCE.get(source.getClass(), desc.getClass(), null);
        beanCopier.copy(source, desc, null);
        return desc;
    }

    /**
     * 列表对象基于class创建拷贝
     *
     * @param sourceList 数据来源实体列表
     * @param desc       描述对象 转换后的对象
     * @return desc
     */
    public static <T, V> List<V> copyList(List<T> sourceList, Class<V> desc) {
        if (ObjectUtil.isNull(sourceList)) {
            return null;
        }
        if (CollUtil.isEmpty(sourceList)) {
            return CollUtil.newArrayList();
        }
        return StreamUtils.toList(sourceList, source -> {
            V target = ReflectUtil.newInstanceIfPossible(desc);
            copy(source, target);
            return target;
        });
    }

    /**
     * bean拷贝到map
     *
     * @param bean 数据来源实体
     * @return map对象
     */
    @SuppressWarnings("unchecked")
    public static <T> Map<String, Object> copyToMap(T bean) {
        if (ObjectUtil.isNull(bean)) {
            return null;
        }
        return BeanMap.create(bean);
    }

    /**
     * map拷贝到bean
     *
     * @param map       数据来源
     * @param beanClass bean类
     * @return bean对象
     */
    public static <T> T mapToBean(Map<String, Object> map, Class<T> beanClass) {
        if (MapUtil.isEmpty(map)) {
            return null;
        }
        if (ObjectUtil.isNull(beanClass)) {
            return null;
        }
        T bean = ReflectUtil.newInstanceIfPossible(beanClass);
        return mapToBean(map, bean);
    }

    /**
     * map拷贝到bean
     *
     * @param map  数据来源
     * @param bean bean对象
     * @return bean对象
     */
    public static <T> T mapToBean(Map<String, Object> map, T bean) {
        if (MapUtil.isEmpty(map)) {
            return null;
        }
        if (ObjectUtil.isNull(bean)) {
            return null;
        }
        BeanMap.create(bean).putAll(map);
        return bean;
    }

    /**
     * map拷贝到map
     *
     * @param map   数据来源
     * @param clazz 返回的对象类型
     * @return map对象
     */
    public static <T, V> Map<String, V> mapToMap(Map<String, T> map, Class<V> clazz) {
        if (MapUtil.isEmpty(map)) {
            return null;
        }
        if (ObjectUtil.isNull(clazz)) {
            return null;
        }
        Map<String, V> copyMap = new LinkedHashMap<>(map.size());
        map.forEach((k, v) -> copyMap.put(k, copy(v, clazz)));
        return copyMap;
    }

    /**
     * BeanCopier属性缓存<br>
     * 缓存用于防止多次反射造成的性能问题
     *
     * @author Looly
     * @since 5.4.1
     */
    public enum BeanCopierCache {
        /**
         * BeanCopier属性缓存单例
         */
        INSTANCE;

        private final SimpleCache<String, BeanCopier> cache = new SimpleCache<>();

        /**
         * 获得类与转换器生成的key在{@link BeanCopier}的Map中对应的元素
         *
         * @param srcClass    源Bean的类
         * @param targetClass 目标Bean的类
         * @param converter   转换器
         * @return Map中对应的BeanCopier
         */
        public BeanCopier get(Class<?> srcClass, Class<?> targetClass, Converter converter) {
            final String key = genKey(srcClass, targetClass, converter);
            return cache.get(key, () -> BeanCopier.create(srcClass, targetClass, converter != null));
        }

        /**
         * 获得类与转换器生成的key
         *
         * @param srcClass    源Bean的类
         * @param targetClass 目标Bean的类
         * @param converter   转换器
         * @return 属性名和Map映射的key
         */
        private String genKey(Class<?> srcClass, Class<?> targetClass, Converter converter) {
            final StringBuilder key = StrUtil.builder()
                .append(srcClass.getName()).append('#').append(targetClass.getName());
            if (null != converter) {
                key.append('#').append(converter.getClass().getName());
            }
            return key.toString();
        }
    }

}
