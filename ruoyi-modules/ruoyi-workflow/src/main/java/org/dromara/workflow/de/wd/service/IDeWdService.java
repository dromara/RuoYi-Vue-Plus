package org.dromara.workflow.de.wd.service;

import org.dromara.workflow.de.wd.domain.bo.DeWdTaskFilterBo;
import org.dromara.workflow.de.wd.domain.vo.DeWdTaskListVo;

import java.util.List;

/**
 * 工作台（WD）服务接口 —— P1 档 A：WD-03 / WD-05 / WD-06 共 3 项。
 *
 * <p>落点真源：flow_task / flow_instance / flow_instance.variable（de.ctx.task.step）
 *
 * @author AIP
 */
public interface IDeWdService {

    /**
     * WD-03：列出「AI 执行中」任务。
     * 判定 = flow_instance.variable JSON 中 de.ctx.task.step ∈ {running, pending}
     */
    List<DeWdTaskListVo> listInProgress(Long userId);

    /**
     * WD-05：按流程 / 项目 / 期限 / 人员责任 / Agent 多维筛选工作台任务。
     */
    List<DeWdTaskListVo> filterTasks(DeWdTaskFilterBo bo);

    /**
     * WD-06：异常 / 求助 / 审核 / 资源失效 集中到「需要我处理」。
     * 判定 = flow_task.flow_status + de.ctx.task.step 双轨聚合。
     */
    List<DeWdTaskListVo> listActionRequired(Long userId);
}