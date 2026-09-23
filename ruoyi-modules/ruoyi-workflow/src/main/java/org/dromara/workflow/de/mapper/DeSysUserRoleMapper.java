package org.dromara.workflow.de.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.workflow.de.domain.DeSysUserRole;
import org.springframework.stereotype.Component;

/**
 * {@code sys_user_role} 只读 Mapper（CX-10 · 2026-09-23 · 2026-09-23 重命名避 alias 冲突）。
 *
 * <p><b>类名 + bean 名同时显式声明</b>：
 * <ol>
 *   <li>类名 = {@code DeSysUserRoleMapper} —— 避开 ruoyi-system 同名 mapper，
 *       避免 MyBatis alias 冲突（{@code The alias 'SysUserRole' is already mapped}）。</li>
 *   <li>bean 名 = {@code deSysUserRoleMapper} —— 同理避开
 *       {@code ConflictingBeanDefinitionException}。</li>
 * </ol>
 *
 * <p><b>只读</b>：本模块不写 {@code sys_user_role}。CX-10 只做撤权后的实时判定。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Component("deSysUserRoleMapper")
public interface DeSysUserRoleMapper extends BaseMapperPlus<DeSysUserRole, DeSysUserRole> {
}