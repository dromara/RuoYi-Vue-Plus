package com.ruoyi.quartz.service.impl;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.mybatisplus.core.ServicePlusImpl;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.quartz.domain.SysJobLog;
import com.ruoyi.quartz.mapper.SysJobLogMapper;
import com.ruoyi.quartz.service.ISysJobLogService;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 定时任务调度日志信息 服务层
 *
 * @author ruoyi
 */
@Service
public class SysJobLogServiceImpl extends ServicePlusImpl<SysJobLogMapper, SysJobLog, SysJobLog> implements ISysJobLogService {

    @Override
    public TableDataInfo<SysJobLog> selectPageJobLogList(SysJobLog jobLog) {
        Map<String, Object> params = jobLog.getParams();
        LambdaQueryWrapper<SysJobLog> lqw = new LambdaQueryWrapper<SysJobLog>()
                .like(StrUtil.isNotBlank(jobLog.getJobName()), SysJobLog::getJobName, jobLog.getJobName())
                .eq(StrUtil.isNotBlank(jobLog.getJobGroup()), SysJobLog::getJobGroup, jobLog.getJobGroup())
                .eq(StrUtil.isNotBlank(jobLog.getStatus()), SysJobLog::getStatus, jobLog.getStatus())
                .like(StrUtil.isNotBlank(jobLog.getInvokeTarget()), SysJobLog::getInvokeTarget, jobLog.getInvokeTarget())
                .apply(Validator.isNotEmpty(params.get("beginTime")),
                        "date_format(create_time,'%y%m%d') >= date_format({0},'%y%m%d')",
                        params.get("beginTime"))
                .apply(Validator.isNotEmpty(params.get("endTime")),
                        "date_format(create_time,'%y%m%d') <= date_format({0},'%y%m%d')",
                        params.get("endTime"));
        return PageUtils.buildDataInfo(page(PageUtils.buildPage(), lqw));
    }

    /**
     * 获取quartz调度器日志的计划任务
     *
     * @param jobLog 调度日志信息
     * @return 调度任务日志集合
     */
    @Override
    public List<SysJobLog> selectJobLogList(SysJobLog jobLog) {
        Map<String, Object> params = jobLog.getParams();
        return list(new LambdaQueryWrapper<SysJobLog>()
                .like(StrUtil.isNotBlank(jobLog.getJobName()), SysJobLog::getJobName, jobLog.getJobName())
                .eq(StrUtil.isNotBlank(jobLog.getJobGroup()), SysJobLog::getJobGroup, jobLog.getJobGroup())
                .eq(StrUtil.isNotBlank(jobLog.getStatus()), SysJobLog::getStatus, jobLog.getStatus())
                .like(StrUtil.isNotBlank(jobLog.getInvokeTarget()), SysJobLog::getInvokeTarget, jobLog.getInvokeTarget())
                .apply(Validator.isNotEmpty(params.get("beginTime")),
                        "date_format(create_time,'%y%m%d') >= date_format({0},'%y%m%d')",
                        params.get("beginTime"))
                .apply(Validator.isNotEmpty(params.get("endTime")),
                        "date_format(create_time,'%y%m%d') <= date_format({0},'%y%m%d')",
                        params.get("endTime")));
    }

    /**
     * 通过调度任务日志ID查询调度信息
     *
     * @param jobLogId 调度任务日志ID
     * @return 调度任务日志对象信息
     */
    @Override
    public SysJobLog selectJobLogById(Long jobLogId) {
        return getById(jobLogId);
    }

    /**
     * 新增任务日志
     *
     * @param jobLog 调度日志信息
     */
    @Override
    public void addJobLog(SysJobLog jobLog) {
        baseMapper.insert(jobLog);
    }

    /**
     * 批量删除调度日志信息
     *
     * @param logIds 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteJobLogByIds(Long[] logIds) {
        return baseMapper.deleteBatchIds(Arrays.asList(logIds));
    }

    /**
     * 删除任务日志
     *
     * @param jobId 调度日志ID
     */
    @Override
    public int deleteJobLogById(Long jobId) {
        return baseMapper.deleteById(jobId);
    }

    /**
     * 清空任务日志
     */
    @Override
    public void cleanJobLog() {
        remove(new LambdaQueryWrapper<>());
    }
}
