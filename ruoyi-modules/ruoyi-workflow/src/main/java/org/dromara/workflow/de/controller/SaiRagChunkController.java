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
import org.dromara.workflow.de.domain.SaiRagChunk;
import org.dromara.workflow.de.service.ISaiRagChunkService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * sai_rag_chunk REST 端点（Map 存根 → typed CRUD，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sai-rag-chunk")
public class SaiRagChunkController extends BaseController {

    private final ISaiRagChunkService saiRagChunkService;

    @Log(title = "sai_rag_chunk", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:sairagchunk:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody SaiRagChunk bo) {
        return R.ok("ok", saiRagChunkService.add(bo));
    }

    @Log(title = "sai_rag_chunk", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:sairagchunk:edit")
    @PutMapping
    public R<Boolean> edit(@Valid @RequestBody SaiRagChunk bo) {
        return R.ok("ok", saiRagChunkService.edit(bo));
    }

    @Log(title = "sai_rag_chunk", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:sairagchunk:remove")
    @DeleteMapping("/{ids}")
    public R<Boolean> remove(@PathVariable Long[] ids) {
        return R.ok("ok", saiRagChunkService.remove(Arrays.asList(ids)));
    }

    @SaCheckPermission("workflow:de:sairagchunk:query")
    @GetMapping("/{id}")
    public R<SaiRagChunk> getInfo(@PathVariable Long id) {
        return R.ok(saiRagChunkService.getInfo(id));
    }

    @SaCheckPermission("workflow:de:sairagchunk:list")
    @GetMapping("/list")
    public R<List<SaiRagChunk>> list(SaiRagChunk bo) {
        return R.ok(saiRagChunkService.list(bo));
    }

    @SaCheckPermission("workflow:de:sairagchunk:list")
    @GetMapping("/page")
    public R<PageResult<SaiRagChunk>> page(SaiRagChunk bo) {
        return R.ok(saiRagChunkService.page(bo));
    }

    @SaCheckPermission("workflow:de:sairagchunk:list")
    @GetMapping("/by-fk/{fkId}")
    public R<List<SaiRagChunk>> listByFk(@PathVariable Long fkId) {
        return R.ok(saiRagChunkService.listByFk(fkId));
    }
}
