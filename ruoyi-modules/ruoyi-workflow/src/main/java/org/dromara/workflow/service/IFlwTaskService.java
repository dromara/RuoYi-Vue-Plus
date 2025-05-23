package org.dromara.workflow.service;

import org.dromara.common.core.domain.dto.StartProcessReturnDTO;
import org.dromara.common.core.domain.dto.UserDTO;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.dromara.workflow.domain.vo.FlowTaskVo;

import java.util.List;
import java.util.Map;

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
     * 查询当前用户的待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 结果
     */
    TableDataInfo<FlowTaskVo> pageByTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery);

    /**
     * 查询当前租户所有待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 结果
     */
    TableDataInfo<FlowHisTaskVo> pageByTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery);

    /**
     * 查询待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 结果
     */
    TableDataInfo<FlowTaskVo> pageByAllTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery);

    /**
     * 查询已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 结果
     */
    TableDataInfo<FlowHisTaskVo> pageByAllTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery);

    /**
     * 查询当前用户的抄送
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 结果
     */
    TableDataInfo<FlowTaskVo> pageByTaskCopy(FlowTaskBo flowTaskBo, PageQuery pageQuery);

    /**
     * 修改任务办理人
     *
     * @param taskIdList 任务id
     * @param userId     用户id
     * @return 结果
     */
    boolean updateAssignee(List<Long> taskIdList, String userId);

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
     * @param definitionId 流程定义id
     * @param nowNodeCode  当前节点
     * @return 结果
     */
    List<Node> getBackTaskNode(Long definitionId, String nowNodeCode);

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
     * @return 结果
     */
    List<FlowTask> selectByIdList(List<Long> taskIdList);

    /**
     * 按照任务id查询任务
     *
     * @param taskId 任务id
     * @return 结果
     */
    FlowTaskVo selectById(Long taskId);

    /**
     * 获取下一节点信息
     *
     * @param bo 参数
     * @return 结果
     */
    List<FlowNode> getNextNodeList(FlowNextNodeBo bo);

    /**
     * 按照任务id查询任务
     *
     * @param taskIdList 任务id
     * @return 结果
     */
    List<FlowHisTask> selectHisTaskByIdList(List<Long> taskIdList);

    /**
     * 按照任务id查询任务
     *
     * @param taskId 任务id
     * @return 结果
     */
    FlowHisTask selectHisTaskById(Long taskId);

    /**
     * 按照实例id查询任务
     *
     * @param instanceIdList 流程实例id
     * @return 结果
     */
    List<FlowTask> selectByInstIdList(List<Long> instanceIdList);

    /**
     * 按照实例id查询任务
     *
     * @param instanceId 流程实例id
     * @return 结果
     */
    List<FlowTask> selectByInstId(Long instanceId);

    /**
     * 任务操作
     *
     * @param bo            参数
     * @param taskOperation 操作类型，委派 delegateTask、转办 transferTask、加签 addSignature、减签 reductionSignature
     * @return 结果
     */
    boolean taskOperation(TaskOperationBo bo, String taskOperation);

    /**
     * 获取任务所有办理人
     *
     * @param taskIdList 任务id
     * @return 结果
     */
    Map<Long, List<UserDTO>> currentTaskAllUser(List<Long> taskIdList);

    /**
     * 获取当前任务的所有办理人
     *
     * @param taskId 任务id
     * @return 结果
     */
    List<UserDTO> currentTaskAllUser(Long taskId);

    /**
     * 按照节点编码查询节点
     *
     * @param nodeCode     节点编码
     * @param definitionId 流程定义id
     * @return 节点
     */
    FlowNode getByNodeCode(String nodeCode, Long definitionId);
}
