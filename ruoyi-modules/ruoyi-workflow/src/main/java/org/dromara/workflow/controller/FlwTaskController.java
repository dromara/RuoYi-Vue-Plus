package org.dromara.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.redis.annotation.RepeatSubmit;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.api.domain.UserDTO;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.dromara.workflow.api.domain.StartProcessReturnDTO;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.dromara.workflow.domain.vo.FlowTaskVo;
import org.dromara.workflow.service.IFlwTaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 任务管理 控制层
 *
 * @author may
 */
@ConditionalOnEnable
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/task")
public class FlwTaskController extends BaseController {

    private final IFlwTaskService flwTaskService;

    /**
     * 启动流程任务。
     *
     * @param startProcessBo 启动流程参数
     * @return 启动结果及后续流程信息
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/startWorkFlow")
    public R<StartProcessReturnDTO> startWorkFlow(@Validated(AddGroup.class) @RequestBody StartProcessBo startProcessBo) {
        StartProcessReturnDTO startProcessReturn = flwTaskService.startWorkFlow(startProcessBo);
        return R.ok("提交成功", startProcessReturn);
    }

    /**
     * 办理当前任务节点。
     *
     * @param completeTaskBo 办理任务参数
     * @return 操作结果
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/completeTask")
    public R<Void> completeTask(@Validated(AddGroup.class) @RequestBody CompleteTaskBo completeTaskBo) {
        return toAjax(flwTaskService.completeTask(completeTaskBo));
    }

    /**
     * 查询当前用户的待办任务。
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 待办任务分页数据
     */
    @GetMapping("/pageByTaskWait")
    public R<PageResult<FlowTaskVo>> pageByTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return R.ok(flwTaskService.pageByTaskWait(flowTaskBo, pageQuery));
    }

    /**
     * 查询当前用户的已办任务。
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 已办任务分页数据
     */
    @GetMapping("/pageByTaskFinish")
    public R<PageResult<FlowHisTaskVo>> pageByTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return R.ok(flwTaskService.pageByTaskFinish(flowTaskBo, pageQuery));
    }

    /**
     * /**
     * 查询全部待办任务。
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 待办任务分页数据
     */
    @SaCheckPermission("workflow:task:list")
    @GetMapping("/pageByAllTaskWait")
    public R<PageResult<FlowTaskVo>> pageByAllTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return R.ok(flwTaskService.pageByAllTaskWait(flowTaskBo, pageQuery));
    }

    /**
     * 查询全部已办任务。
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 已办任务分页数据
     */
    @SaCheckPermission("workflow:task:list")
    @GetMapping("/pageByAllTaskFinish")
    public R<PageResult<FlowHisTaskVo>> pageByAllTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return R.ok(flwTaskService.pageByAllTaskFinish(flowTaskBo, pageQuery));
    }

    /**
     * 查询当前用户收到的抄送任务。
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 抄送任务分页数据
     */
    @GetMapping("/pageByTaskCopy")
    public R<PageResult<FlowTaskVo>> pageByTaskCopy(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return R.ok(flwTaskService.pageByTaskCopy(flowTaskBo, pageQuery));
    }

    /**
     * /**
     * 根据任务 id 查询任务详情。
     *
     * @param taskId 任务id
     * @return 任务详情
     */
    @GetMapping("/getTask/{taskId}")
    public R<FlowTaskVo> getTask(@PathVariable Long taskId) {
        return R.ok(flwTaskService.selectById(taskId));
    }

    /**
     * /**
     * 获取流程下一节点信息。
     *
     * @param bo 参数
     * @return 下一节点列表
     */
    @PostMapping("/getNextNodeList")
    public R<List<FlowNode>> getNextNodeList(@RequestBody FlowNextNodeBo bo) {
        return R.ok(flwTaskService.getNextNodeList(bo));
    }

    /**
     * /**
     * 终止流程任务。
     *
     * @param bo 参数
     * @return 处理结果
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/terminationTask")
    public R<Boolean> terminationTask(@RequestBody FlowTerminationBo bo) {
        return R.ok(flwTaskService.terminationTask(bo));
    }

    /**
     * 执行任务操作，例如委派、转办、加签或减签。
     *
     * @param bo            参数
     * @param taskOperation 操作类型，委派 delegateTask、转办 transferTask、加签 addSignature、减签 reductionSignature
     * @return 操作结果
     */
    @Log(title = "任务管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/taskOperation/{taskOperation}")
    public R<Void> taskOperation(@Validated @RequestBody TaskOperationBo bo, @PathVariable String taskOperation) {
        return toAjax(flwTaskService.taskOperation(bo, taskOperation));
    }

    /**
     * 批量修改任务办理人。
     *
     * @param taskIdList 任务id
     * @param userId     办理人id
     * @return 操作结果
     */
    @Log(title = "任务管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @SaCheckPermission("workflow:task:edit")
    @PutMapping("/updateAssignee/{userId}")
    public R<Void> updateAssignee(@RequestBody List<Long> taskIdList, @PathVariable String userId) {
        return toAjax(flwTaskService.updateAssignee(taskIdList, userId));
    }

    /**
     * 驳回审批到前置节点。
     *
     * @param bo 参数
     * @return 操作结果
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/backProcess")
    public R<Void> backProcess(@Validated({AddGroup.class}) @RequestBody BackProcessBo bo) {
        return toAjax(flwTaskService.backProcess(bo));
    }

    /**
     * 获取当前任务可驳回的前置节点。
     *
     * @param taskId      任务id
     * @param nowNodeCode 当前节点
     * @return 可驳回节点列表
     */
    @GetMapping("/getBackTaskNode/{taskId}/{nowNodeCode}")
    public R<List<Node>> getBackTaskNode(@PathVariable Long taskId, @PathVariable String nowNodeCode) {
        return R.ok(flwTaskService.getBackTaskNode(taskId, nowNodeCode));
    }

    /**
     * 获取当前任务的所有办理人。
     *
     * @param taskId 任务id
     * @return 办理人列表
     */
    @GetMapping("/currentTaskAllUser/{taskId}")
    public R<List<UserDTO>> currentTaskAllUser(@PathVariable Long taskId) {
        return R.ok(flwTaskService.currentTaskAllUser(List.of(taskId)));
    }

    /**
     * 催办任务。
     *
     * @param bo 参数
     * @return 结果
     */
    @PostMapping("/urgeTask")
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("workflow:task:edit")
    public R<Void> urgeTask(@RequestBody FlowUrgeTaskBo bo) {
        return toAjax(flwTaskService.urgeTask(bo));
    }
}
