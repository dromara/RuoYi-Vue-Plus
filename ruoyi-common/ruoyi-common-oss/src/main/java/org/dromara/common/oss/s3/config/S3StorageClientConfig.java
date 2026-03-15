package org.dromara.common.oss.s3.config;

import org.dromara.common.oss.s3.builder.CloudServiceBucketUrlBuilder;
import org.dromara.common.oss.s3.builder.MinioBucketUrlBuilder;
import org.dromara.common.oss.s3.exception.S3StorageException;
import org.dromara.common.oss.s3.util.BucketUrlUtil;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.regions.Region;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;

/**
 * S3存储客户端配置
 *
 * @author 秋辞未寒
 */
@RequiredArgsConstructor
@Builder
@EqualsAndHashCode
public class S3StorageClientConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 访问端点
     */
    private final String endpoint;

    /**
     * 自定义域名
     */
    private final String domain;

    /**
     * 是否使用HTTPS协议
     */
    private final boolean useHttps;

    /**
     * 是否使用路径样式访问（使用域名需要启用路径样式访问）
     */
    private final boolean usePathStyleAccess;

    /**
     * ACCESS_KEY
     */
    private final String accessKey;

    /**
     * SECRET_KEY
     */
    private final String secretKey;

    /**
     * 存储桶
     */
    private final String bucket;

    /**
     * 存储区域
     */
    private final Region region;

    /**
     * 前缀
     */
    private final String prefix;

    /**
     * ACL访问策略配置
     */
    private final S3AclConfig aclConfig;

    /**
     * 访问端点
     */
    public Optional<String> endpoint() {
        return Optional.ofNullable(endpoint);
    }

    /**
     * 自定义域名
     */
    public Optional<String> domain() {
        return Optional.ofNullable(domain);
    }
    /**
     * 是否使用HTTPS协议
     */
    public boolean useHttps() {
        return useHttps;
    }

    /**
     * 是否使用路径样式访问（使用域名需要启用路径样式访问）
     */
    public boolean usePathStyleAccess() {
        return usePathStyleAccess;
    }

    /**
     * ACCESS_KEY
     */
    public Optional<String> accessKey() {
        return Optional.ofNullable(accessKey);
    }

    /**
     * SECRET_KEY
     */
    public Optional<String> secretKey() {
        return Optional.ofNullable(secretKey);
    }

    /**
     * 存储桶
     */
    public Optional<String> bucket() {
        return Optional.ofNullable(bucket);
    }

    /**
     * 存储区域
     */
    public Optional<Region> region() {
        return Optional.ofNullable(region);
    }

    /**
     * 前缀
     */
    public Optional<String> prefix() {
        return Optional.ofNullable(prefix);
    }

    /**
     * ACL访问策略配置
     */
    public Optional<S3AclConfig> aclConfig() {
        return Optional.ofNullable(aclConfig);
    }

    /**
     * 获取访问站点URL地址
     *
     * @return 访问站点URL地址
     */
    public String getEndpointUrl(){
        String endpoint = endpoint()
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> S3StorageException.of("endpoint is not configured."));
        return BucketUrlUtil.getDomainUrl(useHttps, endpoint);
    }

    /**
     * 获取域名URL地址
     *
     * @return 域名URL地址
     */
    public String getDomainUrl(){
        return domain()
                .filter(s -> !s.isBlank())
                // 如果已经配置了自定义域名，则优先使用域名
                .map(s -> BucketUrlUtil.getDomainUrl(useHttps, s))
                // 否则使用站点
                .orElseGet(this::getEndpointUrl);
    }

    /**
     * 获取桶URL地址
     *
     * @param isCloudService 是否是云服务商
     * @return 桶URL地址
     */
    public String getBucketUrl(boolean isCloudService){
        // 如果是云服务商，则优先使用云服务商的
        if (isCloudService) {
            return CloudServiceBucketUrlBuilder.INSTANCE.build(this);
        }
        // 否则默认使用MinIO的
        return MinioBucketUrlBuilder.INSTANCE.build(this);
    }

    /**
     * 复制S3存储客户端配置对象
     */
    public static S3StorageClientConfig copy(S3StorageClientConfig config) {
        return toBuilder(config).build();
    }

    /**
     * 转为S3存储客户端配置构建器对象
     */
    public static S3StorageClientConfigBuilder toBuilder(S3StorageClientConfig config) {
        S3StorageClientConfigBuilder builder = builder()
                .endpoint(config.endpoint().orElse(null))
                .domain(config.domain().orElse(null))
                .useHttps(config.useHttps())
                .usePathStyleAccess(config.usePathStyleAccess())
                .accessKey(config.accessKey().orElse(null))
                .secretKey(config.secretKey().orElse(null))
                .bucket(config.bucket().orElse(null))
                .region(config.region().orElse(null))
                .prefix(config.prefix().orElse(null));
        config.aclConfig().ifPresent(s3AclConfig -> builder.aclConfig(S3AclConfig.copy(s3AclConfig)));
        return builder;
    }
}
