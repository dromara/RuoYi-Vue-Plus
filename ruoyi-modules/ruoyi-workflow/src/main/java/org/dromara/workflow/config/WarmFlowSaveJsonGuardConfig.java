package org.dromara.workflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * AU-11 拦截器注册（D-009 · 2026-09-22）：/warm-flow/save-json 双门
 * （容量 413 + workflow:definition:edit 403）。仅注册该单一路径，不影响其它 warm-flow 端点
 * （query-flow-chart 等读路径不受影响）。
 *
 * @author AIP
 * @since 2026-09-22
 */
@Configuration
public class WarmFlowSaveJsonGuardConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new WarmFlowSaveJsonGuard())
            .addPathPatterns("/warm-flow/save-json");
    }
}
