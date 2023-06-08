package org.dromara.workflow.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.service.IActProcessInstanceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 流程实例管理 控制层
 *
 * @author may
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/processInstance")
public class ActProcessInstanceController extends BaseController {

    private final IActProcessInstanceService iActProcessInstanceService;

    /**
     * 通过流程实例id获取历史流程图
     *
     * @param processInstanceId 流程实例id
     * @param response          响应
     */
    @GetMapping("/getHistoryProcessImage/{processInstanceId}")
    public void getHistoryProcessImage(@NotBlank(message = "流程实例id不能为空") @PathVariable String processInstanceId,
                                       HttpServletResponse response) {
        iActProcessInstanceService.getHistoryProcessImage(processInstanceId, response);
    }
}
