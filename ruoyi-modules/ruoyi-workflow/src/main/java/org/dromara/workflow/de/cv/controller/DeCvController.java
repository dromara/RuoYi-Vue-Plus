package org.dromara.workflow.de.cv.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.cv.domain.bo.DeCvExecuteBo;
import org.dromara.workflow.de.cv.domain.vo.DeCvExecuteResultVo;
import org.dromara.workflow.de.cv.domain.vo.DeCvSessionVo;
import org.dromara.workflow.de.cv.domain.vo.DeCvStatusVo;
import org.dromara.workflow.de.cv.service.IDeCvService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 对话与执行 CV 域 REST 端点（29 号 §五 CV-01~CV-07 会话管理 / 执行触发 / 状态查询，共 3 端点）。
 *
 * <p>与 de-conversation 模块（/de/conversation 的 create / message / complete）分工：
 * 本域补会话列表（sai_agent_conversation）、执行触发与执行状态查询
 * （sai_agent_conversation_record.status ＋ 锚定实例 flow_instance.flow_status）。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/cv")
public class DeCvController extends BaseController {

    private final IDeCvService deCvService;

    /** 会话列表：GET /de/cv/sessions?userId=xxx&agentId= */
    @SaCheckPermission("workflow:de:cv:list")
    @GetMapping("/sessions")
    public R<List<DeCvSessionVo>> sessions(@RequestParam Long userId,
                                           @RequestParam(required = false) Long agentId) {
        return R.ok(deCvService.listSessions(userId, agentId));
    }

    /** CV-01 执行触发：POST /de/cv/execute（落 user 记录，status=3 进行中） */
    @SaCheckPermission("workflow:de:cv:execute")
    @PostMapping("/execute")
    public R<DeCvExecuteResultVo> execute(@Valid @RequestBody DeCvExecuteBo bo) {
        return R.ok(deCvService.execute(bo));
    }

    /** CV-02 执行状态查询：GET /de/cv/status/{conversationId} */
    @SaCheckPermission("workflow:de:cv:query")
    @GetMapping("/status/{conversationId}")
    public R<DeCvStatusVo> status(@PathVariable String conversationId) {
        return R.ok(deCvService.status(conversationId));
    }
}
