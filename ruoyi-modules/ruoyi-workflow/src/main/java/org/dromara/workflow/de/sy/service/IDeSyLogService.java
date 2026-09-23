package org.dromara.workflow.de.sy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dromara.workflow.de.sy.domain.bo.DeSyLogQueryBo;
import org.dromara.workflow.de.sy.domain.vo.DeSyLogVo;

import java.util.List;

/**
 * SY-03 系统日志服务接口（2026-09-23）。
 *
 * <p>包含 sys_oper_log 操作日志（本地镜像 SELECT）+ sj_job_log_message
 * SnailJob 调度日志（HTTP 桥接 ruoyi-snailjob-server 微服务）。
 *
 * @author AIP
 * @since 2026-09-23
 */
public interface IDeSyLogService {

    IPage<DeSyLogVo> page(DeSyLogQueryBo query, Long pageNum, Long pageSize);

    List<DeSyLogVo> list(DeSyLogQueryBo query);

    DeSyLogVo getInfo(Long operId);

    int cleanOperLog();

    int remove(Long[] operIds);
}