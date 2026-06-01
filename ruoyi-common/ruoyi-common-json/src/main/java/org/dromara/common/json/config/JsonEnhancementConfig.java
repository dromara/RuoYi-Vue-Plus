package org.dromara.common.json.config;

import org.dromara.common.json.enhance.JsonFieldProcessor;
import org.dromara.common.json.enhance.JsonValueEnhancer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

/**
 * 响应增强核心配置。
 */
@AutoConfiguration
public class JsonEnhancementConfig {

    /**
     * 创建 JSON 字段增强处理器入口。
     *
     * @param jsonMapper JSON 映射器
     * @param processors 字段处理器集合
     * @return JSON 值增强器
     */
    @Bean
    public JsonValueEnhancer jsonValueEnhancer(JsonMapper jsonMapper, List<JsonFieldProcessor> processors) {
        return new JsonValueEnhancer(jsonMapper, processors);
    }

}
