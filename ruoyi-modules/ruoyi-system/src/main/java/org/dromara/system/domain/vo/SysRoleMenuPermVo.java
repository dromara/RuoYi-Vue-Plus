package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色菜单权限视图
 */
@Data
public class SysRoleMenuPermVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long roleId;

    private String perms;
}
