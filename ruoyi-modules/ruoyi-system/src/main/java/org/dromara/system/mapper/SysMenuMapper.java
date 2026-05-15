package org.dromara.system.mapper;

import cn.hutool.core.collection.CollUtil;
import com.github.yulichang.base.MPJBaseMapper;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.system.domain.SysMenu;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysRoleMenu;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.domain.bo.SysMenuBo;
import org.dromara.system.domain.vo.SysMenuVo;
import org.dromara.system.domain.vo.SysRoleMenuPermVo;

import java.util.*;

/**
 * 菜单表 数据层
 *
 * @author Lion Li
 */
public interface SysMenuMapper extends BaseMapperPlus<SysMenu, SysMenuVo>, MPJBaseMapper<SysMenu> {

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    default Set<String> selectMenuPermsByUserId(Long userId) {
        List<SysMenu> list = this.selectJoinList(SysMenu.class, QueryBuilder.lambdaJoin("m", SysMenu.class)
            .distinct()
            .select(SysMenu::getPerms)
            .leftJoin(SysRoleMenu.class, "srm", SysRoleMenu::getMenuId, SysMenu::getMenuId)
            .leftJoin(SysUserRole.class, "sur", SysUserRole::getRoleId, SysRoleMenu::getRoleId)
            .leftJoin(SysRole.class, "sr", SysRole::getRoleId, SysRoleMenu::getRoleId)
            .eq("sur", SysUserRole::getUserId, userId)
            .eq("sr", SysRole::getStatus, SystemConstants.NORMAL)
            .isNotNull("m", SysMenu::getPerms)
            .build());
        return new HashSet<>(StreamUtils.filter(StreamUtils.toList(list, SysMenu::getPerms), StringUtils::isNotBlank));
    }

