package org.dromara.workflow.de.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.workflow.de.domain.SaiObjectAcl;
import org.dromara.workflow.de.service.ISaiObjectAclService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对象级 ACL 控制层（D-011 · AU-03 方案 B + AU-04/05/06）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/acl")
public class SaiObjectAclController {

    private final ISaiObjectAclService saiObjectAclService;

    @SaCheckPermission("workflow:de:acl:add")
    @Log(title = "对象级 ACL", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> grant(@RequestBody SaiObjectAcl bo) {
        return R.ok("授权成功", saiObjectAclService.grant(bo));
    }

    @SaCheckPermission("workflow:de:acl:remove")
    @Log(title = "对象级 ACL", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Boolean> revoke(@PathVariable Long id) {
        return R.ok(saiObjectAclService.revoke(id));
    }

    @SaCheckPermission("workflow:de:acl:query")
    @GetMapping("/list")
    public R<List<SaiObjectAcl>> list(@RequestParam String objectType, @RequestParam Long objectId) {
        return R.ok(saiObjectAclService.listByObject(objectType, objectId));
    }
}
