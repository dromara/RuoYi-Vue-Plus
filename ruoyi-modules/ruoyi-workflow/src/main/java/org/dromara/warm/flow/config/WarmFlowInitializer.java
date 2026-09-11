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
package org.dromara.warm.flow.config;

import jakarta.annotation.PostConstruct;
import org.dromara.warm.flow.FlowEngine;
import org.dromara.warm.flow.enums.FrameworkType;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 工作流引擎初始化，各服务由注解扫描注册（见 flow.service 包的 @Service）
 *
 * @author warm
 * @since 2023/6/5 23:01
 */
@Component
@EnableConfigurationProperties(WarmFlowProperties.class)
@ConditionalOnEnable
public class WarmFlowInitializer {

    private static final Logger log = LoggerFactory.getLogger(WarmFlowInitializer.class);

    private final WarmFlowProperties warmFlow;

    public WarmFlowInitializer(WarmFlowProperties warmFlow) {
        this.warmFlow = warmFlow;
    }

    @PostConstruct
    public void init() {
        warmFlow.init();
        warmFlow.setFramework(FrameworkType.SPRING_BOOT);
        FlowEngine.setFlowConfig(warmFlow);
        log.info("【warm-flow】，加载完成");
    }
}
