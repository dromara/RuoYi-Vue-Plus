package org.dromara.workflow.de.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.workflow.de.domain.SaiResource;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.service.ISaiResourceService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * sai_resource REST 端点（已实装）。
 *
 * <p>本 controller 共 7 个端点（与 {@code ISaiResourceService} 一一对应）：
 * <ul>
 *   <li>POST /de/sai-resource —— 新增</li>
 *   <li>PUT /de/sai-resource —— 修改</li>
 *   <li>DELETE /de/sai-resource/{ids} —— 删除（逗号分隔）</li>
 *   <li>GET /de/sai-resource/{id} —— 单条详情</li>
 *   <li>GET /de/sai-resource/list —— 列表</li>
 *   <li>GET /de/sai-resource/page —— 分页</li>
 *   <li>GET /de/sai-resource/by-fk/{fkId} —— 按外键查询</li>
 * </ul>
 *
 * <p><b>实装状态</b>：该组端点已由服务实现层提供真实 CRUD（原 OW-03 存根已实化）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sai-resource")
public class SaiResourceController extends BaseController {

    private final ISaiResourceService saiResourceService;

    @Log(title = "sai_resource", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:sairesource:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody SaiResource bo) {
        return R.ok("ok", saiResourceService.add(bo));
    }

    @Log(title = "sai_resource", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:sairesource:edit")
    @PutMapping
    public R<Boolean> edit(@Valid @RequestBody SaiResource bo) {
        return R.ok("ok", saiResourceService.edit(bo));
    }

    @Log(title = "sai_resource", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:sairesource:remove")
    @DeleteMapping("/{ids}")
    public R<Boolean> remove(@PathVariable Long[] ids) {
        return R.ok("ok", saiResourceService.remove(Arrays.asList(ids)));
    }

    @SaCheckPermission("workflow:de:sairesource:query")
    @GetMapping("/{id}")
    public R<SaiResource> getInfo(@PathVariable Long id) {
        return R.ok(saiResourceService.getInfo(id));
    }

    @SaCheckPermission("workflow:de:sairesource:list")
    @GetMapping("/list")
    public R<List<SaiResource>> list(SaiResource bo) {
        return R.ok(saiResourceService.list(bo));
    }

    @SaCheckPermission("workflow:de:sairesource:list")
    @GetMapping("/page")
    public R<PageResult<SaiResource>> page(SaiResource bo) {
        return R.ok(saiResourceService.page(bo));
    }

    @SaCheckPermission("workflow:de:sairesource:list")
    @GetMapping("/by-fk/{fkId}")
    public R<List<SaiResource>> listByFk(@PathVariable Long fkId) {
        return R.ok(saiResourceService.listByFk(fkId));
    }
}