package org.dromara.common.oss.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 不下载对象内容时获取的对象信息。
 *
 * @param key 对象键
 * @param eTag ETag
 * @param lastModified 最后修改时间（UTC）
 * @param size 对象大小（字节）
 * @param contentType 内容类型
 * @param contentDisposition 内容处置方式
 * @param metadata 自定义元数据
 */
public record ObjectInfoResult(String key, String eTag, LocalDateTime lastModified, long size,
                               String contentType, String contentDisposition, Map<String, String> metadata) {
}
