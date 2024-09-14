package org.dromara.workflow.config;


import com.warm.flow.core.handler.TenantHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.dromara.common.satoken.utils.LoginHelper;

/**
 * warmFlow配置
 *
 * @author may
 */
@Configuration
public class WarmFlowConfig {

    /**
     * 全局租户处理器（可配置文件注入，也可用@Bean/@Component方式）
     */
    @Bean
    public TenantHandler tenantHandler() {
        return LoginHelper::getTenantId;
    }
}

