package org.dromara.workflow.de.nc.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.nc.domain.bo.DeNcNoticeBo;
import org.dromara.workflow.de.nc.domain.bo.DeNcNoticeQueryBo;
import org.dromara.workflow.de.nc.domain.vo.DeNcNoticeVo;
import org.dromara.workflow.de.nc.service.IDeNcNoticeService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * NC-04 通知中心 CRUD REST 端点（2026-09-23）。
 *
 * <p>路径：{@code /de/nc/notice/*} —— 与现有 NC-05/06/08 端点
 * （{@link DeNcController}，{@code /de/nc/scope|exec-mode|diff}）路径不重叠，
 * Spring 路由按子路径分发无冲突。
 *
 * <p>真源：{@code sys_notice} 表 + 镜像实体
 * {@link org.dromara.workflow.de.domain.DeSysNotice}。
 *
 * <p>端点（6 个）：
 * <ul>
 *   <li>GET    /de/nc/notice/list      —— 分页</li>
 *   <li>GET    /de/nc/notice/listAll   —— 全量（不带分页）</li>
 *   <li>GET    /de/nc/notice/{id}      —— 详情</li>
 *   <li>POST   /de/nc/notice           —— 新增</li>
 *   <li>PUT    /de/nc/notice           —— 更新</li>
 *   <li>DELETE /de/nc/notice/{ids}     —— 批量删除</li>
 * </ul>
 *
 * @author AIP
 * @since 2026-09-23
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/nc/notice")
public class DeNcFourController extends BaseController {

    private final IDeNcNoticeService noticeService;

    /** 分页查询 */
    @SaCheckPermission("workflow:de:nc:list")
    @GetMapping("/list")
    public R<PageResult<DeNcNoticeVo>> list(DeNcNoticeQueryBo query,
                                            @RequestParam(defaultValue = "1") Long pageNum,
                                            @RequestParam(defaultValue = "10") Long pageSize) {
        com.baomidou.mybatisplus.core.metadata.IPage<DeNcNoticeVo> pg =
            noticeService.page(query, pageNum, pageSize);
        return R.ok(PageResult.build(pg.getRecords(), pg.getTotal()));
    }

    /** 全量列表 */
    @SaCheckPermission("workflow:de:nc:list")
    @GetMapping("/listAll")
    public R<List<DeNcNoticeVo>> listAll(DeNcNoticeQueryBo query) {
        return R.ok(noticeService.list(query));
    }

    /** 详情 */
    @SaCheckPermission("workflow:de:nc:query")
    @GetMapping("/{noticeId}")
    public R<DeNcNoticeVo> getInfo(@PathVariable Long noticeId) {
        return R.ok(noticeService.getInfo(noticeId));
    }

    /** 新增 */
    @Log(title = "通知中心", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:de:nc:add")
    @PostMapping
    public R<Long> add(@Valid @RequestBody DeNcNoticeBo bo) {
        return R.ok("通知已新增", noticeService.add(bo));
    }

    /** 更新 */
    @Log(title = "通知中心", businessType = BusinessType.UPDATE)
    @SaCheckPermission("workflow:de:nc:edit")
    @PutMapping
    public R<Void> edit(@Valid @RequestBody DeNcNoticeBo bo) {
        noticeService.edit(bo);
        return R.ok();
    }

    /** 批量删除 */
    @Log(title = "通知中心", businessType = BusinessType.DELETE)
    @SaCheckPermission("workflow:de:nc:remove")
    @DeleteMapping("/{noticeIds}")
    public R<Void> remove(@PathVariable Long[] noticeIds) {
        noticeService.remove(Arrays.copyOf(noticeIds, noticeIds.length, Long[].class));
        return R.ok();
    }
}