package org.dromara.workflow.de.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.domain.bo.DeAssigneeBo;
import org.dromara.workflow.de.domain.bo.DeBindingBo;
import org.dromara.workflow.de.domain.bo.DeCandidatesBo;
import org.dromara.workflow.de.domain.bo.DeVariableBo;
import org.dromara.workflow.de.domain.vo.DeBindingVo;
import org.dromara.workflow.de.domain.vo.DeVariableAckVo;
import org.dromara.workflow.de.service.IDeBindingService;
import org.springframework.web.bind.annotation.*;

/**
 * 数字员工绑定 REST 端点。
 *
 * <p>D2 决议 = "/de/*" 前缀。本 controller 共 5 个端点：
 * <ul>
 *   <li>POST /de/binding —— P0 AC-01 写绑定</li>
 *   <li>GET /de/binding/{instanceId} —— P0 AC-02 读回 + AC-03 未绑定读空（负例）</li>
 *   <li>PUT /de/binding/variable —— P1 AC-04 写实例运行态（de.ctx，含 N-04 校验）</li>
 *   <li>PUT /de/binding/candidates —— P1 AC-05 写数字员工候选集（int 数组 ≤ 500）</li>
 *   <li>POST /de/binding/assignee —— P1 AC-06 写待办责任人（flow_user 落点）</li>
 * </ul>
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/binding")
public class DeBindingController extends BaseController {

    private final IDeBindingService deBindingService;

    // ===== P0 端点 =====

    /**
     * 写绑定（28 号 §三 AC-01）。
     */
    @Log(title = "数字员工绑定", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:binding:add")
    @PostMapping
    public R<Long> create(@Valid @RequestBody DeBindingBo bo) {
        return R.ok("绑定成功", deBindingService.createBinding(bo));
    }

    /**
     * D-010 补缺：绑定分页列表（前端 List.vue 数据源；富化 agentName/title）。
     */
    @SaCheckPermission("workflow:de:binding:query")
    @GetMapping("/list")
    public R<PageResult<DeBindingVo>> list(
            @RequestParam(required = false) Long instanceId,
            @RequestParam(required = false) String businessCode,
            @RequestParam(required = false) Long agentId,
            org.dromara.common.mybatis.core.page.PageQuery pageQuery) {
        return R.ok(deBindingService.listBindings(instanceId, businessCode, agentId, pageQuery));
    }

    /**
     * 读绑定（28 号 §三 AC-02 正向 + AC-03 负例判别力自检）。
     */
    @SaCheckPermission("workflow:de:binding:query")
    @GetMapping("/{instanceId}")
    public R<DeBindingVo> getByInstance(@PathVariable Long instanceId) {
        DeBindingVo vo = deBindingService.getBinding(instanceId);
        if (vo == null) {
            // AC-03 负例：未绑定实例返回空集（不是空字符串、不是异常）
            return R.ok(vo);
        }
        return R.ok(vo);
    }

    /**
     * 按实例 ID 解绑（AU-12 应用层暴露端点；D-002 已登记）。
     *
     * <p>调用 {@link IDeBindingService#deleteBindingByInstanceId(Long)} →
     * 底座 {@code flwInstanceBizExtMapper.deleteByInstId(Long)}。
     *
     * <p>该端点<b>不替代</b>底座 controller 删除实例路径 —— 那条路径上
     * 「补调用点」属 D-00X（owner 须裁）。本端点仅为 P0 接口侧提供
     * 显式入口，便于 U-01 等验收脚本独立触发清行。
     *
     * @param instanceId flow_instance.id
     * @return 实际删除行数（0 = 该实例无绑定，判别力保留）
     */
    @Log(title = "数字员工绑定", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:binding:remove")
    @DeleteMapping("/{instanceId}")
    public R<Integer> deleteByInstance(@PathVariable Long instanceId) {
        return R.ok("解绑成功", deBindingService.deleteBindingByInstanceId(instanceId));
    }

    // ===== P1 端点 =====

    /**
     * 写实例运行态（AC-04）。
     * PUT /de/binding/variable
     * 入参：DeVariableBo{instanceId, value(=de.ctx JSON 字符串)}
     * 响应：DeVariableAckVo{instanceId, keyPath="de.ctx", validated=true}
     * 拒收：application/json 解析失败、缺 de.ctx 顶层键、缺 v / binding.agent_id /
     *       binding.business_code / task.step 任一必填键、task.step 枚举越界
     */
    @Log(title = "数字员工绑定", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:binding:edit:variable")
    @PutMapping("/variable")
    public R<DeVariableAckVo> updateVariable(@Valid @RequestBody DeVariableBo bo) {
        return R.ok("variable 已校验并写入", deBindingService.updateVariable(bo));
    }

    /**
     * 写数字员工候选集（AC-05）。
     * PUT /de/binding/candidates
     * 入参：DeCandidatesBo{instanceId, candidates=[int...]}（纯 int 数组）
     * 容量：flow_instance.ext varchar(500) = 500 字符上限
     */
    @Log(title = "数字员工绑定", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:binding:edit:candidates")
    @PutMapping("/candidates")
    public R<Boolean> updateCandidates(@Valid @RequestBody DeCandidatesBo bo) {
        return R.ok("candidates 已写入", deBindingService.updateCandidates(bo));
    }

    /**
     * 写待办责任人（AC-06）。
     * POST /de/binding/assignee
     * 落点：flow_user(type='1' 审批人, processed_by, associated=taskId)
     */
    @Log(title = "数字员工绑定", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:binding:assignee")
    @PostMapping("/assignee")
    public R<Long> assignUser(@Valid @RequestBody DeAssigneeBo bo) {
        return R.ok("责任人绑定成功", deBindingService.assignUser(bo));
    }
}