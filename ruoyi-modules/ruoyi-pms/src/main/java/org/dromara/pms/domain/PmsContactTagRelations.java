package org.dromara.pms.domain;

import org.dromara.common.tenant.core.TenantEntity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 联系人标签关联对象 pms_contact_tag_relations
 *
 * @author xuhf
 * @date 2025-05-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_contact_tag_relations")
public class PmsContactTagRelations extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 关联唯一ID
     */
    @TableId(value = "relation_id")
    private Long relationId;

    /**
     * 联系人ID
     */
    private Long contactId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;


}
