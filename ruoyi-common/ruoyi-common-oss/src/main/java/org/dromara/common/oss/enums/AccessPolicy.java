package org.dromara.common.oss.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.common.oss.exception.S3StorageException;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;

import java.util.Arrays;

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
    PRIVATE(0,BucketCannedACL.PRIVATE, ObjectCannedACL.PRIVATE),

    /**
     * 公有读写
     */
    PUBLIC_READ_WRITE(1,BucketCannedACL.PUBLIC_READ_WRITE, ObjectCannedACL.PUBLIC_READ_WRITE),

    /**
     * 公有只读
     */
    PUBLIC_READ(2,BucketCannedACL.PUBLIC_READ, ObjectCannedACL.PUBLIC_READ);

    /**
     * 访问策略类型
     */
    private final Integer type;

    /**
     * 桶权限
     */
    private final BucketCannedACL bucketCannedACL;

    /**
     * 文件对象权限
     */
    private final ObjectCannedACL objectCannedACL;

    public static AccessPolicy formType(String type) {
        return Arrays.stream(values())
            .filter(policy -> policy.getType().toString().equals(type))
            .findFirst()
            .orElseThrow(() -> S3StorageException.form("'type' not found By " + type));
    }
}
