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

    @Bean
    public JsonValueEnhancer jsonValueEnhancer(JsonMapper jsonMapper, List<JsonFieldProcessor> processors) {
        return new JsonValueEnhancer(jsonMapper, processors);
    }

}
