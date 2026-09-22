package org.dromara.workflow.de.hc.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.hc.domain.bo.DeHcCollabRequestBo;
import org.dromara.workflow.de.hc.domain.bo.DeHcParticipantBo;
import org.dromara.workflow.de.hc.service.IDeHcService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * HC 域 REST 端点（HC-04 / HC-05 / HC-06 / HC-08 共 4 端点）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/hc")
public class DeHcController extends BaseController {

    private final IDeHcService deHcService;

    /** HC-04 协作请求 */
    @PostMapping("/collab")
    public R<Long> collabRequest(@Valid @RequestBody DeHcCollabRequestBo bo) {
        return R.ok("协作请求已派发", deHcService.collabRequest(bo));
    }

    /** HC-05 参与者加入 / 退出 / 替补 / 转派 */
    @PostMapping("/participant")
    public R<Long> participant(@Valid @RequestBody DeHcParticipantBo bo) {
        return R.ok("参与者已更新", deHcService.participant(bo));
    }

    /** HC-06 贡献流程分型 */
    @GetMapping("/contribution/{collabId}/status")
    public R<Map<String, Object>> contributionStatus(@PathVariable Long collabId) {
        return R.ok(deHcService.contributionStatus(collabId));
    }

    /** HC-08 节点展示参与者 / 分工 / Agent 授权摘要 */
    @GetMapping("/node/{taskId}/participants")
    public R<Map<String, Object>> nodeParticipants(@PathVariable Long taskId) {
        return R.ok(deHcService.nodeParticipants(taskId));
    }
}