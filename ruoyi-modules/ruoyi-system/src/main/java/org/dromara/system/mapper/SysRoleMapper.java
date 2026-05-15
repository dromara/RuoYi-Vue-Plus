package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.annotation.DataColumn;
import org.dromara.common.mybatis.annotation.DataPermission;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.domain.vo.SysRoleVo;

import java.util.Collection;
import java.util.List;

/**
 * 角色表 数据层
 *
 * @author Lion Li
 */
public interface SysRoleMapper extends BaseMapperPlus<SysRole, SysRoleVo>, MPJBaseMapper<SysRole> {

    /**
     * 分页查询角色列表
     *
     * @param page         分页对象
     * @param queryWrapper 查询条件
     * @return 包含角色信息的分页结果
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "create_dept"),
        @DataColumn(key = "userName", value = "create_by")
    })
    default Page<SysRoleVo> selectPageRoleList(@Param("page") Page<SysRole> page, @Param(Constants.WRAPPER) Wrapper<SysRole> queryWrapper) {
        return this.selectVoPage(page, queryWrapper);
    }

    /**
     * 根据条件查询角色数据
     *
     * @param queryWrapper 查询条件
     * @return 角色数据集合信息
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "create_dept"),
        @DataColumn(key = "userName", value = "create_by")
    })
    default List<SysRoleVo> selectRoleList(@Param(Constants.WRAPPER) Wrapper<SysRole> queryWrapper) {
        return this.selectVoList(queryWrapper);
    }

    /**
     * 根据角色ID集合查询角色数量
     *
     * @param roleIds 角色ID列表
     * @return 匹配的角色数量
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "create_dept"),
        @DataColumn(key = "userName", value = "create_by")
    })
    default long selectRoleCount(Collection<Long> roleIds) {
        return this.lambda().in(SysRole::getRoleId, roleIds).count();
    }

    /**
     * 根据角色ID查询角色信息
     *
     * @param roleId 角色ID
     * @return 对应的角色信息
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "create_dept"),
        @DataColumn(key = "userName", value = "create_by")
    })
    default SysRoleVo selectRoleById(Long roleId) {
        return this.selectVoById(roleId);
    }

    /**
     * 根据用户ID查询角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    default List<SysRoleVo> selectRolesByUserId(Long userId) {
        return this.selectJoinList(SysRoleVo.class, QueryBuilder.lambdaJoin("r", SysRole.class)
            .select(SysRole::getRoleId, SysRole::getRoleName, SysRole::getRoleKey,
                SysRole::getRoleSort, SysRole::getDataScope, SysRole::getStatus)
            .leftJoin(SysUserRole.class, "sur", SysUserRole::getRoleId, SysRole::getRoleId)
            .eq("sur", SysUserRole::getUserId, userId)
            .build());
    }

}
