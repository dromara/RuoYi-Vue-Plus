package org.dromara.workflow.de.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.de.domain.DeSysConfig;
import org.springframework.stereotype.Component;

/**
 * {@code sys_config} 镜像 Mapper（SY-02 系统配置 · 2026-09-23）。
 *
 * <p><b>类名 + bean 名同时显式声明</b>：
 * <ol>
 *   <li>类名 = {@code DeSysConfigMapper} —— 避开 ruoyi-system 同名 mapper。</li>
 *   <li>bean 名 = {@code deSysConfigMapper} —— 避开 bean 冲突。</li>
 * </ol>
 *
 * @author AIP
 * @since 2026-09-23
 */
@Component("deSysConfigMapper")
public interface DeSysConfigMapper extends BaseMapperPlus<DeSysConfig, DeSysConfig> {
}