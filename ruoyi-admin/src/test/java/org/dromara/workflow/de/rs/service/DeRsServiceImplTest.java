package org.dromara.workflow.de.rs.service;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.system.api.OssService;
import org.dromara.system.api.domain.OssDTO;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.de.constants.DeP1Constants;
import org.dromara.workflow.de.rs.domain.bo.DeRsConfirmBo;
import org.dromara.workflow.de.rs.domain.bo.DeRsCrossCheckBo;
import org.dromara.workflow.de.rs.domain.bo.DeRsDecideBo;
import org.dromara.workflow.mapper.FlwHisTaskMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RS 域 {@link DeRsServiceImpl} 纯逻辑单测（T1-W5，不启 Spring、零 DB）。
 *
 * <p>范式与 W1/W3/W4 一致：Proxy 捕获型 stub mapper + 构造注入直调。
 *
 * <p>钉死的业务不变量：
 * <ul>
 *   <li>★RS-05 decision → skipType 值域映射（DDL 约束 PASS/REJECT/NONE）：
 *       accept→PASS · reject→REJECT · require-more/未知→NONE；</li>
 *   <li>★RS-07 action → skipType 值域映射：confirm→PASS · rollback/compensate→REJECT ·
 *       permission-denied/cancel/未知→NONE；</li>
 *   <li>decide/confirm：任务不存在零 insert；留痕 flowStatus=FINISHED、message 前缀
 *       [RS-05]/[RS-07]、关联字段从 task 拷贝、回显请求 taskId 非 DB id；</li>
 *   <li>★crossCheck（RS-06 真实比对）：逐个期望 ossId 经 OssService 回查 sys_oss，
 *       全部解析为非空 URL 对象才 true；部分缺失/空产物/空 URL → false；
 *       只读不产生 insert。</li>
 * </ul>
 *
 * @author AIP
 */
@Tag("dev")
@DisplayName("RS 域 DeRsServiceImpl：决议 / 交叉核验 / 确认（decision/action→skipType 值域映射，纯逻辑）")
class DeRsServiceImplTest {

    private static final Long TASK_ID = 42L;
    private static final Long INST_ID = 99L;

    // ════════════════ decide：RS-05 decision→skipType 值域 ════════════════

    @Test
    @DisplayName("decide：任务不存在必须抛，零 insert")
    void decide_taskMissing_throws() {
        Harness h = new Harness();
        h.task = null;
        assertThrows(ServiceException.class, () -> h.service.decide(decideBo("accept", "ok")));
        assertEquals(0, h.inserted.size());
    }

    @Test
    @DisplayName("★decide：accept → skipType=PASS，flowStatus=FINISHED，message 前缀 [RS-05]")
    void decide_accept_mapsPass() {
        Harness h = new Harness();
        Long r = h.service.decide(decideBo("accept", "同意结案"));
        assertEquals(TASK_ID, r, "必须回显请求 taskId");
        assertEquals(1, h.inserted.size());
        FlowHisTask his = h.inserted.get(0);
        assertEquals(SkipType.PASS.getKey(), his.getSkipType(), "accept 必须映射 PASS");
        assertEquals(DeP1Constants.FLOW_STATUS_FINISHED, his.getFlowStatus());
        assertTrue(his.getMessage().startsWith("[RS-05] accept"));
        assertTrue(his.getMessage().contains("同意结案"), "comment 必须入 message");
        assertEquals(TASK_ID, his.getTaskId());
        assertEquals(INST_ID, his.getInstanceId(), "instanceId 从任务拷贝");
    }

    @Test
    @DisplayName("★decide：reject → skipType=REJECT")
    void decide_reject_mapsReject() {
        Harness h = new Harness();
        h.service.decide(decideBo("reject", "证据不足"));
        assertEquals(SkipType.REJECT.getKey(), h.inserted.get(0).getSkipType());
    }

