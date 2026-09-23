package org.dromara.workflow.de.mm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.de.mm.domain.vo.DeMmConfigVo;
import org.dromara.workflow.de.mm.domain.vo.DeMmConversationDetailVo;
import org.dromara.workflow.de.mm.domain.vo.DeMmMemoryVo;
import org.dromara.workflow.de.mm.service.IDeMmService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 记忆与经验 MM 域 REST 端点（29 号 §五 MM-01~MM-07 记忆检索 / 上下文回溯 / 记忆配置，共 3 端点）。
 *
 * <p>数据落点：sai_agent_conversation_record（按 conversation_id 检索历史 + 摘要）；
 * 记忆开关只读引用 sai_agent.memory_enabled。
 *
 * @author AIP
 */
@ConditionalOnEnable
@RequiredArgsConstructor
@RestController
@RequestMapping("/de/mm")
public class DeMmController extends BaseController {

    private final IDeMmService deMmService;

    /** MM-01 / MM-02：记忆检索。 GET /de/mm/list?userId=xxx&agentId=&conversationId=&role=&keyword= */
    @SaCheckPermission("workflow:de:mm:list")
    @GetMapping("/list")
    public R<List<DeMmMemoryVo>> list(@RequestParam Long userId,
                                      @RequestParam(required = false) Long agentId,
                                      @RequestParam(required = false) String conversationId,
                                      @RequestParam(required = false) String role,
                                      @RequestParam(required = false) String keyword) {
        return R.ok(deMmService.listMemories(userId, agentId, conversationId, role, keyword));
    }

    /** MM-03 / MM-04：上下文回溯 —— 单会话全量历史 + 摘要。 GET /de/mm/detail/{conversationId} */
    @SaCheckPermission("workflow:de:mm:query")
    @GetMapping("/detail/{conversationId}")
    public R<DeMmConversationDetailVo> detail(@PathVariable String conversationId) {
        return R.ok(deMmService.conversationDetail(conversationId));
    }

    /** MM-01：记忆配置读 —— 记忆开关 + 载荷覆盖统计。 GET /de/mm/config/{agentId} */
    @SaCheckPermission("workflow:de:mm:config")
    @GetMapping("/config/{agentId}")
    public R<DeMmConfigVo> config(@PathVariable Long agentId) {
        return R.ok(deMmService.memoryConfig(agentId));
    }
}
