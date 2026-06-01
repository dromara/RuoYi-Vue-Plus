package org.dromara.common.oss.config;

import lombok.Builder;
import org.dromara.common.oss.enums.AccessPolicy;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;

/**
 * S3 ACL访问策略配置
 *
 * @param enabled      是否启用ACL
 * @param accessPolicy 访问策略
 * @author 秋辞未寒
 */
@Builder
public record AccessControlPolicyConfig(
    boolean enabled
    , AccessPolicy accessPolicy
) implements Config<AccessControlPolicyConfig, AccessControlPolicyConfig.AccessControlPolicyConfigBuilder>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 默认访问策略配置
     */
    public static final AccessControlPolicyConfig DEFAULT = AccessControlPolicyConfig.builder()
        .enabled(false)
        .accessPolicy(AccessPolicy.PUBLIC_READ_WRITE)
        .build();

    /**
     * 获取访问策略，未配置时返回默认策略。
     *
     * @return 访问策略
     */
    @Override
    public @NonNull AccessPolicy accessPolicy() {
        return Optional.ofNullable(accessPolicy)
            .orElse(AccessPolicy.PUBLIC_READ_WRITE);
    }

    /**
     * 复制访问策略配置。
     *
     * @return 配置副本
     */
    @Override
    public AccessControlPolicyConfig copy() {
        return toBuilder().build();
    }

    /**
     * 转换为构建器。
     *
     * @return 配置构建器
     */
    @Override
    public AccessControlPolicyConfigBuilder toBuilder() {
        return builder()
            .enabled(enabled)
            .accessPolicy(accessPolicy);
    }

}
