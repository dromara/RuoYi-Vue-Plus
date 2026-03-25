package org.dromara.demo.controller;

import cn.dev33.satoken.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SaToken 权限测试 接口文档输出测试
 *
 * @author AprilWind
 */
@Slf4j
@RestController
@RequestMapping("/demo/saTokenDoc")
public class SaTokenTestController {

    // ====================== 基础场景：单一校验规则 ======================

    /**
     * 场景1：仅登录校验（无角色/权限限制，只需登录态）
     */
    @SaCheckLogin
    @GetMapping("/basic/loginOnly")
    public R<Void> loginOnly() {
        log.info("【场景1】仅登录校验通过");
        return R.ok("仅登录校验通过，无需角色/权限");
    }

    /**
     * 场景2：单一角色校验（AND模式，默认）
     */
    @SaCheckRole("admin")
    @GetMapping("/basic/singleRole")
    public R<Void> singleRole() {
        log.info("【场景2】单一角色(admin)校验通过");
        return R.ok("拥有admin角色，校验通过");
    }

    /**
     * 场景3：单一权限校验（AND模式，默认）
     */
    @SaCheckPermission("system:user:view")
    @GetMapping("/basic/singlePermission")
    public R<Void> singlePermission() {
        log.info("【场景3】单一权限(system:user:view)校验通过");
        return R.ok("拥有system:user:view权限，校验通过");
    }

    /**
     * 场景4：忽略所有权限校验（SaIgnore优先级最高）
     */
    @SaIgnore
    @SaCheckRole("none_exist") // 该注解会被忽略
    @GetMapping("/basic/ignoreAll")
    public R<Void> ignoreAll() {
        log.info("【场景4】SaIgnore忽略所有权限校验");
        return R.ok("SaIgnore生效，所有权限校验被忽略");
    }

    // ====================== 进阶场景：多条件组合（AND/OR） ======================

    /**
     * 场景5：多角色AND模式（必须同时拥有所有角色）
     */
    @SaCheckRole(value = {"admin", "operator"}, mode = SaMode.AND)
    @GetMapping("/advance/multiRoleAnd")
    public R<Void> multiRoleAnd() {
        log.info("【场景5】多角色AND模式(admin+operator)校验通过");
        return R.ok("同时拥有admin和operator角色，校验通过");
    }

    /**
     * 场景6：多角色OR模式（拥有任一角色即可）
     */
    @SaCheckRole(value = {"admin", "test"}, mode = SaMode.OR)
    @GetMapping("/advance/multiRoleOr")
    public R<Void> multiRoleOr() {
        log.info("【场景6】多角色OR模式(admin|test)校验通过");
        return R.ok("拥有admin或test角色，校验通过");
    }

    /**
     * 场景7：多权限AND模式（必须同时拥有所有权限）
     */
    @SaCheckPermission(value = {"system:user:edit", "system:log:view"}, mode = SaMode.AND)
    @GetMapping("/advance/multiPermAnd")
    public R<Void> multiPermAnd() {
        log.info("【场景7】多权限AND模式(system:user:edit+system:log:view)校验通过");
        return R.ok("同时拥有system:user:edit和system:log:view权限，校验通过");
    }

    /**
     * 场景8：多权限OR模式（拥有任一权限即可）
     */
    @SaCheckPermission(value = {"system:user:add", "system:user:delete"}, mode = SaMode.OR)
    @GetMapping("/advance/multiPermOr")
    public R<Void> multiPermOr() {
        log.info("【场景8】多权限OR模式(system:user:add|system:user:delete)校验通过");
        return R.ok("拥有system:user:add或system:user:delete权限，校验通过");
    }

    // ====================== 高级场景：通配符/混合组合 ======================

