package org.dromara.common.oss.s3.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;

/**
 * 访问策略
 *
 * @author 秋辞未寒
 */
@Getter
@AllArgsConstructor
public enum AccessPolicy {

    /**
     * 私有
     */
    PRIVATE(BucketCannedACL.PRIVATE, ObjectCannedACL.PRIVATE),

    /**
     * 公有读写
     */
    PUBLIC_READ_WRITE(BucketCannedACL.PUBLIC_READ_WRITE, ObjectCannedACL.PUBLIC_READ_WRITE),

    /**
     * 公有只读
     */
    PUBLIC_READ(BucketCannedACL.PUBLIC_READ, ObjectCannedACL.PUBLIC_READ);

    /**
     * 桶权限
     */
    private final BucketCannedACL bucketCannedACL;

    /**
     * 文件对象权限
     */
    private final ObjectCannedACL objectCannedACL;

}
