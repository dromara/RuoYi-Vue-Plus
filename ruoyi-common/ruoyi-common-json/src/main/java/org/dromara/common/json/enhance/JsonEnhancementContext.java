package org.dromara.common.json.enhance;

import lombok.Getter;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 单次响应增强上下文。
 */
@Getter
public class JsonEnhancementContext {

    private final JsonMapper jsonMapper;

    private final Map<String, Object> attributes = new LinkedHashMap<>();

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
     * 设置上下文属性。
     *
     * @param key   属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

}
