package org.dromara.workflow.de.hr.service;

import org.dromara.workflow.de.hr.domain.bo.DeHrAssignBo;
import org.dromara.workflow.de.hr.domain.bo.DeHrHandoverBo;
import org.dromara.workflow.de.hr.domain.bo.DeHrTaskActionBo;

/**
 * HR 域服务接口 —— P1 档 A：HR-03 / HR-05 / HR-06 共 3 项。
 *
 * @author AIP
 */
public interface IDeHrService {

    /** HR-03 责任分派（候选组 / 岗位 / 负载 / 代理） */
    Long assign(DeHrAssignBo bo);

    /** HR-05 催办 / 延期 / 请人 / 退回补充 / 升级 */
    Long taskAction(DeHrTaskActionBo bo);

    /** HR-06 人员离岗 / 休假 / 停用 后任务与数字资产交接 */
    int handover(DeHrHandoverBo bo);
}