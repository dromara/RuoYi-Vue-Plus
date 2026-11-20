package org.dromara.common.oss.client;

import cn.hutool.core.util.IdUtil;
import org.dromara.common.oss.config.OssClientConfig;
import org.dromara.common.oss.io.OutputStreamDownloadSubscriber;
import org.dromara.common.oss.model.GetObjectResult;
import org.dromara.common.oss.model.ObjectInfoResult;
import org.dromara.common.oss.model.ObjectListResult;
import org.dromara.common.oss.model.DeleteObjectsResult;
import org.dromara.common.oss.model.HandleAsyncResult;
import org.dromara.common.oss.model.Options;
import org.dromara.common.oss.model.PartUploadResult;
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
import java.util.List;
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
     * @param bucket  存储桶名称
     * @param key     对象键
     * @param path    文件路径
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
     * @param bucket  存储桶名称
     * @param key     对象键
     * @param file    文件对象
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
     * @param bucket  存储桶名称
     * @param key     对象键
     * @param file    随机访问文件
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
     * @param options       可选项
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
     * @param options       可选项
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
     * @param bucket  存储桶名称
     * @param key     对象键
     * @param data    字节数组
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
     * 初始化分片上传（指定存储桶）。
     *
     * @param bucket 存储桶名称
     * @param key 对象键
     * @param options 对象内容类型和元数据；长度、MD5 和传输监听器不用于初始化
     * @return 分片上传任务 ID（uploadId），用于签名单片、查询、合并和取消
     */
    String bucketInitiateMultipartUpload(String bucket, String key, Options options);

    /**
     * 使用默认选项初始化分片上传（指定存储桶）。
     *
     * @param bucket 存储桶名称
     * @param key 对象键
     * @return 分片上传任务 ID（uploadId），用于签名单片、查询、合并和取消
     */
    String bucketInitiateMultipartUpload(String bucket, String key);

    /**
     * 上传单个分片（指定存储桶）。
     *
     * @param bucket 存储桶名称
     * @param key 对象键
     * @param uploadId 上传任务 ID
     * @param partNumber 分片编号，从 1 开始
     * @param body 单片请求体，由调用方提供准确长度并管理流资源
     * @return 分片编号和 ETag，用于完成上传时合并
     */
    PartUploadResult bucketUploadPart(String bucket, String key, String uploadId, int partNumber, AsyncRequestBody body);

    /**
     * 生成分片上传预签名 URL（指定存储桶）。
     *
     * @param bucket 存储桶名称
     * @param key 对象键
     * @param uploadId 上传任务 ID
     * @param partNumber 分片编号，从 1 开始
     * @param expiredTime 签名有效期
     * @return 预签名 URL；上传后需保存响应 ETag，浏览器直传需桶 CORS 暴露 ETag
     */
    String bucketPresignUploadPartUrl(String bucket, String key, String uploadId, int partNumber, Duration expiredTime);

    /**
     * 分页查询已上传分片（指定存储桶）。
     *
     * @param bucket 存储桶名称
     * @param key 对象键
     * @param uploadId 上传任务 ID
     * @param maxParts 每页数量，null 使用 1000
     * @param partNumberMarker 分片编号分页标记，null 使用 0；后续传上一页最后一个分片编号
     * @return 当前页已上传分片，按编号升序；可继续用最后一个分片编号查询，直至返回空列表
     */
    List<PartUploadResult> bucketListParts(String bucket, String key, String uploadId, Integer maxParts, Integer partNumberMarker);

    /**
     * 合并分片完成上传（指定存储桶）。
     *
     * @param bucket 存储桶名称
     * @param key 对象键
     * @param uploadId 上传任务 ID
     * @param parts 已上传分片编号及 ETag，按编号排序后提交；调用方应提供完整分片列表
     * @return 文件地址、key 和 ETag；服务端合并响应不提供大小，size 为 0（未知）；失败时不会自动取消上传
     */
    PutObjectResult bucketCompleteMultipartUpload(String bucket, String key, String uploadId, Collection<PartUploadResult> parts);

    /**
     * 取消分片上传并释放未合并分片（指定存储桶）。
     *
     * @param bucket 存储桶名称
     * @param key 对象键
     * @param uploadId 上传任务 ID
     */
    void bucketAbortMultipartUpload(String bucket, String key, String uploadId);

    /**
     * 初始化分片上传（默认存储桶）。
     *
     * @param key 对象键
     * @param options 对象内容类型和元数据；长度、MD5 和传输监听器不用于初始化
     * @return 分片上传任务 ID（uploadId），用于签名单片、查询、合并和取消
     */
    String initiateMultipartUpload(String key, Options options);

    /**
     * 使用默认选项初始化分片上传（默认存储桶）。
     *
     * @param key 对象键
     * @return 分片上传任务 ID（uploadId），用于签名单片、查询、合并和取消
     */
    String initiateMultipartUpload(String key);

    /**
     * 上传单个分片（默认存储桶）。
     *
     * @param key 对象键
     * @param uploadId 上传任务 ID
     * @param partNumber 分片编号，从 1 开始
     * @param body 单片请求体，由调用方提供准确长度并管理流资源
     * @return 分片编号和 ETag，用于完成上传时合并
     */
    PartUploadResult uploadPart(String key, String uploadId, int partNumber, AsyncRequestBody body);

    /**
     * 生成分片上传预签名 URL（默认存储桶）。
     *
     * @param key 对象键
     * @param uploadId 上传任务 ID
     * @param partNumber 分片编号，从 1 开始
     * @param expiredTime 签名有效期
     * @return 预签名 URL；上传后需保存响应 ETag，浏览器直传需桶 CORS 暴露 ETag
     */
    String presignUploadPartUrl(String key, String uploadId, int partNumber, Duration expiredTime);

    /**
     * 分页查询已上传分片（默认存储桶）。
     *
     * @param key 对象键
     * @param uploadId 上传任务 ID
     * @param maxParts 每页数量，null 使用 1000
     * @param partNumberMarker 分片编号分页标记，null 使用 0；后续传上一页最后一个分片编号
     * @return 当前页已上传分片，按编号升序；可继续用最后一个分片编号查询，直至返回空列表
     */
    List<PartUploadResult> listParts(String key, String uploadId, Integer maxParts, Integer partNumberMarker);

    /**
     * 合并分片完成上传（默认存储桶）。
     *
     * @param key 对象键
     * @param uploadId 上传任务 ID
     * @param parts 已上传分片编号及 ETag，按编号排序后提交；调用方应提供完整分片列表
     * @return 文件地址、key 和 ETag；服务端合并响应不提供大小，size 为 0（未知）；失败时不会自动取消上传
     */
    PutObjectResult completeMultipartUpload(String key, String uploadId, Collection<PartUploadResult> parts);

    /**
     * 取消分片上传并释放未合并分片（默认存储桶）。
     *
     * @param key 对象键
     * @param uploadId 上传任务 ID
     */
    void abortMultipartUpload(String key, String uploadId);

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
     * 查询指定存储桶中的对象信息，不下载对象内容。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @return 对象大小、内容类型、最后修改时间等信息；对象不存在时抛出存储异常
     */
    ObjectInfoResult bucketHeadObject(String bucket, String key);

    /**
     * 按前缀分页列举指定存储桶中的对象。
     *
     * @param bucket            存储桶名称
     * @param prefix            对象键前缀，null 表示不按前缀筛选
     * @param continuationToken 上一页返回的游标，null 表示第一页
     * @param maxKeys           每页最多返回的对象数，null 使用服务端默认值
     * @return 当前页对象摘要和下一页游标；游标为 null 表示没有下一页
     */
    ObjectListResult bucketListObjects(String bucket, String prefix, String continuationToken, Integer maxKeys);

    /**
     * 在对象存储服务中复制对象。
     *
     * @param sourceBucket      源存储桶名称
     * @param sourceKey         源对象键
     * @param destinationBucket 目标存储桶名称
     * @param destinationKey    目标对象键
     * @return 目标对象的 ETag
     */
    String bucketCopyObject(String sourceBucket, String sourceKey, String destinationBucket, String destinationKey);

    /**
     * 批量删除指定存储桶中的对象。请求成功不代表每个对象都删除成功。
     *
     * @param bucket 存储桶名称
     * @param keys   待删除的对象键，数量须为 1 到 1000 个
     * @return 已删除的对象键及逐项失败信息
     */
    DeleteObjectsResult bucketDeleteObjects(String bucket, Collection<String> keys);

    /**
     * 查询指定存储桶中对象的标签。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @return 标签键值对；没有标签时返回空 Map
     */
    Map<String, String> bucketGetObjectTags(String bucket, String key);

    /**
     * 替换指定存储桶中对象的全部标签。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     * @param tags   新标签集合；空 Map 删除全部标签，不接受 null
     */
    void bucketPutObjectTags(String bucket, String key, Map<String, String> tags);

    /**
     * 删除指定存储桶中对象的全部标签。
     *
     * @param bucket 存储桶名称
     * @param key    对象键
     */
    void bucketDeleteObjectTags(String bucket, String key);

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
     * 生成指定存储桶中对象的预签名 GET URL，要求存储服务以附件形式返回文件。
     *
     * @param bucket      存储桶名称
     * @param key         对象键
     * @param expiredTime URL 有效期
     * @param fileName    下载时使用的文件名，不接受 null
     * @return 带附件文件名响应参数的临时下载 URL
     */
    String bucketPresignDownloadUrl(String bucket, String key, Duration expiredTime, String fileName);

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
     * @param key     对象键
     * @param path    文件路径
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
     * @param key     对象键
     * @param file    文件对象
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
     * @param key     对象键
     * @param file    随机访问文件
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
     * @param key           对象键
     * @param channel       数据通道
     * @param contentLength 内容长度
     * @param options       可选项
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
     * @param key           对象键
     * @param in            输入流
     * @param contentLength 内容长度
     * @param options       可选项
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
     * @param key     对象键
     * @param data    字节数组
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
     * 查询默认存储桶中的对象信息，不下载对象内容。
     *
     * @param key 对象键
     * @return 对象信息；对象不存在时抛出存储异常
     */
    ObjectInfoResult headObject(String key);

    /**
     * 按前缀分页列举默认存储桶中的对象。
     *
     * @param prefix            对象键前缀，null 表示不按前缀筛选
     * @param continuationToken 上一页返回的游标，null 表示第一页
     * @param maxKeys           每页最多返回的对象数，null 使用服务端默认值
     * @return 当前页对象摘要和下一页游标；游标为 null 表示没有下一页
     */
    ObjectListResult listObjects(String prefix, String continuationToken, Integer maxKeys);

    /**
     * 在默认存储桶中复制对象。
     *
     * @param sourceKey      源对象键
     * @param destinationKey 目标对象键
     * @return 目标对象的 ETag
     */
    String copyObject(String sourceKey, String destinationKey);

    /**
     * 批量删除默认存储桶中的对象。请求成功不代表每个对象都删除成功。
     *
     * @param keys 待删除的对象键，数量须为 1 到 1000 个
     * @return 已删除的对象键及逐项失败信息
     */
    DeleteObjectsResult deleteObjects(Collection<String> keys);

    /**
     * 查询默认存储桶中对象的标签。
     *
     * @param key 对象键
     * @return 标签键值对；没有标签时返回空 Map
     */
    Map<String, String> getObjectTags(String key);

    /**
     * 替换默认存储桶中对象的全部标签。
     *
     * @param key  对象键
     * @param tags 新标签集合；空 Map 删除全部标签，不接受 null
     */
    void putObjectTags(String key, Map<String, String> tags);

    /**
     * 删除默认存储桶中对象的全部标签。
     *
     * @param key 对象键
     */
    void deleteObjectTags(String key);

    /**
     * 生成默认存储桶中文件下载的预签名 URL。
     *
     * @param key         对象键
     * @param expiredTime URL 过期时间
     * @return 预签名下载 URL
     */
    String presignGetUrl(String key, Duration expiredTime);

    /**
     * 生成默认存储桶中对象的预签名 GET URL，要求存储服务以附件形式返回文件。
     *
     * @param key         对象键
     * @param expiredTime URL 有效期
     * @param fileName    下载时使用的文件名，不接受 null
     * @return 带附件文件名响应参数的临时下载 URL
     */
    String presignDownloadUrl(String key, Duration expiredTime, String fileName);

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
