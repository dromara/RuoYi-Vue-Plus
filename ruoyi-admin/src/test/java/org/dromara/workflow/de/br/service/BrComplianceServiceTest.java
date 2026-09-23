package org.dromara.workflow.de.br.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.dromara.common.core.exception.ServiceException;
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
import org.dromara.workflow.de.domain.SaiAgent;
import org.dromara.workflow.de.domain.SaiAgentConversationRecord;
import org.dromara.workflow.de.domain.SaiAgentSkill;
import org.dromara.workflow.de.domain.SaiSkill;
import org.dromara.workflow.de.domain.FlowDefinition;
import org.dromara.workflow.de.mapper.FlowDefinitionMapper;
import org.dromara.workflow.de.mapper.FlowSkipMapper;
import org.dromara.workflow.de.mapper.SaiAgentConversationRecordMapper;
import org.dromara.workflow.de.mapper.SaiAgentMapper;
import org.dromara.workflow.de.mapper.SaiAgentSkillMapper;
import org.dromara.workflow.de.mapper.SaiSkillMapper;
import org.dromara.workflow.mapper.FlwHisTaskMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 11 条 BR 应用层校验的 surefire 标准化测试（基于 JUnit 5）。
 *
 * <p>本类是从原 {@code main()} 主程序风格迁移到 JUnit 5 的标准版本，
 * 由 surefire 在 {@code test} 阶段自动发现并执行
 * （前提：{@code -Dprofiles.active=dev -Dmaven.test.skip=false}）。
 *
 * <h3>覆盖矩阵（≥ 60 个 @Test：22 基线 + 38 边界）</h3>
 * <ul>
 *   <li>BR-01：基线 2 + 边界 4（INACTIVE→ACTIVE、DISABLED→ACTIVE、agent 不存在、stale read）</li>
 *   <li>BR-03：基线 2 + 边界 5（EXPORT+export_meta、payload 非 JSON、password 关键字、IMPORT 缺 import_meta、agent 不存在）</li>
 *   <li>BR-04：基线 2 + 边界 5（DRAFT、DISABLE、ARCHIVE、ARCHIVE→RESTORE 禁、agent 不存在）</li>
 *   <li>BR-05：基线 2 + 边界 4（ROLLBACK、UPDATE target=current、ROLLBACK target>=current、skill 不存在）</li>
 *   <li>BR-06：基线 2 + 边界 4（TEAM、PERSONAL+team_id、PERSONAL+project_id、metadata 非 JSON）</li>
 *   <li>BR-08：基线 2 + 边界 3（record 不存在、pinned 冲突 DIFF→true、MERGE→false）</li>
 *   <li>BR-09：基线 2 + 边界 4（MANUAL、ASSIST nodeType=1、AUTO nodeType=0 START、AUTO nodeType=3 GATE 拒）</li>
 *   <li>BR-15：基线 2 + 边界 4（DELETE+REJECT、REVOKE+REJECT、BACKUP、DELETE 缺 reason）</li>
 *   <li>BR-16：基线 2 + 边界 3（ROLLBACK、COMPENSATE、reason 空）</li>
 *   <li>BR-17：基线 2 + 边界 4（MIMIC→MIMIC、source 不存在、newModelValue 越界、请求与 DB 不一致）</li>
 *   <li>BR-18：基线 2 + 边界 4（CANCEL+[CANCEL]、message>500、skipType 越界、operation 越界）</li>
 * </ul>
 *
 * <p>由于 BR 校验涉及 DB 读取（MyBatis-Plus mapper），本测试用 {@link java.lang.reflect.Proxy}
 * 构造 stub mapper —— 仅覆盖 BR 实装实际调用的方法（selectById / selectList / selectCount）。
 * 其余 MyBatis-Plus 默认方法不被 BR 实装触发，不需实现。
 *
 * @author AIP
 */
@Tag("dev")
@DisplayName("BrComplianceService 11 条 BR 应用层校验")
public class BrComplianceServiceTest {

    /** 测试用 ID 常量 */
    private static final Long AGENT_ID_ACTIVE = 7L;
    private static final Long AGENT_ID_INACTIVE = 9L;
    private static final Long AGENT_ID_DISABLED = 10L;
    private static final Long AGENT_ID_DEPRECATED = 8L;
    private static final Long SKILL_ID = 100L;
    private static final Long RECORD_ID = 200L;
    private static final Long RECORD_ID_PINNED = 201L;
    private static final Long TASK_ID = 300L;
    private static final Long TASK_ID_START = 301L;
    private static final Long TASK_ID_GATE = 302L;
    private static final Long DEF_ID_CLASSICS = 1001L;
    private static final Long DEF_ID_MIMIC = 1002L;
    private static final Long INSTANCE_ID = 9001L;

    /** 桩数据持有器（同一进程内共享，便于跨 BR 复用） */
    private static final Map<String, Object> STUB_REGISTRY = new HashMap<>();

    private static BrComplianceServiceImpl SHARED_SVC;

    @BeforeAll
    static void seed() {
        seedStubData();
        SHARED_SVC = newSvc();
    }

    // ====================================================================
    // ===== BR-01：状态机迁移 ===========================================
    // ====================================================================

    @Test
    @DisplayName("BR-01 合法 ACTIVE→INACTIVE")
    void br01_passActiveToInactive() {
        DeAgentStatusBo okBo = new DeAgentStatusBo();
        okBo.setAgentId(AGENT_ID_ACTIVE);
        okBo.setFromStatus(1);
        okBo.setToStatus(2);
        okBo.setReason("BR-01 合法转移 ACTIVE→INACTIVE");
        BrComplianceAckVo ack = assertBrPass(
            () -> SHARED_SVC.enforceAgentStatusTransition(okBo));
        assertNotNull(ack);
    }

    @Test
    @DisplayName("BR-01 越界 ACTIVE→ACTIVE")
    void br01_failActiveToActive() {
        DeAgentStatusBo ngBo = new DeAgentStatusBo();
        ngBo.setAgentId(AGENT_ID_ACTIVE);
        ngBo.setFromStatus(1);
        ngBo.setToStatus(1);
        ngBo.setReason("BR-01 越界 ACTIVE→ACTIVE");
        assertBrFail(() -> SHARED_SVC.enforceAgentStatusTransition(ngBo));
    }

