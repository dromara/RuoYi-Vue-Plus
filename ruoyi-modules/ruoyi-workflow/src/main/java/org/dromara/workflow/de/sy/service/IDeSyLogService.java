package org.dromara.workflow.de.sy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dromara.workflow.de.sy.domain.bo.DeSyLogQueryBo;
import org.dromara.workflow.de.sy.domain.vo.DeSyLogVo;

import java.util.Date;
import java.util.List;

/**
 * SY-03 系统日志服务接口（2026-09-23）。
 *
 * <p>包含 sys_oper_log 操作日志（本地镜像 SELECT）+ sj_job_log_message
 * SnailJob 调度日志（HTTP 桥接 ruoyi-snailjob-server 微服务）。
 *
 * <p><b>H-2 安全修复（2026-09-23）</b>：{@link #cleanOperLog(Date)} 必须传 cutoffDate，
 * 防止 {@code delete(null)} 物理全清审计表。{@link #cleanOperLog()} 无参方法已 deprecated。
 *
 * @author AIP
 * @since 2026-09-23
 */
public interface IDeSyLogService {

    IPage<DeSyLogVo> page(DeSyLogQueryBo query, Long pageNum, Long pageSize);

    List<DeSyLogVo> list(DeSyLogQueryBo query);

    DeSyLogVo getInfo(Long operId);

    /**
     * 安全清理：删除 cutoffDate 之前的所有 sys_oper_log 行（H-2 修复后 · 2026-09-23）。
     *
     * @param cutoffDate 截止时间（必须非 null 且早于当前时刻；最多 30 天前）
     * @return 实际删除行数
     */
    int cleanOperLog(Date cutoffDate);

    /**
     * @deprecated 安全审计 H-2：不允许物理全清，必须用 {@link #cleanOperLog(Date)}。
     */
    @Deprecated
    int cleanOperLog();

    int remove(Long[] operIds);
}