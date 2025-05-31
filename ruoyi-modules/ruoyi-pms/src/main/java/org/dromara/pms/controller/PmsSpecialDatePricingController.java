package org.dromara.pms.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.pms.domain.PmsSpecialDatePricing;
import org.dromara.pms.domain.bo.PmsSpecialDatePricingBo;
import org.dromara.pms.domain.vo.PmsSpecialDatePricingVo;
import org.dromara.pms.service.IPmsSpecialDatePricingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 特殊日期价格Controller
 *
 * @author PMS
 * @date 2024-12-01
 */
@Validated
@RestController
@RequestMapping("/pms/pricing/special-dates")
public class PmsSpecialDatePricingController extends BaseController {

    @Autowired
    private IPmsSpecialDatePricingService pmsSpecialDatePricingService;

    /**
     * 查询特殊日期价格列表
     */
    @SaCheckPermission("pms:pricing:specialDates:list")
    @GetMapping("/list")
    public TableDataInfo<PmsSpecialDatePricingVo> list(PmsSpecialDatePricingBo bo, PageQuery pageQuery) {
        return pmsSpecialDatePricingService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出特殊日期价格列表
     */
    @SaCheckPermission("pms:pricing:specialDates:export")
    @Log(title = "特殊日期价格", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(PmsSpecialDatePricingBo bo, HttpServletResponse response) {
        List<PmsSpecialDatePricingVo> list = pmsSpecialDatePricingService.exportList(bo);
        ExcelUtil.exportExcel(list, "特殊日期价格", PmsSpecialDatePricingVo.class, response);
    }

    /**
     * 获取特殊日期价格详细信息
     */
    @SaCheckPermission("pms:pricing:specialDates:query")
    @GetMapping("/{specialDateId}")
    public R<PmsSpecialDatePricingVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long specialDateId) {
        return R.ok(pmsSpecialDatePricingService.queryById(specialDateId));
    }

    /**
     * 新增特殊日期价格
     */
    @SaCheckPermission("pms:pricing:specialDates:add")
    @Log(title = "特殊日期价格", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PmsSpecialDatePricingBo bo) {
        return toAjax(pmsSpecialDatePricingService.insertByBo(bo));
    }

    /**
     * 修改特殊日期价格
     */
    @SaCheckPermission("pms:pricing:specialDates:edit")
    @Log(title = "特殊日期价格", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsSpecialDatePricingBo bo) {
        return toAjax(pmsSpecialDatePricingService.updateByBo(bo));
    }

    /**
     * 删除特殊日期价格
     */
    @SaCheckPermission("pms:pricing:specialDates:remove")
    @Log(title = "特殊日期价格", businessType = BusinessType.DELETE)
    @DeleteMapping("/{specialDateIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] specialDateIds) {
        return toAjax(pmsSpecialDatePricingService.deleteWithValidByIds(List.of(specialDateIds), true));
    }

    /**
     * 查询指定日期范围内的特殊日期价格
     */
    @SaCheckPermission("pms:pricing:specialDates:query")
    @GetMapping("/range")
    public R<List<PmsSpecialDatePricing>> getSpecialDatesByRange(@RequestParam String tenantId,
            @RequestParam Long deptId,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<PmsSpecialDatePricing> specialDates = pmsSpecialDatePricingService.getSpecialDatesByRange(
                tenantId, deptId, roomTypeId, startDate, endDate);
        return R.ok(specialDates);
    }

    /**
     * 查询指定日期的特殊价格
     */
    @SaCheckPermission("pms:pricing:specialDates:query")
    @GetMapping("/date")
    public R<PmsSpecialDatePricing> getSpecialDateByDate(@RequestParam String tenantId,
            @RequestParam Long deptId,
            @RequestParam Long roomTypeId,
            @RequestParam LocalDate specialDate) {
        PmsSpecialDatePricing specialDatePricing = pmsSpecialDatePricingService.getSpecialDateByDate(
                tenantId, deptId, roomTypeId, specialDate);
        return R.ok(specialDatePricing);
    }

    /**
     * 查询有效的特殊日期价格
     */
    @SaCheckPermission("pms:pricing:specialDates:query")
    @GetMapping("/active")
    public R<List<PmsSpecialDatePricing>> getActiveSpecialDates(@RequestParam String tenantId,
            @RequestParam Long deptId) {
        List<PmsSpecialDatePricing> activeSpecialDates = pmsSpecialDatePricingService.getActiveSpecialDates(
                tenantId, deptId);
        return R.ok(activeSpecialDates);
    }

    /**
     * 检查特殊日期冲突
     */
    @SaCheckPermission("pms:pricing:specialDates:query")
    @PostMapping("/check-conflict")
    public R<Boolean> checkDateConflict(@RequestBody PmsSpecialDatePricingBo bo) {
        return R.ok(pmsSpecialDatePricingService.checkDateConflict(bo));
    }

    /**
     * 批量设置特殊日期价格
     */
    @SaCheckPermission("pms:pricing:specialDates:add")
    @Log(title = "批量设置特殊日期价格", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public R<Void> batchInsertSpecialDates(@RequestBody List<PmsSpecialDatePricingBo> specialDates) {
        return toAjax(pmsSpecialDatePricingService.batchInsertSpecialDates(specialDates));
    }

    /**
     * 批量更新特殊日期价格状态
     */
    @SaCheckPermission("pms:pricing:specialDates:edit")
    @Log(title = "批量更新特殊日期价格状态", businessType = BusinessType.UPDATE)
    @PutMapping("/batch/status")
    public R<Void> batchUpdateStatus(@RequestBody Map<String, Object> params) {
        List<Long> dateIds = (List<Long>) params.get("dateIds");
        String status = (String) params.get("status");
        return toAjax(pmsSpecialDatePricingService.batchUpdateStatus(dateIds, status));
    }

    /**
     * 启用特殊日期价格
     */
    @SaCheckPermission("pms:pricing:specialDates:edit")
    @Log(title = "启用特殊日期价格", businessType = BusinessType.UPDATE)
    @PutMapping("/{specialDateId}/enable")
    public R<Void> enableSpecialDate(@NotNull(message = "特殊日期ID不能为空") @PathVariable Long specialDateId) {
        return toAjax(pmsSpecialDatePricingService.enableSpecialDate(specialDateId));
    }

    /**
     * 禁用特殊日期价格
     */
    @SaCheckPermission("pms:pricing:specialDates:edit")
    @Log(title = "禁用特殊日期价格", businessType = BusinessType.UPDATE)
    @PutMapping("/{specialDateId}/disable")
    public R<Void> disableSpecialDate(@NotNull(message = "特殊日期ID不能为空") @PathVariable Long specialDateId) {
        return toAjax(pmsSpecialDatePricingService.disableSpecialDate(specialDateId));
    }

    /**
     * 复制特殊日期价格到其他日期
     */
    @SaCheckPermission("pms:pricing:specialDates:add")
    @Log(title = "复制特殊日期价格", businessType = BusinessType.INSERT)
    @PostMapping("/{sourceId}/copy")
    public R<Void> copySpecialDate(@NotNull(message = "源特殊日期ID不能为空") @PathVariable Long sourceId,
            @RequestParam LocalDate targetDate) {
        return toAjax(pmsSpecialDatePricingService.copySpecialDate(sourceId, targetDate));
    }

    /**
     * 查询按日期类型分组的统计信息
     */
    @SaCheckPermission("pms:pricing:specialDates:query")
    @GetMapping("/stats")
    public R<List<Map<String, Object>>> getStatsByDateType(@RequestParam String tenantId,
            @RequestParam Long deptId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<Map<String, Object>> stats = pmsSpecialDatePricingService.getStatsByDateType(
                tenantId, deptId, startDate, endDate);
        return R.ok(stats);
    }

    /**
     * 清理过期的特殊日期价格
     */
    @SaCheckPermission("pms:pricing:specialDates:remove")
    @Log(title = "清理过期特殊日期价格", businessType = BusinessType.DELETE)
    @DeleteMapping("/clean-expired")
    public R<Integer> cleanExpiredDates(@RequestParam LocalDate beforeDate) {
        Integer count = pmsSpecialDatePricingService.cleanExpiredDates(beforeDate);
        return R.ok(count);
    }
}
