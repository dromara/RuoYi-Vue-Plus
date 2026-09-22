package org.dromara.workflow.de.im.service;

import org.dromara.workflow.de.im.domain.bo.DeImExpertConsultBo;
import org.dromara.workflow.de.im.domain.bo.DeImRecoverBo;

/**
 * IM 域服务接口 —— P1 档 A：IM-04 / IM-06 共 2 项。
 *
 * @author AIP
 */
public interface IDeImService {

    /** IM-04 真人专家咨询与正式审批接入原任务 */
    Long expertConsult(DeImExpertConsultBo bo);

    /** IM-06 连接失效 / 成员退群 / 暂停解除 / 重新授权 / 消息投递恢复 */
    Long recover(DeImRecoverBo bo);
}