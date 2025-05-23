package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.dto.StartProcessReturnDTO;
import org.dromara.common.core.domain.dto.UserDTO;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.UserService;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.ValidatorUtils;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.*;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.core.service.*;
import org.dromara.warm.flow.core.utils.ExpressionUtil;
import org.dromara.warm.flow.core.utils.MapUtil;
import org.dromara.warm.flow.orm.entity.*;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.dromara.warm.flow.orm.mapper.FlowInstanceMapper;
import org.dromara.warm.flow.orm.mapper.FlowNodeMapper;
import org.dromara.warm.flow.orm.mapper.FlowTaskMapper;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.enums.TaskAssigneeType;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.dromara.workflow.domain.vo.FlowTaskVo;
import org.dromara.workflow.handler.FlowProcessEventHandler;
import org.dromara.workflow.handler.WorkflowPermissionHandler;
import org.dromara.workflow.mapper.FlwCategoryMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.workflow.service.IFlwCommonService;
import org.dromara.workflow.service.IFlwTaskAssigneeService;
import org.dromara.workflow.service.IFlwTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.dromara.workflow.common.constant.FlowConstant.*;

/**
 * 任务 服务层实现
 *
 * @author may
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwTaskServiceImpl implements IFlwTaskService {

    private final TaskService taskService;
    private final InsService insService;
    private final DefService defService;
    private final HisTaskService hisTaskService;
    private final NodeService nodeService;
    private final FlowInstanceMapper flowInstanceMapper;
    private final FlowTaskMapper flowTaskMapper;
    private final FlowHisTaskMapper flowHisTaskMapper;
    private final IdentifierGenerator identifierGenerator;
    private final FlowProcessEventHandler flowProcessEventHandler;
    private final UserService userService;
    private final FlwTaskMapper flwTaskMapper;
    private final FlwCategoryMapper flwCategoryMapper;
    private final FlowNodeMapper flowNodeMapper;
    private final IFlwTaskAssigneeService flwTaskAssigneeService;
    private final IFlwCommonService flwCommonService;

    /**
     * 启动任务
     *
     * @param startProcessBo 启动流程参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StartProcessReturnDTO startWorkFlow(StartProcessBo startProcessBo) {
        String businessId = startProcessBo.getBusinessId();
        if (StringUtils.isBlank(businessId)) {
            throw new ServiceException("启动工作流时必须包含业务ID");
        }
        // 启动流程实例（提交申请）
        Map<String, Object> variables = startProcessBo.getVariables();
        // 流程发起人
        variables.put(INITIATOR, LoginHelper.getUserIdStr());
        // 业务id
        variables.put(BUSINESS_ID, businessId);
        FlowInstance flowInstance = flowInstanceMapper.selectOne(new LambdaQueryWrapper<>(FlowInstance.class)
            .eq(FlowInstance::getBusinessId, businessId));
        if (ObjectUtil.isNotNull(flowInstance)) {
            BusinessStatusEnum.checkStartStatus(flowInstance.getFlowStatus());
            List<Task> taskList = taskService.list(new FlowTask().setInstanceId(flowInstance.getId()));
            taskService.mergeVariable(flowInstance, variables);
            insService.updateById(flowInstance);
            StartProcessReturnDTO dto = new StartProcessReturnDTO();
            dto.setProcessInstanceId(taskList.get(0).getInstanceId());
            dto.setTaskId(taskList.get(0).getId());
            return dto;
        }
        FlowParams flowParams = new FlowParams();
        flowParams.flowCode(startProcessBo.getFlowCode());
        flowParams.variable(startProcessBo.getVariables());
        flowParams.flowStatus(BusinessStatusEnum.DRAFT.getStatus());
        Instance instance;
        try {
            instance = insService.start(businessId, flowParams);
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
        // 申请人执行流程
        List<Task> taskList = taskService.list(new FlowTask().setInstanceId(instance.getId()));
        if (taskList.size() > 1) {
            throw new ServiceException("请检查流程第一个环节是否为申请人！");
        }
        StartProcessReturnDTO dto = new StartProcessReturnDTO();
        dto.setProcessInstanceId(instance.getId());
        dto.setTaskId(taskList.get(0).getId());
        return dto;
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
            // 获取任务ID并查询对应的流程任务和实例信息
            Long taskId = completeTaskBo.getTaskId();
            List<String> messageType = completeTaskBo.getMessageType();
            String notice = completeTaskBo.getNotice();
            // 获取抄送人
            List<FlowCopyBo> flowCopyList = completeTaskBo.getFlowCopyList();
            FlowTask flowTask = flowTaskMapper.selectById(taskId);
            if (ObjectUtil.isNull(flowTask)) {
                throw new ServiceException("流程任务不存在或任务已审批！");
            }
            Instance ins = insService.getById(flowTask.getInstanceId());
            // 获取流程定义信息
            Definition definition = defService.getById(flowTask.getDefinitionId());
            // 检查流程状态是否为草稿、已撤销或已退回状态，若是则执行流程提交监听
            if (BusinessStatusEnum.isDraftOrCancelOrBack(ins.getFlowStatus())) {
                flowProcessEventHandler.processHandler(definition.getFlowCode(), ins.getBusinessId(), ins.getFlowStatus(), null, true);
            }
            // 设置弹窗处理人
            Map<String, Object> assigneeMap = setPopAssigneeMap(completeTaskBo.getAssigneeMap(), ins.getVariableMap());
            if (CollUtil.isNotEmpty(assigneeMap)) {
                completeTaskBo.getVariables().putAll(assigneeMap);
            }
            // 构建流程参数，包括变量、跳转类型、消息、处理人、权限等信息
            FlowParams flowParams = new FlowParams();
            flowParams.variable(completeTaskBo.getVariables());
            flowParams.skipType(SkipType.PASS.getKey());
            flowParams.message(completeTaskBo.getMessage());
            flowParams.flowStatus(BusinessStatusEnum.WAITING.getStatus()).hisStatus(TaskStatusEnum.PASS.getStatus());

            flowParams.hisTaskExt(completeTaskBo.getFileId());
            // 执行任务跳转，并根据返回的处理人设置下一步处理人
            Instance instance = taskService.skip(taskId, flowParams);
            this.setHandler(instance, flowTask, flowCopyList);
            // 消息通知
            flwCommonService.sendMessage(definition.getFlowName(), ins.getId(), messageType, notice);
            //设置下一环节处理人
            setNextHandler(ins.getId());
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 设置下一环节处理人
     *
     * @param instanceId 实例ID
     */
    private void setNextHandler(Long instanceId) {
        Instance inst = insService.getById(instanceId);
        List<FlowTask> flowTaskList = selectByInstId(instanceId);
        Map<String, Object> variableMap = inst.getVariableMap();
        for (FlowTask task : flowTaskList) {
            if (variableMap != null && variableMap.containsKey(task.getNodeCode())) {
                String userIds = variableMap.get(task.getNodeCode()).toString();
                // 批量删除现有任务的办理人记录
                flwCommonService.getFlowUserService().deleteByTaskIds(List.of(task.getId()));
                // 批量新增任务办理人记录
                Set<User> users = flwCommonService.buildFlowUser(List.of(userIds.split(StringUtils.SEPARATOR)), task.getId());
                flwCommonService.getFlowUserService().saveBatch(new ArrayList<>(users));
                variableMap.remove(task.getNodeCode());
            }
        }
        taskService.mergeVariable(inst, variableMap);
    }

    /**
     * 设置弹窗处理人
     *
     * @param assigneeMap  处理人
     * @param variablesMap 变量
     */
    private Map<String, Object> setPopAssigneeMap(Map<String, Object> assigneeMap, Map<String, Object> variablesMap) {
        Map<String, Object> map = new HashMap<>();
        if (CollUtil.isEmpty(assigneeMap)) {
            return map;
        }
        for (Map.Entry<String, Object> entry : assigneeMap.entrySet()) {
            if (variablesMap.containsKey(entry.getKey())) {
                String userIds = variablesMap.get(entry.getKey()).toString();
                if (StringUtils.isNotBlank(userIds)) {
                    Set<String> hashSet = new HashSet<>();
                    //弹窗传入的选人
                    List<String> popUserIds = Arrays.asList(entry.getValue().toString().split(StringUtils.SEPARATOR));
                    //已有的选人
                    List<String> variableUserIds = Arrays.asList(userIds.split(StringUtils.SEPARATOR));
                    hashSet.addAll(popUserIds);
                    hashSet.addAll(variableUserIds);
                    map.put(entry.getKey(), String.join(StringUtils.SEPARATOR, hashSet));
                }
            } else {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    /**
     * 设置办理人
     *
     * @param instance     实例
     * @param task         (当前任务)未办理的任务
     * @param flowCopyList 抄送人
     */
    private void setHandler(Instance instance, FlowTask task, List<FlowCopyBo> flowCopyList) {
        if (ObjectUtil.isNull(instance)) {
            return;
        }
        // 添加抄送人
        this.setCopy(task, flowCopyList);
        // 根据流程实例ID查询所有关联的任务
        List<FlowTask> flowTasks = this.selectByInstId(instance.getId());
        if (CollUtil.isEmpty(flowTasks)) {
            return;
        }
        List<Long> taskIdList = StreamUtils.toList(flowTasks, FlowTask::getId);
        // 获取与当前任务关联的用户列表
        List<User> associatedUsers = flwCommonService.getFlowUserService().getByAssociateds(taskIdList);
        if (CollUtil.isEmpty(associatedUsers)) {
            return;
        }
        List<User> userList = new ArrayList<>();
        // 遍历任务列表，处理每个任务的办理人
        for (FlowTask flowTask : flowTasks) {
            List<User> users = StreamUtils.filter(associatedUsers, user -> Objects.equals(user.getAssociated(), flowTask.getId()));
            if (CollUtil.isNotEmpty(users)) {
                userList.addAll(flwCommonService.buildUser(users, flowTask.getId()));
            }
        }
        // 批量删除现有任务的办理人记录
        flwCommonService.getFlowUserService().deleteByTaskIds(taskIdList);
        // 确保要保存的 userList 不为空
        if (CollUtil.isEmpty(userList)) {
            return;
        }
        flwCommonService.getFlowUserService().saveBatch(userList);
    }

    /**
     * 添加抄送人
     *
     * @param task         任务信息
     * @param flowCopyList 抄送人
     */
    public void setCopy(FlowTask task, List<FlowCopyBo> flowCopyList) {
        if (CollUtil.isEmpty(flowCopyList)) {
            return;
        }
        // 添加抄送人记录
        FlowHisTask flowHisTask = flowHisTaskMapper.selectList(new LambdaQueryWrapper<>(FlowHisTask.class).eq(FlowHisTask::getTaskId, task.getId())).get(0);
        FlowNode flowNode = new FlowNode();
        flowNode.setNodeCode(flowHisTask.getTargetNodeCode());
        flowNode.setNodeName(flowHisTask.getTargetNodeName());
        //生成新的任务id
        long taskId = identifierGenerator.nextId(null).longValue();
        task.setId(taskId);
        task.setNodeName("【抄送】" + task.getNodeName());
        Date updateTime = new Date(flowHisTask.getUpdateTime().getTime() - 1000);
        FlowParams flowParams = FlowParams.build();
        flowParams.skipType(SkipType.NONE.getKey());
        flowParams.hisStatus(TaskStatusEnum.COPY.getStatus());
        flowParams.message("【抄送给】" + StreamUtils.join(flowCopyList, FlowCopyBo::getUserName));
        HisTask hisTask = hisTaskService.setSkipHisTask(task, flowNode, flowParams);
        hisTask.setCreateTime(updateTime);
        hisTask.setUpdateTime(updateTime);
        hisTaskService.save(hisTask);
        List<User> userList = flowCopyList.stream()
            .map(flowCopy -> {
                FlowUser flowUser = new FlowUser();
                flowUser.setType(TaskAssigneeType.COPY.getCode());
                flowUser.setProcessedBy(String.valueOf(flowCopy.getUserId()));
                flowUser.setAssociated(taskId);
                return flowUser;
            }).collect(Collectors.toList());
        // 批量保存抄送人员
        flwCommonService.getFlowUserService().saveBatch(userList);
    }

    /**
     * 查询当前用户的待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowTaskVo> pageByTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = buildQueryWrapper(flowTaskBo);
        queryWrapper.eq("t.node_type", NodeType.BETWEEN.getKey());
        queryWrapper.in("t.processed_by", SpringUtils.getBean(WorkflowPermissionHandler.class).permissions());
        queryWrapper.in("t.flow_status", BusinessStatusEnum.WAITING.getStatus());
        Page<FlowTaskVo> page = this.getFlowTaskVoPage(pageQuery, queryWrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 查询当前用户的已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowHisTaskVo> pageByTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = buildQueryWrapper(flowTaskBo);
        queryWrapper.eq("t.node_type", NodeType.BETWEEN.getKey());
        queryWrapper.in("t.approver", LoginHelper.getUserIdStr());
        queryWrapper.orderByDesc("t.create_time").orderByDesc("t.update_time");
        Page<FlowHisTaskVo> page = flwTaskMapper.getListFinishTask(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 查询待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowTaskVo> pageByAllTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = buildQueryWrapper(flowTaskBo);
        queryWrapper.eq("t.node_type", NodeType.BETWEEN.getKey());
        Page<FlowTaskVo> page = getFlowTaskVoPage(pageQuery, queryWrapper);
        return TableDataInfo.build(page);
    }

    private Page<FlowTaskVo> getFlowTaskVoPage(PageQuery pageQuery, QueryWrapper<FlowTaskBo> queryWrapper) {
        Page<FlowTaskVo> page = flwTaskMapper.getListRunTask(pageQuery.build(), queryWrapper);
        List<FlowTaskVo> records = page.getRecords();
        if (CollUtil.isNotEmpty(records)) {
            List<Long> taskIds = StreamUtils.toList(records, FlowTaskVo::getId);
            Map<Long, List<UserDTO>> listMap = currentTaskAllUser(taskIds);
            records.forEach(t -> {
                List<UserDTO> userList = listMap.getOrDefault(t.getId(), Collections.emptyList());
                if (CollUtil.isNotEmpty(userList)) {
                    t.setAssigneeIds(StreamUtils.join(userList, e -> String.valueOf(e.getUserId())));
                    t.setAssigneeNames(StreamUtils.join(userList, UserDTO::getNickName));
                }
            });
        }
        return page;
    }

    /**
     * 查询已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowHisTaskVo> pageByAllTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = buildQueryWrapper(flowTaskBo);
        Page<FlowHisTaskVo> page = flwTaskMapper.getListFinishTask(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 查询当前用户的抄送
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowTaskVo> pageByTaskCopy(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        QueryWrapper<FlowTaskBo> queryWrapper = buildQueryWrapper(flowTaskBo);
        queryWrapper.in("t.processed_by", LoginHelper.getUserIdStr());
        Page<FlowTaskVo> page = flwTaskMapper.getTaskCopyByPage(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    private QueryWrapper<FlowTaskBo> buildQueryWrapper(FlowTaskBo flowTaskBo) {
        QueryWrapper<FlowTaskBo> wrapper = Wrappers.query();
        wrapper.like(StringUtils.isNotBlank(flowTaskBo.getNodeName()), "t.node_name", flowTaskBo.getNodeName());
        wrapper.like(StringUtils.isNotBlank(flowTaskBo.getFlowName()), "t.flow_name", flowTaskBo.getFlowName());
        wrapper.like(StringUtils.isNotBlank(flowTaskBo.getFlowCode()), "t.flow_code", flowTaskBo.getFlowCode());
        wrapper.in(CollUtil.isNotEmpty(flowTaskBo.getCreateByIds()), "t.create_by", flowTaskBo.getCreateByIds());
        if (StringUtils.isNotBlank(flowTaskBo.getCategory())) {
            List<Long> categoryIds = flwCategoryMapper.selectCategoryIdsByParentId(Convert.toLong(flowTaskBo.getCategory()));
            wrapper.in("t.category", StreamUtils.toList(categoryIds, Convert::toStr));
        }
        wrapper.orderByDesc("t.create_time");
        return wrapper;
    }

    /**
     * 驳回任务
     *
     * @param bo 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean backProcess(BackProcessBo bo) {
        try {
            Long taskId = bo.getTaskId();
            String notice = bo.getNotice();
            List<String> messageType = bo.getMessageType();
            String message = bo.getMessage();
            FlowTask task = flowTaskMapper.selectById(taskId);
            if (ObjectUtil.isNull(task)) {
                throw new ServiceException("任务不存在！");
            }
            Instance inst = insService.getById(task.getInstanceId());
            BusinessStatusEnum.checkBackStatus(inst.getFlowStatus());
            Long definitionId = task.getDefinitionId();
            Definition definition = defService.getById(definitionId);
            String applyNodeCode = flwCommonService.applyNodeCode(definitionId);
            FlowParams flowParams = FlowParams.build();
            flowParams.nodeCode(bo.getNodeCode());
            flowParams.message(message);
            flowParams.skipType(SkipType.REJECT.getKey());
            flowParams.flowStatus(applyNodeCode.equals(bo.getNodeCode()) ? TaskStatusEnum.BACK.getStatus() : TaskStatusEnum.WAITING.getStatus())
                .hisStatus(TaskStatusEnum.BACK.getStatus());
            flowParams.hisTaskExt(bo.getFileId());
            taskService.skip(task.getId(), flowParams);

            Instance instance = insService.getById(inst.getId());
            this.setHandler(instance, task, null);
            // 消息通知
            flwCommonService.sendMessage(definition.getFlowName(), instance.getId(), messageType, notice);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 获取可驳回的前置节点
     *
     * @param definitionId 流程定义id
     * @param nowNodeCode  当前节点
     */
    @Override
    public List<Node> getBackTaskNode(Long definitionId, String nowNodeCode) {
        List<Node> nodeCodes = nodeService.getByNodeCodes(Collections.singletonList(nowNodeCode), definitionId);
        if (!CollUtil.isNotEmpty(nodeCodes)) {
            return nodeCodes;
        }
        //判断是否配置了固定驳回节点
        Node node = nodeCodes.get(0);
        if (StringUtils.isNotBlank(node.getAnyNodeSkip())) {
            return nodeService.getByNodeCodes(Collections.singletonList(node.getAnyNodeSkip()), definitionId);
        }
        //获取可驳回的前置节点
        List<Node> nodes = nodeService.previousNodeList(definitionId, nowNodeCode);
        if (CollUtil.isNotEmpty(nodes)) {
            return StreamUtils.filter(nodes, e -> NodeType.BETWEEN.getKey().equals(e.getNodeType()));
        }
        return nodes;
    }

    /**
     * 终止任务
     *
     * @param bo 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean terminationTask(FlowTerminationBo bo) {
        try {
            Long taskId = bo.getTaskId();
            Task task = taskService.getById(taskId);
            if (task == null) {
                throw new ServiceException("任务不存在！");
            }
            Instance instance = insService.getById(task.getInstanceId());
            if (ObjectUtil.isNotNull(instance)) {
                BusinessStatusEnum.checkInvalidStatus(instance.getFlowStatus());
            }
            FlowParams flowParams = new FlowParams();
            flowParams.message(bo.getComment());
            flowParams.flowStatus(BusinessStatusEnum.TERMINATION.getStatus())
                .hisStatus(TaskStatusEnum.TERMINATION.getStatus());
            taskService.termination(taskId, flowParams);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 按照任务id查询任务
     *
     * @param taskIdList 任务id
     */
    @Override
    public List<FlowTask> selectByIdList(List<Long> taskIdList) {
        return flowTaskMapper.selectList(new LambdaQueryWrapper<>(FlowTask.class)
            .in(FlowTask::getId, taskIdList));
    }

    /**
     * 按照任务id查询任务
     *
     * @param taskId 任务id
     */
    @Override
    public FlowTaskVo selectById(Long taskId) {
        Task task = taskService.getById(taskId);
        if (ObjectUtil.isNull(task)) {
            return null;
        }
        FlowTaskVo flowTaskVo = BeanUtil.toBean(task, FlowTaskVo.class);
        Instance instance = insService.getById(task.getInstanceId());
        Definition definition = defService.getById(task.getDefinitionId());
        flowTaskVo.setFlowStatus(instance.getFlowStatus());
        flowTaskVo.setVersion(definition.getVersion());
        flowTaskVo.setFlowCode(definition.getFlowCode());
        flowTaskVo.setFlowName(definition.getFlowName());
        flowTaskVo.setBusinessId(instance.getBusinessId());
        //设置按钮权限
        FlowNode flowNode = getByNodeCode(flowTaskVo.getNodeCode(), instance.getDefinitionId());
        if (ObjectUtil.isNull(flowNode)) {
            throw new NullPointerException("当前【" + flowTaskVo.getNodeCode() + "】节点编码不存在");
        }
        flowTaskVo.setButtonList(flowTaskVo.getButtonList(flowNode.getExt()));
        flowTaskVo.setNodeRatio(flowNode.getNodeRatio());
        flowTaskVo.setApplyNode(flowNode.getNodeCode().equals(flwCommonService.applyNodeCode(task.getDefinitionId())));
        return flowTaskVo;
    }

    /**
     * 获取下一节点信息
     *
     * @param bo 参数
     */
    @Override
    public List<FlowNode> getNextNodeList(FlowNextNodeBo bo) {
        Long taskId = bo.getTaskId();
        Map<String, Object> variables = bo.getVariables();
        Task task = taskService.getById(taskId);
        Instance instance = insService.getById(task.getInstanceId());
        Definition definition = defService.getById(task.getDefinitionId());
        Map<String, Object> mergeVariable = MapUtil.mergeAll(instance.getVariableMap(), variables);
        //获取下一节点列表
        List<Node> nextNodeList = nodeService.getNextNodeList(task.getDefinitionId(), task.getNodeCode(), null, SkipType.PASS.getKey(), mergeVariable);
        List<FlowNode> nextFlowNodes = BeanUtil.copyToList(nextNodeList, FlowNode.class);
        if (CollUtil.isNotEmpty(nextNodeList)) {
            //构建以下节点数据
            List<Task> buildNextTaskList = StreamUtils.toList(nextNodeList, node -> taskService.addTask(node, instance, definition, null));
            //办理人变量替换
            ExpressionUtil.evalVariable(buildNextTaskList, mergeVariable);
            for (FlowNode flowNode : nextFlowNodes) {
                buildNextTaskList.stream().filter(t -> t.getNodeCode().equals(flowNode.getNodeCode())).findFirst().ifPresent(t -> {
                    if (CollUtil.isNotEmpty(t.getPermissionList())) {
                        List<UserDTO> users = flwTaskAssigneeService.fetchUsersByStorageId(String.join(StringUtils.SEPARATOR, t.getPermissionList()));
                        if (CollUtil.isNotEmpty(users)) {
                            flowNode.setPermissionFlag(StreamUtils.join(users, e -> String.valueOf(e.getUserId())));
                        }
                    }
                });
            }
        }
        return nextFlowNodes;
    }

    /**
     * 按照任务id查询任务
     *
     * @param taskIdList 任务id
     * @return 结果
     */
    @Override
    public List<FlowHisTask> selectHisTaskByIdList(List<Long> taskIdList) {
        return flowHisTaskMapper.selectList(new LambdaQueryWrapper<>(FlowHisTask.class)
            .in(FlowHisTask::getId, taskIdList));
    }

    /**
     * 按照任务id查询任务
     *
     * @param taskId 任务id
     * @return 结果
     */
    @Override
    public FlowHisTask selectHisTaskById(Long taskId) {
        return flowHisTaskMapper.selectOne(new LambdaQueryWrapper<>(FlowHisTask.class)
            .eq(FlowHisTask::getId, taskId));
    }

    /**
     * 按照实例id查询任务
     *
     * @param instanceIdList 流程实例id
     */
    @Override
    public List<FlowTask> selectByInstIdList(List<Long> instanceIdList) {
        return flowTaskMapper.selectList(new LambdaQueryWrapper<>(FlowTask.class)
            .in(FlowTask::getInstanceId, instanceIdList));
    }

    /**
     * 按照实例id查询任务
     *
     * @param instanceId 流程实例id
     */
    @Override
    public List<FlowTask> selectByInstId(Long instanceId) {
        return flowTaskMapper.selectList(new LambdaQueryWrapper<>(FlowTask.class)
            .eq(FlowTask::getInstanceId, instanceId));
    }

    /**
     * 任务操作
     *
     * @param bo            参数
     * @param taskOperation 操作类型，委派 delegateTask、转办 transferTask、加签 addSignature、减签 reductionSignature
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean taskOperation(TaskOperationBo bo, String taskOperation) {
        FlowParams flowParams = new FlowParams();
        flowParams.message(bo.getMessage());
        if (LoginHelper.isSuperAdmin() || LoginHelper.isTenantAdmin()) {
            flowParams.ignore(true);
        }

        // 根据操作类型构建 FlowParams
        switch (taskOperation) {
            case DELEGATE_TASK, TRANSFER_TASK -> {
                ValidatorUtils.validate(bo, AddGroup.class);
                flowParams.addHandlers(Collections.singletonList(bo.getUserId()));
            }
            case ADD_SIGNATURE -> {
                ValidatorUtils.validate(bo, EditGroup.class);
                flowParams.addHandlers(bo.getUserIds());
            }
            case REDUCTION_SIGNATURE -> {
                ValidatorUtils.validate(bo, EditGroup.class);
                flowParams.reductionHandlers(bo.getUserIds());
            }
            default -> {
                log.error("Invalid operation type:{} ", taskOperation);
                throw new ServiceException("Invalid operation type " + taskOperation);
            }
        }

        Long taskId = bo.getTaskId();
        Task task = taskService.getById(taskId);
        FlowNode flowNode = getByNodeCode(task.getNodeCode(), task.getDefinitionId());
        if ("addSignature".equals(taskOperation) || "reductionSignature".equals(taskOperation)) {
            if (flowNode.getNodeRatio().compareTo(BigDecimal.ZERO) == 0) {
                throw new ServiceException(task.getNodeName() + "不是会签节点！");
            }
        }
        // 设置任务状态并执行对应的任务操作
        switch (taskOperation) {
            //委派任务
            case DELEGATE_TASK -> {
                flowParams.hisStatus(TaskStatusEnum.DEPUTE.getStatus());
                return taskService.depute(taskId, flowParams);
            }
            //转办任务
            case TRANSFER_TASK -> {
                flowParams.hisStatus(TaskStatusEnum.TRANSFER.getStatus());
                return taskService.transfer(taskId, flowParams);
            }
            //加签，增加办理人
            case ADD_SIGNATURE -> {
                flowParams.hisStatus(TaskStatusEnum.SIGN.getStatus());
                return taskService.addSignature(taskId, flowParams);
            }
            //减签，减少办理人
            case REDUCTION_SIGNATURE -> {
                flowParams.hisStatus(TaskStatusEnum.SIGN_OFF.getStatus());
                return taskService.reductionSignature(taskId, flowParams);
            }
            default -> {
                log.error("Invalid operation type:{} ", taskOperation);
                throw new ServiceException("Invalid operation type " + taskOperation);
            }
        }
    }

    /**
     * 修改任务办理人（此方法将会批量修改所有任务的办理人）
     *
     * @param taskIdList 任务id
     * @param userId     用户id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAssignee(List<Long> taskIdList, String userId) {
        if (CollUtil.isEmpty(taskIdList)) {
            return false;
        }
        try {
            List<FlowTask> flowTasks = this.selectByIdList(taskIdList);
            // 批量删除现有任务的办理人记录
            if (CollUtil.isNotEmpty(flowTasks)) {
                flwCommonService.getFlowUserService().deleteByTaskIds(StreamUtils.toList(flowTasks, FlowTask::getId));
                List<User> userList = flowTasks.stream()
                    .map(flowTask -> {
                        FlowUser flowUser = new FlowUser();
                        flowUser.setType(TaskAssigneeType.APPROVER.getCode());
                        flowUser.setProcessedBy(userId);
                        flowUser.setAssociated(flowTask.getId());
                        return flowUser;
                    })
                    .collect(Collectors.toList());
                if (CollUtil.isNotEmpty(userList)) {
                    flwCommonService.getFlowUserService().saveBatch(userList);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
        return true;
    }

    /**
     * 获取任务所有办理人
     *
     * @param taskIdList 任务id
     */
    @Override
    public Map<Long, List<UserDTO>> currentTaskAllUser(List<Long> taskIdList) {
        Map<Long, List<UserDTO>> map = new HashMap<>();
        // 获取与当前任务关联的用户列表
        List<User> associatedUsers = flwCommonService.getFlowUserService().getByAssociateds(taskIdList);
        Map<Long, List<User>> listMap = StreamUtils.groupByKey(associatedUsers, User::getAssociated);
        for (Map.Entry<Long, List<User>> entry : listMap.entrySet()) {
            List<User> value = entry.getValue();
            if (CollUtil.isNotEmpty(value)) {
                List<UserDTO> userDTOS = userService.selectListByIds(StreamUtils.toList(value, e -> Long.valueOf(e.getProcessedBy())));
                map.put(entry.getKey(), userDTOS);
            }
        }
        return map;
    }

    /**
     * 获取当前任务的所有办理人
     *
     * @param taskId 任务id
     */
    @Override
    public List<UserDTO> currentTaskAllUser(Long taskId) {
        // 获取与当前任务关联的用户列表
        List<User> userList = flwCommonService.getFlowUserService().getByAssociateds(Collections.singletonList(taskId));
        if (CollUtil.isEmpty(userList)) {
            return Collections.emptyList();
        }
        return userService.selectListByIds(StreamUtils.toList(userList, e -> Long.valueOf(e.getProcessedBy())));
    }

    /**
     * 按照节点编码查询节点
     *
     * @param nodeCode     节点编码
     * @param definitionId 流程定义id
     */
    @Override
    public FlowNode getByNodeCode(String nodeCode, Long definitionId) {
        return flowNodeMapper.selectOne(new LambdaQueryWrapper<FlowNode>()
            .eq(FlowNode::getNodeCode, nodeCode)
            .eq(FlowNode::getDefinitionId, definitionId));
    }

}
