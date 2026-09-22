package org.dromara.workflow.de.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson 2 兼容 Bean 提供器。
 *
 * <p>Spring Boot 4 默认 Bean 是 Jackson 3（{@code tools.jackson.databind.ObjectMapper}）。
 * 本仓 de 包下 6 个业务实现（{@code BrComplianceServiceImpl}、
 * {@code DeBindingServiceImpl}、{@code DeSyServiceImpl}、{@code DeWdServiceImpl}、
 * {@code DeWsServiceImpl}、…）使用的是 Jackson 2 API（{@code com.fasterxml.jackson.databind.ObjectMapper}）。</p>
 *
 * <p>本配置仅暴露一个 Jackson 2 {@link ObjectMapper} bean，让上述实现可直接注入使用，
 * <strong>不替换、不修改</strong>底座的 Jackson 3 默认配置（{@code JacksonConfig}）。</p>
 *
 * <p>位置：{@code org.dromara.workflow.de.config}，属于项目自有代码（de 子包），
 * 不在底座 vendored 范围（见 {@code docs/对底座的偏离登记.md}）。</p>
 *
 * <p>{@code @Primary} 是因为 Spring Boot 4 的 {@code JacksonAutoConfiguration} 不再提供
 * Jackson 2 {@code ObjectMapper}，但 spring-web 中部分过滤器仍按类型查找 ObjectMapper，
 * 标 {@code @Primary} 让 Jackson 2 类型优先。</p>
 *
 * @author AIP
 */
@Configuration
public class Jackson2CompatConfig {

    /**
     * Jackson 2 {@link ObjectMapper} bean，供 de 包下使用 Jackson 2 API 的 Service 注入。
     *
     * @return Jackson 2 ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper jackson2ObjectMapper() {
        return new ObjectMapper();
    }

}