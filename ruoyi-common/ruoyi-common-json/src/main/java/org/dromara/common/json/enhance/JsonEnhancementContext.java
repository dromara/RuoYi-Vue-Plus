package org.dromara.common.json.enhance;

import lombok.Getter;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单次响应增强上下文。
 */
@Getter
public class JsonEnhancementContext {

    private final JsonMapper jsonMapper;

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    public JsonEnhancementContext(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

}