    // ----- BR-01 边界（4 个）-----
    @Test
    @DisplayName("BR-01-边界 INACTIVE→ACTIVE")
    void br01_edge_inactiveToActive() {
        DeAgentStatusBo bo = new DeAgentStatusBo();
        bo.setAgentId(AGENT_ID_INACTIVE);
        bo.setFromStatus(2);
        bo.setToStatus(1);
        bo.setReason("BR-01-边界 INACTIVE→ACTIVE");
        assertBrPass(() -> SHARED_SVC.enforceAgentStatusTransition(bo));
    }

    @Test
    @DisplayName("BR-01-边界 DISABLED→ACTIVE")
    void br01_edge_disabledToActive() {
        DeAgentStatusBo bo = new DeAgentStatusBo();
        bo.setAgentId(AGENT_ID_DISABLED);
        bo.setFromStatus(4);
        bo.setToStatus(1);
        bo.setReason("BR-01-边界 DISABLED→ACTIVE");
        assertBrPass(() -> SHARED_SVC.enforceAgentStatusTransition(bo));
    }

    @Test
    @DisplayName("BR-01-边界 agent 不存在")
    void br01_edge_agentMissing() {
        DeAgentStatusBo bo = new DeAgentStatusBo();
        bo.setAgentId(99999L);
        bo.setFromStatus(1);
        bo.setToStatus(2);
        bo.setReason("BR-01-边界 agent 不存在");
        assertBrFail(() -> SHARED_SVC.enforceAgentStatusTransition(bo));
    }

    @Test
    @DisplayName("BR-01-边界 fromStatus stale read")
    void br01_edge_staleRead() {
        DeAgentStatusBo bo = new DeAgentStatusBo();
        bo.setAgentId(AGENT_ID_ACTIVE);
        bo.setFromStatus(2); // 实际 status=1
        bo.setToStatus(1);
        bo.setReason("BR-01-边界 stale read");
        assertBrFail(() -> SHARED_SVC.enforceAgentStatusTransition(bo));
    }

    // ====================================================================
    // ===== BR-03：配置包 ===============================================
    // ====================================================================

    @Test
    @DisplayName("BR-03 合法 IMPORT+import_meta")
    void br03_passImportMeta() {
        DeAgentConfigBo okBo = new DeAgentConfigBo();
        okBo.setAgentId(AGENT_ID_ACTIVE);
        okBo.setOperation("IMPORT");
        okBo.setPayload("{\"import_meta\":{\"version\":1}}");
        assertBrPass(() -> SHARED_SVC.validateConfigPackageImportExport(okBo));
    }

    @Test
    @DisplayName("BR-03 越界 operation=NUKED")
    void br03_failOperationNuked() {
        DeAgentConfigBo ngBo = new DeAgentConfigBo();
        ngBo.setAgentId(AGENT_ID_ACTIVE);
        ngBo.setOperation("NUKED");
        ngBo.setPayload("{\"import_meta\":{}}");
        assertBrFail(() -> SHARED_SVC.validateConfigPackageImportExport(ngBo));
    }

    // ----- BR-03 边界（5 个）-----
    @Test
    @DisplayName("BR-03-边界 EXPORT+export_meta")
    void br03_edge_exportMeta() {
        DeAgentConfigBo bo = new DeAgentConfigBo();
        bo.setAgentId(AGENT_ID_ACTIVE);
        bo.setOperation("EXPORT");
        bo.setPayload("{\"export_meta\":{\"version\":1}}");
        assertBrPass(() -> SHARED_SVC.validateConfigPackageImportExport(bo));
    }

    @Test
    @DisplayName("BR-03-边界 payload 非 JSON")
    void br03_edge_invalidJson() {
        DeAgentConfigBo bo = new DeAgentConfigBo();
        bo.setAgentId(AGENT_ID_ACTIVE);
        bo.setOperation("IMPORT");
        bo.setPayload("not json at all");
        assertBrFail(() -> SHARED_SVC.validateConfigPackageImportExport(bo));
    }

    @Test
    @DisplayName("BR-03-边界 payload 含 password 关键字")
    void br03_edge_passwordKeyword() {
        DeAgentConfigBo bo = new DeAgentConfigBo();
        bo.setAgentId(AGENT_ID_ACTIVE);
        bo.setOperation("EXPORT");
        bo.setPayload("{\"export_meta\":{},\"password\":\"x\"}");
        assertBrFail(() -> SHARED_SVC.validateConfigPackageImportExport(bo));
    }

    @Test
    @DisplayName("BR-03-边界 IMPORT 缺 import_meta")
    void br03_edge_missingImportMeta() {
        DeAgentConfigBo bo = new DeAgentConfigBo();
        bo.setAgentId(AGENT_ID_ACTIVE);
        bo.setOperation("IMPORT");
        bo.setPayload("{\"foo\":\"bar\"}");
        assertBrFail(() -> SHARED_SVC.validateConfigPackageImportExport(bo));
    }

    @Test
    @DisplayName("BR-03-边界 agent 不存在")
    void br03_edge_agentMissing() {
        DeAgentConfigBo bo = new DeAgentConfigBo();
        bo.setAgentId(99999L);
        bo.setOperation("IMPORT");
        bo.setPayload("{\"import_meta\":{}}");
        assertBrFail(() -> SHARED_SVC.validateConfigPackageImportExport(bo));
    }

    // ====================================================================
    // ===== BR-04：生命周期 =============================================
    // ====================================================================

    @Test
    @DisplayName("BR-04 合法 AVAILABLE")
    void br04_passAvailable() {
        DeLifecycleStageBo okBo = new DeLifecycleStageBo();
        okBo.setAgentId(AGENT_ID_ACTIVE);
        okBo.setStage("AVAILABLE");
        okBo.setReason("BR-04 合法 AVAILABLE");
        assertBrPass(() -> SHARED_SVC.enforceAgentLifecycleStatus(okBo));
    }

    @Test
    @DisplayName("BR-04 越界 stage=PURGE")
    void br04_failStagePurge() {
        DeLifecycleStageBo ngBo = new DeLifecycleStageBo();
        ngBo.setAgentId(AGENT_ID_ACTIVE);
        ngBo.setStage("PURGE");
        ngBo.setReason("BR-04 越界 PURGE");
        assertBrFail(() -> SHARED_SVC.enforceAgentLifecycleStatus(ngBo));
    }

    // ----- BR-04 边界（5 个）-----
    @Test
    @DisplayName("BR-04-边界 DRAFT")
    void br04_edge_draft() {
        DeLifecycleStageBo bo = new DeLifecycleStageBo();
        bo.setAgentId(AGENT_ID_ACTIVE);
        bo.setStage("DRAFT");
        bo.setReason("BR-04-边界 DRAFT");
        assertBrPass(() -> SHARED_SVC.enforceAgentLifecycleStatus(bo));
    }

