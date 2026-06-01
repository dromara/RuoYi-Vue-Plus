package org.dromara.common.encrypt.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 加密字段处理器。
 *
 * @author Lion Li
 */
@Slf4j
public class EncryptedFieldProcessor {

    private final EncryptorManager encryptorManager;
    private final EncryptContextFactory contextFactory;

    /**
     * 构造加密字段处理器。
     *
     * @param encryptorManager 加解密管理器
     * @param contextFactory 加密上下文工厂
     */
    public EncryptedFieldProcessor(EncryptorManager encryptorManager, EncryptContextFactory contextFactory) {
        this.encryptorManager = encryptorManager;
        this.contextFactory = contextFactory;
    }

    /**
     * 加密对象字段，并返回原始字段快照。
     *
     * @param sourceObject 待加密对象
     * @return 原始字段快照
     */
    public List<FieldSnapshot> encrypt(Object sourceObject) {
        List<FieldSnapshot> snapshots = new ArrayList<>();
        handle(sourceObject, Collections.newSetFromMap(new IdentityHashMap<>()), (target, field, value) -> {
            String encrypt = encryptorManager.encrypt(value, contextFactory.create(field));
            if (!Objects.equals(value, encrypt)) {
                snapshots.add(new FieldSnapshot(target, field, value));
                field.set(target, encrypt);
            }
        });
        return snapshots;
    }

    /**
     * 解密对象字段。
     *
     * @param sourceObject 待解密对象
     */
    public void decrypt(Object sourceObject) {
        handle(sourceObject, Collections.newSetFromMap(new IdentityHashMap<>()), (target, field, value) ->
            field.set(target, encryptorManager.decrypt(value, contextFactory.create(field))));
    }

    private void handle(Object sourceObject, Set<Object> visited, FieldHandler fieldHandler) {
        if (ObjectUtil.isNull(sourceObject) || sourceObject instanceof String || visited.contains(sourceObject)) {
            return;
        }
        visited.add(sourceObject);
        if (sourceObject instanceof Map<?, ?> map) {
            new HashSet<>(map.values()).forEach(value -> handle(value, visited, fieldHandler));
            return;
        }
        if (sourceObject instanceof Collection<?> collection) {
            if (CollUtil.isEmpty(collection)) {
                return;
            }
            collection.forEach(item -> handle(item, visited, fieldHandler));
            return;
        }
        Set<Field> fields = encryptorManager.getFieldCache(sourceObject.getClass());
        if (CollUtil.isEmpty(fields)) {
            return;
        }
        try {
            for (Field field : fields) {
                String value = Convert.toStr(field.get(sourceObject));
                if (ObjectUtil.isNotNull(value)) {
                    fieldHandler.handle(sourceObject, field, value);
                }
            }
        } catch (Exception e) {
            log.error("处理加密字段时出错", e);
        }
    }

    /**
     * 加密字段处理回调。
     */
    @FunctionalInterface
    private interface FieldHandler {

        /**
         * 处理单个加密字段。
         *
         * @param target 字段所属对象
         * @param field 加密字段
         * @param value 字段原始字符串值
         * @throws IllegalAccessException 字段访问失败时抛出
         */
        void handle(Object target, Field field, String value) throws IllegalAccessException;
    }

    /**
     * 字段原始值快照。
     */
    public record FieldSnapshot(Object target, Field field, Object value) {

        /**
         * 恢复原始字段值。
         */
        public void restore() {
            try {
                field.set(target, value);
            } catch (IllegalAccessException e) {
                log.error("恢复加密字段时出错", e);
            }
        }
    }
}
