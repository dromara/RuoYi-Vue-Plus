package org.dromara.workflow.service;

import org.dromara.common.core.domain.PageResult;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.system.api.domain.UserDTO;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.api.domain.StartProcessReturnDTO;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.dromara.workflow.domain.vo.FlowTaskVo;

import java.util.Collection;
import java.util.List;

/**
 * 任务 服务层
 *
 * @author may
 */
public interface IFlwTaskService {

    /**
     * 启动任务
     *
     * @param startProcessBo 启动流程参数
     * @return 结果
     */
    StartProcessReturnDTO startWorkFlow(StartProcessBo startProcessBo);

    /**
     * 办理任务
     *
     * @param completeTaskBo 办理任务参数
     * @return 结果
     */
    boolean completeTask(CompleteTaskBo completeTaskBo);

    /**
     * 添加抄送人
     *
     * @param task         任务信息
     * @param flowCopyList 抄送人
     */
    void setCopy(Task task, List<FlowCopyBo> flowCopyList);

    /**
     * 查询当前用户的待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 当前用户待办任务分页结果
     */
    PageResult<FlowTaskVo> pageByTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery);

    /**
     * 查询当前租户所有待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 当前用户已办任务分页结果
     */
    PageResult<FlowHisTaskVo> pageByTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery);

    /**
     * 查询待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 当前租户待办任务分页结果
     */
    PageResult<FlowTaskVo> pageByAllTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery);

    /**
     * 查询已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 当前租户已办任务分页结果
     */
    PageResult<FlowHisTaskVo> pageByAllTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery);

    /**
     * 查询当前用户的抄送
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 当前用户抄送分页结果
     */
    PageResult<FlowTaskVo> pageByTaskCopy(FlowTaskBo flowTaskBo, PageQuery pageQuery);

    /**
     * 修改任务办理人
     *
     * @param taskIdList 任务id
     * @param userId     用户id
     * @return 结果
     */
    boolean updateAssignee(Collection<Long> taskIdList, String userId);

    /**
     * 驳回审批
     *
     * @param bo 参数
     * @return 结果
     */
    boolean backProcess(BackProcessBo bo);

    /**
     * 获取可驳回的前置节点
     *
     * @param taskId      任务id
     * @param nowNodeCode 当前节点
     * @return 结果
     */
    List<Node> getBackTaskNode(Long taskId, String nowNodeCode);

    /**
     * 终止任务
     *
     * @param bo 参数
     * @return 结果
     */
    boolean terminationTask(FlowTerminationBo bo);

    /**
     * 按照任务id查询任务
     *
     * @param taskIdList 任务id
     * @return 任务列表
     */
    List<FlowTask> selectByIdList(Collection<Long> taskIdList);

    /**
     * 按照任务id查询任务
     *
     * @param taskId 任务id
     * @return 任务详情
     */
    FlowTaskVo selectById(Long taskId);

    /**
     * 获取下一节点信息
     *
     * @param bo 参数
     * @return 下一节点列表
     */
    List<FlowNode> getNextNodeList(FlowNextNodeBo bo);

    /**
     * 按照任务id查询任务
     *
     * @param taskId 任务id
     * @return 历史任务详情
     */
    FlowHisTask selectHisTaskById(Long taskId);

    /**
     * 按照实例id查询任务
     *
     * @param instanceId 流程实例id
     * @return 任务列表
     */
    List<FlowTask> selectByInstId(Long instanceId);

    /**
     * 按照实例id查询任务
     *
     * @param instanceIds 列表
     * @return 任务列表
     */
    List<FlowTask> selectByInstIds(Collection<Long> instanceIds);

    /**
     * 判断流程是否已结束（即该流程实例下是否还有未完成的任务）
     *
     * @param instanceId 流程实例ID
     * @return true 表示任务已全部结束；false 表示仍有任务存在
     */
    boolean isTaskEnd(Long instanceId);

    /**
     * 任务操作
     *
     * @param bo            参数
     * @param taskOperation 操作类型，委派 delegateTask、转办 transferTask、加签 addSignature、减签 reductionSignature
     * @return 结果
     */
    boolean taskOperation(TaskOperationBo bo, String taskOperation);

    /**
     * 获取当前任务的所有办理人
     *
     * @param taskIds 任务id
     * @return 办理人列表
     */
    List<UserDTO> currentTaskAllUser(List<Long> taskIds);

    /**
     * 按照节点编码查询节点
     *
     * @param nodeCode     节点编码
     * @param definitionId 流程定义id
     * @return 节点
     */
    FlowNode getByNodeCode(String nodeCode, Long definitionId);

    /**
     * 催办任务
     *
     * @param bo 参数
     * @return 是否催办成功
     */
    boolean urgeTask(FlowUrgeTaskBo bo);
}
