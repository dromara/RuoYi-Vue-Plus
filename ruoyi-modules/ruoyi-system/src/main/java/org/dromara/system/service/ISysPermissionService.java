package org.dromara.system.service;

import org.dromara.common.core.domain.dto.RoleDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户权限处理
 *
 * @author Lion Li
 */
public interface ISysPermissionService {

    /**
     * 获取角色数据权限
     *
     * @param userId  用户id
     * @return 角色权限信息
     */
    Set<String> getRolePermission(Long userId);

    /**
     * 获取菜单数据权限
     *
     * @param userId  用户id
     * @return 菜单权限信息
     */
    Set<String> getMenuPermission(Long userId);

    /**
     * 根据角色列表构建数据权限角色映射
     *
     * @param roles 角色列表
     * @return key 为权限码 value 为命中的角色列表
     */
    Map<String, List<RoleDTO>> getDataScopeRoleMap(List<RoleDTO> roles);

}
