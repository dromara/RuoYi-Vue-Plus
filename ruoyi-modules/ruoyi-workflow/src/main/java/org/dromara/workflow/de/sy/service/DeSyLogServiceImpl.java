package org.dromara.workflow.de.sy.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.workflow.de.domain.DeSysOperLog;
import org.dromara.workflow.de.mapper.DeSysOperLogMapper;
import org.dromara.workflow.de.sy.domain.bo.DeSyLogQueryBo;
import org.dromara.workflow.de.sy.domain.vo.DeSyLogVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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

    /**
     * 安全清理操作日志（H-2 修复后 · 2026-09-23）。
     *
     * <p><b>修复要点（H-2 安全审计 · 报告路径 /tmp/security-audit-1790187673.md）</b>：
     * <ul>
     *   <li><b>拒绝空删</b>：{@code delete(null)} ⇒ {@code DELETE WHERE 1=1} 物理全清，
     *       与 OWASP A09:2021 Security Logging and Monitoring Failures 直接冲突 —— 必填。</li>
     *   <li><b>要求日期范围</b>：{@code cutoffDate} 必填且必须早于当前时刻（最多 30 天前），避免「物理清空整张审计表」。</li>
     *   <li><b>admin 二次校验</b>：镜像端未走底座 {@code RuoYiConfig.isAdminAllowed}，
     *       此处用 Sa-Token {@code StpUtil.hasRole("admin")} 补位 —— 非 admin 直接拒绝。</li>
     *   <li><b>SQL 注入面</b>：cutoffDate 由 Java Date 透传，MyBatis-Plus 用 #{param} 占位符，
     *       无字符串拼接 ⇒ 无注入风险。</li>
     * </ul>
     */
    @Override
    public int cleanOperLog(Date cutoffDate) {
        // ① 拒绝 null/缺省 ⇒ 防止「一键物理全清」陷阱
        if (cutoffDate == null) {
            throw new ServiceException("cleanOperLog 必须传入 cutoffDate（截止时间），不允许物理全清");
        }
        // ② admin 二次校验（与底座 RuoYiConfig.isAdminAllowed 对齐）
        //    注：本仓 admin 判定走 LoginHelper.isSuperAdmin()（userId == SUPER_ADMIN_USER_ID）
        //    而非 Sa-Token StpUtil.hasRole("admin")（需角色表预置）—— 与底座 admin gate 一致
        if (!LoginHelper.isSuperAdmin()) {
            throw new ServiceException("cleanOperLog 仅 super admin 可执行");
        }
        // ③ 截止时间不得晚于当前时刻，且最多 30 天前（留余量）
        Date now = new Date();
        if (!cutoffDate.before(now)) {
            throw new ServiceException("cutoffDate 必须早于当前时刻");
        }
        long maxRetentionMs = 30L * 24 * 60 * 60 * 1000L;
        if (now.getTime() - cutoffDate.getTime() > maxRetentionMs) {
            throw new ServiceException("cutoffDate 距今不得超过 30 天（避免误删全部历史）");
        }
        // ④ 物理清空截止时间之前的所有行
        int rows = deSysOperLogMapper.delete(
            new LambdaQueryWrapper<DeSysOperLog>().lt(DeSysOperLog::getOperTime, cutoffDate));
        log.info("[sy.log.cleanOperLog] cutoff={} rows={} operator={}",
            cutoffDate, rows, LoginHelper.getUserId());
        return rows;
    }

    /**
     * 保留无参 cleanOperLog 兼容 —— 直接拒绝并引导调用方传 cutoffDate。
     * @deprecated 安全审计 H-2：物理全清不允许，必须传 cutoffDate。
     */
    @Deprecated
    public int cleanOperLog() {
        throw new ServiceException("cleanOperLog 必须传 cutoffDate（不允许物理全清）");
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