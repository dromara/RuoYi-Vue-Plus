package com.ruoyi.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.lock.annotation.Lock4j;
import com.ruoyi.common.core.constant.TenantConstants;
import com.ruoyi.common.core.constant.UserConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.excel.utils.ExcelUtil;
import com.ruoyi.common.idempotent.annotation.RepeatSubmit;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.mybatis.core.page.PageQuery;
import com.ruoyi.common.mybatis.core.page.TableDataInfo;
import com.ruoyi.common.tenant.helper.TenantHelper;
import com.ruoyi.common.web.core.BaseController;
import com.ruoyi.system.domain.bo.SysTenantBo;
import com.ruoyi.system.domain.bo.SysUserBo;
import com.ruoyi.system.domain.vo.SysTenantVo;
import com.ruoyi.system.service.ISysTenantService;
import com.ruoyi.system.service.ISysUserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户管理
 *
 * @author Michelle.Chung
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/tenant")
public class SysTenantController extends BaseController {

    private final ISysTenantService sysTenantService;
    private final ISysUserService sysUserService;

    /**
     * 查询租户列表
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:list")
    @GetMapping("/list")
    public TableDataInfo<SysTenantVo> list(SysTenantBo bo, PageQuery pageQuery) {
        return sysTenantService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出租户列表
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:export")
    @Log(title = "租户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysTenantBo bo, HttpServletResponse response) {
        List<SysTenantVo> list = sysTenantService.queryList(bo);
        ExcelUtil.exportExcel(list, "租户", SysTenantVo.class, response);
    }

    /**
     * 获取租户详细信息
     *
     * @param id 主键
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:query")
    @GetMapping("/{id}")
    public R<SysTenantVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(sysTenantService.queryById(id));
    }

    /**
     * 新增租户
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:add")
    @Log(title = "租户", businessType = BusinessType.INSERT)
    @Lock4j
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SysTenantBo bo) {
        if (TenantConstants.NOT_PASS.equals(sysTenantService.checkCompanyNameUnique(bo))) {
            throw new ServiceException("新增租户'" + bo.getCompanyName() + "'失败，企业名称已存在");
        }
        SysUserBo userBo = new SysUserBo();
        userBo.setUserName(bo.getUsername());
        // 判断用户名是否重复
        if (UserConstants.NOT_UNIQUE.equals(sysUserService.checkUserNameUnique(userBo))) {
            throw new ServiceException("新增用户'" + bo.getUsername() + "'失败，登录账号已存在");
        }
        return toAjax(sysTenantService.insertByBo(bo));
    }

    /**
     * 修改租户
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:edit")
    @Log(title = "租户", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SysTenantBo bo) {
        if (UserConstants.NOT_UNIQUE.equals(sysTenantService.checkCompanyNameUnique(bo))) {
            throw new ServiceException("修改租户'" + bo.getCompanyName() + "'失败，公司名称已存在");
        }
        return toAjax(sysTenantService.updateByBo(bo));
    }

    /**
     * 状态修改
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenantPackage:edit")
    @Log(title = "租户套餐", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody SysTenantBo bo) {
        return toAjax(sysTenantService.updateTenantStatus(bo));
    }

    /**
     * 删除租户
     *
     * @param ids 主键串
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @SaCheckPermission("system:tenant:remove")
    @Log(title = "租户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(sysTenantService.deleteWithValidByIds(List.of(ids), true));
    }

    /**
     * 动态切换租户
     *
     * @param tenantId 租户ID
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @GetMapping("/dynamic/{tenantId}")
    public R<Void> dynamicTenant(@NotBlank(message = "租户ID不能为空") @PathVariable String tenantId) {
        TenantHelper.setDynamic(tenantId);
        return R.ok();
    }

    /**
     * 清除动态租户
     */
    @SaCheckRole(TenantConstants.SUPER_ADMIN_ROLE_KEY)
    @GetMapping("/dynamic/clear")
    public R<Void> dynamicClear() {
        TenantHelper.clearDynamic();
        return R.ok();
    }

}
