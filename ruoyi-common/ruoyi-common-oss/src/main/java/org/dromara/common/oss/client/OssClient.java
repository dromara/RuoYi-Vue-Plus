package org.dromara.common.oss.client;

import cn.hutool.core.util.IdUtil;
import org.dromara.common.oss.config.OssClientConfig;
import org.dromara.common.oss.io.OutputStreamDownloadSubscriber;
import org.dromara.common.oss.model.GetObjectResult;
import org.dromara.common.oss.model.HandleAsyncResult;
import org.dromara.common.oss.model.Options;
import org.dromara.common.oss.model.PutObjectResult;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * S3 存储客户端接口。
 * <p>
 * 本接口同时提供两套对象操作 API：
 * 一套通过 {@code bucketXxx(...)} 显式指定存储桶，
 * 另一套通过无前缀方法使用默认存储桶。
 * </p>
 *
 * @author 秋辞未寒
 */
public interface OssClient extends AutoCloseable {

    /**
     * S3 存储客户端ID
     * <p>
     * 用于标识客户端，初始化后不允许更改
     *
     * @return S3 存储客户端ID
     */
    default String clientId() {
        return IdUtil.fastSimpleUUID();
    }

    /**
     * 获取客户端配置copy副本
     */
    OssClientConfig config();

    /**
     * 是否已经初始化
     */
    boolean isInitialized();

    /**
     * 初始化客户端
     */
    void initialize();

    /**
     * 校验客户端配置
     *
     * <p>注意：该方法不会修改任何既有的配置和状态，你看可以理解为这仅仅是一个配置展示的方法，以供调用者根据当前的配置，自行决定是否需要重新构建客户端。</p>
     *
     * @param verifyConfigAction 校验配置动作函数
     * @return 是否一致
     */
    boolean verifyConfig(Function<OssClientConfig, Boolean> verifyConfigAction);

    /**
     * 校验客户端配置与传入的待校验配置是否一致
     *
     * <p>注意：该方法不会修改任何既有的配置和状态，你看可以理解为这仅仅是一个配置展示的方法，以供调用者根据当前的配置，自行决定是否需要重新构建客户端。</p>
     *
     * @param verifyConfig 待校验的配置
     * @return 是否一致
     */
    boolean verifyConfig(OssClientConfig verifyConfig);

    /**
     * 执行自定义上传请求。
     *
     * @param body                            上传请求体
     * @param putObjectRequestBuilderConsumer PutObject 请求构建回调
     * @param transferListeners               传输监听器集合
     * @param handleAsyncAction               上传完成后的结果处理函数
     * @param <T>                             返回值类型
     * @return 处理后的结果
     */
    <T> T doCustomUpload(AsyncRequestBody body, Consumer<PutObjectRequest.Builder> putObjectRequestBuilderConsumer, Collection<TransferListener> transferListeners, BiFunction<CompletedUpload, Throwable, T> handleAsyncAction);

    /**
     * 执行自定义上传请求。
     *
     * @param body                            上传请求体
     * @param putObjectRequestBuilderConsumer PutObject 请求构建回调
     * @param handleAsyncAction               上传完成后的结果处理函数
     * @param <T>                             返回值类型
     * @return 处理后的结果
     */
    <T> T doCustomUpload(AsyncRequestBody body, Consumer<PutObjectRequest.Builder> putObjectRequestBuilderConsumer, BiFunction<CompletedUpload, Throwable, T> handleAsyncAction);

    /**
     * 执行自定义上传请求，并返回统一异步处理结果。
     *
     * @param body                            上传请求体
     * @param putObjectRequestBuilderConsumer PutObject 请求构建回调
     * @param transferListeners               传输监听器集合
     * @return 上传结果
     */
    HandleAsyncResult<PutObjectResponse> doCustomUpload(AsyncRequestBody body, Consumer<PutObjectRequest.Builder> putObjectRequestBuilderConsumer, Collection<TransferListener> transferListeners);

