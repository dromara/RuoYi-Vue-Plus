package org.dromara.common.oss.client;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.oss.config.OssClientConfig;
import org.dromara.common.oss.exception.S3StorageException;
import org.dromara.common.oss.io.OutputStreamDownloadSubscriber;
import org.dromara.common.oss.model.GetObjectResult;
import org.dromara.common.oss.model.HandleAsyncResult;
import org.dromara.common.oss.model.PutObjectResult;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.async.ResponsePublisher;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.DownloadRequest;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 抽象S3存储客户端实现类。
 *
 * @author 秋辞未寒
 */
@Slf4j
public abstract class AbstractOssClientImpl implements OssClient {

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * S3 存储客户端ID
     * <p>
     * 用于标识客户端，初始化后不允许更改
     */
    protected final String clientId;

    /**
     * S3 存储客户端配置。
     */
    protected OssClientConfig config;

    /**
     * Amazon S3 异步客户端。
     */
    protected S3AsyncClient s3AsyncClient;

    /**
     * 用于管理 S3 数据传输的高级工具。
     */
    protected S3TransferManager s3TransferManager;

    /**
     * AWS S3 预签名 URL 生成器。
     */
    protected S3Presigner s3Presigner;

    /**
     * 异步调度线程池。
     */
    protected ExecutorService asyncExecutor;

    public AbstractOssClientImpl(String clientId, OssClientConfig config) {
        Assert.notNull(config, () -> S3StorageException.form("S3StorageClientConfig must not be null"));
        // 如果没有设置存储客户端ID，则随机生成一个
        this.clientId = StringUtils.isBlank(clientId) ? IdUtil.fastSimpleUUID() : clientId;
        this.config = config;
        this.initialize();
    }

    @Override
    public String clientId() {
        return this.clientId;
    }

    @Override
    public OssClientConfig config() {
        // 仅返回copy副本，防篡改
        return this.config.copy();
    }

    @Override
    public boolean isInitialized() {
        return initialized.get();
    }

    @Override
    public void initialize() {
        // 如果已经是初始化状态，则直接返回
        if (isInitialized()) {
            return;
        }
        try {
            doInitialize();
            // 将状态转为已初始化
            initialized.compareAndSet(false, true);
        } catch (Exception e) {
            if (e instanceof S3StorageException) {
                throw e;
            }
            throw S3StorageException.form(e);
        }
    }

    abstract void doInitialize();

    @Override
    public void refresh(OssClientConfig config) {
        if (Objects.equals(this.config, config)) {
            return;
        }
        // 如果状态本来就是未初始化，直接则调用初始化
        if (!initialized.get()) {
            this.initialize();
        }
        // 将状态转为未初始化
        if (initialized.compareAndSet(false, true)) {
            try {
                this.close();
            } catch (Exception e) {
                // 异常不影响刷新逻辑，此处屏蔽异常
            }
            // 状态交换成功才进行刷新
            this.initialize();
        }
    }

    @Override
    public boolean verifyConfig(Function<OssClientConfig, Boolean> verifyConfigAction) {
        OssClientConfig config = config();
        return Boolean.TRUE.equals(verifyConfigAction.apply(config));
    }

    @Override
    public boolean verifyConfig(OssClientConfig verifyConfig) {
        return verifyConfig((config) -> Objects.equals(config, verifyConfig));
    }

    @Override
    public <T> T doCustomUpload(AsyncRequestBody body, Consumer<PutObjectRequest.Builder> putObjectRequestBuilderConsumer, Collection<TransferListener> transferListeners, BiFunction<CompletedUpload, Throwable, T> handleAsyncAction) {
        try {
            return s3TransferManager.upload(uploadRequestBuilder -> {
                    uploadRequestBuilder.requestBody(body)
                        .putObjectRequest(putObjectRequestBuilderConsumer)
                        .transferListeners(transferListeners);
                })
                .completionFuture()
                .handleAsync(handleAsyncAction)
                .join();
        } catch (Exception e) {
            if (e instanceof S3StorageException ex) {
                throw ex;
            }
            throw S3StorageException.form(e);
        }
    }

