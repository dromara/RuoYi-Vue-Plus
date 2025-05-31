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
import org.dromara.pms.domain.bo.PmsPricingCalculationBo;
import org.dromara.pms.domain.vo.PmsPricingCalculationVo;
import org.dromara.pms.service.IPmsPricingCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 价格计算历史Controller
 *
 * @author PMS
 * @date 2024-12-01
 */
@Validated
@RestController
@RequestMapping("/pms/pricing/calculations")
public class PmsPricingCalculationController extends BaseController {

    @Autowired
    private IPmsPricingCalculationService pmsPricingCalculationService;

    /**
     * 查询价格计算历史列表
     */
    @SaCheckPermission("pms:pricing:calculations:list")
    @GetMapping("/list")
    public TableDataInfo<PmsPricingCalculationVo> list(PmsPricingCalculationBo bo, PageQuery pageQuery) {
        return pmsPricingCalculationService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出价格计算历史列表
     */
    @SaCheckPermission("pms:pricing:calculations:export")
    @Log(title = "价格计算历史", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(PmsPricingCalculationBo bo, HttpServletResponse response) {
        List<PmsPricingCalculationVo> list = pmsPricingCalculationService.exportList(bo);
        ExcelUtil.exportExcel(list, "价格计算历史", PmsPricingCalculationVo.class, response);
    }

    /**
     * 获取价格计算历史详细信息
     */
    @SaCheckPermission("pms:pricing:calculations:query")
    @GetMapping("/{calculationId}")
    public R<PmsPricingCalculationVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long calculationId) {
        return R.ok(pmsPricingCalculationService.queryById(calculationId));
    }

    /**
     * 新增价格计算历史
     */
    @SaCheckPermission("pms:pricing:calculations:add")
    @Log(title = "价格计算历史", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PmsPricingCalculationBo bo) {
        return toAjax(pmsPricingCalculationService.insertByBo(bo));
    }

    /**
     * 修改价格计算历史
     */
    @SaCheckPermission("pms:pricing:calculations:edit")
    @Log(title = "价格计算历史", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsPricingCalculationBo bo) {
        return toAjax(pmsPricingCalculationService.updateByBo(bo));
    }

    /**
     * 删除价格计算历史
     */
    @SaCheckPermission("pms:pricing:calculations:remove")
    @Log(title = "价格计算历史", businessType = BusinessType.DELETE)
    @DeleteMapping("/{calculationIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] calculationIds) {
        return toAjax(pmsPricingCalculationService.deleteWithValidByIds(List.of(calculationIds), true));
    }

    /**
     * 计算房间价格
     */
    @SaCheckPermission("pms:pricing:calculations:calculate")
    @Log(title = "计算房间价格", businessType = BusinessType.OTHER)
    @PostMapping("/calculate")
    public R<Map<String, Object>> calculatePrice(@RequestBody Map<String, Object> params) {
        String tenantId = (String) params.get("tenantId");
        Long deptId = Long.valueOf(params.get("deptId").toString());
        Long roomTypeId = Long.valueOf(params.get("roomTypeId").toString());
        LocalDate checkInDate = LocalDate.parse(params.get("checkInDate").toString());
        LocalDate checkOutDate = LocalDate.parse(params.get("checkOutDate").toString());
        Integer numAdults = (Integer) params.get("numAdults");
        Integer numChildren = (Integer) params.get("numChildren");
        String channelCode = (String) params.get("channelCode");
        String memberLevel = (String) params.get("memberLevel");
        Integer advanceBookingDays = (Integer) params.get("advanceBookingDays");
        Boolean saveHistory = (Boolean) params.getOrDefault("saveHistory", true);

        Map<String, Object> result = pmsPricingCalculationService.calculatePrice(
                tenantId, deptId, roomTypeId, checkInDate, checkOutDate,
                numAdults, numChildren, channelCode, memberLevel, advanceBookingDays, saveHistory);

        return R.ok(result);
    }

    /**
     * 批量计算价格
     */
    @SaCheckPermission("pms:pricing:calculations:calculate")
    @Log(title = "批量计算价格", businessType = BusinessType.OTHER)
    @PostMapping("/batch-calculate")
    public R<List<Map<String, Object>>> batchCalculatePrice(@RequestBody List<PmsPricingCalculationBo> calculations) {
        List<Map<String, Object>> results = pmsPricingCalculationService.batchCalculatePrice(calculations);
        return R.ok(results);
    }

    /**
     * 查询价格趋势数据
     */
    @SaCheckPermission("pms:pricing:calculations:analysis")
    @GetMapping("/trend")
    public R<List<Map<String, Object>>> getPriceTrend(@RequestParam String tenantId,
            @RequestParam Long deptId,
            @RequestParam Long roomTypeId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<Map<String, Object>> trend = pmsPricingCalculationService.getPriceTrend(
                tenantId, deptId, roomTypeId, startDate, endDate);
        return R.ok(trend);
    }

    /**
     * 查询规则效果分析数据
     */
    @SaCheckPermission("pms:pricing:calculations:analysis")
    @GetMapping("/rule-effect")
    public R<List<Map<String, Object>>> getRuleEffectAnalysis(@RequestParam String tenantId,
            @RequestParam Long deptId,
            @RequestParam Long ruleId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<Map<String, Object>> analysis = pmsPricingCalculationService.getRuleEffectAnalysis(
                tenantId, deptId, ruleId, startDate, endDate);
        return R.ok(analysis);
    }

    /**
     * 查询收益分析数据
     */
    @SaCheckPermission("pms:pricing:calculations:analysis")
    @GetMapping("/revenue-analysis")
    public R<List<Map<String, Object>>> getRevenueAnalysis(@RequestParam String tenantId,
            @RequestParam Long deptId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<Map<String, Object>> analysis = pmsPricingCalculationService.getRevenueAnalysis(
                tenantId, deptId, startDate, endDate);
        return R.ok(analysis);
    }

    /**
     * 查询平均价格
     */
    @SaCheckPermission("pms:pricing:calculations:analysis")
    @GetMapping("/average-price")
    public R<Map<String, Object>> getAveragePrice(@RequestParam String tenantId,
            @RequestParam Long deptId,
            @RequestParam Long roomTypeId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        Map<String, Object> averagePrice = pmsPricingCalculationService.getAveragePrice(
                tenantId, deptId, roomTypeId, startDate, endDate);
        return R.ok(averagePrice);
    }

    /**
     * 查询价格分布统计
     */
    @SaCheckPermission("pms:pricing:calculations:analysis")
    @GetMapping("/price-distribution")
    public R<List<Map<String, Object>>> getPriceDistribution(@RequestParam String tenantId,
            @RequestParam Long deptId,
            @RequestParam Long roomTypeId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<Map<String, Object>> distribution = pmsPricingCalculationService.getPriceDistribution(
                tenantId, deptId, roomTypeId, startDate, endDate);
        return R.ok(distribution);
    }

    /**
     * 查询渠道价格对比
     */
    @SaCheckPermission("pms:pricing:calculations:analysis")
    @GetMapping("/channel-comparison")
    public R<List<Map<String, Object>>> getChannelPriceComparison(@RequestParam String tenantId,
            @RequestParam Long deptId,
            @RequestParam Long roomTypeId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<Map<String, Object>> comparison = pmsPricingCalculationService.getChannelPriceComparison(
                tenantId, deptId, roomTypeId, startDate, endDate);
        return R.ok(comparison);
    }

    /**
     * 清理过期的计算记录
     */
    @SaCheckPermission("pms:pricing:calculations:remove")
    @Log(title = "清理过期计算记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/clean-expired")
    public R<Integer> cleanExpiredRecords(@RequestParam LocalDateTime beforeDate) {
        Integer count = pmsPricingCalculationService.cleanExpiredRecords(beforeDate);
        return R.ok(count);
    }
}
