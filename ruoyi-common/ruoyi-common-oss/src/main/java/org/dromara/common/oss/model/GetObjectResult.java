package org.dromara.common.oss.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Get文件对象结果
 *
 * @param key
 * @param eTag
 * @param lastModified
 * @param size
 * @param contentType
 * @param contentDisposition
 * @param contentRange
 * @param contentEncoding
 * @param contentLanguage
 * @param metadata
 * @author 秋辞未寒
 */
public record GetObjectResult(
        String key,
        String eTag,
        LocalDateTime lastModified,
        long size,
        String contentType,
        String contentDisposition,
        String contentRange,
        String contentEncoding,
        String contentLanguage,
        Map<String, String> metadata
) {

    public static GetObjectResult form(String key, String eTag, LocalDateTime lastModified, long size
            , String contentType, String contentDisposition, String contentRange, String contentEncoding, String contentLanguage
            , Map<String, String> metadata) {
        return new GetObjectResult(key, eTag, lastModified, size, contentType, contentDisposition, contentRange, contentEncoding, contentLanguage, metadata);
    }

}
