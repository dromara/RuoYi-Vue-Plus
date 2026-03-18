package org.dromara.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.lang.tree.Tree;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.SysMenu;
import org.dromara.system.domain.bo.SysMenuBo;
import org.dromara.system.domain.vo.RouterVo;
import org.dromara.system.domain.vo.SysMenuVo;
import org.dromara.system.service.ISysMenuService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单信息
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/menu")
public class SysMenuController extends BaseController {

    private final ISysMenuService menuService;

    /**
     * 获取路由信息
     *
     * @return 当前用户可访问的路由信息
     */
    @GetMapping("/getRouters")
    public R<List<RouterVo>> getRouters() {
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(LoginHelper.getUserId());
        return R.ok(menuService.buildMenus(menus));
    }

    /**
     * 查询菜单列表。
     *
     * @param menu 查询条件
     * @return 菜单列表
     */
    @SaCheckRole(value = {
        SystemConstants.SUPER_ADMIN_ROLE_KEY,
    }, mode = SaMode.OR)
    @SaCheckPermission("system:menu:list")
    @GetMapping("/list")
    public R<List<SysMenuVo>> list(SysMenuBo menu) {
        List<SysMenuVo> menus = menuService.selectMenuList(menu, LoginHelper.getUserId());
        return R.ok(menus);
    }

    /**
     * 根据菜单编号获取详细信息
     *
     * @param menuId 菜单ID
     * @return 菜单详情
     */
    @SaCheckRole(value = {
        SystemConstants.SUPER_ADMIN_ROLE_KEY,
    }, mode = SaMode.OR)
    @SaCheckPermission("system:menu:query")
    @GetMapping(value = "/{menuId}")
    public R<SysMenuVo> getInfo(@PathVariable Long menuId) {
        return R.ok(menuService.selectMenuById(menuId));
    }

    /**
     * 获取菜单下拉树列表。
     *
     * @param menu 查询条件
     * @return 菜单树
     */
    @SaCheckPermission("system:menu:query")
    @GetMapping("/treeselect")
    public R<List<Tree<Long>>> treeselect(SysMenuBo menu) {
        List<SysMenuVo> menus = menuService.selectMenuList(menu, LoginHelper.getUserId());
        return R.ok(menuService.buildMenuTreeSelect(menus));
    }

    /**
     * 加载对应角色菜单列表树
     *
     * @param roleId 角色ID
     * @return 角色菜单树及选中节点
     */
    @SaCheckPermission("system:menu:query")
    @GetMapping(value = "/roleMenuTreeselect/{roleId}")
    public R<MenuTreeSelectVo> roleMenuTreeselect(@PathVariable("roleId") Long roleId) {
        List<SysMenuVo> menus = menuService.selectMenuList(LoginHelper.getUserId());
        MenuTreeSelectVo selectVo = new MenuTreeSelectVo(
            menuService.selectMenuListByRoleId(roleId),
            menuService.buildMenuTreeSelect(menus));
        return R.ok(selectVo);
    }

    /**
     * 新增菜单。
     *
     * @param menu 菜单参数
     * @return 操作结果
     */
    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:menu:add")
    @Log(title = "菜单管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated @RequestBody SysMenuBo menu) {
        if (!menuService.checkMenuNameUnique(menu)) {
            return R.fail("新增菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        } else if (SystemConstants.YES.equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath())) {
            return R.fail("新增菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        } else if (!menuService.checkRouteConfigUnique(menu)) {
            return R.fail("新增菜单'" + menu.getMenuName() + "'失败，路由名称或地址已存在");
        }
        return toAjax(menuService.insertMenu(menu));
    }

    /**
     * 修改菜单。
     *
     * @param menu 菜单参数
     * @return 操作结果
     */
    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:menu:edit")
    @Log(title = "菜单管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated @RequestBody SysMenuBo menu) {
        if (!menuService.checkMenuNameUnique(menu)) {
            return R.fail("修改菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        } else if (SystemConstants.YES.equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath())) {
            return R.fail("修改菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        } else if (menu.getMenuId().equals(menu.getParentId())) {
            return R.fail("修改菜单'" + menu.getMenuName() + "'失败，上级菜单不能选择自己");
        } else if (!menuService.checkRouteConfigUnique(menu)) {
            return R.fail("修改菜单'" + menu.getMenuName() + "'失败，路由名称或地址已存在");
        }
        return toAjax(menuService.updateMenu(menu));
    }

    /**
     * 删除菜单
     *
     * @param menuId 菜单ID
     * @return 操作结果
     */
    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:menu:remove")
    @Log(title = "菜单管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{menuId}")
    public R<Void> remove(@PathVariable("menuId") Long menuId) {
        if (menuService.hasChildByMenuId(menuId)) {
            return R.warn("存在子菜单,不允许删除");
        }
        if (menuService.checkMenuExistRole(menuId)) {
            return R.warn("菜单已分配,不允许删除");
        }
        return toAjax(menuService.deleteMenuById(menuId));
    }

    /**
     * 角色菜单列表树信息
     *
     * @param checkedKeys 选中菜单列表
     * @param menus       菜单下拉树结构列表
     */
    public record MenuTreeSelectVo(List<Long> checkedKeys, List<Tree<Long>> menus) {
    }

    /**
     * 批量级联删除菜单
     *
     * @param menuIds 菜单ID串
     * @return 操作结果
     */
    @SaCheckRole(SystemConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:menu:remove")
    @Log(title = "菜单管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/cascade/{menuIds}")
    public R<Void> remove(@PathVariable("menuIds") Long[] menuIds) {
        List<Long> menuIdList = List.of(menuIds);
        if (menuService.hasChildByMenuId(menuIdList)) {
            return R.warn("存在子菜单,不允许删除");
        }
        menuService.deleteMenuById(menuIdList);
        return R.ok();
    }

}
