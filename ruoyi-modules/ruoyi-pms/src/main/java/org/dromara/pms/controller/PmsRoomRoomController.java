package org.dromara.pms.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.web.core.BaseController;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.pms.domain.vo.PmsRoomRoomVo;
import org.dromara.pms.domain.bo.PmsRoomRoomBo;
import org.dromara.pms.service.IPmsRoomRoomService;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 房间管理
 *
 * @author xuhf
 * @date 2025-05-28
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/pms/room")
@Tag(name = "房间管理", description = "房间管理")
public class PmsRoomRoomController extends BaseController {

    private final IPmsRoomRoomService pmsRoomRoomService;

    /**
     * 查询房间管理列表
     */
    @Operation(summary = "查询房间管理列表")
    @SaCheckPermission("pms:room:list")
    @GetMapping("/list")
    public TableDataInfo<PmsRoomRoomVo> list(PmsRoomRoomBo bo, PageQuery pageQuery) {
        return pmsRoomRoomService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出房间管理列表
     */
    @Operation(summary = "导出房间管理列表")
    @SaCheckPermission("pms:room:export")
    @Log(title = "房间管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(PmsRoomRoomBo bo, HttpServletResponse response) {
        List<PmsRoomRoomVo> list = pmsRoomRoomService.queryList(bo);
        ExcelUtil.exportExcel(list, "房间管理", PmsRoomRoomVo.class, response);
    }

    /**
     * 获取房间管理详细信息
     */
    @Operation(summary = "获取房间管理详细信息")
    @SaCheckPermission("pms:room:query")
    @GetMapping("/{roomId}")
    public R<PmsRoomRoomVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long roomId) {
        return R.ok(pmsRoomRoomService.queryById(roomId));
    }

    /**
     * 新增房间管理
     */
    @Operation(summary = "新增房间管理")
    @SaCheckPermission("pms:room:add")
    @Log(title = "房间管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PmsRoomRoomBo bo) {
        return toAjax(pmsRoomRoomService.insertByBo(bo));
    }

    /**
     * 修改房间管理
     */
    @Operation(summary = "修改房间管理")
    @SaCheckPermission("pms:room:edit")
    @Log(title = "房间管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsRoomRoomBo bo) {
        return toAjax(pmsRoomRoomService.updateByBo(bo));
    }

    /**
     * 删除房间管理
     */
    @Operation(summary = "删除房间管理")
    @SaCheckPermission("pms:room:remove")
    @Log(title = "房间管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roomIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] roomIds) {
        return toAjax(pmsRoomRoomService.deleteWithValidByIds(List.of(roomIds), true));
    }

    /**
     * 根据房型ID查询房间列表
     */
    @Operation(summary = "根据房型ID查询房间列表")
    @SaCheckPermission("pms:room:query")
    @GetMapping("/roomType/{roomTypeId}")
    public R<List<PmsRoomRoomVo>> listByRoomType(@PathVariable Long roomTypeId) {
        return R.ok(pmsRoomRoomService.queryByRoomTypeId(roomTypeId));
    }

    /**
     * 根据部门ID查询房间列表
     */
    @Operation(summary = "根据部门ID查询房间列表")
    @SaCheckPermission("pms:room:query")
    @GetMapping("/dept/{deptId}")
    public R<List<PmsRoomRoomVo>> listByDept(@PathVariable Long deptId) {
        return R.ok(pmsRoomRoomService.queryByDeptId(deptId));
    }

    /**
     * 根据状态查询房间列表
     */
    @Operation(summary = "根据状态查询房间列表")
    @SaCheckPermission("pms:room:query")
    @GetMapping("/status")
    public R<List<PmsRoomRoomVo>> listByStatus(
            @Parameter(description = "部门ID") @RequestParam Long deptId,
            @Parameter(description = "房间物理状态") @RequestParam(required = false) String roomStatus,
            @Parameter(description = "清洁状态") @RequestParam(required = false) String cleaningStatus) {
        return R.ok(pmsRoomRoomService.queryByStatus(deptId, roomStatus, cleaningStatus));
    }

    /**
     * 更新房间状态
     */
    @Operation(summary = "更新房间状态")
    @SaCheckPermission("pms:room:updateStatus")
    @Log(title = "房间状态更新", businessType = BusinessType.UPDATE)
    @PutMapping("/{roomId}/status")
    public R<Void> updateRoomStatus(
            @PathVariable Long roomId,
            @Parameter(description = "房间物理状态") @RequestParam String roomStatus,
            @Parameter(description = "状态备注") @RequestParam(required = false) String statusRemarks) {
        return toAjax(pmsRoomRoomService.updateRoomStatus(roomId, roomStatus, statusRemarks));
    }

    /**
     * 更新清洁状态
     */
    @Operation(summary = "更新清洁状态")
    @SaCheckPermission("pms:room:updateStatus")
    @Log(title = "房间清洁状态更新", businessType = BusinessType.UPDATE)
    @PutMapping("/{roomId}/cleaning")
    public R<Void> updateCleaningStatus(
            @PathVariable Long roomId,
            @Parameter(description = "清洁状态") @RequestParam String cleaningStatus) {
        return toAjax(pmsRoomRoomService.updateCleaningStatus(roomId, cleaningStatus));
    }

    /**
     * 批量更新房间状态
     */
    @Operation(summary = "批量更新房间状态")
    @SaCheckPermission("pms:room:updateStatus")
    @Log(title = "批量房间状态更新", businessType = BusinessType.UPDATE)
    @PutMapping("/batch/status")
    public R<Void> batchUpdateRoomStatus(
            @Parameter(description = "房间ID列表") @RequestBody List<Long> roomIds,
            @Parameter(description = "房间物理状态") @RequestParam String roomStatus,
            @Parameter(description = "状态备注") @RequestParam(required = false) String statusRemarks) {
        return toAjax(pmsRoomRoomService.batchUpdateRoomStatus(roomIds, roomStatus, statusRemarks));
    }

    /**
     * 批量更新清洁状态
     */
    @Operation(summary = "批量更新清洁状态")
    @SaCheckPermission("pms:room:updateStatus")
    @Log(title = "批量房间清洁状态更新", businessType = BusinessType.UPDATE)
    @PutMapping("/batch/cleaning")
    public R<Void> batchUpdateCleaningStatus(
            @Parameter(description = "房间ID列表") @RequestBody List<Long> roomIds,
            @Parameter(description = "清洁状态") @RequestParam String cleaningStatus) {
        return toAjax(pmsRoomRoomService.batchUpdateCleaningStatus(roomIds, cleaningStatus));
    }
}
