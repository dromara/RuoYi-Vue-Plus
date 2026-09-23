package org.dromara.workflow.de.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.de.domain.DeSysNotice;
import org.springframework.stereotype.Component;

/**
 * {@code sys_notice} 镜像 Mapper（NC-04 通知中心 CRUD · 2026-09-23）。
 *
 * <p><b>类名 + bean 名同时显式声明</b>：
 * <ol>
 *   <li>类名 = {@code DeSysNoticeMapper} —— 避开 ruoyi-system 的同名
 *       {@code org.dromara.system.mapper.SysNoticeMapper}，避免
 *       MyBatis alias 冲突（{@code The alias 'SysNotice' is already mapped}）。</li>
 *   <li>bean 名 = {@code deSysNoticeMapper} —— 同理避开
 *       {@code ConflictingBeanDefinitionException}。</li>
 * </ol>
 *
 * <p>实体类亦同步重命名为 {@link DeSysNotice}（避开 ruoyi-system 的
 * {@code org.dromara.system.domain.SysNotice}，避免
 * {@code typeAliasesPackage: org.dromara.**.domain} 扫描冲突）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Component("deSysNoticeMapper")
public interface DeSysNoticeMapper extends BaseMapperPlus<DeSysNotice, DeSysNotice> {
}