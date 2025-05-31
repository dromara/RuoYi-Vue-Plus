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
import org.dromara.pms.domain.bo.PmsRoomPricingRuleBo;
import org.dromara.pms.domain.vo.PmsRoomPricingRuleVo;
import org.dromara.pms.service.IPmsRoomPricingRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 房间价格规则Controller
 *
 * @author PMS
 * @date 2024-12-01
 */
@Validated
@RestController
@RequestMapping("/pms/pricing/rules")
public class PmsRoomPricingRuleController extends BaseController {

    @Autowired
    private IPmsRoomPricingRuleService pmsRoomPricingRuleService;

    /**
     * 查询房间价格规则列表
     */
    @SaCheckPermission("pms:pricing:rules:list")
    @GetMapping("/list")
    public TableDataInfo<PmsRoomPricingRuleVo> list(PmsRoomPricingRuleBo bo, PageQuery pageQuery) {
        return pmsRoomPricingRuleService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出房间价格规则列表
     */
    @SaCheckPermission("pms:pricing:rules:export")
    @Log(title = "房间价格规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(PmsRoomPricingRuleBo bo, HttpServletResponse response) {
        List<PmsRoomPricingRuleVo> list = pmsRoomPricingRuleService.exportList(bo);
        ExcelUtil.exportExcel(list, "房间价格规则", PmsRoomPricingRuleVo.class, response);
    }

    /**
     * 获取房间价格规则详细信息
     */
    @SaCheckPermission("pms:pricing:rules:query")
    @GetMapping("/{ruleId}")
    public R<PmsRoomPricingRuleVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long ruleId) {
        return R.ok(pmsRoomPricingRuleService.queryById(ruleId));
    }

    /**
     * 新增房间价格规则
     */
    @SaCheckPermission("pms:pricing:rules:add")
    @Log(title = "房间价格规则", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PmsRoomPricingRuleBo bo) {
        return toAjax(pmsRoomPricingRuleService.insertByBo(bo));
    }

    /**
     * 修改房间价格规则
     */
    @SaCheckPermission("pms:pricing:rules:edit")
    @Log(title = "房间价格规则", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsRoomPricingRuleBo bo) {
        return toAjax(pmsRoomPricingRuleService.updateByBo(bo));
    }

    /**
     * 删除房间价格规则
     */
    @SaCheckPermission("pms:pricing:rules:remove")
    @Log(title = "房间价格规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ruleIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ruleIds) {
        return toAjax(pmsRoomPricingRuleService.deleteWithValidByIds(List.of(ruleIds), true));
    }

    /**
     * 启用价格规则
     */
    @SaCheckPermission("pms:pricing:rules:edit")
    @Log(title = "启用价格规则", businessType = BusinessType.UPDATE)
    @PutMapping("/{ruleId}/enable")
    public R<Void> enableRule(@NotNull(message = "规则ID不能为空") @PathVariable Long ruleId) {
        return toAjax(pmsRoomPricingRuleService.enableRule(ruleId));
    }

    /**
     * 禁用价格规则
     */
    @SaCheckPermission("pms:pricing:rules:edit")
    @Log(title = "禁用价格规则", businessType = BusinessType.UPDATE)
    @PutMapping("/{ruleId}/disable")
    public R<Void> disableRule(@NotNull(message = "规则ID不能为空") @PathVariable Long ruleId) {
        return toAjax(pmsRoomPricingRuleService.disableRule(ruleId));
    }

    /**
     * 复制价格规则
     */
    @SaCheckPermission("pms:pricing:rules:add")
    @Log(title = "复制价格规则", businessType = BusinessType.INSERT)
    @PostMapping("/{ruleId}/copy")
    public R<Void> copyRule(@NotNull(message = "规则ID不能为空") @PathVariable Long ruleId,
            @RequestParam String newName) {
        return toAjax(pmsRoomPricingRuleService.copyRule(ruleId, newName));
    }

    /**
     * 批量更新规则状态
     */
    @SaCheckPermission("pms:pricing:rules:edit")
    @Log(title = "批量更新规则状态", businessType = BusinessType.UPDATE)
    @PutMapping("/batch/status")
    public R<Void> batchUpdateStatus(@RequestBody Map<String, Object> params) {
        List<Long> ruleIds = (List<Long>) params.get("ruleIds");
        String status = (String) params.get("status");
        return toAjax(pmsRoomPricingRuleService.batchUpdateStatus(ruleIds, status));
    }

    /**
     * 检查规则冲突
     */
    @SaCheckPermission("pms:pricing:rules:query")
    @PostMapping("/check-conflict")
    public R<Boolean> checkRuleConflict(@RequestBody PmsRoomPricingRuleBo bo) {
        return R.ok(pmsRoomPricingRuleService.checkRuleConflict(bo));
    }

    /**
     * 查询有效的价格规则
     */
    @SaCheckPermission("pms:pricing:rules:query")
    @GetMapping("/active")
    public R<List<PmsRoomPricingRuleVo>> getActiveRules(@RequestParam String tenantId,
            @RequestParam Long deptId) {
        return R.ok(pmsRoomPricingRuleService.getActiveRules(tenantId, deptId)
                .stream()
                .map(rule -> {
                    PmsRoomPricingRuleVo vo = new PmsRoomPricingRuleVo();
                    vo.setRuleId(rule.getRuleId());
                    vo.setName(rule.getName());
                    vo.setDescription(rule.getDescription());
                    vo.setPriceAdjustmentType(rule.getPriceAdjustmentType());
                    vo.setAdjustmentValue(rule.getAdjustmentValue());
                    vo.setPriority(rule.getPriority());
                    vo.setStatus(rule.getStatus());
                    return vo;
                })
                .toList());
    }
}
