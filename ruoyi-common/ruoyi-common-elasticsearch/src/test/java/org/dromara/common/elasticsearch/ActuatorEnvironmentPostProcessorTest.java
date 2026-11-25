package org.dromara.common.elasticsearch;

import org.dromara.common.elasticsearch.config.ActuatorEnvironmentPostProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("common-elasticsearch 功能单元测试")
class ActuatorEnvironmentPostProcessorTest {

    @Test
    @DisplayName("独立应用环境使用各自的 Easy-ES 开关")
    void shouldKeepHealthSettingsLocalToEnvironment() {
        var enabled = new MockEnvironment().withProperty("easy-es.enable", "true");
        var disabled = new MockEnvironment();
        var processor = new ActuatorEnvironmentPostProcessor();
        processor.postProcessEnvironment(enabled, null);
        processor.postProcessEnvironment(disabled, null);
        assertEquals("true", enabled.getProperty("management.health.elasticsearch.enabled"));
        assertEquals("false", disabled.getProperty("management.health.elasticsearch.enabled"));
    }

    /**
     * 验证 Easy-ES 开关会同步到 Elasticsearch 健康检查，并以最高优先级执行。
     */
    @Test
    @DisplayName("同步 Elasticsearch 健康检查开关")
    void shouldSynchronizeElasticsearchHealthFlag() {
        ActuatorEnvironmentPostProcessor processor = new ActuatorEnvironmentPostProcessor();
        MockEnvironment environment = new MockEnvironment().withProperty("easy-es.enable", "true");

        processor.postProcessEnvironment(environment, null);

        assertEquals("true", environment.getProperty("management.health.elasticsearch.enabled"));
        assertEquals(Ordered.HIGHEST_PRECEDENCE, processor.getOrder());
    }

    /**
     * 验证缺少 Easy-ES 配置时健康检查默认关闭，避免未配置连接时触发探测。
     */
    @Test
    @DisplayName("默认关闭 Elasticsearch 健康检查")
    void shouldDisableElasticsearchHealthByDefault() {
        var environment = new MockEnvironment();
        new ActuatorEnvironmentPostProcessor().postProcessEnvironment(environment, null);

        assertEquals("false", environment.getProperty("management.health.elasticsearch.enabled"));
    }
}
