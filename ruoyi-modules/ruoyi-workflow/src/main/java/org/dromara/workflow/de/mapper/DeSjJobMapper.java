package org.dromara.workflow.de.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.de.domain.DeSjJob;
import org.springframework.stereotype.Component;

/**
 * {@code sj_job} 镜像 Mapper（AT-01 自动化创建 · 2026-09-25）。
 *
 * <p>范式与 {@link DeSysOperLogMapper} 一致：类名 + bean 名显式声明
 * （{@code deSjJobMapper}），避开 SnailJob server 侧同名 bean。
 *
 * @author AIP
 * @since 2026-09-25
 */
@Component("deSjJobMapper")
public interface DeSjJobMapper extends BaseMapperPlus<DeSjJob, DeSjJob> {
}
