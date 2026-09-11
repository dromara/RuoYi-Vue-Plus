/*
 *    Copyright 2024-2025, Warm-Flow (290631660@qq.com).
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.dromara.warm.flow.ui.config;

import org.dromara.warm.flow.ui.controller.WarmFlowController;
import org.dromara.warm.flow.ui.controller.WarmFlowUiController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 工作流设计器配置类
 *
 * @author ruoyi
 */
@Configuration
@ConditionalOnProperty(value = "warm-flow.ui", havingValue = "true", matchIfMissing = true)
@Import({WarmFlowUiController.class
    , WarmFlowController.class})
public class WarmFlowUiConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /** warm-flow配置 */
        registry.addResourceHandler("/warm-flow-ui/**")
            .addResourceLocations("classpath:/META-INF/resources/warm-flow-ui/", "classpath:/warm-flow-ui/");

    }
}
