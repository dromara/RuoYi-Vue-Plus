package org.dromara.common.core.factory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("YmlPropertySourceFactory 单元测试")
class YmlPropertySourceFactoryTest {

    /**
     * 验证公共配置源工厂可以将 YAML 层级结构展开为 Spring 属性键。
     *
     * @throws IOException 配置资源读取失败
     */
    @Test
    @DisplayName("解析 YAML 配置资源")
    void shouldLoadYamlAsProperties() throws IOException {
        ByteArrayResource resource = namedResource("common-test.yml", "feature:\n  enabled: true\n  timeout: 30\n");

        PropertySource<?> source = new YmlPropertySourceFactory()
            .createPropertySource(null, new EncodedResource(resource, StandardCharsets.UTF_8));

        assertEquals("common-test.yml", source.getName());
        assertEquals(true, source.getProperty("feature.enabled"));
        assertEquals(30, source.getProperty("feature.timeout"));
    }

    /**
     * 验证非 YAML 资源仍委托 Spring 默认逻辑解析，避免公共工厂破坏 properties 配置。
     *
     * @throws IOException 配置资源读取失败
     */
    @Test
    @DisplayName("回退解析 properties 配置资源")
    void shouldDelegatePropertiesResourcesToSpring() throws IOException {
        ByteArrayResource resource = namedResource("common-test.properties", "feature.mode=strict\n");

        PropertySource<?> source = new YmlPropertySourceFactory()
            .createPropertySource("fallback", new EncodedResource(resource, StandardCharsets.UTF_8));

        assertEquals("fallback", source.getName());
        assertEquals("strict", source.getProperty("feature.mode"));
    }

    /**
     * 创建具有稳定文件名的内存资源，以触发配置源工厂对应的扩展名分支。
     *
     * @param filename 资源文件名
     * @param content  资源内容
     * @return 命名内存资源
     */
    private static ByteArrayResource namedResource(String filename, String content) {
        return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }
}
