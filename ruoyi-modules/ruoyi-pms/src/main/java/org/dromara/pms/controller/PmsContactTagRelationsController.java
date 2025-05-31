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
import org.dromara.pms.domain.vo.PmsContactTagRelationsVo;
import org.dromara.pms.domain.bo.PmsContactTagRelationsBo;
import org.dromara.pms.service.IPmsContactTagRelationsService;
import org.dromara.common.mybatis.core.page.TableDataInfo;

/**
 * 联系人标签关联
 *
 * @author xuhf
 * @date 2025-05-24
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/pms/contactTagRelations")
public class PmsContactTagRelationsController extends BaseController {

    private final IPmsContactTagRelationsService pmsContactTagRelationsService;

    /**
     * 查询联系人标签关联列表
     */
    @SaCheckPermission("pms:contactTagRelations:list")
    @GetMapping("/list")
    public TableDataInfo<PmsContactTagRelationsVo> list(PmsContactTagRelationsBo bo, PageQuery pageQuery) {
        return pmsContactTagRelationsService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出联系人标签关联列表
     */
    @SaCheckPermission("pms:contactTagRelations:export")
    @Log(title = "联系人标签关联", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(PmsContactTagRelationsBo bo, HttpServletResponse response) {
        List<PmsContactTagRelationsVo> list = pmsContactTagRelationsService.queryList(bo);
        ExcelUtil.exportExcel(list, "联系人标签关联", PmsContactTagRelationsVo.class, response);
    }

    /**
     * 获取联系人标签关联详细信息
     *
     * @param relationId 主键
     */
    @SaCheckPermission("pms:contactTagRelations:query")
    @GetMapping("/{relationId}")
    public R<PmsContactTagRelationsVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long relationId) {
        return R.ok(pmsContactTagRelationsService.queryById(relationId));
    }

    /**
     * 新增联系人标签关联
     */
    @SaCheckPermission("pms:contactTagRelations:add")
    @Log(title = "联系人标签关联", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PmsContactTagRelationsBo bo) {
        return toAjax(pmsContactTagRelationsService.insertByBo(bo));
    }

    /**
     * 修改联系人标签关联
     */
    @SaCheckPermission("pms:contactTagRelations:edit")
    @Log(title = "联系人标签关联", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsContactTagRelationsBo bo) {
        return toAjax(pmsContactTagRelationsService.updateByBo(bo));
    }

    /**
     * 删除联系人标签关联
     *
     * @param relationIds 主键串
     */
    @SaCheckPermission("pms:contactTagRelations:remove")
    @Log(title = "联系人标签关联", businessType = BusinessType.DELETE)
    @DeleteMapping("/{relationIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] relationIds) {
        return toAjax(pmsContactTagRelationsService.deleteWithValidByIds(List.of(relationIds), true));
    }

    /**
     * 根据联系人ID查询标签关联列表
     *
     * @param contactId 联系人ID
     */
    @SaCheckPermission("pms:contactTagRelations:list")
    @GetMapping("/contact/{contactId}")
    public R<List<PmsContactTagRelationsVo>> getRelationsByContactId(@PathVariable Long contactId) {
        List<PmsContactTagRelationsVo> list = pmsContactTagRelationsService.queryRelationsByContactId(contactId);
        return R.ok(list);
    }

    /**
     * 根据标签ID查询关联的联系人列表
     *
     * @param tagId 标签ID
     */
    @SaCheckPermission("pms:contactTagRelations:list")
    @GetMapping("/tag/{tagId}")
    public R<List<PmsContactTagRelationsVo>> getRelationsByTagId(@PathVariable Long tagId) {
        List<PmsContactTagRelationsVo> list = pmsContactTagRelationsService.queryRelationsByTagId(tagId);
        return R.ok(list);
    }

    /**
     * 批量保存联系人标签关联
     *
     * @param contactId 联系人ID
     * @param tagIds    标签ID列表
     */
    @SaCheckPermission("pms:contactTagRelations:edit")
    @Log(title = "联系人标签关联", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/batch/{contactId}")
    public R<Void> batchSaveRelations(@PathVariable Long contactId, @RequestBody List<Long> tagIds) {
        return toAjax(pmsContactTagRelationsService.batchSaveRelations(contactId, tagIds));
    }

    /**
     * 根据联系人ID删除所有标签关联
     *
     * @param contactId 联系人ID
     */
    @SaCheckPermission("pms:contactTagRelations:remove")
    @Log(title = "联系人标签关联", businessType = BusinessType.DELETE)
    @DeleteMapping("/contact/{contactId}")
    public R<Void> deleteByContactId(@PathVariable Long contactId) {
        return toAjax(pmsContactTagRelationsService.deleteByContactId(contactId));
    }

    /**
     * 根据标签ID删除所有关联
     *
     * @param tagId 标签ID
     */
    @SaCheckPermission("pms:contactTagRelations:remove")
    @Log(title = "联系人标签关联", businessType = BusinessType.DELETE)
    @DeleteMapping("/tag/{tagId}")
    public R<Void> deleteByTagId(@PathVariable Long tagId) {
        return toAjax(pmsContactTagRelationsService.deleteByTagId(tagId));
    }

    /**
     * 检查联系人和标签的关联是否存在
     *
     * @param contactId 联系人ID
     * @param tagId     标签ID
     */
    @SaCheckPermission("pms:contactTagRelations:query")
    @GetMapping("/exists/{contactId}/{tagId}")
    public R<Boolean> existsRelation(@PathVariable Long contactId, @PathVariable Long tagId) {
        Boolean exists = pmsContactTagRelationsService.existsRelation(contactId, tagId);
        return R.ok(exists);
    }
}
