package org.dromara.workflow.de.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.workflow.de.domain.SaiAgentConversation;
import org.dromara.workflow.de.domain.SaiAgentConversationRecord;
import org.dromara.workflow.de.mapper.SaiAgentConversationMapper;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.dromara.warm.flow.orm.mapper.FlowTaskMapper;
import org.dromara.warm.flow.orm.mapper.FlowUserMapper;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * T-3 原文可见性判定器（AU-03 方案 A · 2026-09-22 顺序推进批③ · owner 全量自决授权）。
 *
 * <p>34 号 §2.2 T-3：正文（sai_agent_conversation_record.content 等）必须**逐对象判**，
 * 规则 = 「任务关系 + 在本次实例内产生」双条件同时成立：
 * <ul>
 *   <li>属主（S-05 语义）⇒ 直接放行</li>
 *   <li>否则：对话锚定实例（D-009 instance_id）→ 任务关系三支任一成立即放行：
 *     ① 待办：flow_user.associated ∈ 实例的 flow_task.id 且 processed_by = 主体
 *     ② 已办：flow_his_task.approver = 主体（该实例下）
 *     ③ 发起：flow_instance.create_by = 主体用户名</li>
 *   <li>无锚定（历史会话）⇒ 无法判「实例内」⇒ 拒绝</li>
 * </ul>
 *
 * <p>方案 B（对象级 ACL 泛化位表）另见设计稿
 * {@code docs/设计包/AU-03-ACL设计稿-2026-09-22.md}——须 owner 特批 G-2「不新造表」红线后另行实施。
 *
 * @author AIP
 * @since 2026-09-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeAccessService {

    private final SaiAgentConversationMapper saiAgentConversationMapper;
    private final FlowTaskMapper flowTaskMapper;
    private final FlowUserMapper flowUserMapper;
    private final FlowHisTaskMapper flowHisTaskMapper;
    private final FlwInstanceMapper flwInstanceMapper;
    private final ISaiObjectAclService saiObjectAclService;

    /**
     * T-3 判定：当前登录用户能否读该对话记录的正文。
     */
    public boolean canReadConversationRecord(SaiAgentConversationRecord record) {
        if (record == null) {
            return false;
        }
        Long userId = LoginHelper.getUserId();
        // ① 属主（S-05）
        if (record.getUserId() != null && record.getUserId().equals(userId)) {
            return true;
        }
        // ★ D-011（AU-03 方案 B）：对象级 ACL 命中即放行（加宽不替换）
        if (saiObjectAclService.hasAccess("conversation_record", record.getId(), userId, "read", null)) {
            log.info("[T3] ACL hit: record={} userId={}", record.getId(), userId);
            return true;
        }
        // ② T-3 双条件：实例内（锚定）+ 任务关系
        SaiAgentConversation conv = saiAgentConversationMapper.selectOne(
            new LambdaQueryWrapper<SaiAgentConversation>()
                .eq(SaiAgentConversation::getConversationId, record.getConversationId())
                .last("LIMIT 1"));
        log.info("[T3] recordId={} convId={} conv={} anchored={}",
            record.getId(), record.getConversationId(),
            conv == null ? "null" : conv.getId(), conv == null ? "null" : conv.getInstanceId());
        if (conv == null || conv.getInstanceId() == null) {
            return false;
        }
        boolean rel = hasTaskRelation(conv.getInstanceId(), userId, LoginHelper.getUsername());
        log.info("[T3] instanceId={} userId={} relation={}", conv.getInstanceId(), userId, rel);
        return rel;
    }

    /**
     * 任务关系三支（34 号 §2.3 ③）：待办 / 已办 / 发起，任一成立即真。
     */
    private boolean hasTaskRelation(Long instanceId, Long userId, String username) {
        String uid = String.valueOf(userId);
        // ① 待办：实例的当前任务上有该主体的办理权
        List<FlowTask> tasks = flowTaskMapper.selectList(
            new LambdaQueryWrapper<FlowTask>().eq(FlowTask::getInstanceId, instanceId));
        log.info("[T3] tasks(instance={}) = {}", instanceId, tasks == null ? 0 : tasks.size());
        if (tasks != null && !tasks.isEmpty()) {
            Long cnt = flowUserMapper.selectCount(new LambdaQueryWrapper<FlowUser>()
                .in(FlowUser::getAssociated, tasks.stream().map(FlowTask::getId).toList())
                .eq(FlowUser::getProcessedBy, uid));
            log.info("[T3] todo cnt(uid={}) = {}", uid, cnt);
            if (cnt != null && cnt > 0) {
                return true;
            }
        }
        // ② 已办：该实例的历史任务 approver = 主体
        Long done = flowHisTaskMapper.selectCount(new LambdaQueryWrapper<org.dromara.warm.flow.orm.entity.FlowHisTask>()
            .eq(org.dromara.warm.flow.orm.entity.FlowHisTask::getInstanceId, instanceId)
            .eq(org.dromara.warm.flow.orm.entity.FlowHisTask::getApprover, uid));
        if (done != null && done > 0) {
            return true;
        }
        // ③ 发起：实例 create_by = 主体用户名（列型实测 varchar）
        if (StrUtil.isNotBlank(username)) {
            FlowInstance inst = flwInstanceMapper.selectById(instanceId);
            if (inst != null && username.equals(inst.getCreateBy())) {
                return true;
            }
        }
        return false;
    }
}