    @Test
    @DisplayName("BR-04-边界 DISABLE")
    void br04_edge_disable() {
        DeLifecycleStageBo bo = new DeLifecycleStageBo();
        bo.setAgentId(AGENT_ID_ACTIVE);
        bo.setStage("DISABLE");
        bo.setReason("BR-04-边界 DISABLE");
        assertBrPass(() -> SHARED_SVC.enforceAgentLifecycleStatus(bo));
    }

    @Test
    @DisplayName("BR-04-边界 ARCHIVE")
    void br04_edge_archive() {
        DeLifecycleStageBo bo = new DeLifecycleStageBo();
        bo.setAgentId(AGENT_ID_ACTIVE);
        bo.setStage("ARCHIVE");
        bo.setReason("BR-04-边界 ARCHIVE");
        assertBrPass(() -> SHARED_SVC.enforceAgentLifecycleStatus(bo));
    }

    @Test
    @DisplayName("BR-04-边界 ARCHIVE→RESTORE 禁止")
    void br04_edge_archiveRestoreForbidden() {
        DeLifecycleStageBo bo = new DeLifecycleStageBo();
        bo.setAgentId(AGENT_ID_DEPRECATED);
        bo.setStage("RESTORE");
        bo.setReason("BR-04-边界 ARCHIVE→RESTORE");
        assertBrFail(() -> SHARED_SVC.enforceAgentLifecycleStatus(bo));
    }

    @Test
    @DisplayName("BR-04-边界 agent 不存在")
    void br04_edge_agentMissing() {
        DeLifecycleStageBo bo = new DeLifecycleStageBo();
        bo.setAgentId(99999L);
        bo.setStage("AVAILABLE");
        bo.setReason("BR-04-边界 agent 不存在");
        assertBrFail(() -> SHARED_SVC.enforceAgentLifecycleStatus(bo));
    }

    // ====================================================================
    // ===== BR-05：技能 UPDATE 单调 =====================================
    // ====================================================================

    @Test
    @DisplayName("BR-05 合法 UPDATE 5→10")
    void br05_passUpdateAscending() {
        DeSkillUpdateBo okBo = new DeSkillUpdateBo();
        okBo.setSkillId(SKILL_ID);
        okBo.setTargetVersion(10L);
        okBo.setOperation("UPDATE");
        assertBrPass(() -> SHARED_SVC.enforceSkillUpdateDependencies(okBo));
    }

    @Test
    @DisplayName("BR-05 越界 UPDATE 5→3")
    void br05_failUpdateDescending() {
        DeSkillUpdateBo ngBo = new DeSkillUpdateBo();
        ngBo.setSkillId(SKILL_ID);
        ngBo.setTargetVersion(3L);
        ngBo.setOperation("UPDATE");
        assertBrFail(() -> SHARED_SVC.enforceSkillUpdateDependencies(ngBo));
    }

    // ----- BR-05 边界（4 个）-----
    @Test
    @DisplayName("BR-05-边界 ROLLBACK 5→3")
    void br05_edge_rollback() {
        DeSkillUpdateBo bo = new DeSkillUpdateBo();
        bo.setSkillId(SKILL_ID);
        bo.setTargetVersion(3L);
        bo.setOperation("ROLLBACK");
        assertBrPass(() -> SHARED_SVC.enforceSkillUpdateDependencies(bo));
    }

    @Test
    @DisplayName("BR-05-边界 UPDATE target=current")
    void br05_edge_updateEqual() {
        DeSkillUpdateBo bo = new DeSkillUpdateBo();
        bo.setSkillId(SKILL_ID);
        bo.setTargetVersion(5L);
        bo.setOperation("UPDATE");
        assertBrFail(() -> SHARED_SVC.enforceSkillUpdateDependencies(bo));
    }

    @Test
    @DisplayName("BR-05-边界 ROLLBACK target>=current")
    void br05_edge_rollbackNotLess() {
        DeSkillUpdateBo bo = new DeSkillUpdateBo();
        bo.setSkillId(SKILL_ID);
        bo.setTargetVersion(5L);
        bo.setOperation("ROLLBACK");
        assertBrFail(() -> SHARED_SVC.enforceSkillUpdateDependencies(bo));
    }

    @Test
    @DisplayName("BR-05-边界 skill 不存在")
    void br05_edge_skillMissing() {
        DeSkillUpdateBo bo = new DeSkillUpdateBo();
        bo.setSkillId(99999L);
        bo.setTargetVersion(6L);
        bo.setOperation("UPDATE");
        assertBrFail(() -> SHARED_SVC.enforceSkillUpdateDependencies(bo));
    }

    // ====================================================================
    // ===== BR-06：记忆 PERSONAL ========================================
    // ====================================================================

    @Test
    @DisplayName("BR-06 合法 PERSONAL 无外键")
    void br06_passPersonalNoForeignKey() {
        DeConvRecordMetaBo okBo = new DeConvRecordMetaBo();
        okBo.setRecordId(RECORD_ID);
        okBo.setScope("PERSONAL");
        okBo.setMetadataJson("{\"foo\":\"bar\"}");
        assertBrPass(() -> SHARED_SVC.enforceMemoryScope(okBo));
    }

    @Test
    @DisplayName("BR-06 越界 scope=GLOBAL")
    void br06_failScopeGlobal() {
        DeConvRecordMetaBo ngBo = new DeConvRecordMetaBo();
        ngBo.setRecordId(RECORD_ID);
        ngBo.setScope("GLOBAL");
        ngBo.setMetadataJson("{}");
        assertBrFail(() -> SHARED_SVC.enforceMemoryScope(ngBo));
    }

    // ----- BR-06 边界（4 个）-----
    @Test
    @DisplayName("BR-06-边界 TEAM")
    void br06_edge_team() {
        DeConvRecordMetaBo bo = new DeConvRecordMetaBo();
        bo.setRecordId(RECORD_ID);
        bo.setScope("TEAM");
        bo.setMetadataJson("{\"foo\":\"bar\"}");
        assertBrPass(() -> SHARED_SVC.enforceMemoryScope(bo));
    }

    @Test
    @DisplayName("BR-06-边界 PERSONAL 带 team_id")
    void br06_edge_personalTeamId() {
        DeConvRecordMetaBo bo = new DeConvRecordMetaBo();
        bo.setRecordId(RECORD_ID);
        bo.setScope("PERSONAL");
        bo.setMetadataJson("{\"team_id\":100}");
        assertBrFail(() -> SHARED_SVC.enforceMemoryScope(bo));
    }

