package org.dromara.common.oss.s3.builder;

import org.dromara.common.oss.s3.client.S3StorageClient;

/**
 * S3存储客户端构建器
 *
 * @param <T> 参数类型
 * @author 秋辞未寒
 */
public interface S3StorageClientBuilder<T> extends Builder<T,S3StorageClient> {

}
