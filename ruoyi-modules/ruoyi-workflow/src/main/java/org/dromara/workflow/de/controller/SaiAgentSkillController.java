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
import org.dromara.workflow.de.domain.SaiAgentSkill;
import org.dromara.workflow.de.service.ISaiAgentSkillService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * sai_agent_skill REST 端点（存根）。
 *
 * <p>本 controller 共 7 个端点（与 {@code ISaiAgentSkillService} 一一对应）：
 * <ul>
 *   <li>POST /de/sai-agent-skill —— 新增</li>
 *   <li>PUT /de/sai-agent-skill —— 修改</li>
 *   <li>DELETE /de/sai-agent-skill/{ids} —— 删除（逗号分隔）</li>
 *   <li>GET /de/sai-agent-skill/{id} —— 单条详情</li>
 *   <li>GET /de/sai-agent-skill/list —— 列表</li>
 *   <li>GET /de/sai-agent-skill/page —— 分页</li>
 *   <li>GET /de/sai-agent-skill/by-fk/{fkId} —— 按外键查询</li>
 * </ul>
 *
 * <p><b>存根说明</b>：所有调用进入服务实现层时统一抛出 {@code UnsupportedOperationException}，
 * 等待 owner 实装（OW-03 决议）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sai-agent-skill")
public class SaiAgentSkillController extends BaseController {

    private final ISaiAgentSkillService saiAgentSkillService;

    @Log(title = "sai_agent_skill", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:saiagentskill:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody SaiAgentSkill bo) {
        return R.ok("ok", saiAgentSkillService.add(bo));
    }

    @Log(title = "sai_agent_skill", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:saiagentskill:edit")
    @PutMapping
    public R<Boolean> edit(@Valid @RequestBody SaiAgentSkill bo) {
        return R.ok("ok", saiAgentSkillService.edit(bo));
    }

    @Log(title = "sai_agent_skill", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:saiagentskill:remove")
    @DeleteMapping("/{ids}")
    public R<Boolean> remove(@PathVariable Long[] ids) {
        return R.ok("ok", saiAgentSkillService.remove(Arrays.asList(ids)));
    }

    @SaCheckPermission("workflow:de:saiagentskill:query")
    @GetMapping("/{id}")
    public R<SaiAgentSkill> getInfo(@PathVariable Long id) {
        return R.ok(saiAgentSkillService.getInfo(id));
    }

    @SaCheckPermission("workflow:de:saiagentskill:list")
    @GetMapping("/list")
    public R<List<SaiAgentSkill>> list(SaiAgentSkill bo) {
        return R.ok(saiAgentSkillService.list(bo));
    }

    @SaCheckPermission("workflow:de:saiagentskill:list")
    @GetMapping("/page")
    public R<PageResult<SaiAgentSkill>> page(SaiAgentSkill bo) {
        return R.ok(saiAgentSkillService.page(bo));
    }

    @SaCheckPermission("workflow:de:saiagentskill:list")
    @GetMapping("/by-fk/{fkId}")
    public R<List<SaiAgentSkill>> listByFk(@PathVariable Long fkId) {
        return R.ok(saiAgentSkillService.listByFk(fkId));
    }
}