    @Test
    @DisplayName("BR-06-边界 PERSONAL 带 project_id")
    void br06_edge_personalProjectId() {
        DeConvRecordMetaBo bo = new DeConvRecordMetaBo();
        bo.setRecordId(RECORD_ID);
        bo.setScope("PERSONAL");
        bo.setMetadataJson("{\"project_id\":200}");
        assertBrFail(() -> SHARED_SVC.enforceMemoryScope(bo));
    }

    @Test
    @DisplayName("BR-06-边界 metadata 非 JSON")
    void br06_edge_metadataInvalid() {
        DeConvRecordMetaBo bo = new DeConvRecordMetaBo();
        bo.setRecordId(RECORD_ID);
        bo.setScope("PERSONAL");
        bo.setMetadataJson("{not json");
        assertBrFail(() -> SHARED_SVC.enforceMemoryScope(bo));
    }

    // ====================================================================
    // ===== BR-08：记忆/技能 DIFF =======================================
    // ====================================================================

    @Test
    @DisplayName("BR-08 合法 DIFF 无冲突")
    void br08_passDiffNoConflict() {
        DeSkillVersionDiffBo okBo = new DeSkillVersionDiffBo();
        okBo.setRecordId(RECORD_ID);
        okBo.setSkillId(SKILL_ID);
        okBo.setOperation("DIFF");
        BrComplianceAckVo ack = assertBrPass(
            () -> SHARED_SVC.enforceMemorySkillVersionDiff(okBo));
        assertNotNull(ack);
    }

    @Test
    @DisplayName("BR-08 越界 skill 不存在")
    void br08_failSkillNotExist() {
        DeSkillVersionDiffBo ngBo = new DeSkillVersionDiffBo();
        ngBo.setRecordId(RECORD_ID);
        ngBo.setSkillId(99999L);
        ngBo.setOperation("DIFF");
        assertBrFail(() -> SHARED_SVC.enforceMemorySkillVersionDiff(ngBo));
    }

    // ----- BR-08 边界（3 个）-----
    @Test
    @DisplayName("BR-08-边界 record 不存在")
    void br08_edge_recordMissing() {
        DeSkillVersionDiffBo bo = new DeSkillVersionDiffBo();
        bo.setRecordId(99999L);
        bo.setSkillId(SKILL_ID);
        bo.setOperation("DIFF");
        assertBrFail(() -> SHARED_SVC.enforceMemorySkillVersionDiff(bo));
    }

    @Test
    @DisplayName("BR-08-边界 pinned 冲突 DIFF → validated=true")
    void br08_edge_pinnedConflictDiff() {
        DeSkillVersionDiffBo bo = new DeSkillVersionDiffBo();
        bo.setRecordId(RECORD_ID_PINNED);
        bo.setSkillId(SKILL_ID);
        bo.setOperation("DIFF");
        // 实现：validated = !conflict || DIFF → DIFF 与冲突同时为 true
        BrComplianceAckVo ack = assertDoesNotThrow(
            () -> SHARED_SVC.enforceMemorySkillVersionDiff(bo));
        assertTrue(Boolean.TRUE.equals(ack.getValidated()),
            "DIFF+conflict 期望 validated=true（DIFF 是只读差异查看）");
    }

    @Test
    @DisplayName("BR-08-边界 pinned 冲突 MERGE → validated=false")
    void br08_edge_pinnedConflictMerge() {
        DeSkillVersionDiffBo bo = new DeSkillVersionDiffBo();
        bo.setRecordId(RECORD_ID_PINNED);
        bo.setSkillId(SKILL_ID);
        bo.setOperation("MERGE");
        BrComplianceAckVo ack = assertDoesNotThrow(
            () -> SHARED_SVC.enforceMemorySkillVersionDiff(bo));
        // MERGE 非只读：冲突时阻断
        assertTrue(Boolean.FALSE.equals(ack.getValidated()),
            "MERGE+conflict 期望 validated=false（MERGE 是写入路径）");
    }

    // ====================================================================
    // ===== BR-09：执行模式 =============================================
    // ====================================================================

    @Test
    @DisplayName("BR-09 合法 AUTO+node_type=1")
    void br09_passAutoMiddle() {
        DeExecutionModeBo okBo = new DeExecutionModeBo();
        okBo.setTaskId(TASK_ID);
        okBo.setMode("AUTO");
        assertBrPass(() -> SHARED_SVC.enforceExecutionMode(okBo));
    }

    @Test
    @DisplayName("BR-09 越界 mode=UNDEFINED")
    void br09_failModeUndefined() {
        DeExecutionModeBo ngBo = new DeExecutionModeBo();
        ngBo.setTaskId(TASK_ID);
        ngBo.setMode("UNDEFINED");
        assertBrFail(() -> SHARED_SVC.enforceExecutionMode(ngBo));
    }

    // ----- BR-09 边界（4 个）-----
    @Test
    @DisplayName("BR-09-边界 MANUAL")
    void br09_edge_manual() {
        DeExecutionModeBo bo = new DeExecutionModeBo();
        bo.setTaskId(TASK_ID);
        bo.setMode("MANUAL");
        assertBrPass(() -> SHARED_SVC.enforceExecutionMode(bo));
    }

    @Test
    @DisplayName("BR-09-边界 ASSIST nodeType=1")
    void br09_edge_assistMiddle() {
        DeExecutionModeBo bo = new DeExecutionModeBo();
        bo.setTaskId(TASK_ID);
        bo.setMode("ASSIST");
        assertBrPass(() -> SHARED_SVC.enforceExecutionMode(bo));
    }

    @Test
    @DisplayName("BR-09-边界 AUTO nodeType=0 START")
    void br09_edge_autoStart() {
        DeExecutionModeBo bo = new DeExecutionModeBo();
        bo.setTaskId(TASK_ID_START);
        bo.setMode("AUTO");
        assertBrPass(() -> SHARED_SVC.enforceExecutionMode(bo));
    }

    @Test
    @DisplayName("BR-09-边界 AUTO nodeType=3 GATE 拒")
    void br09_edge_autoGate() {
        DeExecutionModeBo bo = new DeExecutionModeBo();
        bo.setTaskId(TASK_ID_GATE);
        bo.setMode("AUTO");
        assertBrFail(() -> SHARED_SVC.enforceExecutionMode(bo));
    }

    // ====================================================================
    // ===== BR-15：skip_type ============================================
    // ====================================================================

