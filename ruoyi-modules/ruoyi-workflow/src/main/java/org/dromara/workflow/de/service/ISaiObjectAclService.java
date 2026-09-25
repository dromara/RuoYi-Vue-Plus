package org.dromara.workflow.de.service;

import org.dromara.workflow.de.domain.SaiObjectAcl;

import java.util.List;

/**
 * 对象级 ACL 服务接口（D-011 · AU-03 方案 B）。
 *
 * @author AIP
 * @since 2026-09-23
 */
public interface ISaiObjectAclService {

    /** 授权（INSERT OR UPDATE，幂等） */
    Long grant(SaiObjectAcl bo);

    /** 撤销 */
    Boolean revoke(Long id);

    /** 查对象的 ACL 列表 */
    List<SaiObjectAcl> listByObject(String objectType, Long objectId);

    /**
     * 判定：主体对对象是否有指定类型的授权（AU-03 方案 B 四维合一）。
     *
     * <p><b>主体展开</b>（D-02 补实装）：实时读库解析 {@code userId} 的
     * user 自身 + role（{@code sys_user_role} 实时，非登录快照，CX-10）+ dept（{@code sys_user.dept_id} 实时），
     * 任一命中的授权均计入。
     *
     * <p><b>四维 fail-closed</b>：
     * <ul>
     *   <li>AU-05 期限：{@code expires_at} 非空且已过期 ⇒ 拒；</li>
     *   <li>AU-04 目的：授权 {@code purpose} 非空时，调用方必须声明匹配 purpose，
     *       未声明（{@code null}）⇒ 视为不匹配而拒（授权 {@code purpose} 为空 = 不限，放行）；</li>
     *   <li>AU-06 策略版本：授权 {@code policy_ver} 非空时，调用方 {@code currentPolicyVer}
     *       必须相等，否则（含未声明）⇒ 拒（授权 {@code policy_ver} 为空 = 最新，放行）。</li>
     * </ul>
     *
     * @param purpose          调用方声明的用途（无子用途语境传 {@code null}，仅可命中「不限目的」授权）
     * @param currentPolicyVer 调用方声明的当前策略版本（无版本权威时传 {@code null}，仅可命中「最新」授权）
     */
    boolean hasAccess(String objectType, Long objectId, Long userId, String grantType,
                      String purpose, Integer currentPolicyVer);
}
