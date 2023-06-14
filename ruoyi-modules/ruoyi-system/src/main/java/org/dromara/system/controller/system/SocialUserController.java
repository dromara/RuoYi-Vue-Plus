package org.dromara.system.controller.system;

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
import org.dromara.system.domain.vo.SocialUserVo;
import org.dromara.system.domain.bo.SocialUserBo;
import org.dromara.system.service.ISocialUserService;
import org.dromara.common.mybatis.core.page.TableDataInfo;

/**
 * 社会化关系
 *
 * @author thiszhc
 * @date 2023-06-12
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/socialUser")
public class SocialUserController extends BaseController {

    private final ISocialUserService socialUserService;

    /**
     * 查询社会化关系列表
     */
    @SaCheckPermission("system:user:list")
    @GetMapping("/list")
    public TableDataInfo<SocialUserVo> list(SocialUserBo bo, PageQuery pageQuery) {
        return socialUserService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出社会化关系列表
     */
    @SaCheckPermission("system:user:export")
    @Log(title = "社会化关系", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SocialUserBo bo, HttpServletResponse response) {
        List<SocialUserVo> list = socialUserService.queryList(bo);
        ExcelUtil.exportExcel(list, "社会化关系", SocialUserVo.class, response);
    }

    /**
     * 获取社会化关系详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:user:query")
    @GetMapping("/{id}")
    public R<SocialUserVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(socialUserService.queryById(id));
    }

    /**
     * 新增社会化关系
     */
    @SaCheckPermission("system:user:add")
    @Log(title = "社会化关系", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody SocialUserBo bo) {
        return toAjax(socialUserService.insertByBo(bo));
    }

    /**
     * 修改社会化关系
     */
    @SaCheckPermission("system:user:edit")
    @Log(title = "社会化关系", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody SocialUserBo bo) {
        return toAjax(socialUserService.updateByBo(bo));
    }

    /**
     * 删除社会化关系
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:user:remove")
    @Log(title = "社会化关系", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(socialUserService.deleteWithValidByIds(List.of(ids), true));
    }
}
