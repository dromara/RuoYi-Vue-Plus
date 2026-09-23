package org.dromara.workflow.de.wd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.wd.domain.bo.DeWdTaskFilterBo;
import org.dromara.workflow.de.wd.domain.vo.DeWdTaskListVo;
import org.dromara.workflow.de.wd.service.IDeWdService;
import org.dromara.workflow.service.IFlwDefinitionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工作台 WD 域 REST 端点（WD-01 / WD-02 / WD-03 / WD-04 / WD-05 / WD-06 共 6 端点）。
 *
 * <p>本 controller 的两类端点：
 * <ul>
 *   <li><b>工作流定义管理</b>（WD-01 / WD-02 / WD-04 = 「工作流定义 CRUD」）：
 *       仅透传底座 {@link IFlwDefinitionService}，并不在 de/wd/ 内重复走 service。
 *       D-016 登记：WD-01 透传 {@code IFlwDefinitionService.saveOrUpdate}（D-016 新增方法）；
 *       WD-02 透传既有 {@code publish(Long)}；
 *       WD-04 透传 {@code IFlwDefinitionService.updateActivityStatus}（D-016 新增方法）。</li>
 *   <li><b>任务查询</b>（WD-03 / WD-05 / WD-06 = 「工作台任务视图」，已存在），
 *       走 {@link IDeWdService}。</li>
 * </ul>
 *
 * <p>路径前缀 {@code /de/wd} 与既有 WD-03/05/06 端点保持一致；按 prefix 派发，
 * 不影响前端既有调用。权限串统一为 {@code workflow:de:wd:*}。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/wd")
public class DeWdController extends BaseController {

    private final IDeWdService deWdService;
    private final IFlwDefinitionService flwDefinitionService;

    // ===== 工作流定义管理（WD-01 / WD-02 / WD-04 · D-016 登记）=====

    /**
     * WD-01：流程定义 saveOrUpdate。
     * POST /de/wd/save-or-update
     * <p>入参：{@link FlowDefinition} 完整实体（id 缺省 ⇒ 新增；非空 ⇒ 更新）。
     * <p>透传 {@link IFlwDefinitionService#saveOrUpdate(FlowDefinition)}（D-016 新增）。
     */
    @Log(title = "工作流定义", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:wd:saveOrUpdate")
    @PostMapping("/save-or-update")
    public R<Boolean> saveOrUpdateDefinition(@Valid @RequestBody FlowDefinition def) {
        return R.ok("流程定义保存成功", flwDefinitionService.saveOrUpdate(def));
    }

    /**
     * WD-02：流程定义发布。
     * POST /de/wd/publish/{definitionId}
     * <p>透传底座既有 {@link IFlwDefinitionService#publish(Long)}（不变底座）。
     */
    @Log(title = "工作流定义", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:wd:publish")
    @PostMapping("/publish/{definitionId}")
    public R<Boolean> publishDefinition(@PathVariable Long definitionId) {
        return R.ok("流程定义发布成功", flwDefinitionService.publish(definitionId));
    }

    /**
     * WD-04：流程定义停用（即改 activity_status）。
     * POST /de/wd/stop/{definitionId}
     * <p>实际语义：{@code activityStatus = 0}（挂起）；激活走 {@code /de/wd/activate/{definitionId}}（如需要，由后续切片补）。
     * <p>透传 {@link IFlwDefinitionService#updateActivityStatus(Long, Integer)}（D-016 新增）。
     */
    @Log(title = "工作流定义", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:wd:stop")
    @PostMapping("/stop/{definitionId}")
    public R<Boolean> stopDefinition(@PathVariable Long definitionId) {
        return R.ok("流程定义已停用", flwDefinitionService.updateActivityStatus(definitionId, 0));
    }

    // ===== 工作台任务视图（WD-03 / WD-05 / WD-06 · 既有）=====

    /** WD-03：列出「AI 执行中」任务。 GET /de/wd/in-progress?userId=xxx */
    @GetMapping("/in-progress")
    public R<List<DeWdTaskListVo>> listInProgress(@RequestParam Long userId) {
        return R.ok(deWdService.listInProgress(userId));
    }

    /** WD-05：工作台任务筛选。 POST /de/wd/filter */
    @PostMapping("/filter")
    public R<List<DeWdTaskListVo>> filterTasks(@Valid @RequestBody DeWdTaskFilterBo bo) {
        return R.ok(deWdService.filterTasks(bo));
    }

    /** WD-06：异常 / 求助 / 审核 / 资源失效 集中「需要我处理」。 GET /de/wd/action-required?userId=xxx */
    @GetMapping("/action-required")
    public R<List<DeWdTaskListVo>> listActionRequired(@RequestParam Long userId) {
        return R.ok(deWdService.listActionRequired(userId));
    }
}