package org.dromara.workflow.de.at.service;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.de.at.domain.bo.DeAtCreateBo;
import org.dromara.workflow.de.at.domain.bo.DeAtRunConfigBo;
import org.dromara.workflow.de.domain.DeSjJob;
import org.dromara.workflow.de.mapper.DeSjJobMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AT 域 {@link DeAtServiceImpl} 纯逻辑单测（2026-09-25，不启 Spring、零 DB）。
 *
 * <p>Proxy 捕获型 stub mapper（模拟生成键与回读），钉死的业务不变量：
 * <ul>
 *   <li>★AT-01：校验失败零 insert；合法创建真实 INSERT sj_job 并回读，
 *       返回 DB 生成 id（不得再返回 0L）；trigger_type=1、trigger_interval=cron、
 *       next_trigger_at 已计算、biz_id 防重；user scope 钉 owner_id，agent/node 不钉；</li>
 *   <li>★AT-04：jobId 必须指向真实作业行；maxRetries ≥ 0；
 *       并发/重试更新到 sj_job.parallel_num / max_retry_times。</li>
 * </ul>
 *
 * @author AIP
 */
@Tag("dev")
@DisplayName("AT 域 DeAtServiceImpl：AT-01 真实入库 / AT-04 真实更新（纯逻辑）")
class DeAtServiceImplTest {

    private static final Long TASK_ID = 42L;
    private static final Long JOB_ID = 7001L;

    // ════════════════ AT-01 create：校验失败零写入 ════════════════

    @Test
    @DisplayName("create：scope 非法必须抛，零 insert")
    void create_invalidScope_throws() {
        Harness h = new Harness();
        assertThrows(ServiceException.class, () -> h.service.create(createBo("org", 1L, "0 0 * * * ?", TASK_ID)));
        assertEquals(0, h.inserted.size());
    }

    @Test
    @DisplayName("create：cron 空必须抛，零 insert")
    void create_blankCron_throws() {
        Harness h = new Harness();
        assertThrows(ServiceException.class, () -> h.service.create(createBo("user", 1L, "  ", TASK_ID)));
        assertEquals(0, h.inserted.size());
    }

    @Test
    @DisplayName("create：node scope 目标任务不存在必须抛，零 insert")
    void create_nodeTaskMissing_throws() {
        Harness h = new Harness();
        h.task = null;
        assertThrows(ServiceException.class, () -> h.service.create(createBo("node", 1L, "0 0 * * * ?", TASK_ID)));
        assertEquals(0, h.inserted.size());
    }

    @Test
    @DisplayName("create：cron 无法解析必须抛，零 insert")
    void create_unparseableCron_throws() {
        Harness h = new Harness();
        assertThrows(ServiceException.class, () -> h.service.create(createBo("user", 1L, "not-a-cron", TASK_ID)));
        assertEquals(0, h.inserted.size());
    }

    // ════════════════ AT-01 create：真实入库 + 回读 ════════════════

    @Test
    @DisplayName("★create：user scope 真实入库，返回生成 id（≠0），字段钉死，回读命中")
    void create_user_persists() {
        Harness h = new Harness();
        Long id = h.service.create(createBo("user", 99L, "0 0 * * * ?", TASK_ID));

        assertNotNull(id);
        assertEquals(JOB_ID, id, "必须返回 sj_job 生成主键，不得返回 0L");
        assertEquals(1, h.inserted.size());
        DeSjJob job = h.inserted.get(0);
        assertEquals("ruoyi_group", job.getGroupName());
        assertEquals(Integer.valueOf(1), job.getTriggerType(), "CRON 触发类型=1");
        assertEquals("0 0 * * * ?", job.getTriggerInterval());
        assertNotNull(job.getNextTriggerAt(), "next_trigger_at 必须真实计算");
        assertEquals(Long.valueOf(99L), job.getOwnerId(), "user scope 钉 owner_id=scopeId");
        assertNotNull(job.getBizId());
        assertEquals(1, h.readback.size(), "必须经 selectById 回读验证");
    }

    @Test
    @DisplayName("★create：agent scope owner_id 留空（无真实用户可钉）")
    void create_agent_ownerNull() {
        Harness h = new Harness();
        h.service.create(createBo("agent", 99L, "0 0 * * * ?", TASK_ID));
        assertNull(h.inserted.get(0).getOwnerId());
    }

    @Test
    @DisplayName("create：7 段带年份 Quartz cron 可归一化入库")
    void create_quartzYearCron_ok() {
        Harness h = new Harness();
        Long id = h.service.create(createBo("user", 99L, "0 0 * * * ? 2026", TASK_ID));
        assertEquals(JOB_ID, id);
    }

