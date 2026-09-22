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
import org.dromara.workflow.de.service.ISaiOpenapiUserService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * sai_openapi_user REST 端点（存根）。
 *
 * <p>本 controller 共 7 个端点（与 {@code ISaiOpenapiUserService} 一一对应）：
 * <ul>
 *   <li>POST /de/sai-openapi-user —— 新增</li>
 *   <li>PUT /de/sai-openapi-user —— 修改</li>
 *   <li>DELETE /de/sai-openapi-user/{ids} —— 删除（逗号分隔）</li>
 *   <li>GET /de/sai-openapi-user/{id} —— 单条详情</li>
 *   <li>GET /de/sai-openapi-user/list —— 列表</li>
 *   <li>GET /de/sai-openapi-user/page —— 分页</li>
 *   <li>GET /de/sai-openapi-user/by-fk/{fkId} —— 按外键查询</li>
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
@RequestMapping("/de/sai-openapi-user")
public class SaiOpenapiUserController extends BaseController {

    private final ISaiOpenapiUserService saiOpenapiUserService;

    @Log(title = "sai_openapi_user", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:saiopenapiuser:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody Map<String, Object> bo) {
        return R.ok("ok", saiOpenapiUserService.add(bo));
    }

    @Log(title = "sai_openapi_user", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:saiopenapiuser:edit")
    @PutMapping
    public R<Boolean> edit(@Valid @RequestBody Map<String, Object> bo) {
        return R.ok("ok", saiOpenapiUserService.edit(bo));
    }

    @Log(title = "sai_openapi_user", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:saiopenapiuser:remove")
    @DeleteMapping("/{ids}")
    public R<Boolean> remove(@PathVariable Long[] ids) {
        return R.ok("ok", saiOpenapiUserService.remove(Arrays.asList(ids)));
    }

    @SaCheckPermission("workflow:de:saiopenapiuser:query")
    @GetMapping("/{id}")
    public R<Map<String, Object>> getInfo(@PathVariable Long id) {
        return R.ok(saiOpenapiUserService.getInfo(id));
    }

    @SaCheckPermission("workflow:de:saiopenapiuser:list")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(@RequestBody(required = false) Map<String, Object> bo) {
        return R.ok(saiOpenapiUserService.list(bo));
    }

    @SaCheckPermission("workflow:de:saiopenapiuser:list")
    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@RequestBody(required = false) Map<String, Object> bo) {
        return R.ok(saiOpenapiUserService.page(bo));
    }

    @SaCheckPermission("workflow:de:saiopenapiuser:list")
    @GetMapping("/by-fk/{fkId}")
    public R<List<Map<String, Object>>> listByFk(@PathVariable Long fkId) {
        return R.ok(saiOpenapiUserService.listByFk(fkId));
    }
}