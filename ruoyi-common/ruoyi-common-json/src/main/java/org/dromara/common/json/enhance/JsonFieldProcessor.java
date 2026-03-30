package org.dromara.common.json.enhance;

/**
 * 响应字段处理器。
 */
public interface JsonFieldProcessor {

    default void collect(JsonFieldContext fieldContext, JsonEnhancementContext context) {
    }

    default void prepare(JsonEnhancementContext context) {
    }

    default Object process(JsonFieldContext fieldContext, Object value, JsonEnhancementContext context) {
        return value;
    }

}
