package org.dromara.workflow.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.domain.bo.ProcessInstanceBo;
import org.dromara.workflow.domain.vo.ProcessInstanceVo;
import org.dromara.workflow.service.IActProcessInstanceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
     * 分页查询正在运行的流程实例
     *
     * @param processInstanceBo 参数
     */
    @GetMapping("/getProcessInstanceRunningByPage")
    public TableDataInfo<ProcessInstanceVo> getProcessInstanceRunningByPage(ProcessInstanceBo processInstanceBo) {
        return iActProcessInstanceService.getProcessInstanceRunningByPage(processInstanceBo);
    }

    /**
     * 分页查询已结束的流程实例
     *
     * @param processInstanceBo 参数
     */
    @GetMapping("/getProcessInstanceFinishByPage")
    public TableDataInfo<ProcessInstanceVo> getProcessInstanceFinishByPage(ProcessInstanceBo processInstanceBo) {
        return iActProcessInstanceService.getProcessInstanceFinishByPage(processInstanceBo);
    }

    /**
     * 通过流程实例id获取历史流程图
     *
     * @param processInstanceId 流程实例id
     * @param response          响应
     */
    @GetMapping("/getHistoryProcessImage/{processInstanceId}")
    public void getHistoryProcessImage(@NotBlank(message = "流程实例id不能为空") @PathVariable String processInstanceId, HttpServletResponse response) {
        iActProcessInstanceService.getHistoryProcessImage(processInstanceId, response);
    }

    /**
     * 获取审批记录
     *
     * @param processInstanceId 流程实例id
     */
    @GetMapping("/getHistoryRecord/{processInstanceId}")
    public R<Map<String, Object>> getHistoryRecord(@NotBlank(message = "流程实例id不能为空") @PathVariable String processInstanceId) {
        return R.ok(iActProcessInstanceService.getHistoryRecord(processInstanceId));
    }
}
