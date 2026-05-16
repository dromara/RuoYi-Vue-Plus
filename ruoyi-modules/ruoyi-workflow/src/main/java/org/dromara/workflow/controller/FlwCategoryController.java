package org.dromara.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.lang.tree.Tree;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.excel.utils.ExcelBuilder;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.constant.FlowConstant;
import org.dromara.workflow.domain.bo.FlowCategoryBo;
import org.dromara.workflow.domain.vo.FlowCategoryVo;
import org.dromara.workflow.service.IFlwCategoryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程分类
 *
 * @author may
 */
@ConditionalOnEnable
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/category")
public class FlwCategoryController extends BaseController {

    private final IFlwCategoryService flwCategoryService;

    /**
     * 查询流程分类列表。
     *
     * @param bo 查询条件
     * @return 流程分类列表
     */
    @SaCheckPermission("workflow:category:list")
    @GetMapping("/list")
    public R<List<FlowCategoryVo>> list(FlowCategoryBo bo) {
        List<FlowCategoryVo> list = flwCategoryService.queryList(bo);
        return R.ok(list);
    }

    /**
     * 导出流程分类列表。
     *
     * @param bo 查询条件
     * @param response 响应流
     */
    @SaCheckPermission("workflow:category:export")
    @Log(title = "流程分类", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(FlowCategoryBo bo, HttpServletResponse response) {
        List<FlowCategoryVo> list = flwCategoryService.queryList(bo);
        ExcelBuilder.of(list, FlowCategoryVo.class).sheetName("流程分类").toResponse(response);
    }

    /**
     * 获取单个流程分类详情。
     *
     * @param categoryId 主键
     * @return 流程分类详情
     */
    @SaCheckPermission("workflow:category:query")
    @GetMapping("/{categoryId}")
    public R<FlowCategoryVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long categoryId) {
        return R.ok(flwCategoryService.queryById(categoryId));
    }

    /**
     * 新增流程分类，并校验分类名称唯一性。
     *
     * @param category 分类信息
     * @return 操作结果
     */
    @SaCheckPermission("workflow:category:add")
    @Log(title = "流程分类", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody FlowCategoryBo category) {
        if (!flwCategoryService.checkCategoryNameUnique(category)) {
            return R.fail("新增流程分类'" + category.getCategoryName() + "'失败，流程分类名称已存在");
        }
        return toAjax(flwCategoryService.insertByBo(category));
    }

    /**
     * 修改流程分类，并校验名称唯一及父子关系合法性。
     *
     * @param category 分类信息
     * @return 操作结果
     */
    @SaCheckPermission("workflow:category:edit")
    @Log(title = "流程分类", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody FlowCategoryBo category) {
        Long categoryId = category.getCategoryId();
        if (!flwCategoryService.checkCategoryNameUnique(category)) {
            return R.fail("修改流程分类'" + category.getCategoryName() + "'失败，流程分类名称已存在");
        } else if (category.getParentId().equals(categoryId)) {
            return R.fail("修改流程分类'" + category.getCategoryName() + "'失败，上级流程分类不能是自己");
        }
        return toAjax(flwCategoryService.updateByBo(category));
    }

    /**
     * 删除流程分类，删除前校验默认分类、子节点和绑定流程定义。
     *
     * @param categoryId 主键
     * @return 操作结果
     */
    @SaCheckPermission("workflow:category:remove")
    @Log(title = "流程分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryId}")
    public R<Void> remove(@PathVariable Long categoryId) {
        if (FlowConstant.FLOW_CATEGORY_ID.equals(categoryId)) {
            return R.warn("默认流程分类,不允许删除");
        }
        if (flwCategoryService.hasChildByCategoryId(categoryId)) {
            return R.warn("存在下级流程分类,不允许删除");
        }
        if (flwCategoryService.checkCategoryExistDefinition(categoryId)) {
            return R.warn("流程分类存在流程定义,不允许删除");
        }
        return toAjax(flwCategoryService.deleteWithValidById(categoryId));
    }

    /**
     * 获取流程分类树列表，用于流程定义选择分类节点。
     *
     * @param categoryBo 流程分类
     * @return 流程分类树
     */
    @GetMapping("/categoryTree")
    public R<List<Tree<String>>> categoryTree(FlowCategoryBo categoryBo) {
        return R.ok(flwCategoryService.selectCategoryTreeList(categoryBo));
    }

}
