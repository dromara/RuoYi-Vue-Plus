package org.dromara.common.oss.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 一页对象列表及下一页游标。
 *
 * @param objects 当前页对象
 * @param nextContinuationToken 下一页游标；为 null 表示已到末页
 */
public record ObjectListResult(List<Item> objects, String nextContinuationToken) {

    /**
     * 列表中的对象摘要。
     *
     * @param key 对象键
     * @param eTag ETag
     * @param lastModified 最后修改时间（UTC）
     * @param size 字节数
     */
    public record Item(String key, String eTag, LocalDateTime lastModified, long size) {
    }
}
