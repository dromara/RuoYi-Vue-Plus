package org.dromara.workflow.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.toolkit.JoinWrappers;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.workflow.domain.FlowInstanceBizExt;
import org.dromara.workflow.domain.bo.FlowTaskBo;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;

import java.util.List;

/**
 * 任务信息Mapper接口
 *
 * @author may
 * @date 2024-03-02
 */
public interface FlwHisTaskMapper extends BaseMapperPlus<FlowHisTask, FlowHisTaskVo> {

    default Page<FlowHisTaskVo> getListFinishTask(Page<FlowHisTaskVo> page,
                                                  FlowTaskBo bo,
                                                  List<String> categoryIds,
                                                  String userId) {
        MPJLambdaWrapper<FlowHisTask> wrapper = JoinWrappers.lambda("a", FlowHisTask.class)
            .selectAs(FlowHisTask::getId, FlowHisTaskVo::getId)
            .selectAs(FlowHisTask::getNodeCode, FlowHisTaskVo::getNodeCode)
            .selectAs(FlowHisTask::getNodeName, FlowHisTaskVo::getNodeName)
            .selectAs(FlowHisTask::getCooperateType, FlowHisTaskVo::getCooperateType)
            .selectAs(FlowHisTask::getApprover, FlowHisTaskVo::getApprover)
            .selectAs(FlowHisTask::getCollaborator, FlowHisTaskVo::getCollaborator)
            .selectAs(FlowHisTask::getNodeType, FlowHisTaskVo::getNodeType)
            .selectAs(FlowHisTask::getTargetNodeCode, FlowHisTaskVo::getTargetNodeCode)
            .selectAs(FlowHisTask::getTargetNodeName, FlowHisTaskVo::getTargetNodeName)
            .selectAs(FlowHisTask::getDefinitionId, FlowHisTaskVo::getDefinitionId)
            .selectAs(FlowHisTask::getInstanceId, FlowHisTaskVo::getInstanceId)
            .selectAs(FlowHisTask::getFlowStatus, FlowHisTaskVo::getFlowTaskStatus)
            .selectAs(FlowHisTask::getMessage, FlowHisTaskVo::getMessage)
            .selectAs(FlowHisTask::getExt, FlowHisTaskVo::getExt)
            .selectAs(FlowHisTask::getCreateTime, FlowHisTaskVo::getCreateTime)
            .selectAs(FlowHisTask::getUpdateTime, FlowHisTaskVo::getUpdateTime)
            .selectAs(FlowHisTask::getFormCustom, FlowHisTaskVo::getFormCustom)
            .selectAs(FlowHisTask::getFormPath, FlowHisTaskVo::getFormPath)
            .selectAs("b", FlowInstance::getFlowStatus, FlowHisTaskVo::getFlowStatus)
            .selectAs("b", FlowInstance::getBusinessId, FlowHisTaskVo::getBusinessId)
            .selectAs("b", FlowInstance::getCreateBy, FlowHisTaskVo::getCreateBy)
            .selectAs("c", FlowDefinition::getFlowName, FlowHisTaskVo::getFlowName)
            .selectAs("c", FlowDefinition::getFlowCode, FlowHisTaskVo::getFlowCode)
            .selectAs("c", FlowDefinition::getCategory, FlowHisTaskVo::getCategory)
            .selectAs("c", FlowDefinition::getVersion, FlowHisTaskVo::getVersion)
            .selectAs("biz", FlowInstanceBizExt::getBusinessCode, FlowHisTaskVo::getBusinessCode)
            .selectAs("biz", FlowInstanceBizExt::getBusinessTitle, FlowHisTaskVo::getBusinessTitle)
            .leftJoin(FlowInstance.class, "b", FlowInstance::getId, FlowHisTask::getInstanceId)
            .leftJoin(FlowDefinition.class, "c", FlowDefinition::getId, FlowHisTask::getDefinitionId)
            .leftJoin(FlowInstanceBizExt.class, "biz", FlowInstanceBizExt::getInstanceId, FlowInstance::getId)
            .eq("a", FlowHisTask::getDelFlag, "0")
            .eq("b", FlowInstance::getDelFlag, "0")
            .eq("c", FlowDefinition::getDelFlag, "0")
            .in("a", FlowHisTask::getNodeType, List.of("1", "3", "4"))
            .like(hasText(bo.getNodeName()), "a", FlowHisTask::getNodeName, bo.getNodeName())
            .like(hasText(bo.getFlowName()), "c", FlowDefinition::getFlowName, bo.getFlowName())
            .like(hasText(bo.getFlowCode()), "c", FlowDefinition::getFlowCode, bo.getFlowCode())
            .like(hasText(bo.getFlowStatus()), "b", FlowInstance::getFlowStatus, bo.getFlowStatus())
            .in(hasItems(bo.getCreateByIds()), "b", FlowInstance::getCreateBy, bo.getCreateByIds())
            .in(hasItems(categoryIds), "c", FlowDefinition::getCategory, categoryIds)
            .between(hasBetween(bo), "a", FlowHisTask::getCreateTime, bo.getParams().get("beginTime"), bo.getParams().get("endTime"))
            .eq(StringUtils.isNotBlank(userId), "a", FlowHisTask::getNodeType, NodeType.BETWEEN.getKey())
            .eq(StringUtils.isNotBlank(userId), "a", FlowHisTask::getApprover, userId)
            .orderByDesc("a", FlowHisTask::getCreateTime)
            .orderByDesc("a", FlowHisTask::getUpdateTime);
        return wrapper.page(page, FlowHisTaskVo.class);
    }

    default boolean hasText(String value) {
        return StringUtils.isNotBlank(value);
    }

    default boolean hasItems(List<?> values) {
        return values != null && !values.isEmpty();
    }

    default boolean hasBetween(FlowTaskBo bo) {
        return bo != null && bo.getParams() != null && bo.getParams().get("beginTime") != null && bo.getParams().get("endTime") != null;
    }

}
