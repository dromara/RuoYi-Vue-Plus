package org.dromara.workflow.de.sy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.de.constants.DeP1Constants;
import org.dromara.workflow.de.constants.DeVariableConstants;
import org.dromara.workflow.de.sy.domain.bo.DeSyCorrectionBo;
import org.dromara.workflow.mapper.FlwHisTaskMapper;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SY 域 {@link DeSyServiceImpl} 纯逻辑单测（T1-W4，不启 Spring、零 DB）。
 *
 * <p>范式与 W1/W3 一致：Proxy 捕获型 stub mapper + 构造注入直调。
 *
 * <p>钉死的业务不变量：
 * <ul>
 *   <li>ackState：taskId 必填/任务存在性 fail-closed；delivered/read 由 createTime/updateTime
 *       非空驱动；mainTaskAchieved 由 flowStatus==FINISHED("2") 驱动；两占位恒 false；</li>
 *   <li>★correctionVersion：任务不存在抛；纠正必须落 flow_his_task 审计留痕——message 拼
 *       [SY-06] + summary + conflictingFact、skipType=NONE、taskId/instanceId 从任务拷贝、
 *       回显 bo.taskId（非新事实的 DB id）；</li>
 *   <li>★checkpointRecover：recoverable 三态——实例 null / variable null / variable 缺
 *       de.ctx key 均为 false，仅含 key 时 true；checkpoint 直取 variable；</li>
 *   <li>★lateArrivals：按 message tag 三分类（late/withdrawn/node-change），无 tag 丢弃，
 *       各桶命中顺序保持、validated 恒 true；</li>
 *   <li>handoverPackageValidate：packageSize=trail.size、contributionVersions 映射 id 序列、
 *       readability 空轨 0 / 非空 1.0。</li>
 * </ul>
 *
 * @author AIP
 */
@Tag("dev")
@DisplayName("SY 域 DeSyServiceImpl：确认状态 / 纠正版本 / 检查点恢复 / 迟到项 / 交接包校验（纯逻辑）")
class DeSyServiceImplTest {

    private static final Long TASK_ID = 42L;
    private static final Long INST_ID = 99L;

    // ════════════════ ackState ════════════════

    @Test
    @DisplayName("ackState：taskId 为 null 必须 fail-closed")
    void ackState_null_throws() {
        assertThrows(ServiceException.class, () -> new Harness().service.ackState(null));
    }

    @Test
    @DisplayName("ackState：任务不存在必须抛『任务不存在』")
    void ackState_taskMissing_throws() {
        Harness h = new Harness();
        h.task = null;
        ServiceException ex = assertThrows(ServiceException.class, () -> h.service.ackState(TASK_ID));
        assertTrue(ex.getMessage().contains("任务不存在"));
    }

    @Test
    @DisplayName("ackState：全新任务（无时间戳/未结束）——五态全 false")
    void ackState_freshAllFalse() {
        Harness h = new Harness();
        h.task = new FlowTask();
        Map<String, Object> st = h.service.ackState(TASK_ID);
        assertFalse((Boolean) st.get("delivered"));
        assertFalse((Boolean) st.get("read"));
        assertFalse((Boolean) st.get("contributionAccepted"), "占位必须恒 false");
        assertFalse((Boolean) st.get("mainTaskAchieved"));
        assertFalse((Boolean) st.get("businessHandoverDone"), "占位必须恒 false");
    }

    @Test
    @DisplayName("★ackState：时间戳齐备 + flowStatus=FINISHED → delivered/read/mainTaskAchieved 全 true")
    void ackState_finishedAllTrue() {
        Harness h = new Harness();
        h.task = task();
        Map<String, Object> st = h.service.ackState(TASK_ID);
        assertTrue((Boolean) st.get("delivered"), "createTime 非空即 delivered");
        assertTrue((Boolean) st.get("read"), "updateTime 非空即 read");
        assertTrue((Boolean) st.get("mainTaskAchieved"), "flowStatus=2 即主任务达成");
    }

    // ════════════════ correctionVersion ════════════════

    @Test
    @DisplayName("correctionVersion：任务不存在必须抛，零 insert")
    void correction_taskMissing_throws() {
        Harness h = new Harness();
        h.task = null;
        assertThrows(ServiceException.class, () -> h.service.correctionVersion(correctionBo()));
        assertEquals(0, h.inserted.size());
    }

