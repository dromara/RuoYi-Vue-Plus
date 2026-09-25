package org.dromara.workflow.de.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.workflow.de.domain.SaiClientNode;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.service.ISaiClientNodeService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * sai_client_node REST 端点（已实装）。
 *
 * <p>本 controller 共 7 个端点（与 {@code ISaiClientNodeService} 一一对应）：
 * <ul>
 *   <li>POST /de/sai-client-node —— 新增</li>
 *   <li>PUT /de/sai-client-node —— 修改</li>
 *   <li>DELETE /de/sai-client-node/{ids} —— 删除（逗号分隔）</li>
 *   <li>GET /de/sai-client-node/{id} —— 单条详情</li>
 *   <li>GET /de/sai-client-node/list —— 列表</li>
 *   <li>GET /de/sai-client-node/page —— 分页</li>
 *   <li>GET /de/sai-client-node/by-fk/{fkId} —— 按外键查询</li>
 * </ul>
 *
 * <p><b>实装状态</b>：该组端点已由服务实现层提供真实 CRUD（原 OW-03 存根已实化）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sai-client-node")
public class SaiClientNodeController extends BaseController {

    private final ISaiClientNodeService saiClientNodeService;

    @Log(title = "sai_client_node", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:saiclientnode:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody SaiClientNode bo) {
        return R.ok("ok", saiClientNodeService.add(bo));
    }

    @Log(title = "sai_client_node", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:saiclientnode:edit")
    @PutMapping
    public R<Boolean> edit(@Valid @RequestBody SaiClientNode bo) {
        return R.ok("ok", saiClientNodeService.edit(bo));
    }

    @Log(title = "sai_client_node", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:saiclientnode:remove")
    @DeleteMapping("/{ids}")
    public R<Boolean> remove(@PathVariable Long[] ids) {
        return R.ok("ok", saiClientNodeService.remove(Arrays.asList(ids)));
    }

    @SaCheckPermission("workflow:de:saiclientnode:query")
    @GetMapping("/{id}")
    public R<SaiClientNode> getInfo(@PathVariable Long id) {
        return R.ok(saiClientNodeService.getInfo(id));
    }

    @SaCheckPermission("workflow:de:saiclientnode:list")
    @GetMapping("/list")
    public R<List<SaiClientNode>> list(SaiClientNode bo) {
        return R.ok(saiClientNodeService.list(bo));
    }

    @SaCheckPermission("workflow:de:saiclientnode:list")
    @GetMapping("/page")
    public R<PageResult<SaiClientNode>> page(SaiClientNode bo) {
        return R.ok(saiClientNodeService.page(bo));
    }

    @SaCheckPermission("workflow:de:saiclientnode:list")
    @GetMapping("/by-fk/{fkId}")
    public R<List<SaiClientNode>> listByFk(@PathVariable Long fkId) {
        return R.ok(saiClientNodeService.listByFk(fkId));
    }
}