    @Test
    @DisplayName("BR-15 合法 MODIFY+PASS")
    void br15_passModifyPass() {
        DeSkipTypeBo okBo = new DeSkipTypeBo();
        okBo.setDefinitionId(DEF_ID_CLASSICS);
        okBo.setSkipType("PASS");
        okBo.setOperation("MODIFY");
        assertBrPass(() -> SHARED_SVC.enforceSkipTypeAtDefinition(okBo));
    }

    @Test
    @DisplayName("BR-15 越界 DELETE+PASS")
    void br15_failDeletePass() {
        DeSkipTypeBo ngBo = new DeSkipTypeBo();
        ngBo.setDefinitionId(DEF_ID_CLASSICS);
        ngBo.setSkipType("PASS");
        ngBo.setOperation("DELETE");
        ngBo.setReason("BR-15 DELETE 必须 REJECT");
        assertBrFail(() -> SHARED_SVC.enforceSkipTypeAtDefinition(ngBo));
    }

    // ----- BR-15 边界（4 个）-----
    @Test
    @DisplayName("BR-15-边界 DELETE+REJECT+reason")
    void br15_edge_deleteReject() {
        DeSkipTypeBo bo = new DeSkipTypeBo();
        bo.setDefinitionId(DEF_ID_CLASSICS);
        bo.setSkipType("REJECT");
        bo.setOperation("DELETE");
        bo.setReason("BR-15-边界 DELETE+REJECT");
        assertBrPass(() -> SHARED_SVC.enforceSkipTypeAtDefinition(bo));
    }

    @Test
    @DisplayName("BR-15-边界 REVOKE+REJECT+reason")
    void br15_edge_revokeReject() {
        DeSkipTypeBo bo = new DeSkipTypeBo();
        bo.setDefinitionId(DEF_ID_CLASSICS);
        bo.setSkipType("REJECT");
        bo.setOperation("REVOKE");
        bo.setReason("BR-15-边界 REVOKE+REJECT");
        assertBrPass(() -> SHARED_SVC.enforceSkipTypeAtDefinition(bo));
    }

    @Test
    @DisplayName("BR-15-边界 BACKUP+reason")
    void br15_edge_backup() {
        DeSkipTypeBo bo = new DeSkipTypeBo();
        bo.setDefinitionId(DEF_ID_CLASSICS);
        bo.setSkipType("PASS");
        bo.setOperation("BACKUP");
        bo.setReason("BR-15-边界 BACKUP");
        assertBrPass(() -> SHARED_SVC.enforceSkipTypeAtDefinition(bo));
    }

    @Test
    @DisplayName("BR-15-边界 DELETE 缺 reason")
    void br15_edge_deleteNoReason() {
        DeSkipTypeBo bo = new DeSkipTypeBo();
        bo.setDefinitionId(DEF_ID_CLASSICS);
        bo.setSkipType("REJECT");
        bo.setOperation("DELETE");
        bo.setReason("");
        assertBrFail(() -> SHARED_SVC.enforceSkipTypeAtDefinition(bo));
    }

    // ====================================================================
    // ===== BR-16：CANCEL/REJECT ========================================
    // ====================================================================

    @Test
    @DisplayName("BR-16 合法 CANCEL")
    void br16_passCancel() {
        DeSkipCompensationBo okBo = new DeSkipCompensationBo();
        okBo.setInstanceId(INSTANCE_ID);
        okBo.setOperation("CANCEL");
        okBo.setReason("BR-16 合法 CANCEL");
        assertBrPass(() -> SHARED_SVC.enforceCancelRollbackCompensation(okBo));
    }

    @Test
    @DisplayName("BR-16 越界 operation=NUKED")
    void br16_failOperationNuked() {
        DeSkipCompensationBo ngBo = new DeSkipCompensationBo();
        ngBo.setInstanceId(INSTANCE_ID);
        ngBo.setOperation("NUKED");
        ngBo.setReason("BR-16 越界");
        assertBrFail(() -> SHARED_SVC.enforceCancelRollbackCompensation(ngBo));
    }

    // ----- BR-16 边界（3 个）-----
    @Test
    @DisplayName("BR-16-边界 ROLLBACK")
    void br16_edge_rollback() {
        DeSkipCompensationBo bo = new DeSkipCompensationBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setOperation("ROLLBACK");
        bo.setReason("BR-16-边界 ROLLBACK");
        assertBrPass(() -> SHARED_SVC.enforceCancelRollbackCompensation(bo));
    }

    @Test
    @DisplayName("BR-16-边界 COMPENSATE")
    void br16_edge_compensate() {
        DeSkipCompensationBo bo = new DeSkipCompensationBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setOperation("COMPENSATE");
        bo.setReason("BR-16-边界 COMPENSATE");
        assertBrPass(() -> SHARED_SVC.enforceCancelRollbackCompensation(bo));
    }

    @Test
    @DisplayName("BR-16-边界 reason 空")
    void br16_edge_reasonEmpty() {
        DeSkipCompensationBo bo = new DeSkipCompensationBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setOperation("CANCEL");
        bo.setReason("");
        assertBrFail(() -> SHARED_SVC.enforceCancelRollbackCompensation(bo));
    }

    // ====================================================================
    // ===== BR-17：模型继承 =============================================
    // ====================================================================

    @Test
    @DisplayName("BR-17 合法 CLASSICS→CLASSICS")
    void br17_passClassicsToClassics() {
        DeDefinitionInheritBo okBo = new DeDefinitionInheritBo();
        okBo.setSourceDefinitionId(DEF_ID_CLASSICS);
        okBo.setNewDefinitionId(DEF_ID_CLASSICS);
        okBo.setNewModelValue("CLASSICS");
        assertBrPass(() -> SHARED_SVC.validateDefinitionModelInheritance(okBo));
    }

    @Test
    @DisplayName("BR-17 越界 CLASSICS→MIMIC")
    void br17_failCrossModel() {
        DeDefinitionInheritBo ngBo = new DeDefinitionInheritBo();
        ngBo.setSourceDefinitionId(DEF_ID_CLASSICS);
        ngBo.setNewDefinitionId(DEF_ID_MIMIC);
        ngBo.setNewModelValue("MIMIC");
        assertBrFail(() -> SHARED_SVC.validateDefinitionModelInheritance(ngBo));
    }

