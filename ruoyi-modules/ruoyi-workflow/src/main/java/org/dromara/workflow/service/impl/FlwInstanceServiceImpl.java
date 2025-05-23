package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.dto.UserDTO;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.constant.ExceptionCons;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.service.ChartService;
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
import org.dromara.workflow.domain.bo.FlowCancelBo;
import org.dromara.workflow.domain.bo.FlowInstanceBo;
import org.dromara.workflow.domain.bo.FlowInvalidBo;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;
import org.dromara.workflow.domain.vo.FlowInstanceVo;
import org.dromara.workflow.domain.vo.FlowVariableVo;
import org.dromara.workflow.handler.FlowProcessEventHandler;
import org.dromara.workflow.mapper.FlwCategoryMapper;
import org.dromara.workflow.mapper.FlwInstanceMapper;
import org.dromara.workflow.service.IFlwCommonService;
import org.dromara.workflow.service.IFlwInstanceService;
import org.dromara.workflow.service.IFlwTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
    private final ChartService chartService;
    private final TaskService taskService;
    private final FlowHisTaskMapper flowHisTaskMapper;
    private final FlowInstanceMapper flowInstanceMapper;
    private final FlowProcessEventHandler flowProcessEventHandler;
    private final IFlwTaskService flwTaskService;
    private final FlwInstanceMapper flwInstanceMapper;
    private final FlwCategoryMapper flwCategoryMapper;
    private final IFlwCommonService flwCommonService;

    /**
     * 分页查询正在运行的流程实例
     *
     * @param flowInstanceBo 流程实例
     * @param pageQuery      分页
     */
    @Override
    public TableDataInfo<FlowInstanceVo> selectRunningInstanceList(FlowInstanceBo flowInstanceBo, PageQuery pageQuery) {
        QueryWrapper<FlowInstanceBo> queryWrapper = buildQueryWrapper(flowInstanceBo);
        queryWrapper.in("fi.flow_status", BusinessStatusEnum.runningStatus());
        Page<FlowInstanceVo> page = flwInstanceMapper.selectInstanceList(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 分页查询已结束的流程实例
     *
     * @param flowInstanceBo 流程实例
     * @param pageQuery      分页
     */
    @Override
    public TableDataInfo<FlowInstanceVo> selectFinishInstanceList(FlowInstanceBo flowInstanceBo, PageQuery pageQuery) {
        QueryWrapper<FlowInstanceBo> queryWrapper = buildQueryWrapper(flowInstanceBo);
        queryWrapper.in("fi.flow_status", BusinessStatusEnum.finishStatus());
        Page<FlowInstanceVo> page = flwInstanceMapper.selectInstanceList(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 根据业务id查询流程实例详细信息
     *
     * @param businessId 业务id
     * @return 结果
     */
    @Override
    public FlowInstanceVo queryByBusinessId(Long businessId) {
        FlowInstance instance = this.selectInstByBusinessId(String.valueOf(businessId));
        FlowInstanceVo instanceVo = BeanUtil.toBean(instance, FlowInstanceVo.class);
        Definition definition = defService.getById(instanceVo.getDefinitionId());
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
    private QueryWrapper<FlowInstanceBo> buildQueryWrapper(FlowInstanceBo flowInstanceBo) {
        QueryWrapper<FlowInstanceBo> queryWrapper = Wrappers.query();
        queryWrapper.like(StringUtils.isNotBlank(flowInstanceBo.getNodeName()), "fi.node_name", flowInstanceBo.getNodeName());
        queryWrapper.like(StringUtils.isNotBlank(flowInstanceBo.getFlowName()), "fd.flow_name", flowInstanceBo.getFlowName());
        queryWrapper.like(StringUtils.isNotBlank(flowInstanceBo.getFlowCode()), "fd.flow_code", flowInstanceBo.getFlowCode());
        if (StringUtils.isNotBlank(flowInstanceBo.getCategory())) {
            List<Long> categoryIds = flwCategoryMapper.selectCategoryIdsByParentId(Convert.toLong(flowInstanceBo.getCategory()));
            queryWrapper.in("fd.category", StreamUtils.toList(categoryIds, Convert::toStr));
        }
        queryWrapper.eq(StringUtils.isNotBlank(flowInstanceBo.getBusinessId()), "fi.business_id", flowInstanceBo.getBusinessId());
        queryWrapper.in(CollUtil.isNotEmpty(flowInstanceBo.getCreateByIds()), "fi.create_by", flowInstanceBo.getCreateByIds());
        queryWrapper.eq("fi.del_flag", "0");
        queryWrapper.orderByDesc("fi.create_time");
        return queryWrapper;
    }

    /**
     * 根据业务id查询流程实例
     *
     * @param businessId 业务id
     */
    @Override
    public FlowInstance selectInstByBusinessId(String businessId) {
        return flowInstanceMapper.selectOne(new LambdaQueryWrapper<FlowInstance>().eq(FlowInstance::getBusinessId, businessId));
    }

    /**
     * 按照实例id查询流程实例
     *
     * @param instanceId 实例id
     */
    @Override
    public FlowInstance selectInstById(Long instanceId) {
        return flowInstanceMapper.selectById(instanceId);
    }

    /**
     * 按照实例id查询流程实例
     *
     * @param instanceIds 实例id
     */
    @Override
    public List<FlowInstance> selectInstListByIdList(List<Long> instanceIds) {
        return flowInstanceMapper.selectByIds(instanceIds);
    }

    /**
     * 按照业务id删除流程实例
     *
     * @param businessIds 业务id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByBusinessIds(List<Long> businessIds) {
        List<FlowInstance> flowInstances = flowInstanceMapper.selectList(new LambdaQueryWrapper<FlowInstance>().in(FlowInstance::getBusinessId, StreamUtils.toList(businessIds,Convert::toStr)));
        if (CollUtil.isEmpty(flowInstances)) {
            log.warn("未找到对应的流程实例信息，无法执行删除操作。");
            return false;
        }
        return insService.remove(StreamUtils.toList(flowInstances, FlowInstance::getId));
    }

    /**
     * 按照实例id删除流程实例
     *
     * @param instanceIds 实例id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByInstanceIds(List<Long> instanceIds) {
        // 获取实例信息
        List<Instance> instances = insService.getByIds(instanceIds);
        if (CollUtil.isEmpty(instances)) {
            log.warn("未找到对应的流程实例信息，无法执行删除操作。");
            return false;
        }
        // 获取定义信息
        Map<Long, Definition> definitionMap = defService.getByIds(
            StreamUtils.toList(instances, Instance::getDefinitionId)
        ).stream().collect(Collectors.toMap(Definition::getId, definition -> definition));

        // 逐一触发删除事件
        instances.forEach(instance -> {
            Definition definition = definitionMap.get(instance.getDefinitionId());
            if (ObjectUtil.isNull(definition)) {
                log.warn("实例 ID: {} 对应的流程定义信息未找到，跳过删除事件触发。", instance.getId());
                return;
            }
            flowProcessEventHandler.processDeleteHandler(definition.getFlowCode(), instance.getBusinessId());
        });

        // 删除实例
        return insService.remove(instanceIds);
    }

    /**
     * 撤销流程
     *
     * @param bo 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelProcessApply(FlowCancelBo bo) {
        try {
            Instance instance = selectInstByBusinessId(bo.getBusinessId());
            if (instance == null) {
                throw new ServiceException(ExceptionCons.NOT_FOUNT_INSTANCE);
            }
            Definition definition = defService.getById(instance.getDefinitionId());
            if (definition == null) {
                throw new ServiceException(ExceptionCons.NOT_FOUNT_DEF);
            }
            String message = bo.getMessage();
            BusinessStatusEnum.checkCancelStatus(instance.getFlowStatus());
            String applyNodeCode = flwCommonService.applyNodeCode(definition.getId());
            //撤销
            flwCommonService.backTask(message, instance.getId(), applyNodeCode, BusinessStatusEnum.CANCEL.getStatus(), BusinessStatusEnum.CANCEL.getStatus());
            //判断或签节点是否有多个，只保留一个
            List<Task> currentTaskList = taskService.list(FlowEngine.newTask().setInstanceId(instance.getId()));
            if (CollUtil.isNotEmpty(currentTaskList)) {
                if (currentTaskList.size() > 1) {
                    currentTaskList.remove(0);
                    flwCommonService.deleteRunTask(StreamUtils.toList(currentTaskList, Task::getId));
                }
            }

        } catch (Exception e) {
            log.error("撤销失败: {}", e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
        return true;
    }

    /**
     * 获取当前登陆人发起的流程实例
     *
     * @param instanceBo 流程实例
     * @param pageQuery  分页
     */
    @Override
    public TableDataInfo<FlowInstanceVo> selectCurrentInstanceList(FlowInstanceBo instanceBo, PageQuery pageQuery) {
        QueryWrapper<FlowInstanceBo> queryWrapper = buildQueryWrapper(instanceBo);
        queryWrapper.eq("fi.create_by", LoginHelper.getUserIdStr());
        Page<FlowInstanceVo> page = flwInstanceMapper.selectInstanceList(pageQuery.build(), queryWrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 获取流程图,流程记录
     *
     * @param businessId 业务id
     */
    @Override
    public Map<String, Object> flowImage(String businessId) {
        FlowInstance flowInstance = this.selectInstByBusinessId(businessId);
        if (ObjectUtil.isNull(flowInstance)) {
            throw new ServiceException(ExceptionCons.NOT_FOUNT_INSTANCE);
        }
        Long instanceId = flowInstance.getId();
        //运行中的任务
        List<FlowHisTaskVo> list = new ArrayList<>();
        List<FlowTask> flowTaskList = flwTaskService.selectByInstId(instanceId);
        if (CollUtil.isNotEmpty(flowTaskList)) {
            List<FlowHisTaskVo> flowHisTaskVos = BeanUtil.copyToList(flowTaskList, FlowHisTaskVo.class);
            for (FlowHisTaskVo flowHisTaskVo : flowHisTaskVos) {
                flowHisTaskVo.setFlowStatus(TaskStatusEnum.WAITING.getStatus());
                flowHisTaskVo.setUpdateTime(null);
                flowHisTaskVo.setRunDuration(null);
                List<UserDTO> allUser = flwTaskService.currentTaskAllUser(flowHisTaskVo.getId());
                if (CollUtil.isNotEmpty(allUser)) {
                    String join = StreamUtils.join(allUser, e -> String.valueOf(e.getUserId()));
                    flowHisTaskVo.setApprover(join);
                }
                if (BusinessStatusEnum.isDraftOrCancelOrBack(flowInstance.getFlowStatus())) {
                    flowHisTaskVo.setApprover(LoginHelper.getUserIdStr());
                    flowHisTaskVo.setApproveName(LoginHelper.getLoginUser().getNickname());
                }
            }
            list.addAll(flowHisTaskVos);
        }
        //历史任务
        LambdaQueryWrapper<FlowHisTask> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(FlowHisTask::getInstanceId, instanceId);
        wrapper.eq(FlowHisTask::getNodeType, NodeType.BETWEEN.getKey());
        wrapper.orderByDesc(FlowHisTask::getCreateTime).orderByDesc(FlowHisTask::getUpdateTime);
        List<FlowHisTask> flowHisTasks = flowHisTaskMapper.selectList(wrapper);
        if (CollUtil.isNotEmpty(flowHisTasks)) {
            list.addAll(BeanUtil.copyToList(flowHisTasks, FlowHisTaskVo.class));
        }
        String flowChart = chartService.chartIns(instanceId);
        return Map.of("list", list, "image", flowChart);
    }

    /**
     * 按照实例id更新状态
     *
     * @param instanceId 实例id
     * @param status     状态
     */
    @Override
    public void updateStatus(Long instanceId, String status) {
        LambdaUpdateWrapper<FlowInstance> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(FlowInstance::getFlowStatus, status);
        wrapper.eq(FlowInstance::getId, instanceId);
        flowInstanceMapper.update(wrapper);
    }

    /**
     * 获取流程变量
     *
     * @param instanceId 实例id
     */
    @Override
    public Map<String, Object> instanceVariable(Long instanceId) {
        Map<String, Object> map = new HashMap<>();
        FlowInstance flowInstance = flowInstanceMapper.selectById(instanceId);
        Map<String, Object> variableMap = flowInstance.getVariableMap();
        List<FlowVariableVo> list = new ArrayList<>();
        if (CollUtil.isNotEmpty(variableMap)) {
            for (Map.Entry<String, Object> entry : variableMap.entrySet()) {
                FlowVariableVo flowVariableVo = new FlowVariableVo();
                flowVariableVo.setKey(entry.getKey());
                flowVariableVo.setValue(entry.getValue().toString());
                list.add(flowVariableVo);
            }
        }
        map.put("variableList", list);
        map.put("variable", flowInstance.getVariable());
        return map;
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
     * 按任务id查询实例
     *
     * @param taskIdList 任务id
     */
    @Override
    public List<FlowInstance> selectByTaskIdList(List<Long> taskIdList) {
        if (CollUtil.isEmpty(taskIdList)) {
            return Collections.emptyList();
        }
        Set<Long> instanceIds = new HashSet<>();
        List<FlowTask> flowTaskList = flwTaskService.selectByIdList(taskIdList);
        for (FlowTask flowTask : flowTaskList) {
            instanceIds.add(flowTask.getInstanceId());
        }
        List<FlowHisTask> flowHisTaskList = flwTaskService.selectHisTaskByIdList(taskIdList);
        for (FlowHisTask flowHisTask : flowHisTaskList) {
            instanceIds.add(flowHisTask.getInstanceId());
        }
        if (!instanceIds.isEmpty()) {
            return this.selectInstListByIdList(new ArrayList<>(instanceIds));
        }
        return Collections.emptyList();
    }

    /**
     * 作废流程
     *
     * @param bo 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processInvalid(FlowInvalidBo bo) {
        try {
            Instance instance = insService.getById(bo.getId());
            if (instance != null) {
                BusinessStatusEnum.checkInvalidStatus(instance.getFlowStatus());
            }
            List<FlowTask> flowTaskList = flwTaskService.selectByInstId(bo.getId());
            for (FlowTask flowTask : flowTaskList) {
                FlowParams flowParams = new FlowParams();
                flowParams.message(bo.getComment());
                flowParams.flowStatus(BusinessStatusEnum.INVALID.getStatus())
                    .hisStatus(TaskStatusEnum.INVALID.getStatus());
                flowParams.ignore(true);
                taskService.termination(flowTask.getId(), flowParams);
            }
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }
}
