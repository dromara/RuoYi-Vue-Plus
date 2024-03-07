package org.dromara.workflow.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.workflow.domain.bo.ProcessInstanceBo;
import org.dromara.workflow.domain.bo.ProcessInvalidBo;
import org.dromara.workflow.domain.bo.TaskUrgingBo;
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

    private final IActProcessInstanceService actProcessInstanceService;

    /**
     * 分页查询正在运行的流程实例
     *
     * @param bo 参数
     */
    @GetMapping("/getPageByRunning")
    public TableDataInfo<ProcessInstanceVo> getPageByRunning(ProcessInstanceBo bo, PageQuery pageQuery) {
        return actProcessInstanceService.getPageByRunning(bo, pageQuery);
    }

    /**
     * 分页查询已结束的流程实例
     *
     * @param bo 参数
     */
    @GetMapping("/getPageByFinish")
    public TableDataInfo<ProcessInstanceVo> getPageByFinish(ProcessInstanceBo bo, PageQuery pageQuery) {
        return actProcessInstanceService.getPageByFinish(bo, pageQuery);
    }

    /**
     * 通过流程实例id获取历史流程图
     *
     * @param processInstanceId 流程实例id
     */
    @GetMapping("/getHistoryImage/{processInstanceId}")
    public R<String> getHistoryImage(@NotBlank(message = "流程实例id不能为空") @PathVariable String processInstanceId) {
        return R.ok("操作成功", actProcessInstanceService.getHistoryImage(processInstanceId));
    }

    /**
     * 通过流程实例id获取历史流程图运行中，历史等节点
     *
     * @param processInstanceId 流程实例id
     */
    @GetMapping("/getHistoryList/{processInstanceId}")
    public R<Map<String, Object>> getHistoryList(@NotBlank(message = "流程实例id不能为空") @PathVariable String processInstanceId) {
        return R.ok("操作成功", actProcessInstanceService.getHistoryList(processInstanceId));
    }

    /**
     * 获取审批记录
     *
     * @param processInstanceId 流程实例id
     */
    @GetMapping("/getHistoryRecord/{processInstanceId}")
    public R<Map<String, Object>> getHistoryRecord(@NotBlank(message = "流程实例id不能为空") @PathVariable String processInstanceId) {
        return R.ok(actProcessInstanceService.getHistoryRecord(processInstanceId));
    }

    /**
     * 作废流程实例，不会删除历史记录(删除运行中的实例)
     *
     * @param processInvalidBo 参数
     */
    @Log(title = "流程实例管理", businessType = BusinessType.DELETE)
    @RepeatSubmit()
    @PostMapping("/deleteRunInstance")
    public R<Void> deleteRunInstance(@Validated(AddGroup.class) @RequestBody ProcessInvalidBo processInvalidBo) {
        return toAjax(actProcessInstanceService.deleteRunInstance(processInvalidBo));
    }

    /**
     * 运行中的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param processInstanceIds 流程实例id
     */
    @Log(title = "流程实例管理", businessType = BusinessType.DELETE)
    @RepeatSubmit()
    @DeleteMapping("/deleteRunAndHisInstance/{processInstanceIds}")
    public R<Void> deleteRunAndHisInstance(@NotNull(message = "流程实例id不能为空") @PathVariable String[] processInstanceIds) {
        return toAjax(actProcessInstanceService.deleteRunAndHisInstance(Arrays.asList(processInstanceIds)));
    }

    /**
     * 已完成的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param processInstanceIds 流程实例id
     */
    @Log(title = "流程实例管理", businessType = BusinessType.DELETE)
    @RepeatSubmit()
    @DeleteMapping("/deleteFinishAndHisInstance/{processInstanceIds}")
    public R<Void> deleteFinishAndHisInstance(@NotNull(message = "流程实例id不能为空") @PathVariable String[] processInstanceIds) {
        return toAjax(actProcessInstanceService.deleteFinishAndHisInstance(Arrays.asList(processInstanceIds)));
    }

    /**
     * 撤销流程申请
     *
     * @param processInstanceId 流程实例id
     */
    @Log(title = "流程实例管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/cancelProcessApply/{processInstanceId}")
    public R<Void> cancelProcessApply(@NotBlank(message = "流程实例id不能为空") @PathVariable String processInstanceId) {
        return toAjax(actProcessInstanceService.cancelProcessApply(processInstanceId));
    }

    /**
     * 分页查询当前登录人单据
     *
     * @param bo 参数
     */
    @GetMapping("/getPageByCurrent")
    public TableDataInfo<ProcessInstanceVo> getPageByCurrent(ProcessInstanceBo bo, PageQuery pageQuery) {
        return actProcessInstanceService.getPageByCurrent(bo, pageQuery);
    }

    /**
     * 任务催办(给当前任务办理人发送站内信，邮件，短信等)
     *
     * @param taskUrgingBo 任务催办
     */
    @Log(title = "流程实例管理", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/taskUrging")
    public R<Void> taskUrging(@RequestBody TaskUrgingBo taskUrgingBo) {
        return toAjax(actProcessInstanceService.taskUrging(taskUrgingBo));
    }

}
