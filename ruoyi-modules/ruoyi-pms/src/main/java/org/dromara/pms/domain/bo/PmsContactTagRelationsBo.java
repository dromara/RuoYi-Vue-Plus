package org.dromara.pms.domain.bo;

import org.dromara.pms.domain.PmsContactTagRelations;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;

/**
 * 联系人标签关联业务对象 pms_contact_tag_relations
 *
 * @author xuhf
 * @date 2025-05-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsContactTagRelations.class, reverseConvertGenerate = false)
public class PmsContactTagRelationsBo extends BaseEntity {

    /**
     * 关联唯一ID
     */
    @NotNull(message = "关联唯一ID不能为空", groups = { EditGroup.class })
    private Long relationId;

    /**
     * 联系人ID
     */
    @NotNull(message = "联系人ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long contactId;

    /**
     * 标签ID
     */
    @NotNull(message = "标签ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long tagId;


}
