package org.dromara.workflow.de.sy.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.sy.domain.bo.DeSyConfigBo;
import org.dromara.workflow.de.sy.domain.bo.DeSyConfigQueryBo;
import org.dromara.workflow.de.sy.domain.vo.DeSyConfigVo;
import org.dromara.workflow.de.sy.service.IDeSyConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * SY-02 系统配置 REST 端点（2026-09-23）。
 *
 * <p>路径：{@code /de/sy/config/*} —— 与现有 SY-05/06/07/08/09 端点
 * （{@link DeSyController}，{@code /de/sy/{taskId}/ack-state|...}）路径不重叠。
 *
 * <p>端点：
 * <ul>
 *   <li>GET    /de/sy/config/list            —— 分页</li>
 *   <li>GET    /de/sy/config/listAll         —— 全量</li>
 *   <li>GET    /de/sy/config/{id}            —— 详情</li>
 *   <li>GET    /de/sy/config/key/{key}       —— 按 key 取值</li>
 *   <li>POST   /de/sy/config                 —— 新增</li>
 *   <li>PUT    /de/sy/config                 —— 更新</li>
 *   <li>DELETE /de/sy/config/{ids}           —— 批量删除</li>
 *   <li>DELETE /de/sy/config/refreshCache    —— 刷新缓存</li>
 * </ul>
 *
 * @author AIP
 * @since 2026-09-23
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sy/config")
public class DeSyConfigController extends BaseController {

    private final IDeSyConfigService configService;

    @SaCheckPermission("workflow:de:sy-config:list")
    @GetMapping("/list")
    public R<PageResult<DeSyConfigVo>> list(DeSyConfigQueryBo query,
                                            @RequestParam(defaultValue = "1") Long pageNum,
                                            @RequestParam(defaultValue = "10") Long pageSize) {
        com.baomidou.mybatisplus.core.metadata.IPage<DeSyConfigVo> pg =
            configService.page(query, pageNum, pageSize);
        return R.ok(PageResult.build(pg.getRecords(), pg.getTotal()));
    }

    @SaCheckPermission("workflow:de:sy-config:list")
    @GetMapping("/listAll")
    public R<List<DeSyConfigVo>> listAll(DeSyConfigQueryBo query) {
        return R.ok(configService.list(query));
    }

    @SaCheckPermission("workflow:de:sy-config:query")
    @GetMapping("/{configId}")
    public R<DeSyConfigVo> getInfo(@PathVariable Long configId) {
        return R.ok(configService.getInfo(configId));
    }

    @SaCheckPermission("workflow:de:sy-config:query")
    @GetMapping("/key/{configKey}")
    public R<String> getConfigByKey(@PathVariable String configKey) {
        return R.data(configService.getConfigByKey(configKey));
    }

    @Log(title = "系统配置", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:sy-config:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody DeSyConfigBo bo) {
        return R.ok("配置已新增", configService.add(bo));
    }

    @Log(title = "系统配置", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:sy-config:edit")
    @PutMapping
    public R<Void> edit(@Valid @RequestBody DeSyConfigBo bo) {
        configService.edit(bo);
        return R.ok();
    }

    @Log(title = "系统配置", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:sy-config:remove")
    @DeleteMapping("/{configIds}")
    public R<Void> remove(@PathVariable Long[] configIds) {
        configService.remove(Arrays.copyOf(configIds, configIds.length, Long[].class));
        return R.ok();
    }

    @Log(title = "系统配置", businessType = BusinessType.CLEAN)
    @SaCheckPermission("workflow:de:sy-config:edit")
    @DeleteMapping("/refreshCache")
    public R<Void> refreshCache() {
        configService.refreshCache();
        return R.ok();
    }
}