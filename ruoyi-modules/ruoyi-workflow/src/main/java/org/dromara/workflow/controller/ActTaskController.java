package org.dromara.workflow.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.workflow.domain.bo.StartProcessBo;
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
public class ActTaskController {

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
}
