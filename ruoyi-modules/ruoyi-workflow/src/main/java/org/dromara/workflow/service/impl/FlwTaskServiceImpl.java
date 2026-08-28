package org.dromara.workflow.service.impl;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.liteflow.utils.LiteFlowUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.common.mybatis.utils.IdGeneratorUtil;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.api.UserService;
import org.dromara.system.api.domain.UserDTO;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.constant.ExceptionCons;
import org.dromara.warm.flow.core.dto.FlowCombine;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.*;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.core.enums.UserType;
import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.warm.flow.core.service.*;
import org.dromara.warm.flow.core.utils.ExpressionUtil;
import org.dromara.warm.flow.core.utils.MapUtil;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowNode;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.dromara.warm.flow.orm.mapper.FlowNodeMapper;
import org.dromara.warm.flow.orm.mapper.FlowTaskMapper;
import org.dromara.workflow.api.domain.StartProcessReturnDTO;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.enums.TaskAssigneeType;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.domain.bo.*;
import org.dromara.workflow.domain.context.CompleteTaskContext;
import org.dromara.workflow.domain.context.StartProcessContext;
import org.dromara.workflow.domain.context.TaskOperationContext;
import org.dromara.workflow.domain.vo.FlowCopyVo;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.dromara.workflow.domain.vo.FlowTaskVo;
import org.dromara.workflow.domain.vo.NodeExtVo;
import org.dromara.workflow.mapper.FlwCategoryMapper;
import org.dromara.workflow.mapper.FlwHisTaskMapper;
import org.dromara.workflow.mapper.FlwTaskMapper;
import org.dromara.workflow.service.IFlwCommonService;
import org.dromara.workflow.service.IFlwNodeExtService;
import org.dromara.workflow.service.IFlwTaskAssigneeService;
import org.dromara.workflow.service.IFlwTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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

    private static final String START_PROCESS_CHAIN = "startProcessChain";
    private static final String COMPLETE_TASK_CHAIN = "completeTaskChain";
    private static final String TASK_OPERATION_CHAIN = "taskOperationChain";
    private static final Set<String> TASK_READ_ASSIGNEE_TYPES = Set.of(
        TaskAssigneeType.APPROVER.getCode(),
        TaskAssigneeType.TRANSFER.getCode(),
        TaskAssigneeType.DELEGATE.getCode(),
        TaskAssigneeType.COPY.getCode()
    );

    private final TaskService taskService;
    private final InsService insService;
    private final DefService defService;
    private final HisTaskService hisTaskService;
    private final NodeService nodeService;
    private final FlowTaskMapper flowTaskMapper;
    private final FlowHisTaskMapper flowHisTaskMapper;
    private final UserService userService;
    private final FlwTaskMapper flwTaskMapper;
    private final FlwHisTaskMapper flwHisTaskMapper;
    private final FlwCategoryMapper flwCategoryMapper;
    private final FlowNodeMapper flowNodeMapper;
    private final IFlwTaskAssigneeService flwTaskAssigneeService;
    private final IFlwCommonService flwCommonService;
    private final IFlwNodeExtService flwNodeExtService;

    /**
     * 启动任务
     *
     * @param startProcessBo 启动流程参数
     * @return 启动后的流程实例标识与首个任务标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Lock4j(keys = {"#startProcessBo.flowCode + #startProcessBo.businessId"})
    public StartProcessReturnDTO startWorkFlow(StartProcessBo startProcessBo) {
        StartProcessContext context = new StartProcessContext(startProcessBo);
        LiteFlowUtils.execute(START_PROCESS_CHAIN, context);
        return context.getStartProcessReturn();
    }

    /**
     * 办理任务
     *
     * @param completeTaskBo 办理任务参数
     * @return 办理成功返回 {@code true}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @Lock4j(keys = {"#completeTaskBo.taskId"})
    public boolean completeTask(CompleteTaskBo completeTaskBo) {
        // 办理任务的业务编排交给 LiteFlow 链路，当前方法只保留事务、锁和异常透传边界。
        LiteFlowUtils.execute(COMPLETE_TASK_CHAIN, new CompleteTaskContext(completeTaskBo));
        return true;
    }

    /**
     * 添加抄送人
     *
     * @param task         任务信息
     * @param flowCopyList 抄送人
     */
    @Override
    public void setCopy(Task task, List<FlowCopyBo> flowCopyList) {
        if (CollUtil.isEmpty(flowCopyList)) {
            return;
        }
        // 添加抄送人记录
        List<FlowHisTask> flowHisTasks = flowHisTaskMapper.selectList(
            QueryBuilder.lambda(FlowHisTask.class)
                .eq(FlowHisTask::getTaskId, task.getId())
                .build());
        if (CollUtil.isEmpty(flowHisTasks)) {
            throw new ServiceException("流程历史任务不存在，无法添加抄送记录");
        }
        FlowHisTask flowHisTask = flowHisTasks.getFirst();
        FlowNode flowNode = new FlowNode();
        flowNode.setNodeCode(flowHisTask.getTargetNodeCode());
        flowNode.setNodeName(flowHisTask.getTargetNodeName());
        //生成新的任务id
        long taskId = IdGeneratorUtil.nextLongId();
        task.setId(taskId);
        task.setNodeName("【抄送】" + task.getNodeName());
        Date updateTime = new Date(flowHisTask.getUpdateTime().getTime() - 1000);
        FlowParams flowParams = FlowParams.build()
            .skipType(SkipType.NONE.getKey())
            .hisStatus(TaskStatusEnum.COPY.getStatus())
            .message("【抄送给】" + StreamUtils.join(flowCopyList, FlowCopyBo::getNickName));
        HisTask hisTask = hisTaskService.setSkipHisTask(task, flowNode, flowParams);
        hisTask.setCreateTime(updateTime);
        hisTask.setUpdateTime(updateTime);
        hisTaskService.save(hisTask);
        List<User> userList = StreamUtils.toList(flowCopyList, x ->
            new FlowUser()
                .setType(TaskAssigneeType.COPY.getCode())
                .setProcessedBy(Convert.toStr(x.getUserId()))
                .setAssociated(taskId));
        // 批量保存抄送人员
        FlowEngine.userService().saveBatch(userList);
        // 抄送消息进入“我的抄送”，不和待办列表混用。
        flwCommonService.sendMessage(
            List.of(org.dromara.workflow.common.enums.MessageTypeEnum.SYSTEM_MESSAGE.getCode()),
            "您收到一条新的流程抄送，请及时查看。",
            "单据抄送提醒",
            userService.selectListByIds(StreamUtils.toList(flowCopyList, FlowCopyBo::getUserId)),
            PATH_TASK_COPY
        );
    }

    /**
     * 查询当前用户的待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 当前登录人的待办任务分页结果
     */
    @Override
    public PageResult<FlowTaskVo> pageByTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        Page<FlowTaskVo> page = flwTaskMapper.getListRunTask(pageQuery.build(), flowTaskBo, categoryIds(flowTaskBo), LoginHelper.getUserIdStr());
        this.wrapAssigneeInfo(page.getRecords());
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 查询当前用户的已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 当前登录人的已办任务分页结果
     */
    @Override
    public PageResult<FlowHisTaskVo> pageByTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        Page<FlowHisTaskVo> page = flwHisTaskMapper.getListFinishTask(pageQuery.build(), flowTaskBo, categoryIds(flowTaskBo), LoginHelper.getUserIdStr());
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 查询待办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 全部待办任务分页结果
     */
    @Override
    public PageResult<FlowTaskVo> pageByAllTaskWait(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        Page<FlowTaskVo> page = flwTaskMapper.getListRunTask(pageQuery.build(), flowTaskBo, categoryIds(flowTaskBo), null);
        this.wrapAssigneeInfo(page.getRecords());
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 为流程任务列表封装处理人 ID（assigneeIds）
     *
     * @param taskList 流程任务列表
     */
    private void wrapAssigneeInfo(List<FlowTaskVo> taskList) {
        if (CollUtil.isEmpty(taskList)) {
            return;
        }
        List<User> associatedUsers = FlowEngine.userService().getByAssociateds(StreamUtils.toList(taskList, FlowTaskVo::getId));
        Map<Long, List<User>> taskUserMap = StreamUtils.groupByKey(associatedUsers, User::getAssociated);
        // 组装用户数据回任务列表
        for (FlowTaskVo task : taskList) {
            List<User> users = taskUserMap.get(task.getId());
            task.setAssigneeIds(StreamUtils.join(users, User::getProcessedBy));
        }
    }

    /**
     * 查询已办任务
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 全部已办任务分页结果
     */
    @Override
    public PageResult<FlowHisTaskVo> pageByAllTaskFinish(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        Page<FlowHisTaskVo> page = flwHisTaskMapper.getListFinishTask(pageQuery.build(), flowTaskBo, categoryIds(flowTaskBo), null);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 查询当前用户的抄送
     *
     * @param flowTaskBo 参数
     * @param pageQuery  分页
     * @return 当前登录人收到的抄送分页结果
     */
    @Override
    public PageResult<FlowTaskVo> pageByTaskCopy(FlowTaskBo flowTaskBo, PageQuery pageQuery) {
        Page<FlowTaskVo> page = flwTaskMapper.getTaskCopyByPage(pageQuery.build(), flowTaskBo, categoryIds(flowTaskBo), LoginHelper.getUserIdStr());
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 解析查询条件中的分类及其子分类编码集合。
     *
     * @param flowTaskBo 任务筛选条件
     * @return 分类 id 字符串集合，未指定分类时返回 {@code null}
     */
    private List<String> categoryIds(FlowTaskBo flowTaskBo) {
        if (StringUtils.isNotBlank(flowTaskBo.getCategory())) {
            List<Long> categoryIds = flwCategoryMapper.selectCategoryIdsByParentId(Convert.toLong(flowTaskBo.getCategory()));
            return StreamUtils.toList(categoryIds, Convert::toStr);
        }
        return null;
    }

    /**
     * 驳回任务
     *
     * @param bo 参数
     * @return 驳回成功返回 {@code true}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean backProcess(BackProcessBo bo) {
        Long taskId = bo.getTaskId();
        String notice = bo.getNotice();
        List<String> messageType = bo.getMessageType();
        String message = bo.getMessage();
        FlowTask task = flowTaskMapper.selectById(taskId);
        if (ObjectUtil.isNull(task)) {
            throw new ServiceException("任务不存在！");
        }
        Instance inst = insService.getById(task.getInstanceId());
        if (ObjectUtil.isNull(inst)) {
            throw new ServiceException("流程实例不存在");
        }
        BusinessStatusEnum.checkBackStatus(inst.getFlowStatus());
        Long definitionId = task.getDefinitionId();
        String applyNodeCode = flwCommonService.applyNodeCode(definitionId);

        Map<String, Object> variable = new HashMap<>();
        variable.put(MESSAGE_TYPE, messageType);
        variable.put(MESSAGE_NOTICE, notice);

        FlowParams flowParams = FlowParams.build()
            .nodeCode(bo.getNodeCode())
            .variable(variable)
            .message(message)
            .skipType(SkipType.REJECT.getKey())
            .flowStatus(applyNodeCode.equals(bo.getNodeCode()) ? TaskStatusEnum.BACK.getStatus() : TaskStatusEnum.WAITING.getStatus())
            .hisStatus(TaskStatusEnum.BACK.getStatus())
            .hisTaskExt(bo.getFileId());
        taskService.skip(task.getId(), flowParams);
        return true;
    }

    /**
     * 获取可驳回的前置节点
     *
     * @param taskId      任务id
     * @param nowNodeCode 当前节点
     * @return 当前任务允许驳回的节点列表
     */
    @Override
    public List<Node> getBackTaskNode(Long taskId, String nowNodeCode) {
        FlowTask task = flowTaskMapper.selectById(taskId);
        if (ObjectUtil.isNull(task)) {
            throw new ServiceException("任务不存在！");
        }
        List<Node> nodeCodes = nodeService.getByNodeCodes(Collections.singletonList(nowNodeCode), task.getDefinitionId());
        if (!CollUtil.isNotEmpty(nodeCodes)) {
            return nodeCodes;
        }
        List<User> userList = FlowEngine.userService()
            .getByAssociateds(Collections.singletonList(task.getId()), UserType.DEPUTE.getKey());
        if (CollUtil.isNotEmpty(userList)) {
            return nodeCodes;
        }
        //判断是否配置了固定驳回节点
        Node node = nodeCodes.getFirst();
        if (StringUtils.isNotBlank(node.getAnyNodeSkip())) {
            return nodeService.getByNodeCodes(Collections.singletonList(node.getAnyNodeSkip()), task.getDefinitionId());
        }
        //获取可驳回的前置节点
        Long definitionId = task.getDefinitionId();
        FlowCombine flowCombine = defService.getFlowCombineNoDef(definitionId);
        Map<String, Node> nodeMap = getPreviousNodeMap(nowNodeCode, flowCombine);
        List<HisTask> hisTaskList = hisTaskService.getByInsId(task.getInstanceId());

        Set<String> reachableNodeCodes = new HashSet<>();
        if (CollUtil.isNotEmpty(hisTaskList)) {
            Instance instance = insService.getById(task.getInstanceId());
            if (ObjectUtil.isNull(instance)) {
                throw new ServiceException("流程实例不存在");
            }
            collectReachableNodeCodes(reachableNodeCodes, instance, flowCombine);
            if (!reachableNodeCodes.contains(nowNodeCode)) {
                reachableNodeCodes.clear();
            }
        }

        Set<String> added = new HashSet<>();
        List<Node> backNodeList = new ArrayList<>();
        for (HisTask hisTask : hisTaskList) {
            Node nodeValue = nodeMap.get(hisTask.getNodeCode());
            if (nodeValue != null
                && NodeType.BETWEEN.getKey().equals(nodeValue.getNodeType())
                && (CollUtil.isEmpty(reachableNodeCodes) || reachableNodeCodes.contains(nodeValue.getNodeCode()))
                && added.add(nodeValue.getNodeCode())) {
                backNodeList.add(nodeValue);
            }
        }
        if (CollUtil.isNotEmpty(backNodeList)) {
            Collections.reverse(backNodeList);
            return backNodeList;
        }
        if (CollUtil.isNotEmpty(hisTaskList)) {
            return Collections.emptyList();
        }
        return StreamUtils.filter(nodeMap.values(), e -> NodeType.BETWEEN.getKey().equals(e.getNodeType()));
    }

    private Map<String, Node> getPreviousNodeMap(String nodeCode, FlowCombine flowCombine) {
        if (ObjectUtil.isNull(flowCombine) || CollUtil.isEmpty(flowCombine.getAllNodes()) || CollUtil.isEmpty(flowCombine.getAllSkips())) {
            return Collections.emptyMap();
        }
        Map<String, Node> allNodeMap = StreamUtils.toIdentityMap(flowCombine.getAllNodes(), Node::getNodeCode);
        Map<String, List<Skip>> previousSkipMap = new HashMap<>();
        for (Skip skip : flowCombine.getAllSkips()) {
            previousSkipMap.computeIfAbsent(skip.getNextNodeCode(), k -> new ArrayList<>()).add(skip);
        }

        Map<String, Node> previousNodeMap = new LinkedHashMap<>();
        Set<String> visitedNodeCodes = new HashSet<>();
        Deque<String> nodeQueue = new ArrayDeque<>();
        visitedNodeCodes.add(nodeCode);
        nodeQueue.add(nodeCode);
        while (CollUtil.isNotEmpty(nodeQueue)) {
            String currentNodeCode = nodeQueue.poll();
            List<Skip> previousSkips = previousSkipMap.get(currentNodeCode);
            if (CollUtil.isEmpty(previousSkips)) {
                continue;
            }
            for (Skip previousSkip : previousSkips) {
                String previousNodeCode = previousSkip.getNowNodeCode();
                if (!visitedNodeCodes.add(previousNodeCode)) {
                    continue;
                }
                Node previousNode = allNodeMap.get(previousNodeCode);
                if (ObjectUtil.isNull(previousNode)) {
                    continue;
                }
                previousNodeMap.put(previousNodeCode, previousNode);
                nodeQueue.add(previousNodeCode);
            }
        }
        return previousNodeMap;
    }

    private void collectReachableNodeCodes(Set<String> nodeCodes, Instance instance, FlowCombine flowCombine) {
        if (ObjectUtil.isNull(flowCombine) || CollUtil.isEmpty(flowCombine.getAllNodes())) {
            return;
        }
        Deque<Node> nodeQueue = new ArrayDeque<>();
        flowCombine.getAllNodes().stream()
            .filter(e -> NodeType.START.getKey().equals(e.getNodeType()))
            .findFirst()
            .ifPresent(nodeQueue::add);

        while (CollUtil.isNotEmpty(nodeQueue)) {
            Node currentNode = nodeQueue.poll();
            if (ObjectUtil.isNull(currentNode) || !nodeCodes.add(currentNode.getNodeCode())) {
                continue;
            }

            try {
                List<Node> nextNodes = nodeService.getNextNodeList(currentNode, null, SkipType.PASS.getKey(),
                    instance.getVariableMap(), null, flowCombine);
                if (CollUtil.isNotEmpty(nextNodes)) {
                    nodeQueue.addAll(nextNodes);
                }
            } catch (FlowException e) {
                // 条件变量缺失时跳过当前分支，其他引擎异常继续抛出。
                if (!StringUtils.containsAny(e.getMessage(), ExceptionCons.NULL_CONDITION_VALUE, ExceptionCons.NULL_SKIP_TYPE)) {
                    throw e;
                }
            }
        }
    }

    /**
     * 终止任务
     *
     * @param bo 参数
     * @return 终止成功返回 {@code true}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean terminationTask(FlowTerminationBo bo) {
        Long taskId = bo.taskId();
        Task task = taskService.getById(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在！");
        }
        Instance instance = insService.getById(task.getInstanceId());
        if (ObjectUtil.isNotNull(instance)) {
            BusinessStatusEnum.checkInvalidStatus(instance.getFlowStatus());
        }
        FlowParams flowParams = FlowParams.build()
            .message(bo.comment())
            .flowStatus(BusinessStatusEnum.TERMINATION.getStatus())
            .hisStatus(TaskStatusEnum.TERMINATION.getStatus());
        taskService.termination(taskId, flowParams);
        return true;
    }

    /**
     * 按照任务id查询任务
     *
     * @param taskIdList 任务id
     * @return 任务列表
     */
    @Override
    public List<FlowTask> selectByIdList(Collection<Long> taskIdList) {
        return flowTaskMapper.selectList(QueryBuilder.lambda(FlowTask.class)
            .in(FlowTask::getId, taskIdList)
            .build());
    }

    /**
     * 按照任务id查询任务
     *
     * @param taskId 任务id
     * @return 任务详情视图，不存在时返回 {@code null}
     */
    @Override
    public FlowTaskVo selectById(Long taskId) {
        Task task = taskService.getById(taskId);
        if (ObjectUtil.isNull(task)) {
            return null;
        }
        checkTaskReadAccess(task);
        FlowTaskVo flowTaskVo = BeanUtil.toBean(task, FlowTaskVo.class);
        Instance instance = insService.getById(task.getInstanceId());
        if (ObjectUtil.isNull(instance)) {
            throw new ServiceException("流程实例不存在");
        }
        Definition definition = defService.getById(task.getDefinitionId());
        if (ObjectUtil.isNull(definition)) {
            throw new ServiceException("流程定义不存在");
        }
        flowTaskVo.setFlowStatus(instance.getFlowStatus());
        flowTaskVo.setVersion(definition.getVersion());
        flowTaskVo.setFlowCode(definition.getFlowCode());
        flowTaskVo.setFlowName(definition.getFlowName());
        flowTaskVo.setBusinessId(instance.getBusinessId());
        FlowNode flowNode = this.getByNodeCode(flowTaskVo.getNodeCode(), instance.getDefinitionId());
        if (ObjectUtil.isNull(flowNode)) {
            throw new NullPointerException("当前【" + flowTaskVo.getNodeCode() + "】节点编码不存在");
        }
        NodeExtVo nodeExtVo = flwNodeExtService.parseNodeExt(flowNode.getExt(), instance.getVariableMap());
        //设置按钮权限
        if (CollUtil.isNotEmpty(nodeExtVo.getButtonPermissions())) {
            flowTaskVo.setButtonList(nodeExtVo.getButtonPermissions());
        } else {
            flowTaskVo.setButtonList(new ArrayList<>());
        }
        if (CollUtil.isNotEmpty(nodeExtVo.getCopySettings())) {
            List<FlowCopyVo> list = StreamUtils.toList(nodeExtVo.getCopySettings(), x -> new FlowCopyVo(Convert.toLong(x)));
            flowTaskVo.setCopyList(list);
        } else {
            flowTaskVo.setCopyList(new ArrayList<>());
        }
        if (CollUtil.isNotEmpty(nodeExtVo.getVariables())) {
            flowTaskVo.setVarList(nodeExtVo.getVariables());
        } else {
            flowTaskVo.setVarList(new HashMap<>());
        }
        flowTaskVo.setNodeRatio(flowNode.getNodeRatio());
        flowTaskVo.setApplyNode(flowNode.getNodeCode().equals(flwCommonService.applyNodeCode(task.getDefinitionId())));
        return flowTaskVo;
    }

    /**
     * 获取下一节点信息
     *
     * @param bo 参数
     * @return 当前任务在给定变量下可流转到的下一审批节点列表
     */
    @Override
    public List<FlowNode> getNextNodeList(FlowNextNodeBo bo) {
        Long taskId = bo.getTaskId();
        Map<String, Object> variables = bo.getVariables();
        Task task = taskService.getById(taskId);
        if (ObjectUtil.isNull(task)) {
            throw new ServiceException("任务不存在！");
        }
        Instance instance = insService.getById(task.getInstanceId());
        if (ObjectUtil.isNull(instance)) {
            throw new ServiceException("流程实例不存在");
        }
        Definition definition = defService.getById(task.getDefinitionId());
        if (ObjectUtil.isNull(definition)) {
            throw new ServiceException("流程定义不存在");
        }
        Map<String, Object> mergeVariable = MapUtil.mergeAll(instance.getVariableMap(), variables);
        // 获取下一节点列表
        List<Node> nextNodeList = nodeService.getNextNodeList(task.getDefinitionId(), task.getNodeCode(), null, SkipType.PASS.getKey(), mergeVariable);
        if (CollUtil.isEmpty(nextNodeList)) {
            return new ArrayList<>();
        }
        List<FlowNode> nextFlowNodes = BeanUtil.copyToList(nextNodeList, FlowNode.class);
        // 只获取中间节点
        nextFlowNodes = StreamUtils.filter(nextFlowNodes, node -> NodeType.BETWEEN.getKey().equals(node.getNodeType()));
        if (CollUtil.isNotEmpty(nextNodeList)) {
            //构建以下节点数据
            List<Task> buildNextTaskList = StreamUtils.toList(nextNodeList, node -> taskService.addTask(node, instance, definition, FlowParams.build()));
            //办理人变量替换
            ExpressionUtil.evalVariable(buildNextTaskList, FlowParams.build().variable(mergeVariable));
            for (FlowNode flowNode : nextFlowNodes) {
                StreamUtils.findFirst(buildNextTaskList, t -> t.getNodeCode().equals(flowNode.getNodeCode()))
                    .ifPresent(first -> {
                        List<UserDTO> users;
                        if (CollUtil.isNotEmpty(first.getPermissionList())
                            && CollUtil.isNotEmpty(users = flwTaskAssigneeService.fetchUsersByStorageIds(StringUtils.joinComma(first.getPermissionList())))) {
                            flowNode.setPermissionFlag(StreamUtils.join(users, e -> Convert.toStr(e.getUserId())));
                        }
                    });
            }
        }
        return nextFlowNodes;
    }

    /**
     * 按照任务id查询任务
     *
     * @param taskId 任务id
     * @return 结果
     */
    @Override
    public FlowHisTask selectHisTaskById(Long taskId) {
        return flowHisTaskMapper.selectOne(QueryBuilder.lambda(FlowHisTask.class)
            .eq(FlowHisTask::getId, taskId)
            .build());
    }

    /**
     * 按照实例id查询任务
     *
     * @param instanceId 流程实例id
     * @return 运行中的任务列表
     */
    @Override
    public List<FlowTask> selectByInstId(Long instanceId) {
        return flowTaskMapper.selectList(QueryBuilder.lambda(FlowTask.class)
            .eq(FlowTask::getInstanceId, instanceId)
            .build());
    }

    /**
     * 按照实例id查询任务
     *
     * @param instanceIds 流程实例id
     * @return 运行中的任务列表
     */
    @Override
    public List<FlowTask> selectByInstIds(Collection<Long> instanceIds) {
        return flowTaskMapper.selectList(QueryBuilder.lambda(FlowTask.class)
            .in(FlowTask::getInstanceId, instanceIds)
            .build());
    }

    /**
     * 判断流程是否已结束（即该流程实例下是否还有未完成的任务）
     *
     * @param instanceId 流程实例ID
     * @return true 表示任务已全部结束；false 表示仍有任务存在
     */
    @Override
    public boolean isTaskEnd(Long instanceId) {
        boolean exists = flowTaskMapper.exists(QueryBuilder.lambda(FlowTask.class)
            .eq(FlowTask::getInstanceId, instanceId)
            .build());
        return !exists;
    }

    /**
     * 任务操作
     *
     * @param bo            参数
     * @param taskOperation 操作类型，委派 delegateTask、转办 transferTask、加签 addSignature、减签 reductionSignature
     * @return 操作成功返回 {@code true}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean taskOperation(TaskOperationBo bo, String taskOperation) {
        TaskOperationContext context = new TaskOperationContext(bo, taskOperation);
        LiteFlowUtils.execute(TASK_OPERATION_CHAIN, context);
        return context.isResult();
    }

    /**
     * 修改任务办理人（此方法将会批量修改所有任务的办理人）
     *
     * @param taskIdList 任务id
     * @param userId     用户id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAssignee(Collection<Long> taskIdList, String userId) {
        if (CollUtil.isEmpty(taskIdList)) {
            return false;
        }
        List<FlowTask> flowTasks = this.selectByIdList(taskIdList);
        // 批量删除现有任务的办理人记录
        if (CollUtil.isNotEmpty(flowTasks)) {
            FlowEngine.userService().deleteByTaskIds(StreamUtils.toList(flowTasks, FlowTask::getId));
            List<User> userList = StreamUtils.toList(flowTasks, flowTask ->
                new FlowUser()
                    .setType(TaskAssigneeType.APPROVER.getCode())
                    .setProcessedBy(userId)
                    .setAssociated(flowTask.getId()));
            if (CollUtil.isNotEmpty(userList)) {
                FlowEngine.userService().saveBatch(userList);
            }
        }
        return true;
    }

    /**
     * 获取当前任务的所有办理人
     *
     * @param taskIds 任务id
     */
    @Override
    public List<UserDTO> currentTaskAllUser(List<Long> taskIds) {
        if (CollUtil.isEmpty(taskIds)) {
            return Collections.emptyList();
        }
        List<Long> validTaskIds = new ArrayList<>();
        for (Long taskId : taskIds) {
            if (ObjectUtil.isNull(taskId)) {
                continue;
            }
            Task task = taskService.getById(taskId);
            if (ObjectUtil.isNotNull(task)) {
                checkTaskReadAccess(task);
                validTaskIds.add(taskId);
            }
        }
        if (CollUtil.isEmpty(validTaskIds)) {
            return Collections.emptyList();
        }
        // 获取与当前任务关联的用户列表
        List<User> userList = FlowEngine.userService().getByAssociateds(validTaskIds);
        if (CollUtil.isEmpty(userList)) {
            return Collections.emptyList();
        }
        return userService.selectListByIds(StreamUtils.toSet(userList, e -> Convert.toLong(e.getProcessedBy())));
    }

    /**
     * 校验当前登录用户是否可以读取任务信息。
     *
     * <p>任务读取接口同时服务于普通审批和流程监控，不能只依赖管理菜单权限。
     * 普通用户必须是任务办理关系人、流程发起人或抄送接收人；监控/管理角色
     * 通过任务列表或编辑权限进入管理读取范围。</p>
     *
     * @param task 当前任务
     */
    private void checkTaskReadAccess(Task task) {
        if (LoginHelper.isSuperAdmin()
            || StpUtil.hasPermission("workflow:task:list")
            || StpUtil.hasPermission("workflow:task:edit")) {
            return;
        }
        String userId = LoginHelper.getUserIdStr();
        List<User> associatedUsers = FlowEngine.userService().getByAssociateds(List.of(task.getId()));
        boolean taskAssignee = CollUtil.isNotEmpty(associatedUsers)
            && associatedUsers.stream()
            .anyMatch(user -> Objects.equals(userId, user.getProcessedBy())
                && TASK_READ_ASSIGNEE_TYPES.contains(Convert.toStr(user.getType())));
        if (taskAssignee) {
            return;
        }
        Instance instance = insService.getById(task.getInstanceId());
        if (ObjectUtil.isNotNull(instance) && Objects.equals(instance.getCreateBy(), userId)) {
            return;
        }
        throw new NotPermissionException("无权访问该流程任务");
    }

    /**
     * 按照节点编码查询节点
     *
     * @param nodeCode     节点编码
     * @param definitionId 流程定义id
     */
    @Override
    public FlowNode getByNodeCode(String nodeCode, Long definitionId) {
        return flowNodeMapper.selectOne(QueryBuilder.lambda(FlowNode.class)
            .eq(FlowNode::getNodeCode, nodeCode)
            .eq(FlowNode::getDefinitionId, definitionId)
            .build());
    }

    /**
     * 催办任务
     *
     * @param bo 参数
     */
    @Override
    public boolean urgeTask(FlowUrgeTaskBo bo) {
        if (CollUtil.isEmpty(bo.getTaskIdList())) {
            return false;
        }
        List<UserDTO> userList = this.currentTaskAllUser(bo.getTaskIdList());
        if (CollUtil.isEmpty(userList)) {
            return false;
        }
        List<String> messageType = bo.getMessageType();
        String message = bo.getMessage();
        flwCommonService.sendMessage(messageType, message, "单据审批提醒", userList, PATH_TASK_WAITING);
        return true;
    }
}
