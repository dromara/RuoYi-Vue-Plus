package org.dromara.common.oss;

import org.dromara.common.oss.enums.AccessPolicy;
import org.dromara.common.oss.exception.S3StorageException;
import org.dromara.common.oss.util.BucketUrlUtil;
import org.dromara.common.oss.config.OssClientConfig;
import org.dromara.common.oss.properties.OssProperties;
import software.amazon.awssdk.regions.Region;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("common-oss 功能单元测试")
class OssFunctionTest {

    /**
     * 验证桶地址生成会规范化已有协议头，并正确区分路径风格与站点风格。
     */
    @Test
    @DisplayName("生成规范的桶访问地址")
    void shouldBuildNormalizedBucketUrls() {
        assertEquals("https://s3.example.com/images",
            BucketUrlUtil.getPathStyleBucketUrl(true, "http://s3.example.com", "images"));
        assertEquals("http://images.s3.example.com",
            BucketUrlUtil.getSiteStyleBucketUrl(false, "https://s3.example.com", "images"));
        assertEquals("https://s3.example.com", BucketUrlUtil.rebuildUrlHeader(true, "HTTP://s3.example.com"));
    }

    /**
     * 验证访问策略类型可以映射到枚举，未知类型会抛出明确的存储异常。
     */
    @Test
    @DisplayName("解析 OSS 访问策略")
    void shouldResolveAccessPolicyOrRejectUnknownType() {
        assertEquals(AccessPolicy.PRIVATE, AccessPolicy.formType("0"));
        assertEquals(AccessPolicy.PUBLIC_READ_WRITE, AccessPolicy.formType("1"));
        assertEquals(AccessPolicy.PUBLIC_READ, AccessPolicy.formType("2"));
        assertThrows(S3StorageException.class, () -> AccessPolicy.formType("9"));
    }

    /**
     * 验证旧版 OSS 属性可转换为客户端配置，并按 endpoint 类型推断路径风格和默认 Region。
     */
    @Test
    @DisplayName("从兼容属性构建 OSS 客户端配置")
    void shouldBuildClientConfigFromLegacyProperties() {
        OssProperties properties = properties("http://minio.example.com", null, "images");
        properties.setRegion(" ");
        properties.setIsHttps("Y");
        properties.setAccessPolicy("2");

        OssClientConfig config = OssClientConfig.formProperties(properties);

        assertTrue(config.useHttps());
        assertTrue(config.usePathStyleAccess());
        assertEquals(Region.US_EAST_1, config.region().orElseThrow());
        assertEquals("https://minio.example.com", config.getEndpointUrl());
        assertEquals("https://minio.example.com/images", config.getBucketUrl());
        assertTrue(config.accessControlPolicyConfig().enabled());
        assertEquals(AccessPolicy.PUBLIC_READ, config.accessControlPolicyConfig().accessPolicy());
    }

    /**
     * 验证自定义域名只直接服务默认桶，访问其他桶时仍回退标准 S3 endpoint 地址。
     */
    @Test
    @DisplayName("区分默认桶和其他桶的自定义域名")
    void shouldUseCustomDomainOnlyForDefaultBucket() {
        OssProperties properties = properties("https://oss-cn-hangzhou.aliyuncs.com", "https://cdn.example.com", "images");
        properties.setRegion("ap-southeast-1");

        OssClientConfig config = OssClientConfig.formProperties(properties);

        assertFalse(config.usePathStyleAccess());
        assertEquals("https://cdn.example.com", config.getBucketUrl());
        assertEquals("https://archive.oss-cn-hangzhou.aliyuncs.com", config.getBucketUrl("archive"));
        assertEquals(Region.AP_SOUTHEAST_1, config.region().orElseThrow());
    }

    /**
     * 验证必要 endpoint 或 bucket 缺失时明确失败，并且复制配置会深复制嵌套配置对象。
     */
    @Test
    @DisplayName("校验 OSS 必要配置并复制客户端配置")
    void shouldValidateRequiredConfigAndCopyNestedSettings() {
        OssClientConfig missingEndpoint = OssClientConfig.builder().bucket("images").build();
        OssClientConfig missingBucket = OssClientConfig.builder().endpoint("s3.example.com").build();
        OssClientConfig config = OssClientConfig.formProperties(properties("s3.example.com", null, "images"));

        assertThrows(S3StorageException.class, missingEndpoint::getEndpointUrl);
        assertThrows(S3StorageException.class, missingBucket::getBucketUrl);

        OssClientConfig copied = config.copy();
        assertNotSame(config, copied);
        assertEquals(config.getEndpointUrl(), copied.getEndpointUrl());
        assertEquals(config.getBucketUrl(), copied.getBucketUrl());
        assertEquals(config.region(), copied.region());
        assertEquals(config.prefix(), copied.prefix());
        assertEquals(config.accessControlPolicyConfig(), copied.accessControlPolicyConfig());
        assertEquals(config.asyncExecutorConfig(), copied.asyncExecutorConfig());
        assertNotSame(config.accessControlPolicyConfig(), copied.accessControlPolicyConfig());
        assertNotSame(config.asyncExecutorConfig(), copied.asyncExecutorConfig());
    }

    /**
     * 创建覆盖 URL 构造所需字段的 OSS 属性。
     *
     * @param endpoint endpoint 地址
     * @param domain   自定义域名
     * @param bucket   默认桶
     * @return OSS 属性
     */
    private static OssProperties properties(String endpoint, String domain, String bucket) {
        OssProperties properties = new OssProperties();
        properties.setEndpoint(endpoint);
        properties.setDomainUrl(domain);
        properties.setBucketName(bucket);
        properties.setAccessKey("access-key");
        properties.setSecretKey("secret-key");
        properties.setPrefix("business");
        properties.setIsHttps("Y");
        return properties;
    }
}
