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
import org.dromara.workflow.de.domain.SaiAgentConversationRecord;
import org.dromara.workflow.de.domain.bo.SaiAgentConversationBo;
import org.dromara.workflow.de.domain.vo.SaiAgentConversationVo;
import org.dromara.workflow.de.service.IDeSaiConvService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 智能体对话 REST 端点（sai_agent_conversation + sai_agent_conversation_record）。
 *
 * <p>本 controller 共 7 个端点（与 {@code IDeSaiConvService} 一一对应）：
 * <ul>
 *   <li>POST /de/sai-conv —— 新增对话</li>
 *   <li>PUT /de/sai-conv —— 修改对话</li>
 *   <li>DELETE /de/sai-conv/{ids} —— 删除对话（逗号分隔）</li>
 *   <li>GET /de/sai-conv/{id} —— 单条详情</li>
 *   <li>GET /de/sai-conv/list —— 分页列表（PageQuery 参数）</li>
 *   <li>GET /de/sai-conv/by-agent/{agentId} —— 按 agent_id 查询</li>
 *   <li>GET /de/sai-conv/record/{recordId} —— 按 record_id 查 sai_agent_conversation_record 单条</li>
 * </ul>
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sai-conv")
public class DeSaiConvController extends BaseController {

    private final IDeSaiConvService deSaiConvService;

    /**
     * 新增对话（sai_agent_conversation）。
     */
    @Log(title = "智能体对话", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:saiconv:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody SaiAgentConversationBo bo) {
        return R.ok("对话已新建", deSaiConvService.add(bo));
    }

    /**
     * 修改对话（sai_agent_conversation）。
     */
    @Log(title = "智能体对话", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:saiconv:edit")
    @PutMapping
    public R<Boolean> edit(@Valid @RequestBody SaiAgentConversationBo bo) {
        return R.ok("对话已更新", deSaiConvService.edit(bo));
    }

    /**
     * 删除对话（sai_agent_conversation；逗号分隔 id）。
     *
     * @param ids 主键集合（逗号分隔，例如 {@code 1,2,3}）
     */
    @Log(title = "智能体对话", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:saiconv:remove")
    @DeleteMapping("/{ids}")
    public R<Boolean> remove(@PathVariable Long[] ids) {
        return R.ok("对话已删除", deSaiConvService.remove(Arrays.asList(ids)));
    }

    /**
     * 单条详情（sai_agent_conversation）。
     */
    @SaCheckPermission("workflow:de:saiconv:query")
    @GetMapping("/{id}")
    public R<SaiAgentConversationVo> getById(@PathVariable Long id) {
        return R.ok(deSaiConvService.getById(id));
    }

    /**
     * 分页列表（sai_agent_conversation，按 BO 过滤 + PageQuery 翻页）。
     */
    @SaCheckPermission("workflow:de:saiconv:list")
    @GetMapping("/list")
    public R<PageResult<SaiAgentConversationVo>> list(SaiAgentConversationBo bo) {
        return R.ok(deSaiConvService.list(bo));
    }

    /**
     * 按 agent_id 查询对话列表（无分页；用于会话历史侧栏）。
     */
    @SaCheckPermission("workflow:de:saiconv:list")
    @GetMapping("/by-agent/{agentId}")
    public R<List<SaiAgentConversationVo>> listByAgentId(@PathVariable Long agentId) {
        return R.ok(deSaiConvService.listByAgentId(agentId));
    }

    /**
     * 按 record_id 查 sai_agent_conversation_record 单条。
     */
    @SaCheckPermission("workflow:de:saiconv:query")
    @GetMapping("/record/{recordId}")
    public R<SaiAgentConversationRecord> getRecordById(@PathVariable Long recordId) {
        return R.ok(deSaiConvService.getRecordById(recordId));
    }
}