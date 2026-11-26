package org.dromara.common.oss;

import org.dromara.common.oss.enums.AccessPolicy;
import org.dromara.common.oss.exception.S3StorageException;
import org.dromara.common.oss.util.BucketUrlUtil;
import org.dromara.common.oss.config.OssClientConfig;
import org.dromara.common.oss.client.DefaultOssClientImpl;
import org.dromara.common.oss.properties.OssProperties;
import software.amazon.awssdk.regions.Region;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.dromara.common.oss.model.HandleAsyncResult;
import org.dromara.common.oss.model.PartUploadResult;
import org.dromara.common.oss.model.PutObjectResult;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import software.amazon.awssdk.services.s3.model.*;
import org.dromara.common.oss.io.OutputStreamDownloadSubscriber;
import org.dromara.common.oss.model.DeleteObjectsResult;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("common-oss 功能单元测试")
class OssFunctionTest {

    @Test
    void pathKeysNormalizeBusinessPrefixesAndPreserveOnlyTheExtension() {
        var props = properties("s3.example.com", null, "images");
        props.setPrefix(" //attachments// ");
        var client = new DefaultOssClientImpl("keys", OssClientConfig.formProperties(props));
        String datedId = "[0-9]{4}/[0-9]{2}/[0-9]{2}/[0-9a-f]{32}";
        assertTrue(client.buildPathKey("/orders/", "quarter.final.pdf").matches("attachments/orders/" + datedId + "[.]pdf"));
        assertTrue(client.buildPathKey("README").matches("attachments/" + datedId));
        props.setPrefix(null);
        var plain = new DefaultOssClientImpl("keys", OssClientConfig.formProperties(props));
        assertTrue(plain.buildPathKey(null).matches(datedId));
        assertTrue(plain.buildPathKey("orders", "a.csv").matches("orders/" + datedId + "[.]csv"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "https://files.example.com/base"})
    void signedUrlsBindTheObjectLifetimeMetadataAndPartNumber(String domain) throws Exception {
        var config = OssClientConfig.formProperties(properties("s3.example.com", domain, "images"));
        try (var client = new DefaultOssClientImpl("sign", config)) {
            client.initialize();
            var ttl = Duration.ofMinutes(4);
            var get = java.net.URI.create(client.presignGetUrl("a b", ttl));
            assertTrue(get.getRawPath().endsWith("/a%20b"));
            assertTrue(get.getQuery().contains("X-Amz-Expires=240"));
            if (!domain.isEmpty()) {
                assertEquals("files.example.com", get.getHost());
                assertEquals("/base/a%20b", get.getRawPath());
            }
            var upload = java.net.URI.create(client.presignPutUrl("a b", ttl, java.util.Map.of("owner", "sales")));
            assertTrue(upload.getQuery().contains("x-amz-meta-owner"));
            var part = java.net.URI.create(client.presignUploadPartUrl("a b", "batch", 4, ttl));
            assertTrue(part.getQuery().contains("partNumber=4"));
            assertTrue(part.getQuery().contains("uploadId=batch"));
            assertEquals(get.getRawPath(), part.getRawPath());
        }
    }

    @Test
    void uploadUsesActualByteLengthAndPreservesMetadataAndSdkErrors() {
        var config = OssClientConfig.formProperties(properties("s3.example.com", null, "images"));
        var client = spy(new DefaultOssClientImpl("upload", config));
        var options = org.dromara.common.oss.model.Options.create().setLength(90L).setContentType("text/plain")
            .addMetadataItem("project", "reports").addMetadataItem("owner", "sales");
        doAnswer(call -> {
            var builder = PutObjectRequest.builder();
            call.<Consumer<PutObjectRequest.Builder>>getArgument(1).accept(builder);
            var request = builder.build();
            assertEquals("images", request.bucket());
            assertEquals("report", request.key());
            assertEquals(3L, request.contentLength());
            assertEquals("text/plain", request.contentType());
            assertEquals(java.util.Map.of("project", "reports", "owner", "sales"), request.metadata());
            return HandleAsyncResult.success(PutObjectResponse.builder().eTag("etag").build());
        }).when(client).doCustomUpload(any(), any(), nullable(java.util.Collection.class));
        byte[] bytes = {1, 2, 3};
        assertEquals(3L, client.upload("report", bytes, options).size());
        var failure = S3StorageException.form("service unavailable");
        doReturn(HandleAsyncResult.failure(new java.util.concurrent.CompletionException(failure)))
            .when(client).doCustomUpload(any(), any(), nullable(java.util.Collection.class));
        assertEquals(failure, assertThrows(S3StorageException.class, () -> client.upload("report", bytes)));
        doReturn(HandleAsyncResult.success(null)).when(client).doCustomUpload(any(), any(), nullable(java.util.Collection.class));
        assertThrows(S3StorageException.class, () -> client.upload("report", bytes));
    }

    @Test
    void emptyTagsClearTheObjectWhileNonemptyTagsReplaceItsTagSet() {
        var client = new DefaultOssClientImpl("tags", OssClientConfig.formProperties(properties("s3.example.com", null, "images")));
        S3AsyncClient sdk = mock();
        ReflectionTestUtils.setField(client, "s3AsyncClient", sdk);
        when(sdk.putObjectTagging(org.mockito.ArgumentMatchers.<Consumer<PutObjectTaggingRequest.Builder>>any()))
            .thenAnswer(call -> {
                var builder = PutObjectTaggingRequest.builder();
                call.<Consumer<PutObjectTaggingRequest.Builder>>getArgument(0).accept(builder);
                var req = builder.build();
                assertEquals("images", req.bucket());
                assertEquals("report", req.key());
                assertEquals(List.of(Tag.builder().key("owner").value("sales").build()), req.tagging().tagSet());
                return CompletableFuture.completedFuture(PutObjectTaggingResponse.builder().build());
            });
        when(sdk.deleteObjectTagging(org.mockito.ArgumentMatchers.<Consumer<DeleteObjectTaggingRequest.Builder>>any()))
            .thenAnswer(call -> {
                var builder = DeleteObjectTaggingRequest.builder();
                call.<Consumer<DeleteObjectTaggingRequest.Builder>>getArgument(0).accept(builder);
                assertEquals("report", builder.build().key());
                return CompletableFuture.completedFuture(DeleteObjectTaggingResponse.builder().build());
            });
        when(sdk.getObjectTagging(org.mockito.ArgumentMatchers.<Consumer<GetObjectTaggingRequest.Builder>>any()))
            .thenReturn(CompletableFuture.completedFuture(GetObjectTaggingResponse.builder()
                .tagSet(Tag.builder().key("owner").value("sales").build()).build()));
        var tags = java.util.Map.of("owner", "sales");
        client.putObjectTags("report", tags);
        assertEquals(tags, client.getObjectTags("report"));
        client.putObjectTags("report", java.util.Map.of());
        verify(sdk).deleteObjectTagging(org.mockito.ArgumentMatchers.<Consumer<DeleteObjectTaggingRequest.Builder>>any());
    }
    @Test
    void listObjectsForwardsPagingParametersAndReturnsTheNextCursor() {
        var config = OssClientConfig.formProperties(properties("s3.example.com", null, "images"));
        var client = new DefaultOssClientImpl("list", config);
        S3AsyncClient sdk = mock();
        ReflectionTestUtils.setField(client, "s3AsyncClient", sdk);
        when(sdk.listObjectsV2(org.mockito.ArgumentMatchers.<Consumer<ListObjectsV2Request.Builder>>any()))
            .thenAnswer(call -> {
                var builder = ListObjectsV2Request.builder();
                call.<Consumer<ListObjectsV2Request.Builder>>getArgument(0).accept(builder);
                var request = builder.build();
                assertEquals("images", request.bucket());
                assertEquals("reports/", request.prefix());
                assertEquals("page-2", request.continuationToken());
                assertEquals(1, request.maxKeys());
                var object = S3Object.builder().key("reports/a").size(17L).eTag("tag")
                    .lastModified(Instant.EPOCH).build();
                return CompletableFuture.completedFuture(ListObjectsV2Response.builder()
                    .contents(object).nextContinuationToken("page-3").build());
            });
        var page = client.listObjects("reports/", "page-2", 1);
        assertEquals("page-3", page.nextContinuationToken());
        assertEquals("reports/a", page.objects().getFirst().key());
        assertEquals(17L, page.objects().getFirst().size());
        assertEquals("tag", page.objects().getFirst().eTag());
    }

    @Test
    void batchDeleteReturnsIndividualErrorsAndRejectsInvalidBatchSizes() {
        var config = OssClientConfig.formProperties(properties("s3.example.com", null, "images"));
        var client = new DefaultOssClientImpl("delete", config);
        S3AsyncClient sdk = mock();
        ReflectionTestUtils.setField(client, "s3AsyncClient", sdk);
        assertThrows(IllegalArgumentException.class, () -> client.deleteObjects(null));
        assertThrows(IllegalArgumentException.class, () -> client.deleteObjects(List.of()));
        assertThrows(IllegalArgumentException.class, () -> client.deleteObjects(java.util.Collections.nCopies(1001, "x")));
        verifyNoInteractions(sdk);
        when(sdk.deleteObjects(org.mockito.ArgumentMatchers.<Consumer<DeleteObjectsRequest.Builder>>any()))
            .thenAnswer(call -> {
                var builder = DeleteObjectsRequest.builder();
                call.<Consumer<DeleteObjectsRequest.Builder>>getArgument(0).accept(builder);
                var request = builder.build();
                assertEquals("images", request.bucket());
                assertEquals(List.of("one", "two"), request.delete().objects().stream().map(ObjectIdentifier::key).toList());
                return CompletableFuture.completedFuture(DeleteObjectsResponse.builder()
                    .deleted(DeletedObject.builder().key("one").build())
                    .errors(S3Error.builder().key("two").code("AccessDenied").message("locked").build()).build());
            });
        var result = client.deleteObjects(List.of("one", "two"));
        assertEquals(List.of("one"), result.deletedKeys());
        assertEquals(List.of(new DeleteObjectsResult.Error("two", "AccessDenied", "locked")), result.errors());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void downloadWritesOnlyRemainingBytesAndHonorsChannelOwnership(boolean ownsChannel) throws Exception {
        var output = new ByteArrayOutputStream();
        try (var channel = Channels.newChannel(output)) {
            try (var subscriber = OutputStreamDownloadSubscriber.create(channel, ownsChannel)) {
                subscriber.accept(ByteBuffer.wrap(new byte[]{0, 1, 2, 3}, 1, 2));
                subscriber.accept(ByteBuffer.wrap(new byte[]{4}));
            }
            org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[]{1, 2, 4}, output.toByteArray());
            assertEquals(!ownsChannel, channel.isOpen());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void listPartsUsesRequestedOrDefaultPageAndSortsResults(boolean customPage) {
        var config = OssClientConfig.formProperties(properties("s3.example.com", null, "images"));
        var client = new DefaultOssClientImpl("parts", config);
        S3AsyncClient sdk = mock();
        ReflectionTestUtils.setField(client, "s3AsyncClient", sdk);
        when(sdk.listParts(org.mockito.ArgumentMatchers.<Consumer<ListPartsRequest.Builder>>any()))
            .thenAnswer(call -> {
                var builder = ListPartsRequest.builder();
                call.<Consumer<ListPartsRequest.Builder>>getArgument(0).accept(builder);
                var request = builder.build();
                assertEquals(customPage ? 4 : 1000, request.maxParts());
                assertEquals(customPage ? 1 : 0, request.partNumberMarker());
                assertEquals("upload", request.uploadId());
                assertEquals("archive", request.key());
                assertEquals("images", request.bucket());
                return CompletableFuture.completedFuture(ListPartsResponse.builder().parts(
                    Part.builder().partNumber(3).eTag("third").build(),
                    Part.builder().partNumber(2).eTag("second").build()).build());
            });
        var parts = client.listParts("archive", "upload", customPage ? 4 : null, customPage ? 1 : null);
        assertEquals(List.of(new PartUploadResult(2, "second"), new PartUploadResult(3, "third")), parts);
    }
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("普通上传和分片合并返回可访问的对象地址并保留原始键")
    void shouldEncodeUploadedObjectUrl(boolean multipart) {
        var config = OssClientConfig.formProperties(properties("oss-cn-hangzhou.aliyuncs.com",
            "https://cdn.example.com/assets/", "images"));
        var client = spy(new DefaultOssClientImpl("test", config));
        String key = "目录/a b?#%2F.txt";
        PutObjectResult result;
        if (multipart) {
            S3AsyncClient s3 = mock();
            ReflectionTestUtils.setField(client, "s3AsyncClient", s3);
            when(s3.completeMultipartUpload(org.mockito.ArgumentMatchers.<Consumer<CompleteMultipartUploadRequest.Builder>>any()))
                .thenReturn(CompletableFuture.completedFuture(CompleteMultipartUploadResponse.builder().eTag("etag").build()));
            result = client.bucketCompleteMultipartUpload("images", key, "upload", List.of(new PartUploadResult(1, "part")));
        } else {
            doReturn(HandleAsyncResult.success(PutObjectResponse.builder().eTag("etag").build()))
                .when(client).doCustomUpload(any(), any(), nullable(java.util.Collection.class));
            result = client.bucketUpload("images", key, new byte[]{1});
        }
        assertEquals(key, result.key());
        assertEquals("https://cdn.example.com/assets/%E7%9B%AE%E5%BD%95/a%20b%3F%23%252F.txt", result.url());
    }

    @Test
    @DisplayName("路径风格桶地址保留 endpoint 前缀且只拼接一个斜杠")
    void shouldAppendBucketAfterEndpointPath() {
        var config = OssClientConfig.formProperties(properties("http://minio.example.com/storage/", null, "images"));
        assertEquals("https://minio.example.com/storage/images", config.getBucketUrl());
    }

    @Test
    @DisplayName("云厂商域名的大小写不影响桶寻址方式")
    void shouldRecognizeMixedCaseCloudEndpoint() {
        var config = OssClientConfig.formProperties(properties("OSS-CN-HANGZHOU.ALIYUNCS.COM", null, "images"));
        assertFalse(config.usePathStyleAccess());
    }

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

    @Test
    @DisplayName("临时下载签名包含附件文件名")
    void shouldSignAttachmentDispositionForDefaultAndBoundDomain() throws Exception {
        for (String domain : new String[]{"", "https://files.example.com"}) {
            OssClientConfig config = OssClientConfig.formProperties(properties("s3.example.com", domain, "images"));
            try (DefaultOssClientImpl client = new DefaultOssClientImpl("test", config)) {
                client.initialize();
                String url = client.presignDownloadUrl("reports/1.pdf", Duration.ofMinutes(2), "报告.pdf");
                assertTrue(url.contains("X-Amz-Signature="));
                assertTrue(url.contains("response-content-disposition="));
                assertTrue(url.contains("attachment"));
                if (!domain.isEmpty()) {
                    assertTrue(url.startsWith("https://files.example.com/reports/1.pdf?"));
                }
            }
        }
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
