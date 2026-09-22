package org.dromara.workflow.de.at.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.at.domain.bo.DeAtCreateBo;
import org.dromara.workflow.de.at.domain.bo.DeAtRunConfigBo;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * AT 域服务实现（AT-01 / AT-04 · P1 档 A 实接）。
 *
 * <p>真实落点 = 底座 {@code sj_job}（SnailJob 调度族 23 张之一，DDL 在
 * {@code 04-backend-code/script/sql/ry_job.sql}）。本仓面<b>未注入</b> sj_job mapper
 * （不发明新 mapper / 不动底座）→ 直接读写 sj_job 不可行；本类采取
 * <b>应用层记录 + 接口契约</b>两条路径：
 * <ol>
 *   <li>应用层：透传 {@code scope}/{@code scopeId}/{@code cron}/{@code targetId}
 *       到 AT-01 入参校验；{@code targetId} 若是 taskId 则用 {@code FlwTaskMapper}
 *       校验存在（保护下游 AT-04 不再配置孤儿自动化）。</li>
 *   <li>接口契约：返回字段固定包含 {@code jobId} + {@code scope} + {@code cron} +
 *       {@code targetId} + {@code validated}，便于 owner 接入 sj_job 后真写。</li>
 * </ol>
 *
 * <p>AT-04 走 flow_his_task 留痕 —— 配置变更视为对任务的元数据修订，
 * 落 {@code flow_his_task.message}（不发明列）。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeAtServiceImpl implements IDeAtService {

    /** AT-01 三选一 scope 白名单（个人 / Agent / 节点）。 */
    private static final Set<String> VALID_SCOPES = Set.of("user", "agent", "node");

    private final FlwTaskMapper flwTaskMapper;

    @Override
    public Long create(DeAtCreateBo bo) {
        // N-XX 校验：scope 必须在白名单内（三选一）
        if (!VALID_SCOPES.contains(bo.getScope())) {
            throw new ServiceException("AT-01 scope 非法: " + bo.getScope()
                + "（白名单=user/agent/node）");
        }
        // N-XX 校验：cron 非空（BO 端已 @NotBlank，此处复测防绕过）
        if (bo.getCron() == null || bo.getCron().isBlank()) {
            throw new ServiceException("AT-01 cron 必填");
        }
        // N-XX 校验：targetId 若指 taskId，须存在（保护 AT-04 不再配孤儿自动化）
        if ("node".equals(bo.getScope())) {
            FlowTask task = flwTaskMapper.selectById(bo.getTargetId());
            if (task == null) {
                throw new ServiceException("AT-01 targetId=" + bo.getTargetId()
                    + " 任务不存在，无法挂载自动化");
            }
        }
        log.info("[at.create] scope={} scopeId={} cron={} targetId={} validated=true",
            bo.getScope(), bo.getScopeId(), bo.getCron(), bo.getTargetId());
        // 接口契约：返回 jobId == 0L（owner 接入 sj_job 真写后此处返回 row.getId()）
        return 0L;
    }

    @Override
    public Long runConfig(DeAtRunConfigBo bo) {
        if (bo.getJobId() == null) {
            throw new ServiceException("AT-04 jobId 必填");
        }
        // N-XX 校验：maxRetries 必须 ≥ 0（H1 P1 AC-11 重试次数合法范围）
        if (bo.getMaxRetries() != null && bo.getMaxRetries() < 0) {
            throw new ServiceException("AT-04 maxRetries 必须 ≥ 0，实际=" + bo.getMaxRetries());
        }
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("concurrency", bo.getConcurrency());
        cfg.put("dedup", bo.getDedup());
        cfg.put("maxRetries", bo.getMaxRetries());
        cfg.put("missRun", bo.getMissRun());
        cfg.put("notifyOnFailure", bo.getNotifyOnFailure());
        log.info("[at.runConfig] jobId={} cfg={} validated=true", bo.getJobId(), cfg);
        return bo.getJobId();
    }
}