    /**
     * 执行自定义上传请求，并返回统一异步处理结果。
     *
     * @param body                            上传请求体
     * @param putObjectRequestBuilderConsumer PutObject 请求构建回调
     * @return 上传结果
     */
    HandleAsyncResult<PutObjectResponse> doCustomUpload(AsyncRequestBody body, Consumer<PutObjectRequest.Builder> putObjectRequestBuilderConsumer);

    /**
     * 将本地路径对应的文件上传到指定存储桶。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param path   文件路径
     * @param options 可选项
     * @return 上传结果
     */
    PutObjectResult bucketUpload(String bucket, String key, Path path, Options options);

    /**
     * 将本地路径对应的文件上传到指定存储桶。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param path   文件路径
     * @return 上传结果
     */
    PutObjectResult bucketUpload(String bucket, String key, Path path);

    /**
     * 将文件上传到指定存储桶。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param file   文件对象
     * @param options 可选项
     * @return 上传结果
     */
    PutObjectResult bucketUpload(String bucket, String key, File file, Options options);

    /**
     * 将文件上传到指定存储桶。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param file   文件对象
     * @return 上传结果
     */
    PutObjectResult bucketUpload(String bucket, String key, File file);

    /**
     * 将随机访问文件上传到指定存储桶。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param file   随机访问文件
     * @param options 可选项
     * @return 上传结果
     */
    PutObjectResult bucketUpload(String bucket, String key, RandomAccessFile file, Options options);

    /**
     * 将随机访问文件上传到指定存储桶。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param file   随机访问文件
     * @return 上传结果
     */
    PutObjectResult bucketUpload(String bucket, String key, RandomAccessFile file);

    /**
     * 将可读通道中的数据上传到指定存储桶。
     *
     * @param bucket        存储桶名称
     * @param key           对象键
     * @param channel       数据通道
     * @param contentLength 内容长度
     * @param options 可选项
     * @return 上传结果
     */
    PutObjectResult bucketUpload(String bucket, String key, ReadableByteChannel channel, long contentLength, Options options);

    /**
     * 将可读通道中的数据上传到指定存储桶。
     *
     * @param bucket        存储桶名称
     * @param key           对象键
     * @param channel       数据通道
     * @param contentLength 内容长度
     * @return 上传结果
     */
    PutObjectResult bucketUpload(String bucket, String key, ReadableByteChannel channel, long contentLength);

    /**
     * 将可读通道中的数据上传到指定存储桶。
     *
     * @param bucket        存储桶名称
     * @param key           对象键
     * @param in            输入流
     * @param contentLength 内容长度
     * @param options 可选项
     * @return 上传结果
     */
    PutObjectResult bucketUpload(String bucket, String key, InputStream in, long contentLength, Options options);

    /**
     * 将输入流中的数据上传到指定存储桶。
     *
     * @param bucket        存储桶名称
     * @param key           对象键
     * @param in            输入流
     * @param contentLength 内容长度
     * @return 上传结果
     */
    PutObjectResult bucketUpload(String bucket, String key, InputStream in, long contentLength);

    /**
     * 将字节数组上传到指定存储桶。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param data   字节数组
     * @param options 可选项
     * @return 上传结果
     */
    PutObjectResult bucketUpload(String bucket, String key, byte[] data, Options options);

    /**
     * 将字节数组上传到指定存储桶。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param data   字节数组
     * @return 上传结果
     */
    PutObjectResult bucketUpload(String bucket, String key, byte[] data);

    /**
     * 执行自定义下载请求。
     *
     * @param getObjectRequestBuilderConsumer GetObject 请求构建回调
     * @param responseTransformer             下载响应转换器
     * @param transferListeners               传输监听器集合
     * @param <T>                             下载结果类型
     * @return 下载结果
     */
    <T> T doCustomDownload(Consumer<GetObjectRequest.Builder> getObjectRequestBuilderConsumer, AsyncResponseTransformer<GetObjectResponse, T> responseTransformer, Collection<TransferListener> transferListeners);

    /**
     * 将指定存储桶中的对象下载到订阅器。
     *
     * @param bucket             存储桶名称
     * @param key                对象键
     * @param downloadSubscriber 下载订阅器
     * @return 下载结果
     */
    GetObjectResult bucketDownload(String bucket, String key, OutputStreamDownloadSubscriber downloadSubscriber);

