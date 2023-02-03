package com.ruoyi.system.service;

import cn.hutool.core.collection.CollUtil;
import com.ruoyi.system.domain.vo.SysRoleVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户权限处理
 *
 * @author ruoyi
 */
@RequiredArgsConstructor
@Service
public class SysPermissionService {

    private final ISysRoleService roleService;
    private final ISysMenuService menuService;

    /**
     * 获取角色数据权限
     *
     * @param userId  用户id
     * @param isAdmin 是否管理员
     * @return 角色权限信息
     */
    public Set<String> getRolePermission(Long userId, boolean isAdmin) {
        Set<String> roles = new HashSet<>();
        // 管理员拥有所有权限
        if (isAdmin) {
            roles.add("admin");
        } else {
            roles.addAll(roleService.selectRolePermissionByUserId(userId));
        }
        return roles;
    }

    /**
     * 获取菜单数据权限
     *
     * @param userId  用户id
     * @param isAdmin 是否管理员
     * @return 菜单权限信息
     */
    public Set<String> getMenuPermission(Long userId, boolean isAdmin) {
        Set<String> perms = new HashSet<>();
        // 管理员拥有所有权限
        if (isAdmin) {
            perms.add("*:*:*");
        } else {
            List<SysRoleVo> roles = roleService.selectRolesByUserId(userId);
            if (CollUtil.isNotEmpty(roles)) {
                // 多角色设置permissions属性，以便数据权限匹配权限
                for (SysRoleVo role : roles) {
                    Set<String> rolePerms = menuService.selectMenuPermsByRoleId(role.getRoleId());
                    role.setPermissions(rolePerms);
                    perms.addAll(rolePerms);
                }
            } else {
                perms.addAll(menuService.selectMenuPermsByUserId(userId));
            }
        }
        return perms;
    }
}
