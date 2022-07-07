package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户和角色关联 sys_user_role
 *
 * @author Lion Li
 */

@Data
@TableName("sys_user_role")
@Schema(name = "用户和角色关联")
public class SysUserRole {

    /**
     * 用户ID
     */
    @TableId(type = IdType.INPUT)
    @Schema(name = "用户ID")
    private Long userId;

    /**
     * 角色ID
     */
    @Schema(name = "角色ID")
    private Long roleId;

}