    @Test
    @DisplayName("decide：require-more → skipType=NONE（无流转动作）")
    void decide_requireMore_mapsNone() {
        Harness h = new Harness();
        h.service.decide(decideBo("require-more", "补充材料"));
        assertEquals(SkipType.NONE.getKey(), h.inserted.get(0).getSkipType());
    }

    @Test
    @DisplayName("★decide：未知 decision → 兜底 NONE（值域闭合，不得抛/不得误映射）")
    void decide_unknown_mapsNone() {
        Harness h = new Harness();
        h.service.decide(decideBo("frobnicate", "?"));
        assertEquals(SkipType.NONE.getKey(), h.inserted.get(0).getSkipType());
    }

    // ════════════════ confirm：RS-07 action→skipType 值域 ════════════════

    @Test
    @DisplayName("confirm：任务不存在必须抛，零 insert")
    void confirm_taskMissing_throws() {
        Harness h = new Harness();
        h.task = null;
        assertThrows(ServiceException.class, () -> h.service.confirm(confirmBo("confirm", "r")));
        assertEquals(0, h.inserted.size());
    }

    @Test
    @DisplayName("★confirm：action=confirm → skipType=PASS，message 前缀 [RS-07]")
    void confirm_action_mapsPass() {
        Harness h = new Harness();
        Long r = h.service.confirm(confirmBo("confirm", "交接完成"));
        assertEquals(TASK_ID, r);
        FlowHisTask his = h.inserted.get(0);
        assertEquals(SkipType.PASS.getKey(), his.getSkipType());
        assertTrue(his.getMessage().startsWith("[RS-07] confirm"));
        assertTrue(his.getMessage().contains("交接完成"));
    }

    @Test
    @DisplayName("confirm：action=rollback → skipType=REJECT")
    void confirm_rollback_mapsReject() {
        Harness h = new Harness();
        h.service.confirm(confirmBo("rollback", "回退"));
        assertEquals(SkipType.REJECT.getKey(), h.inserted.get(0).getSkipType());
    }

    @Test
    @DisplayName("confirm：action=compensate → skipType=REJECT")
    void confirm_compensate_mapsReject() {
        Harness h = new Harness();
        h.service.confirm(confirmBo("compensate", "补偿"));
        assertEquals(SkipType.REJECT.getKey(), h.inserted.get(0).getSkipType());
    }

    @Test
    @DisplayName("confirm：action=permission-denied → skipType=NONE")
    void confirm_permissionDenied_mapsNone() {
        Harness h = new Harness();
        h.service.confirm(confirmBo("permission-denied", "无权"));
        assertEquals(SkipType.NONE.getKey(), h.inserted.get(0).getSkipType());
    }

    @Test
    @DisplayName("confirm：action=cancel → skipType=NONE")
    void confirm_cancel_mapsNone() {
        Harness h = new Harness();
        h.service.confirm(confirmBo("cancel", "取消"));
        assertEquals(SkipType.NONE.getKey(), h.inserted.get(0).getSkipType());
    }

    @Test
    @DisplayName("★confirm：未知 action → 兜底 NONE（值域闭合）")
    void confirm_unknown_mapsNone() {
        Harness h = new Harness();
        h.service.confirm(confirmBo("weird", "?"));
        assertEquals(SkipType.NONE.getKey(), h.inserted.get(0).getSkipType());
    }

    // ════════════════ crossCheck ════════════════

    @Test
    @DisplayName("crossCheck：任务不存在必须抛")
    void crossCheck_taskMissing_throws() {
        Harness h = new Harness();
        h.task = null;
        DeRsCrossCheckBo bo = new DeRsCrossCheckBo();
        bo.setTaskId(TASK_ID);
        assertThrows(ServiceException.class, () -> h.service.crossCheck(bo));
    }

    @Test
    @DisplayName("★crossCheck：全部期望 ossId 解析为真实对象（非空 URL）→ true")
    void crossCheck_allResolved_passes() {
        Harness h = new Harness();
        h.ossRows = List.of(oss(1L, "/file/1"), oss(2L, "/file/2"));
        DeRsCrossCheckBo bo = crossCheckBo(Map.of("1", "h1", "2", "h2"));
        assertTrue(h.service.crossCheck(bo), "全部产物回查命中必须通过");
        assertEquals(0, h.inserted.size(), "交叉核验只读，不得写历史");
    }

