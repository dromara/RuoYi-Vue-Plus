package org.dromara.pms.controller;

import java.util.List;
import java.util.Map;

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
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.pms.domain.vo.PmsRoomTypeVo;
import org.dromara.pms.domain.bo.PmsRoomTypeBo;
import org.dromara.pms.service.IPmsRoomTypeService;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.dromara.common.log.enums.BusinessType;

/**
 * 房型管理
 *
 * @author xuhf
 * @date 2025-05-28
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/pms/roomType")
@Tag(name = "房型管理", description = "房型管理")
public class PmsRoomTypeController extends BaseController {

    private final IPmsRoomTypeService pmsRoomTypeService;

    /**
     * 查询房型管理列表
     */
    @Operation(summary = "查询房型管理列表")
    @SaCheckPermission("pms:roomType:list")
    @GetMapping("/list")
    public TableDataInfo<PmsRoomTypeVo> list(PmsRoomTypeBo bo, PageQuery pageQuery) {
        return pmsRoomTypeService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出房型管理列表
     */
    @Operation(summary = "导出房型管理列表")
    @SaCheckPermission("pms:roomType:export")
    @Log(title = "房型管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(PmsRoomTypeBo bo, HttpServletResponse response) {
        List<PmsRoomTypeVo> list = pmsRoomTypeService.queryList(bo);
        ExcelUtil.exportExcel(list, "房型管理", PmsRoomTypeVo.class, response);
    }

    /**
     * 获取房型管理详细信息
     */
    @Operation(summary = "获取房型管理详细信息")
    @Parameters({
            @Parameter(name = "roomTypeId", description = "房型ID", in = ParameterIn.PATH, required = true)
    })
    @SaCheckPermission("pms:roomType:query")
    @GetMapping("/{roomTypeId}")
    public R<PmsRoomTypeVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long roomTypeId) {
        return R.ok(pmsRoomTypeService.queryById(roomTypeId));
    }

    /**
     * 新增房型管理
     */
    @Operation(summary = "新增房型管理")
    @SaCheckPermission("pms:roomType:add")
    @Log(title = "房型管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PmsRoomTypeBo bo) {
        return toAjax(pmsRoomTypeService.insertByBo(bo));
    }

    /**
     * 修改房型管理
     */
    @Operation(summary = "修改房型管理")
    @SaCheckPermission("pms:roomType:edit")
    @Log(title = "房型管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsRoomTypeBo bo) {
        return toAjax(pmsRoomTypeService.updateByBo(bo));
    }

    /**
     * 删除房型管理
     */
    @Operation(summary = "删除房型管理")
    @Parameters({
            @Parameter(name = "roomTypeIds", description = "房型ID串", in = ParameterIn.PATH, required = true)
    })
    @SaCheckPermission("pms:roomType:remove")
    @Log(title = "房型管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roomTypeIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] roomTypeIds) {
        return toAjax(pmsRoomTypeService.deleteWithValidByIds(List.of(roomTypeIds), true));
    }

    /**
     * 根据部门ID查询房型列表
     */
    @Operation(summary = "根据部门ID查询房型列表")
    @Parameters({
            @Parameter(name = "deptId", description = "部门ID", in = ParameterIn.PATH, required = true)
    })
    @SaCheckPermission("pms:roomType:list")
    @GetMapping("/dept/{deptId}")
    public R<List<PmsRoomTypeVo>> listByDeptId(@PathVariable Long deptId) {
        return R.ok(pmsRoomTypeService.queryByDeptId(deptId));
    }

    /**
     * 校验房型代码唯一性
     */
    @Operation(summary = "校验房型代码唯一性")
    @Parameters({
            @Parameter(name = "typeCode", description = "房型代码", required = true),
            @Parameter(name = "deptId", description = "部门ID", required = true),
            @Parameter(name = "roomTypeId", description = "房型ID（编辑时传入）", required = false)
    })
    @SaCheckPermission("pms:roomType:list")
    @GetMapping("/checkTypeCode")
    public R<Boolean> checkTypeCode(@RequestParam String typeCode,
            @RequestParam Long deptId,
            @RequestParam(required = false) Long roomTypeId) {
        return R.ok(pmsRoomTypeService.checkTypeCodeUnique(typeCode, deptId, roomTypeId));
    }

    /**
     * 获取房型选项列表（用于下拉选择）
     */
    @Operation(summary = "获取房型选项列表")
    @Parameters({
            @Parameter(name = "deptId", description = "部门ID（可选）", required = false)
    })
    @SaCheckPermission("pms:roomType:list")
    @GetMapping("/options")
    public R<List<Map<String, Object>>> getOptions(@RequestParam(required = false) Long deptId) {
        return R.ok(pmsRoomTypeService.getOptions(deptId));
    }

}
