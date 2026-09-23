package org.dromara.workflow.de.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.de.domain.DeSysOperLog;
import org.springframework.stereotype.Component;

/**
 * {@code sys_oper_log} 镜像 Mapper（SY-03 系统操作日志 · 2026-09-23）。
 *
 * <p><b>类名 + bean 名同时显式声明</b>：
 * <ol>
 *   <li>类名 = {@code DeSysOperLogMapper} —— 避开 ruoyi-system 同名 mapper。</li>
 *   <li>bean 名 = {@code deSysOperLogMapper} —— 避开 bean 冲突。</li>
 * </ol>
 *
 * <p><b>只读</b>：本 mapper 只暴露查询接口（{@code selectPage / selectList /
 * selectById / deleteByIds / 物理全清}），不暴露 INSERT —— 操作日志由底座
 * controller 自动写入。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Component("deSysOperLogMapper")
public interface DeSysOperLogMapper extends BaseMapperPlus<DeSysOperLog, DeSysOperLog> {
}