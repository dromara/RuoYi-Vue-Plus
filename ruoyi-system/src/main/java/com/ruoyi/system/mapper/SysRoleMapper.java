package com.ruoyi.system.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.mybatisplus.core.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色表 数据层
 *
 * @author ruoyi
 */
public interface SysRoleMapper extends BaseMapperPlus<SysRole> {

    Page<SysRole> selectPageRoleList(@Param("page") Page<SysRole> page, @Param("role") SysRole role);

    /**
     * 根据条件分页查询角色数据
     *
     * @param role 角色信息
     * @return 角色数据集合信息
     */
    public List<SysRole> selectRoleList(SysRole role);

    /**
     * 根据用户ID查询角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    public List<SysRole> selectRolePermissionByUserId(Long userId);


    /**
     * 根据用户ID获取角色选择框列表
     *
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    public List<Integer> selectRoleListByUserId(Long userId);

    /**
     * 根据用户ID查询角色
     *
     * @param userName 用户名
     * @return 角色列表
     */
    public List<SysRole> selectRolesByUserName(String userName);

}