    @Test
    @DisplayName("★correctionVersion：审计留痕落 flow_his_task——message 拼装/skipType=NONE/关联字段拷贝")
    void correction_writesAuditTrail() {
        Harness h = new Harness();
        Long r = h.service.correctionVersion(correctionBo());
        assertEquals(TASK_ID, r, "必须回显请求 taskId，而非 DB 生成 id");
        assertEquals(1, h.inserted.size(), "纠正必须恰好产生一条 flow_his_task 留痕");
        FlowHisTask his = h.inserted.get(0);
        assertEquals(TASK_ID, his.getTaskId(), "taskId 从任务拷贝");
        assertEquals(INST_ID, his.getInstanceId(), "instanceId 从任务拷贝");
        assertEquals(SkipType.NONE.getKey(), his.getSkipType(), "纠正留痕无流转，skipType 必为 NONE");
        assertTrue(his.getMessage().contains("[SY-06] correction"));
        assertTrue(his.getMessage().contains("旧摘要"));
        assertTrue(his.getMessage().contains("冲突事实"), "message 必须携带冲突来源");
    }

    // ════════════════ checkpointRecover ════════════════

    @Test
    @DisplayName("checkpointRecover：taskId 为 null 必须 fail-closed")
    void checkpoint_null_throws() {
        assertThrows(ServiceException.class, () -> new Harness().service.checkpointRecover(null));
    }

    @Test
    @DisplayName("checkpointRecover：任务不存在必须抛")
    void checkpoint_taskMissing_throws() {
        Harness h = new Harness();
        h.task = null;
        assertThrows(ServiceException.class, () -> h.service.checkpointRecover(TASK_ID));
    }

    @Test
    @DisplayName("★checkpointRecover：实例缺失 → recoverable=false，checkpoint=null")
    void checkpoint_noInstance_notRecoverable() {
        Harness h = new Harness();
        h.instance = null;
        Map<String, Object> out = h.service.checkpointRecover(TASK_ID);
        assertEquals(Boolean.FALSE, out.get("recoverable"));
        org.junit.jupiter.api.Assertions.assertNull(out.get("checkpoint"));
    }

    @Test
    @DisplayName("checkpointRecover：variable 缺 de.ctx key → recoverable=false")
    void checkpoint_variableWithoutKey_notRecoverable() {
        Harness h = new Harness();
        h.instance = new FlowInstance();
        h.instance.setVariable("{\"other\":1}");
        assertFalse((Boolean) h.service.checkpointRecover(TASK_ID).get("recoverable"));
    }

    @Test
    @DisplayName("checkpointRecover：variable 含 de.ctx key → recoverable=true 且 checkpoint 回显原串")
    void checkpoint_variableWithKey_recoverable() {
        Harness h = new Harness();
        String var = "{\"" + DeVariableConstants.VARIABLE_KEY + "\":{\"v\":3}}";
        h.instance = new FlowInstance();
        h.instance.setVariable(var);
        Map<String, Object> out = h.service.checkpointRecover(TASK_ID);
        assertTrue((Boolean) out.get("recoverable"));
        assertEquals(var, out.get("checkpoint"));
        assertEquals(TASK_ID, out.get("taskId"));
    }

    // ════════════════ lateArrivals ════════════════

    @Test
    @DisplayName("lateArrivals：taskId 为 null 必须 fail-closed（SY-08 前缀）")
    void late_null_throws() {
        ServiceException ex = assertThrows(ServiceException.class,
            () -> new Harness().service.lateArrivals(null));
        assertTrue(ex.getMessage().contains("SY-08"));
    }

    @Test
    @DisplayName("lateArrivals：任务不存在必须抛")
    void late_taskMissing_throws() {
        Harness h = new Harness();
        h.task = null;
        assertThrows(ServiceException.class, () -> h.service.lateArrivals(TASK_ID));
    }

    @Test
    @DisplayName("★lateArrivals：按 message tag 三分类，无 tag 记录丢弃，validated 恒 true")
    void late_classifiesByTag() {
        Harness h = new Harness();
        h.hisTrail = List.of(
            his(1L, "[SY-08] late contribution"),
            his(2L, "[SY-08] withdrawn resource"),
            his(3L, "[SY-08] node-change detected"),
            his(4L, "unrelated plain message"));
        Map<String, Object> out = h.service.lateArrivals(TASK_ID);
        assertEquals(1, ((List<?>) out.get("lateContributions")).size());
        assertEquals(1, ((List<?>) out.get("withdrawnResources")).size());
        assertEquals(1, ((List<?>) out.get("submittedNodeChanges")).size());
        assertTrue((Boolean) out.get("validated"));
    }

    // ════════════════ handoverPackageValidate ════════════════

    @Test
    @DisplayName("handoverPackageValidate：taskId 为 null 必须 fail-closed（SY-09 前缀）")
    void handover_null_throws() {
        ServiceException ex = assertThrows(ServiceException.class,
            () -> new Harness().service.handoverPackageValidate(null));
        assertTrue(ex.getMessage().contains("SY-09"));
    }

