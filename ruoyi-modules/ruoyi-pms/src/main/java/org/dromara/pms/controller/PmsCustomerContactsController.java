package org.dromara.pms.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
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
import org.dromara.pms.domain.bo.PmsCustomerContactsBo;
import org.dromara.pms.domain.vo.PmsCustomerContactsVo;
import org.dromara.pms.service.IPmsCustomerContactsService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 客户联系人Controller
 *
 * @author xuhf
 * @date 2025-05-24
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/pms/contacts")
public class PmsCustomerContactsController extends BaseController {

    private final IPmsCustomerContactsService pmsCustomerContactsService;

    /**
     * 查询客户联系人列表
     */
    @SaCheckPermission("pms:contacts:list")
    @GetMapping("/list")
    public TableDataInfo<PmsCustomerContactsVo> list(PmsCustomerContactsBo bo, PageQuery pageQuery) {
        return pmsCustomerContactsService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出客户联系人列表
     */
    @Log(title = "客户联系人", businessType = BusinessType.EXPORT)
    @SaCheckPermission("pms:contacts:export")
    @PostMapping("/export")
    public void export(PmsCustomerContactsBo bo, HttpServletResponse response) {
        List<PmsCustomerContactsVo> list = pmsCustomerContactsService.queryList(bo);
        ExcelUtil.exportExcel(list, "客户联系人", PmsCustomerContactsVo.class, response);
    }

    /**
     * 获取客户联系人详细信息
     */
    @SaCheckPermission("pms:contacts:query")
    @GetMapping("/{contactId}")
    public R<PmsCustomerContactsVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long contactId) {
        return R.ok(pmsCustomerContactsService.queryById(contactId));
    }

    /**
     * 新增客户联系人
     */
    @Log(title = "客户联系人", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @SaCheckPermission("pms:contacts:add")
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PmsCustomerContactsBo bo) {
        return toAjax(pmsCustomerContactsService.insertByBo(bo));
    }

    /**
     * 修改客户联系人
     */
    @Log(title = "客户联系人", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @SaCheckPermission("pms:contacts:edit")
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PmsCustomerContactsBo bo) {
        return toAjax(pmsCustomerContactsService.updateByBo(bo));
    }

    /**
     * 删除客户联系人
     */
    @Log(title = "客户联系人", businessType = BusinessType.DELETE)
    @SaCheckPermission("pms:contacts:remove")
    @DeleteMapping("/{contactIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] contactIds) {
        return toAjax(pmsCustomerContactsService.deleteWithValidByIds(List.of(contactIds), true));
    }

    /**
     * 保存联系人标签关联
     *
     * @param contactId 联系人ID
     * @param tagIds    标签ID列表
     */
    @Log(title = "客户联系人标签", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @SaCheckPermission("pms:contacts:edit")
    @PostMapping("/{contactId}/tags")
    public R<Void> saveContactTags(@PathVariable Long contactId, @RequestBody List<Long> tagIds) {
        return toAjax(pmsCustomerContactsService.saveContactTags(contactId, tagIds));
    }

    /**
     * 查询联系人详情包含标签信息
     *
     * @param contactId 联系人ID
     */
    @SaCheckPermission("pms:contacts:query")
    @GetMapping("/{contactId}/withTags")
    public R<PmsCustomerContactsVo> getInfoWithTags(@NotNull(message = "主键不能为空") @PathVariable Long contactId) {
        return R.ok(pmsCustomerContactsService.queryByIdWithTags(contactId));
    }
}
