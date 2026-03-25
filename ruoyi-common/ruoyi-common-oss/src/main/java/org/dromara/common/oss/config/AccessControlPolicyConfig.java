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

    @Override
    public @NonNull AccessPolicy accessPolicy() {
        return Optional.ofNullable(accessPolicy)
            .orElse(AccessPolicy.PUBLIC_READ_WRITE);
    }

    @Override
    public AccessControlPolicyConfig copy() {
        return toBuilder().build();
    }

    @Override
    public AccessControlPolicyConfigBuilder toBuilder() {
        return builder()
            .enabled(enabled)
            .accessPolicy(accessPolicy);
    }

}
