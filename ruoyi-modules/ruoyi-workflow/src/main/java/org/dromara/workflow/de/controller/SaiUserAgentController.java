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
import org.dromara.workflow.de.domain.SaiUserAgent;
import org.dromara.workflow.de.service.ISaiUserAgentService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * sai_user_agent REST 端点（存根）。
 *
 * <p>本 controller 共 7 个端点（与 {@code ISaiUserAgentService} 一一对应）：
 * <ul>
 *   <li>POST /de/sai-user-agent —— 新增</li>
 *   <li>PUT /de/sai-user-agent —— 修改</li>
 *   <li>DELETE /de/sai-user-agent/{ids} —— 删除（逗号分隔）</li>
 *   <li>GET /de/sai-user-agent/{id} —— 单条详情</li>
 *   <li>GET /de/sai-user-agent/list —— 列表</li>
 *   <li>GET /de/sai-user-agent/page —— 分页</li>
 *   <li>GET /de/sai-user-agent/by-fk/{fkId} —— 按外键查询</li>
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
@RequestMapping("/de/sai-user-agent")
public class SaiUserAgentController extends BaseController {

    private final ISaiUserAgentService saiUserAgentService;

    @Log(title = "sai_user_agent", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:saiuseragent:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody SaiUserAgent bo) {
        return R.ok("ok", saiUserAgentService.add(bo));
    }

    @Log(title = "sai_user_agent", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:saiuseragent:edit")
    @PutMapping
    public R<Boolean> edit(@Valid @RequestBody SaiUserAgent bo) {
        return R.ok("ok", saiUserAgentService.edit(bo));
    }

    @Log(title = "sai_user_agent", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:saiuseragent:remove")
    @DeleteMapping("/{ids}")
    public R<Boolean> remove(@PathVariable Long[] ids) {
        return R.ok("ok", saiUserAgentService.remove(Arrays.asList(ids)));
    }

    @SaCheckPermission("workflow:de:saiuseragent:query")
    @GetMapping("/{id}")
    public R<SaiUserAgent> getInfo(@PathVariable Long id) {
        return R.ok(saiUserAgentService.getInfo(id));
    }

    @SaCheckPermission("workflow:de:saiuseragent:list")
    @GetMapping("/list")
    public R<List<SaiUserAgent>> list(SaiUserAgent bo) {
        return R.ok(saiUserAgentService.list(bo));
    }

    @SaCheckPermission("workflow:de:saiuseragent:list")
    @GetMapping("/page")
    public R<PageResult<SaiUserAgent>> page(SaiUserAgent bo) {
        return R.ok(saiUserAgentService.page(bo));
    }

    @SaCheckPermission("workflow:de:saiuseragent:list")
    @GetMapping("/by-fk/{fkId}")
    public R<List<SaiUserAgent>> listByFk(@PathVariable Long fkId) {
        return R.ok(saiUserAgentService.listByFk(fkId));
    }
}