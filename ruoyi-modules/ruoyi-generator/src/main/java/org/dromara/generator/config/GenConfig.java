package org.dromara.generator.config;

import cn.hutool.extra.template.TemplateConfig;
import org.dromara.common.core.factory.YmlPropertySourceFactory;
import org.dromara.generator.config.properties.GenProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

/**
 * 读取代码生成相关配置
 *
 * @author ruoyi
 */
@AutoConfiguration
@EnableConfigurationProperties(GenProperties.class)
@PropertySource(value = "classpath:generator.yml", factory = YmlPropertySourceFactory.class)
public class GenConfig {

    private static GenProperties genProperties;

    public GenConfig(GenProperties genProperties) {
        GenConfig.genProperties = genProperties;
    }

    public static String getAuthor() {
        return genProperties.getAuthor();
    }

    public static String getPackageName() {
        return genProperties.getPackageName();
    }

    public static boolean getAutoRemovePre() {
        return genProperties.isAutoRemovePre();
    }

    public static String getTablePrefix() {
        return genProperties.getTablePrefix();
    }

    public static TemplateConfig getTemplateConfig() {
        return genProperties.getTemplateConfig();
    }
}
