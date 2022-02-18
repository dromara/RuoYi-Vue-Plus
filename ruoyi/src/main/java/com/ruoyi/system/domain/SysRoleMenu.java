package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 角色和菜单关联 sys_role_menu
 *
 * @author Lion Li
 */

@Data
@TableName("sys_role_menu")
@ApiModel("角色和菜单关联")
public class SysRoleMenu {

    /**
     * 角色ID
     */
    @TableId(type = IdType.INPUT)
    @ApiModelProperty(value = "角色ID")
    private Long roleId;

    /**
     * 菜单ID
     */
    @ApiModelProperty(value = "角色ID")
    private Long menuId;

}
