package org.dromara.workflow.flowable.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.dromara.workflow.flowable.handler.TaskTimeoutJobHandler;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;


/**
 * flowable配置
 *
 * @author may
 */
@Configuration
public class FlowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {

    @Autowired
    private GlobalFlowableListener globalFlowableListener;
    @Autowired
    private IdentifierGenerator identifierGenerator;

    @Override
    public void configure(SpringProcessEngineConfiguration processEngineConfiguration) {
        processEngineConfiguration.setIdGenerator(() -> identifierGenerator.nextId(null).toString());
        processEngineConfiguration.setEventListeners(Collections.singletonList(globalFlowableListener));
        processEngineConfiguration.addCustomJobHandler(new TaskTimeoutJobHandler());
    }
}
