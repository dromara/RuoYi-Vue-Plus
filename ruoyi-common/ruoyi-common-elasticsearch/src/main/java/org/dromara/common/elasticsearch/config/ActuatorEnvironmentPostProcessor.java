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

    /**
     * 根据 easy-es 开关同步设置 Elasticsearch 健康检查开关。
     *
     * @param environment Spring 环境配置
     * @param application Spring 应用实例
     */
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String enable = environment.getProperty("easy-es.enable", "false");
        System.setProperty("management.health.elasticsearch.enabled", enable);
    }

    /**
     * 返回环境后处理器执行顺序。
     *
     * @return 最高优先级
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