    @Test
    @DisplayName("create：回读为空必须抛（写而未达 = 失败）")
    void create_readbackNull_throws() {
        Harness h = new Harness();
        h.simulateReadbackMissing = true;
        assertThrows(ServiceException.class, () -> h.service.create(createBo("user", 99L, "0 0 * * * ?", TASK_ID)));
    }

    @Test
    @DisplayName("★create：同一 scope+targetId 重复创建必须被拦截（不产生重复副作用）")
    void create_duplicate_throws() {
        Harness h = new Harness();
        h.simulateDuplicate = true;
        assertThrows(ServiceException.class, () -> h.service.create(createBo("user", 99L, "0 0 * * * ?", TASK_ID)));
        assertEquals(0, h.inserted.size(), "唯一键拦截后不得有成功入库行");
    }

    // ════════════════ AT-04 runConfig：真实更新 ════════════════

    @Test
    @DisplayName("runConfig：jobId 无真实作业必须抛")
    void runConfig_jobMissing_throws() {
        Harness h = new Harness();
        assertThrows(ServiceException.class, () -> h.service.runConfig(runConfigBo(8888L)));
    }

    @Test
    @DisplayName("runConfig：maxRetries < 0 必须抛")
    void runConfig_negativeRetries_throws() {
        Harness h = new Harness();
        h.seedJob(JOB_ID);
        DeAtRunConfigBo bo = runConfigBo(JOB_ID);
        bo.setMaxRetries(-1);
        assertThrows(ServiceException.class, () -> h.service.runConfig(bo));
    }

    @Test
    @DisplayName("★runConfig：并发/重试真实更新到 sj_job")
    void runConfig_updatesColumns() {
        Harness h = new Harness();
        h.seedJob(JOB_ID);
        DeAtRunConfigBo bo = runConfigBo(JOB_ID);
        bo.setConcurrency(5);
        bo.setMaxRetries(3);

        h.service.runConfig(bo);

        assertEquals(1, h.updated.size());
        DeSjJob job = h.updated.get(0);
        assertEquals(Integer.valueOf(5), job.getParallelNum());
        assertEquals(Integer.valueOf(3), job.getMaxRetryTimes());
    }

    // ════════════════ fixtures ════════════════

    private static DeAtCreateBo createBo(String scope, Long scopeId, String cron, Long targetId) {
        DeAtCreateBo bo = new DeAtCreateBo();
        bo.setScope(scope);
        bo.setScopeId(scopeId);
        bo.setCron(cron);
        bo.setTargetId(targetId);
        return bo;
    }

    private static DeAtRunConfigBo runConfigBo(Long jobId) {
        DeAtRunConfigBo bo = new DeAtRunConfigBo();
        bo.setJobId(jobId);
        return bo;
    }

    private static FlowTask task() {
        FlowTask t = new FlowTask();
        t.setId(TASK_ID);
        return t;
    }

    /** 捕获型夹具：模拟 sj_job 生成键、回读与更新。 */
    private final class Harness {
        final DeAtServiceImpl service;
        final List<DeSjJob> inserted = new ArrayList<>();
        final List<DeSjJob> updated = new ArrayList<>();
        final Map<Long, DeSjJob> store = new HashMap<>();
        final List<DeSjJob> readback = new ArrayList<>();
        FlowTask task = task();
        boolean simulateReadbackMissing = false;
        boolean simulateDuplicate = false;
        private long nextId = JOB_ID;

        Harness() {
            FlwTaskMapper taskMapper = stub(FlwTaskMapper.class, (m, a) ->
                "selectById".equals(m.getName()) ? task : defaultValue(m.getReturnType()));
            DeSjJobMapper jobMapper = stub(DeSjJobMapper.class, (m, a) -> {
                switch (m.getName()) {
                    case "insert": {
                        if (simulateDuplicate) {
                            throw new org.springframework.dao.DuplicateKeyException(
                                "uk_sj_job_01 duplicate biz_id");
                        }
                        DeSjJob job = (DeSjJob) a[0];
                        job.setId(nextId++);
                        inserted.add(job);
                        store.put(job.getId(), job);
                        return 1;
                    }
                    case "selectById": {
                        DeSjJob job = simulateReadbackMissing ? null : store.get((Long) a[0]);
                        if (job != null) {
                            readback.add(job);
                        }
                        return job;
                    }
                    case "updateById":
                        updated.add((DeSjJob) a[0]);
                        return 1;
                    default:
                        return defaultValue(m.getReturnType());
                }
            });
            this.service = new DeAtServiceImpl(taskMapper, jobMapper);
        }

        void seedJob(Long id) {
            DeSjJob job = new DeSjJob();
            job.setId(id);
            store.put(id, job);
        }
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
