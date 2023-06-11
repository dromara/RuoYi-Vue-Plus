package org.dromara.workflow.service;

import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.TaskVo;

import java.util.Map;

/**
 * 任务 服务层
 *
 * @author may
 */
public interface IActTaskService {
    /**
     * 启动任务
     *
     * @param startProcessBo 启动流程参数
     * @return 结果
     */
    Map<String, Object> startWorkFlow(StartProcessBo startProcessBo);


    /**
     * 办理任务
     *
     * @param completeTaskBo 办理任务参数
     * @return 结果
     */
    boolean completeTask(CompleteTaskBo completeTaskBo);

    /**
     * 查询当前用户的待办任务
     *
     * @param taskBo 参数
     * @return 结果
     */
    TableDataInfo<TaskVo> getTaskWaitByPage(TaskBo taskBo);

    /**
     * 查询当前用户的已办任务
     *
     * @param taskBo 参数
     * @return 结果
     */
    TableDataInfo<TaskVo> getTaskFinishByPage(TaskBo taskBo);

    /**
     * 委派任务
     *
     * @param delegateBo 参数
     * @return 结果
     */
    boolean delegateTask(DelegateBo delegateBo);

    /**
     * 终止任务
     *
     * @param terminationBo 参数
     * @return 结果
     */
    boolean terminationTask(TerminationBo terminationBo);

    /**
     * 转办任务
     *
     * @param transmitBo 参数
     * @return 结果
     */
    boolean transferTask(TransmitBo transmitBo);
}
