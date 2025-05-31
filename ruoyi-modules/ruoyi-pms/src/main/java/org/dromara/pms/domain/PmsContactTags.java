package org.dromara.pms.domain;

import org.dromara.common.tenant.core.TenantEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 联系人标签对象 pms_contact_tags
 *
 * @author xuhf
 * @date 2025-05-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_contact_tags")
public class PmsContactTags extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签唯一ID
     */
    @TableId(value = "tag_id")
    private Long tagId;

    /**
     * 部门ID (门店, 可空)
     */
    private Long deptId;

    /**
     * 标签名称
     */
    private String name;

    /**
     * 标签显示颜色
     */
    private String color;

    /**
     * 标签分类
     */
    private String category;

    /**
     * 标签描述
     */
    private String description;

    /**
     * 是否为系统预设标签
     */
    private Long isSystem;

    /**
     * 排序值
     */
    private Long sortOrder;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;


}
