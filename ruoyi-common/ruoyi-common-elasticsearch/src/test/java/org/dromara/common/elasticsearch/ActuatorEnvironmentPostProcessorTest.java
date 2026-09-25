package org.dromara.common.elasticsearch;

import org.dromara.common.elasticsearch.config.ActuatorEnvironmentPostProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("common-elasticsearch 功能单元测试")
class ActuatorEnvironmentPostProcessorTest {

    /**
     * 清理测试写入的 JVM 系统属性，避免影响同一测试进程中的其他用例。
     */
    @AfterEach
    void clearHealthProperty() {
        System.clearProperty("management.health.elasticsearch.enabled");
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

        assertEquals("true", System.getProperty("management.health.elasticsearch.enabled"));
        assertEquals(Ordered.HIGHEST_PRECEDENCE, processor.getOrder());
    }

    /**
     * 验证缺少 Easy-ES 配置时健康检查默认关闭，避免未配置连接时触发探测。
     */
    @Test
    @DisplayName("默认关闭 Elasticsearch 健康检查")
    void shouldDisableElasticsearchHealthByDefault() {
        new ActuatorEnvironmentPostProcessor().postProcessEnvironment(new MockEnvironment(), null);

        assertEquals("false", System.getProperty("management.health.elasticsearch.enabled"));
    }
}
