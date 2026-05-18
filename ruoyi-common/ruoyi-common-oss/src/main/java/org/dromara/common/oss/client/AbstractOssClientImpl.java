package org.dromara.common.oss.client;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import org.dromara.common.core.utils.DateUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.oss.config.OssClientConfig;
import org.dromara.common.oss.exception.S3StorageException;
import org.dromara.common.oss.io.OutputStreamDownloadSubscriber;
import org.dromara.common.oss.model.GetObjectResult;
import org.dromara.common.oss.model.HandleAsyncResult;
import org.dromara.common.oss.model.Options;
import org.dromara.common.oss.model.PutObjectResult;
import org.jspecify.annotations.NullMarked;
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
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 抽象S3存储客户端实现类。
 *
 * @author 秋辞未寒
 */
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

    /**
     * 构造 S3 存储客户端基础实现。
     *
     * @param clientId 客户端 ID
     * @param config S3 存储客户端配置
     */
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
            throw toStorageException(e);
        }
    }

    abstract void doInitialize();

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
    public String buildPathKey(String fileName) {
        return buildPathKey(null, fileName);
    }

    @Override
    public String buildPathKey(String businessPrefix, String fileName) {
        String defaultPrefix = config.prefix()
            .orElse("");
        String mergedPrefix = mergePrefix(defaultPrefix, businessPrefix);
        String suffix = suffix(fileName);
        String datePath = DateUtils.datePath();
        String uuid = IdUtil.fastSimpleUUID();
        String path = mergedPrefix.isEmpty() ? datePath + StringUtils.SLASH + uuid : mergedPrefix + StringUtils.SLASH + datePath + StringUtils.SLASH + uuid;
        return path + suffix;
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
            throw toStorageException(e);
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
    public PutObjectResult bucketUpload(String bucket, String key, Path path, Options options) {
        AsyncRequestBody body = AsyncRequestBody.fromFile(path);
        return bucketUpload(bucket, key, body, options);
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, Path path) {
        return bucketUpload(bucket, key, path, Options.builder());
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, File file, Options options) {
        AsyncRequestBody body = AsyncRequestBody.fromFile(file);
        return bucketUpload(bucket, key, body, options);
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, File file) {
        return bucketUpload(bucket, key, file, Options.builder());
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, RandomAccessFile file, Options options) {
        try {
            // 以文件的大小为准
            options.setLength(file.length());
            return bucketUpload(bucket, key, file.getChannel(), -1L, options);
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, RandomAccessFile file) {
        return bucketUpload(bucket, key, file, Options.builder());
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, ReadableByteChannel channel, long contentLength, Options options) {
        // 让调用者自行处理通道的关闭
        InputStream in = Channels.newInputStream(channel);
        try {
            // 如果可以实时获取文件大小，则优先是有实时获取的
            long size = contentLength;
            if (channel instanceof SeekableByteChannel byteChannel) {
                size = byteChannel.size();
            }
            return bucketUpload(bucket, key, in, size, options);
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, ReadableByteChannel channel, long contentLength) {
        return bucketUpload(bucket, key, channel, contentLength, Options.builder());
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, InputStream in, long contentLength, Options options) {
        options.setLength(contentLength);
        AsyncRequestBody body = AsyncRequestBody.fromInputStream(in, contentLength, asyncExecutor);
        return bucketUpload(bucket, key, body, options);
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, InputStream in, long contentLength) {
        return bucketUpload(bucket, key, in, contentLength, Options.builder());
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, byte[] data, Options options) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            return bucketUpload(bucket, key, in, data.length, options);
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, byte[] data) {
        return bucketUpload(bucket, key, data, Options.builder());
    }

    @NullMarked
    private PutObjectResult bucketUpload(String bucket, String key, AsyncRequestBody body, Options options) {
        // 优先使用body中的内容大小，如果不存在，再获取可选项中的
        Long contentLength = body.contentLength().orElse(options.getLength());
        // 优先使用body中的内容类型，如果不存在，再获取可选项中的
        String contentType = StringUtils.isBlank(options.getContentType()) ? body.contentType() : options.getContentType();
        String md5Digest = options.getMd5Digest();
        Map<String, String> metadata = options.getMetadata();
        Collection<TransferListener> transferListeners = options.getTransferListeners();
        HandleAsyncResult<PutObjectResponse> result = doCustomUpload(body, builder -> {
            builder.bucket(bucket)
                .key(key)
                .contentMD5(md5Digest)
                .contentType(contentType)
                .contentLength(contentLength)
                .metadata(metadata);
        }, transferListeners);
        if (result.isFailure()) {
            throw S3StorageException.form(result.error());
        }
        Optional<PutObjectResponse> opt = result.getResult();
        if (opt.isEmpty()) {
            throw S3StorageException.form("response is empty.");
        }
        PutObjectResponse response = opt.get();
        // 不知道什么原因导致 response.size() 返回了一个 null size ，此处做一个适配...
        Long size = response.size();
        if (size == null) {
            size = contentLength == null ? 0 : contentLength;
        }
        String bucketUrl = config.getBucketUrl(bucket);
        return PutObjectResult.form("%s/%s".formatted(bucketUrl, key), key, response.eTag(), size);
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
            throw toStorageException(e);
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
            throw toStorageException(e);
        }
    }

    @Override
    public <T> T bucketDownload(String bucket, String key, BiFunction<GetObjectResult, InputStream, T> downloadTransformer) {
        try (ResponseInputStream<GetObjectResponse> responseInputStream = doCustomDownload(builder -> builder.bucket(bucket).key(key), AsyncResponseTransformer.toBlockingInputStream(), null)) {
            GetObjectResponse response = responseInputStream.response();
            GetObjectResult getObjectResult = buildGetObjectResult(key, response);
            return downloadTransformer.apply(getObjectResult, responseInputStream);
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    @Override
    public GetObjectResult bucketDownload(String bucket, String key, Path path) {
        try (OutputStream out = Files.newOutputStream(path)) {
            return bucketDownload(bucket, key, out);
        } catch (Exception e) {
            throw toStorageException(e);
        }
    }

    @Override
    public GetObjectResult bucketDownload(String bucket, String key, File file) {
        try (FileOutputStream out = new FileOutputStream(file)) {
            return bucketDownload(bucket, key, out);
        } catch (Exception e) {
            throw toStorageException(e);
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
            s3AsyncClient.deleteObject(builder -> builder.bucket(bucket).key(key)).join();
            return true;
        } catch (Exception e) {
            throw toStorageException(e);
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
            throw toStorageException(e);
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
            throw toStorageException(e);
        }
    }

    @Override
    public PutObjectResult upload(String key, Path path, Options options) {
        return bucketUpload(defaultBucket(), key, path, options);
    }

    @Override
    public PutObjectResult upload(String key, Path path) {
        return bucketUpload(defaultBucket(), key, path);
    }

    @Override
    public PutObjectResult upload(String key, File file, Options options) {
        return bucketUpload(defaultBucket(), key, file, options);
    }

    @Override
    public PutObjectResult upload(String key, File file) {
        return bucketUpload(defaultBucket(), key, file);
    }

    @Override
    public PutObjectResult upload(String key, RandomAccessFile file, Options options) {
        return bucketUpload(defaultBucket(), key, file, options);
    }

    @Override
    public PutObjectResult upload(String key, RandomAccessFile file) {
        return bucketUpload(defaultBucket(), key, file);
    }

    @Override
    public PutObjectResult upload(String key, ReadableByteChannel channel, long contentLength, Options options) {
        return bucketUpload(defaultBucket(), key, channel, contentLength, options);
    }

    @Override
    public PutObjectResult upload(String key, ReadableByteChannel channel, long contentLength) {
        return bucketUpload(defaultBucket(), key, channel, contentLength);
    }

    @Override
    public PutObjectResult upload(String key, InputStream in, long contentLength, Options options) {
        return bucketUpload(defaultBucket(), key, in, contentLength, options);
    }

    @Override
    public PutObjectResult upload(String key, InputStream in, long contentLength) {
        return bucketUpload(defaultBucket(), key, in, contentLength);
    }

    @Override
    public PutObjectResult upload(String key, byte[] data, Options options) {
        return bucketUpload(defaultBucket(), key, data, options);
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

    private String mergePrefix(String defaultPrefix, String businessPrefix) {
        String left = normalizePrefix(defaultPrefix);
        String right = normalizePrefix(businessPrefix);
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty()) {
            return left;
        }
        return left + StringUtils.SLASH + right;
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null) {
            return "";
        }
        String normalized = prefix.trim();
        while (normalized.startsWith(StringUtils.SLASH)) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith(StringUtils.SLASH)) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String suffix(String fileName) {
        if (fileName == null) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return fileName.substring(index);
    }

    private S3StorageException toStorageException(Throwable e) {
        Throwable cause = unwrapAsyncException(e);
        if (cause instanceof S3StorageException ex) {
            return ex;
        }
        return S3StorageException.form(cause);
    }

    private Throwable unwrapAsyncException(Throwable e) {
        Throwable cause = e;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
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
