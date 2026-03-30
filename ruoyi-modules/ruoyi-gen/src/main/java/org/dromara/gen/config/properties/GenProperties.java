package org.dromara.gen.config.properties;

import cn.hutool.extra.template.TemplateConfig;
import lombok.Data;
import org.dromara.common.core.factory.YmlPropertySourceFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 代码生成配置属性
 *
 * @author 秋辞未寒
 */
@Data
@Component
@ConfigurationProperties(prefix = "gen")
@PropertySource(value = "classpath:generator.yml", factory = YmlPropertySourceFactory.class)
public class GenProperties {

    /**
     * 作者
     */
    private String author;

    /**
     * 生成包路径
     */
    private String packageName;

    /**
     * 自动去除表前缀，默认是false
     */
    private boolean autoRemovePre = false;

    /**
     * 表前缀(类名不会包含表前缀)
     */
    private String tablePrefix;

    /**
     * 模板配置
     */
    private TemplateConfig templateConfig = new TemplateConfig(StandardCharsets.UTF_8, null, TemplateConfig.ResourceMode.CLASSPATH);

}
