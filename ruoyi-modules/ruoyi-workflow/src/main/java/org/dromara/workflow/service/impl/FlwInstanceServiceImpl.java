package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.PageResult;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.query.LambdaJoinQueryBuilder;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.constant.ExceptionCons;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.entity.User;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.dromara.warm.flow.orm.mapper.FlowInstanceMapper;
import org.dromara.workflow.common.ConditionalOnEnable;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.domain.FlowInstanceBizExt;
import org.dromara.workflow.domain.bo.FlowCancelBo;
import org.dromara.workflow.domain.bo.FlowInstanceBo;
import org.dromara.workflow.domain.bo.FlowInvalidBo;
import org.dromara.workflow.domain.bo.FlowVariableBo;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.dromara.workflow.domain.vo.FlowInstanceVo;
import org.dromara.workflow.handler.FlowProcessEventHandler;
import org.dromara.workflow.mapper.FlwCategoryMapper;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.dromara.workflow.service.IFlwInstanceService;
import org.dromara.workflow.service.IFlwTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;

/**
 * 流程实例 服务层实现
 *
 * @author may
 */
@ConditionalOnEnable
@Slf4j
@RequiredArgsConstructor
@Service
public class FlwInstanceServiceImpl implements IFlwInstanceService {

    private final InsService insService;
    private final DefService defService;
    private final TaskService taskService;
    private final FlowHisTaskMapper flowHisTaskMapper;
    private final FlowInstanceMapper flowInstanceMapper;
    private final FlowProcessEventHandler flowProcessEventHandler;
    private final IFlwTaskService flwTaskService;
    private final FlwInstanceMapper flwInstanceMapper;
    private final FlwCategoryMapper flwCategoryMapper;

