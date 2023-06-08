package org.dromara.workflow.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.domain.bo.CompleteTaskBo;
import org.dromara.workflow.domain.bo.StartProcessBo;
import org.dromara.workflow.domain.bo.TaskBo;
import org.dromara.workflow.domain.vo.TaskVo;
import org.dromara.workflow.service.IActTaskService;
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
}
