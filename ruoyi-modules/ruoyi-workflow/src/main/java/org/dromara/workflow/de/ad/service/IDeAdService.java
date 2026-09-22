package org.dromara.workflow.de.ad.service;

import org.dromara.workflow.de.ad.domain.bo.DeAdHandoverBo;

import java.util.List;
import java.util.Map;

/**
 * AD 域服务接口 —— P1 档 A：AD-03 / AD-04 / AD-07 共 3 项。
 *
 * @author AIP
 */
public interface IDeAdService {

    /** AD-03 分开组织任职 / 身份认证 / 角色策略 / 资源归属 / 资产授权 */
    Map<String, Object> dimensionSeparate(Long userId);

    /** AD-04 管理员维护资源目录 + 员工自助选用 */
    List<Map<String, Object>> resourceCatalog(Long userId);

    /** AD-07 离职 / 撤权 / 冻结资产 / 交接 一致性预览 */
    Map<String, Object> assetHandover(DeAdHandoverBo bo);
}