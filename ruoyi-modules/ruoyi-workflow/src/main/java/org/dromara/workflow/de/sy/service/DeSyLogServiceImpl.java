package org.dromara.workflow.de.sy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.workflow.de.domain.DeSysOperLog;
import org.dromara.workflow.de.mapper.DeSysOperLogMapper;
import org.dromara.workflow.de.sy.domain.bo.DeSyLogQueryBo;
import org.dromara.workflow.de.sy.domain.vo.DeSyLogVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SY-03 系统日志服务实现（2026-09-23）。
 *
 * <p><b>只读镜像</b>：本类不写 sys_oper_log —— 操作日志由底座 controller
 * 自动写入（{@code com.aop.LogAspect} 拦截 @Log 注解）。本类只暴露
 * SELECT / DELETE，DELETE 用于清空与按 ID 物理删除。
 *
 * <p><b>SnailJob 调度日志</b>：sj_job_log_message 在独立微服务
 * {@code ruoyi-snailjob-server}（{@code 04-backend-code/ruoyi-extend/ruoyi-snailjob-server}）
 * 的库中，不在本仓库可见；通过 HTTP 调用桥接，详见 {@link org.dromara.workflow.de.sy.client.SnailJobLogHttpClient}。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeSyLogServiceImpl implements IDeSyLogService {

    private final DeSysOperLogMapper deSysOperLogMapper;

    @Override
    public IPage<DeSyLogVo> page(DeSyLogQueryBo query, Long pageNum, Long pageSize) {
        Page<DeSysOperLog> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        IPage<DeSysOperLog> rows = deSysOperLogMapper.selectPage(page, buildWrapper(query));
        return rows.convert(this::toVo);
    }

    @Override
    public List<DeSyLogVo> list(DeSyLogQueryBo query) {
        List<DeSysOperLog> rows = deSysOperLogMapper.selectList(buildWrapper(query));
        List<DeSyLogVo> out = new ArrayList<>(rows.size());
        for (DeSysOperLog r : rows) {
            out.add(toVo(r));
        }
        return out;
    }

    @Override
    public DeSyLogVo getInfo(Long operId) {
        DeSysOperLog row = deSysOperLogMapper.selectById(operId);
        return row == null ? null : toVo(row);
    }

    @Override
    public int cleanOperLog() {
        // 物理全清（对应底座 monitor/sysoperlog/clean 端点的「全部清空」语义）
        int rows = deSysOperLogMapper.delete(null);
        log.info("[sy.log.cleanOperLog] 物理清空 rows={}", rows);
        return rows;
    }

    @Override
    public int remove(Long[] operIds) {
        if (operIds == null || operIds.length == 0) {
            return 0;
        }
        int rows = deSysOperLogMapper.deleteByIds(Arrays.asList(operIds));
        log.info("[sy.log.remove] ids={} rows={}", (Object[]) operIds, rows);
        return rows;
    }

    private LambdaQueryWrapper<DeSysOperLog> buildWrapper(DeSyLogQueryBo query) {
        LambdaQueryWrapper<DeSysOperLog> w = new LambdaQueryWrapper<>();
        if (query == null) {
            return w.orderByDesc(DeSysOperLog::getOperTime);
        }
        if (query.getTitle() != null && !query.getTitle().isBlank()) {
            w.like(DeSysOperLog::getTitle, query.getTitle());
        }
        if (query.getOperName() != null && !query.getOperName().isBlank()) {
            w.like(DeSysOperLog::getOperName, query.getOperName());
        }
        if (query.getBusinessType() != null) {
            w.eq(DeSysOperLog::getBusinessType, query.getBusinessType());
        }
        if (query.getStatus() != null) {
            w.eq(DeSysOperLog::getStatus, query.getStatus());
        }
        if (query.getOperIp() != null && !query.getOperIp().isBlank()) {
            w.like(DeSysOperLog::getOperIp, query.getOperIp());
        }
        w.orderByDesc(DeSysOperLog::getOperTime);
        return w;
    }

    private DeSyLogVo toVo(DeSysOperLog r) {
        DeSyLogVo vo = new DeSyLogVo();
        BeanUtils.copyProperties(r, vo);
        return vo;
    }
}