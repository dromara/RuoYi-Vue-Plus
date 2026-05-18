package org.dromara.common.elasticsearch.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * 健康检查配置注入
 *
 * @author Lion Li
 */
public class ActuatorEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String enable = environment.getProperty("easy-es.enable", "false");
        System.setProperty("management.health.elasticsearch.enabled", enable);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