    @Test
    @DisplayName("★crossCheck：部分 ossId 查无对象（部分成功）→ false")
    void crossCheck_partialMissing_fails() {
        Harness h = new Harness();
        h.ossRows = List.of(oss(1L, "/file/1"));
        DeRsCrossCheckBo bo = crossCheckBo(Map.of("1", "h1", "2", "h2"));
        assertFalse(h.service.crossCheck(bo), "任一期望产物缺失必须红");
    }

    @Test
    @DisplayName("★crossCheck：全部查无（失败）→ false")
    void crossCheck_allMissing_fails() {
        Harness h = new Harness();
        h.ossRows = List.of();
        DeRsCrossCheckBo bo = crossCheckBo(Map.of("1", "h1"));
        assertFalse(h.service.crossCheck(bo));
    }

    @Test
    @DisplayName("crossCheck：expected 为空（不确定，无产物可核）→ false")
    void crossCheck_emptyExpected_fails() {
        Harness h = new Harness();
        DeRsCrossCheckBo bo = crossCheckBo(Map.of());
        assertFalse(h.service.crossCheck(bo));
        bo.setExpected(null);
        assertFalse(h.service.crossCheck(bo));
    }

    @Test
    @DisplayName("crossCheck：对象 URL 为空 = 回执不指向真实外部对象 → false")
    void crossCheck_blankUrl_fails() {
        Harness h = new Harness();
        h.ossRows = List.of(oss(1L, "  "));
        DeRsCrossCheckBo bo = crossCheckBo(Map.of("1", "h1"));
        assertFalse(h.service.crossCheck(bo));
    }

    // ════════════════ fixtures ════════════════

    private static DeRsDecideBo decideBo(String decision, String comment) {
        DeRsDecideBo bo = new DeRsDecideBo();
        bo.setTaskId(TASK_ID);
        bo.setDecision(decision);
        bo.setComment(comment);
        return bo;
    }

    private static DeRsCrossCheckBo crossCheckBo(Map<String, String> expected) {
        DeRsCrossCheckBo bo = new DeRsCrossCheckBo();
        bo.setTaskId(TASK_ID);
        bo.setExpected(expected);
        return bo;
    }

    private static OssDTO oss(Long ossId, String url) {
        OssDTO dto = new OssDTO();
        dto.setOssId(ossId);
        dto.setUrl(url);
        return dto;
    }

    private static DeRsConfirmBo confirmBo(String action, String reason) {
        DeRsConfirmBo bo = new DeRsConfirmBo();
        bo.setTaskId(TASK_ID);
        bo.setAction(action);
        bo.setReason(reason);
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
        return t;
    }

    /** 捕获型夹具：task / ossRows 可编程返回；insert 全量记录。 */
    private static final class Harness {
        final DeRsServiceImpl service;
        final List<FlowHisTask> inserted = new ArrayList<>();
        FlowTask task = task();
        List<OssDTO> ossRows = List.of();

        Harness() {
            FlwTaskMapper taskMapper = stub(FlwTaskMapper.class, (m, a) -> {
                if ("selectById".equals(m.getName())) {
                    return task;
                }
                return defaultValue(m.getReturnType());
            });
            FlwHisTaskMapper hisMapper = stub(FlwHisTaskMapper.class, (m, a) -> {
                if ("insert".equals(m.getName())) {
                    inserted.add((FlowHisTask) a[0]);
                    return 1;
                }
                return defaultValue(m.getReturnType());
            });
            OssService ossService = stub(OssService.class, (m, a) -> {
                if ("selectByIds".equals(m.getName())) {
                    return new ArrayList<>(ossRows);
                }
                return defaultValue(m.getReturnType());
            });
            this.service = new DeRsServiceImpl(taskMapper, hisMapper, ossService);
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
