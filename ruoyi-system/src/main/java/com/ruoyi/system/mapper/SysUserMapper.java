package com.ruoyi.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.DataColumn;
import com.ruoyi.common.annotation.DataPermission;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户表 数据层
 *
 * @author Lion Li
 */
public interface SysUserMapper extends BaseMapperPlus<SysUserMapper, SysUser, SysUser> {

    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.user_id")
    })
    Page<SysUser> selectPageUserList(@Param("page") Page<SysUser> page, @Param(Constants.WRAPPER) Wrapper<SysUser> queryWrapper);

    /**
     * 根据条件分页查询用户列表
     *
     * @param queryWrapper 查询条件
     * @return 用户信息集合信息
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.user_id")
    })
    List<SysUser> selectUserList(@Param(Constants.WRAPPER) Wrapper<SysUser> queryWrapper);

    /**
     * 根据条件分页查询已配用户角色列表
     *
     * @param queryWrapper 查询条件
     * @return 用户信息集合信息
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.user_id")
    })
    Page<SysUser> selectAllocatedList(@Param("page") Page<SysUser> page, @Param(Constants.WRAPPER) Wrapper<SysUser> queryWrapper);

    /**
     * 根据条件分页查询未分配用户角色列表
     *
     * @param queryWrapper 查询条件
     * @return 用户信息集合信息
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.user_id")
    })
    Page<SysUser> selectUnallocatedList(@Param("page") Page<SysUser> page, @Param(Constants.WRAPPER) Wrapper<SysUser> queryWrapper);

    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    SysUser selectUserByUserName(String userName);

    /**
     * 通过手机号查询用户
     *
     * @param phonenumber 手机号
     * @return 用户对象信息
     */
    SysUser selectUserByPhonenumber(String phonenumber);

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象信息
     */
    SysUser selectUserById(Long userId);

}
