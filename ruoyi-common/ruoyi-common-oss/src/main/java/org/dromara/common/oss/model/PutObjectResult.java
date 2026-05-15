package org.dromara.common.oss.model;

/**
 * Put文件对象结果
 *
 * @param url
 * @param key
 * @param eTag
 * @param size
 * @author 秋辞未寒
 */
public record PutObjectResult(
        String url,
        String key,
        String eTag,
        long size
) {

    /**
     * 构建文件上传结果。
     *
     * @param url  文件地址
     * @param key  文件标识
     * @param eTag 文件 ETag
     * @param size 文件大小
     * @return 文件上传结果
     */
    public static PutObjectResult form(String url, String key, String eTag, long size) {
        return new PutObjectResult(url, key, eTag, size);
    }

}
