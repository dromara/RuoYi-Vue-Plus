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
     * 判定：主体对对象是否有指定类型的授权。
     * 检查顺序：直接 user 授权 → subject 的 role 授权 → 过期/目的不匹配的跳过。
     */
    boolean hasAccess(String objectType, Long objectId, Long userId, String grantType, String purpose);
}
