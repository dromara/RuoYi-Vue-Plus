package org.dromara.common.oss.model;

/**
 * 已上传的单个分片，用于查询上传进度和提交合并。
 *
 * @param partNumber 分片编号，从 1 开始
 * @param eTag       存储服务返回的分片 ETag
 * @author AprilWind
 */
public record PartUploadResult(Integer partNumber, String eTag) {

    /**
     * 构建已上传分片结果。
     *
     * @param partNumber 分片编号
     * @param eTag       分片 ETag
     * @return 已上传分片
     */
    public static PartUploadResult form(Integer partNumber, String eTag) {
        return new PartUploadResult(partNumber, eTag);
    }
}
