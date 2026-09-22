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
import org.dromara.workflow.de.service.ISaiStoreInstanceService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * sai_store_instance REST 端点（存根）。
 *
 * <p>本 controller 共 7 个端点（与 {@code ISaiStoreInstanceService} 一一对应）：
 * <ul>
 *   <li>POST /de/sai-store-instance —— 新增</li>
 *   <li>PUT /de/sai-store-instance —— 修改</li>
 *   <li>DELETE /de/sai-store-instance/{ids} —— 删除（逗号分隔）</li>
 *   <li>GET /de/sai-store-instance/{id} —— 单条详情</li>
 *   <li>GET /de/sai-store-instance/list —— 列表</li>
 *   <li>GET /de/sai-store-instance/page —— 分页</li>
 *   <li>GET /de/sai-store-instance/by-fk/{fkId} —— 按外键查询</li>
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
@RequestMapping("/de/sai-store-instance")
public class SaiStoreInstanceController extends BaseController {

    private final ISaiStoreInstanceService saiStoreInstanceService;

    @Log(title = "sai_store_instance", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:saistoreinstance:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody Map<String, Object> bo) {
        return R.ok("ok", saiStoreInstanceService.add(bo));
    }

    @Log(title = "sai_store_instance", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:saistoreinstance:edit")
    @PutMapping
    public R<Boolean> edit(@Valid @RequestBody Map<String, Object> bo) {
        return R.ok("ok", saiStoreInstanceService.edit(bo));
    }

    @Log(title = "sai_store_instance", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:saistoreinstance:remove")
    @DeleteMapping("/{ids}")
    public R<Boolean> remove(@PathVariable Long[] ids) {
        return R.ok("ok", saiStoreInstanceService.remove(Arrays.asList(ids)));
    }

    @SaCheckPermission("workflow:de:saistoreinstance:query")
    @GetMapping("/{id}")
    public R<Map<String, Object>> getInfo(@PathVariable Long id) {
        return R.ok(saiStoreInstanceService.getInfo(id));
    }

    @SaCheckPermission("workflow:de:saistoreinstance:list")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(@RequestBody(required = false) Map<String, Object> bo) {
        return R.ok(saiStoreInstanceService.list(bo));
    }

    @SaCheckPermission("workflow:de:saistoreinstance:list")
    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@RequestBody(required = false) Map<String, Object> bo) {
        return R.ok(saiStoreInstanceService.page(bo));
    }

    @SaCheckPermission("workflow:de:saistoreinstance:list")
    @GetMapping("/by-fk/{fkId}")
    public R<List<Map<String, Object>>> listByFk(@PathVariable Long fkId) {
        return R.ok(saiStoreInstanceService.listByFk(fkId));
    }
}