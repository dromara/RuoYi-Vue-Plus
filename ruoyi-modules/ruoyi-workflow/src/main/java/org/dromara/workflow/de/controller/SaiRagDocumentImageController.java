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
import org.dromara.workflow.de.domain.SaiRagDocumentImage;
import org.dromara.workflow.de.service.ISaiRagDocumentImageService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * sai_rag_document_image REST 端点（Map 存根 → typed CRUD，列真源 ry_ai.sql DDL）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sai-rag-document-image")
public class SaiRagDocumentImageController extends BaseController {

    private final ISaiRagDocumentImageService saiRagDocumentImageService;

    @Log(title = "sai_rag_document_image", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:sairagdocumentimage:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody SaiRagDocumentImage bo) {
        return R.ok("ok", saiRagDocumentImageService.add(bo));
    }

    @Log(title = "sai_rag_document_image", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:sairagdocumentimage:edit")
    @PutMapping
    public R<Boolean> edit(@Valid @RequestBody SaiRagDocumentImage bo) {
        return R.ok("ok", saiRagDocumentImageService.edit(bo));
    }

    @Log(title = "sai_rag_document_image", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:sairagdocumentimage:remove")
    @DeleteMapping("/{ids}")
    public R<Boolean> remove(@PathVariable Long[] ids) {
        return R.ok("ok", saiRagDocumentImageService.remove(Arrays.asList(ids)));
    }

    @SaCheckPermission("workflow:de:sairagdocumentimage:query")
    @GetMapping("/{id}")
    public R<SaiRagDocumentImage> getInfo(@PathVariable Long id) {
        return R.ok(saiRagDocumentImageService.getInfo(id));
    }

    @SaCheckPermission("workflow:de:sairagdocumentimage:list")
    @GetMapping("/list")
    public R<List<SaiRagDocumentImage>> list(SaiRagDocumentImage bo) {
        return R.ok(saiRagDocumentImageService.list(bo));
    }

    @SaCheckPermission("workflow:de:sairagdocumentimage:list")
    @GetMapping("/page")
    public R<PageResult<SaiRagDocumentImage>> page(SaiRagDocumentImage bo) {
        return R.ok(saiRagDocumentImageService.page(bo));
    }

    @SaCheckPermission("workflow:de:sairagdocumentimage:list")
    @GetMapping("/by-fk/{fkId}")
    public R<List<SaiRagDocumentImage>> listByFk(@PathVariable Long fkId) {
        return R.ok(saiRagDocumentImageService.listByFk(fkId));
    }
}
