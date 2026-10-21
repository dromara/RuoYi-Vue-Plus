package org.dromara.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.domain.bo.FlowEventWaitBo;
import org.dromara.workflow.domain.bo.FlowTimerTriggerBo;
import org.dromara.workflow.domain.vo.FlowEventWaitVo;
import org.dromara.workflow.service.IFlwEventWaitService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 定时任务（事件等待记录）控制器。
 * <p>数据来源为引擎事件等待表，默认只查定时类事件；消息类 / 错误类等待需显式放开事件类型范围。
 * 写操作只有一项：对定时启动任务手动补发一次实例——补发不读取、也不推进等待记录，
 * 定时任务的下次触发时间不受影响。</p>
 *
 * @author may
 * @since 2026/10/21
 */
@ConditionalOnEnable
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/eventWait")
public class FlwEventWaitController extends BaseController {

    private final IFlwEventWaitService flwEventWaitService;

    /**
     * 分页查询定时任务（事件等待记录）列表。
     *
     * @param bo 查询条件
     * @return 等待记录分页数据
     */
    @SaCheckPermission("workflow:eventWait:list")
    @GetMapping("/list")
    public R<PageResult<FlowEventWaitVo>> list(FlowEventWaitBo bo) {
        return R.ok(flwEventWaitService.queryPageList(bo));
    }

    /**
     * 按同一组筛选条件统计各状态等待记录数量。
     *
     * @param bo 查询条件
     * @return 各状态计数
     */
    @SaCheckPermission("workflow:eventWait:list")
    @GetMapping("/summary")
    public R<Map<String, Long>> summary(FlowEventWaitBo bo) {
        return R.ok(flwEventWaitService.summary(bo));
    }

    /**
     * 查询定时任务详情。
     *
     * @param id 事件等待记录主键
     * @return 等待记录详情
     */
    @SaCheckPermission("workflow:eventWait:query")
    @GetMapping("/{id}")
    public R<FlowEventWaitVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return R.ok(flwEventWaitService.queryById(id));
    }

    /**
     * 手动补发一次定时启动任务（运维应急）。
     * <p>不推进等待记录，下一次定时触发照常进行；实例发起人记为当前登录用户。</p>
     *
     * @param bo 补发参数
     * @return 新建实例主键
     */
    @SaCheckPermission("workflow:eventWait:trigger")
    @Log(title = "定时任务手动补发", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/trigger")
    public R<Long> trigger(@RequestBody FlowTimerTriggerBo bo) {
        return R.ok(flwEventWaitService.manualTrigger(bo));
    }
}