    @Test
    @DisplayName("handoverPackageValidate：空轨迹 → packageSize=0、readability=0")
    void handover_emptyTrail() {
        Harness h = new Harness();
        h.hisTrail = List.of();
        Map<String, Object> out = h.service.handoverPackageValidate(TASK_ID);
        assertEquals(0, out.get("packageSize"));
        assertEquals(0.0, out.get("readability"), "空轨迹可读度必须为 0（三元表达式提升为 double）");
        assertTrue(((List<?>) out.get("contributionVersions")).isEmpty());
    }

    @Test
    @DisplayName("★handoverPackageValidate：非空轨迹 → packageSize 匹配、版本序列映射 id、readability=1.0")
    void handover_nonEmptyTrail() {
        Harness h = new Harness();
        h.hisTrail = List.of(his(10L, "a"), his(11L, "b"), his(12L, "c"));
        Map<String, Object> out = h.service.handoverPackageValidate(TASK_ID);
        assertEquals(3, out.get("packageSize"));
        assertEquals(List.of(10L, 11L, 12L), out.get("contributionVersions"), "必须按轨迹顺序映射 id");
        assertEquals(1.0, out.get("readability"), "非空轨迹可读度必须为 1.0");
        assertTrue((Boolean) out.get("validated"));
    }

    // ════════════════ fixtures ════════════════

    private static DeSyCorrectionBo correctionBo() {
        DeSyCorrectionBo bo = new DeSyCorrectionBo();
        bo.setTaskId(TASK_ID);
        bo.setSummary("旧摘要");
        bo.setConflictingFact("冲突事实");
        return bo;
    }

    private static FlowTask task() {
        FlowTask t = new FlowTask();
        t.setId(TASK_ID);
        t.setInstanceId(INST_ID);
        t.setDefinitionId(5L);
        t.setNodeCode("n1");
        t.setNodeName("节点1");
        t.setNodeType(1);
        t.setFlowStatus(DeP1Constants.FLOW_STATUS_FINISHED);
        t.setCreateTime(new Date());
        t.setUpdateTime(new Date());
        return t;
    }

    private static FlowHisTask his(Long id, String message) {
        FlowHisTask h = new FlowHisTask();
        h.setId(id);
        h.setMessage(message);
        h.setNodeCode("n1");
        h.setNodeName("节点1");
        h.setCreateTime(new Date());
        return h;
    }

    /** 捕获型夹具：task/instance 可编程返回；hisTrail 为 selectList 返回；insert 全量记录。 */
    private static final class Harness {
        final DeSyServiceImpl service;
        final List<FlowHisTask> inserted = new ArrayList<>();
        FlowTask task = task();
        FlowInstance instance;
        List<FlowHisTask> hisTrail = List.of();

        Harness() {
            FlwTaskMapper taskMapper = stub(FlwTaskMapper.class, (m, a) -> {
                if ("selectById".equals(m.getName())) {
                    return task;
                }
                return defaultValue(m.getReturnType());
            });
            FlwInstanceMapper instMapper = stub(FlwInstanceMapper.class, (m, a) -> {
                if ("selectById".equals(m.getName())) {
                    return instance;
                }
                return defaultValue(m.getReturnType());
            });
            FlwHisTaskMapper hisMapper = stub(FlwHisTaskMapper.class, (m, a) -> {
                switch (m.getName()) {
                    case "insert" -> {
                        inserted.add((FlowHisTask) a[0]);
                        return 1;
                    }
                    case "selectList" -> {
                        return hisTrail;
                    }
                    default -> {
                        return defaultValue(m.getReturnType());
                    }
                }
            });
            this.service = new DeSyServiceImpl(taskMapper, instMapper, hisMapper, new ObjectMapper());
        }

        @SuppressWarnings("unchecked")
        private static <T> T stub(Class<T> iface, Handler handler) {
            return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface},
                (proxy, m, a) -> {
                    Object r = handler.handle(m, a == null ? new Object[0] : a);
                    return r != null ? r : defaultValue(m.getReturnType());
                });
        }

        private interface Handler {
            Object handle(java.lang.reflect.Method method, Object[] args) throws Exception;
        }

        private static Object defaultValue(Class<?> type) {
            if (type == boolean.class || type == Boolean.class) {
                return false;
            }
            if (type == int.class || type == Integer.class) {
                return 0;
            }
            if (type == long.class || type == Long.class) {
                return 0L;
            }
            if (List.class.isAssignableFrom(type)) {
                return List.of();
            }
            return null;
        }
    }
}
