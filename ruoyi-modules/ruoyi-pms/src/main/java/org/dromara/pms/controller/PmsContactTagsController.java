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
import org.dromara.pms.domain.vo.PmsContactTagsVo;
import org.dromara.pms.domain.bo.PmsContactTagsBo;
import org.dromara.pms.service.IPmsContactTagsService;
import org.dromara.common.mybatis.core.page.TableDataInfo;

/**
 * 联系人标签
 *
 * @author xuhf
 * @date 2025-05-24
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/pms/contactTags")
public class PmsContactTagsController extends BaseController {

    private final IPmsContactTagsService pmsContactTagsService;

    /**
     * 查询联系人标签列表
     */
    @SaCheckPermission("pms:contactTags:list")
    @GetMapping("/list")
    public TableDataInfo<PmsContactTagsVo> list(PmsContactTagsBo bo, PageQuery pageQuery) {
        return pmsContactTagsService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出联系人标签列表
     */
    @SaCheckPermission("pms:contactTags:export")
    @Log(title = "联系人标签", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(PmsContactTagsBo bo, HttpServletResponse response) {
        List<PmsContactTagsVo> list = pmsContactTagsService.queryList(bo);
        ExcelUtil.exportExcel(list, "联系人标签", PmsContactTagsVo.class, response);
    }

    /**
     * 获取联系人标签详细信息
     *
     * @param tagId 主键
     */
    @SaCheckPermission("pms:contactTags:query")
    @GetMapping("/{tagId}")
    public R<PmsContactTagsVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long tagId) {
        return R.ok(pmsContactTagsService.queryById(tagId));
    }

    /**
     * 新增联系人标签
     */
    @SaCheckPermission("pms:contactTags:add")
    @Log(title = "联系人标签", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PmsContactTagsBo bo) {
        return toAjax(pmsContactTagsService.insertByBo(bo));
    }

    /**
     * 修改联系人标签
     */
    @SaCheckPermission("pms:contactTags:edit")
    @Log(title = "联系人标签", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsContactTagsBo bo) {
        return toAjax(pmsContactTagsService.updateByBo(bo));
    }

    /**
     * 删除联系人标签
     *
     * @param tagIds 主键串
     */
    @SaCheckPermission("pms:contactTags:remove")
    @Log(title = "联系人标签", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tagIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] tagIds) {
        return toAjax(pmsContactTagsService.deleteWithValidByIds(List.of(tagIds), true));
    }

    /**
     * 根据分类查询标签列表
     *
     * @param category 标签分类
     */
    @SaCheckPermission("pms:contactTags:list")
    @GetMapping("/category/{category}")
    public R<List<PmsContactTagsVo>> getTagsByCategory(@PathVariable String category) {
        List<PmsContactTagsVo> list = pmsContactTagsService.queryTagsByCategory(category);
        return R.ok(list);
    }

    /**
     * 查询可用的标签分类列表
     */
    @SaCheckPermission("pms:contactTags:list")
    @GetMapping("/categories")
    public R<List<String>> getCategories() {
        List<String> categories = pmsContactTagsService.queryDistinctCategories();
        return R.ok(categories);
    }

    /**
     * 根据部门ID查询标签列表
     *
     * @param deptId 部门ID
     */
    @SaCheckPermission("pms:contactTags:list")
    @GetMapping("/dept/{deptId}")
    public R<List<PmsContactTagsVo>> getTagsByDeptId(@PathVariable Long deptId) {
        List<PmsContactTagsVo> list = pmsContactTagsService.queryTagsByDeptId(deptId);
        return R.ok(list);
    }

    /**
     * 查询所有可用标签（用于下拉选择）
     */
    @SaCheckPermission("pms:contactTags:list")
    @GetMapping("/available")
    public R<List<PmsContactTagsVo>> getAllAvailableTags() {
        List<PmsContactTagsVo> list = pmsContactTagsService.queryAllAvailableTags();
        return R.ok(list);
    }
}
