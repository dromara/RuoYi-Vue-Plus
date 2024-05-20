package org.dromara.workflow.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.TaskVo;
import org.dromara.workflow.domain.vo.VariableVo;

import java.util.List;
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
     * @param taskBo    参数
     * @param pageQuery 分页
     * @return 结果
     */
    TableDataInfo<TaskVo> getPageByTaskWait(TaskBo taskBo, PageQuery pageQuery);

    /**
     * 查询当前租户所有待办任务
     *
     * @param taskBo    参数
     * @param pageQuery 分页
     * @return 结果
     */
    TableDataInfo<TaskVo> getPageByAllTaskWait(TaskBo taskBo, PageQuery pageQuery);


    /**
     * 查询当前用户的已办任务
     *
     * @param taskBo    参数
     * @param pageQuery 参数
     * @return 结果
     */
    TableDataInfo<TaskVo> getPageByTaskFinish(TaskBo taskBo, PageQuery pageQuery);

    /**
     * 查询当前用户的抄送
     *
     * @param taskBo    参数
     * @param pageQuery 参数
     * @return 结果
     */
    TableDataInfo<TaskVo> getPageByTaskCopy(TaskBo taskBo, PageQuery pageQuery);

    /**
     * 查询当前租户所有已办任务
     *
     * @param taskBo    参数
     * @param pageQuery 参数
     * @return 结果
     */
    TableDataInfo<TaskVo> getPageByAllTaskFinish(TaskBo taskBo, PageQuery pageQuery);

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

    /**
     * 会签任务加签
     *
     * @param addMultiBo 参数
     * @return 结果
     */
    boolean addMultiInstanceExecution(AddMultiBo addMultiBo);

    /**
     * 会签任务减签
     *
     * @param deleteMultiBo 参数
     * @return 结果
     */
    boolean deleteMultiInstanceExecution(DeleteMultiBo deleteMultiBo);

    /**
     * 驳回审批
     *
     * @param backProcessBo 参数
     * @return 流程实例id
     */
    String backProcess(BackProcessBo backProcessBo);

    /**
     * 修改任务办理人
     *
     * @param taskIds 任务id
     * @param userId  办理人id
     * @return 结果
     */
    boolean updateAssignee(String[] taskIds, String userId);

    /**
     * 查询流程变量
     *
     * @param taskId 任务id
     * @return 结果
     */
    List<VariableVo> getInstanceVariable(String taskId);

    /**
     * 查询工作流任务用户选择加签人员
     *
     * @param taskId 任务id
     * @return 结果
     */
    String getTaskUserIdsByAddMultiInstance(String taskId);

    /**
     * 查询工作流选择减签人员
     *
     * @param taskId 任务id
     * @return 结果
     */
    List<TaskVo> getListByDeleteMultiInstance(String taskId);
}
