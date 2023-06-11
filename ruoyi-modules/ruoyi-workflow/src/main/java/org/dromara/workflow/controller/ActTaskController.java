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
     * 查询当前用户的已办任务
     *
     * @param taskBo 参数
     */
    @GetMapping("/getTaskFinishByPage")
    public TableDataInfo<TaskVo> getTaskFinishByPage(TaskBo taskBo) {
        return iActTaskService.getTaskFinishByPage(taskBo);
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

}
