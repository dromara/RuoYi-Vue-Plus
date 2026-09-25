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
import org.dromara.workflow.de.domain.SaiModelProvider;
import org.dromara.workflow.de.service.ISaiModelProviderService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * sai_model_provider REST 端点（Map 存根 → typed CRUD，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sai-model-provider")
public class SaiModelProviderController extends BaseController {

    private final ISaiModelProviderService saiModelProviderService;

    @Log(title = "sai_model_provider", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:saimodelprovider:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody SaiModelProvider bo) {
        return R.ok("ok", saiModelProviderService.add(bo));
    }

    @Log(title = "sai_model_provider", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:saimodelprovider:edit")
    @PutMapping
    public R<Boolean> edit(@Valid @RequestBody SaiModelProvider bo) {
        return R.ok("ok", saiModelProviderService.edit(bo));
    }

    @Log(title = "sai_model_provider", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:saimodelprovider:remove")
    @DeleteMapping("/{ids}")
    public R<Boolean> remove(@PathVariable Long[] ids) {
        return R.ok("ok", saiModelProviderService.remove(Arrays.asList(ids)));
    }

    @SaCheckPermission("workflow:de:saimodelprovider:query")
    @GetMapping("/{id}")
    public R<SaiModelProvider> getInfo(@PathVariable Long id) {
        return R.ok(saiModelProviderService.getInfo(id));
    }

    @SaCheckPermission("workflow:de:saimodelprovider:list")
    @GetMapping("/list")
    public R<List<SaiModelProvider>> list(SaiModelProvider bo) {
        return R.ok(saiModelProviderService.list(bo));
    }

    @SaCheckPermission("workflow:de:saimodelprovider:list")
    @GetMapping("/page")
    public R<PageResult<SaiModelProvider>> page(SaiModelProvider bo) {
        return R.ok(saiModelProviderService.page(bo));
    }

    @SaCheckPermission("workflow:de:saimodelprovider:list")
    @GetMapping("/by-fk/{fkId}")
    public R<List<SaiModelProvider>> listByFk(@PathVariable Long fkId) {
        return R.ok(saiModelProviderService.listByFk(fkId));
    }
}
