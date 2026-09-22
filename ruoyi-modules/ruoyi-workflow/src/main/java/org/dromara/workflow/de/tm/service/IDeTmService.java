package org.dromara.workflow.de.tm.service;

import java.util.Map;

/**
 * TM 域服务接口 —— P1 档 A：TM-03 / TM-06 共 2 项。
 *
 * <p>TM 域面临 C-06 团队表未造；本类仅暴露端点形状 + 占位。
 *
 * @author AIP
 */
public interface IDeTmService {

    /** TM-03 团队共享技能 / 知识 与 成员私有能力 区分 */
    Map<String, Object> skillScope(Long teamId, Long userId);

    /** TM-06 团队执行输出对接节点完成检查与交接包 */
    Map<String, Object> handover(Long teamId, Long taskId);
}