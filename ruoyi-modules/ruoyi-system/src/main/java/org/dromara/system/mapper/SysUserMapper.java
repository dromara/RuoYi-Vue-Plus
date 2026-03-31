package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysDept;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysUserExportVo;
import org.dromara.system.domain.vo.SysUserVo;

import java.util.List;

/**
 * 用户表 数据层
 *
 * @author Lion Li
 */
public interface SysUserMapper extends BaseMapperPlus<SysUser, SysUserVo>, MPJBaseMapper<SysUser> {

    /**
     * 分页查询用户列表，并进行数据权限控制
     *
     * @param page         分页参数
     * @param queryWrapper 查询条件
     * @return 分页的用户信息
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "create_by")
    })
    default Page<SysUserVo> selectPageUserList(Page<SysUser> page, Wrapper<SysUser> queryWrapper) {
        return this.selectVoPage(page, queryWrapper);
    }

    /**
     * 查询用户列表，并进行数据权限控制
     *
     * @param queryWrapper 查询条件
     * @return 用户信息集合
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "create_by")
    })
    default List<SysUserVo> selectUserList(Wrapper<SysUser> queryWrapper) {
        return this.selectVoList(queryWrapper);
    }

    /**
     * 根据条件分页查询用户列表
     *
     * @param user      查询条件
     * @param deptIds   部门ID集合
     * @return 用户信息集合信息
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.create_by")
    })
    default List<SysUserExportVo> selectUserExportList(SysUserBo user, List<Long> deptIds) {
        MPJLambdaWrapper<SysUser> wrapper = JoinWrappers.lambda("u", SysUser.class)
            .selectAll(SysUser.class)
            .selectAs("u1", SysUser::getUserName, SysUserExportVo::getLeaderName)
            .leftJoin(SysDept.class, "d", SysDept::getDeptId, SysUser::getDeptId)
            .leftJoin(SysUser.class, "u1", SysUser::getUserId, SysDept::getLeader)
            .eq("u", SysUser::getDelFlag, SystemConstants.NORMAL)
            .like(StringUtils.isNotBlank(user.getUserName()), "u", SysUser::getUserName, user.getUserName())
            .like(StringUtils.isNotBlank(user.getNickName()), "u", SysUser::getNickName, user.getNickName())
            .eq(StringUtils.isNotBlank(user.getStatus()), "u", SysUser::getStatus, user.getStatus())
            .like(StringUtils.isNotBlank(user.getPhoneNumber()), "u", SysUser::getPhoneNumber, user.getPhoneNumber())
            .between(user.getParams().get("beginTime") != null && user.getParams().get("endTime") != null,
                "u", SysUser::getCreateTime, user.getParams().get("beginTime"), user.getParams().get("endTime"))
            .in(deptIds != null && !deptIds.isEmpty(), "u", SysUser::getDeptId, deptIds)
            .orderByAsc("u", SysUser::getUserId);
        return this.selectJoinList(SysUserExportVo.class, wrapper);
    }

    /**
     * 根据条件分页查询已配用户角色列表
     *
     * @param page         分页信息
     * @param user         查询条件
     * @return 用户信息集合信息
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.create_by")
    })
    default Page<SysUserVo> selectAllocatedList(Page<SysUserVo> page, SysUserBo user) {
        MPJLambdaWrapper<SysUser> wrapper = this.buildUserRoleJoinWrapper(user)
            .eq(user.getRoleId() != null, "r", SysRole::getRoleId, user.getRoleId())
            .orderByAsc("u", SysUser::getUserId);
        return this.selectJoinPage(page, SysUserVo.class, wrapper);
    }

    /**
     * 根据条件分页查询未分配用户角色列表
     *
     * @param page    分页信息
     * @param user    查询条件
     * @param userIds 未分配用户角色的用户ID列表
     * @return 用户信息集合信息
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "d.dept_id"),
        @DataColumn(key = "userName", value = "u.create_by")
    })
    default Page<SysUserVo> selectUnallocatedList(Page<SysUserVo> page, SysUserBo user, List<Long> userIds) {
        MPJLambdaWrapper<SysUser> wrapper = this.buildUserRoleJoinWrapper(user)
            .notIn(userIds != null && !userIds.isEmpty(), "u", SysUser::getUserId, userIds)
            .orderByAsc("u", SysUser::getUserId);
        return this.selectJoinPage(page, SysUserVo.class, wrapper);
    }

    /**
     * 根据用户ID统计用户数量
     *
     * @param userId 用户ID
     * @return 用户数量
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "create_by")
    })
    default long countUserById(Long userId) {
        return this.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUserId, userId));
    }

    /**
     * 根据条件更新用户数据
     *
     * @param user          要更新的用户实体
     * @param updateWrapper 更新条件封装器
     * @return 更新操作影响的行数
     */
    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "create_by")
    })
    int update(@Param(Constants.ENTITY) SysUser user, @Param(Constants.WRAPPER) Wrapper<SysUser> updateWrapper);

    /**
     * 根据用户ID更新用户数据
     *
     * @param user 要更新的用户实体
     * @return 更新操作影响的行数
     */
    @Override
    @DataPermission({
        @DataColumn(key = "deptName", value = "dept_id"),
        @DataColumn(key = "userName", value = "create_by")
    })
    int updateById(@Param(Constants.ENTITY) SysUser user);

    default MPJLambdaWrapper<SysUser> buildUserRoleJoinWrapper(SysUserBo user) {
        return JoinWrappers.lambda("u", SysUser.class)
            .distinct()
            .selectAll(SysUser.class)
            .leftJoin(SysDept.class, "d", SysDept::getDeptId, SysUser::getDeptId)
            .leftJoin(SysUserRole.class, "sur", SysUserRole::getUserId, SysUser::getUserId)
            .leftJoin(SysRole.class, "r", SysRole::getRoleId, SysUserRole::getRoleId)
            .eq("u", SysUser::getDelFlag, SystemConstants.NORMAL)
            .like(StringUtils.isNotBlank(user.getUserName()), "u", SysUser::getUserName, user.getUserName())
            .eq(StringUtils.isNotBlank(user.getStatus()), "u", SysUser::getStatus, user.getStatus())
            .like(StringUtils.isNotBlank(user.getPhoneNumber()), "u", SysUser::getPhoneNumber, user.getPhoneNumber());
    }

}
