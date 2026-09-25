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
import org.dromara.workflow.de.domain.SaiAgentMcpServer;
import org.dromara.workflow.de.service.ISaiAgentMcpServerService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * sai_agent_mcp_server REST 端点（Map 存根 → typed CRUD，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sai-agent-mcp-server")
public class SaiAgentMcpServerController extends BaseController {

    private final ISaiAgentMcpServerService saiAgentMcpServerService;

    @Log(title = "sai_agent_mcp_server", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:saiagentmcpserver:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody SaiAgentMcpServer bo) {
        return R.ok("ok", saiAgentMcpServerService.add(bo));
    }

    @Log(title = "sai_agent_mcp_server", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:saiagentmcpserver:edit")
    @PutMapping
    public R<Boolean> edit(@Valid @RequestBody SaiAgentMcpServer bo) {
        return R.ok("ok", saiAgentMcpServerService.edit(bo));
    }

    @Log(title = "sai_agent_mcp_server", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:saiagentmcpserver:remove")
    @DeleteMapping("/{ids}")
    public R<Boolean> remove(@PathVariable Long[] ids) {
        return R.ok("ok", saiAgentMcpServerService.remove(Arrays.asList(ids)));
    }

    @SaCheckPermission("workflow:de:saiagentmcpserver:query")
    @GetMapping("/{id}")
    public R<SaiAgentMcpServer> getInfo(@PathVariable Long id) {
        return R.ok(saiAgentMcpServerService.getInfo(id));
    }

    @SaCheckPermission("workflow:de:saiagentmcpserver:list")
    @GetMapping("/list")
    public R<List<SaiAgentMcpServer>> list(SaiAgentMcpServer bo) {
        return R.ok(saiAgentMcpServerService.list(bo));
    }

    @SaCheckPermission("workflow:de:saiagentmcpserver:list")
    @GetMapping("/page")
    public R<PageResult<SaiAgentMcpServer>> page(SaiAgentMcpServer bo) {
        return R.ok(saiAgentMcpServerService.page(bo));
    }

    @SaCheckPermission("workflow:de:saiagentmcpserver:list")
    @GetMapping("/by-fk/{fkId}")
    public R<List<SaiAgentMcpServer>> listByFk(@PathVariable Long fkId) {
        return R.ok(saiAgentMcpServerService.listByFk(fkId));
    }
}
