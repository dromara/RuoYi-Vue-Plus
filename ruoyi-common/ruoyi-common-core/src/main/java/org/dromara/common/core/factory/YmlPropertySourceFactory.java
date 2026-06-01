package org.dromara.common.core.factory;

import org.dromara.common.core.utils.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;

import java.io.IOException;
import java.util.Objects;

/**
 * yml 配置源工厂
 *
 * @author Lion Li
 */
public class YmlPropertySourceFactory extends DefaultPropertySourceFactory {

    /**
     * 创建配置属性源，支持将 yml/yaml 资源解析为 PropertiesPropertySource。
     *
     * @param name     属性源名称
     * @param resource 配置资源
     * @return 属性源
     * @throws IOException 读取配置资源失败时抛出
     */
    @Override
    public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource) throws IOException {
        String sourceName = resource.getResource().getFilename();
        if (StringUtils.isNotBlank(sourceName) && StringUtils.endsWithAny(sourceName, ".yml", ".yaml")) {
            YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
            factory.setResources(resource.getResource());
            factory.afterPropertiesSet();
            return new PropertiesPropertySource(sourceName, Objects.requireNonNull(factory.getObject()));
        }
        return super.createPropertySource(name, resource);
    }

}
