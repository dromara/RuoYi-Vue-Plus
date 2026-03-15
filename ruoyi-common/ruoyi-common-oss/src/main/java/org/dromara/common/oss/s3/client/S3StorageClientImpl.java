package org.dromara.common.oss.s3.client;

import org.dromara.common.oss.s3.config.S3StorageClientConfig;
import org.dromara.common.oss.s3.domain.GetObjectResult;
import org.dromara.common.oss.s3.domain.HandleAsyncResult;
import org.dromara.common.oss.s3.domain.PutObjectResult;
import org.dromara.common.oss.s3.exception.S3StorageException;
import org.dromara.common.oss.s3.io.OutputStreamDownloadSubscriber;
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
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * S3 存储客户端实现类。
 *
 * @author 秋辞未寒
 */
public class S3StorageClientImpl implements S3StorageClient {

    /**
     * S3 存储客户端配置。
     */
    private final S3StorageClientConfig config;

    /**
     * Amazon S3 异步客户端。
     */
    private final S3AsyncClient s3AsyncClient;

    /**
     * 用于管理 S3 数据传输的高级工具。
     */
    private final S3TransferManager s3TransferManager;

    /**
     * AWS S3 预签名 URL 生成器。
     */
    private final S3Presigner s3Presigner;

    /**
     * 异步调度线程池。
     */
    private final ExecutorService executorService;

    public S3StorageClientImpl(S3StorageClientConfig config, S3AsyncClient s3AsyncClient, S3TransferManager s3TransferManager, S3Presigner s3Presigner) {
        this(config,s3AsyncClient,s3TransferManager,s3Presigner, Executors.newSingleThreadExecutor());
    }

    public S3StorageClientImpl(S3StorageClientConfig config, S3AsyncClient s3AsyncClient, S3TransferManager s3TransferManager, S3Presigner s3Presigner, ExecutorService executorService) {
        this.config = config;
        this.s3AsyncClient = s3AsyncClient;
        this.s3TransferManager = s3TransferManager;
        this.s3Presigner = s3Presigner;
        this.executorService = executorService;
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
            throw S3StorageException.of(e);
        }
    }

    @Override
    public <T> T doCustomUpload(AsyncRequestBody body, Consumer<PutObjectRequest.Builder> putObjectRequestBuilderConsumer, BiFunction<CompletedUpload, Throwable, T> handleAsyncAction) {
        return doCustomUpload(body, putObjectRequestBuilderConsumer, null, handleAsyncAction);
    }

    @Override
    public HandleAsyncResult<PutObjectResponse> doCustomUpload(AsyncRequestBody body, Consumer<PutObjectRequest.Builder> putObjectRequestBuilderConsumer, Collection<TransferListener> transferListeners) {
        return doCustomUpload(body, putObjectRequestBuilderConsumer, transferListeners, (completedUpload, throwable) -> HandleAsyncResult.of(completedUpload.response(), throwable));
    }

    @Override
    public HandleAsyncResult<PutObjectResponse> doCustomUpload(AsyncRequestBody body, Consumer<PutObjectRequest.Builder> putObjectRequestBuilderConsumer) {
        return doCustomUpload(body, putObjectRequestBuilderConsumer, null, (completedUpload, throwable) -> HandleAsyncResult.of(completedUpload.response(), throwable));
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
            throw S3StorageException.of(e);
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
            throw S3StorageException.of(e);
        }
    }

    @Override
    public PutObjectResult bucketUpload(String bucket, String key, InputStream in, long contentLength) {
        AsyncRequestBody body = AsyncRequestBody.fromInputStream(builder -> builder.inputStream(in)
                .contentLength(contentLength)
                .executor(executorService));
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
            throw S3StorageException.of(e);
        }
    }


    private PutObjectResult bucketUpload(String bucket, String key, AsyncRequestBody body) {
        HandleAsyncResult<PutObjectResponse> result = doCustomUpload(body, builder -> builder.bucket(bucket).key(key));
        if (result.isFailure()) {
            throw S3StorageException.of(result.error());
        }
        Optional<PutObjectResponse> opt = result.getResult();
        if (opt.isEmpty()) {
            throw S3StorageException.of("response is empty.");
        }
        PutObjectResponse response = opt.get();
        return PutObjectResult.of(null, key, response.eTag(), response.size());
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
            throw S3StorageException.of(e);
        }
    }

    @Override
    public GetObjectResult bucketDownload(String bucket, String key, OutputStreamDownloadSubscriber downloadSubscriber) {
        try {
            ResponsePublisher<GetObjectResponse> publisher = doCustomDownload(builder -> builder.bucket(bucket).key(key), AsyncResponseTransformer.toPublisher(), null);
            GetObjectResult getObjectResult = buildGetObjectResult(key, publisher.response());
            publisher.subscribe(downloadSubscriber);
            return getObjectResult;
        } catch (Exception e) {
            if (e instanceof S3StorageException ex) {
                throw ex;
            }
            throw S3StorageException.of(e);
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
            throw S3StorageException.of(e);
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
            throw S3StorageException.of(e);
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
        return GetObjectResult.of(
                key,
                response.eTag(),
                LocalDateTime.from(response.lastModified()),
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
            throw S3StorageException.of(e);
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
            throw S3StorageException.of(e);
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
            throw S3StorageException.of(e);
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
                .orElseThrow(() -> S3StorageException.of("bucket is not configured."));
    }

    @Override
    public boolean verifyConfig(Function<S3StorageClientConfig,Boolean> verifyConfigAction) {
        S3StorageClientConfig copy = S3StorageClientConfig.copy(config);
        return Boolean.TRUE.equals(verifyConfigAction.apply(copy));
    }

    @Override
    public boolean verifyConfig(S3StorageClientConfig verifyConfig) {
        return verifyConfig(config -> Objects.equals(config,verifyConfig));
    }

    @Override
    public void close() throws Exception {
        s3TransferManager.close();
        s3AsyncClient.close();
        s3Presigner.close();
        executorService.close();
    }
}
