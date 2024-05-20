package org.dromara.common.oss.enumd;

import lombok.AllArgsConstructor;
import lombok.Getter;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;

/**
 * 桶访问策略配置
 *
 * @author 陈賝
 */
@Getter
@AllArgsConstructor
public enum AccessPolicyType {

    /**
     * private
     */
    PRIVATE("0", BucketCannedACL.PRIVATE, ObjectCannedACL.PRIVATE, PolicyType.WRITE),

    /**
     * public
     */
    PUBLIC("1", BucketCannedACL.PUBLIC_READ_WRITE, ObjectCannedACL.PUBLIC_READ_WRITE, PolicyType.READ_WRITE),

    /**
     * custom
     */
    CUSTOM("2", BucketCannedACL.PUBLIC_READ, ObjectCannedACL.PUBLIC_READ, PolicyType.READ);

    /**
     * 桶 权限类型（数据库值）
     */
    private final String type;

    /**
     * 桶 权限类型
     */
    private final BucketCannedACL bucketCannedACL;

    /**
     * 文件对象 权限类型
     */
    private final ObjectCannedACL objectCannedACL;

    /**
     * 桶策略类型
     */
    private final PolicyType policyType;

    public static AccessPolicyType getByType(String type) {
        for (AccessPolicyType value : values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        throw new RuntimeException("'type' not found By " + type);
    }

}
