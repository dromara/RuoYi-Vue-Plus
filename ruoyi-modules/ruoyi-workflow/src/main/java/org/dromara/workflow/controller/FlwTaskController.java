package org.dromara.workflow.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.warm.flow.core.dto.FlowParams;
import com.warm.flow.core.dto.ModifyHandler;
import com.warm.flow.core.entity.Instance;
import com.warm.flow.core.entity.Task;
import com.warm.flow.core.enums.CooperateType;
import com.warm.flow.core.enums.FlowStatus;
import com.warm.flow.core.service.InsService;
import com.warm.flow.core.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.dromara.workflow.domain.vo.FlowTaskVo;
import org.dromara.workflow.service.IFlwTaskService;
import org.dromara.workflow.utils.WorkflowUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import java.util.Collections;
import java.util.Map;

/**
 * 任务管理 控制层
 *
 * @author may
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/task")
public class FlwTaskController extends BaseController {

    private final IFlwTaskService flwTaskService;
    private final TaskService taskService;
    private final InsService insService;


    /**
     * 启动任务
     *
     * @param startProcessBo 启动流程参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/startWorkFlow")
    public R<Map<String, Object>> startWorkFlow(@Validated(AddGroup.class) @RequestBody StartProcessBo startProcessBo) {
        Map<String, Object> map = flwTaskService.startWorkFlow(startProcessBo);
        return R.ok("提交成功", map);
    }

    /**
     * 办理任务
     *
     * @param completeTaskBo 办理任务参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/completeTask")
    public R<Void> completeTask(@Validated(AddGroup.class) @RequestBody CompleteTaskBo completeTaskBo) {
        return toAjax(flwTaskService.completeTask(completeTaskBo));
    }

    /**
     * 查询当前用户的待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @GetMapping("/getPageByTaskWait")
    public TableDataInfo<FlowTaskVo> getPageByTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return flwTaskService.getPageByTaskWait(flowTaskBo, pageQuery);
    }

    /**
     * 查询当前用户的已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */

    @GetMapping("/getPageByTaskFinish")
    public TableDataInfo<FlowHisTaskVo> getPageByTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return flwTaskService.getPageByTaskFinish(flowTaskBo, pageQuery);
    }

    /**
     * 查询待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @GetMapping("/getPageByAllTaskWait")
    public TableDataInfo<FlowTaskVo> getPageByAllTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return flwTaskService.getPageByAllTaskWait(flowTaskBo, pageQuery);
    }

    /**
     * 查询已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */

    @GetMapping("/getPageByAllTaskFinish")
    public TableDataInfo<FlowHisTaskVo> getPageByAllTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return flwTaskService.getPageByAllTaskFinish(flowTaskBo, pageQuery);
    }

    /**
     * 查询当前用户的抄送
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @GetMapping("/getPageByTaskCopy")
    public TableDataInfo<FlowTaskVo> getPageByTaskCopy(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        return flwTaskService.getPageByTaskCopy(flowTaskBo, pageQuery);
    }

    /**
     * 根据taskId查询代表任务
     *
     * @param taskId 任务id
     */
    @GetMapping("/getTaskById/{taskId}")
    public R<FlowTaskVo> getTaskById(@PathVariable Long taskId) {
        Task task = taskService.getById(taskId);
        if (ObjectUtil.isNotNull(task)) {
            FlowTaskVo flowTaskVo = BeanUtil.toBean(task, FlowTaskVo.class);
            Instance instance = insService.getById(task.getInstanceId());
            flowTaskVo.setFlowStatus(instance.getFlowStatus());
            flowTaskVo.setFlowStatusName(FlowStatus.getValueByKey(instance.getFlowStatus()));
            return R.ok(flowTaskVo);
        }
        return R.fail();
    }

    /**
     * 终止任务
     *
     * @param bo 参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/terminationTask")
    public R<Instance> terminationTask(@RequestBody TerminationBo bo) {
        FlowParams flowParams = new FlowParams();
        flowParams.handler(String.valueOf(LoginHelper.getUserId()));
        flowParams.message(bo.getComment());
        flowParams.permissionFlag(WorkflowUtils.permissionList());
        return R.ok(taskService.termination(bo.getTaskId(), flowParams));
    }

    /**
     * 委派任务
     *
     * @param bo 参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/delegateTask")
    public R<Void> delegateTask(@Validated({AddGroup.class}) @RequestBody DelegateBo bo) {
        return toAjax(taskService.depute(
            bo.getTaskId(),
            String.valueOf(LoginHelper.getUserId()),
            WorkflowUtils.permissionList(),
            Collections.singletonList(bo.getUserId()),
            bo.getMessage()));
    }

    /**
     * 转办任务
     *
     * @param bo 参数
     */
    @Log(title = "任务管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/transferTask")
    public R<Void> transferTask(@Validated({AddGroup.class}) @RequestBody TransferBo bo) {
        return toAjax(taskService.transfer(
            bo.getTaskId(),
            String.valueOf(LoginHelper.getUserId()),
            WorkflowUtils.permissionList(),
            Collections.singletonList(bo.getUserId()),
            bo.getMessage()));
    }

    /**
     * 加签
     *
     * @param bo 参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/addSignature")
    public R<Void> addSignature(@Validated({AddGroup.class}) @RequestBody AddSignatureBo bo) {
        return toAjax(taskService.addSignature(
            bo.getTaskId(),
            String.valueOf(LoginHelper.getUserId()),
            WorkflowUtils.permissionList(),
            bo.getUserIds(),
            bo.getMessage()));
    }

    /**
     * 减签
     *
     * @param bo 参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/reductionSignature")
    public R<Void> reductionSignature(@Validated({AddGroup.class}) @RequestBody ReductionSignatureBo bo) {
        return toAjax(taskService.reductionSignature(
            bo.getTaskId(),
            String.valueOf(LoginHelper.getUserId()),
            WorkflowUtils.permissionList(),
            bo.getUserIds(),
            bo.getMessage()));
    }

    /**
     * 修改任务办理人
     *
     * @param taskId 任务id
     * @param userId 办理人id
     */
    @Log(title = "任务管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/updateAssignee/{taskId}/{userId}")
    public R<Void> updateAssignee(@PathVariable Long taskId, @PathVariable String userId) {
        ModifyHandler modifyHandler = new ModifyHandler()
            .setTaskId(taskId)
            .setAddHandlers(Collections.singletonList(userId))
            .setPermissionFlag(WorkflowUtils.permissionList())
            .setCooperateType(CooperateType.APPROVAL.getKey())
            .setMessage("修改任务办理人")
            .setCurUser(String.valueOf(LoginHelper.getUserId()))
            .setIgnore(false);
        return toAjax(taskService.updateHandler(modifyHandler));
    }

    /**
     * 驳回审批
     *
     * @param bo 参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/backProcess")
    public R<Void> backProcess(@Validated({AddGroup.class}) @RequestBody BackProcessBo bo) {
        return toAjax(flwTaskService.backProcess(bo));
    }

}
