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
import org.dromara.workflow.de.domain.SaiAgent;
import org.dromara.workflow.de.service.ISaiAgentService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * sai_agent REST 端点（已实装）。
 *
 * <p>本 controller 共 7 个端点（与 {@code ISaiAgentService} 一一对应）：
 * <ul>
 *   <li>POST /de/sai-agent —— 新增</li>
 *   <li>PUT /de/sai-agent —— 修改</li>
 *   <li>DELETE /de/sai-agent/{ids} —— 删除（逗号分隔）</li>
 *   <li>GET /de/sai-agent/{id} —— 单条详情</li>
 *   <li>GET /de/sai-agent/list —— 列表</li>
 *   <li>GET /de/sai-agent/page —— 分页</li>
 *   <li>GET /de/sai-agent/by-fk/{fkId} —— 按外键查询</li>
 * </ul>
 *
 * <p><b>实装状态</b>：该组端点已由服务实现层提供真实 CRUD（原 OW-03 存根已实化）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sai-agent")
public class SaiAgentController extends BaseController {

    private final ISaiAgentService saiAgentService;

    @Log(title = "sai_agent", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:saiagent:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody SaiAgent bo) {
        return R.ok("ok", saiAgentService.add(bo));
    }

    @Log(title = "sai_agent", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:saiagent:edit")
    @PutMapping
    public R<Boolean> edit(@Valid @RequestBody SaiAgent bo) {
        return R.ok("ok", saiAgentService.edit(bo));
    }

    @Log(title = "sai_agent", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:saiagent:remove")
    @DeleteMapping("/{ids}")
    public R<Boolean> remove(@PathVariable Long[] ids) {
        return R.ok("ok", saiAgentService.remove(Arrays.asList(ids)));
    }

    @SaCheckPermission("workflow:de:saiagent:query")
    @GetMapping("/{id}")
    public R<SaiAgent> getInfo(@PathVariable Long id) {
        return R.ok(saiAgentService.getInfo(id));
    }

    @SaCheckPermission("workflow:de:saiagent:list")
    @GetMapping("/list")
    public R<List<SaiAgent>> list(SaiAgent bo) {
        return R.ok(saiAgentService.list(bo));
    }

    @SaCheckPermission("workflow:de:saiagent:list")
    @GetMapping("/page")
    public R<PageResult<SaiAgent>> page(SaiAgent bo, org.dromara.common.mybatis.core.page.PageQuery pageQuery) {
        return R.ok(saiAgentService.page(bo, pageQuery));
    }

    @SaCheckPermission("workflow:de:saiagent:list")
    @GetMapping("/by-fk/{fkId}")
    public R<List<SaiAgent>> listByFk(@PathVariable Long fkId) {
        return R.ok(saiAgentService.listByFk(fkId));
    }
}