package org.dromara.workflow.de.sy.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.sy.client.SnailJobLogHttpClient;
import org.dromara.workflow.de.sy.domain.bo.DeSyLogQueryBo;
import org.dromara.workflow.de.sy.domain.vo.DeSyLogVo;
import org.dromara.workflow.de.sy.service.IDeSyLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * SY-03 系统日志 REST 端点（2026-09-23）。
 *
 * <p>路径：{@code /de/sy/log/*} —— 与现有 SY-05/06/07/08/09 端点
 * （{@link DeSyController}）路径不重叠，与 SY-02 配置端点
 * （{@link DeSyConfigController}，{@code /de/sy/config/*}）也不重叠。
 *
 * <p>端点：
 * <ul>
 *   <li>GET    /de/sy/log/operlist       —— 操作日志分页（sys_oper_log）</li>
 *   <li>GET    /de/sy/log/operlistAll    —— 操作日志全量</li>
 *   <li>GET    /de/sy/log/{operId}       —— 操作日志详情</li>
 *   <li>DELETE /de/sy/log/{operIds}      —— 操作日志批量删除</li>
 *   <li>DELETE /de/sy/log/clean          —— 操作日志物理清空</li>
 *   <li>POST   /de/sy/log/snailjob-list  —— SnailJob 调度日志（HTTP 桥接，fail-soft）</li>
 * </ul>
 *
 * @author AIP
 * @since 2026-09-23
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/sy/log")
public class DeSyLogController extends BaseController {

    private final IDeSyLogService logService;
    private final SnailJobLogHttpClient snailJobLogHttpClient;

    @SaCheckPermission("workflow:de:sy-log:list")
    @GetMapping("/operlist")
    public R<PageResult<DeSyLogVo>> operList(DeSyLogQueryBo query,
                                             @RequestParam(defaultValue = "1") Long pageNum,
                                             @RequestParam(defaultValue = "10") Long pageSize) {
        com.baomidou.mybatisplus.core.metadata.IPage<DeSyLogVo> pg =
            logService.page(query, pageNum, pageSize);
        return R.ok(PageResult.build(pg.getRecords(), pg.getTotal()));
    }

    @SaCheckPermission("workflow:de:sy-log:list")
    @GetMapping("/operlistAll")
    public R<List<DeSyLogVo>> operListAll(DeSyLogQueryBo query) {
        return R.ok(logService.list(query));
    }

    @SaCheckPermission("workflow:de:sy-log:query")
    @GetMapping("/{operId}")
    public R<DeSyLogVo> getInfo(@PathVariable Long operId) {
        return R.ok(logService.getInfo(operId));
    }

    @Log(title = "操作日志", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:sy-log:remove")
    @DeleteMapping("/{operIds}")
    public R<Void> remove(@PathVariable Long[] operIds) {
        logService.remove(Arrays.copyOf(operIds, operIds.length, Long[].class));
        return R.ok();
    }

    /**
     * 清理操作日志（H-2 修复后 · 2026-09-23）。
     *
     * <p>权限两层校验：{@code @SaCheckPermission}（拥有 perm）→
     * service 层 {@code LoginHelper.isSuperAdmin()}（与底座 RuoYiConfig.isAdminAllowed 对齐）。
     *
     * <p>必须传 {@code cutoffDate}（yyyy-MM-dd HH:mm:ss），否则
     * service 抛 {@code ServiceException("cleanOperLog 必须传入 cutoffDate...")}。
     */
    @Log(title = "操作日志", businessType = BusinessType.CLEAN)
    @SaCheckPermission("workflow:de:sy-log:remove")
    @DeleteMapping("/clean")
    public R<Void> clean(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date cutoffDate) {
        logService.cleanOperLog(cutoffDate);
        return R.ok();
    }

    /** SnailJob 调度日志（HTTP 桥接 + fail-soft） */
    @SaCheckPermission("workflow:de:sy-log:query")
    @PostMapping("/snailjob-list")
    public R<Map<String, Object>> snailjobList(@RequestBody(required = false) Map<String, Object> query,
                                              @RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize) {
        return R.ok(snailJobLogHttpClient.listJobLog(query, pageNum, pageSize));
    }
}