    @Test
    @DisplayName("BR-17 越界 MIMIC→CLASSICS（对称跨继承拒绝）")
    void br17_failCrossModelSymmetric() {
        DeDefinitionInheritBo ngBo = new DeDefinitionInheritBo();
        ngBo.setSourceDefinitionId(DEF_ID_MIMIC);
        ngBo.setNewDefinitionId(DEF_ID_CLASSICS);
        ngBo.setNewModelValue("CLASSICS");
        assertBrFail(() -> SHARED_SVC.validateDefinitionModelInheritance(ngBo));
    }

    // ----- BR-17 边界（4 个）-----
    @Test
    @DisplayName("BR-17-边界 MIMIC→MIMIC")
    void br17_edge_mimicToMimic() {
        DeDefinitionInheritBo bo = new DeDefinitionInheritBo();
        bo.setSourceDefinitionId(DEF_ID_MIMIC);
        bo.setNewDefinitionId(DEF_ID_MIMIC);
        bo.setNewModelValue("MIMIC");
        assertBrPass(() -> SHARED_SVC.validateDefinitionModelInheritance(bo));
    }

    @Test
    @DisplayName("BR-17-边界 source 不存在")
    void br17_edge_sourceMissing() {
        DeDefinitionInheritBo bo = new DeDefinitionInheritBo();
        bo.setSourceDefinitionId(99999L);
        bo.setNewDefinitionId(DEF_ID_CLASSICS);
        bo.setNewModelValue("CLASSICS");
        assertBrFail(() -> SHARED_SVC.validateDefinitionModelInheritance(bo));
    }

    @Test
    @DisplayName("BR-17-边界 newModelValue 越界")
    void br17_edge_modelValueUnknown() {
        DeDefinitionInheritBo bo = new DeDefinitionInheritBo();
        bo.setSourceDefinitionId(DEF_ID_CLASSICS);
        bo.setNewDefinitionId(DEF_ID_CLASSICS);
        bo.setNewModelValue("UNKNOWN");
        assertBrFail(() -> SHARED_SVC.validateDefinitionModelInheritance(bo));
    }

    @Test
    @DisplayName("BR-17-边界 请求与 DB model_value 不一致")
    void br17_edge_dbMismatch() {
        DeDefinitionInheritBo bo = new DeDefinitionInheritBo();
        bo.setSourceDefinitionId(DEF_ID_CLASSICS);
        bo.setNewDefinitionId(DEF_ID_MIMIC); // DB=MIMIC
        bo.setNewModelValue("CLASSICS");    // 但声称 CLASSICS
        assertBrFail(() -> SHARED_SVC.validateDefinitionModelInheritance(bo));
    }

    // ====================================================================
    // ===== BR-18：[COMPENSATE] 前缀 =====================================
    // ====================================================================

    @Test
    @DisplayName("BR-18 合法 [COMPENSATE]+PASS")
    void br18_passCompensatePass() {
        DeHisTaskTraceBo okBo = new DeHisTaskTraceBo();
        okBo.setInstanceId(INSTANCE_ID);
        okBo.setOperation("COMPENSATE");
        okBo.setSkipType("PASS");
        okBo.setMessage("[COMPENSATE] BR-18 合法");
        assertBrPass(() -> SHARED_SVC.enforceCompensationTraceability(okBo));
    }

    @Test
    @DisplayName("BR-18 越界 message 缺前缀")
    void br18_failMessageMissingPrefix() {
        DeHisTaskTraceBo ngBo = new DeHisTaskTraceBo();
        ngBo.setInstanceId(INSTANCE_ID);
        ngBo.setOperation("COMPENSATE");
        ngBo.setSkipType("PASS");
        ngBo.setMessage("BR-18 越界 无前缀");
        assertBrFail(() -> SHARED_SVC.enforceCompensationTraceability(ngBo));
    }

    // ----- BR-18 边界（4 个）-----
    @Test
    @DisplayName("BR-18-边界 CANCEL+[CANCEL] 前缀")
    void br18_edge_cancelPrefix() {
        DeHisTaskTraceBo bo = new DeHisTaskTraceBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setOperation("CANCEL");
        bo.setSkipType("REJECT");
        bo.setMessage("[CANCEL] BR-18-边界");
        assertBrPass(() -> SHARED_SVC.enforceCompensationTraceability(bo));
    }

    @Test
    @DisplayName("BR-18-边界 message>500 字符")
    void br18_edge_messageTooLong() {
        DeHisTaskTraceBo bo = new DeHisTaskTraceBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setOperation("COMPENSATE");
        bo.setSkipType("PASS");
        StringBuilder sb = new StringBuilder("[COMPENSATE] ");
        while (sb.length() <= 500) {
            sb.append("x");
        }
        bo.setMessage(sb.toString());
        assertBrFail(() -> SHARED_SVC.enforceCompensationTraceability(bo));
    }

    @Test
    @DisplayName("BR-18-边界 skipType 越界")
    void br18_edge_skipTypeUnknown() {
        DeHisTaskTraceBo bo = new DeHisTaskTraceBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setOperation("COMPENSATE");
        bo.setSkipType("OTHER");
        bo.setMessage("[COMPENSATE] BR-18");
        assertBrFail(() -> SHARED_SVC.enforceCompensationTraceability(bo));
    }

    @Test
    @DisplayName("BR-18-边界 operation 越界")
    void br18_edge_operationUnknown() {
        DeHisTaskTraceBo bo = new DeHisTaskTraceBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setOperation("ROLLBACK");
        bo.setSkipType("PASS");
        bo.setMessage("[COMPENSATE] BR-18");
        assertBrFail(() -> SHARED_SVC.enforceCompensationTraceability(bo));
    }

    // ====================================================================
    // ===== N1 边界扩展（BR-18 跨配对 / BR-01 null / BR-15 BACKUP 空）====
    // ====================================================================

