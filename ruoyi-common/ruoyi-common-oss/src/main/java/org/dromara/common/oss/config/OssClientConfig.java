package org.dromara.common.oss.config;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.oss.constant.OssConstant;
import org.dromara.common.oss.enums.AccessPolicy;
import org.dromara.common.oss.exception.S3StorageException;
import org.dromara.common.oss.properties.OssProperties;
import org.dromara.common.oss.util.BucketUrlUtil;
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
public class OssClientConfig implements Config<OssClientConfig, OssClientConfig.OssClientConfigBuilder>, Serializable {

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
    private final AccessControlPolicyConfig accessControlPolicyConfig;

    /**
     * 异步调度池配置
     */
    private final OssAsyncExecutorConfig asyncExecutorConfig;

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
     * 根据 OSS 配置属性构建客户端配置。
     *
     * @param properties OSS 配置属性
     * @return 客户端配置
     */
    public static OssClientConfig formProperties(OssProperties properties) {
        return formPropertiesBuilder(properties).build();
    }

    /**
     * 根据 OSS 配置属性构建客户端配置构造器。
     *
     * @param properties OSS 配置属性
     * @return 客户端配置构造器
     */
    public static OssClientConfigBuilder formPropertiesBuilder(OssProperties properties) {
        return builder()
            .endpoint(properties.getEndpoint())
            .domain(properties.getDomainUrl())
            .accessKey(properties.getAccessKey())
            .secretKey(properties.getSecretKey())
            .bucket(properties.getBucketName())
            .region(parseRegion(properties.getRegion()))
            .prefix(properties.getPrefix())
            .useHttps(SystemConstants.YES.equals(properties.getIsHttps()))
            .usePathStyleAccess(resolvePathStyleAccess(properties))
            .accessControlPolicyConfig(resolveAccessControlPolicy(properties.getAccessPolicy()));
    }

    /**
     * 获取访问站点URL地址
     *
     * @return 访问站点URL地址
     */
    public String getEndpointUrl() {
        return BucketUrlUtil.rebuildUrlHeader(useHttps, getEndpoint());
    }

    /**
     * 获取域名URL地址
     *
     * @return 域名URL地址
     */
    public String getDomainUrl() {
        return domain()
            // 如果已经配置了自定义域名，则优先使用域名
            .filter(StringUtils::isNotBlank)
            .map(domain -> BucketUrlUtil.rebuildUrlHeader(useHttps, domain.trim()))
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
        return getBucketUrl(bucket);
    }

    /**
     * 获取桶URL地址
     *
     * @return 桶URL地址
     */
    public String getBucketUrl(String bucket) {
        String url = getAccessBaseUrl();
        // 根据是否使用路径风格配置项决定存储桶的URL风格
        return usePathStyleAccess ? BucketUrlUtil.getPathStyleBucketUrl(useHttps, url, bucket) : BucketUrlUtil.getSiteStyleBucketUrl(useHttps, url, bucket);
    }

    /**
     * 解析 S3 Region。
     *
     * @param regionString Region 字符串
     * @return Region 对象
     */
    private static Region parseRegion(String regionString) {
        if (StringUtils.isBlank(regionString)) {
            return Region.US_EAST_1;
        }
        return Region.of(regionString);
    }

    /**
     * 兼容旧配置推断是否使用路径风格访问。
     *
     * @param properties OSS 配置属性
     * @return 是否使用路径风格访问
     */
    private static boolean resolvePathStyleAccess(OssProperties properties) {
        // 旧配置没有显式路径风格字段，只能继续按内置云厂商 endpoint 做兼容推断。
        return !StringUtils.containsAny(properties.getEndpoint(), OssConstant.CLOUD_SERVICE);
    }

    /**
     * 解析 ACL 访问策略配置。
     *
     * @param accessPolicyString 访问策略字符串
     * @return ACL 访问策略配置
     */
    private static AccessControlPolicyConfig resolveAccessControlPolicy(String accessPolicyString) {
        // 绝大多数云厂商不允许操作 ACL，默认禁用；当前业务只用访问策略判断是否生成预签名 URL。
        if (StringUtils.isBlank(accessPolicyString)) {
            return AccessControlPolicyConfig.DEFAULT;
        }
        return AccessControlPolicyConfig.builder()
            .enabled(true)
            .accessPolicy(AccessPolicy.formType(accessPolicyString))
            .build();
    }

    /**
     * 获取用于访问对象的基础 URL。
     *
     * @return 基础 URL
     */
    private String getAccessBaseUrl() {
        return domain()
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .orElseGet(this::getEndpoint);
    }

    /**
     * 获取 endpoint 配置。
     *
     * @return endpoint
     */
    private String getEndpoint() {
        return endpoint()
            .filter(s -> !s.isBlank())
            .orElseThrow(() -> S3StorageException.form("endpoint is not configured."));
    }

    /**
     * ACL访问策略配置
     */
    public @NonNull AccessControlPolicyConfig accessControlPolicyConfig() {
        return Optional.ofNullable(accessControlPolicyConfig)
            .orElse(AccessControlPolicyConfig.DEFAULT);
    }

    /**
     * ACL访问策略配置
     */
    public @NonNull OssAsyncExecutorConfig asyncExecutorConfig() {
        return Optional.ofNullable(asyncExecutorConfig)
            .orElse(OssAsyncExecutorConfig.DEFAULT);
    }

    /**
     * 复制S3存储客户端配置对象
     */
    @Override
    public OssClientConfig copy() {
        return toBuilder().build();
    }

    /**
     * 转为S3存储客户端配置构建器对象
     */
    @Override
    public OssClientConfigBuilder toBuilder() {
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
}
