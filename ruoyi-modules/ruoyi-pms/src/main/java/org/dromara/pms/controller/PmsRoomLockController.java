package org.dromara.pms.controller;

import java.util.List;
import java.util.Date;

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
import org.dromara.pms.domain.vo.PmsRoomLockVo;
import org.dromara.pms.domain.bo.PmsRoomLockBo;
import org.dromara.pms.service.IPmsRoomLockService;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 房间锁定管理
 *
 * @author xuhf
 * @date 2025-05-28
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/pms/roomLock")
@Tag(name = "房间锁定管理", description = "房间锁定管理")
public class PmsRoomLockController extends BaseController {

    private final IPmsRoomLockService pmsRoomLockService;

    /**
     * 查询房间锁定管理列表
     */
    @Operation(summary = "查询房间锁定管理列表")
    @SaCheckPermission("pms:roomLock:list")
    @GetMapping("/list")
    public TableDataInfo<PmsRoomLockVo> list(PmsRoomLockBo bo, PageQuery pageQuery) {
        return pmsRoomLockService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出房间锁定管理列表
     */
    @Operation(summary = "导出房间锁定管理列表")
    @SaCheckPermission("pms:roomLock:export")
    @Log(title = "房间锁定管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(PmsRoomLockBo bo, HttpServletResponse response) {
        List<PmsRoomLockVo> list = pmsRoomLockService.queryList(bo);
        ExcelUtil.exportExcel(list, "房间锁定管理", PmsRoomLockVo.class, response);
    }

    /**
     * 获取房间锁定管理详细信息
     */
    @Operation(summary = "获取房间锁定管理详细信息")
    @SaCheckPermission("pms:roomLock:query")
    @GetMapping("/{lockId}")
    public R<PmsRoomLockVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long lockId) {
        return R.ok(pmsRoomLockService.queryById(lockId));
    }

    /**
     * 新增房间锁定管理
     */
    @Operation(summary = "新增房间锁定管理")
    @SaCheckPermission("pms:roomLock:add")
    @Log(title = "房间锁定管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PmsRoomLockBo bo) {
        return toAjax(pmsRoomLockService.insertByBo(bo));
    }

    /**
     * 修改房间锁定管理
     */
    @Operation(summary = "修改房间锁定管理")
    @SaCheckPermission("pms:roomLock:edit")
    @Log(title = "房间锁定管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsRoomLockBo bo) {
        return toAjax(pmsRoomLockService.updateByBo(bo));
    }

    /**
     * 删除房间锁定管理
     */
    @Operation(summary = "删除房间锁定管理")
    @SaCheckPermission("pms:roomLock:remove")
    @Log(title = "房间锁定管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{lockIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] lockIds) {
        return toAjax(pmsRoomLockService.deleteWithValidByIds(List.of(lockIds), true));
    }

    /**
     * 根据房间ID查询活跃的锁定记录
     */
    @Operation(summary = "根据房间ID查询活跃的锁定记录")
    @SaCheckPermission("pms:roomLock:query")
    @GetMapping("/room/{roomId}/active")
    public R<List<PmsRoomLockVo>> getActiveLocksByRoom(@PathVariable Long roomId) {
        return R.ok(pmsRoomLockService.queryActiveLocksByRoomId(roomId));
    }

    /**
     * 根据部门ID查询锁定列表
     */
    @Operation(summary = "根据部门ID查询锁定列表")
    @SaCheckPermission("pms:roomLock:query")
    @GetMapping("/dept/{deptId}")
    public R<List<PmsRoomLockVo>> listByDept(@PathVariable Long deptId) {
        return R.ok(pmsRoomLockService.queryByDeptId(deptId));
    }

    /**
     * 根据锁定类型查询锁定列表
     */
    @Operation(summary = "根据锁定类型查询锁定列表")
    @SaCheckPermission("pms:roomLock:query")
    @GetMapping("/type")
    public R<List<PmsRoomLockVo>> listByLockType(
            @Parameter(description = "部门ID") @RequestParam Long deptId,
            @Parameter(description = "锁定类型") @RequestParam String lockType) {
        return R.ok(pmsRoomLockService.queryByLockType(deptId, lockType));
    }

    /**
     * 查询指定时间范围内的锁定记录
     */
    @Operation(summary = "查询指定时间范围内的锁定记录")
    @SaCheckPermission("pms:roomLock:query")
    @GetMapping("/timeRange")
    public R<List<PmsRoomLockVo>> listByTimeRange(
            @Parameter(description = "部门ID") @RequestParam Long deptId,
            @Parameter(description = "开始时间") @RequestParam Date startTime,
            @Parameter(description = "结束时间") @RequestParam Date endTime) {
        return R.ok(pmsRoomLockService.queryByTimeRange(deptId, startTime, endTime));
    }

    /**
     * 检查房间在指定时间段是否有冲突的锁定
     */
    @Operation(summary = "检查房间时间冲突")
    @SaCheckPermission("pms:roomLock:query")
    @GetMapping("/checkConflict")
    public R<Boolean> checkTimeConflict(
            @Parameter(description = "房间ID") @RequestParam Long roomId,
            @Parameter(description = "开始时间") @RequestParam Date startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) Date endTime,
            @Parameter(description = "排除的锁定ID") @RequestParam(required = false) Long excludeId) {
        return R.ok(pmsRoomLockService.checkTimeConflict(roomId, startTime, endTime, excludeId));
    }

    /**
     * 解锁房间
     */
    @Operation(summary = "解锁房间")
    @SaCheckPermission("pms:roomLock:unlock")
    @Log(title = "房间解锁", businessType = BusinessType.UPDATE)
    @PutMapping("/{lockId}/unlock")
    public R<Void> unlockRoom(
            @PathVariable Long lockId,
            @Parameter(description = "解锁原因") @RequestParam String unlockReason) {
        return toAjax(pmsRoomLockService.unlockRoom(lockId, unlockReason));
    }

    /**
     * 批量解锁房间
     */
    @Operation(summary = "批量解锁房间")
    @SaCheckPermission("pms:roomLock:unlock")
    @Log(title = "批量房间解锁", businessType = BusinessType.UPDATE)
    @PutMapping("/batch/unlock")
    public R<Void> batchUnlockRooms(
            @Parameter(description = "锁定ID列表") @RequestBody List<Long> lockIds,
            @Parameter(description = "解锁原因") @RequestParam String unlockReason) {
        return toAjax(pmsRoomLockService.batchUnlockRooms(lockIds, unlockReason));
    }

    /**
     * 自动过期锁定记录
     */
    @Operation(summary = "自动过期锁定记录")
    @SaCheckPermission("pms:roomLock:edit")
    @Log(title = "自动过期锁定", businessType = BusinessType.UPDATE)
    @PutMapping("/autoExpire")
    public R<Void> autoExpireLocks() {
        return toAjax(pmsRoomLockService.autoExpireLocks());
    }
}
