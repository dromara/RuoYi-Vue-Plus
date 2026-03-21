package org.dromara.common.oss.s3.config;

import lombok.*;
import org.dromara.common.oss.s3.enums.AccessPolicy;
import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;

/**
 * S3 ACL访问策略配置
 *
 * @author 秋辞未寒
 */
@Data
@Builder
@EqualsAndHashCode
public class S3AccessControlPolicyConfig implements Config<S3AccessControlPolicyConfig,S3AccessControlPolicyConfig.S3AccessControlPolicyConfigBuilder>,Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final S3AccessControlPolicyConfig DEFAULT = S3AccessControlPolicyConfig.builder().build();

    /**
     * 是否启用ACL
     */
    private boolean enabled;

    /**
     * 访问策略
     */
    private AccessPolicy accessPolicy;

    public boolean enabled() {
        return enabled;
    }

    public @NonNull AccessPolicy accessPolicy() {
        return Optional.ofNullable(accessPolicy)
            .orElse(AccessPolicy.PRIVATE);
    }

    @Override
    public S3AccessControlPolicyConfig copy() {
        return toBuilder().build();
    }

    @Override
    public S3AccessControlPolicyConfigBuilder toBuilder() {
        return builder()
            .enabled(enabled)
            .accessPolicy(accessPolicy);
    }

}
