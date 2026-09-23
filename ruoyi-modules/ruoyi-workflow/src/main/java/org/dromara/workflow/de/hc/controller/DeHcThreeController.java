package org.dromara.workflow.de.hc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.hc.domain.bo.DeHcThreeCategoryBo;
import org.dromara.workflow.de.hc.domain.vo.DeHcThreeCategoryVo;
import org.dromara.workflow.de.hc.service.IDeHcThreeService;
import org.springframework.web.bind.annotation.*;

/**
 * HC-03 三分类位落点 REST 端点（2026-09-23）。
 *
 * <p>路径：{@code /de/hc/three-category/*} —— 与现有 HC-04/05/06/08 端点
 * （{@link DeHcController}）路径不重叠。
 *
 * <p>真源：flow_node.permission_flag（varchar 200 + @@ 分隔 + : 前缀；DDL 自带「多权限」注释）。
 * 落点策略 = 复用 flow_node.permission_flag 单字段（owner 全量自决授权下走 default），D-017 登记。
 *
 * <p>端点：
 * <ul>
 *   <li>GET /de/hc/three-category/{nodeId} —— 读三分类位</li>
 *   <li>PUT /de/hc/three-category           —— 写三分类位（拼装 permission_flag）</li>
 * </ul>
 *
 * @author AIP
 * @since 2026-09-23
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/hc/three-category")
public class DeHcThreeController extends BaseController {

    private final IDeHcThreeService deHcThreeService;

    @SaCheckPermission("workflow:de:hc:query")
    @GetMapping("/{nodeId}")
    public R<DeHcThreeCategoryVo> getThreeCategory(@PathVariable Long nodeId) {
        return R.ok(deHcThreeService.getThreeCategory(nodeId));
    }

    @Log(title = "HC 三分类位", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:hc:edit")
    @PutMapping
    public R<DeHcThreeCategoryVo> upsert(@Valid @RequestBody DeHcThreeCategoryBo bo) {
        return R.ok("三分类位已落点", deHcThreeService.upsertThreeCategory(bo));
    }
}