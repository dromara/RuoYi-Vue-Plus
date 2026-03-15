package org.dromara.common.oss.s3.builder;

import org.dromara.common.oss.s3.client.S3StorageClient;
import org.dromara.common.oss.s3.client.S3StorageClientImpl;
import org.dromara.common.oss.s3.config.S3StorageClientConfig;
import org.dromara.common.oss.s3.exception.S3StorageException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import java.net.URI;
import java.time.Duration;

/**
 * 默认S3存储客户端构建器
 *
 * @author 秋辞未寒
 */
public enum DefaultS3StorageClientBuilder implements S3StorageClientBuilder<S3StorageClientConfig> {
    INSTANCE;

    @Override
    public S3StorageClient build(S3StorageClientConfig config) {
        String accessKey = config.accessKey()
                .filter(bucket -> !bucket.isBlank())
                .orElseThrow(() -> S3StorageException.of("accessKey is not configured."));
        String secretKey = config.secretKey()
                .filter(bucket -> !bucket.isBlank())
                .orElseThrow(() -> S3StorageException.of("secretKey is not configured."));
        Region region = config.region()
                .orElse(Region.US_EAST_1);
        String endpointUrl = config.getEndpointUrl();
        String domainUrl = config.getDomainUrl();
        // MinIO 使用 HTTPS 限制使用域名访问，站点填域名。需要启用路径样式访问
        boolean usePathStyleAccess = config.usePathStyleAccess();
        try {
            // 创建 AWS 认证信息
            StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));

            // 创建AWS基于 Netty 的 S3 客户端
            S3AsyncClient client = S3AsyncClient.builder()
                    .credentialsProvider(credentialsProvider)
                    .endpointOverride(URI.create(endpointUrl))
                    .region(region)
                    .forcePathStyle(usePathStyleAccess)
                    .httpClient(
                            NettyNioAsyncHttpClient.builder()
                                    .connectionTimeout(Duration.ofSeconds(60))
                                    .connectionAcquisitionTimeout(Duration.ofSeconds(30))
                                    .maxConcurrency(100)
                                    .maxPendingConnectionAcquires(1000)
                                    .build()
                    )
                    .build();

            //AWS基于 CRT 的 S3 AsyncClient 实例用作 S3 传输管理器的底层客户端
            S3TransferManager transferManager = S3TransferManager.builder().s3Client(client).build();

            // 创建 预签名 URL 的生成器 实例，用于生成 S3 预签名 URL
            S3Presigner presigner = S3Presigner.builder()
                    .region(region)
                    .credentialsProvider(credentialsProvider)
                    .endpointOverride(URI.create(domainUrl))
                    .serviceConfiguration(
                            // 创建 S3 配置对象
                            S3Configuration.builder()
                                    .chunkedEncodingEnabled(false)
                                    .pathStyleAccessEnabled(usePathStyleAccess)
                                    .build()
                    )
                    .build();

            return new S3StorageClientImpl(config,client,transferManager,presigner);
        } catch (Exception e) {
            if (e instanceof S3StorageException) {
                throw e;
            }
            throw S3StorageException.of(e);
        }
    }
}
