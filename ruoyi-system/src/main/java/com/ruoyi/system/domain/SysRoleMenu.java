package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色和菜单关联 sys_role_menu
 *
 * @author Lion Li
 */

@Data
@TableName("sys_role_menu")
@Schema(name = "角色和菜单关联")
public class SysRoleMenu {

    /**
     * 角色ID
     */
    @TableId(type = IdType.INPUT)
    @Schema(name = "角色ID")
    private Long roleId;

    /**
     * 菜单ID
     */
    @Schema(name = "角色ID")
    private Long menuId;

}
