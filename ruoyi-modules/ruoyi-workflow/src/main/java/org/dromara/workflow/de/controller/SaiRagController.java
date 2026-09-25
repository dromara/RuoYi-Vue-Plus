package org.dromara.workflow.de.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.workflow.de.domain.SaiRag;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.service.ISaiRagService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * sai_rag REST 端点（已实装）。
 *
 * <p>本 controller 共 7 个端点（与 {@code ISaiRagService} 一一对应）：
 * <ul>
 *   <li>POST /de/sai-rag —— 新增</li>
 *   <li>PUT /de/sai-rag —— 修改</li>
 *   <li>DELETE /de/sai-rag/{ids} —— 删除（逗号分隔）</li>
 *   <li>GET /de/sai-rag/{id} —— 单条详情</li>
 *   <li>GET /de/sai-rag/list —— 列表</li>
 *   <li>GET /de/sai-rag/page —— 分页</li>
 *   <li>GET /de/sai-rag/by-fk/{fkId} —— 按外键查询</li>
 * </ul>
 *
 * <p><b>实装状态</b>：该组端点已由服务实现层提供真实 CRUD（原 OW-03 存根已实化）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sai-rag")
public class SaiRagController extends BaseController {

    private final ISaiRagService saiRagService;

    @Log(title = "sai_rag", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:sairag:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody SaiRag bo) {
        return R.ok("ok", saiRagService.add(bo));
    }

    @Log(title = "sai_rag", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:sairag:edit")
    @PutMapping
    public R<Boolean> edit(@Valid @RequestBody SaiRag bo) {
        return R.ok("ok", saiRagService.edit(bo));
    }

    @Log(title = "sai_rag", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:sairag:remove")
    @DeleteMapping("/{ids}")
    public R<Boolean> remove(@PathVariable Long[] ids) {
        return R.ok("ok", saiRagService.remove(Arrays.asList(ids)));
    }

    @SaCheckPermission("workflow:de:sairag:query")
    @GetMapping("/{id}")
    public R<SaiRag> getInfo(@PathVariable Long id) {
        return R.ok(saiRagService.getInfo(id));
    }

    @SaCheckPermission("workflow:de:sairag:list")
    @GetMapping("/list")
    public R<List<SaiRag>> list(SaiRag bo) {
        return R.ok(saiRagService.list(bo));
    }

    @SaCheckPermission("workflow:de:sairag:list")
    @GetMapping("/page")
    public R<PageResult<SaiRag>> page(SaiRag bo, org.dromara.common.mybatis.core.page.PageQuery pageQuery) {
        return R.ok(saiRagService.page(bo));
    }

    @SaCheckPermission("workflow:de:sairag:list")
    @GetMapping("/by-fk/{fkId}")
    public R<List<SaiRag>> listByFk(@PathVariable Long fkId) {
        return R.ok(saiRagService.listByFk(fkId));
    }
}