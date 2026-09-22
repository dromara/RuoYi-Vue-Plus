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
import org.dromara.workflow.de.service.ISaiRagDocumentImageService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * sai_rag_document_image REST 端点（存根）。
 *
 * <p>本 controller 共 7 个端点（与 {@code ISaiRagDocumentImageService} 一一对应）：
 * <ul>
 *   <li>POST /de/sai-rag-document-image —— 新增</li>
 *   <li>PUT /de/sai-rag-document-image —— 修改</li>
 *   <li>DELETE /de/sai-rag-document-image/{ids} —— 删除（逗号分隔）</li>
 *   <li>GET /de/sai-rag-document-image/{id} —— 单条详情</li>
 *   <li>GET /de/sai-rag-document-image/list —— 列表</li>
 *   <li>GET /de/sai-rag-document-image/page —— 分页</li>
 *   <li>GET /de/sai-rag-document-image/by-fk/{fkId} —— 按外键查询</li>
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
@RequestMapping("/de/sai-rag-document-image")
public class SaiRagDocumentImageController extends BaseController {

    private final ISaiRagDocumentImageService saiRagDocumentImageService;

    @Log(title = "sai_rag_document_image", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:sairagdocumentimage:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody Map<String, Object> bo) {
        return R.ok("ok", saiRagDocumentImageService.add(bo));
    }

    @Log(title = "sai_rag_document_image", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:sairagdocumentimage:edit")
    @PutMapping
    public R<Boolean> edit(@Valid @RequestBody Map<String, Object> bo) {
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
    public R<Map<String, Object>> getInfo(@PathVariable Long id) {
        return R.ok(saiRagDocumentImageService.getInfo(id));
    }

    @SaCheckPermission("workflow:de:sairagdocumentimage:list")
    @GetMapping("/list")
    public R<List<Map<String, Object>>> list(@RequestBody(required = false) Map<String, Object> bo) {
        return R.ok(saiRagDocumentImageService.list(bo));
    }

    @SaCheckPermission("workflow:de:sairagdocumentimage:list")
    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@RequestBody(required = false) Map<String, Object> bo) {
        return R.ok(saiRagDocumentImageService.page(bo));
    }

    @SaCheckPermission("workflow:de:sairagdocumentimage:list")
    @GetMapping("/by-fk/{fkId}")
    public R<List<Map<String, Object>>> listByFk(@PathVariable Long fkId) {
        return R.ok(saiRagDocumentImageService.listByFk(fkId));
    }
}