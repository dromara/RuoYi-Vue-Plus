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
import org.dromara.workflow.de.service.ISaiAgentConversationRecordService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * sai_agent_conversation_record REST 端点（存根）。
 *
 * <p>本 controller 共 7 个端点（与 {@code ISaiAgentConversationRecordService} 一一对应）：
 * <ul>
 *   <li>POST /de/sai-agent-conversation-record —— 新增</li>
 *   <li>PUT /de/sai-agent-conversation-record —— 修改</li>
 *   <li>DELETE /de/sai-agent-conversation-record/{ids} —— 删除（逗号分隔）</li>
 *   <li>GET /de/sai-agent-conversation-record/{id} —— 单条详情</li>
 *   <li>GET /de/sai-agent-conversation-record/list —— 列表</li>
 *   <li>GET /de/sai-agent-conversation-record/page —— 分页</li>
 *   <li>GET /de/sai-agent-conversation-record/by-fk/{fkId} —— 按外键查询</li>
 * </ul>
 *
 * <p><b>存根说明</b>：所有调用进入服务实现层时统一抛出 {@code UnsupportedOperationException}，
 * 等待 owner 实装（OW-03 决议）。
 *
 * <p><b>与 {@code DeSaiConvController} 的关系</b>：该 controller 暴露的
 * {@code GET /de/sai-conv/record/{recordId}} 端点是单条读；
 * 本 controller 的 7 端点是独立 CRUD 接口（与该派生读不相互替代）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sai-agent-conversation-record")
public class SaiAgentConversationRecordController extends BaseController {

    private final ISaiAgentConversationRecordService saiAgentConversationRecordService;

    @Log(title = "sai_agent_conversation_record", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:saiconvrecord:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody SaiAgentConversationRecord bo) {
        return R.ok("ok", saiAgentConversationRecordService.add(bo));
    }

    @Log(title = "sai_agent_conversation_record", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:saiconvrecord:edit")
    @PutMapping
    public R<Boolean> edit(@Valid @RequestBody SaiAgentConversationRecord bo) {
        return R.ok("ok", saiAgentConversationRecordService.edit(bo));
    }

    @Log(title = "sai_agent_conversation_record", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:saiconvrecord:remove")
    @DeleteMapping("/{ids}")
    public R<Boolean> remove(@PathVariable Long[] ids) {
        return R.ok("ok", saiAgentConversationRecordService.remove(Arrays.asList(ids)));
    }

    @SaCheckPermission("workflow:de:saiconvrecord:query")
    @GetMapping("/{id}")
    public R<SaiAgentConversationRecord> getInfo(@PathVariable Long id) {
        return R.ok(saiAgentConversationRecordService.getInfo(id));
    }

    @SaCheckPermission("workflow:de:saiconvrecord:list")
    @GetMapping("/list")
    public R<List<SaiAgentConversationRecord>> list(SaiAgentConversationRecord bo) {
        return R.ok(saiAgentConversationRecordService.list(bo));
    }

    @SaCheckPermission("workflow:de:saiconvrecord:list")
    @GetMapping("/page")
    public R<PageResult<SaiAgentConversationRecord>> page(SaiAgentConversationRecord bo) {
        return R.ok(saiAgentConversationRecordService.page(bo));
    }

    @SaCheckPermission("workflow:de:saiconvrecord:list")
    @GetMapping("/by-fk/{fkId}")
    public R<List<SaiAgentConversationRecord>> listByFk(@PathVariable Long fkId) {
        return R.ok(saiAgentConversationRecordService.listByFk(fkId));
    }
}