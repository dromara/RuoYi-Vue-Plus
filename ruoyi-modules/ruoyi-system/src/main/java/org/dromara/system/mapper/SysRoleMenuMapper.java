package org.dromara.system.mapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysRoleMenu;

import java.util.List;

/**
 * 角色与菜单关联表 数据层
 *
 * @author Lion Li
 */
public interface SysRoleMenuMapper extends BaseMapperPlus<SysRoleMenu, SysRoleMenu> {

    /**
     * 根据菜单ID串删除关联关系
     *
     * @return 结果
     */
    default int deleteByMenuIds(List<Long> menuIds) {
        LambdaUpdateWrapper<SysRoleMenu> lqw = new LambdaUpdateWrapper<SysRoleMenu>()
            .in(SysRoleMenu::getMenuId, menuIds);
        return this.delete(lqw);
    }

}
