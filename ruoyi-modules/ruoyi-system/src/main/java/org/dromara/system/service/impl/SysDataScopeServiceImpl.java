package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.CacheNames;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.system.domain.SysRoleDept;
import org.dromara.system.mapper.SysDeptMapper;
import org.dromara.system.mapper.SysRoleDeptMapper;
import org.dromara.system.service.ISysDataScopeService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据权限 实现
 * <p>
 * 注意: 此Service内不允许调用标注`数据权限`注解的方法
 * 例如: deptMapper.selectList 此 selectList 方法标注了`数据权限`注解 会出现循环解析的问题
 * 当前实现仅负责返回角色自定义部门范围以及部门树展开后的 id 串，供数据权限插件拼装 SQL 时使用。
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service("sdss")
public class SysDataScopeServiceImpl implements ISysDataScopeService {

    private final SysRoleDeptMapper roleDeptMapper;
    private final SysDeptMapper deptMapper;

    /**
     * 获取角色自定义权限
     *
     * @param roleId 角色Id
     * @return 逗号分隔的部门 id 字符串，未配置时返回 `-1`
     */
    @Cacheable(cacheNames = CacheNames.SYS_ROLE_CUSTOM, key = "#roleId", condition = "#roleId != null")
    @Override
    public String getRoleCustom(Long roleId) {
        if (ObjectUtil.isNull(roleId)) {
            return "-1";
        }
        List<SysRoleDept> list = roleDeptMapper.lambda()
            .select(SysRoleDept::getDeptId)
            .eq(SysRoleDept::getRoleId, roleId)
            .list();
        if (CollUtil.isNotEmpty(list)) {
            return StreamUtils.join(list, rd -> Convert.toStr(rd.getDeptId()));
        }
        return "-1";
    }

    /**
     * 获取部门及以下权限
     *
     * @param deptId 部门Id
     * @return 当前部门及其子部门的逗号分隔 id 字符串，未查询到时返回 `-1`
     */
    @Cacheable(cacheNames = CacheNames.SYS_DEPT_AND_CHILD, key = "#deptId", condition = "#deptId != null")
    @Override
    public String getDeptAndChild(Long deptId) {
        if (ObjectUtil.isNull(deptId)) {
            return "-1";
        }
        List<Long> deptIds = deptMapper.selectDeptAndChildById(deptId);
        return CollUtil.isNotEmpty(deptIds) ? StreamUtils.join(deptIds, Convert::toStr) : "-1";
    }

}
