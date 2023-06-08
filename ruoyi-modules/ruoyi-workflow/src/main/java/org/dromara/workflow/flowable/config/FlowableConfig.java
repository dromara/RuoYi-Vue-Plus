package org.dromara.workflow.flowable.config;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Configuration;

/**
 * flowable配置
 *
 * @author may
 */
@Configuration
public class FlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {

    @Override
    public void configure(SpringProcessEngineConfiguration processEngineConfiguration) {
        processEngineConfiguration.setIdGenerator(IdWorker::getIdStr);
    }
}
