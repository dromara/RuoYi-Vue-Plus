package org.dromara.common.oss.client;

import org.dromara.common.oss.config.OssAsyncExecutorConfig;
import org.dromara.common.oss.config.OssClientConfig;
import org.dromara.common.oss.exception.S3StorageException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * 默认S3存储客户端实现类。
 *
 * @author 秋辞未寒
 */
public class DefaultOssClientImpl extends AbstractOssClientImpl {

    public DefaultOssClientImpl(String clientId, OssClientConfig config) {
        super(clientId, config);
    }

    @Override
    void doInitialize() {
        // 校验配置
        String accessKey = config.accessKey()
            .filter(bucket -> !bucket.isBlank())
            .orElseThrow(() -> S3StorageException.form("accessKey is not configured."));
        String secretKey = config.secretKey()
            .filter(bucket -> !bucket.isBlank())
            .orElseThrow(() -> S3StorageException.form("secretKey is not configured."));
        String endpointUrl = config.getEndpointUrl();
        String domainUrl = config.getDomainUrl();
        Region region = config.region().orElse(Region.US_EAST_1);
        // MinIO 使用 HTTPS 限制使用域名访问，站点填域名。需要启用路径样式访问
        boolean usePathStyleAccess = config.usePathStyleAccess();

        // 创建 AWS 认证信息
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        S3Configuration s3Configuration = S3Configuration.builder()
            .chunkedEncodingEnabled(false)
            .pathStyleAccessEnabled(usePathStyleAccess)
            .build();

        // 创建AWS基于 Netty 的 S3 客户端
        this.s3AsyncClient = S3AsyncClient.builder()
            .credentialsProvider(credentialsProvider)
            .endpointOverride(URI.create(endpointUrl))
            .region(region)
            .forcePathStyle(usePathStyleAccess)
            .serviceConfiguration(s3Configuration)
            .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
            .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
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
        this.s3TransferManager = S3TransferManager.builder().s3Client(this.s3AsyncClient).build();

        // 创建 预签名 URL 的生成器 实例，用于生成 S3 预签名 URL
        this.s3Presigner = S3Presigner.builder()
            .region(region)
            .credentialsProvider(credentialsProvider)
            .endpointOverride(URI.create(domainUrl))
            .serviceConfiguration(s3Configuration)
            .build();

        // 创建异步调度器对象
        OssAsyncExecutorConfig asyncExecutorConfig = config.asyncExecutorConfig();
        // 是否使用虚拟线程
        if (asyncExecutorConfig.enabledVirtualThread()) {
            this.asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
        } else {
            this.asyncExecutor = Executors.newScheduledThreadPool(asyncExecutorConfig.corePoolSize());
        }
    }
}
