package com.ruoyi.framework.config;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafProperties;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.spring5.ISpringTemplateEngine;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * springboot-admin server配置类
 *
 * @author Lion Li
 */
@Configuration
@EnableAdminServer
public class AdminServerConfig {

    @Lazy
    @Bean(name = TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    @ConditionalOnMissingBean(Executor.class)
    public ThreadPoolTaskExecutor applicationTaskExecutor(TaskExecutorBuilder builder) {
        return builder.build();
    }

    /**
     * 解决 admin 与 项目 页面的交叉引用 将 admin 的路由放到最后
     * @param properties
     * @param templateResolvers
     * @param dialects
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(ISpringTemplateEngine.class)
    SpringTemplateEngine templateEngine(ThymeleafProperties properties,
                                        ObjectProvider<ITemplateResolver> templateResolvers, ObjectProvider<IDialect> dialects) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setEnableSpringELCompiler(properties.isEnableSpringElCompiler());
        engine.setRenderHiddenMarkersBeforeCheckboxes(properties.isRenderHiddenMarkersBeforeCheckboxes());
        templateResolvers.orderedStream().forEach(engine::addTemplateResolver);
        dialects.orderedStream().forEach(engine::addDialect);
        Set<ITemplateResolver> templateResolvers1 = engine.getTemplateResolvers();
        templateResolvers1 = templateResolvers1.stream()
                .sorted(Comparator.comparing(ITemplateResolver::getOrder))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        engine.setTemplateResolvers(templateResolvers1);
        return engine;
    }
}
