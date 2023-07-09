package org.dromara.workflow.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.workflow.domain.bo.SysUserMultiBo;
import org.dromara.workflow.domain.vo.TaskVo;
import org.dromara.workflow.service.IWorkflowUserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 工作流用户选人管理 控制层
 *
 * @author may
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/user")
public class WorkflowUserController extends BaseController {

    private final IWorkflowUserService iWorkflowUserService;

    /**
     * 分页查询工作流选择加签人员
     *
     * @param sysUserMultiBo 参数
     */
    @GetMapping("/getWorkflowAddMultiListByPage")
    public TableDataInfo<SysUserVo> getWorkflowAddMultiInstanceByPage(SysUserMultiBo sysUserMultiBo) {
        return iWorkflowUserService.getWorkflowAddMultiInstanceByPage(sysUserMultiBo);
    }

    /**
     * 查询工作流选择减签人员
     *
     * @param taskId 任务id
     */
    @GetMapping("/getWorkflowDeleteMultiInstanceList/{taskId}")
    public R<List<TaskVo>> getWorkflowDeleteMultiInstanceList(@PathVariable String taskId) {
        return R.ok(iWorkflowUserService.getWorkflowDeleteMultiInstanceList(taskId));
    }
}