    /**
     * 将指定存储桶中的对象下载到转换器中，由使用者决定返回值。
     *
     * @param bucket              存储桶名称
     * @param key                 对象键
     * @param downloadTransformer 下载转换器
     * @return 下载结果
     */
    <T> T bucketDownload(String bucket, String key, BiFunction<GetObjectResult, InputStream, T> downloadTransformer);

    /**
     * 将指定存储桶中的对象下载到本地路径。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param path   本地路径
     * @return 下载结果
     */
    GetObjectResult bucketDownload(String bucket, String key, Path path);

    /**
     * 将指定存储桶中的对象下载到文件。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param file   本地文件
     * @return 下载结果
     */
    GetObjectResult bucketDownload(String bucket, String key, File file);

    /**
     * 将指定存储桶中的对象下载到随机访问文件。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param file   随机访问文件
     * @return 下载结果
     */
    GetObjectResult bucketDownload(String bucket, String key, RandomAccessFile file);

    /**
     * 将指定存储桶中的对象下载到可写通道。
     *
     * @param bucket  存储桶名称
     * @param key     对象键
     * @param channel 可写通道
     * @return 下载结果
     */
    GetObjectResult bucketDownload(String bucket, String key, WritableByteChannel channel);

    /**
     * 将指定存储桶中的对象下载到输出流。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param out    输出流
     * @return 下载结果
     */
    GetObjectResult bucketDownload(String bucket, String key, OutputStream out);

    /**
     * 删除指定存储桶中的对象。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @return 是否删除成功
     */
    boolean bucketDelete(String bucket, String key);

    /**
     * 生成指定存储桶中文件下载的预签名 URL。
     *
     * @param bucket      存储桶名称
     * @param key         对象键
     * @param expiredTime URL 过期时间
     * @return 预签名下载 URL
     */
    String bucketPresignGetUrl(String bucket, String key, Duration expiredTime);

    /**
     * 生成指定存储桶中文件上传的预签名 URL。
     *
     * @param bucket      存储桶名称
     * @param key         对象键
     * @param expiredTime URL 过期时间
     * @param metadata    对象元数据
     * @return 预签名上传 URL
     */
    String bucketPresignPutUrl(String bucket, String key, Duration expiredTime, Map<String, String> metadata);

    /**
     * 将本地路径对应的文件上传到默认存储桶。
     *
     * @param key  对象键
     * @param path 文件路径
     * @param options 可选项
     * @return 上传结果
     */
    PutObjectResult upload(String key, Path path, Options options);

    /**
     * 将本地路径对应的文件上传到默认存储桶。
     *
     * @param key  对象键
     * @param path 文件路径
     * @return 上传结果
     */
    PutObjectResult upload(String key, Path path);

    /**
     * 将文件上传到默认存储桶。
     *
     * @param key  对象键
     * @param file 文件对象
     * @param options 可选项
     * @return 上传结果
     */
    PutObjectResult upload(String key, File file, Options options);

    /**
     * 将文件上传到默认存储桶。
     *
     * @param key  对象键
     * @param file 文件对象
     * @return 上传结果
     */
    PutObjectResult upload(String key, File file);

    /**
     * 将随机访问文件上传到默认存储桶。
     *
     * @param key  对象键
     * @param file 随机访问文件
     * @param options 可选项
     * @return 上传结果
     */
    PutObjectResult upload(String key, RandomAccessFile file, Options options);

    /**
     * 将随机访问文件上传到默认存储桶。
     *
     * @param key  对象键
     * @param file 随机访问文件
     * @return 上传结果
     */
    PutObjectResult upload(String key, RandomAccessFile file);

    /**
     * 将可读通道中的数据上传到默认存储桶。
     *
     * @param key    对象键
     * @param channel            数据通道
     * @param contentLength 内容长度
     * @param options 可选项
     * @return 上传结果
     */
    PutObjectResult upload(String key, ReadableByteChannel channel, long contentLength, Options options);

