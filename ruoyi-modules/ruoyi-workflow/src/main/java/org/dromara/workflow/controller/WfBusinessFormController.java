package org.dromara.workflow.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.dromara.workflow.domain.WfBusinessForm;
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
import org.dromara.workflow.domain.vo.WfBusinessFormVo;
import org.dromara.workflow.domain.bo.WfBusinessFormBo;
import org.dromara.workflow.service.IWfBusinessFormService;
import org.dromara.common.mybatis.core.page.TableDataInfo;

/**
 * 发起流程
 *
 * @author may
 * @date 2023-09-16
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/businessForm")
public class WfBusinessFormController extends BaseController {

    private final IWfBusinessFormService wfBusinessFormService;

    /**
     * 查询发起流程列表
     */
    @SaCheckPermission("workflow:businessForm:list")
    @GetMapping("/list")
    public TableDataInfo<WfBusinessFormVo> list(WfBusinessFormBo bo, PageQuery pageQuery) {
        return wfBusinessFormService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出发起流程列表
     */
    @SaCheckPermission("workflow:businessForm:export")
    @Log(title = "发起流程", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(WfBusinessFormBo bo, HttpServletResponse response) {
        List<WfBusinessFormVo> list = wfBusinessFormService.queryList(bo);
        ExcelUtil.exportExcel(list, "发起流程", WfBusinessFormVo.class, response);
    }

    /**
     * 获取发起流程详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("workflow:businessForm:query")
    @GetMapping("/{id}")
    public R<WfBusinessFormVo> getInfo(@NotNull(message = "主键不能为空")
                                       @PathVariable Long id) {
        return R.ok(wfBusinessFormService.queryById(id));
    }

    /**
     * 新增发起流程
     */
    @SaCheckPermission("workflow:businessForm:add")
    @Log(title = "发起流程", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<WfBusinessForm> add(@Validated(AddGroup.class) @RequestBody WfBusinessFormBo bo) {
        return R.ok(wfBusinessFormService.insertByBo(bo));
    }

    /**
     * 修改发起流程
     */
    @SaCheckPermission("workflow:businessForm:edit")
    @Log(title = "发起流程", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<WfBusinessForm> edit(@Validated(EditGroup.class) @RequestBody WfBusinessFormBo bo) {
        return R.ok(wfBusinessFormService.updateByBo(bo));
    }

    /**
     * 删除发起流程
     *
     * @param ids 主键串
     */
    @SaCheckPermission("workflow:businessForm:remove")
    @Log(title = "发起流程", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(wfBusinessFormService.deleteWithValidByIds(List.of(ids)));
    }
}
