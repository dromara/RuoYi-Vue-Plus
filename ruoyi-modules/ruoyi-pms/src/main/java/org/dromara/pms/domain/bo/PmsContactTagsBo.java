package org.dromara.pms.domain.bo;

import org.dromara.pms.domain.PmsContactTags;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;

/**
 * 联系人标签业务对象 pms_contact_tags
 *
 * @author xuhf
 * @date 2025-05-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = PmsContactTags.class, reverseConvertGenerate = false)
public class PmsContactTagsBo extends BaseEntity {

    /**
     * 标签唯一ID
     */
    @NotNull(message = "标签唯一ID不能为空", groups = { EditGroup.class })
    private Long tagId;

    /**
     * 部门ID (门店, 可空)
     */
    private Long deptId;

    /**
     * 标签名称
     */
    @NotBlank(message = "标签名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 1, max = 50, message = "标签名称长度应在1-50个字符之间", groups = { AddGroup.class, EditGroup.class })
    private String name;

    /**
     * 标签显示颜色
     */
    @NotBlank(message = "标签颜色不能为空", groups = { AddGroup.class, EditGroup.class })
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "标签颜色格式不正确", groups = { AddGroup.class, EditGroup.class })
    private String color;

    /**
     * 标签分类
     */
    @NotBlank(message = "标签分类不能为空", groups = { AddGroup.class, EditGroup.class })
    private String category;

    /**
     * 标签描述
     */
    @Size(max = 200, message = "标签描述不能超过200个字符", groups = { AddGroup.class, EditGroup.class })
    private String description;

    /**
     * 是否为系统预设标签
     */
    private Long isSystem;

    /**
     * 排序值
     */
    @Min(value = 0, message = "排序值不能小于0", groups = { AddGroup.class, EditGroup.class })
    @Max(value = 9999, message = "排序值不能大于9999", groups = { AddGroup.class, EditGroup.class })
    private Long sortOrder;

}
