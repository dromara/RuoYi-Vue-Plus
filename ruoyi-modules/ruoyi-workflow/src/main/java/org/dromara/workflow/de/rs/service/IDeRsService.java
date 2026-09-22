package org.dromara.workflow.de.rs.service;

import org.dromara.workflow.de.rs.domain.bo.DeRsConfirmBo;
import org.dromara.workflow.de.rs.domain.bo.DeRsCrossCheckBo;
import org.dromara.workflow.de.rs.domain.bo.DeRsDecideBo;

/**
 * RS 域服务接口 —— P1 档 A：RS-05 / RS-06 / RS-07 共 3 项。
 *
 * @author AIP
 */
public interface IDeRsService {

    /** RS-05 审核者逐项接受 / 退回 / 要求补充 */
    Long decide(DeRsDecideBo bo);

    /** RS-06 外部系统写入结果与提交产物交叉核验 */
    Boolean crossCheck(DeRsCrossCheckBo bo);

    /** RS-07 接收确认 / 权限不足 / 业务取消 / 回退 / 补偿 */
    Long confirm(DeRsConfirmBo bo);
}