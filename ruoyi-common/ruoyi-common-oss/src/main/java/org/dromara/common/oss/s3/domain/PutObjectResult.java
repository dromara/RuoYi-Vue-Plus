package org.dromara.common.oss.s3.domain;

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

    public static PutObjectResult form(String url, String key, String eTag, long size) {
        return new PutObjectResult(url, key, eTag, size);
    }

}
