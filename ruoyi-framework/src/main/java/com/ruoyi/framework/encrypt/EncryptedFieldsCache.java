package com.ruoyi.framework.encrypt;

import com.ruoyi.common.annotation.EncryptField;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 类加密字段缓存类
 *
 * @author 老马
 * @date 2023-01-12 11:07
 */
public class EncryptedFieldsCache {
    private static final Map<Class<?>, Set<Field>> ENCRYPTED_FIELD_CACHE = new ConcurrentHashMap<>();

    public static Set<Field> get(Class<?> sourceClazz) {
        return ENCRYPTED_FIELD_CACHE.computeIfAbsent(sourceClazz, clazz -> {
            Field[] declaredFields = clazz.getDeclaredFields();
            Set<Field> fieldSet = Arrays.stream(declaredFields).filter(field ->
                    field.isAnnotationPresent(EncryptField.class) && field.getType() == String.class)
                .collect(Collectors.toSet());
            for (Field field : fieldSet) {
                field.setAccessible(true);
            }
            return fieldSet;
        });
    }
}