    /**
     * 场景9：权限通配符匹配（前缀匹配）
     * 拥有system:user:* 即可匹配所有用户模块权限
     */
    @SaCheckPermission("system:user:*")
    @GetMapping("/advanced/permWildcardPrefix")
    public R<Void> permWildcardPrefix() {
        log.info("【场景9】权限通配符(system:user:*)校验通过");
        return R.ok("拥有system:user:*前缀权限，校验通过");
    }

    /**
     * 场景10：角色通配符匹配（前缀匹配）
     * 拥有admin_* 即可匹配所有admin开头的角色
     */
    @SaCheckRole("admin_*")
    @GetMapping("/advanced/roleWildcardPrefix")
    public R<Void> roleWildcardPrefix() {
        log.info("【场景10】角色通配符(admin_*)校验通过");
        return R.ok("拥有admin_*前缀角色，校验通过");
    }

    /**
     * 场景11：权限+角色混合AND模式（所有条件必须满足）
     * 需同时满足：拥有admin角色 + 拥有system:user:all权限
     */
    @SaCheckRole("admin")
    @SaCheckPermission("system:user:all")
    @GetMapping("/advanced/mixRolePermAnd")
    public R<Void> mixRolePermAnd() {
        log.info("【场景11】角色+权限混合AND(admin+system:user:all)校验通过");
        return R.ok("拥有admin角色且拥有system:user:all权限，校验通过");
    }

    /**
     * 场景12：权限+角色混合OR模式（任一条件满足即可）
     * 满足任一：拥有super_admin角色 | 拥有system:manage权限
     */
    @SaCheckRole(value = {"super_admin"}, mode = SaMode.OR)
    @SaCheckPermission(value = {"system:manage"}, mode = SaMode.OR)
    @GetMapping("/advanced/mixRolePermOr")
    public R<Void> mixRolePermOr() {
        log.info("【场景12】角色+权限混合OR(super_admin|system:manage)校验通过");
        return R.ok("拥有super_admin角色或system:manage权限，校验通过");
    }

    /**
     * 场景13：orRole参数（权限校验失败时，兜底角色校验）
     * 核心逻辑：无system:user:export权限时，检查是否有admin/operator角色
     */
    @SaCheckPermission(value = "system:user:export", orRole = {"admin", "operator"})
    @GetMapping("/advanced/permWithOrRole")
    public R<Void> permWithOrRole() {
        log.info("【场景13】权限+orRole兜底校验通过");
        return R.ok("拥有system:user:export权限，或拥有admin/operator角色，校验通过");
    }

    // ====================== 特殊场景：临时权限/注解覆盖 ======================

    /**
     * 场景14：SaIgnore局部覆盖（方法注解覆盖类注解，若有）
     * 假设类上有@SaCheckLogin，方法上@SaIgnore会覆盖
     */
    @SaIgnore
    @GetMapping("/special/ignoreOverride")
    public R<Void> ignoreOverride() {
        log.info("【场景14】SaIgnore覆盖类级别权限注解");
        return R.ok("方法级SaIgnore覆盖类级别权限校验");
    }

    /**
     * 场景15：临时权限校验（SaCheckPermission逻辑：临时权限>永久权限）
     * 注：临时权限需通过SaToken API手动设置，如 SaHolder.getStpLogic().setTempPermission("system:temp:test")
     */
    @SaCheckPermission("system:temp:test")
    @GetMapping("/special/tempPermission")
    public R<Void> tempPermission() {
        log.info("【场景15】临时权限(system:temp:test)校验通过");
        return R.ok("临时权限校验通过（需先通过API设置临时权限）");
    }

    /**
     * 场景16：登录类型指定（多端登录场景，如PC/APP/小程序）
     * 注：需配合SaToken多账号体系配置
     */
    @SaCheckLogin(type = "PC") // 仅校验PC端的登录态
    @GetMapping("/special/loginTypeSpecify")
    public R<Void> loginTypeSpecify() {
        log.info("【场景16】指定登录类型(PC)校验通过");
        return R.ok("仅PC端登录态校验通过");
    }
}
