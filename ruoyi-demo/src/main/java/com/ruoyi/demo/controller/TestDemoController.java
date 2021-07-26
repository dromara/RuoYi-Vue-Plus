package com.ruoyi.demo.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.demo.domain.bo.TestDemoBo;
import com.ruoyi.demo.domain.vo.TestDemoVo;
import com.ruoyi.demo.service.ITestDemoService;
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
 * 测试单表Controller
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Validated
@Api(value = "测试单表控制器", tags = {"测试单表管理"})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
@RequestMapping("/demo/demo")
public class TestDemoController extends BaseController {

    private final ITestDemoService iTestDemoService;

    /**
     * 查询测试单表列表
     */
    @ApiOperation("查询测试单表列表")
    @PreAuthorize("@ss.hasPermi('demo:demo:list')")
    @GetMapping("/list")
    public TableDataInfo<TestDemoVo> list(@Validated TestDemoBo bo) {
        return iTestDemoService.queryPageList(bo);
    }

	/**
	 * 自定义分页查询
	 */
	@ApiOperation("自定义分页查询")
	@PreAuthorize("@ss.hasPermi('demo:demo:list')")
	@GetMapping("/page")
	public TableDataInfo<TestDemoVo> page(@Validated TestDemoBo bo) {
		return iTestDemoService.customPageList(bo);
	}

	/**
     * 导出测试单表列表
     */
    @ApiOperation("导出测试单表列表")
    @PreAuthorize("@ss.hasPermi('demo:demo:export')")
    @Log(title = "测试单表", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult<TestDemoVo> export(@Validated TestDemoBo bo) {
        List<TestDemoVo> list = iTestDemoService.queryList(bo);
        ExcelUtil<TestDemoVo> util = new ExcelUtil<TestDemoVo>(TestDemoVo.class);
        return util.exportExcel(list, "测试单表");
    }

    /**
     * 获取测试单表详细信息
     */
    @ApiOperation("获取测试单表详细信息")
    @PreAuthorize("@ss.hasPermi('demo:demo:query')")
    @GetMapping("/{id}")
    public AjaxResult<TestDemoVo> getInfo(@NotNull(message = "主键不能为空")
                                                  @PathVariable("id") Long id) {
        return AjaxResult.success(iTestDemoService.queryById(id));
    }

    /**
     * 新增测试单表
     */
    @ApiOperation("新增测试单表")
    @PreAuthorize("@ss.hasPermi('demo:demo:add')")
    @Log(title = "测试单表", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping()
    public AjaxResult<Void> add(@Validated(AddGroup.class) @RequestBody TestDemoBo bo) {
        return toAjax(iTestDemoService.insertByBo(bo) ? 1 : 0);
    }

    /**
     * 修改测试单表
     */
    @ApiOperation("修改测试单表")
    @PreAuthorize("@ss.hasPermi('demo:demo:edit')")
    @Log(title = "测试单表", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public AjaxResult<Void> edit(@Validated(EditGroup.class) @RequestBody TestDemoBo bo) {
        return toAjax(iTestDemoService.updateByBo(bo) ? 1 : 0);
    }

    /**
     * 删除测试单表
     */
    @ApiOperation("删除测试单表")
    @PreAuthorize("@ss.hasPermi('demo:demo:remove')")
    @Log(title = "测试单表" , businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult<Void> remove(@NotEmpty(message = "主键不能为空")
                                       @PathVariable Long[] ids) {
        return toAjax(iTestDemoService.deleteWithValidByIds(Arrays.asList(ids), true) ? 1 : 0);
    }
}
