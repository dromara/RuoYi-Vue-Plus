package org.dromara.common.oss.entity;

/**
 * 上传返回体
 *
 * @param url       文件访问地址
 * @param filename  文件名
 * @param eTag      存储服务返回的 ETag
 * @author Lion Li
 */
public record UploadResult(
    String url,
    String filename,
    String eTag
) {
}
