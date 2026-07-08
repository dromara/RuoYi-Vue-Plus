package org.dromara.common.liteflow.config;

import org.dromara.common.liteflow.component.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * LiteFlow 公共节点自动配置。
 *
 * @author Lion Li
 */
@AutoConfiguration
@ConditionalOnProperty(value = "liteflow.enable", havingValue = "true")
public class LiteFlowAutoConfiguration {

    /**
     * 空节点，用于显式表达无需处理的分支。
     *
     * @return 空节点
     */
    @Bean("noop")
    public NoopComponent noopComponent() {
        return new NoopComponent();
    }

    /**
     * 失败节点，用于显式抛出业务失败提示。
     *
     * @return 失败节点
     */
    @Bean("fail")
    public FailComponent failComponent() {
        return new FailComponent();
    }

    /**
     * 上下文必填校验节点。
     *
     * @return 上下文必填校验节点
     */
    @Bean("contextRequired")
    public ContextRequiredComponent contextRequiredComponent() {
        return new ContextRequiredComponent();
    }

    /**
     * 恒为 true 的条件节点。
     *
     * @return true 条件节点
     */
    @Bean("alwaysTrue")
    public AlwaysTrueComponent alwaysTrueComponent() {
        return new AlwaysTrueComponent();
    }

    /**
     * 恒为 false 的条件节点。
     *
     * @return false 条件节点
     */
    @Bean("alwaysFalse")
    public AlwaysFalseComponent alwaysFalseComponent() {
        return new AlwaysFalseComponent();
    }

}
