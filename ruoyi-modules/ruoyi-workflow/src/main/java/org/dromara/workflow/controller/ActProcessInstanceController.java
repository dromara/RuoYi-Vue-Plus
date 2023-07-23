package org.dromara.workflow.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.domain.bo.ProcessInstanceBo;
import org.dromara.workflow.domain.bo.ProcessInvalidBo;
import org.dromara.workflow.domain.vo.ProcessInstanceVo;
import org.dromara.workflow.service.IActProcessInstanceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
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

    /**
     * 作废流程实例，不会删除历史记录(删除运行中的实例)
     *
     * @param processInvalidBo 参数
     */
    @Log(title = "流程实例管理", businessType = BusinessType.DELETE)
    @PostMapping("/deleteRuntimeProcessInst")
    public R<Void> deleteRuntimeProcessInst(@Validated(AddGroup.class) @RequestBody ProcessInvalidBo processInvalidBo) {
        return toAjax(iActProcessInstanceService.deleteRuntimeProcessInst(processInvalidBo));
    }

    /**
     * 运行中的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param processInstanceIds 流程实例id
     */
    @Log(title = "流程实例管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/deleteRuntimeProcessAndHisInst/{processInstanceIds}")
    public R<Void> deleteRuntimeProcessAndHisInst(@NotNull(message = "流程实例id不能为空") @PathVariable String[] processInstanceIds) {
        return toAjax(iActProcessInstanceService.deleteRuntimeProcessAndHisInst(Arrays.asList(processInstanceIds)));
    }

    /**
     * 已完成的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param processInstanceIds 流程实例id
     */
    @Log(title = "流程实例管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/deleteFinishProcessAndHisInst/{processInstanceIds}")
    public R<Void> deleteFinishProcessAndHisInst(@NotNull(message = "流程实例id不能为空") @PathVariable String[] processInstanceIds) {
        return toAjax(iActProcessInstanceService.deleteFinishProcessAndHisInst(Arrays.asList(processInstanceIds)));
    }

    /**
     * 撤销流程申请
     *
     * @param processInstanceId 流程实例id
     */
    @Log(title = "流程实例管理", businessType = BusinessType.INSERT)
    @PostMapping("/cancelProcessApply/{processInstanceId}")
    public R<Void> cancelProcessApply(@NotBlank(message = "流程实例id不能为空") @PathVariable String processInstanceId) {
        return toAjax(iActProcessInstanceService.cancelProcessApply(processInstanceId));
    }

    /**
     * 分页查询当前登录人单据
     *
     * @param processInstanceBo 参数
     */
    @GetMapping("/getCurrentSubmitByPage")
    public TableDataInfo<ProcessInstanceVo> getCurrentSubmitByPage(ProcessInstanceBo processInstanceBo) {
        return iActProcessInstanceService.getCurrentSubmitByPage(processInstanceBo);
    }
}
