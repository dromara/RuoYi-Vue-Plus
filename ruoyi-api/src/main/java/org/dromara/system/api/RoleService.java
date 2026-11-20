package org.dromara.system.api;

import org.dromara.system.api.domain.RoleDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 通用 角色服务
 *
 * @author AprilWind
 */
public interface RoleService {

    /**
     * 根据角色 ID 列表查询角色名称映射关系
     *
     * @param roleIds 角色 ID 列表
     * @return Map，其中 key 为角色 ID，value 为对应的角色名称
     */
    Map<Long, String> selectRoleNamesByIds(Collection<Long> roleIds);

    /**
     * 根据ID查询角色
     *
     * @param roleId 角色ID
     * @return 角色信息
     */
    RoleDTO selectById(Long roleId);

    /**
     * 根据ID集合查询角色
     *
     * @param roleIds 角色ID 集合
     * @return 查询范围内的角色列表
     */
    List<RoleDTO> selectListByIds(Collection<Long> roleIds);

    /**
     * 查询用户关联的角色
     *
     * @param userId 用户ID
     * @return 用户关联的角色列表
     */
    List<RoleDTO> selectListByUserId(Long userId);

}