    /**
     * 将可读通道中的数据上传到默认存储桶。
     *
     * @param key           对象键
     * @param channel       数据通道
     * @param contentLength 内容长度
     * @return 上传结果
     */
    PutObjectResult upload(String key, ReadableByteChannel channel, long contentLength);

    /**
     * 将输入流中的数据上传到默认存储桶。
     *
     * @param key    对象键
     * @param in            输入流
     * @param contentLength 内容长度
     * @param options 可选项
     * @return 上传结果
     */
    PutObjectResult upload(String key, InputStream in, long contentLength, Options options);

    /**
     * 将输入流中的数据上传到默认存储桶。
     *
     * @param key           对象键
     * @param in            输入流
     * @param contentLength 内容长度
     * @return 上传结果
     */
    PutObjectResult upload(String key, InputStream in, long contentLength);

    /**
     * 将字节数组上传到默认存储桶。
     *
     * @param key    对象键
     * @param data   字节数组
     * @param options 可选项
     * @return 上传结果
     */
    PutObjectResult upload(String key, byte[] data, Options options);

    /**
     * 将字节数组上传到默认存储桶。
     *
     * @param key  对象键
     * @param data 字节数组
     * @return 上传结果
     */
    PutObjectResult upload(String key, byte[] data);

    /**
     * 将默认存储桶中的对象下载到订阅器。
     *
     * @param key                对象键
     * @param downloadSubscriber 下载订阅器
     * @return 下载结果
     */
    GetObjectResult download(String key, OutputStreamDownloadSubscriber downloadSubscriber);

    /**
     * 将指定存储桶中的对象下载到转换器中，由使用者决定返回值。
     *
     * @param key                 对象键
     * @param downloadTransformer 下载转换器
     * @return 下载结果
     */
    <T> T download(String key, BiFunction<GetObjectResult, InputStream, T> downloadTransformer);

    /**
     * 将默认存储桶中的对象下载到本地路径。
     *
     * @param key  对象键
     * @param path 本地路径
     * @return 下载结果
     */
    GetObjectResult download(String key, Path path);

    /**
     * 将默认存储桶中的对象下载到文件。
     *
     * @param key  对象键
     * @param file 本地文件
     * @return 下载结果
     */
    GetObjectResult download(String key, File file);

    /**
     * 将默认存储桶中的对象下载到随机访问文件。
     *
     * @param key  对象键
     * @param file 随机访问文件
     * @return 下载结果
     */
    GetObjectResult download(String key, RandomAccessFile file);

    /**
     * 将默认存储桶中的对象下载到可写通道。
     *
     * @param key     对象键
     * @param channel 可写通道
     * @return 下载结果
     */
    GetObjectResult download(String key, WritableByteChannel channel);

    /**
     * 将默认存储桶中的对象下载到输出流。
     *
     * @param key 对象键
     * @param out 输出流
     * @return 下载结果
     */
    GetObjectResult download(String key, OutputStream out);

    /**
     * 删除默认存储桶中的对象。
     *
     * @param key 对象键
     * @return 是否删除成功
     */
    boolean delete(String key);

    /**
     * 生成默认存储桶中文件下载的预签名 URL。
     *
     * @param key         对象键
     * @param expiredTime URL 过期时间
     * @return 预签名下载 URL
     */
    String presignGetUrl(String key, Duration expiredTime);

    /**
     * 生成默认存储桶中文件上传的预签名 URL。
     *
     * @param key         对象键
     * @param expiredTime URL 过期时间
     * @param metadata    对象元数据
     * @return 预签名上传 URL
     */
    String presignPutUrl(String key, Duration expiredTime, Map<String, String> metadata);

    /**
     * 根据客户端配置生成默认对象Key。
     *
     * @param fileName 原始文件名
     * @return 对象Key
     */
    String buildPathKey(String fileName);

    /**
     * 根据业务前缀和客户端默认前缀生成对象Key。
     *
     * @param businessPrefix 业务前缀
     * @param fileName       原始文件名
     * @return 对象Key
     */
    String buildPathKey(String businessPrefix, String fileName);
}
