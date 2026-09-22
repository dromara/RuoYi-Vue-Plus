package org.dromara.workflow.de.br.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.de.br.domain.bo.DeAgentStatusBo;
import org.dromara.workflow.de.br.domain.bo.DeSkipCompensationBo;
import org.dromara.workflow.de.br.domain.bo.DeHisTaskTraceBo;
import org.dromara.workflow.de.br.domain.bo.DeAgentConfigBo;
import org.dromara.workflow.de.br.domain.bo.DeDefinitionInheritBo;
import org.dromara.workflow.de.br.domain.bo.DeExecutionModeBo;
import org.dromara.workflow.de.br.domain.bo.DeLifecycleStageBo;
import org.dromara.workflow.de.br.domain.bo.DeConvRecordMetaBo;
import org.dromara.workflow.de.br.domain.bo.DeSkillVersionDiffBo;
import org.dromara.workflow.de.br.domain.bo.DeSkillUpdateBo;
import org.dromara.workflow.de.br.domain.bo.DeSkipTypeBo;
import org.dromara.workflow.de.br.domain.vo.BrComplianceAckVo;
import org.dromara.workflow.de.domain.FlowDefinition;
import org.dromara.workflow.de.domain.FlowSkip;
import org.dromara.workflow.de.domain.SaiAgent;
import org.dromara.workflow.de.domain.SaiAgentConversationRecord;
import org.dromara.workflow.de.domain.SaiAgentSkill;
import org.dromara.workflow.de.domain.SaiSkill;
import org.dromara.workflow.de.mapper.FlowDefinitionMapper;
import org.dromara.workflow.de.mapper.FlowSkipMapper;
import org.dromara.workflow.de.mapper.SaiAgentConversationRecordMapper;
import org.dromara.workflow.de.mapper.SaiAgentMapper;
import org.dromara.workflow.de.mapper.SaiAgentSkillMapper;
import org.dromara.workflow.de.mapper.SaiSkillMapper;
import org.dromara.workflow.mapper.FlwHisTaskMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 11 条 BR 应用层校验 service 实现。
 *
 * <p><b>单一真源</b>：`docs/03号业务环节实施清单-2026-09-18.md` §四（BR × N-XX 映射）。
 *
 * <p><b>硬约束</b>（沿用 `docs/CLAUDE.md`）：
 * <ul>
 *   <li>不动底座 schema</li>
 *   <li>不动底座正文</li>
 *   <li>不新造表 / 不发明列</li>
 *   <li>本类方法<b>不挂 endpoint</b>，owner 接 sys_menu 后再挂</li>
 * </ul>
 *
 * <p><b>DB 访问策略</b>：所有 BR 校验都走 de/mapper/ 下的薄 mapper（与 SaiAgentMapper 同模式）；
 * 不为底层已有表新增 mapper 接口外的方法，所有校验都是<b>只读 SELECT</b>，不写库。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrComplianceServiceImpl implements IBrComplianceService {

    // ===== 物理枚举（sai_agent.status DDL 注释：04-backend-code/script/sql/ry_ai.sql:153）=====
    private static final int SAI_AGENT_STATUS_ACTIVE = 1;
    private static final int SAI_AGENT_STATUS_INACTIVE = 2;
    private static final int SAI_AGENT_STATUS_DEPRECATED = 3;
    private static final int SAI_AGENT_STATUS_DISABLED = 4;

    // ===== 物理枚举（flow_task.node_type：04-backend-code/script/sql/ry_workflow.sql:104）=====
    private static final int FLOW_NODE_TYPE_START = 0;
    private static final int FLOW_NODE_TYPE_MIDDLE = 1;
    private static final int FLOW_NODE_TYPE_EXCLUSIVE_GATEWAY = 3;

    // ===== 物理枚举（flow_skip.skip_type：04-backend-code/script/sql/ry_workflow.sql:63）=====
    private static final String SKIP_TYPE_PASS = "PASS";
    private static final String SKIP_TYPE_REJECT = "REJECT";

    // ===== 物理枚举（flow_definition.model_value：04-backend-code/script/sql/ry_workflow.sql:9）=====
    private static final String MODEL_VALUE_CLASSICS = "CLASSICS";
    private static final String MODEL_VALUE_MIMIC = "MIMIC";

    // ===== BR-08 / BR-06 metadata 键命名空间 =====
    private static final String META_SCOPE = "de.scope";
    private static final String META_PINNED_SKILL_VERSION = "de.pinned_skill_version";
    private static final String SCOPE_PERSONAL = "PERSONAL";
    private static final String SCOPE_TEAM = "TEAM";
    private static final String SCOPE_PROJECT = "PROJECT";

    // ===== BR-18 消息前缀 =====
    private static final String MESSAGE_PREFIX_COMPENSATE = "[COMPENSATE]";
    private static final String MESSAGE_PREFIX_CANCEL = "[CANCEL]";

    // ===== BR-05 影响面上限（应用层强制）=====
    private static final int MAX_AFFECTED_AGENTS = 50;

    // ===== BR-03 payload 长度上限（sai_agent.config TEXT 上限）=====
    private static final int MAX_CONFIG_PAYLOAD_LENGTH = 65535;

    // ===== BR 编号常量 =====
    private static final String BR_ID_01 = "BR-01";
    private static final String BR_ID_03 = "BR-03";
    private static final String BR_ID_04 = "BR-04";
    private static final String BR_ID_05 = "BR-05";
    private static final String BR_ID_06 = "BR-06";
    private static final String BR_ID_08 = "BR-08";
    private static final String BR_ID_09 = "BR-09";
    private static final String BR_ID_15 = "BR-15";
    private static final String BR_ID_16 = "BR-16";
    private static final String BR_ID_17 = "BR-17";
    private static final String BR_ID_18 = "BR-18";

    private final SaiAgentMapper saiAgentMapper;
    private final SaiSkillMapper saiSkillMapper;
    private final SaiAgentSkillMapper saiAgentSkillMapper;
    private final SaiAgentConversationRecordMapper saiAgentConversationRecordMapper;
    private final FlowSkipMapper flowSkipMapper;
    private final FlowDefinitionMapper flowDefinitionMapper;
    private final FlwTaskMapper flwTaskMapper;
    private final FlwHisTaskMapper flwHisTaskMapper;
    private final ObjectMapper objectMapper;

    // =====================================================================
    // BR-01 / BR-03 / BR-04 —— sai_agent 主线
    // =====================================================================

    @Override
    public BrComplianceAckVo enforceAgentStatusTransition(DeAgentStatusBo bo) {
        SaiAgent agent = saiAgentMapper.selectById(bo.getAgentId());
        if (agent == null) {
            throw new ServiceException("数字员工不存在: id=" + bo.getAgentId());
        }

        if (!agent.getStatus().equals(bo.getFromStatus())) {
            throw new ServiceException(
                BR_ID_01 + " 状态机校验：fromStatus(" + bo.getFromStatus() + ") 与当前 sai_agent.status("
                    + agent.getStatus() + ") 不一致（stale read）");
        }

        Set<String> allowed = new HashSet<>();
        allowed.add(SAI_AGENT_STATUS_ACTIVE + "->" + SAI_AGENT_STATUS_INACTIVE);
        allowed.add(SAI_AGENT_STATUS_ACTIVE + "->" + SAI_AGENT_STATUS_DEPRECATED);
        allowed.add(SAI_AGENT_STATUS_ACTIVE + "->" + SAI_AGENT_STATUS_DISABLED);
        allowed.add(SAI_AGENT_STATUS_INACTIVE + "->" + SAI_AGENT_STATUS_ACTIVE);
        allowed.add(SAI_AGENT_STATUS_DEPRECATED + "->" + SAI_AGENT_STATUS_ACTIVE);
        allowed.add(SAI_AGENT_STATUS_DISABLED + "->" + SAI_AGENT_STATUS_ACTIVE);
        String key = bo.getFromStatus() + "->" + bo.getToStatus();
        if (!allowed.contains(key)) {
            throw new ServiceException(
                BR_ID_01 + " 状态机越界：sai_agent.status " + key + " 不在合法转移集 " + allowed);
        }
        if (StrUtil.isBlank(bo.getReason())) {
            throw new ServiceException(BR_ID_01 + " reason 必填");
        }

        BrComplianceAckVo ack = new BrComplianceAckVo();
        ack.setBrId(BR_ID_01);
        ack.setValidated(true);
        ack.setDescription("sai_agent.status " + key + " 合法；待 caller 落库");
        ack.getExtras().put("fromStatus", bo.getFromStatus());
        ack.getExtras().put("toStatus", bo.getToStatus());
        log.info("[br.01] agent={} {} ok", bo.getAgentId(), key);
        return ack;
    }

    @Override
    public BrComplianceAckVo validateConfigPackageImportExport(DeAgentConfigBo bo) {
        SaiAgent agent = saiAgentMapper.selectById(bo.getAgentId());
        if (agent == null) {
            throw new ServiceException("数字员工不存在: id=" + bo.getAgentId());
        }

        String op = bo.getOperation();
        if (!Arrays.asList("IMPORT", "EXPORT", "CLONE", "SHARE").contains(op)) {
            throw new ServiceException(BR_ID_03 + " operation 越界: " + op);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(bo.getPayload());
        } catch (Exception e) {
            throw new ServiceException(BR_ID_03 + " payload 不是合法 JSON: " + e.getMessage());
        }

        if (("IMPORT".equals(op) || "CLONE".equals(op)) && root.get("import_meta") == null) {
            throw new ServiceException(BR_ID_03 + " " + op + " 必须含顶层键 import_meta");
        }
        if (("EXPORT".equals(op) || "SHARE".equals(op)) && root.get("export_meta") == null) {
            throw new ServiceException(BR_ID_03 + " " + op + " 必须含顶层键 export_meta");
        }
        if (bo.getPayload().length() > MAX_CONFIG_PAYLOAD_LENGTH) {
            throw new ServiceException(
                BR_ID_03 + " payload 长度 " + bo.getPayload().length()
                    + " 超 TEXT 上限 " + MAX_CONFIG_PAYLOAD_LENGTH);
        }

        String lower = bo.getPayload().toLowerCase();
        for (String secret : Arrays.asList("password", "token", "secret", "apikey", "api_key")) {
            String patternKey = "\"" + secret + "\"";
            String patternAssign = secret + "=";
            if (lower.contains(patternKey) || lower.contains(patternAssign)) {
                throw new ServiceException(
                    BR_ID_03 + " 配置包疑似含个人凭据关键字: " + secret + "（禁止裸导出）");
            }
        }

        BrComplianceAckVo ack = new BrComplianceAckVo();
        ack.setBrId(BR_ID_03);
        ack.setValidated(true);
        ack.setDescription("sai_agent.config " + op + " 形状合法");
        ack.getExtras().put("operation", op);
        ack.getExtras().put("payloadLength", bo.getPayload().length());
        log.info("[br.03] agent={} op={} len={} ok", bo.getAgentId(), op, bo.getPayload().length());
        return ack;
    }

    @Override
    public BrComplianceAckVo enforceAgentLifecycleStatus(DeLifecycleStageBo bo) {
        SaiAgent agent = saiAgentMapper.selectById(bo.getAgentId());
        if (agent == null) {
            throw new ServiceException("数字员工不存在: id=" + bo.getAgentId());
        }

        Map<String, Integer> stageToStatus = Map.of(
            "DRAFT", SAI_AGENT_STATUS_ACTIVE,
            "TEST", SAI_AGENT_STATUS_ACTIVE,
            "AVAILABLE", SAI_AGENT_STATUS_ACTIVE,
            "DISABLE", SAI_AGENT_STATUS_DISABLED,
            "ARCHIVE", SAI_AGENT_STATUS_DEPRECATED,
            "RESTORE", SAI_AGENT_STATUS_ACTIVE,
            "REPLACE", SAI_AGENT_STATUS_ACTIVE
        );
        Integer targetStatus = stageToStatus.get(bo.getStage());
        if (targetStatus == null) {
            throw new ServiceException(BR_ID_04 + " stage 越界: " + bo.getStage());
        }

        Integer current = agent.getStatus();
        if (StrUtil.isBlank(bo.getReason())) {
            throw new ServiceException(BR_ID_04 + " reason 必填");
        }
        if (SAI_AGENT_STATUS_DEPRECATED == current && "RESTORE".equals(bo.getStage())) {
            throw new ServiceException(
                BR_ID_04 + " ARCHIVE 不可直接 RESTORE（须先经 DISABLE 走流程 4→1）");
        }

        BrComplianceAckVo ack = new BrComplianceAckVo();
        ack.setBrId(BR_ID_04);
        ack.setValidated(true);
        ack.setDescription(
            "sai_agent.status 阶段 " + bo.getStage() + " → 物理值 " + targetStatus + " 合法");
        ack.getExtras().put("stage", bo.getStage());
        ack.getExtras().put("currentStatus", current);
        ack.getExtras().put("targetStatus", targetStatus);
        log.info("[br.04] agent={} stage={} {}→{} ok",
            bo.getAgentId(), bo.getStage(), current, targetStatus);
        return ack;
    }

    // =====================================================================
    // BR-05 / BR-06 / BR-08 —— sai_skill / sai_agent_conversation_record 主线
    // =====================================================================

    @Override
    public BrComplianceAckVo enforceSkillUpdateDependencies(DeSkillUpdateBo bo) {
        SaiSkill skill = saiSkillMapper.selectById(bo.getSkillId());
        if (skill == null) {
            throw new ServiceException(BR_ID_05 + " sai_skill 不存在: id=" + bo.getSkillId());
        }
        Long currentVersion = skill.getVersion() == null ? 0L : skill.getVersion();

        String op = bo.getOperation();
        if ("UPDATE".equals(op) && bo.getTargetVersion() <= currentVersion) {
            throw new ServiceException(
                BR_ID_05 + " UPDATE 必须单调递增：current=" + currentVersion
                    + " target=" + bo.getTargetVersion());
        }
        if ("ROLLBACK".equals(op) && bo.getTargetVersion() >= currentVersion) {
            throw new ServiceException(
                BR_ID_05 + " ROLLBACK 必须 < current：current=" + currentVersion
                    + " target=" + bo.getTargetVersion());
        }

        List<SaiAgentSkill> deps = saiAgentSkillMapper.selectList(
            new LambdaQueryWrapper<SaiAgentSkill>().eq(SaiAgentSkill::getSkillId, bo.getSkillId()));
        int affectedAgentCount = deps.size();
        if (affectedAgentCount > MAX_AFFECTED_AGENTS) {
            throw new ServiceException(
                BR_ID_05 + " 影响 Agent 数 " + affectedAgentCount
                    + " 超过上限 " + MAX_AFFECTED_AGENTS + "（防止不可控回滚）");
        }

        BrComplianceAckVo ack = new BrComplianceAckVo();
        ack.setBrId(BR_ID_05);
        ack.setValidated(true);
        ack.setDescription(
            "sai_skill.version " + op + " current=" + currentVersion
                + "→target=" + bo.getTargetVersion() + " 合法");
        ack.getExtras().put("currentVersion", currentVersion);
        ack.getExtras().put("targetVersion", bo.getTargetVersion());
        ack.getExtras().put("affectedAgentCount", affectedAgentCount);
        log.info("[br.05] skill={} {} v{}→v{} deps={} ok",
            bo.getSkillId(), op, currentVersion, bo.getTargetVersion(), affectedAgentCount);
        return ack;
    }

    @Override
    public BrComplianceAckVo enforceMemoryScope(DeConvRecordMetaBo bo) {
        String scope = bo.getScope();
        if (!Arrays.asList(SCOPE_PERSONAL, SCOPE_TEAM, SCOPE_PROJECT).contains(scope)) {
            throw new ServiceException(BR_ID_06 + " scope 越界: " + scope);
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(bo.getMetadataJson());
        } catch (Exception e) {
            throw new ServiceException(BR_ID_06 + " metadataJson 不是合法 JSON: " + e.getMessage());
        }
        JsonNode patched = root.deepCopy();
        ((ObjectNode) patched).put(META_SCOPE, scope);

        if (SCOPE_PERSONAL.equals(scope)) {
            if (root.has("team_id") || root.has("project_id")) {
                throw new ServiceException(
                    BR_ID_06 + " PERSONAL scope 不得带 team_id / project_id 字段");
            }
        }

        BrComplianceAckVo ack = new BrComplianceAckVo();
        ack.setBrId(BR_ID_06);
        ack.setValidated(true);
        ack.setDescription("sai_agent_conversation_record.metadata de.scope=" + scope + " 合法");
        ack.getExtras().put("scope", scope);
        ack.getExtras().put("scopedMetadataJson", patched.toString());
        log.info("[br.06] record={} scope={} ok", bo.getRecordId(), scope);
        return ack;
    }

    @Override
    public BrComplianceAckVo enforceMemorySkillVersionDiff(DeSkillVersionDiffBo bo) {
        SaiSkill skill = saiSkillMapper.selectById(bo.getSkillId());
        if (skill == null) {
            throw new ServiceException(BR_ID_08 + " sai_skill 不存在: id=" + bo.getSkillId());
        }
        Long currentVersion = skill.getVersion() == null ? 0L : skill.getVersion();

        SaiAgentConversationRecord rec = saiAgentConversationRecordMapper.selectById(bo.getRecordId());
        if (rec == null) {
            throw new ServiceException(
                BR_ID_08 + " sai_agent_conversation_record 不存在: id=" + bo.getRecordId());
        }
        String meta = rec.getMetadata();
        Long pinnedVersion = null;
        if (StrUtil.isNotBlank(meta)) {
            try {
                JsonNode root = objectMapper.readTree(meta);
                JsonNode pinNode = root.get(META_PINNED_SKILL_VERSION);
                if (pinNode != null && pinNode.isNumber()) {
                    pinnedVersion = pinNode.asLong();
                }
            } catch (Exception e) {
                throw new ServiceException(BR_ID_08 + " record metadata 不可解析: " + e.getMessage());
            }
        }

        boolean conflict = pinnedVersion != null && !pinnedVersion.equals(currentVersion);
        String op = bo.getOperation();
        List<String> conflicts = new ArrayList<>();
        String rollbackHint = null;
        if (conflict) {
            conflicts.add("pinned=" + pinnedVersion + " current=" + currentVersion);
            if ("ROLLBACK".equals(op)) {
                rollbackHint = "ROLLBACK skill v" + currentVersion + "→v" + pinnedVersion
                    + " 后，需同步刷新 metadata.de.pinned_skill_version 为 " + pinnedVersion;
            } else {
                rollbackHint = "MERGE：将 pinned=" + pinnedVersion + " 升级到 current="
                    + currentVersion + "（须 owner 确认是否要同步覆盖 memory）";
            }
        }

        BrComplianceAckVo ack = new BrComplianceAckVo();
        ack.setBrId(BR_ID_08);
        ack.setValidated(!conflict || "DIFF".equals(op));
        ack.setDescription("DIFF 完成：conflict=" + conflict);
        ack.getExtras().put("pinnedVersion", pinnedVersion);
        ack.getExtras().put("currentVersion", currentVersion);
        ack.getExtras().put("conflicts", conflicts);
        ack.getExtras().put("rollbackHint", rollbackHint);
        log.info("[br.08] record={} skill={} pinned={} current={} conflict={}",
            bo.getRecordId(), bo.getSkillId(), pinnedVersion, currentVersion, conflict);
        return ack;
    }

    // =====================================================================
    // BR-09 —— flow_task 主线
    // =====================================================================

    @Override
    public BrComplianceAckVo enforceExecutionMode(DeExecutionModeBo bo) {
        FlowTask task = flwTaskMapper.selectById(bo.getTaskId());
        if (task == null) {
            throw new ServiceException("flow_task 不存在: id=" + bo.getTaskId());
        }
        Integer nodeType = task.getNodeType();

        String mode = bo.getMode();
        Integer resolvedNodeType;
        switch (mode) {
            case "AUTO":
                if (nodeType != FLOW_NODE_TYPE_START && nodeType != FLOW_NODE_TYPE_MIDDLE) {
                    throw new ServiceException(
                        BR_ID_09 + " AUTO 模式要求 flow_task.node_type ∈ {0,1}，当前=" + nodeType);
                }
                resolvedNodeType = nodeType;
                break;
            case "MANUAL":
                if (nodeType != FLOW_NODE_TYPE_MIDDLE) {
                    throw new ServiceException(
                        BR_ID_09 + " MANUAL 模式要求 flow_task.node_type=1（中间节点），当前=" + nodeType);
                }
                resolvedNodeType = FLOW_NODE_TYPE_MIDDLE;
                break;
            case "ASSIST":
                if (nodeType != FLOW_NODE_TYPE_MIDDLE
                    && nodeType != FLOW_NODE_TYPE_EXCLUSIVE_GATEWAY) {
                    throw new ServiceException(
                        BR_ID_09 + " ASSIST 模式要求 flow_task.node_type ∈ {1,3}，当前=" + nodeType);
                }
                resolvedNodeType = nodeType;
                break;
            default:
                throw new ServiceException(BR_ID_09 + " mode 越界: " + mode);
        }

        BrComplianceAckVo ack = new BrComplianceAckVo();
        ack.setBrId(BR_ID_09);
        ack.setValidated(true);
        ack.setDescription("flow_task.node_type=" + nodeType + " 兼容 mode=" + mode);
        ack.getExtras().put("mode", mode);
        ack.getExtras().put("currentNodeType", nodeType);
        ack.getExtras().put("resolvedNodeType", resolvedNodeType);
        log.info("[br.09] task={} mode={} nodeType={} ok", bo.getTaskId(), mode, nodeType);
        return ack;
    }

    // =====================================================================
    // BR-15 / BR-16 / BR-18 —— flow_skip 主线
    // =====================================================================

    @Override
    public BrComplianceAckVo enforceSkipTypeAtDefinition(DeSkipTypeBo bo) {
        String op = bo.getOperation();
        if (!Arrays.asList("MODIFY", "DELETE", "REVOKE", "BACKUP").contains(op)) {
            throw new ServiceException(BR_ID_15 + " operation 越界: " + op);
        }

        String resolved;
        switch (op) {
            case "MODIFY":
                if (!SKIP_TYPE_PASS.equals(bo.getSkipType())) {
                    throw new ServiceException(BR_ID_15 + " MODIFY 必须 skip_type=PASS，当前=" + bo.getSkipType());
                }
                resolved = SKIP_TYPE_PASS;
                break;
            case "DELETE":
                if (!SKIP_TYPE_REJECT.equals(bo.getSkipType())) {
                    throw new ServiceException(BR_ID_15 + " DELETE 必须 skip_type=REJECT，当前=" + bo.getSkipType());
                }
                if (StrUtil.isBlank(bo.getReason())) {
                    throw new ServiceException(BR_ID_15 + " DELETE 必须填 reason");
                }
                resolved = SKIP_TYPE_REJECT;
                break;
            case "REVOKE":
                if (!SKIP_TYPE_REJECT.equals(bo.getSkipType())) {
                    throw new ServiceException(BR_ID_15 + " REVOKE 必须 skip_type=REJECT，当前=" + bo.getSkipType());
                }
                if (StrUtil.isBlank(bo.getReason())) {
                    throw new ServiceException(BR_ID_15 + " REVOKE 必须填 reason");
                }
                resolved = SKIP_TYPE_REJECT;
                break;
            case "BACKUP":
                if (StrUtil.isBlank(bo.getReason())) {
                    throw new ServiceException(BR_ID_15 + " BACKUP 必须填 reason（审计追溯用）");
                }
                resolved = bo.getSkipType();
                break;
            default:
                throw new ServiceException(BR_ID_15 + " operation 不可达: " + op);
        }

        BrComplianceAckVo ack = new BrComplianceAckVo();
        ack.setBrId(BR_ID_15);
        ack.setValidated(true);
        ack.setDescription("flow_skip.skip_type=" + resolved + " 兼容 operation=" + op);
        ack.getExtras().put("operation", op);
        ack.getExtras().put("resolvedSkipType", resolved);
        log.info("[br.15] def={} op={} skipType={} ok",
            bo.getDefinitionId(), op, resolved);
        return ack;
    }

    @Override
    public BrComplianceAckVo enforceCancelRollbackCompensation(DeSkipCompensationBo bo) {
        String op = bo.getOperation();
        if (!Arrays.asList("CANCEL", "ROLLBACK", "COMPENSATE").contains(op)) {
            throw new ServiceException(BR_ID_16 + " operation 越界: " + op);
        }
        if (StrUtil.isBlank(bo.getReason())) {
            throw new ServiceException(BR_ID_16 + " reason 必填");
        }

        String resolved;
        boolean messageRequired = false;
        switch (op) {
            case "CANCEL":
                resolved = SKIP_TYPE_REJECT;
                break;
            case "ROLLBACK":
                resolved = SKIP_TYPE_REJECT;
                break;
            case "COMPENSATE":
                resolved = SKIP_TYPE_PASS;
                messageRequired = true;
                break;
            default:
                throw new ServiceException(BR_ID_16 + " operation 不可达: " + op);
        }

        // BR-16 仅校验，不强制存在 flow_skip 行（实例可尚未建立 flow_skip）；落库由 caller 负责
        // 实例存在性校验留待 P3 caller 决定（此处不引入新依赖 flwInstanceMapper）

        BrComplianceAckVo ack = new BrComplianceAckVo();
        ack.setBrId(BR_ID_16);
        ack.setValidated(true);
        ack.setDescription("flow_skip.skip_type=" + resolved + " 兼容 operation=" + op);
        ack.getExtras().put("operation", op);
        ack.getExtras().put("resolvedSkipType", resolved);
        ack.getExtras().put("messageRequired", messageRequired);
        log.info("[br.16] instance={} op={} skipType={} msgReq={} ok",
            bo.getInstanceId(), op, resolved, messageRequired);
        return ack;
    }

    @Override
    public BrComplianceAckVo validateDefinitionModelInheritance(DeDefinitionInheritBo bo) {
        FlowDefinition srcDef = flowDefinitionMapper.selectById(bo.getSourceDefinitionId());
        if (srcDef == null) {
            throw new ServiceException(
                BR_ID_17 + " 源 flow_definition 不存在: id=" + bo.getSourceDefinitionId());
        }
        String sourceModelValue = srcDef.getModelValue();

        FlowDefinition newDef = flowDefinitionMapper.selectById(bo.getNewDefinitionId());
        if (newDef == null) {
            throw new ServiceException(
                BR_ID_17 + " 新 flow_definition 不存在: id=" + bo.getNewDefinitionId());
        }
        String newModelValueActual = newDef.getModelValue();

        Set<String> validModels = new HashSet<>(Arrays.asList(MODEL_VALUE_CLASSICS, MODEL_VALUE_MIMIC));
        if (!validModels.contains(sourceModelValue)) {
            throw new ServiceException(
                BR_ID_17 + " 源 model_value=" + sourceModelValue + " 越界（不在 CLASSICS / MIMIC）");
        }
        if (!validModels.contains(bo.getNewModelValue())) {
            throw new ServiceException(
                BR_ID_17 + " 新 modelValue=" + bo.getNewModelValue() + " 越界（不在 CLASSICS / MIMIC）");
        }
        if (!sourceModelValue.equals(bo.getNewModelValue())) {
            throw new ServiceException(
                BR_ID_17 + " 跨设计器模型继承禁止：source=" + sourceModelValue
                    + " new=" + bo.getNewModelValue() + "（节点合同 form_path / permission_flag / ext 不一致）");
        }
        if (!newModelValueActual.equals(bo.getNewModelValue())) {
            throw new ServiceException(
                BR_ID_17 + " 新定义 DB model_value=" + newModelValueActual
                    + " 与请求 " + bo.getNewModelValue() + " 不一致");
        }

        BrComplianceAckVo ack = new BrComplianceAckVo();
        ack.setBrId(BR_ID_17);
        ack.setValidated(true);
        ack.setDescription("flow_definition.model_value 同模型继承合法");
        ack.getExtras().put("sourceModelValue", sourceModelValue);
        ack.getExtras().put("resolvedModelValue", bo.getNewModelValue());
        log.info("[br.17] src={} new={} model={} ok",
            bo.getSourceDefinitionId(), bo.getNewDefinitionId(), bo.getNewModelValue());
        return ack;
    }

    @Override
    public BrComplianceAckVo enforceCompensationTraceability(DeHisTaskTraceBo bo) {
        String op = bo.getOperation();
        if (!Arrays.asList("CANCEL", "COMPENSATE").contains(op)) {
            throw new ServiceException(BR_ID_18 + " operation 越界: " + op);
        }
        if (!SKIP_TYPE_PASS.equals(bo.getSkipType()) && !SKIP_TYPE_REJECT.equals(bo.getSkipType())) {
            throw new ServiceException(
                BR_ID_18 + " skipType 越界: " + bo.getSkipType() + "（仅 PASS / REJECT）");
        }
        if (bo.getMessage().length() > 500) {
            throw new ServiceException(
                BR_ID_18 + " message 长度 " + bo.getMessage().length() + " 超 flow_his_task.message 上限 500");
        }
        String prefix = "CANCEL".equals(op) ? MESSAGE_PREFIX_CANCEL : MESSAGE_PREFIX_COMPENSATE;
        if (!bo.getMessage().startsWith(prefix)) {
            throw new ServiceException(
                BR_ID_18 + " message 必须以 " + prefix + " 开头，当前："
                    + bo.getMessage().substring(0, Math.min(20, bo.getMessage().length())));
        }

        // flow_his_task 是否已有该实例的历史行（只读校验）
        Long hisCount = flwHisTaskMapper.selectCount(
            new LambdaQueryWrapper<org.dromara.warm.flow.orm.entity.FlowHisTask>()
                .eq(org.dromara.warm.flow.orm.entity.FlowHisTask::getInstanceId, bo.getInstanceId()));
        boolean hasHistory = hisCount != null && hisCount > 0;

        BrComplianceAckVo ack = new BrComplianceAckVo();
        ack.setBrId(BR_ID_18);
        ack.setValidated(true);
        ack.setDescription("flow_his_task.message 已带 " + prefix + " 标记，flow_skip.skip_type="
            + bo.getSkipType() + " 合法");
        ack.getExtras().put("messagePrefix", prefix);
        ack.getExtras().put("resolvedSkipType", bo.getSkipType());
        ack.getExtras().put("hasHistory", hasHistory);
        log.info("[br.18] instance={} op={} skipType={} prefix={} hasHis={} ok",
            bo.getInstanceId(), op, bo.getSkipType(), prefix, hasHistory);
        return ack;
    }
}
