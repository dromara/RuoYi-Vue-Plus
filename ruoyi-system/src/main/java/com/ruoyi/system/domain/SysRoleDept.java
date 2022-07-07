package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色和部门关联 sys_role_dept
 *
 * @author Lion Li
 */

@Data
@TableName("sys_role_dept")
@Schema(name = "角色和部门关联")
public class SysRoleDept {

    /**
     * 角色ID
     */
    @TableId(type = IdType.INPUT)
    @Schema(name = "角色ID")
    private Long roleId;

    /**
     * 部门ID
     */
    @Schema(name = "部门ID")
    private Long deptId;

}
