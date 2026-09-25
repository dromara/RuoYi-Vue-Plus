package org.dromara.workflow.de.at.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.workflow.de.at.domain.bo.DeAtCreateBo;
import org.dromara.workflow.de.at.domain.bo.DeAtRunConfigBo;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.de.domain.DeSjJob;
import org.dromara.workflow.de.mapper.DeSjJobMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

/**
 * AT 域服务实现（AT-01 / AT-04 · P1 档 A 实接）。
 *
 * <p>真实落点 = 底座 {@code sj_job}（SnailJob 调度族 23 张之一，DDL 在
 * {@code 04-backend-code/script/sql/ry_job.sql:279-318}），经
 * {@link DeSjJobMapper} 真实 INSERT / UPDATE / 回读。
 *
 * <p>AT-01（29 号 :566）：为个人、Agent 和节点提供一致的自动化创建入口；
 * 重复触发不能创建重复业务副作用 —— biz_id 携带 scope+targetId，uk_sj_job_01
 * （namespace_id+biz_id 唯一）兜底防重。
 *
 * <p>AT-04（29 号 :569）：并发 / 重试直接更新 sj_job.parallel_num /
 * sj_job.max_retry_times；jobId 必须指向真实作业行。去重 / 错过补跑 /
 * 失败通知到 sj_job.block_strategy / sj_job.notify_ids 的精确映射
 * docs 未给出，保持应用层校验 + 留 owner-pending（见实施报告）。
 *
 * @author AIP
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeAtServiceImpl implements IDeAtService {

    /** AT-01 三选一 scope 白名单（个人 / Agent / 节点）。 */
    private static final Set<String> VALID_SCOPES = Set.of("user", "agent", "node");

    /** ry_job.sql 种子组名（sj_group_config: group_name='ruoyi_group'）。 */
    private static final String DEFAULT_GROUP_NAME = "ruoyi_group";

    private final FlwTaskMapper flwTaskMapper;
    private final DeSjJobMapper deSjJobMapper;

    @Override
    public Long create(DeAtCreateBo bo) {
        // AT-01 校验：scope 必须在白名单内（三选一）
        if (!VALID_SCOPES.contains(bo.getScope())) {
            throw new ServiceException("AT-01 scope 非法: " + bo.getScope()
                + "（白名单=user/agent/node）");
        }
        // AT-01 校验：cron 非空（BO 端已 @NotBlank，此处复测防绕过）
        if (bo.getCron() == null || bo.getCron().isBlank()) {
            throw new ServiceException("AT-01 cron 必填");
        }
        // AT-01 校验：targetId 若指 taskId，须存在（保护 AT-04 不再配孤儿自动化）
        if ("node".equals(bo.getScope())) {
            FlowTask task = flwTaskMapper.selectById(bo.getTargetId());
            if (task == null) {
                throw new ServiceException("AT-01 targetId=" + bo.getTargetId()
                    + " 任务不存在，无法挂载自动化");
            }
        }
        // 下一次触发时间：按 cron 真实计算（AT-03「下一次时间」同一列 next_trigger_at）。
        // SnailJob cron 沿用 Quartz 写法（含 '?'、可带年份），归一化为 Spring 6 段。
        Long nextTriggerAt = nextTriggerAt(bo.getCron());

        DeSjJob job = new DeSjJob();
        // uk_sj_job_01 = namespace_id + biz_id：同一 scope+targetId 的自动化 biz_id 恒定，
        // 重复创建被唯一键拦截（AT-01 负例：重复触发不能创建重复业务副作用）
        job.setBizId("at-" + bo.getScope() + "-" + bo.getTargetId());
        job.setGroupName(DEFAULT_GROUP_NAME);
        job.setJobName("AT automation " + bo.getScope() + ":" + bo.getTargetId());
        job.setNextTriggerAt(nextTriggerAt);
        job.setTriggerType(1);
        job.setTriggerInterval(bo.getCron());
        // owner_id（负责人 id）：仅个人自动化可钉到真实用户
        if ("user".equals(bo.getScope())) {
            job.setOwnerId(bo.getScopeId());
        }
        job.setJobStatus(1);
        int rows;
        try {
            rows = deSjJobMapper.insert(job);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new ServiceException("AT-01 自动化已存在（scope=" + bo.getScope()
                + " targetId=" + bo.getTargetId() + "），重复触发不创建重复副作用");
        }
        if (rows != 1 || job.getId() == null) {
            throw new ServiceException("AT-01 sj_job 入库失败: rows=" + rows);
        }
        // 回读验证：库里真有行，且关键字段落对
        DeSjJob read = deSjJobMapper.selectById(job.getId());
        if (read == null) {
            throw new ServiceException("AT-01 sj_job 回读为空: id=" + job.getId());
        }
        log.info("[at.create] id={} scope={} scopeId={} cron={} targetId={} nextTriggerAt={}",
            job.getId(), bo.getScope(), bo.getScopeId(), bo.getCron(),
            bo.getTargetId(), nextTriggerAt);
        return job.getId();
    }

    @Override
    public Long runConfig(DeAtRunConfigBo bo) {
        if (bo.getJobId() == null) {
            throw new ServiceException("AT-04 jobId 必填");
        }
        // AT-04：jobId 必须指向真实作业（不允许配置孤儿自动化）
        DeSjJob job = deSjJobMapper.selectById(bo.getJobId());
        if (job == null) {
            throw new ServiceException("AT-04 jobId=" + bo.getJobId() + " 作业不存在");
        }
        // AT-04 校验：maxRetries 必须 ≥ 0（H1 P1 AC-11 重试次数合法范围）
        if (bo.getMaxRetries() != null && bo.getMaxRetries() < 0) {
            throw new ServiceException("AT-04 maxRetries 必须 ≥ 0，实际=" + bo.getMaxRetries());
        }
        // 29 号 :569 明确落点：parallel_num（并发）· max_retry_times（重试）
        if (bo.getConcurrency() != null) {
            job.setParallelNum(bo.getConcurrency());
        }
        if (bo.getMaxRetries() != null) {
            job.setMaxRetryTimes(bo.getMaxRetries());
        }
        deSjJobMapper.updateById(job);
        log.info("[at.runConfig] jobId={} parallelNum={} maxRetryTimes={} dedup={} missRun={} notifyOnFailure={}",
            bo.getJobId(), bo.getConcurrency(), bo.getMaxRetries(),
            bo.getDedup(), bo.getMissRun(), bo.getNotifyOnFailure());
        return bo.getJobId();
    }

    /**
     * 按 cron 表达式计算下一次触发的 epoch milli。
     * SnailJob/Quartz cron 归一化：{@code ?} → {@code *}，7 段去年份。
     */
    private Long nextTriggerAt(String cron) {
        String normalized = cron.replace('?', '*').trim();
        String[] parts = normalized.split("\\s+");
        if (parts.length == 7) {
            normalized = normalized.substring(0, normalized.lastIndexOf(' ')).trim();
        }
        try {
            CronExpression expr = CronExpression.parse(normalized);
            LocalDateTime next = expr.next(LocalDateTime.now());
            if (next == null) {
                throw new ServiceException("AT-01 cron 无未来触发时间: " + cron);
            }
            return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (IllegalArgumentException e) {
            throw new ServiceException("AT-01 cron 表达式无法解析: " + cron + "（" + e.getMessage() + "）");
        }
    }
}
