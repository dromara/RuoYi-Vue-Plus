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
import org.dromara.workflow.de.domain.SaiSkillFile;
import org.dromara.workflow.de.service.ISaiSkillFileService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * sai_skill_file REST 端点（Map 存根 → typed CRUD，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sai-skill-file")
public class SaiSkillFileController extends BaseController {

    private final ISaiSkillFileService saiSkillFileService;

    @Log(title = "sai_skill_file", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:saiskillfile:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody SaiSkillFile bo) {
        return R.ok("ok", saiSkillFileService.add(bo));
    }

    @Log(title = "sai_skill_file", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:saiskillfile:edit")
    @PutMapping
    public R<Boolean> edit(@Valid @RequestBody SaiSkillFile bo) {
        return R.ok("ok", saiSkillFileService.edit(bo));
    }

    @Log(title = "sai_skill_file", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:saiskillfile:remove")
    @DeleteMapping("/{ids}")
    public R<Boolean> remove(@PathVariable Long[] ids) {
        return R.ok("ok", saiSkillFileService.remove(Arrays.asList(ids)));
    }

    @SaCheckPermission("workflow:de:saiskillfile:query")
    @GetMapping("/{id}")
    public R<SaiSkillFile> getInfo(@PathVariable Long id) {
        return R.ok(saiSkillFileService.getInfo(id));
    }

    @SaCheckPermission("workflow:de:saiskillfile:list")
    @GetMapping("/list")
    public R<List<SaiSkillFile>> list(SaiSkillFile bo) {
        return R.ok(saiSkillFileService.list(bo));
    }

    @SaCheckPermission("workflow:de:saiskillfile:list")
    @GetMapping("/page")
    public R<PageResult<SaiSkillFile>> page(SaiSkillFile bo) {
        return R.ok(saiSkillFileService.page(bo));
    }

    @SaCheckPermission("workflow:de:saiskillfile:list")
    @GetMapping("/by-fk/{fkId}")
    public R<List<SaiSkillFile>> listByFk(@PathVariable Long fkId) {
        return R.ok(saiSkillFileService.listByFk(fkId));
    }
}
