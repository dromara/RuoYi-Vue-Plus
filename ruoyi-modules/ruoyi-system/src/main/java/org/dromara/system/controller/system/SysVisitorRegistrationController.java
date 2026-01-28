package org.dromara.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.SysVisitorRegistrationBo;
import org.dromara.system.domain.vo.SysVisitorRegistrationVo;
import org.dromara.system.service.ISysVisitorRegistrationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 访客预约登记控制器
 *
 * @author System
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/visitor")
public class SysVisitorRegistrationController extends BaseController {

    private final ISysVisitorRegistrationService visitorRegistrationService;

    /**
     * 查询访客预约登记列表
     */
    @SaCheckPermission("system:visitor:list")
    @GetMapping("/list")
    public TableDataInfo<SysVisitorRegistrationVo> list(SysVisitorRegistrationBo bo, PageQuery pageQuery) {
        return visitorRegistrationService.selectPageVisitorRegistrationList(bo, pageQuery);
    }

    /**
     * 获取访客预约登记详细信息
     *
     * @param id 主键ID
     */
    @SaCheckPermission("system:visitor:query")
    @GetMapping(value = "/{id}")
    public R<SysVisitorRegistrationVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(visitorRegistrationService.selectVisitorRegistrationById(id));
    }

    /**
     * 新增访客预约登记
     */
    @SaCheckPermission("system:visitor:add")
    @Log(title = "访客预约登记", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody SysVisitorRegistrationBo bo) {
        return toAjax(visitorRegistrationService.insertVisitorRegistration(bo));
    }

    /**
     * 修改访客预约登记
     */
    @SaCheckPermission("system:visitor:edit")
    @Log(title = "访客预约登记", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody SysVisitorRegistrationBo bo) {
        return toAjax(visitorRegistrationService.updateVisitorRegistration(bo));
    }

    /**
     * 删除访客预约登记
     *
     * @param ids 主键ID集合
     */
    @SaCheckPermission("system:visitor:remove")
    @Log(title = "访客预约登记", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotNull(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(visitorRegistrationService.deleteVisitorRegistrationByIds(List.of(ids)));
    }

    /**
     * 访客签到
     *
     * @param id 主键ID
     */
    @SaCheckPermission("system:visitor:checkin")
    @Log(title = "访客签到", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/checkin/{id}")
    public R<Void> checkIn(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(visitorRegistrationService.checkIn(id));
    }

    /**
     * 访客签离
     *
     * @param id 主键ID
     */
    @SaCheckPermission("system:visitor:checkout")
    @Log(title = "访客签离", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/checkout/{id}")
    public R<Void> checkOut(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(visitorRegistrationService.checkOut(id));
    }

    /**
     * 取消访客预约
     *
     * @param id 主键ID
     */
    @SaCheckPermission("system:visitor:cancel")
    @Log(title = "取消访客预约", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/cancel/{id}")
    public R<Void> cancelAppointment(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(visitorRegistrationService.cancelAppointment(id));
    }
}