package org.dromara.common.oss.s3.builder;

import org.dromara.common.oss.s3.config.S3StorageClientConfig;
import org.dromara.common.oss.s3.exception.S3StorageException;
import org.dromara.common.oss.s3.util.BucketUrlUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;

/**
 * 云服务商文件对象桶URL构建器
 *
 * @author 秋辞未寒
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public enum CloudServiceBucketUrlBuilder implements StrBuilder<S3StorageClientConfig> {

    INSTANCE;

    @Override
    public String build(S3StorageClientConfig config) {
        boolean useHttps = config.useHttps();
        Optional<String> domainOpt = config.domain().filter(s -> !s.isBlank());
        // 如果已经配置了自定义域名，则优先使用域名
        if (domainOpt.isPresent()) {
            // 云服务商一般都支持桶映射到域名，这里不再特殊处理，仅处理链接的协议头即可
            return BucketUrlUtil.getDomainUrl(useHttps, domainOpt.get());
        }
        // 否则使用站点
        String endpoint = config.endpoint()
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> S3StorageException.of("endpoint is not configured."));
        // 如果未配置桶，则抛异常
        String bucket = config.bucket()
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> S3StorageException.of("bucket is not configured."));
        return BucketUrlUtil.getSiteStyleBucketUrl(useHttps, endpoint, bucket);
    }
}
