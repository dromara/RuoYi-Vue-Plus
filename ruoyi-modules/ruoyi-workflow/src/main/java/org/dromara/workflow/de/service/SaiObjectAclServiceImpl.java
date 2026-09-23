package org.dromara.workflow.de.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.workflow.de.domain.SaiObjectAcl;
import org.dromara.workflow.de.mapper.SaiObjectAclMapper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

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
    public boolean hasAccess(String objectType, Long objectId, Long userId, String grantType, String purpose) {
        Date now = new Date();
        List<SaiObjectAcl> grants = list(new LambdaQueryWrapper<SaiObjectAcl>()
            .eq(SaiObjectAcl::getObjectType, objectType)
            .eq(SaiObjectAcl::getObjectId, objectId)
            .eq(SaiObjectAcl::getGrantType, grantType)
            .eq(SaiObjectAcl::getSubjectType, "user")
            .eq(SaiObjectAcl::getSubjectId, userId));
        log.info("[ACL] hasAccess: type={} objId={} userId={} grant={} grants={}",
            objectType, objectId, userId, grantType, grants == null ? 0 : grants.size());
        for (SaiObjectAcl g : grants) {
            if (isEffective(g, purpose, now)) {
                return true;
            }
        }
        return false;
    }

    private boolean isEffective(SaiObjectAcl g, String purpose, Date now) {
        if (g.getExpiresAt() != null && g.getExpiresAt().before(now)) {
            return false;
        }
        if (purpose != null && g.getPurpose() != null && !purpose.equals(g.getPurpose())) {
            return false;
        }
        return true;
    }
}
