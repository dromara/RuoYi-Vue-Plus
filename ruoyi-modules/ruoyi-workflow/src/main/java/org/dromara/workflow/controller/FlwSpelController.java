package org.dromara.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.domain.bo.FlowSpelBo;
import org.dromara.workflow.domain.vo.FlowSpelVo;
import org.dromara.workflow.service.IFlwSpelService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程 SpEL 表达式定义控制器，负责表达式规则的增删改查。
 *
 * @author Michelle.Chung
 * @date 2025-07-04
 */
@ConditionalOnEnable
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/spel")
public class FlwSpelController extends BaseController {

    private final IFlwSpelService flwSpelService;

    /**
     * 分页查询流程 SpEL 表达式定义列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 表达式分页数据
     */
    @SaCheckPermission("workflow:spel:list")
    @GetMapping("/list")
    public R<PageResult<FlowSpelVo>> list(FlowSpelBo bo, PageQuery pageQuery) {
        return R.ok(flwSpelService.queryPageList(bo, pageQuery));
    }

    /**
     * 获取流程 SpEL 表达式定义详情。
     *
     * @param id 主键
     * @return 表达式详情
     */
    @SaCheckPermission("workflow:spel:query")
    @GetMapping("/{id}")
    public R<FlowSpelVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(flwSpelService.queryById(id));
    }

    /**
     * /**
     * 新增流程 SpEL 表达式定义。
     *
     * @param bo 表达式信息
     * @return 操作结果
     */
    @SaCheckPermission("workflow:spel:add")
    @Log(title = "流程spel表达式定义", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody FlowSpelBo bo) {
        return toAjax(flwSpelService.insertByBo(bo));
    }

    /**
     * 修改流程 SpEL 表达式定义。
     *
     * @param bo 表达式信息
     * @return 操作结果
     */
    @SaCheckPermission("workflow:spel:edit")
    @Log(title = "流程spel表达式定义", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody FlowSpelBo bo) {
        return toAjax(flwSpelService.updateByBo(bo));
    }

    /**
     * 批量删除流程 SpEL 表达式定义。
     *
     * @param ids 主键串
     * @return 操作结果
     */
    @SaCheckPermission("workflow:spel:remove")
    @Log(title = "流程spel表达式定义", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable Long[] ids) {
        return toAjax(flwSpelService.deleteWithValidByIds(List.of(ids), true));
    }
}
