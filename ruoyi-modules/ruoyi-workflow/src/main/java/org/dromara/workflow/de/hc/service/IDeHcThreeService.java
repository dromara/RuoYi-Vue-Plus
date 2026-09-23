package org.dromara.workflow.de.hc.service;

import org.dromara.workflow.de.hc.domain.bo.DeHcThreeCategoryBo;
import org.dromara.workflow.de.hc.domain.vo.DeHcThreeCategoryVo;

/**
 * HC-03 三分类位落点服务接口（2026-09-23）。
 *
 * @author AIP
 * @since 2026-09-23
 */
public interface IDeHcThreeService {

    /** GET 端点：读三分类位（解析 flow_node.permission_flag） */
    DeHcThreeCategoryVo getThreeCategory(Long nodeId);

    /** PUT 端点：写三分类位（拼装 permission_flag） */
    DeHcThreeCategoryVo upsertThreeCategory(DeHcThreeCategoryBo bo);
}