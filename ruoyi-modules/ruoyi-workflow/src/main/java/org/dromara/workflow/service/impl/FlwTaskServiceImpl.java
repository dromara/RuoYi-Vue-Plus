package org.dromara.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.warm.flow.core.dto.FlowParams;
import com.warm.flow.core.entity.Instance;
import com.warm.flow.core.entity.Task;
import com.warm.flow.core.entity.User;
import com.warm.flow.core.enums.FlowStatus;
import com.warm.flow.core.enums.NodeType;
import com.warm.flow.core.enums.SkipType;
import com.warm.flow.core.service.InsService;
import com.warm.flow.core.service.TaskService;
import com.warm.flow.core.service.UserService;
import com.warm.flow.orm.entity.FlowInstance;
import com.warm.flow.orm.entity.FlowTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.*;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.workflow.service.IFlwInstanceService;
import org.dromara.workflow.service.IFlwTaskService;
import org.dromara.workflow.service.IWfDefinitionConfigService;
import org.dromara.workflow.utils.WorkflowUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.dromara.workflow.common.constant.FlowConstant.*;

/**
 * 任务 服务层实现
 *
 * @author may
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwTaskServiceImpl implements IFlwTaskService {

    private final TaskService taskService;
    private final InsService insService;
    private final FlwTaskMapper flwTaskMapper;
    private final UserService userService;
    private final IWfDefinitionConfigService wfDefinitionConfigService;
    private final IFlwInstanceService iFlwInstanceService;

    /**
     * 启动任务
     *
     * @param startProcessBo 启动流程参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> startWorkFlow(StartProcessBo startProcessBo) {
        Map<String, Object> map = new HashMap<>(16);
        if (StringUtils.isBlank(startProcessBo.getBusinessKey())) {
            throw new ServiceException("启动工作流时必须包含业务ID");
        }
        // 启动流程实例（提交申请）
        Map<String, Object> variables = startProcessBo.getVariables();
        // 流程发起人
        variables.put(INITIATOR, (String.valueOf(LoginHelper.getUserId())));
        // 业务id
        variables.put(BUSINESS_KEY, startProcessBo.getBusinessKey());
        WfDefinitionConfigVo wfDefinitionConfigVo = wfDefinitionConfigService.getByTableNameLastVersion(startProcessBo.getTableName());
        if (wfDefinitionConfigVo == null) {
            throw new ServiceException("请到流程定义绑定业务表名与流程KEY！");
        }

        FlowInstance flowInstance = iFlwInstanceService.instanceByBusinessId(startProcessBo.getBusinessKey());
        if (flowInstance != null) {
            List<Task> taskList = taskService.list(new FlowTask().setInstanceId(flowInstance.getId()));
            map.put("processInstanceId", taskList.get(0).getInstanceId());
            map.put("taskId", taskList.get(0).getId());
            return map;
        }
        FlowParams flowParams = new FlowParams();
        flowParams.flowCode(wfDefinitionConfigVo.getProcessKey());
        flowParams.variable(startProcessBo.getVariables());

        Instance instance;
        try {
            instance = insService.start(startProcessBo.getBusinessKey(), flowParams);
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
        // 申请人执行流程
        List<Task> taskList = taskService.list(new FlowTask().setInstanceId(instance.getId()));
        if (taskList.size() > 1) {
            throw new ServiceException("请检查流程第一个环节是否为申请人！");
        }
        map.put("processInstanceId", instance.getId());
        map.put("taskId", taskList.get(0).getId());
        return map;
    }

    /**
     * 办理任务
     *
     * @param completeTaskBo 办理任务参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeTask(CompleteTaskBo completeTaskBo) {
        try {
            String userId = String.valueOf(LoginHelper.getUserId());
            Long taskId = completeTaskBo.getTaskId();
            FlowParams flowParams = new FlowParams();
            flowParams.variable(completeTaskBo.getVariables());
            flowParams.skipType(SkipType.PASS.getKey());
            flowParams.message(completeTaskBo.getMessage());
            flowParams.handler(userId);
            flowParams.permissionFlag(WorkflowUtils.permissionList());
            taskService.skip(taskId, flowParams);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 查询当前用户的待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowTaskVo> getPageByTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("t.processed_by", WorkflowUtils.permissionList());
        queryWrapper.in("t.flow_status", FlowStatus.APPROVAL.getKey());
        Page<FlowTaskVo> page = buildTaskWaitingPage(pageQuery, queryWrapper, flowTaskBo);
        return TableDataInfo.build(page);
    }

    /**
     * 查询当前用户的已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowHisTaskVo> getPageByTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("t.approver", LoginHelper.getUserId());
        Page<FlowHisTaskVo> page = buildTaskFinishPage(pageQuery, queryWrapper, flowTaskBo);
        return TableDataInfo.build(page);
    }

    /**
     * 查询待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowTaskVo> getPageByAllTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("t.processed_by", WorkflowUtils.permissionList());
        Page<FlowTaskVo> page = buildTaskWaitingPage(pageQuery, queryWrapper, flowTaskBo);
        return TableDataInfo.build(page);
    }

    private Page<FlowTaskVo> buildTaskWaitingPage(PageQuery pageQuery, QueryWrapper<FlowTaskBo> queryWrapper, FlowTaskBo flowTaskBo) {
        commonCondition(queryWrapper, flowTaskBo);
        Page<FlowTaskVo> page = flwTaskMapper.getTaskWaitByPage(pageQuery.build(), queryWrapper);
        List<FlowTaskVo> records = page.getRecords();
        if (CollUtil.isNotEmpty(records)) {
            List<Long> taskIds = StreamUtils.toList(records, FlowTaskVo::getId);
            List<User> userList = userService.getByAssociateds(taskIds);
            for (FlowTaskVo data : records) {
                if (CollUtil.isNotEmpty(userList)) {
                    List<User> users = StreamUtils.filter(userList, e -> e.getAssociated().toString().equals(data.getId().toString()));
                    data.setUserList(CollUtil.isEmpty(users) ? Collections.emptyList() : users);
                    data.setUserDTOList(WorkflowUtils.getHandlerUser(users));
                }
                data.setFlowStatusName(FlowStatus.getValueByKey(data.getFlowStatus()));
            }
        }
        return page;
    }

    /**
     * 通用条件
     *
     * @param queryWrapper 查询条件
     * @param flowTaskBo   参数
     */
    private void commonCondition(QueryWrapper<FlowTaskBo> queryWrapper, FlowTaskBo flowTaskBo) {
        queryWrapper.like(StringUtils.isNotBlank(flowTaskBo.getNodeName()), "t.node_name", flowTaskBo.getNodeName());
        queryWrapper.like(StringUtils.isNotBlank(flowTaskBo.getFlowName()), "t.flow_name", flowTaskBo.getFlowName());
        queryWrapper.eq(StringUtils.isNotBlank(flowTaskBo.getFlowCode()), "t.flow_code", flowTaskBo.getFlowCode());
        queryWrapper.eq("t.node_type", NodeType.BETWEEN.getKey());
    }

    /**
     * 查询已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowHisTaskVo> getPageByAllTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = new QueryWrapper<>();
        Page<FlowHisTaskVo> page = buildTaskFinishPage(pageQuery, queryWrapper, flowTaskBo);
        return TableDataInfo.build(page);
    }

    private Page<FlowHisTaskVo> buildTaskFinishPage(PageQuery pageQuery, QueryWrapper<FlowTaskBo> queryWrapper, FlowTaskBo flowTaskBo) {
        commonCondition(queryWrapper, flowTaskBo);
        Page<FlowHisTaskVo> page = flwTaskMapper.getTaskFinishByPage(pageQuery.build(), queryWrapper);
        List<FlowHisTaskVo> records = page.getRecords();
        if (CollUtil.isNotEmpty(records)) {
            for (FlowHisTaskVo data : records) {
                data.setFlowStatusName(FlowStatus.getValueByKey(data.getFlowStatus()));
            }
        }
        return page;
    }

    /**
     * 查询当前用户的抄送
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowTaskVo> getPageByTaskCopy(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(flowTaskBo.getNodeName()), "t.node_name", flowTaskBo.getNodeName());
        queryWrapper.like(StringUtils.isNotBlank(flowTaskBo.getFlowName()), "t.flow_name", flowTaskBo.getFlowName());
        queryWrapper.eq(StringUtils.isNotBlank(flowTaskBo.getFlowCode()), "t.flow_code", flowTaskBo.getFlowCode());
        Page<FlowTaskVo> page = flwTaskMapper.getTaskCopyByPage(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 驳回任务
     *
     * @param bo 参数
     */
    @Override
    public boolean backProcess(BackProcessBo bo) {
        try {
            String userId = String.valueOf(LoginHelper.getUserId());
            Long taskId = bo.getTaskId();
            FlowParams flowParams = new FlowParams();
            flowParams.variable(bo.getVariables());
            flowParams.skipType(SkipType.REJECT.getKey());
            flowParams.message(bo.getMessage());
            flowParams.handler(userId);
            flowParams.nodeCode(bo.getTargetNodeCode());
            taskService.skip(taskId, flowParams);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }
}
