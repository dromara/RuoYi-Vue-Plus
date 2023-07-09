package org.dromara.workflow.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.TaskVo;
import org.dromara.workflow.service.IActTaskService;
import org.dromara.workflow.utils.WorkflowUtils;
import org.flowable.engine.TaskService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
public class ActTaskController extends BaseController {

    private final IActTaskService iActTaskService;

    private final TaskService taskService;


    /**
     * 启动任务
     *
     * @param startProcessBo 启动流程参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/startWorkFlow")
    public R<Map<String, Object>> startWorkFlow(@RequestBody StartProcessBo startProcessBo) {
        Map<String, Object> map = iActTaskService.startWorkFlow(startProcessBo);
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
        return toAjax(iActTaskService.completeTask(completeTaskBo));
    }

    /**
     * 查询当前用户的待办任务
     *
     * @param taskBo 参数
     */
    @GetMapping("/getTaskWaitByPage")
    public TableDataInfo<TaskVo> getTaskWaitByPage(TaskBo taskBo) {
        return iActTaskService.getTaskWaitByPage(taskBo);
    }

    /**
     * 查询当前租户所有待办任务
     *
     * @param taskBo 参数
     */
    @GetMapping("/getAllTaskWaitByPage")
    public TableDataInfo<TaskVo> getAllTaskWaitByPage(TaskBo taskBo) {
        return iActTaskService.getAllTaskWaitByPage(taskBo);
    }

    /**
     * 查询当前用户的已办任务
     *
     * @param taskBo 参数
     */
    @GetMapping("/getTaskFinishByPage")
    public TableDataInfo<TaskVo> getTaskFinishByPage(TaskBo taskBo) {
        return iActTaskService.getTaskFinishByPage(taskBo);
    }

    /**
     * 查询当前租户所有已办任务
     *
     * @param taskBo 参数
     */
    @GetMapping("/getAllTaskFinishByPage")
    public TableDataInfo<TaskVo> getAllTaskFinishByPage(TaskBo taskBo) {
        return iActTaskService.getAllTaskFinishByPage(taskBo);
    }

    /**
     * 签收（拾取）任务
     *
     * @param taskId 任务id
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @PostMapping("/claim/{taskId}")
    public R<Void> claimTask(@NotBlank(message = "任务id不能为空") @PathVariable String taskId) {
        try {
            taskService.claim(taskId, String.valueOf(LoginHelper.getUserId()));
            return R.ok();
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("签收任务失败：" + e.getMessage());
        }
    }

    /**
     * 归还（拾取的）任务
     *
     * @param taskId 任务id
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @PostMapping("/returnTask/{taskId}")
    public R<Void> returnTask(@NotBlank(message = "任务id不能为空") @PathVariable String taskId) {
        try {
            taskService.setAssignee(taskId, null);
            return R.ok();
        } catch (Exception e) {
            e.printStackTrace();
            return R.fail("归还任务失败：" + e.getMessage());
        }
    }

    /**
     * 委派任务
     *
     * @param delegateBo 参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @PostMapping("/delegateTask")
    public R<Void> delegateTask(@Validated({AddGroup.class}) @RequestBody DelegateBo delegateBo) {
        return toAjax(iActTaskService.delegateTask(delegateBo));
    }

    /**
     * 终止任务
     *
     * @param terminationBo 参数
     */
    @Log(title = "任务管理", businessType = BusinessType.DELETE)
    @PostMapping("/terminationTask")
    public R<Void> terminationTask(@RequestBody TerminationBo terminationBo) {
        return toAjax(iActTaskService.terminationTask(terminationBo));
    }

    /**
     * 转办任务
     *
     * @param transmitBo 参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @PostMapping("/transferTask")
    public R<Void> transferTask(@Validated({AddGroup.class}) @RequestBody TransmitBo transmitBo) {
        return toAjax(iActTaskService.transferTask(transmitBo));
    }

    /**
     * 会签任务加签
     *
     * @param addMultiBo 参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @PostMapping("/addMultiInstanceExecution")
    public R<Void> addMultiInstanceExecution(@Validated({AddGroup.class}) @RequestBody AddMultiBo addMultiBo) {
        return toAjax(iActTaskService.addMultiInstanceExecution(addMultiBo));
    }

    /**
     * 会签任务减签
     *
     * @param deleteMultiBo 参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @PostMapping("/deleteMultiInstanceExecution")
    public R<Void> deleteMultiInstanceExecution(@Validated({AddGroup.class}) @RequestBody DeleteMultiBo deleteMultiBo) {
        return toAjax(iActTaskService.deleteMultiInstanceExecution(deleteMultiBo));
    }

    /**
     * 驳回审批
     *
     * @param backProcessBo 参数
     */
    @Log(title = "任务管理", businessType = BusinessType.INSERT)
    @PostMapping("/backProcess")
    public R<String> backProcess(@RequestBody BackProcessBo backProcessBo) {
        return R.ok(iActTaskService.backProcess(backProcessBo));
    }

    /**
     * 获取流程状态
     *
     * @param taskId 任务id
     */
    @GetMapping("/getBusinessStatus/{taskId}")
    public R<String> getBusinessStatus(@PathVariable String taskId) {
        return R.ok("操作成功", WorkflowUtils.getBusinessStatus(taskId));
    }


}
