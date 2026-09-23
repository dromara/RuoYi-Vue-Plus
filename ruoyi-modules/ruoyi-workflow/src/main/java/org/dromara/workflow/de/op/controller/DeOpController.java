package org.dromara.workflow.de.op.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.op.domain.vo.DeOpAlertVo;
import org.dromara.workflow.de.op.domain.vo.DeOpConfigVo;
import org.dromara.workflow.de.op.domain.vo.DeOpMetricsVo;
import org.dromara.workflow.de.op.service.IDeOpService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 运营与调度 OP 域 REST 端点（29 号 §五 OP 域运营指标 / 告警 / 调度口径，共 3 端点）。
 *
 * <p>数据落点：sai_agent_usage_stat（复用现有表，按 stat_date 窗口聚合）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/op")
public class DeOpController extends BaseController {

    private final IDeOpService deOpService;

    /** OP-04 运营指标：GET /de/op/metrics?agentId=&userId=&days=（days 默认 7） */
    @SaCheckPermission("workflow:de:op:list")
    @GetMapping("/metrics")
    public R<DeOpMetricsVo> metrics(@RequestParam(required = false) Long agentId,
                                    @RequestParam(required = false) Long userId,
                                    @RequestParam(required = false, defaultValue = "7") Integer days) {
        return R.ok(deOpService.metrics(agentId, userId, days));
    }

    /** OP-03 / OP-04 告警：GET /de/op/alerts?days=（环比窗口对比，days 默认 7） */
    @SaCheckPermission("workflow:de:op:query")
    @GetMapping("/alerts")
    public R<List<DeOpAlertVo>> alerts(@RequestParam(required = false, defaultValue = "7") Integer days) {
        return R.ok(deOpService.alerts(days));
    }

    /** OP-05 / OP-06 调度口径读：GET /de/op/config（统计维度 + 实数据窗口） */
    @SaCheckPermission("workflow:de:op:config")
    @GetMapping("/config")
    public R<DeOpConfigVo> config() {
        return R.ok(deOpService.config());
    }
}
