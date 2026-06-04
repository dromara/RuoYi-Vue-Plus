package org.dromara.workflow.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.yulichang.base.MPJBaseMapper;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.common.mybatis.core.query.QueryBuilder;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.workflow.common.enums.TaskStatusEnum;
import org.dromara.workflow.domain.FlowInstanceBizExt;
import org.dromara.workflow.domain.bo.FlowTaskBo;
import org.dromara.workflow.domain.vo.FlowHisTaskVo;

import java.util.List;
import java.util.Map;

/**
 * 任务信息Mapper接口
 *
 * @author may
 * @date 2024-03-02
 */
public interface FlwHisTaskMapper extends BaseMapperPlus<FlowHisTask, FlowHisTaskVo>, MPJBaseMapper<FlowHisTask> {

    /**
     * 分页查询已办任务列表。
     *
     * @param page        分页对象
     * @param bo          查询条件
     * @param categoryIds 流程分类 ID 集合
     * @param userId      当前用户 ID
     * @return 已办任务分页结果
     */
    default Page<FlowHisTaskVo> getListFinishTask(Page<FlowHisTaskVo> page, FlowTaskBo bo, List<String> categoryIds, String userId) {
        Map<String, Object> params = bo.getParams();
        return QueryBuilder.lambdaJoin("a", FlowHisTask.class)
            .select(FlowHisTask::getId, FlowHisTask::getNodeCode, FlowHisTask::getNodeName,
                FlowHisTask::getCooperateType, FlowHisTask::getApprover, FlowHisTask::getCollaborator,
                FlowHisTask::getNodeType, FlowHisTask::getTargetNodeCode, FlowHisTask::getTargetNodeName,
                FlowHisTask::getDefinitionId, FlowHisTask::getInstanceId)
            .selectAs(FlowHisTask::getFlowStatus, FlowHisTaskVo::getFlowTaskStatus)
            .select(FlowHisTask::getMessage, FlowHisTask::getExt, FlowHisTask::getCreateTime,
                FlowHisTask::getUpdateTime, FlowHisTask::getFormCustom, FlowHisTask::getFormPath)
            .select("b", FlowInstance::getFlowStatus, FlowInstance::getBusinessId, FlowInstance::getCreateBy)
            .select("c", FlowDefinition::getFlowName, FlowDefinition::getFlowCode, FlowDefinition::getCategory,
                FlowDefinition::getVersion)
            .select("biz", FlowInstanceBizExt::getBusinessCode, FlowInstanceBizExt::getBusinessTitle)
            .leftJoin(FlowInstance.class, "b", FlowInstance::getId, FlowHisTask::getInstanceId)
            .leftJoin(FlowDefinition.class, "c", FlowDefinition::getId, FlowHisTask::getDefinitionId)
            .leftJoin(FlowInstanceBizExt.class, "biz", FlowInstanceBizExt::getInstanceId, FlowInstance::getId)
            .in("a", FlowHisTask::getNodeType, List.of(1, 3, 4))
            .ne("a", FlowHisTask::getFlowStatus, TaskStatusEnum.COPY.getStatus())
            .likeIfText("a", FlowHisTask::getNodeName, bo.getNodeName())
            .likeIfText("c", FlowDefinition::getFlowName, bo.getFlowName())
            .likeIfText("c", FlowDefinition::getFlowCode, bo.getFlowCode())
            .likeIfText("b", FlowInstance::getFlowStatus, bo.getFlowStatus())
            .inIfNotEmpty("b", FlowInstance::getCreateBy, bo.getCreateByIds())
            .inIfNotEmpty("c", FlowDefinition::getCategory, categoryIds)
            .betweenParams("a", FlowHisTask::getCreateTime, params, "beginTime", "endTime")
            .eq(userId != null && !userId.isBlank(), "a", FlowHisTask::getNodeType, NodeType.BETWEEN.getKey())
            .eqIfText("a", FlowHisTask::getApprover, userId)
            .orderByDesc("a", FlowHisTask::getCreateTime)
            .orderByDesc("a", FlowHisTask::getUpdateTime)
            .page(page, FlowHisTaskVo.class);
    }

}
