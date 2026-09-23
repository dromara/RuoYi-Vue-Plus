package org.dromara.workflow.de.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * {@code sys_user_role} 只读镜像实体（CX-10 · 2026-09-23）。
 *
 * <p><b>底座表</b>：{@code sys_user_role} —— DDL
 * {@code 04-backend-code/script/sql/ry_vue.sql:349-354}
 * （{@code user_id bigint(20) not null} / {@code role_id bigint(20) not null}，
 * 主键 {@code (user_id, role_id)}，索引 {@code idx_sys_user_role_rid(role_id)}）。
 *
 * <p><b>只读引用，不是写入路径</b>：29 号 §五 CX-10 的落点写的就是
 * 「{@code flow_instance.variable} + <b>只读引用</b> {@code sys_user_role.role_id}」。
 * 本类不承载任何 insert / update / delete —— 撤权由 ruoyi-system 的
 * 用户角色管理写入，本模块只实时读它。
 *
 * <p><b>为什么本模块要自带一份镜像</b>（三条，均可复核）：
 * <ol>
 *   <li>ruoyi-workflow 只依赖 ruoyi-api（接口 + DTO），拿不到 ruoyi-system 的
 *       {@code org.dromara.system.domain.SysUserRole}。</li>
 *   <li>两个 API 都没有「用户 → 角色 ID 列表」的方法：
 *       {@code RoleService} 只有 {@code selectRoleNamesByIds}（角色名映射），
 *       {@code UserService} 只有 {@code selectUserIdsByRoleIds}（反方向）。</li>
 *   <li>会话里的 {@code LoginUser.roles} 是<b>登录时快照</b> —— 撤权后仍持旧值，
 *       不满足 CX-10「撤权/过期后不继续读写」⇒ 必须实时读库，不能用会话缓存。</li>
 * </ol>
 *
 * @author AIP
 * @since 2026-09-23
 */
@Data
@TableName("sys_user_role")
public class DeSysUserRole implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** sys_user_role.user_id（复合主键首列；本类只读，IdType 仅占位） */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    /** sys_user_role.role_id */
    private Long roleId;
}
