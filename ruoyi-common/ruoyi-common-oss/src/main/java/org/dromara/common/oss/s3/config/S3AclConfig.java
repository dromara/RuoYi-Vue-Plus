package org.dromara.common.oss.s3.config;

import org.dromara.common.oss.s3.enums.AccessPolicy;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;

/**
 * ACL访问策略配置
 *
 * @author 秋辞未寒
 */
@RequiredArgsConstructor
@Builder
@EqualsAndHashCode
public class S3AclConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否启用ACL
     */
    private final boolean enabled;

    /**
     * 访问策略
     */
    private final AccessPolicy accessPolicy;

    public boolean enabled() {
        return enabled;
    }

    public Optional<AccessPolicy> accessPolicy() {
        return Optional.ofNullable(accessPolicy);
    }

    /**
     * 复制ACL访问策略配置对象
     */
    public static S3AclConfig copy(S3AclConfig config) {
        return toBuilder(config).build();
    }

    /**
     * 转为ACL访问策略配置构建器对象
     */
    public static S3AclConfigBuilder toBuilder(S3AclConfig config) {
        return builder()
                .enabled(config.enabled())
                .accessPolicy(config.accessPolicy().orElse(null));
    }
}
