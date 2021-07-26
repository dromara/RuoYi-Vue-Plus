package com.ruoyi.demo.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.demo.domain.bo.TestTreeBo;
import com.ruoyi.demo.domain.vo.TestTreeVo;
import com.ruoyi.demo.service.ITestTreeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * 测试树表Controller
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Validated
@Api(value = "测试树表控制器", tags = {"测试树表管理"})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
@RequestMapping("/demo/tree")
public class TestTreeController extends BaseController {

    private final ITestTreeService iTestTreeService;

    /**
     * 查询测试树表列表
     */
    @ApiOperation("查询测试树表列表")
    @PreAuthorize("@ss.hasPermi('demo:tree:list')")
    @GetMapping("/list")
    public AjaxResult<List<TestTreeVo>> list(@Validated TestTreeBo bo) {
        List<TestTreeVo> list = iTestTreeService.queryList(bo);
        return AjaxResult.success(list);
    }

    /**
     * 导出测试树表列表
     */
    @ApiOperation("导出测试树表列表")
    @PreAuthorize("@ss.hasPermi('demo:tree:export')")
    @Log(title = "测试树表", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult<TestTreeVo> export(@Validated TestTreeBo bo) {
        List<TestTreeVo> list = iTestTreeService.queryList(bo);
        ExcelUtil<TestTreeVo> util = new ExcelUtil<TestTreeVo>(TestTreeVo.class);
        return util.exportExcel(list, "测试树表");
    }

    /**
     * 获取测试树表详细信息
     */
    @ApiOperation("获取测试树表详细信息")
    @PreAuthorize("@ss.hasPermi('demo:tree:query')")
    @GetMapping("/{id}")
    public AjaxResult<TestTreeVo> getInfo(@NotNull(message = "主键不能为空")
                                                  @PathVariable("id") Long id) {
        return AjaxResult.success(iTestTreeService.queryById(id));
    }

    /**
     * 新增测试树表
     */
    @ApiOperation("新增测试树表")
    @PreAuthorize("@ss.hasPermi('demo:tree:add')")
    @Log(title = "测试树表", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping()
    public AjaxResult<Void> add(@Validated(AddGroup.class) @RequestBody TestTreeBo bo) {
        return toAjax(iTestTreeService.insertByBo(bo) ? 1 : 0);
    }

    /**
     * 修改测试树表
     */
    @ApiOperation("修改测试树表")
    @PreAuthorize("@ss.hasPermi('demo:tree:edit')")
    @Log(title = "测试树表", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public AjaxResult<Void> edit(@Validated(EditGroup.class) @RequestBody TestTreeBo bo) {
        return toAjax(iTestTreeService.updateByBo(bo) ? 1 : 0);
    }

    /**
     * 删除测试树表
     */
    @ApiOperation("删除测试树表")
    @PreAuthorize("@ss.hasPermi('demo:tree:remove')")
    @Log(title = "测试树表" , businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult<Void> remove(@NotEmpty(message = "主键不能为空")
                                       @PathVariable Long[] ids) {
        return toAjax(iTestTreeService.deleteWithValidByIds(Arrays.asList(ids), true) ? 1 : 0);
    }
}
