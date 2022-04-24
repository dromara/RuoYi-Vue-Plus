package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.SysRoleDept;
import com.ruoyi.system.domain.SysRoleMenu;
import com.ruoyi.system.domain.SysUserRole;
import com.ruoyi.system.mapper.SysRoleDeptMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysRoleMenuMapper;
import com.ruoyi.system.mapper.SysUserRoleMapper;
import com.ruoyi.system.service.ISysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 角色 业务层处理
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysRoleServiceImpl implements ISysRoleService {

    private final SysRoleMapper baseMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleDeptMapper roleDeptMapper;

    @Override
    public TableDataInfo<SysRole> selectPageRoleList(SysRole role, PageQuery pageQuery) {
        Page<SysRole> page = baseMapper.selectPageRoleList(pageQuery.build(), this.buildQueryWrapper(role));
        return TableDataInfo.build(page);
    }

    /**
     * 根据条件分页查询角色数据
     *
     * @param role 角色信息
     * @return 角色数据集合信息
     */
    @Override
    public List<SysRole> selectRoleList(SysRole role) {
        return baseMapper.selectRoleList(this.buildQueryWrapper(role));
    }

    private Wrapper<SysRole> buildQueryWrapper(SysRole role) {
        Map<String, Object> params = role.getParams();
        QueryWrapper<SysRole> wrapper = Wrappers.query();
        wrapper.eq("r.del_flag", UserConstants.ROLE_NORMAL)
            .eq(ObjectUtil.isNotNull(role.getRoleId()), "r.role_id", role.getRoleId())
            .like(StringUtils.isNotBlank(role.getRoleName()), "r.role_name", role.getRoleName())
            .eq(StringUtils.isNotBlank(role.getStatus()), "r.status", role.getStatus())
            .like(StringUtils.isNotBlank(role.getRoleKey()), "r.role_key", role.getRoleKey())
            .between(params.get("beginTime") != null && params.get("endTime") != null,
                "r.create_time", params.get("beginTime"), params.get("endTime"))
            .orderByAsc("r.role_sort");
        return wrapper;
    }

    /**
     * 根据用户ID查询角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    public List<SysRole> selectRolesByUserId(Long userId) {
        List<SysRole> userRoles = baseMapper.selectRolePermissionByUserId(userId);
        List<SysRole> roles = selectRoleAll();
        for (SysRole role : roles) {
            for (SysRole userRole : userRoles) {
                if (role.getRoleId().longValue() == userRole.getRoleId().longValue()) {
                    role.setFlag(true);
                    break;
                }
            }
        }
        return roles;
    }

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectRolePermissionByUserId(Long userId) {
        List<SysRole> perms = baseMapper.selectRolePermissionByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (SysRole perm : perms) {
            if (ObjectUtil.isNotNull(perm)) {
                permsSet.addAll(Arrays.asList(perm.getRoleKey().trim().split(",")));
            }
        }
        return permsSet;
    }

    /**
     * 查询所有角色
     *
     * @return 角色列表
     */
    @Override
    public List<SysRole> selectRoleAll() {
        return this.selectRoleList(new SysRole());
    }

    /**
     * 根据用户ID获取角色选择框列表
     *
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    @Override
    public List<Long> selectRoleListByUserId(Long userId) {
        return baseMapper.selectRoleListByUserId(userId);
    }

    /**
     * 通过角色ID查询角色
     *
     * @param roleId 角色ID
     * @return 角色对象信息
     */
    @Override
    public SysRole selectRoleById(Long roleId) {
        return baseMapper.selectById(roleId);
    }

    /**
     * 校验角色名称是否唯一
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public String checkRoleNameUnique(SysRole role) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getRoleName, role.getRoleName())
            .ne(ObjectUtil.isNotNull(role.getRoleId()), SysRole::getRoleId, role.getRoleId()));
        if (exist) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验角色权限是否唯一
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public String checkRoleKeyUnique(SysRole role) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getRoleKey, role.getRoleKey())
            .ne(ObjectUtil.isNotNull(role.getRoleId()), SysRole::getRoleId, role.getRoleId()));
        if (exist) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验角色是否允许操作
     *
     * @param role 角色信息
     */
    @Override
    public void checkRoleAllowed(SysRole role) {
        if (ObjectUtil.isNotNull(role.getRoleId()) && role.isAdmin()) {
            throw new ServiceException("不允许操作超级管理员角色");
        }
    }

    /**
     * 校验角色是否有数据权限
     *
     * @param roleId 角色id
     */
    @Override
    public void checkRoleDataScope(Long roleId) {
        if (!LoginHelper.isAdmin()) {
            SysRole role = new SysRole();
            role.setRoleId(roleId);
            List<SysRole> roles = this.selectRoleList(role);
            if (CollUtil.isEmpty(roles)) {
                throw new ServiceException("没有权限访问角色数据！");
            }
        }
    }

    /**
     * 通过角色ID查询角色使用数量
     *
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    public long countUserRoleByRoleId(Long roleId) {
        return userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
    }

    /**
     * 新增保存角色信息
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRole(SysRole role) {
        // 新增角色信息
        baseMapper.insert(role);
        return insertRoleMenu(role);
    }

    /**
     * 修改保存角色信息
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRole(SysRole role) {
        // 修改角色信息
        baseMapper.updateById(role);
        // 删除角色与菜单关联
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, role.getRoleId()));
        return insertRoleMenu(role);
    }

    /**
     * 修改角色状态
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public int updateRoleStatus(SysRole role) {
        return baseMapper.updateById(role);
    }

    /**
     * 修改数据权限信息
     *
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int authDataScope(SysRole role) {
        // 修改角色信息
        baseMapper.updateById(role);
        // 删除角色与部门关联
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, role.getRoleId()));
        // 新增角色和部门信息（数据权限）
        return insertRoleDept(role);
    }

    /**
     * 新增角色菜单信息
     *
     * @param role 角色对象
     */
    public int insertRoleMenu(SysRole role) {
        int rows = 1;
        // 新增用户与角色管理
        List<SysRoleMenu> list = new ArrayList<SysRoleMenu>();
        for (Long menuId : role.getMenuIds()) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(role.getRoleId());
            rm.setMenuId(menuId);
            list.add(rm);
        }
        if (list.size() > 0) {
            rows = roleMenuMapper.insertBatch(list) ? list.size() : 0;
        }
        return rows;
    }

    /**
     * 新增角色部门信息(数据权限)
     *
     * @param role 角色对象
     */
    public int insertRoleDept(SysRole role) {
        int rows = 1;
        // 新增角色与部门（数据权限）管理
        List<SysRoleDept> list = new ArrayList<SysRoleDept>();
        for (Long deptId : role.getDeptIds()) {
            SysRoleDept rd = new SysRoleDept();
            rd.setRoleId(role.getRoleId());
            rd.setDeptId(deptId);
            list.add(rd);
        }
        if (list.size() > 0) {
            rows = roleDeptMapper.insertBatch(list) ? list.size() : 0;
        }
        return rows;
    }

    /**
     * 通过角色ID删除角色
     *
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoleById(Long roleId) {
        // 删除角色与菜单关联
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        // 删除角色与部门关联
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId));
        return baseMapper.deleteById(roleId);
    }

    /**
     * 批量删除角色信息
     *
     * @param roleIds 需要删除的角色ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoleByIds(Long[] roleIds) {
        for (Long roleId : roleIds) {
            checkRoleAllowed(new SysRole(roleId));
            checkRoleDataScope(roleId);
            SysRole role = selectRoleById(roleId);
            if (countUserRoleByRoleId(roleId) > 0) {
                throw new ServiceException(String.format("%1$s已分配,不能删除", role.getRoleName()));
            }
        }
        List<Long> ids = Arrays.asList(roleIds);
        // 删除角色与菜单关联
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, ids));
        // 删除角色与部门关联
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().in(SysRoleDept::getRoleId, ids));
        return baseMapper.deleteBatchIds(ids);
    }

    /**
     * 取消授权用户角色
     *
     * @param userRole 用户和角色关联信息
     * @return 结果
     */
    @Override
    public int deleteAuthUser(SysUserRole userRole) {
        return userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
            .eq(SysUserRole::getRoleId, userRole.getRoleId())
            .eq(SysUserRole::getUserId, userRole.getUserId()));
    }

    /**
     * 批量取消授权用户角色
     *
     * @param roleId  角色ID
     * @param userIds 需要取消授权的用户数据ID
     * @return 结果
     */
    @Override
    public int deleteAuthUsers(Long roleId, Long[] userIds) {
        return userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
            .eq(SysUserRole::getRoleId, roleId)
            .in(SysUserRole::getUserId, Arrays.asList(userIds)));
    }

    /**
     * 批量选择授权用户角色
     *
     * @param roleId  角色ID
     * @param userIds 需要授权的用户数据ID
     * @return 结果
     */
    @Override
    public int insertAuthUsers(Long roleId, Long[] userIds) {
        // 新增用户与角色管理
        int rows = 1;
        List<SysUserRole> list = new ArrayList<SysUserRole>();
        for (Long userId : userIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            list.add(ur);
        }
        if (list.size() > 0) {
            rows = userRoleMapper.insertBatch(list) ? list.size() : 0;
        }
        return rows;
    }
}
