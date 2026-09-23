package org.dromara.workflow.de.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.hr.domain.bo.DeHrThreeTierBo;
import org.dromara.workflow.de.hr.domain.vo.DeHrThreeTierVo;
import org.dromara.workflow.de.hr.service.IDeHrFourService;
import org.springframework.web.bind.annotation.*;

/**
 * HR-04 三分位落点 REST 端点（2026-09-23）。
 *
 * <p>路径：{@code /de/hr/three-tier/*} —— 与现有 HR-03/05/06 端点
 * （{@link DeHrController}）路径不重叠。
 *
 * <p>真源：flow_user(type=1/2/3 + processed_by + associated=taskId)
 * + flow_instance.variable.de.ctx.hr.three_tier JSON 分段。
 * 落点策略 = 复用既有表 + JSON 分段（owner 全量自决授权下走 default），D-016 登记。
 *
 * <p>端点：
 * <ul>
 *   <li>GET  /de/hr/three-tier/{instanceId}?taskId=&lt;id&gt; —— 读三分位</li>
 *   <li>PUT  /de/hr/three-tier                 —— 写三分位（落 flow_user + 写 variable）</li>
 * </ul>
 *
 * @author AIP
 * @since 2026-09-23
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/hr/three-tier")
public class DeHrFourController extends BaseController {

    private final IDeHrFourService deHrFourService;

    /** 读三分位 */
    @SaCheckPermission("workflow:de:hr:query")
    @GetMapping("/{instanceId}")
    public R<DeHrThreeTierVo> getThreeTier(@PathVariable Long instanceId,
                                           @RequestParam(required = false) Long taskId) {
        return R.ok(deHrFourService.getThreeTier(instanceId, taskId));
    }

    /** 写三分位（flow_user 三行 + variable JSON 段） */
    @Log(title = "HR 三分位", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:hr:edit")
    @PutMapping
    public R<DeHrThreeTierVo> upsert(@Valid @RequestBody DeHrThreeTierBo bo) {
        return R.ok("三分位已落点", deHrFourService.upsertThreeTier(bo));
    }
}