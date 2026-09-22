package org.dromara.workflow.de.hc.service;

import org.dromara.workflow.de.hc.domain.bo.DeHcCollabRequestBo;
import org.dromara.workflow.de.hc.domain.bo.DeHcParticipantBo;

import java.util.Map;

/**
 * HC 域服务接口 —— P1 档 A：HC-04 / HC-05 / HC-06 / HC-08 共 4 项。
 *
 * @author AIP
 */
public interface IDeHcService {

    /** HC-04 协作请求 */
    Long collabRequest(DeHcCollabRequestBo bo);

    /** HC-05 参与者加入 / 退出 / 替补 / 转派 */
    Long participant(DeHcParticipantBo bo);

    /** HC-06 贡献流程分型 */
    Map<String, Object> contributionStatus(Long collabId);

    /** HC-08 节点展示参与者 / 分工 / Agent 授权摘要 */
    Map<String, Object> nodeParticipants(Long taskId);
}