    /**
     * 边界：BR-18 跨配对（op=CANCEL + skipType=PASS，与 BR-16 严格映射相反）。
     *
     * <p>BR-16 enforceCancelRollbackCompensation 强制 op→skipType 一对一
     * （CANCEL→REJECT / COMPENSATE→PASS），但 BR-18 enforceCompensationTraceability
     * 只校验 message 前缀与长度，<b>不</b>限制 op→skipType 的固定配对。
     * 因此 op=CANCEL + skipType=PASS 这种「跨配对」在 BR-18 视角下合法。
     *
     * <p>断言：合法通过 + ack.validated=true + extras.messagePrefix="[CANCEL]"。
     */
    @Test
    @DisplayName("BR-18-边界-N1 跨配对：CANCEL+PASS（与 BR-16 严格映射相反，BR-18 视角合法）")
    void br18_edge_n1_crossPair_cancelWithPass() {
        DeHisTaskTraceBo bo = new DeHisTaskTraceBo();
        bo.setInstanceId(INSTANCE_ID);
        bo.setOperation("CANCEL");
        bo.setSkipType("PASS"); // 跨配对：BR-16 视角非法，BR-18 视角合法
        bo.setMessage("[CANCEL] BR-18-N1 跨配对 PASS（与 BR-16 严格映射相反）");
        BrComplianceAckVo ack = assertBrPass(
            () -> SHARED_SVC.enforceCompensationTraceability(bo));
        assertEquals("CANCEL", ack.getExtras().get("operation"),
            "ack 应记录原始 operation=CANCEL");
        assertEquals("PASS", ack.getExtras().get("resolvedSkipType"),
            "ack 应记录 skipType=PASS（不强制 REJECT）");
        assertEquals("[CANCEL]", ack.getExtras().get("messagePrefix"),
            "ack 应记录 message 前缀 [CANCEL]");
    }

    /**
     * 边界：BR-01 状态机迁移 BO 中 fromStatus 为 null。
     *
     * <p>实装 {@code !agent.getStatus().equals(bo.getFromStatus())}（{@code BrComplianceServiceImpl:140}）：
     * 当 {@code bo.getFromStatus()} 为 null 时 {@code Integer.equals(null)} 返回 false ⇒ 命中
     * 「stale read / 不一致」分支抛异常。{@code bo.getToStatus()} 为 null 时同样
     * 进入状态机越界分支（key="X->null" 不在合法转移集）。
     *
     * <p>断言：两者任一为 null 都抛 ServiceException。
     */
    @Test
    @DisplayName("BR-01-边界-N1 fromStatus=null / toStatus=null → 状态机校验失败")
    void br01_edge_n1_nullFromStatus() {
        DeAgentStatusBo bo = new DeAgentStatusBo();
        bo.setAgentId(AGENT_ID_ACTIVE);
        bo.setFromStatus(null); // 边界：缺省 → null
        bo.setToStatus(2);
        bo.setReason("BR-01-边界-N1 fromStatus=null");
        assertBrFail(() -> SHARED_SVC.enforceAgentStatusTransition(bo),
            "BR-01 fromStatus=null 应抛 ServiceException（agent.getStatus().equals(null)→false ⇒ stale read）");
    }

    @Test
    @DisplayName("BR-01-边界-N1 toStatus=null → 状态机越界")
    void br01_edge_n1_nullToStatus() {
        DeAgentStatusBo bo = new DeAgentStatusBo();
        bo.setAgentId(AGENT_ID_ACTIVE);
        bo.setFromStatus(1);
        bo.setToStatus(null); // 边界：缺省 → null
        bo.setReason("BR-01-边界-N1 toStatus=null");
        assertBrFail(() -> SHARED_SVC.enforceAgentStatusTransition(bo),
            "BR-01 toStatus=null 应抛 ServiceException（key='1->null' 不在合法转移集）");
    }

    /**
     * 边界：BR-15 BACKUP 路径下 reason 为空。
     *
     * <p>实装 {@code enforceSkipTypeAtDefinition(:490)}：BACKUP 分支独立校验
     * {@code StrUtil.isBlank(bo.getReason())} ⇒ 缺 reason 时抛
     * 「BR-15 BACKUP 必须填 reason（审计追溯用）」。
     *
     * <p>既有 {@code br15_edge_deleteNoReason} 只覆盖 DELETE 路径；本测试补
     * BACKUP 路径（{@code DeSkipTypeBo:34} 注释「BACKUP / DELETE 必填」）。
     */
    @Test
    @DisplayName("BR-15-边界-N1 BACKUP+空 reason → 审计追溯必填拒收")
    void br15_edge_n1_backupEmptyReason() {
        DeSkipTypeBo bo = new DeSkipTypeBo();
        bo.setDefinitionId(DEF_ID_CLASSICS);
        bo.setSkipType("PASS");
        bo.setOperation("BACKUP");
        bo.setReason(""); // 边界：空 reason（BACKUP 路径独立校验）
        assertBrFail(() -> SHARED_SVC.enforceSkipTypeAtDefinition(bo),
            "BR-15 BACKUP 必须填 reason（审计追溯用），空 reason 应抛 ServiceException");
    }

    // ====================================================================
    // ===== 断言辅助 =====================================================
    // ====================================================================

    @FunctionalInterface
    private interface BrCall {
        BrComplianceAckVo invoke();
    }

    private static BrComplianceAckVo assertBrPass(BrCall call) {
        BrComplianceAckVo ack = assertDoesNotThrow(call::invoke,
            "期望合法 BR 不抛 ServiceException");
        assertNotNull(ack, "ack 不应为 null");
        assertTrue(Boolean.TRUE.equals(ack.getValidated()),
            "期望 ack.validated=true，实际 false");
        return ack;
    }

    private static void assertBrFail(BrCall call) {
        assertBrFail(call, "期望非法 BR 抛 ServiceException");
    }

    private static void assertBrFail(BrCall call, String message) {
        assertThrows(ServiceException.class, call::invoke, message);
    }

    // ====================================================================
    // ===== 桩数据 + Mapper 构造 =========================================
    // ====================================================================

