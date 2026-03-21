package org.dromara.common.oss.s3.config;

import cn.hutool.http.HttpUtil;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.oss.constant.OssConstant;
import org.dromara.common.oss.properties.OssProperties;
import org.dromara.common.oss.s3.exception.S3StorageException;
import org.dromara.common.oss.s3.util.BucketUrlUtil;
import org.jspecify.annotations.NonNull;
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
public class S3StorageClientConfig implements Config<S3StorageClientConfig, S3StorageClientConfig.S3StorageClientConfigBuilder>, Serializable {

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
    private final S3AccessControlPolicyConfig accessControlPolicyConfig;

    /**
     * 异步调度池配置
     */
    private final S3AsyncExecutorConfig asyncExecutorConfig;

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
    public @NonNull S3AccessControlPolicyConfig accessControlPolicyConfig() {
        return Optional.ofNullable(accessControlPolicyConfig)
            .orElse(S3AccessControlPolicyConfig.DEFAULT);
    }

    /**
     * ACL访问策略配置
     */
    public @NonNull S3AsyncExecutorConfig asyncExecutorConfig() {
        return Optional.ofNullable(asyncExecutorConfig)
            .orElse(S3AsyncExecutorConfig.DEFAULT);
    }

    /**
     * 获取访问站点URL地址
     *
     * @return 访问站点URL地址
     */
    public String getEndpointUrl() {
        String endpoint = endpoint()
            .filter(s -> !s.isBlank())
            .orElseThrow(() -> S3StorageException.form("endpoint is not configured."));
        return BucketUrlUtil.rebuildUrlHeader(useHttps, endpoint);
    }

    /**
     * 获取域名URL地址
     *
     * @return 域名URL地址
     */
    public String getDomainUrl() {
        return domain()
            // 如果已经配置了自定义域名，则优先使用域名
            // 检查携带协议头
            .filter(s -> HttpUtil.isHttp(s) || HttpUtil.isHttps(s))
            // 否则使用站点
            .orElseGet(this::getEndpointUrl);
    }

    /**
     * 获取桶URL地址
     *
     * @return 桶URL地址
     */
    public String getBucketUrl() {
        // 如果未配置桶，则抛异常
        String bucket = bucket()
            .filter(s -> !s.isBlank())
            .orElseThrow(() -> S3StorageException.form("bucket is not configured."));
        // 如果已经配置了自定义域名，则优先使用域名
        String url = domain()
            // 检查携带协议头
            .filter(s -> HttpUtil.isHttp(s) || HttpUtil.isHttps(s))
            // 否则使用站点
            .orElseGet(() ->
                endpoint()
                    .filter(s -> !s.isBlank())
                    .orElseThrow(() -> S3StorageException.form("endpoint is not configured."))
            );
        // 根据是否使用路径风格配置项决定存储桶的URL风格
        return usePathStyleAccess ? BucketUrlUtil.getPathStyleBucketUrl(useHttps, url, bucket) : BucketUrlUtil.getSiteStyleBucketUrl(useHttps, url, bucket);
    }

    /**
     * 复制S3存储客户端配置对象
     */
    @Override
    public S3StorageClientConfig copy() {
        return toBuilder().build();
    }

    /**
     * 转为S3存储客户端配置构建器对象
     */
    @Override
    public S3StorageClientConfigBuilder toBuilder() {
        return builder()
            .endpoint(endpoint)
            .domain(domain)
            .useHttps(useHttps)
            .usePathStyleAccess(usePathStyleAccess)
            .accessKey(accessKey)
            .secretKey(secretKey)
            .bucket(bucket)
            .region(region)
            .prefix(prefix)
            .accessControlPolicyConfig(accessControlPolicyConfig().copy())
            .asyncExecutorConfig(asyncExecutorConfig().copy());
    }

    public static S3StorageClientConfig formProperties(OssProperties properties){
        return formPropertiesBuilder(properties).build();
    }

    public static S3StorageClientConfigBuilder formPropertiesBuilder(OssProperties properties){
        String regionString = properties.getRegion();
        Region region = Region.US_EAST_1;
        if (StringUtils.isNotBlank(regionString)) {
            region = Region.of(regionString);
        }

        // 是否使用路径风格应当由使用者明确去配置，此处的配置只是为了适配旧的配置项
        // MinIO 使用 HTTPS 限制使用域名访问，站点填域名。需要启用路径样式访问
        boolean usePathStyleAccess = !StringUtils.containsAny(properties.getEndpoint(), OssConstant.CLOUD_SERVICE);

//        // 目前自定义实现的 Client 中并没有实际使用到ACL相关配置，只是作为一个扩展点保留，有需要ACL的自行实现调用逻辑
//        String accessPolicyString = properties.getAccessPolicy();
//        // 绝大多数的云厂商都是不允许操作ACL的，所以此处的默认配置也是禁用ACL的
//        S3AccessControlPolicyConfig accessControlPolicyConfig = S3AccessControlPolicyConfig.DEFAULT;
//        if (StringUtils.isNotBlank(accessPolicyString)) {
//            accessControlPolicyConfig = S3AccessControlPolicyConfig.builder()
//                .enabled(true)
//                .accessPolicy(AccessPolicy.formType(accessPolicyString))
//                .build();
//        }
        return builder()
            .endpoint(properties.getEndpoint())
            .domain(properties.getDomainUrl())
            .accessKey(properties.getAccessKey())
            .secretKey(properties.getSecretKey())
            .bucket(properties.getBucketName())
            .region(region)
            .useHttps(SystemConstants.YES.equals(properties.getIsHttps()))
            .usePathStyleAccess(usePathStyleAccess);
//            .accessControlPolicyConfig(accessControlPolicyConfig);
    }
}
