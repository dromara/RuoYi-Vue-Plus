package org.dromara.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.enums.UserStatus;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.dromara.workflow.domain.bo.SysUserMultiBo;
import org.dromara.workflow.domain.vo.MultiInstanceVo;
import org.dromara.workflow.domain.vo.TaskVo;
import org.dromara.workflow.service.IWorkflowUserService;
import org.dromara.workflow.utils.WorkflowUtils;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.engine.impl.bpmn.behavior.SequentialMultiInstanceBehavior;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 工作流用户选人管理 业务处理层
 *
 * @author may
 */
@RequiredArgsConstructor
@Service
public class WorkflowUserServiceImpl implements IWorkflowUserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final TaskService taskService;
    private final RuntimeService runtimeService;

    /**
     * 分页查询工作流选择加签人员
     *
     * @param sysUserMultiBo 参数
     */
    @Override
    @SuppressWarnings("unchecked")
    public TableDataInfo<SysUserVo> getWorkflowAddMultiInstanceByPage(SysUserMultiBo sysUserMultiBo) {
        Task task = taskService.createTaskQuery().taskId(sysUserMultiBo.getTaskId()).singleResult();
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        MultiInstanceVo multiInstance = WorkflowUtils.isMultiInstance(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
        if (multiInstance == null) {
            return TableDataInfo.build();
        }
        LambdaQueryWrapper<SysUser> queryWrapper = Wrappers.lambdaQuery();
        //检索条件
        queryWrapper.eq(StringUtils.isNotEmpty(sysUserMultiBo.getDeptId()), SysUser::getDeptId, sysUserMultiBo.getDeptId());
        queryWrapper.eq(SysUser::getStatus, UserStatus.OK.getCode());
        if (multiInstance.getType() instanceof SequentialMultiInstanceBehavior) {
            List<Long> assigneeList = (List<Long>) runtimeService.getVariable(task.getExecutionId(), multiInstance.getAssigneeList());
            queryWrapper.notIn(CollectionUtil.isNotEmpty(assigneeList), SysUser::getUserId, assigneeList);
        } else {
            List<Task> list = taskService.createTaskQuery().processInstanceId(task.getProcessInstanceId()).list();
            List<Long> userIds = StreamUtils.toList(list, e -> Long.valueOf(e.getAssignee()));
            queryWrapper.notIn(CollectionUtil.isNotEmpty(userIds), SysUser::getUserId, userIds);
        }
        queryWrapper.like(StringUtils.isNotEmpty(sysUserMultiBo.getUserName()), SysUser::getUserName, sysUserMultiBo.getUserName());
        queryWrapper.like(StringUtils.isNotEmpty(sysUserMultiBo.getNickName()), SysUser::getNickName, sysUserMultiBo.getNickName());
        Page<SysUser> page = new Page<>(sysUserMultiBo.getPageNum(), sysUserMultiBo.getPageSize());
        Page<SysUserVo> userPage = sysUserMapper.selectVoPage(page, queryWrapper);
        return TableDataInfo.build(recordPage(userPage));
    }

    /**
     * 查询工作流选择减签人员
     *
     * @param taskId 任务id 任务id
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<TaskVo> getWorkflowDeleteMultiInstanceList(String taskId) {
        Task task = taskService.createTaskQuery().taskTenantId(TenantHelper.getTenantId()).taskId(taskId).singleResult();
        List<Task> taskList = taskService.createTaskQuery().taskTenantId(TenantHelper.getTenantId()).processInstanceId(task.getProcessInstanceId()).list();
        MultiInstanceVo multiInstance = WorkflowUtils.isMultiInstance(task.getProcessDefinitionId(), task.getTaskDefinitionKey());
        List<TaskVo> taskListVo = new ArrayList<>();
        if (multiInstance == null) {
            return Collections.emptyList();
        }
        List<Long> assigneeList = new ArrayList<>();
        if (multiInstance.getType() instanceof SequentialMultiInstanceBehavior) {
            List<Object> variable = (List<Object>) runtimeService.getVariable(task.getExecutionId(), multiInstance.getAssigneeList());
            for (Object o : variable) {
                assigneeList.add(Long.valueOf(o.toString()));
            }
        }

        if (multiInstance.getType() instanceof SequentialMultiInstanceBehavior) {
            List<Long> userIds = StreamUtils.filter(assigneeList, e -> !String.valueOf(e).equals(task.getAssignee()));
            List<SysUserVo> sysUsers = null;
            if (CollectionUtil.isNotEmpty(userIds)) {
                sysUsers = sysUserMapper.selectVoBatchIds(userIds);
            }
            for (Long userId : userIds) {
                TaskVo taskVo = new TaskVo();
                taskVo.setId("串行会签");
                taskVo.setExecutionId("串行会签");
                taskVo.setProcessInstanceId(task.getProcessInstanceId());
                taskVo.setName(task.getName());
                taskVo.setAssignee(userId);
                if (CollectionUtil.isNotEmpty(sysUsers)) {
                    sysUsers.stream().filter(u -> u.getUserId().toString().equals(userId.toString())).findFirst().ifPresent(u -> taskVo.setAssigneeName(u.getNickName()));
                }
                taskListVo.add(taskVo);
            }
            return taskListVo;
        } else if (multiInstance.getType() instanceof ParallelMultiInstanceBehavior) {
            List<Task> tasks = StreamUtils.filter(taskList, e -> StringUtils.isBlank(e.getParentTaskId()) && !e.getExecutionId().equals(task.getExecutionId()) && e.getTaskDefinitionKey().equals(task.getTaskDefinitionKey()));
            if (CollectionUtil.isNotEmpty(tasks)) {
                List<Long> userIds = StreamUtils.toList(tasks, e -> Long.valueOf(e.getAssignee()));
                List<SysUserVo> sysUsers = null;
                if (CollectionUtil.isNotEmpty(userIds)) {
                    sysUsers = sysUserMapper.selectVoBatchIds(userIds);
                }
                for (Task t : tasks) {
                    TaskVo taskVo = new TaskVo();
                    taskVo.setId(t.getId());
                    taskVo.setExecutionId(t.getExecutionId());
                    taskVo.setProcessInstanceId(t.getProcessInstanceId());
                    taskVo.setName(t.getName());
                    taskVo.setAssignee(Long.valueOf(t.getAssignee()));
                    if (CollectionUtil.isNotEmpty(sysUsers)) {
                        sysUsers.stream().filter(u -> u.getUserId().toString().equals(t.getAssignee())).findFirst().ifPresent(e -> taskVo.setAssigneeName(e.getNickName()));
                    }
                    taskListVo.add(taskVo);
                }
                return taskListVo;
            }
        }
        return Collections.emptyList();
    }

    /**
     * 翻译部门
     *
     * @param page 用户分页数据
     */
    private Page<SysUserVo> recordPage(Page<SysUserVo> page) {
        List<SysUserVo> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            return page;
        }
        List<Long> collectDeptId = StreamUtils.toList(records, SysUserVo::getDeptId);
        if (CollUtil.isEmpty(collectDeptId)) {
            return page;
        }
        page.setRecords(records);
        return page;
    }

    /**
     * 按照用户id查询用户
     *
     * @param userIds 用户id
     */
    @Override
    public List<SysUserVo> getUserListByIds(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysUser> queryWrapper = Wrappers.lambdaQuery();
        // 检索条件
        queryWrapper.eq(SysUser::getStatus, UserStatus.OK.getCode());
        queryWrapper.in(SysUser::getUserId, userIds);
        return sysUserMapper.selectVoList(queryWrapper);
    }

    /**
     * 按照角色id查询关联用户id
     *
     * @param roleIds 角色id
     */
    @Override
    public List<SysUserRole> getUserRoleListByRoleIds(List<Long> roleIds) {
        return sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleId, roleIds));
    }

    /**
     * 分页查询用户
     *
     * @param sysUserBo 参数
     * @param pageQuery 分页
     */
    @Override
    public TableDataInfo<SysUserVo> getUserListByPage(SysUserBo sysUserBo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysUser> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(sysUserBo.getDeptId() != null, SysUser::getDeptId, sysUserBo.getDeptId());
        queryWrapper.eq(SysUser::getStatus, UserStatus.OK.getCode());
        queryWrapper.like(StringUtils.isNotEmpty(sysUserBo.getUserName()), SysUser::getUserName, sysUserBo.getUserName());
        queryWrapper.like(StringUtils.isNotEmpty(sysUserBo.getNickName()), SysUser::getNickName, sysUserBo.getNickName());
        Page<SysUserVo> userPage = sysUserMapper.selectVoPage(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(recordPage(userPage));
    }
}
