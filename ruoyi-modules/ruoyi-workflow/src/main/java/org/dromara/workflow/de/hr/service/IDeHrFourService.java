package org.dromara.workflow.de.hr.service;

import org.dromara.workflow.de.hr.domain.bo.DeHrThreeTierBo;
import org.dromara.workflow.de.hr.domain.vo.DeHrThreeTierVo;

/**
 * HR-04 三分位落点服务接口（2026-09-23）。
 *
 * @author AIP
 * @since 2026-09-23
 */
public interface IDeHrFourService {

    /** GET 端点：读三分位（解析 flow_instance.variable.de.ctx.hr.three_tier） */
    DeHrThreeTierVo getThreeTier(Long instanceId, Long taskId);

    /** PUT 端点：写三分位（落 flow_user type=1/2/3 + 更新 flow_instance.variable） */
    DeHrThreeTierVo upsertThreeTier(DeHrThreeTierBo bo);
}