package org.dromara.workflow.de.service;

import org.dromara.workflow.de.domain.SaiObjectAcl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SaiObjectAclServiceImpl} D-02「四维合一 + 主体展开」纯判定单测（不启 Spring、零 DB）。
 *
 * <p>目标私有方法 {@code matches(SaiObjectAcl, userId, roleIds, deptId, purpose, currentPolicyVer, now)}
 * 是本服务的安全决策内核：它把 subject 维度（user/role/dept）与四维有效性
 * （AU-05 期限 / AU-04 目的 / AU-06 策略版本，均 fail-closed）收敛为一次布尔判定。
 * 通过反射直调该纯方法，无需 mapper / Spring 容器即可钉死越权边界。
 *
 * <p>钉死的不变量：
 * <ul>
 *   <li>subject：user 精确匹配、role ∈ 实时角色集、dept 实时部门匹配，未知类型拒；</li>
 *   <li>AU-05：expires_at 过期 ⇒ 拒，永久/未来 ⇒ 放行；</li>
 *   <li>AU-04 fail-closed：授权声明 purpose 而调用方未声明或不等 ⇒ 拒；授权 purpose 空 ⇒ 不限；</li>
 *   <li>AU-06 fail-closed：授权钉 policy_ver 而调用方版本不等/未声明 ⇒ 拒；授权版本空 ⇒ 最新放行。</li>
 * </ul>
 *
 * @author AIP
 */
@Tag("dev")
@DisplayName("D-02 ACL 四维合一：subject 展开 + purpose/policyVer/expires fail-closed（反射直调纯判定）")
class SaiObjectAclServiceImplTest {

    /** 构造不依赖被注入 mapper 的纯判定方法（matches 只读入参对象，不用 deSysUserRoleMapper/userService）。 */
    private boolean matches(SaiObjectAcl g, Long userId, Set<Long> roleIds, Long deptId,
                            String purpose, Integer currentPolicyVer, Date now) throws Exception {
        SaiObjectAclServiceImpl svc = new SaiObjectAclServiceImpl(null, null);
        Method m = SaiObjectAclServiceImpl.class.getDeclaredMethod("matches",
            SaiObjectAcl.class, Long.class, Set.class, Long.class, String.class, Integer.class, Date.class);
        m.setAccessible(true);
        return (boolean) m.invoke(svc, g, userId, roleIds, deptId, purpose, currentPolicyVer, now);
    }

    private SaiObjectAcl userGrant() {
        SaiObjectAcl g = new SaiObjectAcl();
        g.setSubjectType("user");
        g.setSubjectId(100L);
        g.setGrantType("read");
        return g;
    }

    @Test
    @DisplayName("user 主体精确命中；异 user 拒")
    void userSubject() throws Exception {
        Date now = new Date();
        assertTrue(matches(userGrant(), 100L, Set.of(), null, null, null, now));
        assertFalse(matches(userGrant(), 200L, Set.of(), null, null, null, now));
    }

    @Test
    @DisplayName("role 主体展开：subject=角色且在实时角色集内命中；不在集内拒")
    void roleSubject() throws Exception {
        Date now = new Date();
        SaiObjectAcl g = new SaiObjectAcl();
        g.setSubjectType("role");
        g.setSubjectId(7L);
        assertTrue(matches(g, 100L, Set.of(7L, 8L), null, null, null, now));
        assertFalse(matches(g, 100L, Set.of(9L), null, null, null, now));
    }

    @Test
    @DisplayName("dept 主体展开：subject=用户实时部门命中；部门未知或不匹配拒")
    void deptSubject() throws Exception {
        Date now = new Date();
        SaiObjectAcl g = new SaiObjectAcl();
        g.setSubjectType("dept");
        g.setSubjectId(50L);
        assertTrue(matches(g, 100L, Set.of(), 50L, null, null, now));
        assertFalse(matches(g, 100L, Set.of(), 51L, null, null, now));
        assertFalse(matches(g, 100L, Set.of(), null, null, null, now));
    }

    @Test
    @DisplayName("未知 subjectType 一律拒（fail-safe）")
    void unknownSubject() throws Exception {
        SaiObjectAcl g = new SaiObjectAcl();
        g.setSubjectType("group");
        g.setSubjectId(1L);
        assertFalse(matches(g, 100L, Set.of(1L), 1L, null, null, new Date()));
    }

    @Test
    @DisplayName("AU-05 期限维：过期拒、永久放行")
    void expires() throws Exception {
        Date now = new Date();
        SaiObjectAcl g = userGrant();
        g.setExpiresAt(new Date(now.getTime() - 1000));
        assertFalse(matches(g, 100L, Set.of(), null, null, null, now));
        g.setExpiresAt(new Date(now.getTime() + 100000));
        assertTrue(matches(g, 100L, Set.of(), null, null, null, now));
    }

    @Test
    @DisplayName("AU-04 目的维 fail-closed：授权声明 purpose 时调用方须匹配，未声明即拒；授权空=不限")
    void purpose() throws Exception {
        Date now = new Date();
        SaiObjectAcl g = userGrant();
        g.setPurpose("audit");
        assertFalse(matches(g, 100L, Set.of(), null, null, null, now));   // 调用方未声明
        assertFalse(matches(g, 100L, Set.of(), null, "report", null, now)); // 不等
        assertTrue(matches(g, 100L, Set.of(), null, "audit", null, now));  // 匹配
        g.setPurpose(null);
        assertTrue(matches(g, 100L, Set.of(), null, null, null, now));     // 授权不限目的
    }

    @Test
    @DisplayName("AU-06 策略版本维 fail-closed：授权钉版本时调用方须相等，未声明即拒；授权空=最新放行")
    void policyVer() throws Exception {
        Date now = new Date();
        SaiObjectAcl g = userGrant();
        g.setPolicyVer(3);
        assertFalse(matches(g, 100L, Set.of(), null, null, null, now)); // 调用方无版本权威
        assertFalse(matches(g, 100L, Set.of(), null, null, 2, now));   // 版本不等
        assertTrue(matches(g, 100L, Set.of(), null, null, 3, now));    // 版本相等
        g.setPolicyVer(null);
        assertTrue(matches(g, 100L, Set.of(), null, null, null, now)); // 最新
    }

    @Test
    @DisplayName("subjectId 为空拒（脏数据防御）")
    void nullSubjectId() throws Exception {
        SaiObjectAcl g = new SaiObjectAcl();
        g.setSubjectType("user");
        g.setSubjectId(null);
        assertFalse(matches(g, 100L, Set.of(), null, null, null, new Date()));
    }
}