    /**
     * 分页查询正在运行的流程实例
     *
     * @param flowInstanceBo 流程实例
     * @param pageQuery      分页
     * @return 当前符合条件的运行中流程实例分页结果
     */
    @Override
    public PageResult<FlowInstanceVo> selectRunningInstanceList(FlowInstanceBo flowInstanceBo, PageQuery pageQuery) {
        MPJLambdaWrapper<FlowInstance> queryWrapper = buildQueryWrapper(flowInstanceBo);
        queryWrapper.in("fi", FlowInstance::getFlowStatus, BusinessStatusEnum.runningStatus());
        Page<FlowInstanceVo> page = flwInstanceMapper.selectInstanceList(pageQuery.build(), queryWrapper);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 分页查询已结束的流程实例
     *
     * @param flowInstanceBo 流程实例
     * @param pageQuery      分页
     * @return 当前符合条件的已结束流程实例分页结果
     */
    @Override
    public PageResult<FlowInstanceVo> selectFinishInstanceList(FlowInstanceBo flowInstanceBo, PageQuery pageQuery) {
        MPJLambdaWrapper<FlowInstance> queryWrapper = buildQueryWrapper(flowInstanceBo);
        queryWrapper.in("fi", FlowInstance::getFlowStatus, BusinessStatusEnum.finishStatus());
        Page<FlowInstanceVo> page = flwInstanceMapper.selectInstanceList(pageQuery.build(), queryWrapper);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 根据业务id查询流程实例详细信息
     *
     * @param businessId 业务id
     * @return 结果
     */
    @Override
    public FlowInstanceVo queryByBusinessId(Long businessId) {
        FlowInstance instance = this.selectInstByBusinessId(Convert.toStr(businessId));
        if (ObjectUtil.isNull(instance)) {
            throw new ServiceException(ExceptionCons.NOT_FOUNT_INSTANCE);
        }
        FlowInstanceVo instanceVo = BeanUtil.toBean(instance, FlowInstanceVo.class);
        Definition definition = defService.getById(instanceVo.getDefinitionId());
        if (ObjectUtil.isNull(definition)) {
            throw new ServiceException(ExceptionCons.NOT_FOUNT_DEF);
        }
        instanceVo.setFlowName(definition.getFlowName());
        instanceVo.setFlowCode(definition.getFlowCode());
        instanceVo.setVersion(definition.getVersion());
        instanceVo.setFormCustom(definition.getFormCustom());
        instanceVo.setFormPath(definition.getFormPath());
        instanceVo.setCategory(definition.getCategory());
        return instanceVo;
    }

    /**
     * 通用查询条件
     *
     * @param flowInstanceBo 查询条件
     * @return 查询条件构造方法
     */
    private MPJLambdaWrapper<FlowInstance> buildQueryWrapper(FlowInstanceBo flowInstanceBo) {
        LambdaJoinQueryBuilder<FlowInstance> queryBuilder = QueryBuilder.lambdaJoin("fi", FlowInstance.class)
            .select(FlowInstance::getId, FlowInstance::getCreateTime, FlowInstance::getUpdateTime,
                FlowInstance::getDelFlag, FlowInstance::getDefinitionId, FlowInstance::getBusinessId,
                FlowInstance::getNodeType, FlowInstance::getNodeCode, FlowInstance::getNodeName,
                FlowInstance::getVariable, FlowInstance::getFlowStatus, FlowInstance::getActivityStatus,
                FlowInstance::getCreateBy, FlowInstance::getExt)
            .select("fd", org.dromara.warm.flow.orm.entity.FlowDefinition::getFlowName,
                org.dromara.warm.flow.orm.entity.FlowDefinition::getFlowCode,
                org.dromara.warm.flow.orm.entity.FlowDefinition::getVersion,
                org.dromara.warm.flow.orm.entity.FlowDefinition::getFormCustom,
                org.dromara.warm.flow.orm.entity.FlowDefinition::getFormPath,
                org.dromara.warm.flow.orm.entity.FlowDefinition::getCategory)
            .select("biz", FlowInstanceBizExt::getBusinessCode, FlowInstanceBizExt::getBusinessTitle)
            .leftJoin(org.dromara.warm.flow.orm.entity.FlowDefinition.class, "fd", org.dromara.warm.flow.orm.entity.FlowDefinition::getId, FlowInstance::getDefinitionId)
            .leftJoin(FlowInstanceBizExt.class, "biz", FlowInstanceBizExt::getInstanceId, FlowInstance::getId);
        queryBuilder.likeIfText("fi", FlowInstance::getNodeName, flowInstanceBo.getNodeName());
        queryBuilder.likeIfText("fd", org.dromara.warm.flow.orm.entity.FlowDefinition::getFlowName, flowInstanceBo.getFlowName());
        queryBuilder.likeIfText("fd", org.dromara.warm.flow.orm.entity.FlowDefinition::getFlowCode, flowInstanceBo.getFlowCode());
        if (StringUtils.isNotBlank(flowInstanceBo.getCategory())) {
            List<Long> categoryIds = flwCategoryMapper.selectCategoryIdsByParentId(Convert.toLong(flowInstanceBo.getCategory()));
            queryBuilder.inIfNotEmpty("fd", org.dromara.warm.flow.orm.entity.FlowDefinition::getCategory, StreamUtils.toList(categoryIds, Convert::toStr));
        }
        queryBuilder.eqIfText("fi", FlowInstance::getBusinessId, flowInstanceBo.getBusinessId());
        queryBuilder.inIfNotEmpty("fi", FlowInstance::getCreateBy, flowInstanceBo.getCreateByIds());
        queryBuilder.orderByDesc("fi", FlowInstance::getCreateTime);
        return queryBuilder.build();
    }

    /**
     * 根据业务id查询流程实例
     *
     * @param businessId 业务id
     * @return 对应的流程实例，不存在时返回 {@code null}
     */
    @Override
    public FlowInstance selectInstByBusinessId(String businessId) {
        return flowInstanceMapper.selectOne(
            QueryBuilder.lambda(FlowInstance.class)
                .eq(FlowInstance::getBusinessId, businessId)
                .build());
    }

    /**
     * 按照实例id查询流程实例
     *
     * @param instanceId 实例id
     * @return 实例详情，不存在时返回 {@code null}
     */
    @Override
    public FlowInstance selectInstById(Long instanceId) {
        return flowInstanceMapper.selectById(instanceId);
    }

    /**
     * 按照实例id查询流程实例
     *
     * @param instanceIds 实例id
     * @return 实例列表
     */
    @Override
    public List<FlowInstance> selectInstListByIdList(Collection<Long> instanceIds) {
        return flowInstanceMapper.selectByIds(instanceIds);
    }

    /**
     * 按照业务id删除流程实例
     *
     * @param businessIds 业务id
     * @return 删除成功返回 {@code true}，未找到实例时返回 {@code false}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByBusinessIds(List<String> businessIds) {
        List<FlowInstance> flowInstances = flowInstanceMapper.selectList(
            QueryBuilder.lambda(FlowInstance.class)
                .in(FlowInstance::getBusinessId, businessIds)
                .build());
        if (CollUtil.isEmpty(flowInstances)) {
            log.warn("未找到对应的流程实例信息，无法执行删除操作。");
            return false;
        }
        // 发送事件
        processDeleteHandler(flowInstances);
        return insService.remove(StreamUtils.toList(flowInstances, FlowInstance::getId));
    }

    /**
     * 按照实例id删除流程实例
     *
     * @param instanceIds 实例id
     * @return 删除成功返回 {@code true}，未找到实例时返回 {@code false}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByInstanceIds(Collection<Long> instanceIds) {
        // 获取实例信息
        List<FlowInstance> flowInstances = flowInstanceMapper.selectByIds(instanceIds);
        if (CollUtil.isEmpty(flowInstances)) {
            log.warn("未找到对应的流程实例信息，无法执行删除操作。");
            return false;
        }
        // 发送事件
        processDeleteHandler(flowInstances);
        // 删除实例
        return insService.remove((List<Long>) instanceIds);
    }

    /**
     * 按照实例id删除已完成的流程实例
     *
     * @param instanceIds 实例id
     * @return 删除成功返回 {@code true}，未找到实例时返回 {@code false}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteHisByInstanceIds(Collection<Long> instanceIds) {
        // 获取实例信息
        List<FlowInstance> flowInstances = flowInstanceMapper.selectByIds(instanceIds);
        if (CollUtil.isEmpty(flowInstances)) {
            log.warn("未找到对应的流程实例信息，无法执行删除操作。");
            return false;
        }
        // 发送事件
        processDeleteHandler(flowInstances);
        List<FlowTask> flowTaskList = flwTaskService.selectByInstIds(instanceIds);
        if (CollUtil.isNotEmpty(flowTaskList)) {
            FlowEngine.userService().deleteByTaskIds(StreamUtils.toList(flowTaskList, FlowTask::getId));
        }
        FlowEngine.taskService().deleteByInsIds((List<Long>) instanceIds);
        FlowEngine.hisTaskService().deleteByInsIds((List<Long>) instanceIds);
        FlowEngine.insService().removeByIds(instanceIds);
        return true;
    }


    private void processDeleteHandler(List<FlowInstance> flowInstances) {

        String userId = LoginHelper.getUserIdStr();
        for (FlowInstance flowInstance : flowInstances) {
            //如果创建人与当前登陆人一致或者当前登陆人为管理员才能删除
            if (LoginHelper.isSuperAdmin() || flowInstance.getCreateBy().equals(userId)) {
                continue;
            }
            throw new ServiceException("权限不足，无法删除流程实例信息!");
        }
        // 获取定义信息
        Map<Long, Definition> definitionMap = StreamUtils.toMap(
            defService.getByIds(StreamUtils.toList(flowInstances, Instance::getDefinitionId)),
            Definition::getId,
            Function.identity()
        );

        // 逐一触发删除事件
        flowInstances.forEach(instance -> {
            Definition definition = definitionMap.get(instance.getDefinitionId());
            if (ObjectUtil.isNull(definition)) {
                log.warn("实例 ID: {} 对应的流程定义信息未找到，跳过删除事件触发。", instance.getId());
                return;
            }
            flowProcessEventHandler.processDeleteHandler(definition.getFlowCode(), instance.getBusinessId());
        });
    }

    /**
     * 撤销流程
     *
     * @param bo 参数
     * @return 撤销成功返回 {@code true}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelProcessApply(FlowCancelBo bo) {
        Instance instance = selectInstByBusinessId(bo.businessId());
        if (instance == null) {
            throw new ServiceException(ExceptionCons.NOT_FOUNT_INSTANCE);
        }
        Definition definition = defService.getById(instance.getDefinitionId());
        if (definition == null) {
            throw new ServiceException(ExceptionCons.NOT_FOUNT_DEF);
        }
        String userIdStr = LoginHelper.getUserIdStr();
        if (!LoginHelper.isSuperAdmin() && !instance.getCreateBy().equals(userIdStr)) {
            throw new ServiceException("权限不足，无法撤销流程!");
        }
        String message = bo.message();
        BusinessStatusEnum.checkCancelStatus(instance.getFlowStatus());
        FlowParams flowParams = FlowParams.build()
            .message(message)
            .flowStatus(BusinessStatusEnum.CANCEL.getStatus())
            .hisStatus(BusinessStatusEnum.CANCEL.getStatus())
            .handler(userIdStr)
            .ignore(true);
        taskService.revoke(instance.getId(), flowParams);
        return true;
    }

    /**
     * 获取当前登陆人发起的流程实例
     *
     * @param instanceBo 流程实例
     * @param pageQuery  分页
     * @return 当前登录人发起的流程实例分页结果
     */
    @Override
    public PageResult<FlowInstanceVo> selectCurrentInstanceList(FlowInstanceBo instanceBo, PageQuery pageQuery) {
        MPJLambdaWrapper<FlowInstance> queryWrapper = buildQueryWrapper(instanceBo);
        queryWrapper.eq("fi", FlowInstance::getCreateBy, LoginHelper.getUserIdStr());
        Page<FlowInstanceVo> page = flwInstanceMapper.selectInstanceList(pageQuery.build(), queryWrapper);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 获取流程图,流程记录
     *
     * @param businessId 业务id
     * @return 包含流程记录列表和实例 id 的结果集
     */
    @Override
    public Map<String, Object> flowHisTaskList(String businessId) {
        FlowInstance flowInstance = this.selectInstByBusinessId(businessId);
        if (ObjectUtil.isNull(flowInstance)) {
            throw new ServiceException(ExceptionCons.NOT_FOUNT_INSTANCE);
        }
        Long instanceId = flowInstance.getId();

        // 先组装待审批任务（运行中的任务）
        List<FlowHisTaskVo> runningTaskVos = new ArrayList<>();
        List<FlowTask> runningTasks = flwTaskService.selectByInstId(instanceId);
        if (CollUtil.isNotEmpty(runningTasks)) {
            runningTaskVos = BeanUtil.copyToList(runningTasks, FlowHisTaskVo.class);

            List<User> associatedUsers = FlowEngine.userService()
                .getByAssociateds(StreamUtils.toList(runningTasks, FlowTask::getId));
            Map<Long, List<User>> taskUserMap = StreamUtils.groupByKey(associatedUsers, User::getAssociated);

            for (FlowHisTaskVo vo : runningTaskVos) {
                vo.setFlowStatus(TaskStatusEnum.WAITING.getStatus());
                vo.setUpdateTime(null);
                vo.setRunDuration(null);

                List<User> users = taskUserMap.get(vo.getId());
                if (CollUtil.isNotEmpty(users)) {
                    vo.setApprover(StreamUtils.join(users, User::getProcessedBy));
                }
            }
        }

        // 再组装历史任务（已处理任务）
        List<FlowHisTaskVo> hisTaskVos = new ArrayList<>();
        List<FlowHisTask> hisTasks = flowHisTaskMapper.selectList(
            QueryBuilder.lambda(FlowHisTask.class)
                .eq(FlowHisTask::getInstanceId, instanceId)
                .eq(FlowHisTask::getNodeType, NodeType.BETWEEN.getKey())
                .orderByDesc(FlowHisTask::getUpdateTime)
                .build()
        );
        if (CollUtil.isNotEmpty(hisTasks)) {
            hisTaskVos = BeanUtil.copyToList(hisTasks, FlowHisTaskVo.class);
        }

        // 结果列表，待审批任务在前，历史任务在后
        List<FlowHisTaskVo> combinedList = new ArrayList<>();
        combinedList.addAll(runningTaskVos);
        combinedList.addAll(hisTaskVos);

        return Map.of("list", combinedList, "instanceId", instanceId);
    }

    /**
     * 按照实例id更新状态
     *
     * @param instanceId 实例id
     * @param status     状态
     */
    @Override
    public void updateStatus(Long instanceId, String status) {
        flowInstanceMapper.update(Wrappers.lambdaUpdate(FlowInstance.class)
            .set(FlowInstance::getFlowStatus, status)
            .eq(FlowInstance::getId, instanceId));
    }

    /**
     * 获取流程变量
     *
     * @param instanceId 实例id
     * @return 变量明细列表及原始变量字符串
     */
    @Override
    public Map<String, Object> instanceVariable(Long instanceId) {
        FlowInstance flowInstance = flowInstanceMapper.selectById(instanceId);
        if (ObjectUtil.isNull(flowInstance)) {
            throw new ServiceException(ExceptionCons.NOT_FOUNT_INSTANCE);
        }
        Map<String, Object> variableMap = Optional.ofNullable(flowInstance.getVariableMap()).orElse(Collections.emptyMap());
        List<Map<String, Object>> variableList = variableMap.entrySet().stream()
            .map(entry -> Map.of("key", entry.getKey(), "value", entry.getValue()))
            .toList();
        return Map.of("variableList", variableList, "variable", flowInstance.getVariable());
    }

    /**
     * 设置流程变量
     *
     * @param bo 参数
     * @return 更新成功返回 {@code true}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateVariable(FlowVariableBo bo) {
        FlowInstance flowInstance = flowInstanceMapper.selectById(bo.instanceId());
        if (flowInstance == null) {
            throw new ServiceException(ExceptionCons.NOT_FOUNT_INSTANCE);
        }
        Map<String, Object> variableMap = new HashMap<>(Optional.ofNullable(flowInstance.getVariableMap()).orElse(Collections.emptyMap()));
        if (!variableMap.containsKey(bo.key())) {
            log.error("变量不存在: {}", bo.key());
            return false;
        }
        variableMap.put(bo.key(), bo.value());
        flowInstance.setVariable(FlowEngine.jsonConvert.objToStr(variableMap));
        return flowInstanceMapper.updateById(flowInstance) > 0;
    }

    /**
     * 设置流程变量
     *
     * @param instanceId 实例id
     * @param variable   流程变量
     */
    @Override
    public void setVariable(Long instanceId, Map<String, Object> variable) {
        Instance instance = insService.getById(instanceId);
        if (instance != null) {
            taskService.mergeVariable(instance, variable);
            insService.updateById(instance);
        }
    }

    /**
     * 按任务id查询实例
     *
     * @param taskId 任务id
     * @return 任务关联的流程实例，运行中任务不存在时回退查询历史任务
     */
    @Override
    public FlowInstance selectByTaskId(Long taskId) {
        Task task = taskService.getById(taskId);
        if (task == null) {
            FlowHisTask flowHisTask = flwTaskService.selectHisTaskById(taskId);
            if (flowHisTask != null) {
                return this.selectInstById(flowHisTask.getInstanceId());
            }
        } else {
            return this.selectInstById(task.getInstanceId());
        }
        return null;
    }

    /**
     * 作废流程
     *
     * @param bo 参数
     * @return 作废成功返回 {@code true}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processInvalid(FlowInvalidBo bo) {
        Instance instance = insService.getById(bo.id());
        if (instance != null) {
            BusinessStatusEnum.checkInvalidStatus(instance.getFlowStatus());
        }
        FlowParams flowParams = FlowParams.build()
            .message(bo.comment())
            .flowStatus(BusinessStatusEnum.INVALID.getStatus())
            .hisStatus(TaskStatusEnum.INVALID.getStatus())
            .ignore(true);
        taskService.terminationByInsId(bo.id(), flowParams);
        return true;
    }
}