    private static void seedStubData() {
        // sai_agent: id=7 status=1 (ACTIVE), id=8 status=3 (DEPRECATED), id=9 status=2 (INACTIVE), id=10 status=4 (DISABLED)
        SaiAgent active = new SaiAgent();
        active.setId(AGENT_ID_ACTIVE);
        active.setStatus(1);
        active.setName("test-agent-active");
        STUB_REGISTRY.put("agent:" + AGENT_ID_ACTIVE, active);

        SaiAgent inactive = new SaiAgent();
        inactive.setId(AGENT_ID_INACTIVE);
        inactive.setStatus(2);
        inactive.setName("test-agent-inactive");
        STUB_REGISTRY.put("agent:" + AGENT_ID_INACTIVE, inactive);

        SaiAgent disabled = new SaiAgent();
        disabled.setId(AGENT_ID_DISABLED);
        disabled.setStatus(4);
        disabled.setName("test-agent-disabled");
        STUB_REGISTRY.put("agent:" + AGENT_ID_DISABLED, disabled);

        SaiAgent deprecated = new SaiAgent();
        deprecated.setId(AGENT_ID_DEPRECATED);
        deprecated.setStatus(3);
        deprecated.setName("test-agent-deprecated");
        STUB_REGISTRY.put("agent:" + AGENT_ID_DEPRECATED, deprecated);

        // sai_skill: id=100 version=5
        SaiSkill skill = new SaiSkill();
        skill.setId(SKILL_ID);
        skill.setVersion(5L);
        STUB_REGISTRY.put("skill:" + SKILL_ID, skill);

        // sai_agent_conversation_record: id=200 metadata=null（无 pinned_skill_version）
        SaiAgentConversationRecord rec = new SaiAgentConversationRecord();
        rec.setId(RECORD_ID);
        rec.setMetadata(null);
        STUB_REGISTRY.put("record:" + RECORD_ID, rec);

        // sai_agent_conversation_record: id=201 metadata 含 de.pinned_skill_version=3（与 skill version=5 冲突）
        SaiAgentConversationRecord recPinned = new SaiAgentConversationRecord();
        recPinned.setId(RECORD_ID_PINNED);
        recPinned.setMetadata("{\"de.pinned_skill_version\":3,\"foo\":\"bar\"}");
        STUB_REGISTRY.put("record:" + RECORD_ID_PINNED, recPinned);

        // flow_task: id=300 node_type=1 (MIDDLE), id=301 node_type=0 (START), id=302 node_type=3 (GATE)
        FlowTask task = new FlowTask();
        task.setId(TASK_ID);
        task.setNodeType(1);
        STUB_REGISTRY.put("task:" + TASK_ID, task);

        FlowTask startTask = new FlowTask();
        startTask.setId(TASK_ID_START);
        startTask.setNodeType(0);
        STUB_REGISTRY.put("task:" + TASK_ID_START, startTask);

        FlowTask gateTask = new FlowTask();
        gateTask.setId(TASK_ID_GATE);
        gateTask.setNodeType(3);
        STUB_REGISTRY.put("task:" + TASK_ID_GATE, gateTask);

        // flow_definition: id=1001 model_value=CLASSICS, id=1002 model_value=MIMIC
        FlowDefinition defClassics = new FlowDefinition();
        defClassics.setId(DEF_ID_CLASSICS);
        defClassics.setModelValue("CLASSICS");
        STUB_REGISTRY.put("def:" + DEF_ID_CLASSICS, defClassics);

        FlowDefinition defMimic = new FlowDefinition();
        defMimic.setId(DEF_ID_MIMIC);
        defMimic.setModelValue("MIMIC");
        STUB_REGISTRY.put("def:" + DEF_ID_MIMIC, defMimic);

        // sai_agent_skill deps for skill 100: empty list（让 BR-05 不超限）
        STUB_REGISTRY.put("deps:" + SKILL_ID, new ArrayList<>());
    }

    private static BrComplianceServiceImpl newSvc() {
        return new BrComplianceServiceImpl(
            stubMapper(SaiAgentMapper.class, id -> STUB_REGISTRY.get("agent:" + id)),
            stubMapper(SaiSkillMapper.class, id -> STUB_REGISTRY.get("skill:" + id)),
            stubMapper(SaiAgentSkillMapper.class, wrapper -> {
                // 从 wrapper 中取 skillId（lambda 表达式不易直接抓）—— 退而返回 STUB_REGISTRY 里 deps
                return (List<?>) STUB_REGISTRY.getOrDefault("deps:" + SKILL_ID, new ArrayList<>());
            }),
            stubMapper(SaiAgentConversationRecordMapper.class,
                id -> STUB_REGISTRY.get("record:" + id)),
            stubMapper(FlowSkipMapper.class, null, null),
            stubMapper(FlowDefinitionMapper.class, id -> STUB_REGISTRY.get("def:" + id)),
            stubMapper(FlwTaskMapper.class, id -> STUB_REGISTRY.get("task:" + id)),
            stubMapper(FlwHisTaskMapper.class, null, () -> 0L),
            new com.fasterxml.jackson.databind.ObjectMapper()
        );
    }

    /**
     * 通用 stub mapper 构造（selectById + selectList + selectCount）。
     *
     * <p>BR 实装只调用这三个方法；其余 MyBatis-Plus 默认方法不被触发，不实现。
     *
     * @param iface        mapper 接口
     * @param byIdResolver id → entity 解析器（可为 null ⇒ 总是返回 null）
     * @param <M>          mapper 类型
     * @return 动态代理实例
     */
    @SuppressWarnings("unchecked")
    private static <M> M stubMapper(Class<M> iface, Function<Object, Object> byIdResolver) {
        return stubMapper(iface, byIdResolver, null);
    }

    @SuppressWarnings("unchecked")
    private static <M> M stubMapper(
        Class<M> iface,
        Function<Object, Object> byIdResolver,
        java.util.function.Supplier<Object> countSupplier
    ) {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("selectById".equals(name) && method.getParameterCount() == 1) {
                if (byIdResolver == null) {
                    return null;
                }
                return byIdResolver.apply(args[0]);
            }
            if ("selectList".equals(name) && method.getParameterCount() == 1) {
                // 简化：返回 STUB_REGISTRY 中 "deps:*" 的列表；其它情况返回空
                return new ArrayList<>();
            }
            if ("selectCount".equals(name) && method.getParameterCount() == 1) {
                if (countSupplier != null) {
                    Object v = countSupplier.get();
                    return v == null ? 0L : ((Number) v).longValue();
                }
                return 0L;
            }
            // equals/hashCode/toString —— Object 默认
            if ("equals".equals(name) || "hashCode".equals(name) || "toString".equals(name)) {
                return method.invoke(proxy, args);
            }
            // 其它方法返回零值
            Class<?> rt = method.getReturnType();
            if (rt == boolean.class) {
                return false;
            }
            if (rt == int.class || rt == long.class || rt == short.class || rt == byte.class) {
                return 0;
            }
            if (rt == double.class || rt == float.class) {
                return 0.0;
            }
            if (rt == List.class || rt == Collection.class || rt == Iterable.class) {
                return Collections.emptyList();
            }
            return null;
        };
        return (M) Proxy.newProxyInstance(
            iface.getClassLoader(), new Class<?>[]{iface}, handler);
    }

    // 兜底断言：保证 import 不被 IDE 视为未用
    @SuppressWarnings("unused")
    private static void touchImports() {
        assertEquals(0, 0);
        assertTrue(true);
        fail("never called");
    }

    // 防止 unused-warning：部分导入类型在编译期需要存在
    @SuppressWarnings("unused")
    private static final Class<?>[] TOUCHED_TYPES = new Class<?>[]{
        Wrapper.class,
        SaiAgentSkill.class,
    };
}