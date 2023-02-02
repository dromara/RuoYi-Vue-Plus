package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.*;

import java.util.Date;

import com.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * 岗位信息业务对象 sys_post
 *
 * @author ruoyi
 * @date 2023-02-01
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class SysPostBo extends BaseEntity {

    /**
     * 岗位ID
     */
    @NotNull(message = "岗位ID不能为空", groups = { EditGroup.class })
    private Long postId;

    /**
     * 岗位编码
     */
    @NotBlank(message = "岗位编码不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 64, message = "岗位编码长度不能超过{max}个字符")
    private String postCode;

    /**
     * 岗位名称
     */
    @NotBlank(message = "岗位名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 50, message = "岗位名称长度不能超过{max}个字符")
    private String postName;

    /**
     * 显示顺序
     */
    @NotNull(message = "显示顺序不能为空", groups = { AddGroup.class, EditGroup.class })
    private Integer postSort;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;


}