    @Override
    public <T> T doCustomUpload(AsyncRequestBody body, Consumer<PutObjectRequest.Builder> putObjectRequestBuilderConsumer, BiFunction<CompletedUpload, Throwable, T> handleAsyncAction) {
        return doCustomUpload(body, putObjectRequestBuilderConsumer, null, handleAsyncAction);
    }

    @Override
    public HandleAsyncResult<PutObjectResponse> doCustomUpload(AsyncRequestBody body, Consumer<PutObjectRequest.Builder> putObjectRequestBuilderConsumer, Collection<TransferListener> transferListeners) {
        return doCustomUpload(body, putObjectRequestBuilderConsumer, transferListeners, (completedUpload, throwable) -> {
            if (completedUpload == null) {
                return HandleAsyncResult.of(null, throwable);
            }
            return HandleAsyncResult.of(completedUpload.response(), throwable);
        });
    }

    @Override
    public HandleAsyncResult<PutObjectResponse> doCustomUpload(AsyncRequestBody body, Consumer<PutObjectRequest.Builder> putObjectRequestBuilderConsumer) {
        return doCustomUpload(body, putObjectRequestBuilderConsumer, null, (completedUpload, throwable) -> {
            if (completedUpload == null) {
                return HandleAsyncResult.of(null, throwable);
            }
            return HandleAsyncResult.of(completedUpload.response(), throwable);
        });
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, Path path) {
        AsyncRequestBody body = AsyncRequestBody.fromFile(path);
        return bucketUpload(bucket, key, body);
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, File file) {
        AsyncRequestBody body = AsyncRequestBody.fromFile(file);
        return bucketUpload(bucket, key, body);
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, RandomAccessFile file) {
        try {
            return bucketUpload(bucket, key, file.getChannel(), -1L);
        } catch (Exception e) {
            if (e instanceof S3StorageException ex) {
                throw ex;
            }
            throw S3StorageException.form(e);
        }
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, ReadableByteChannel channel, long contentLength) {
        long size = contentLength;
        try (channel; InputStream in = Channels.newInputStream(channel)) {
            if (channel instanceof FileChannel fileChannel) {
                size = fileChannel.size();
            }
            return bucketUpload(bucket, key, in, size);
        } catch (Exception e) {
            if (e instanceof S3StorageException ex) {
                throw ex;
            }
            throw S3StorageException.form(e);
        }
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, InputStream in, long contentLength) {
        AsyncRequestBody body = AsyncRequestBody.fromInputStream(in, contentLength, asyncExecutor);
        return bucketUpload(bucket, key, body);
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, byte[] data) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            return bucketUpload(bucket, key, in, data.length);
        } catch (Exception e) {
            if (e instanceof S3StorageException ex) {
                throw ex;
            }
            throw S3StorageException.form(e);
        }
    }


    private PutObjectResult bucketUpload(String bucket, String key, AsyncRequestBody body) {
        Long contentLength = body.contentLength().orElse(null);
        HandleAsyncResult<PutObjectResponse> result = doCustomUpload(body, builder -> {
            builder.bucket(bucket)
                .key(key)
                .contentLength(contentLength)
            ;
        });
        if (result.isFailure()) {
            throw S3StorageException.form(result.error());
        }
        Optional<PutObjectResponse> opt = result.getResult();
        if (opt.isEmpty()) {
            throw S3StorageException.form("response is empty.");
        }
        PutObjectResponse response = opt.get();
        String bucketUrl = config.getBucketUrl(bucket);
        // 不知道什么原因导致 response.size() 返回了一个 null size ，此处做一个适配...
        Long size = response.size();
        size = size == null ? contentLength : size;
        return PutObjectResult.form("%s/%s".formatted(bucketUrl, key), key, response.eTag(), size == null ? 0 : size);
    }

    @Override
    public <T> T doCustomDownload(Consumer<GetObjectRequest.Builder> getObjectRequestBuilderConsumer, AsyncResponseTransformer<GetObjectResponse, T> responseTransformer, Collection<TransferListener> transferListeners) {
        try {
            DownloadRequest<T> downloadRequest = DownloadRequest.builder()
                .responseTransformer(responseTransformer)
                .getObjectRequest(getObjectRequestBuilderConsumer)
                .transferListeners(transferListeners)
                .build();
            return s3TransferManager.download(downloadRequest)
                .completionFuture()
                .join()
                .result();
        } catch (Exception e) {
            if (e instanceof S3StorageException ex) {
                throw ex;
            }
            throw S3StorageException.form(e);
        }
    }

    @Override
    public GetObjectResult bucketDownload(String bucket, String key, OutputStreamDownloadSubscriber downloadSubscriber) {
        try {
            ResponsePublisher<GetObjectResponse> publisher = doCustomDownload(builder -> builder.bucket(bucket).key(key), AsyncResponseTransformer.toPublisher(), null);
            GetObjectResult getObjectResult = buildGetObjectResult(key, publisher.response());
            publisher.subscribe(downloadSubscriber).join();
            return getObjectResult;
        } catch (Exception e) {
            if (e instanceof S3StorageException ex) {
                throw ex;
            }
            throw S3StorageException.form(e);
        }
    }

    @Override
    public <T> T bucketDownload(String bucket, String key, BiFunction<GetObjectResult, InputStream, T> downloadTransformer) {
        try {
            ResponseInputStream<GetObjectResponse> responseInputStream = doCustomDownload(builder -> builder.bucket(bucket).key(key), AsyncResponseTransformer.toBlockingInputStream(), null);
            GetObjectResponse response = responseInputStream.response();
            GetObjectResult getObjectResult = buildGetObjectResult(key, response);
            return downloadTransformer.apply(getObjectResult, responseInputStream);
        } catch (Exception e) {
            if (e instanceof S3StorageException ex) {
                throw ex;
            }
            throw S3StorageException.form(e);
        }
    }

    @Override
    public GetObjectResult bucketDownload(String bucket, String key, Path path) {
        try (OutputStream out = Files.newOutputStream(path)) {
            return bucketDownload(bucket, key, out);
        } catch (Exception e) {
            if (e instanceof S3StorageException ex) {
                throw ex;
            }
            throw S3StorageException.form(e);
        }
    }

    @Override
    public GetObjectResult bucketDownload(String bucket, String key, File file) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            return bucketDownload(bucket, key, out);
        } catch (Exception e) {
            if (e instanceof S3StorageException ex) {
                throw ex;
            }
            throw S3StorageException.form(e);
        }
    }

    @Override
    public GetObjectResult bucketDownload(String bucket, String key, RandomAccessFile file) {
        return bucketDownload(bucket, key, file.getChannel());
    }

    @Override
    public GetObjectResult bucketDownload(String bucket, String key, WritableByteChannel channel) {
        return bucketDownload(bucket, key, OutputStreamDownloadSubscriber.create(channel));
    }

    @Override
    public GetObjectResult bucketDownload(String bucket, String key, OutputStream out) {
        return bucketDownload(bucket, key, OutputStreamDownloadSubscriber.create(out));
    }

    private GetObjectResult buildGetObjectResult(String key, GetObjectResponse response) {
        return GetObjectResult.form(
            key,
            response.eTag(),
            response.lastModified().atOffset(ZoneOffset.UTC).toLocalDateTime(),
            response.contentLength(),
            response.contentType(),
            response.contentDisposition(),
            response.contentRange(),
            response.contentEncoding(),
            response.contentLanguage(),
            response.metadata()
        );
    }

    @Override
    public boolean bucketDelete(String bucket, String key) {
        try {
            DeleteObjectResponse response = s3AsyncClient.deleteObject(builder -> builder.bucket(bucket).key(key)).join();
            return Boolean.TRUE.equals(response.deleteMarker());
        } catch (Exception e) {
            throw S3StorageException.form(e);
        }
    }

    @Override
    public String bucketPresignGetUrl(String bucket, String key, Duration expiredTime) {
        try {
            return s3Presigner.presignGetObject(getObjectPresignRequestBuilder -> {
                    getObjectPresignRequestBuilder.signatureDuration(expiredTime)
                        .getObjectRequest(getObjectRequestBuilder -> getObjectRequestBuilder.bucket(bucket).key(key));
                })
                .url()
                .toExternalForm();
        } catch (Exception e) {
            throw S3StorageException.form(e);
        }
    }

    @Override
    public String bucketPresignPutUrl(String bucket, String key, Duration expiredTime, Map<String, String> metadata) {
        try {
            return s3Presigner.presignPutObject(putObjectPresignRequestBuilder -> {
                    putObjectPresignRequestBuilder.signatureDuration(expiredTime)
                        .putObjectRequest(putObjectRequestBuilder -> putObjectRequestBuilder.bucket(bucket).key(key).metadata(metadata));
                })
                .url()
                .toExternalForm();
        } catch (Exception e) {
            throw S3StorageException.form(e);
        }
    }

    @Override
    public PutObjectResult upload(String key, Path path) {
        return bucketUpload(defaultBucket(), key, path);
    }

    @Override
    public PutObjectResult upload(String key, File file) {
        return bucketUpload(defaultBucket(), key, file);
    }

    @Override
    public PutObjectResult upload(String key, RandomAccessFile file) {
        return bucketUpload(defaultBucket(), key, file);
    }

    @Override
    public PutObjectResult upload(String key, ReadableByteChannel channel, long contentLength) {
        return bucketUpload(defaultBucket(), key, channel, contentLength);
    }

    @Override
    public PutObjectResult upload(String key, InputStream in, long contentLength) {
        return bucketUpload(defaultBucket(), key, in, contentLength);
    }

    @Override
    public PutObjectResult upload(String key, byte[] data) {
        return bucketUpload(defaultBucket(), key, data);
    }

    @Override
    public GetObjectResult download(String key, OutputStreamDownloadSubscriber downloadSubscriber) {
        return bucketDownload(defaultBucket(), key, downloadSubscriber);
    }

    @Override
    public <T> T download(String key, BiFunction<GetObjectResult, InputStream, T> downloadTransformer) {
        return bucketDownload(defaultBucket(), key, downloadTransformer);
    }

    @Override
    public GetObjectResult download(String key, Path path) {
        return bucketDownload(defaultBucket(), key, path);
    }

    @Override
    public GetObjectResult download(String key, File file) {
        return bucketDownload(defaultBucket(), key, file);
    }

    @Override
    public GetObjectResult download(String key, RandomAccessFile file) {
        return bucketDownload(defaultBucket(), key, file);
    }

    @Override
    public GetObjectResult download(String key, WritableByteChannel channel) {
        return bucketDownload(defaultBucket(), key, channel);
    }

    @Override
    public GetObjectResult download(String key, OutputStream out) {
        return bucketDownload(defaultBucket(), key, out);
    }

    @Override
    public boolean delete(String key) {
        return bucketDelete(defaultBucket(), key);
    }

    @Override
    public String presignGetUrl(String key, Duration expiredTime) {
        return bucketPresignGetUrl(defaultBucket(), key, expiredTime);
    }

    @Override
    public String presignPutUrl(String key, Duration expiredTime, Map<String, String> metadata) {
        return bucketPresignPutUrl(defaultBucket(), key, expiredTime, metadata);
    }

    private String defaultBucket() {
        return config.bucket()
            .filter(bucket -> !bucket.isBlank())
            .orElseThrow(() -> S3StorageException.form("bucket is not configured."));
    }

    @Override
    public void close() throws Exception {
        if (s3TransferManager != null) {
            s3TransferManager.close();
        }
        if (s3AsyncClient != null) {
            s3AsyncClient.close();
        }
        if (s3Presigner != null) {
            s3Presigner.close();
        }
        if (asyncExecutor != null) {
            asyncExecutor.close();
        }
        // 重置初始化状态为 false
        initialized.compareAndSet(true, false);
    }
}
