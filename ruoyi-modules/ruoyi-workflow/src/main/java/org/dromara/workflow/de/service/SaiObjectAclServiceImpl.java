package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.system.api.UserService;
import org.dromara.system.api.domain.UserDTO;
import org.dromara.workflow.de.domain.DeSysUserRole;
import org.dromara.workflow.de.domain.SaiObjectAcl;
import org.dromara.workflow.de.mapper.DeSysUserRoleMapper;
import org.dromara.workflow.de.mapper.SaiObjectAclMapper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 对象级 ACL 服务实现（D-011 · AU-03 方案 B + AU-04/05/06 四维）。
 *
 * <p>判定语义 = 方案 A 关系判定 OR 本表命中（ACL 是加宽不是替换）。
 *
 * @author AIP
 * @since 2026-09-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaiObjectAclServiceImpl
        extends ServiceImpl<SaiObjectAclMapper, SaiObjectAcl>
        implements ISaiObjectAclService {

    private final DeSysUserRoleMapper deSysUserRoleMapper;
    private final UserService userService;

    @Override
    public Long grant(SaiObjectAcl bo) {
        SaiObjectAcl existing = getOne(new LambdaQueryWrapper<SaiObjectAcl>()
            .eq(SaiObjectAcl::getObjectType, bo.getObjectType())
            .eq(SaiObjectAcl::getObjectId, bo.getObjectId())
            .eq(SaiObjectAcl::getSubjectType, bo.getSubjectType())
            .eq(SaiObjectAcl::getSubjectId, bo.getSubjectId())
            .eq(SaiObjectAcl::getGrantType, bo.getGrantType()));
        if (existing != null) {
            bo.setId(existing.getId());
            updateById(bo);
            return existing.getId();
        }
        save(bo);
        return bo.getId();
    }

    @Override
    public Boolean revoke(Long id) {
        return removeById(id);
    }

    @Override
    public List<SaiObjectAcl> listByObject(String objectType, Long objectId) {
        return list(new LambdaQueryWrapper<SaiObjectAcl>()
            .eq(SaiObjectAcl::getObjectType, objectType)
            .eq(SaiObjectAcl::getObjectId, objectId));
    }

    @Override
    public boolean hasAccess(String objectType, Long objectId, Long userId, String grantType,
                             String purpose, Integer currentPolicyVer) {
        Date now = new Date();
        // 主体展开（D-02 补实装）：实时读库解析 user 自身 + role + dept，避免登录快照导致的撤权漏判（CX-10）。
        Set<Long> roleIds = resolveRoleIds(userId);
        Long deptId = resolveDeptId(userId);
        // 按对象维度取全部候选授权，主体匹配与四维判定在内存完成（idx_obj 命中，代价可忽略）。
        List<SaiObjectAcl> grants = list(new LambdaQueryWrapper<SaiObjectAcl>()
            .eq(SaiObjectAcl::getObjectType, objectType)
            .eq(SaiObjectAcl::getObjectId, objectId)
            .eq(SaiObjectAcl::getGrantType, grantType));
        log.info("[ACL] hasAccess: type={} objId={} userId={} roles={} dept={} grant={} purpose={} policyVer={} candidates={}",
            objectType, objectId, userId, roleIds, deptId, grantType, purpose, currentPolicyVer,
            grants == null ? 0 : grants.size());
        if (grants == null) {
            return false;
        }
        for (SaiObjectAcl g : grants) {
            if (matches(g, userId, roleIds, deptId, purpose, currentPolicyVer, now)) {
                return true;
            }
        }
        return false;
    }

    /** 纯判定：主体命中（user/role/dept 任一）AND 四维有效（期限/目的/策略版本 fail-closed）。 */
    private boolean matches(SaiObjectAcl g, Long userId, Set<Long> roleIds, Long deptId,
                            String purpose, Integer currentPolicyVer, Date now) {
        // 主体维度
        Long subjectId = g.getSubjectId();
        if (subjectId == null) {
            return false;
        }
        boolean subjectHit = switch (g.getSubjectType() == null ? "" : g.getSubjectType()) {
            case "user" -> subjectId.equals(userId);
            case "role" -> roleIds.contains(subjectId);
            case "dept" -> deptId != null && deptId.equals(subjectId);
            default -> false;
        };
        if (!subjectHit) {
            return false;
        }
        // AU-05 期限维：非空且已过期 ⇒ 拒。
        if (g.getExpiresAt() != null && g.getExpiresAt().before(now)) {
            return false;
        }
        // AU-04 目的维 fail-closed：授权声明了 purpose，调用方必须声明匹配；未声明即不命中（授权 purpose 空 = 不限）。
        if (g.getPurpose() != null && !g.getPurpose().equals(purpose)) {
            return false;
        }
        // AU-06 策略版本维 fail-closed：授权钉了 policy_ver，调用方 currentPolicyVer 必须相等；未声明即不命中（授权版本空 = 最新）。
        if (g.getPolicyVer() != null && !g.getPolicyVer().equals(currentPolicyVer)) {
            return false;
        }
        return true;
    }

    /** 实时读 {@code sys_user_role}（非登录快照，CX-10 撤权即时生效）。 */
    private Set<Long> resolveRoleIds(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        List<DeSysUserRole> rows = deSysUserRoleMapper.selectList(
            new LambdaQueryWrapper<DeSysUserRole>().eq(DeSysUserRole::getUserId, userId));
        Set<Long> roleIds = new HashSet<>();
        if (rows != null) {
            for (DeSysUserRole r : rows) {
                if (r.getRoleId() != null) {
                    roleIds.add(r.getRoleId());
                }
            }
        }
        return roleIds;
    }

    /** 实时读 {@code sys_user.dept_id}（经 ruoyi-api UserService）。 */
    private Long resolveDeptId(Long userId) {
        if (userId == null) {
            return null;
        }
        UserDTO u = userService.selectById(userId);
        return u == null ? null : u.getDeptId();
    }
}
