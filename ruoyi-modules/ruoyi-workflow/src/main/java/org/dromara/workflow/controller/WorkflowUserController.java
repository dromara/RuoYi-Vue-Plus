package org.dromara.workflow.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.SysUserBo;
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

    private final IWorkflowUserService workflowUserService;

    /**
     * 分页查询工作流选择加签人员
     *
     * @param bo 参数
     */
    @GetMapping("/getPageByAddMultiInstance")
    public TableDataInfo<SysUserVo> getPageByAddMultiInstance(SysUserMultiBo bo, PageQuery pageQuery) {
        return workflowUserService.getPageByAddMultiInstance(bo, pageQuery);
    }

    /**
     * 查询工作流选择减签人员
     *
     * @param taskId 任务id
     */
    @GetMapping("/getListByDeleteMultiInstance/{taskId}")
    public R<List<TaskVo>> getListByDeleteMultiInstance(@PathVariable String taskId) {
        return R.ok(workflowUserService.getListByDeleteMultiInstance(taskId));
    }

    /**
     * 按照用户id查询用户
     *
     * @param userIds 用户id
     */
    @GetMapping("/getUserListByIds/{userIds}")
    public R<List<SysUserVo>> getUserListByIds(@PathVariable List<Long> userIds) {
        return R.ok(workflowUserService.getUserListByIds(userIds));
    }

    /**
     * 分页查询用户
     *
     * @param sysUserBo 参数
     * @param pageQuery 分页
     */
    @GetMapping("/getPageByUserList")
    public TableDataInfo<SysUserVo> getPageByUserList(SysUserBo sysUserBo, PageQuery pageQuery) {
        return workflowUserService.getPageByUserList(sysUserBo, pageQuery);
    }
}
