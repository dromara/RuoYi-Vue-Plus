package org.dromara.common.json.enhance;

import lombok.Getter;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 单次响应增强上下文。
 */
@Getter
public class JsonEnhancementContext {

    private final JsonMapper jsonMapper;

    private final Map<String, Object> attributes = new LinkedHashMap<>();

    private boolean processingRequired;

    /**
     * 构造响应增强上下文。
     *
     * @param jsonMapper JSON 映射器
     */
    public JsonEnhancementContext(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * 获取上下文属性。
     *
     * @param key 属性键
     * @param <T> 属性值类型
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /**
     * 获取上下文属性，不存在时创建并写入。
     *
     * @param key      属性键
     * @param supplier 属性值创建器
     * @param <T>      属性值类型
     * @return 属性值
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrCreateAttribute(String key, Supplier<T> supplier) {
        Object value = attributes.computeIfAbsent(key, ignored -> supplier.get());
        return (T) value;
    }

    /**
     * 设置上下文属性。
     *
     * @param key   属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * 判断上下文是否包含指定属性。
     */
    public boolean containsAttribute(String key) {
        return attributes.containsKey(key);
    }

    /**
     * 移除上下文属性。
     */
    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    /**
     * 标记本次响应存在需要处理的字段。
     */
    public void markProcessingRequired() {
        this.processingRequired = true;
    }

}
