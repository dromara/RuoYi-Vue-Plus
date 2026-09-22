package org.dromara.workflow.de.ag.service;

import org.dromara.workflow.de.ag.domain.vo.DeAgOverviewVo;

import java.util.List;

/**
 * AG 域服务接口 —— P1 档 A：AG-04 / AG-05 共 2 项。
 *
 * @author AIP
 */
public interface IDeAgService {

    /** AG-04 数字员工详情页（概览 / 能力 / 任务 / 记忆 / 设置） */
    DeAgOverviewVo overview(Long agentId);

    /** AG-05 我创建的 / 共享给我的 / 团队使用 关系 */
    List<String> relations(Long agentId, Long userId);
}