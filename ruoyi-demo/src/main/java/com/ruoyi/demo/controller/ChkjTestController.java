package com.ruoyi.demo.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.demo.bo.ChkjTestAddBo;
import com.ruoyi.demo.bo.ChkjTestEditBo;
import com.ruoyi.demo.bo.ChkjTestQueryBo;
import com.ruoyi.demo.service.IChkjTestService;
import com.ruoyi.demo.vo.ChkjTestVo;
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
 * 测试Controller
 * 
 * @author Lion Li
 * @date 2021-05-14
 */
@Api(value = "测试控制器", tags = {"测试管理"})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@RestController
@RequestMapping("/demo/test")
public class ChkjTestController extends BaseController {

    private final IChkjTestService iChkjTestService;

    /**
     * 查询测试列表
     */
    @ApiOperation("查询测试列表")
    @PreAuthorize("@ss.hasPermi('demo:test:list')")
    @GetMapping("/list")
    public TableDataInfo<ChkjTestVo> list(@Validated ChkjTestQueryBo bo) {
        return iChkjTestService.queryPageList(bo);
    }

    /**
     * 导出测试列表
     */
    @ApiOperation("导出测试列表")
    @PreAuthorize("@ss.hasPermi('demo:test:export')")
    @Log(title = "测试", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult<ChkjTestVo> export(@Validated ChkjTestQueryBo bo) {
        List<ChkjTestVo> list = iChkjTestService.queryList(bo);
        ExcelUtil<ChkjTestVo> util = new ExcelUtil<ChkjTestVo>(ChkjTestVo.class);
        return util.exportExcel(list, "测试");
    }

    /**
     * 获取测试详细信息
     */
    @ApiOperation("获取测试详细信息")
    @PreAuthorize("@ss.hasPermi('demo:test:query')")
    @GetMapping("/{id}")
    public AjaxResult<ChkjTestVo> getInfo(@NotNull(message = "主键不能为空")
                                                  @PathVariable("id") Long id) {
        return AjaxResult.success(iChkjTestService.queryById(id));
    }

    /**
     * 新增测试
     */
    @ApiOperation("新增测试")
    @PreAuthorize("@ss.hasPermi('demo:test:add')")
    @Log(title = "测试", businessType = BusinessType.INSERT)
    @PostMapping()
    public AjaxResult<Void> add(@Validated @RequestBody ChkjTestAddBo bo) {
        return toAjax(iChkjTestService.insertByAddBo(bo) ? 1 : 0);
    }

    /**
     * 修改测试
     */
    @ApiOperation("修改测试")
    @PreAuthorize("@ss.hasPermi('demo:test:edit')")
    @Log(title = "测试", businessType = BusinessType.UPDATE)
    @PutMapping()
    public AjaxResult<Void> edit(@Validated @RequestBody ChkjTestEditBo bo) {
        return toAjax(iChkjTestService.updateByEditBo(bo) ? 1 : 0);
    }

    /**
     * 删除测试
     */
    @ApiOperation("删除测试")
    @PreAuthorize("@ss.hasPermi('demo:test:remove')")
    @Log(title = "测试" , businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult<Void> remove(@NotEmpty(message = "主键不能为空")
                                       @PathVariable Long[] ids) {
        return toAjax(iChkjTestService.deleteWithValidByIds(Arrays.asList(ids), true) ? 1 : 0);
    }
}
