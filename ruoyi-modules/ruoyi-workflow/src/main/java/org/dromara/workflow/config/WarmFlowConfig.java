package org.dromara.workflow.config;

import org.dromara.workflow.common.ConditionalOnEnable;
import org.springframework.context.annotation.Configuration;

/**
 * WarmFlow 工作流配置入口，在工作流开关开启时注册相关组件。
 *
 * @author may
 */
@ConditionalOnEnable
@Configuration
public class WarmFlowConfig {

}