    /**
     * 根据角色ID查询权限
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    default Set<String> selectMenuPermsByRoleId(Long roleId) {
        List<SysMenu> list = this.selectJoinList(SysMenu.class, QueryBuilder.lambdaJoin("m", SysMenu.class)
            .distinct()
            .select(SysMenu::getPerms)
            .leftJoin(SysRoleMenu.class, "srm", SysRoleMenu::getMenuId, SysMenu::getMenuId)
            .leftJoin(SysRole.class, "sr", SysRole::getRoleId, SysRoleMenu::getRoleId)
            .eq("srm", SysRoleMenu::getRoleId, roleId)
            .eq("sr", SysRole::getStatus, SystemConstants.NORMAL)
            .isNotNull("m", SysMenu::getPerms)
            .build());
        return new HashSet<>(StreamUtils.filter(StreamUtils.toList(list, SysMenu::getPerms), StringUtils::isNotBlank));
    }

    /**
     * 根据角色ID列表批量查询权限
     *
     * @param roleIds 角色ID列表
     * @return 角色权限映射
     */
    default Map<Long, Set<String>> selectMenuPermsByRoleIds(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Map.of();
        }
        List<SysRoleMenuPermVo> list = this.selectJoinList(SysRoleMenuPermVo.class, QueryBuilder.lambdaJoin("m", SysMenu.class)
            .distinct()
            .selectAs("srm", SysRoleMenu::getRoleId, SysRoleMenuPermVo::getRoleId)
            .selectAs(SysMenu::getPerms, SysRoleMenuPermVo::getPerms)
            .leftJoin(SysRoleMenu.class, "srm", SysRoleMenu::getMenuId, SysMenu::getMenuId)
            .leftJoin(SysRole.class, "sr", SysRole::getRoleId, SysRoleMenu::getRoleId)
            .in("srm", SysRoleMenu::getRoleId, roleIds)
            .eq("sr", SysRole::getStatus, SystemConstants.NORMAL)
            .isNotNull("m", SysMenu::getPerms)
            .build());
        Map<Long, Set<String>> result = new LinkedHashMap<>();
        for (SysRoleMenuPermVo item : list) {
            if (StringUtils.isBlank(item.getPerms())) {
                continue;
            }
            result.computeIfAbsent(item.getRoleId(), key -> new LinkedHashSet<>()).add(item.getPerms());
        }
        return result;
    }

    /**
     * 查询全部正常状态的目录和菜单
     *
     * @return 菜单列表
     */
    default List<SysMenu> selectMenuTreeAll() {
        return this.lambda()
            .in(SysMenu::getMenuType, SystemConstants.TYPE_DIR, SystemConstants.TYPE_MENU)
            .eq(SysMenu::getStatus, SystemConstants.NORMAL)
            .orderByAsc(SysMenu::getParentId)
            .orderByAsc(SysMenu::getOrderNum)
            .list();
    }

    /**
     * 根据角色ID查询菜单树信息
     *
     * @param roleId            角色ID
     * @param menuCheckStrictly 菜单树选择项是否关联显示
     * @return 选中菜单列表
     */
    default List<Long> selectMenuListByRoleId(Long roleId, boolean menuCheckStrictly) {
        List<SysMenu> menus = this.selectJoinList(SysMenu.class, QueryBuilder.lambdaJoin("m", SysMenu.class)
            .distinct()
            .select(SysMenu::getMenuId, SysMenu::getParentId, SysMenu::getOrderNum)
            .leftJoin(SysRoleMenu.class, "srm", SysRoleMenu::getMenuId, SysMenu::getMenuId)
            .leftJoin(SysRole.class, "sr", SysRole::getRoleId, SysRoleMenu::getRoleId)
            .eq("srm", SysRoleMenu::getRoleId, roleId)
            .eq("sr", SysRole::getStatus, SystemConstants.NORMAL)
            .orderByAsc("m", SysMenu::getParentId)
            .orderByAsc("m", SysMenu::getOrderNum)
            .build());
        Set<Long> parentIds = menuCheckStrictly ? new HashSet<>(StreamUtils.toList(menus, SysMenu::getParentId)) : Collections.emptySet();
        return menus.stream()
            .map(SysMenu::getMenuId)
            .filter(menuId -> !parentIds.contains(menuId))
            .toList();
    }

    /**
     * 根据用户ID和查询条件查询菜单列表
     *
     * @param menu   菜单查询条件
     * @param userId 用户ID
     * @return 菜单列表
     */
    default List<SysMenuVo> selectMenuListByUserId(SysMenuBo menu, Long userId) {
        return this.selectJoinList(SysMenuVo.class, QueryBuilder.lambdaJoin("m", SysMenu.class)
            .distinct()
            .selectAll(SysMenu.class)
            .leftJoin(SysRoleMenu.class, "srm", SysRoleMenu::getMenuId, SysMenu::getMenuId)
            .leftJoin(SysUserRole.class, "sur", SysUserRole::getRoleId, SysRoleMenu::getRoleId)
            .leftJoin(SysRole.class, "sr", SysRole::getRoleId, SysRoleMenu::getRoleId)
            .eq("sur", SysUserRole::getUserId, userId)
            .eq("sr", SysRole::getStatus, SystemConstants.NORMAL)
            .likeIfText("m", SysMenu::getMenuName, menu.getMenuName())
            .eqIfText("m", SysMenu::getVisible, menu.getVisible())
            .eqIfText("m", SysMenu::getStatus, menu.getStatus())
            .eqIfText("m", SysMenu::getMenuType, menu.getMenuType())
            .eqIfPresent("m", SysMenu::getParentId, menu.getParentId())
            .orderByAsc("m", SysMenu::getParentId)
            .orderByAsc("m", SysMenu::getOrderNum)
            .build());
    }

    /**
     * 根据用户ID查询正常状态的目录和菜单
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    default List<SysMenu> selectMenuTreeByUserId(Long userId) {
        return this.selectJoinList(SysMenu.class, QueryBuilder.lambdaJoin("m", SysMenu.class)
            .distinct()
            .selectAll(SysMenu.class)
            .leftJoin(SysRoleMenu.class, "srm", SysRoleMenu::getMenuId, SysMenu::getMenuId)
            .leftJoin(SysUserRole.class, "sur", SysUserRole::getRoleId, SysRoleMenu::getRoleId)
            .leftJoin(SysRole.class, "sr", SysRole::getRoleId, SysRoleMenu::getRoleId)
            .eq("sur", SysUserRole::getUserId, userId)
            .eq("sr", SysRole::getStatus, SystemConstants.NORMAL)
            .in("m", SysMenu::getMenuType, SystemConstants.TYPE_DIR, SystemConstants.TYPE_MENU)
            .eq("m", SysMenu::getStatus, SystemConstants.NORMAL)
            .orderByAsc("m", SysMenu::getParentId)
            .orderByAsc("m", SysMenu::getOrderNum)
            .build());
    }

}
